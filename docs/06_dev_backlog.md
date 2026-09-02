# 06. 개발 대상 목록 (Backlog) — my_race

상태: `[x]` 완료 · `[~]` 골격만 · `[ ]` 미착수
우선순위: P0 출시 필수 · P1 출시 직후 · P2 후속
**세션 시작 시 이 파일을 열고, 종료 시 상태와 날짜를 갱신할 것.**

## A. 백엔드 (Kotlin / Spring Boot)
| # | P | 항목 | 상태 | 파일 | 메모 |
|---|---|---|---|---|---|
| A01 | P0 | 프로젝트 골격(Gradle, 프로파일, H2/Postgres) | [x] | build.gradle.kts, application.yml | |
| A02 | P0 | 도메인 엔티티 8종 + 리포지토리 | [x] | domain/*, repository/* | |
| A03 | P0 | KRA API 클라이언트(WebClient) + DTO | [~] | external/* | API227 성적표 필드 매핑 완료·별칭 처리·probe 진단·자동 백필. 나머지 API는 실키로 확인 |
| A04 | P0 | Mock 클라이언트(샘플 데이터) | [x] | external/MockKraApiClient.kt | |
| A05 | P0 | IngestService: 출마표 업서트 | [x] | service/IngestService.kt | |
| A06 | P0 | IngestService: 결과 업서트 | [x] | service/IngestService.kt | |
| A07 | P0 | 변경 감지(entriesHash) → 재예측 | [~] | service/IngestService.kt | 해시 계산 구현, 알림 outbox 미연결 |
| A08 | P0 | RuleScoreEngineV1 (7팩터) | [x] | prediction/* | 가중치 application.yml 외부화 |
| A09 | P0 | PredictionService (계산·저장·최신조회) | [x] | service/PredictionService.kt | |
| A10 | P0 | HitEvaluationService + summary | [x] | service/HitEvaluationService.kt | |
| A11 | P0 | REST: /races, /races/{id}, /results | [x] | api/controller/RaceController.kt | |
| A12 | P0 | REST: /horses, /jockeys, /trainers | [~] | api/controller/EntityController.kt | trainer 조합성적 미구현 |
| A13 | P0 | REST: /hits/summary, /hits/races | [x] | api/controller/HitController.kt | |
| A14 | P0 | REST: /devices, 성인확인 인터셉터 | [x] | api/controller/DeviceController.kt, config/DeviceInterceptor.kt | |
| A15 | P0 | 프리미엄 잠금(EntitlementService) | [x] | service/EntitlementService.kt | 영수증 검증 stub |
| A16 | P0 | 스케줄러 4종 | [x] | scheduler/IngestScheduler.kt | 크론 05_logic §1.1 |
| A17 | P0 | 전역 예외 → 에러 포맷 | [x] | api/ApiExceptionHandler.kt | |
| A18 | P0 | Swagger(springdoc) | [x] | build.gradle.kts | |
| A19 | P0 | 백테스트 하네스(asOf 시간누수 방지) | [ ] | test/BacktestTest.kt | **출시 게이트** |
| A20 | P1 | Google Play / App Store 영수증 실검증 | [ ] | service/StoreReceiptVerifier.kt | |
| A21 | P1 | notification_outbox + FCM 발송 | [ ] | service/NotificationService.kt | |
| A22 | P1 | 원격 플래그(adsEnabled, freeRankLimit) | [~] | config/FeatureFlags.kt | 프로퍼티만 |
| A23 | P1 | 응답 캐시(Caffeine) TTL | [ ] | config/CacheConfig.kt | 04_api_spec §7 |
| A24 | P1 | Flyway 마이그레이션 | [ ] | resources/db/migration | 현재 ddl-auto |
| A25 | P2 | ML 엔진 v2 (LightGBM/ONNX) | [ ] | prediction/MlScoreEngineV2.kt | |
| A26 | P2 | 관리자 API 인증(X-Admin-Key) | [~] | api/controller/AdminController.kt | 키 비교만 |

## B. 프론트엔드 (Flutter)
| # | P | 항목 | 상태 | 파일 | 메모 |
|---|---|---|---|---|---|
| B01 | P0 | 프로젝트 골격, pubspec, 테마(게이트색) | [x] | pubspec.yaml, core/theme/* | |
| B02 | P0 | ApiClient(dio, X-Device-Id, 에러 매핑) | [x] | core/network/api_client.dart | |
| B03 | P0 | 모델 클래스(fromJson) | [x] | models/* | |
| B04 | P0 | Riverpod providers + 설정 영속화(shared_preferences) | [x] | providers/* | |
| B05 | P0 | go_router + 하단 탭 셸 | [x] | core/router/app_router.dart | |
| B06 | P0 | S01 성인확인·면책 / S02 초기설정 | [x] | features/onboarding/* | |
| B07 | P0 | S03 경주 목록(경마장×날짜 필터) | [x] | features/races/race_list_page.dart | |
| B08 | P0 | S04 경주 상세(추천순위·잠금카드·출마표 토글) | [x] | features/race_detail/* | |
| B09 | P0 | S05 경주마 상세(근거·착순 막대·이력) | [x] | features/horse/horse_page.dart | |
| B10 | P0 | S06 기수 상세 | [x] | features/jockey/jockey_page.dart | |
| B11 | P1 | S07 조교사 상세 | [~] | features/jockey/jockey_page.dart (PersonPage) | 조합 성적은 서버 A12 이후 |
| B12 | P0 | S08 경주 결과 | [x] | features/results/results_page.dart | |
| B13 | P0 | S09 적중 이력 | [x] | features/results/hits_page.dart | |
| B14 | P0 | S10 프리미엄 모달(UI만) | [x] | features/settings/premium_sheet.dart | in_app_purchase 미연결 |
| B15 | P0 | S11 설정 / S12 출처·면책 | [x] | features/settings/* | |
| B16 | P0 | 오프라인 캐시(마지막 응답 저장) | [ ] | core/network/cache.dart | |
| B17 | P1 | in_app_purchase 연동 + verify API | [ ] | providers/premium_provider.dart | |
| B18 | P1 | AdMob 배너(무료 사용자, 원격 플래그) | [ ] | features/common/ad_banner.dart | 성인 콘텐츠 설정 |
| B19 | P1 | FCM 푸시 수신·딥링크 | [ ] | core/push/ | |
| B20 | P1 | 위젯 테스트(경주 상세 잠금 규칙) | [ ] | test/ | |
| B21 | P2 | 다크 모드(시스템 따라가기/라이트/다크) | [x] | core/theme, main.dart | 설정 > 화면 모드 |
| B22 | P1 | UI 애니메이션(등장 스태거·점수바·카운트업·펄스·쉬머) | [x] | core/anim/animations.dart | 설정에서 OFF 가능 |
| B23 | P1 | 과거조회(캘린더, 경주 있는 날만 활성) | [x] | races/results 페이지, /races/dates | |

## C. 운영·정책
| # | P | 항목 | 상태 | 메모 |
|---|---|---|---|---|
| C01 | P0 | 공공데이터포털 KRA API 활용 신청·키 발급 | [ ] | 운영 키는 트래픽 상향 신청 |
| C02 | P0 | 이용약관·개인정보 처리방침 문안 | [ ] | 성인 확인, 기기식별자 수집 명시 |
| C03 | P0 | 스토어 연령등급(성인) 및 설명문 금지어 검수 | [ ] | 01_requirements §5 |
| C04 | P0 | 12개월 백테스트 결과 리포트(출시 게이트) | [ ] | A19 선행 |
| C05 | P1 | AdMob 계정 정책 검토·거절 시 대응 플로우 | [ ] | 원격 플래그로 즉시 OFF |
| C06 | P1 | 모니터링(업스트림 실패율, p95) | [ ] | Actuator + 알림 |

## D. 다음 세션 착수 순서(권장)
0. **빌드 검증**: `backend/./gradlew test`, `frontend/flutter analyze` — 이 세션은 툴체인 부재로 미실행
1. A19 백테스트 하네스 → C04 리포트 (사업 지속 여부 결정)
2. A03 실키로 KRA 필드 매핑 검증 (Mock → 실데이터)
3. B16 오프라인 캐시, B11 조교사
4. A20/B17 구독 실연동 → A21/B19 푸시
