import 'package:flutter/material.dart';
import 'package:flutter_localizations/flutter_localizations.dart';
import 'package:flutter_riverpod/flutter_riverpod.dart';
import 'package:intl/date_symbol_data_local.dart';
import 'package:shared_preferences/shared_preferences.dart';

import 'core/router/app_router.dart';
import 'core/theme/app_theme.dart';
import 'providers/settings_provider.dart';

Future<void> main() async {
  WidgetsFlutterBinding.ensureInitialized();
  await initializeDateFormatting('ko');
  final prefs = await SharedPreferences.getInstance();
  runApp(ProviderScope(
    overrides: [sharedPrefsProvider.overrideWithValue(prefs)],
    child: const MyRaceApp(),
  ));
}

class MyRaceApp extends ConsumerStatefulWidget {
  const MyRaceApp({super.key});
  @override
  ConsumerState<MyRaceApp> createState() => _MyRaceAppState();
}

/// 테마 모드(system/light/dark) + 시스템 밝기 변화를 관찰해 AppColors 팔레트를 전환한다.
class _MyRaceAppState extends ConsumerState<MyRaceApp> with WidgetsBindingObserver {
  @override
  void initState() { super.initState(); WidgetsBinding.instance.addObserver(this); }
  @override
  void dispose() { WidgetsBinding.instance.removeObserver(this); super.dispose(); }
  @override
  void didChangePlatformBrightness() => setState(() {});

  @override
  Widget build(BuildContext context) {
    final mode = ref.watch(settingsProvider.select((s) => s.themeMode));
    final platformDark = WidgetsBinding.instance.platformDispatcher.platformBrightness == Brightness.dark;
    final dark = switch (mode) { ThemeMode.dark => true, ThemeMode.light => false, ThemeMode.system => platformDark };
    AppColors.setDark(dark); // 하위 위젯이 build 에서 읽는 정적 팔레트
    final router = ref.watch(routerProvider);
    return MaterialApp.router(
      title: 'my_race',
      theme: AppTheme.light(),
      darkTheme: AppTheme.dark(),
      themeMode: mode,
      routerConfig: router,
      locale: const Locale('ko'),
      supportedLocales: const [Locale('ko'), Locale('en')],
      localizationsDelegates: GlobalMaterialLocalizations.delegates,
      debugShowCheckedModeBanner: false,
    );
  }
}
