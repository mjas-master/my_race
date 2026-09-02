import 'package:flutter/material.dart';
import 'package:flutter_riverpod/flutter_riverpod.dart';
import 'package:go_router/go_router.dart';
import 'package:url_launcher/url_launcher.dart';

import '../../core/theme/app_theme.dart';
import '../../models/models.dart';
import '../../providers/data_providers.dart';
import '../../providers/settings_provider.dart';
import '../common/widgets.dart';
import 'premium_sheet.dart';

/// S11 설정
class SettingsPage extends ConsumerWidget {
  const SettingsPage({super.key});

  @override
  Widget build(BuildContext context, WidgetRef ref) {
    final s = ref.watch(settingsProvider);
    final premium = ref.watch(premiumProvider);
    final n = ref.read(settingsProvider.notifier);

    Future<void> save(AppSettings ns) async { await n.update(ns); ref.invalidate(deviceRegisterProvider); }

    return Scaffold(
      appBar: AppBar(title: const Text('설정')),
      body: ListView(padding: const EdgeInsets.all(16), children: [
        Card2(color: premium ? AppColors.turf2 : AppColors.tan2, border: premium ? AppColors.turf : AppColors.tan,
          onTap: premium ? null : () => showPremiumSheet(context),
          child: Row(children: [
            Expanded(child: Text(premium ? '프리미엄 플랜' : '무료 플랜', style: AppText.name)),
            Text(premium ? '이용 중' : '프리미엄 보기', style: TextStyle(fontSize: 12, fontWeight: FontWeight.w500, color: premium ? AppColors.turf : AppColors.tan)),
          ])),
        const SizedBox(height: 8),
        _row('기본 경마장', trailing: DropdownButton<int>(
          value: s.preferredMeet, underline: SizedBox(), isDense: true, style: AppText.meta.copyWith(color: AppColors.ink),
          items: [1, 3, 2].map((m) => DropdownMenuItem(value: m, child: Text(meetName(m)))).toList(),
          onChanged: (m) => m == null ? null : save(s.copyWith(preferredMeet: m)))),
        _row('화면 모드', trailing: SegmentedButton<ThemeMode>(
          showSelectedIcon: false, style: const ButtonStyle(visualDensity: VisualDensity.compact),
          segments: const [
            ButtonSegment(value: ThemeMode.system, label: Text('시스템', style: TextStyle(fontSize: 11))),
            ButtonSegment(value: ThemeMode.light, label: Text('라이트', style: TextStyle(fontSize: 11))),
            ButtonSegment(value: ThemeMode.dark, label: Text('다크', style: TextStyle(fontSize: 11))),
          ],
          selected: {s.themeMode}, onSelectionChanged: (v) => save(s.copyWith(themeMode: v.first)))),
        _row('애니메이션 효과', trailing: Switch(value: s.animations, activeColor: AppColors.turf, onChanged: (v) => save(s.copyWith(animations: v)))),
        _row('출마표 확정 알림', trailing: Switch(value: s.notifyEntries, activeColor: AppColors.turf, onChanged: (v) => save(s.copyWith(notifyEntries: v)))),
        _row('추천 순위 갱신 알림', trailing: Switch(value: s.notifyRankUpdate, activeColor: AppColors.turf, onChanged: (v) => save(s.copyWith(notifyRankUpdate: v)))),
        _row('경주 30분 전 알림', trailing: Switch(value: s.notifyPreRace, activeColor: AppColors.turf,
            onChanged: premium ? (v) => save(s.copyWith(notifyPreRace: v)) : (_) => showPremiumSheet(context))),
        _row('구독 관리 · 복원', onTap: () => showPremiumSheet(context)),
        _row('문의하기', onTap: () => launchUrl(Uri.parse('mailto:support@myrace.app?subject=my_race 문의'))),
        _row('이용약관 · 개인정보 처리방침', onTap: () => context.push('/settings/legal')),
        _row('데이터 출처', value: '공공데이터포털', onTap: () => launchUrl(Uri.parse('https://www.data.go.kr'), mode: LaunchMode.externalApplication)),
        _row('앱 버전', value: '1.0.0 (1)'),
        const SizedBox(height: 12),
        Text('기기 ID ${s.deviceId.substring(0, 8)}…', style: AppText.label),
      ]),
    );
  }

  Widget _row(String title, {String? value, Widget? trailing, VoidCallback? onTap}) => InkWell(
        onTap: onTap,
        child: Container(
          padding: const EdgeInsets.symmetric(vertical: 12),
          decoration: BoxDecoration(border: Border(bottom: BorderSide(color: AppColors.line))),
          child: Row(children: [
            Expanded(child: Text(title, style: const TextStyle(fontSize: 14))),
            if (value != null) Text(value, style: AppText.meta),
            if (trailing != null) trailing else if (onTap != null) Padding(padding: EdgeInsets.only(left: 6), child: Icon(Icons.chevron_right, size: 16, color: AppColors.ink3)),
          ]),
        ),
      );
}
