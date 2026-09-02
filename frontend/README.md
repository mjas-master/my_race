# my_race frontend (Flutter)

```bash
flutter create . --platforms=android,ios --project-name my_race   # 플랫폼 폴더 생성(최초 1회)
flutter pub get
flutter run --dart-define=API_BASE_URL=http://10.0.2.2:8080        # Android 에뮬레이터 → 로컬 백엔드
```
- 개발 중 백엔드 mock 시드 디바이스를 쓰려면 `--dart-define=DEVICE_ID=dev-device`
- 구조/화면/규칙: ../docs/00_README.md, 02_screens.html, 03_class_diagram.md §3
