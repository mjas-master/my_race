import 'package:flutter/material.dart';
import 'package:flutter_riverpod/flutter_riverpod.dart';
import 'package:go_router/go_router.dart';

import '../../core/theme/app_theme.dart';
import '../../models/models.dart';
import '../../providers/data_providers.dart';
import '../../providers/settings_provider.dart';
import '../common/widgets.dart';

/// S01 성인 확인 · 면책
class AdultGatePage extends ConsumerStatefulWidget {
  const AdultGatePage({super.key});
  @override
  ConsumerState<AdultGatePage> createState() => _AdultGateState();
}

class _AdultGateState extends ConsumerState<AdultGatePage> {
  bool checked = false;

  @override
  Widget build(BuildContext context) => Scaffold(
        body: SafeArea(child: Padding(
          padding: const EdgeInsets.fromLTRB(24, 40, 24, 24),
          child: Column(crossAxisAlignment: CrossAxisAlignment.start, children: [
            const Text('my_race', style: TextStyle(fontSize: 40, fontWeight: FontWeight.w900, letterSpacing: -1.5)),
            const SizedBox(height: 16),
            Text('이번 주 경주, 데이터로 미리 봅니다.\n한국마사회 공공데이터 기반.', style: TextStyle(fontSize: 15, color: AppColors.ink2, height: 1.6)),
            const SizedBox(height: 24),
            const Card2(color: Color(0xFFFAFBF9), child: Text.rich(TextSpan(children: [
              TextSpan(text: '이 앱은 '), TextSpan(text: '만 19세 이상', style: TextStyle(fontWeight: FontWeight.w700)),
              TextSpan(text: '만 이용할 수 있습니다.\n제공되는 예측은 참고 정보이며 결과를 보장하지 않습니다. 배팅 대행이나 판매를 하지 않습니다.'),
            ]), style: TextStyle(fontSize: 13, height: 1.7))),
            const SizedBox(height: 12),
            CheckboxListTile(
              value: checked, onChanged: (v) => setState(() => checked = v ?? false), contentPadding: EdgeInsets.zero,
              controlAffinity: ListTileControlAffinity.leading, activeColor: AppColors.turf,
              title: const Text('만 19세 이상이며 위 내용을 확인했습니다', style: TextStyle(fontSize: 13)),
            ),
            const Spacer(),
            FilledButton(
              onPressed: checked ? () async {
                final n = ref.read(settingsProvider.notifier);
                await n.update(ref.read(settingsProvider).copyWith(adultConfirmed: true));
                if (context.mounted) context.go('/onboarding/setup');
              } : null,
              child: const Text('시작하기'),
            ),
            const SizedBox(height: 8),
            Center(child: TextButton(onPressed: () => context.push('/legal'), child: Text('이용약관 · 개인정보 처리방침', style: TextStyle(fontSize: 11, color: AppColors.ink3)))),
          ]),
        )),
      );
}

/// S02 초기 설정
class SetupPage extends ConsumerStatefulWidget {
  const SetupPage({super.key});
  @override
  ConsumerState<SetupPage> createState() => _SetupState();
}

class _SetupState extends ConsumerState<SetupPage> {
  int meet = 1; bool notify = true; bool saving = false;
  static const days = {1: '금·토·일', 3: '금·일', 2: '금·토'};

  @override
  Widget build(BuildContext context) => Scaffold(
        body: SafeArea(child: Padding(
          padding: const EdgeInsets.fromLTRB(20, 32, 20, 20),
          child: Column(crossAxisAlignment: CrossAxisAlignment.start, children: [
            const Text('주로 보는 경마장을\n골라주세요', style: TextStyle(fontSize: 24, fontWeight: FontWeight.w900, height: 1.3, letterSpacing: -0.5)),
            const SizedBox(height: 8),
            Text('홈 화면 기본 필터로 쓰입니다. 나중에 바꿀 수 있어요.', style: AppText.label),
            const SizedBox(height: 16),
            for (final m in [1, 3, 2]) Padding(padding: const EdgeInsets.only(bottom: 8), child: Card2(
              onTap: () => setState(() => meet = m),
              color: meet == m ? AppColors.turf2 : null, border: meet == m ? AppColors.turf : null,
              child: Row(children: [Expanded(child: Text(m == 3 ? '부산경남' : meetName(m), style: AppText.name)), Text(days[m]!, style: AppText.meta)]),
            )),
            const SizedBox(height: 8),
            Card2(child: Row(children: [
              Expanded(child: Text('경주 전 알림 받기', style: AppText.name)),
              Switch(value: notify, onChanged: (v) => setState(() => notify = v), activeColor: AppColors.turf),
            ])),
            const SizedBox(height: 6),
            Text('출마표 확정, 추천 순위 갱신 시 알려드립니다.', style: AppText.label),
            const Spacer(),
            FilledButton(
              onPressed: saving ? null : () async {
                setState(() => saving = true);
                final n = ref.read(settingsProvider.notifier);
                await n.update(ref.read(settingsProvider).copyWith(preferredMeet: meet, notifyEntries: notify, notifyRankUpdate: notify, onboarded: true));
                ref.invalidate(deviceRegisterProvider);
                if (context.mounted) context.go('/');
              },
              child: const Text('완료'),
            ),
          ]),
        )),
      );
}
