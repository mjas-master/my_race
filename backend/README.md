# my_race backend (Kotlin / Spring Boot)

```bash
./gradlew bootRun                 # mock 데이터로 기동 (KRA 키 불필요)
KRA_SERVICE_KEY='일반인증키(Decoding)' ./gradlew bootRun   # 키만 넣으면 자동으로 실데이터 모드
```

## 실 KRA 데이터 연결 절차 (키만 있으면 됨)
1. 공공데이터포털에서 아래 API 활용신청(개발계정 자동승인, 일 3,000건):
   - 한국마사회_경주별상세성적표 (15089492) ✔ 필수 — 이것 하나로 출전·결과·기수/조교사 이력 전부 확보
   - 한국마사회 경주계획표 (15056499) — 미래 경주 일정
   - 경주마/기수/조교사 성적 정보 — 누적 통산 성적(선택)
2. `KRA_SERVICE_KEY` 환경변수에 **일반 인증키(Decoding)** 를 넣고 기동. `myrace.ingest.mock=auto` 가 실API로 전환.
3. 점검: `curl -H 'X-Admin-Key: dev-admin' 'localhost:8080/v1/admin/kra/probe?meet=1&date=2026-08-30'`
   → 엔드포인트별 ok/건수/응답 필드명이 나옴. `entrySheet`·`racePlan` 등이 실패하면 `application.yml > myrace.kra.endpoints` 경로만 고치면 됨
   (raceResult 는 필드까지 검증된 경로. 출전표가 없어도 raceResult 로 자동 대체되어 서비스는 동작).
4. 과거 적재(백테스트 데이터): `POST /v1/admin/ingest/results?meet=1&date=YYYY-MM-DD` 를 지난 1년 경마일(금·토·일)에 대해 반복
   → `POST /v1/admin/predict` → `POST /v1/admin/evaluate` → `GET /v1/hits/summary?weeks=52`
5. 기수/조교사 누적성적: `POST /v1/admin/ingest/persons?meet=1` (매주 월 06:30 자동)
- Swagger: http://localhost:8080/swagger-ui.html
- H2 콘솔: http://localhost:8080/h2-console (jdbc:h2:mem:myrace)
- 개발용 헤더: `X-Device-Id: dev-device` (성인확인 완료 상태로 시드됨)

```bash
curl -H 'X-Device-Id: dev-device' 'localhost:8080/v1/races?meet=1&date=2026-09-04'
curl -H 'X-Device-Id: dev-device' 'localhost:8080/v1/hits/summary?weeks=8'
curl -X POST -H 'X-Admin-Key: dev-admin' 'localhost:8080/v1/admin/predict?meet=1&date=2026-09-04'
```
Gradle Wrapper 는 저장소에 포함하지 않았습니다. `gradle wrapper --gradle-version 8.9` 로 생성하세요.
문서: ../docs/
