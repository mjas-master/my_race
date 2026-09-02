# my_race — 프로젝트 인수인계 문서 (다른 세션에서 이어서 개발하기)

> 이 문서 하나만 읽으면 다음 세션에서 바로 개발을 이어갈 수 있도록 작성했습니다.
> 작성 기준일: 2026-09-02

## 1. 프로젝트 한 줄 정의
한국마사회(KRA) 공공데이터를 수집·분석하여 **경주별 추천 순위와 근거 지표**를 보여주는 경마 정보 앱.
무료(출마표·결과·기본 순위) → 유료 구독(상세 근거·적중 이력·알림)의 프리미엄 모델. 배너 광고는 보조.

## 2. 문서 지도
| 파일 | 내용 | 이어서 개발할 때 언제 보나 |
|---|---|---|
| `00_README.md` | 이 문서. 전체 개요·규칙·현재 진행 상태 | 항상 먼저 |
| `01_requirements.md` | 요구사항, 범위(MVP/후속), 법적·정책 제약, 비기능 요구 | 기능 추가/변경 시 |
| `02_screens.html` | 화면 구성안 12개 (브라우저로 열기) | UI 작업 시 |
| `03_class_diagram.md` | 도메인 모델·백엔드 클래스·앱 상태 클래스 다이어그램(Mermaid) | 모델/DB 변경 시 |
| `04_api_spec.md` | REST API 정의서 + `04_openapi.yaml` | 프론트-백 연동 시 |
| `05_logic.md` | 데이터 수집·정제·예측 점수 산식·적중 검증 로직 | 예측/스케줄러 작업 시 |
| `06_dev_backlog.md` | 개발 대상 목록(체크리스트, 우선순위, 상태) | **매 세션 시작·종료 시 갱신** |

## 3. 저장소 구조
```
my_race/
├─ docs/                 설계 문서 (위 표)
├─ backend/              Kotlin + Spring Boot 3.3, Gradle KTS, JPA, PostgreSQL(운영)/H2(개발)
│  └─ src/main/kotlin/com/myrace/
│     ├─ config/         WebClient, Scheduling, CORS, 프로퍼티
│     ├─ domain/         JPA 엔티티 (Race, Entry, Horse, Jockey, Trainer, Prediction, HitRecord...)
│     ├─ repository/     Spring Data 리포지토리
│     ├─ external/       KRA 공공데이터 API 클라이언트 + 응답 DTO
│     ├─ service/        수집(Ingest), 조회, 적중 검증
│     ├─ prediction/     점수 산식(규칙 기반 v1) — 모델 교체 지점
│     ├─ scheduler/      주기 작업(출마표 수집, 결과 수집, 예측 재계산)
│     └─ api/            REST 컨트롤러 + 응답 DTO
└─ frontend/             Flutter 3.x, Riverpod, go_router, dio
   └─ lib/
      ├─ core/           theme, router, network(ApiClient)
      ├─ models/         API 응답 모델 (freezed 없이 수동 fromJson)
      ├─ providers/      Riverpod providers
      └─ features/       화면 단위 폴더
```

## 4. 실행 방법
### 백엔드
```bash
cd backend
# 공공데이터포털 "일반 인증키(Decoding)" 하나만 넣으면 실데이터 모드로 자동 전환(myrace.ingest.mock=auto)
export KRA_SERVICE_KEY=발급키
./gradlew bootRun            # H2 인메모리, http://localhost:8080
# 운영: SPRING_PROFILES_ACTIVE=prod, DB_URL/DB_USER/DB_PASSWORD 환경변수
```
- Swagger UI: `http://localhost:8080/swagger-ui.html`
- 키가 없으면 Mock 클라이언트가 8주치 샘플 데이터를 시드합니다. 키가 있으면 기동 직후 백그라운드로 과거 8주(`MYRACE_BACKFILL_WEEKS`) + 향후 7일을 자동 적재합니다.
- 실키 투입 후 첫 확인: `GET /v1/admin/kra/probe?meet=1` (X-Admin-Key) → 엔드포인트별 응답 수·필드명. 필드/경로가 다르면 `application.yml` 의 `myrace.kra.endpoints` 만 수정.
- 공공데이터포털에서 **경주별상세성적표(API227)** 는 필수, 경주계획표·출전표·경주마·기수·조교사 API는 있으면 사용/없으면 성적표 기반으로 자동 대체됩니다(활용신청은 각각 별도).

### 프론트엔드
```bash
cd frontend
flutter pub get
flutter run --dart-define=API_BASE_URL=http://10.0.2.2:8080   # Android 에뮬레이터
```

## 5. 코딩 규칙 (세션 간 일관성)
- 경마장 코드: `1`=서울, `2`=제주, `3`=부경 (KRA 공공데이터 `meet` 값 그대로 사용)
- 날짜: 서버 내부 `LocalDate`, API는 `yyyy-MM-dd` 문자열, KRA 원본 `yyyyMMdd` 는 external 계층에서만 존재
- 마번 게이트색은 프론트 `theme/gate_colors.dart` 단일 소스에서 관리
- 예측 로직은 `prediction/ScoreEngine.kt` 인터페이스 뒤에 두고, 버전 문자열(`v1-rule`)을 Prediction 테이블에 저장 → 모델 교체 시 적중률 비교 가능
- 표현 금지어(마사회 공인/추천/제휴 등)는 `01_requirements.md` §5 참조. UI 카피 작성 시 반드시 확인
- 성인 인증 미완료 상태에서는 어떤 API 응답도 캐시하지 않음

## 6. 현재 진행 상태 (2026-09-02)
- [x] 설계 문서 일체 (01~06)
- [x] 백엔드 골격: 엔티티·리포지토리·KRA 클라이언트·수집 서비스·규칙 기반 예측·REST API·스케줄러·Mock 데이터
- [x] 프론트 골격: 테마·라우터·API 클라이언트·모델·12개 화면 전부 UI 구현(S07 조교사는 S06 컴포넌트 공유, S10 구독은 UI만)
- [ ] 미구현: 인앱구독 실결제/영수증 검증, AdMob 연동, 푸시(FCM), 오프라인 캐시, 백테스트 하네스, ML 모델(v2), 실제 KRA API 필드 매핑 검증(실키 필요)
- [x] 2차: UI 애니메이션(스태거 등장·점수바·카운트업·펄스·쉬머·트랙 빈상태, 설정에서 OFF 가능), 과거조회(경주목록/결과 캘린더, `GET /races/dates`), 다크모드(시스템/라이트/다크), KRA 실API 자동전환·자동 백필·probe 진단
- ⚠ 이 세션 환경에는 Gradle/Flutter 툴체인이 없어 **컴파일을 실행하지 못했습니다.** 다음 세션 첫 작업: `./gradlew test`, `flutter analyze` 로 빌드 오류부터 잡을 것
- 다음 세션 착수 권장 순서: `06_dev_backlog.md` 의 P0 미완료 항목부터

## 7. 다른 세션에 주는 프롬프트 예시
> "my_race 프로젝트를 이어서 개발한다. docs/00_README.md 와 docs/06_dev_backlog.md 를 먼저 읽고, P0 중 '적중률 검증 API' 를 구현해라. 규칙은 00_README §5 를 따른다."
