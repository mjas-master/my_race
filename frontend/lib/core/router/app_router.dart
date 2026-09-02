import 'package:flutter/material.dart';
import 'package:flutter_riverpod/flutter_riverpod.dart';
import 'package:go_router/go_router.dart';

import '../../features/horse/horse_page.dart';
import '../../features/jockey/jockey_page.dart';
import '../../features/onboarding/onboarding_pages.dart';
import '../../features/race_detail/race_detail_page.dart';
import '../../features/races/race_list_page.dart';
import '../../features/results/hits_page.dart';
import '../../features/results/results_page.dart';
import '../../features/settings/legal_page.dart';
import '../../features/settings/settings_page.dart';
import '../../providers/settings_provider.dart';

/// 테마(라이트/다크) 전환 시 페이지가 다시 build 되도록 Theme 의존성을 걸어준다.
/// (AppColors 는 정적 팔레트라 Theme 변경만으로는 하위가 자동 리빌드되지 않음)
Widget themed(Widget Function() build) => Builder(builder: (c) { Theme.of(c); return build(); });

/// 03_class_diagram.md §4 라우트 매핑.
final routerProvider = Provider<GoRouter>((ref) {
  final onboarded = ref.watch(settingsProvider.select((s) => s.onboarded && s.adultConfirmed));
  return GoRouter(
    initialLocation: onboarded ? '/' : '/onboarding',
    redirect: (_, state) => (!onboarded && !state.uri.path.startsWith('/onboarding') && state.uri.path != '/legal') ? '/onboarding' : null,
    routes: [
      GoRoute(path: '/onboarding', builder: (_, __) => themed(() => AdultGatePage()), routes: [
        GoRoute(path: 'setup', builder: (_, __) => themed(() => SetupPage())),
      ]),
      StatefulShellRoute.indexedStack(
        builder: (_, __, shell) => themed(() => _Shell(shell)),
        branches: [
          StatefulShellBranch(routes: [GoRoute(path: '/', builder: (_, __) => themed(() => RaceListPage()))]),
          StatefulShellBranch(routes: [GoRoute(path: '/results', builder: (_, __) => themed(() => ResultsPage()))]),
          StatefulShellBranch(routes: [GoRoute(path: '/hits', builder: (_, __) => themed(() => HitsPage()))]),
          StatefulShellBranch(routes: [GoRoute(path: '/settings', builder: (_, __) => themed(() => SettingsPage()), routes: [
            GoRoute(path: 'legal', builder: (_, __) => themed(() => LegalPage())),
          ])]),
        ],
      ),
      GoRoute(path: '/legal', builder: (_, __) => themed(() => LegalPage())),
      GoRoute(path: '/race/:id', builder: (_, s) => themed(() => RaceDetailPage(int.parse(s.pathParameters['id']!)))),
      GoRoute(path: '/horse/:hrNo', builder: (_, s) => themed(() => HorsePage(s.pathParameters['hrNo']!))),
      GoRoute(path: '/jockey/:no', builder: (_, s) => themed(() => PersonPage(s.pathParameters['no']!, isJockey: true))),
      GoRoute(path: '/trainer/:no', builder: (_, s) => themed(() => PersonPage(s.pathParameters['no']!, isJockey: false))),
    ],
  );
});

class _Shell extends StatelessWidget {
  final StatefulNavigationShell shell;
  const _Shell(this.shell);

  @override
  Widget build(BuildContext context) => Scaffold(
        body: shell,
        bottomNavigationBar: NavigationBar(
          selectedIndex: shell.currentIndex,
          onDestinationSelected: (i) => shell.goBranch(i, initialLocation: i == shell.currentIndex),
          destinations: const [
            NavigationDestination(icon: Icon(Icons.flag_outlined), selectedIcon: Icon(Icons.flag), label: '경주'),
            NavigationDestination(icon: Icon(Icons.emoji_events_outlined), selectedIcon: Icon(Icons.emoji_events), label: '결과'),
            NavigationDestination(icon: Icon(Icons.track_changes_outlined), selectedIcon: Icon(Icons.track_changes), label: '적중'),
            NavigationDestination(icon: Icon(Icons.settings_outlined), selectedIcon: Icon(Icons.settings), label: '설정'),
          ],
        ),
      );
}
