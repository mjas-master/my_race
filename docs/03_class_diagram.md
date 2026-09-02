# 03. 클래스 다이어그램 — my_race

Mermaid 문법. GitHub / VS Code(Markdown Preview Mermaid) / mermaid.live 에서 렌더링.

## 1. 도메인 모델 (백엔드 JPA 엔티티)

```mermaid
classDiagram
  direction LR

  class Race {
    +Long id
    +Int meet          "1 서울 / 2 제주 / 3 부경"
    +LocalDate raceDate
    +Int raceNo
    +Int distance
    +String track      "모래/잔디"
    +String grade      "국6, 혼2 ..."
    +LocalTime startTime
    +RaceStatus status "PLANNED, ENTRIES_FIXED, FINISHED, CANCELED"
    +Instant updatedAt
    +naturalKey() String
  }

  class Entry {
    +Long id
    +Int gateNo        "마번"
    +Double weightCarried "부담중량"
    +Double horseWeight
    +Int? finishPos    "결과 반영 후"
    +String? finishTime
    +Double? winOdds
    +Double? placeOdds
  }

  class Horse {
    +Long id
    +String hrNo       "KRA 마필번호"
    +String name
    +String country
    +String sex
    +Int age
    +Int rating
    +Int careerStarts
    +Int careerWins
    +Int careerSeconds
  }

  class Jockey {
    +Long id
    +String jkNo
    +String name
    +Int meet
    +Int debutYear
  }

  class Trainer {
    +Long id
    +String trNo
    +String name
    +Int meet
  }

  class Prediction {
    +Long id
    +String engineVersion "v1-rule"
    +Instant computedAt
    +String inputSnapshotJson
  }

  class PredictionItem {
    +Long id
    +Int rank
    +Double score
    +String factorsJson  "근거 지표 map"
  }

  class HitRecord {
    +Long id
    +Boolean winHit      "추천1위 == 1착"
    +Boolean placeHit    "추천1위 in 1~2착"
    +Boolean showHit     "추천1위 in 1~3착"
    +Boolean top3Exact   "추천1~3 == 실제1~3 순서무관"
    +Instant evaluatedAt
  }

  class Subscription {
    +Long id
    +String deviceId
    +Platform platform   "ANDROID, IOS"
    +String productId
    +String purchaseToken
    +Instant expiresAt
    +Boolean active
  }

  class Device {
    +Long id
    +String deviceId
    +String? pushToken
    +Int preferredMeet
    +Boolean adultConfirmed
    +Instant adultConfirmedAt
    +NotifyPrefs prefs
  }

  Race "1" --> "*" Entry
  Entry "*" --> "1" Horse
  Entry "*" --> "1" Jockey
  Entry "*" --> "1" Trainer
  Race "1" --> "0..*" Prediction : 버전별
  Prediction "1" --> "*" PredictionItem
  PredictionItem "*" --> "1" Entry
  Prediction "1" --> "0..1" HitRecord
  Device "1" --> "0..*" Subscription
```

## 2. 백엔드 서비스 계층

```mermaid
classDiagram
  direction TB

  class KraApiClient {
    -WebClient web
    -String serviceKey
    +fetchRacePlan(meet, yyyyMM) List~KraRacePlanDto~
    +fetchEntries(meet, date) List~KraEntryDto~
    +fetchResults(meet, date) List~KraResultDto~
    +fetchHorse(hrNo) KraHorseDto
    +fetchJockey(jkNo) KraJockeyDto
  }
  class MockKraApiClient {
    +모든 메서드를 샘플 데이터로 응답
  }
  KraApiClient <|.. MockKraApiClient

  class IngestService {
    -KraApiClient kra
    -RaceRepository races
    -EntryRepository entries
    -HorseRepository horses
    -JockeyRepository jockeys
    -TrainerRepository trainers
    +ingestEntries(meet, date) IngestReport
    +ingestResults(meet, date) IngestReport
    -upsertHorse(dto) Horse
    -upsertJockey(dto) Jockey
    -upsertTrainer(dto) Trainer
  }

  class ScoreEngine {
    <<interface>>
    +version() String
    +score(ctx: RaceContext) List~ScoredEntry~
  }
  class RuleScoreEngineV1 {
    -FormFactor
    -DistanceFitFactor
    -JockeyFormFactor
    -TrainerFormFactor
    -WeightChangeFactor
    -RestDaysFactor
    +score(ctx) List~ScoredEntry~
  }
  ScoreEngine <|.. RuleScoreEngineV1

  class PredictionService {
    -ScoreEngine engine
    -HistoryQuery history
    -PredictionRepository predictions
    +computeForRace(raceId) Prediction
    +computeForDate(meet, date) Int
    +latestFor(raceId) Prediction?
  }

  class HitEvaluationService {
    -PredictionRepository predictions
    -HitRecordRepository hits
    +evaluateFinishedRaces(date) Int
    +summary(meet?, from, to) HitSummary
  }

  class RaceQueryService {
    +listRaces(meet, date) List~RaceSummary~
    +raceDetail(raceId, premium) RaceDetail
    +results(meet, date) List~RaceResult~
  }

  class HorseQueryService {
    +detail(hrNo, premium) HorseDetail
    +history(hrNo, limit) List~HorseRun~
  }
  class JockeyQueryService {
    +detail(jkNo) JockeyDetail
  }
  class TrainerQueryService {
    +detail(trNo) TrainerDetail
  }

  class EntitlementService {
    -SubscriptionRepository subs
    -StoreReceiptVerifier verifier
    +isPremium(deviceId) Boolean
    +registerPurchase(req) Subscription
  }

  class IngestScheduler {
    +fixEntriesJob()   "화·수·목 18:00"
    +resultsJob()      "금·토·일 10분 간격"
    +recomputeJob()    "출마표 변경 감지 시"
    +evaluateJob()     "결과 수집 후"
  }

  IngestService --> KraApiClient
  PredictionService --> ScoreEngine
  HitEvaluationService --> PredictionService
  IngestScheduler --> IngestService
  IngestScheduler --> PredictionService
  IngestScheduler --> HitEvaluationService
  RaceQueryService --> EntitlementService
  HorseQueryService --> EntitlementService
```

## 3. 프론트엔드 상태/모델 (Flutter · Riverpod)

```mermaid
classDiagram
  direction LR

  class ApiClient {
    -Dio dio
    -String baseUrl
    -String deviceId
    +get(path, query) Future~Map~
    +post(path, body) Future~Map~
  }

  class RaceRepository {
    +listRaces(meet, date) Future~List~RaceSummary~~
    +raceDetail(id) Future~RaceDetail~
    +results(meet, date) Future~List~RaceResult~~
    +hitSummary(meet, weeks) Future~HitSummary~
  }
  class EntityRepository {
    +horse(hrNo) Future~HorseDetail~
    +jockey(jkNo) Future~JockeyDetail~
    +trainer(trNo) Future~TrainerDetail~
  }
  class DeviceRepository {
    +register(prefs) Future~void~
    +updatePrefs(prefs) Future~void~
    +premiumStatus() Future~bool~
  }

  class AppSettings {
    +bool adultConfirmed
    +int preferredMeet
    +bool notifyEntries
    +bool notifyRankUpdate
    +bool notifyPreRace
  }

  class RaceSummary
  class RaceDetail
  class EntryView {
    +int rank
    +int gateNo
    +String horseName
    +String jockeyName
    +String trainerName
    +double weight
    +double? score
    +Map~String,String~? factors
    +bool locked
  }
  class RaceResult
  class HitSummary
  class HorseDetail
  class JockeyDetail

  class Providers {
    apiClientProvider
    settingsProvider : StateNotifier~AppSettings~
    raceListProvider(meet,date) : FutureProvider
    raceDetailProvider(id) : FutureProvider
    resultsProvider(meet,date) : FutureProvider
    hitSummaryProvider(meet,weeks) : FutureProvider
    horseProvider(hrNo) : FutureProvider
    jockeyProvider(jkNo) : FutureProvider
    premiumProvider : FutureProvider~bool~
  }

  RaceRepository --> ApiClient
  EntityRepository --> ApiClient
  DeviceRepository --> ApiClient
  RaceDetail "1" --> "*" EntryView
  Providers --> RaceRepository
  Providers --> EntityRepository
  Providers --> DeviceRepository
  Providers --> AppSettings
```

## 4. 화면-라우트 매핑
| 라우트 | 화면 | Provider |
|---|---|---|
| `/onboarding` | S01, S02 | settingsProvider |
| `/` | S03 경주 목록 | raceListProvider |
| `/race/:id` | S04 | raceDetailProvider |
| `/horse/:hrNo` | S05 | horseProvider |
| `/jockey/:jkNo` | S06 | jockeyProvider |
| `/trainer/:trNo` | S07 | trainerProvider |
| `/results` | S08 | resultsProvider |
| `/hits` | S09 | hitSummaryProvider |
| `/premium` | S10 (모달) | premiumProvider |
| `/settings` | S11 | settingsProvider |
| `/settings/legal` | S12 | — |
