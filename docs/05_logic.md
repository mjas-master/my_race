# 05. 로직 설명서 — 수집 · 예측 · 적중 검증

## 1. 데이터 수집 파이프라인

```
KRA OpenAPI ──▶ KraApiClient(external) ──▶ IngestService(정제/업서트) ──▶ DB
                                                     │
                                                     └─▶ 변경 감지 시 PredictionService.computeForRace
```

### 1.1 스케줄 (Asia/Seoul)
| 작업 | 크론 | 설명 |
|---|---|---|
| 경주 계획 | 매주 월 06:00 | 이번 주·다음 주 편성(meet별) 수집 → Race(PLANNED) 생성 |
| 출마표 | 화·수·목 12:00, 18:00 / 금·토·일 07:00 | 출전표 수집 → Entry 업서트 → 상태 ENTRIES_FIXED → 예측 계산 |
| 결과 | 금·토·일 10:30~19:00 매 10분 | 결과·배당 수집 → Entry.finishPos 갱신 → FINISHED → 적중 평가 |
| 마스터 | 매일 03:00 | 이번 주 출전 Horse/Jockey/Trainer 상세 갱신 |

### 1.2 정제 규칙
- 자연키: `Race(meet, raceDate, raceNo)`, `Entry(raceId, gateNo)`, `Horse(hrNo)`, `Jockey(jkNo)`, `Trainer(trNo)`
- 원본 `yyyyMMdd` → `LocalDate`; 기록 `1:14.2` 문자열 유지 + `finishTimeMs` 파생 컬럼
- 출전 취소(`cancel` 플래그)는 Entry 삭제가 아니라 `scratched=true` 로 유지(예측 재계산 시 제외)
- 응답 필드 누락 시 null 허용, 예측 팩터에서는 결측 → 평균값 대체(팩터별 명시)
- 재시도: 5xx/timeout 3회 지수 백오프(1s, 4s, 16s), 실패 시 `UPSTREAM_UNAVAILABLE` 로그 + 다음 주기 재시도

### 1.3 변경 감지
Entry 집합 해시(`gateNo|hrNo|jkNo|weight` 정렬 후 SHA-256)를 Race.entriesHash 에 저장. 해시가 바뀌면 재예측 + 알림(`RANK_UPDATED`).

## 2. 예측 엔진 v1 (규칙 기반 점수)

목표: ML 없이 **설명 가능한** 점수. 각 팩터는 0~100 정규화 후 가중합. 결과는 순위 + 근거 문장.

### 2.1 팩터 정의
| 키 | 가중치 | 계산 | 결측 처리 |
|---|---|---|---|
| `recentForm` | 0.30 | 최근 3전 평균 착순 `f` → `100 × clamp((8 − f)/7, 0, 1)`. 두수 보정: 착순/출전두수 비율 사용 | 출전 2전 미만 → 50 |
| `distanceFit` | 0.20 | 해당 거리 ±100m 성적. `(승×3 + 준×2 + 3착×1) / (출전×3) × 100`. 출전 0 → 40 | 40 |
| `jockeyForm` | 0.15 | 기수 최근 30일 승률 `w` → `min(100, w × 400)` (25% 승률 = 100) | 30일 출전 5회 미만 → 시즌 승률 사용 |
| `trainerForm` | 0.10 | 조교사 시즌 승률 × 500 clamp 100 | 50 |
| `weightChange` | 0.10 | 전주 대비 부담중량 변화 `Δ`(kg). `50 − Δ × 10` clamp 0~100 (감량 유리) | 50 |
| `restDays` | 0.10 | 휴양일 `d`. 14~35일 = 100, 7~13 = 70, 36~90 = 60, 91+ = 35, <7 = 40 | 60 |
| `classMove` | 0.05 | 등급 강등(쉬운 상대) +100, 유지 60, 승급 30 | 60 |

### 2.2 산식
```
score = Σ (weight_i × factor_i)       // 0~100
tieBreak = recentForm, 그 다음 gateNo 작은 순
rank = score 내림차순
```
`factorsJson` 에는 값 + 사람이 읽는 문장을 함께 저장:
```json
{ "recentForm": {"v": 81.4, "text": "최근 3전 평균 2.3위"},
  "distanceFit": {"v": 66.7, "text": "1200m 4전 2승 1준"} , ... }
```

### 2.3 재현성
- `inputSnapshotJson` 에 팩터 계산에 쓰인 원자료 요약(경주마별 최근 10전 id, 기수 30일 집계) 저장
- `engineVersion` 문자열 변경 시 신규 Prediction 행 추가(기존 유지) → 버전 간 적중률 비교 API 가능

### 2.4 v2 로드맵 (참고)
LightGBM 이진분류(1착 여부) + 랭킹 손실. 피처는 v1 팩터 원값 + 배당(시장 정보) + 주로 상태. 백테스트 하네스 `backend/src/test/.../BacktestTest.kt` 자리 마련.

## 3. 적중 평가
경주 FINISHED 시 최신 Prediction 기준:
```
p1 = 추천 1위 gateNo, p123 = 추천 1~3위 집합
a1..a3 = 실제 1~3착 gateNo
winHit   = (p1 == a1)
placeHit = (p1 in {a1, a2})
showHit  = (p1 in {a1, a2, a3})
top3Exact= (p123 == {a1,a2,a3})
```
- 취소·부전(`scratched`)마가 추천 1위였다면 평가 제외(`excluded=true, reason=SCRATCHED`)
- 집계는 `HitRecord` 를 meet/주(week)/engineVersion 으로 GROUP BY. 요약은 1시간 캐시

## 4. 프리미엄 잠금 규칙 (서버 단일 판정)
| 리소스 | 무료 | 프리미엄 |
|---|---|---|
| RaceDetail.entries | rank ≤ `freeRankLimit`(기본 3): score만, factors null; rank > 3: score/factors null, locked=true | 전체 |
| Horse.recentRuns | 3건 | 10건 |
| Hits summary | 전체 수치 노출 | + /hits/races 상세 |
| Jockey/Trainer | 기본 | + 조합 성적 |
`freeRankLimit` 는 원격 플래그로 조정(전환율 실험).

## 5. 알림
| 이벤트 | 조건 | 대상 |
|---|---|---|
| ENTRIES_FIXED | 출마표 최초 확정 | prefs.notifyEntries, preferredMeet 일치 |
| RANK_UPDATED | entriesHash 변경 후 재예측, 추천 1위 변동 시에만 | prefs.notifyRankUpdate |
| PRE_RACE | 경주 30분 전 | prefs.notifyPreRace + 프리미엄 |
발송 큐: DB 테이블 `notification_outbox` → FCM 배치. MVP에서는 outbox 적재까지만 구현.

## 6. 백테스트 절차 (출시 게이트)
1. 과거 12개월 결과 데이터 적재 (`/admin/ingest/results` 를 날짜 루프)
2. 각 경주에 대해 "경주 전 시점" 데이터만으로 `RuleScoreEngineV1.score` 실행 (시간 누수 방지: `asOf` 파라미터)
3. `HitEvaluationService` 로 집계 → winRate ≥ 0.25, placeRate ≥ 0.45 확인
4. 미달 시 가중치 그리드 탐색(0.05 단위) → 최적 조합을 `v1.1-rule` 로 저장
