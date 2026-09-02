# 04. API 정의서 — my_race Backend

Base URL: `https://api.myrace.app/v1` (개발 `http://localhost:8080/v1`)
형식: JSON, UTF-8. 날짜 `yyyy-MM-dd`, 시각 ISO-8601.
공통 헤더: `X-Device-Id: <uuid>` (필수. 성인 확인·프리미엄 판정·알림에 사용)
공통 에러: `{ "code": "RACE_NOT_FOUND", "message": "...", "traceId": "..." }`

| HTTP | code |
|---|---|
| 400 | INVALID_PARAM |
| 401 | DEVICE_REQUIRED (헤더 누락) |
| 403 | ADULT_NOT_CONFIRMED / PREMIUM_REQUIRED |
| 404 | RACE_NOT_FOUND, HORSE_NOT_FOUND … |
| 503 | UPSTREAM_UNAVAILABLE (KRA API 장애) |

## 1. 디바이스 / 인증
### POST /devices
기기 등록 또는 갱신(idempotent).
```json
// req
{ "deviceId":"uuid", "platform":"ANDROID", "adultConfirmed":true,
  "preferredMeet":1, "pushToken":"fcm...", 
  "prefs":{"notifyEntries":true,"notifyRankUpdate":true,"notifyPreRace":false} }
// 200
{ "deviceId":"uuid", "premium":false, "adultConfirmedAt":"2026-09-02T09:00:00Z" }
```
### GET /devices/me
`{ "deviceId", "premium", "premiumUntil", "preferredMeet", "prefs" }`

## 2. 경주
### GET /races?meet=1&date=2026-09-04
```json
{ "meet":1, "date":"2026-09-04", "updatedAt":"...",
  "races":[
    { "id":1201, "raceNo":1, "distance":1200, "track":"모래", "grade":"국6",
      "startTime":"11:10", "status":"ENTRIES_FIXED", "entryCount":12,
      "topPick": { "gateNo":5, "horseName":"천년의빛", "score":82.4, "hrNo":"0041234" } }
  ]}
```
`meet` 생략 시 전체 경마장. `status=PLANNED` 이면 `topPick=null`.

### GET /races/dates?meet=1&from=2025-09-01&to=2026-09-09
과거조회 캘린더용. 경주가 존재하는 날짜 목록(기본: 최근 1년 + 향후 1주).
```json
{ "meet": 1, "from": "2025-09-01", "to": "2026-09-09", "dates": ["2025-09-05", "2025-09-06", "..."] }
```

### GET /races/{id}
프리미엄 여부에 따라 `entries[].score/factors` 잠금.
```json
{ "id":1201, "meet":1, "meetName":"서울", "date":"2026-09-04", "raceNo":1,
  "distance":1200, "track":"모래", "grade":"국6", "startTime":"11:10",
  "status":"ENTRIES_FIXED", "engineVersion":"v1-rule", "computedAt":"...",
  "premium":false, "freeRankLimit":3,
  "entries":[
    { "rank":1, "gateNo":5, "hrNo":"0041234", "horseName":"천년의빛",
      "jkNo":"080012", "jockeyName":"문세영", "trNo":"0201", "trainerName":"김영관",
      "weightCarried":55.0, "horseWeight":478, "score":82.4,
      "factors": { "recentForm":"최근 3전 평균 2.3위", "distanceFit":"1200m 4전 2승",
                   "jockeyForm":"문세영 최근 30일 승률 18%", "weightChange":"-1kg",
                   "trainerForm":"김영관 승률 14%", "restDays":"휴양 13일" },
      "locked":false,
      "result": null },
    { "rank":4, "gateNo":3, "hrNo":"...", "horseName":"새벽바람", "...":"...",
      "score":null, "factors":null, "locked":true }
  ],
  "disclaimer":"예측은 참고 정보이며 결과를 보장하지 않습니다." }
```
`result` 는 FINISHED 시 `{ "finishPos":1, "finishTime":"1:14.0", "winOdds":2.1, "placeOdds":1.3 }`.

### GET /races/{id}/entries?order=gate
출마표 원본 순서(마번순). 응답 구조 동일, 점수 필드는 위와 같은 잠금 규칙.

## 3. 결과·적중
### GET /results?meet=1&date=2026-08-31
```json
{ "meet":1, "date":"2026-08-31",
  "races":[
    { "id":1188, "raceNo":1, "distance":1200,
      "hit": { "winHit":true, "placeHit":true, "showHit":true },
      "ourTopPick": { "gateNo":5, "horseName":"천년의빛" },
      "top3":[
        { "finishPos":1, "gateNo":5, "horseName":"천년의빛", "finishTime":"1:14.0", "winOdds":2.1 },
        { "finishPos":2, "gateNo":1, "horseName":"글로벌히트", "placeOdds":4.8 },
        { "finishPos":3, "gateNo":3, "horseName":"새벽바람" } ] } ] }
```
### GET /hits/summary?meet=&weeks=4
```json
{ "from":"2026-08-03", "to":"2026-08-31", "engineVersion":"v1-rule", "races":131,
  "winRate":0.275, "placeRate":0.482, "showRate":0.610,
  "byMeet":[ {"meet":1,"meetName":"서울","races":72,"winRate":0.291}, ... ],
  "weekly":[ {"weekStart":"2026-08-03","races":31,"winRate":0.258}, ... ],
  "detailLocked":true }
```
### GET /hits/races?meet=&from=&to= (프리미엄)
경주별 상세 이력 목록. 무료 → 403 PREMIUM_REQUIRED.

## 4. 경주마 / 기수 / 조교사
### GET /horses/{hrNo}
```json
{ "hrNo":"0041234", "name":"천년의빛", "country":"한국", "sex":"수", "age":4, "rating":48,
  "career": {"starts":21,"wins":5,"seconds":4,"thirds":3},
  "byDistance":[ {"distance":1200,"starts":4,"wins":2}, ... ],
  "recentRuns":[ { "date":"2026-08-22","meet":1,"raceNo":3,"distance":1200,"finishPos":1,
                   "finishTime":"1:14.2","jockeyName":"문세영","jkNo":"080012","winOdds":1.8 } ],
  "runsLocked":false, "runsLimit":10 }
```
무료는 `recentRuns` 3건, 프리미엄 10건(`runsLimit`로 표기).

### GET /jockeys/{jkNo}
```json
{ "jkNo":"080012","name":"문세영","meet":1,"debutYear":2008,
  "season":{"starts":312,"wins":57,"winRate":0.184,"placeRate":0.371},
  "last30d":{"starts":46,"wins":9},
  "weeklyWinRate":[{"weekStart":"2026-06-08","rate":0.15}, ...12개],
  "thisWeekEntries":[{"raceId":1201,"raceNo":1,"gateNo":5,"horseName":"천년의빛","ourRank":1}] }
```
### GET /trainers/{trNo}
기수와 동일 구조 + `horsesInCare`, `topJockeys[]` (프리미엄).

## 5. 구독
### POST /subscriptions/verify
```json
// req
{ "platform":"ANDROID", "productId":"premium_monthly", "purchaseToken":"..." }
// 200
{ "active":true, "expiresAt":"2026-10-02T09:00:00Z", "productId":"premium_monthly" }
```
서버는 Google Play Developer API / App Store Server API 로 검증(MVP에서는 stub).

## 6. 운영(내부, `X-Admin-Key`)
| 메서드 | 경로 | 설명 |
|---|---|---|
| POST | /admin/ingest/entries?meet=1&date= | 출마표 즉시 수집 |
| POST | /admin/ingest/results?meet=1&date= | 결과 즉시 수집 |
| POST | /admin/predict?meet=1&date= | 예측 재계산 |
| POST | /admin/evaluate?date= | 적중 평가 |
| GET  | /admin/health/upstream | KRA API 상태 |
| GET  | /admin/flags | 원격 플래그(adsEnabled, freeRankLimit) |

## 7. 캐시 정책
| 엔드포인트 | TTL |
|---|---|
| GET /races, /races/{id} | 60s (경주 당일), 10m (그 외) |
| GET /results | 5m |
| GET /hits/summary | 1h |
| GET /horses, /jockeys, /trainers | 6h |

OpenAPI 3.0 파일: `04_openapi.yaml`
