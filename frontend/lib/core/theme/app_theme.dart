import 'package:flutter/cupertino.dart' show CupertinoPageTransitionsBuilder;
import 'package:flutter/material.dart';

/// 02_screens.html 토큰. 라이트/다크 팔레트 두 벌을 두고
/// [AppColors.setDark] 로 현재 팔레트를 전환한다(MyRaceApp 이 테마 모드·시스템 밝기 변화 시 호출).
class _Palette {
  final Color bg,
      paper,
      ink,
      ink2,
      ink3,
      turf,
      turf2,
      tan,
      tan2,
      red,
      red2,
      line,
      shimmer;
  const _Palette({
    required this.bg,
    required this.paper,
    required this.ink,
    required this.ink2,
    required this.ink3,
    required this.turf,
    required this.turf2,
    required this.tan,
    required this.tan2,
    required this.red,
    required this.red2,
    required this.line,
    required this.shimmer,
  });

  static const light = _Palette(
    bg: Color(0xFFEEF1EC),
    paper: Color(0xFFFFFFFF),
    ink: Color(0xFF17251D),
    ink2: Color(0xFF4A5A51),
    ink3: Color(0xFF8A9690),
    turf: Color(0xFF2B6A47),
    turf2: Color(0xFFE3EFE7),
    tan: Color(0xFFB98F5E),
    tan2: Color(0xFFF4ECDF),
    red: Color(0xFFC0392B),
    red2: Color(0xFFF6E6E4),
    line: Color(0xFFD9DFD9),
    shimmer: Color(0xFFF3F5F2),
  );

  /// 나이트: 잔디 위 야간경마 톤. 순검정 대신 짙은 녹회색.
  static const dark = _Palette(
    bg: Color(0xFF0E1512),
    paper: Color(0xFF161F1A),
    ink: Color(0xFFEAF0EB),
    ink2: Color(0xFFB4C0B8),
    ink3: Color(0xFF7C8A82),
    turf: Color(0xFF6FBF8E),
    turf2: Color(0xFF1E332A),
    tan: Color(0xFFD9B27E),
    tan2: Color(0xFF2E2719),
    red: Color(0xFFE8705F),
    red2: Color(0xFF3A1F1B),
    line: Color(0xFF283530),
    shimmer: Color(0xFF1E2A24),
  );
}

class AppColors {
  static _Palette _p = _Palette.light;
  static bool get isDark => identical(_p, _Palette.dark);
  static void setDark(bool dark) => _p = dark ? _Palette.dark : _Palette.light;

  static Color get bg => _p.bg;
  static Color get paper => _p.paper;
  static Color get ink => _p.ink;
  static Color get ink2 => _p.ink2;
  static Color get ink3 => _p.ink3;
  static Color get turf => _p.turf;
  static Color get turf2 => _p.turf2;
  static Color get tan => _p.tan;
  static Color get tan2 => _p.tan2;
  static Color get red => _p.red;
  static Color get red2 => _p.red2;
  static Color get line => _p.line;
  static Color get shimmer => _p.shimmer;
}

class AppTheme {
  static ThemeData light() => _build(_Palette.light, Brightness.light);
  static ThemeData dark() => _build(_Palette.dark, Brightness.dark);

  static ThemeData _build(_Palette c, Brightness b) {
    final base = ThemeData(useMaterial3: true, brightness: b);
    final scheme = b == Brightness.light
        ? ColorScheme.light(
            primary: c.turf,
            onPrimary: Colors.white,
            secondary: c.tan,
            surface: c.paper,
            onSurface: c.ink,
            error: c.red)
        : ColorScheme.dark(
            primary: c.turf,
            onPrimary: c.bg,
            secondary: c.tan,
            surface: c.paper,
            onSurface: c.ink,
            error: c.red);
    return base.copyWith(
      scaffoldBackgroundColor: c.paper,
      colorScheme: scheme,
      appBarTheme: AppBarTheme(
        backgroundColor: c.paper,
        foregroundColor: c.ink,
        elevation: 0,
        scrolledUnderElevation: 0,
        titleTextStyle: TextStyle(
            fontSize: 17,
            fontWeight: FontWeight.w700,
            color: c.ink,
            letterSpacing: -0.2),
      ),
      dividerColor: c.line,
      textTheme: base.textTheme.apply(
          bodyColor: c.ink,
          displayColor: c.ink,
          fontFamilyFallback: const ['Noto Sans KR']),
      chipTheme: base.chipTheme.copyWith(
        side: BorderSide(color: c.line),
        backgroundColor: c.paper,
        selectedColor: c.ink,
        labelStyle: const TextStyle(fontSize: 12),
        shape: const StadiumBorder(),
        showCheckmark: false,
      ),
      filledButtonTheme: FilledButtonThemeData(
          style: FilledButton.styleFrom(
              backgroundColor: c.turf,
              foregroundColor: b == Brightness.light ? Colors.white : c.bg,
              minimumSize: const Size.fromHeight(48),
              shape: RoundedRectangleBorder(
                  borderRadius: BorderRadius.circular(12)),
              textStyle:
                  const TextStyle(fontSize: 14, fontWeight: FontWeight.w700))),
      navigationBarTheme: NavigationBarThemeData(
        backgroundColor: c.paper,
        indicatorColor: c.turf2,
        height: 64,
        labelTextStyle: WidgetStateProperty.resolveWith((s) => TextStyle(
            fontSize: 11,
            fontWeight: s.contains(WidgetState.selected)
                ? FontWeight.w700
                : FontWeight.w500,
            color: s.contains(WidgetState.selected) ? c.turf : c.ink3)),
      ),
      bottomSheetTheme: BottomSheetThemeData(backgroundColor: c.paper),
      dialogTheme: DialogThemeData(backgroundColor: c.paper),
      datePickerTheme: DatePickerThemeData(
          backgroundColor: c.paper, headerBackgroundColor: c.turf2),
      // 화면 전환: 페이드-슬라이드(iOS는 기본 Cupertino 유지)
      pageTransitionsTheme: const PageTransitionsTheme(builders: {
        TargetPlatform.android: FadeUpwardsPageTransitionsBuilder(),
        TargetPlatform.iOS: CupertinoPageTransitionsBuilder(),
      }),
    );
  }
}

class AppText {
  static const big = TextStyle(
      fontSize: 32, fontWeight: FontWeight.w900, letterSpacing: -1, height: 1);
  static const title = TextStyle(fontSize: 17, fontWeight: FontWeight.w700);
  static const name = TextStyle(fontSize: 14, fontWeight: FontWeight.w500);
  static TextStyle get meta => TextStyle(fontSize: 11, color: AppColors.ink3);
  static TextStyle get label => TextStyle(fontSize: 11, color: AppColors.ink3);
  static TextStyle get score => TextStyle(
      fontSize: 13,
      fontWeight: FontWeight.w700,
      color: AppColors.turf,
      fontFeatures: const [FontFeature.tabularFigures()]);
}
