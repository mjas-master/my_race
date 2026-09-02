import 'package:flutter/material.dart';

import '../../core/theme/app_theme.dart';
import '../common/widgets.dart';

/// S10 프리미엄 구독 (UI만. B17 에서 in_app_purchase + POST /subscriptions/verify 연결)
Future<void> showPremiumSheet(BuildContext context) => showModalBottomSheet(
      context: context, isScrollControlled: true, showDragHandle: true,
      shape: const RoundedRectangleBorder(borderRadius: BorderRadius.vertical(top: Radius.circular(20))),
      builder: (_) => const _PremiumSheet(),
    );

class _PremiumSheet extends StatefulWidget {
  const _PremiumSheet();
  @override
  State<_PremiumSheet> createState() => _PremiumSheetState();
}

class _PremiumSheetState extends State<_PremiumSheet> {
  bool yearly = false;
  static const perks = ['1~12위 전체 추천 순위', '점수 근거 6개 지표·차트', '경주별 적중 이력 상세', '출마표 확정·순위 갱신 알림', '배너 광고 제거'];

  @override
  Widget build(BuildContext context) => Padding(
        padding: EdgeInsets.fromLTRB(20, 4, 20, 20 + MediaQuery.of(context).viewInsets.bottom),
        child: Column(mainAxisSize: MainAxisSize.min, crossAxisAlignment: CrossAxisAlignment.start, children: [
          const Text('전체 순위와 근거를\n광고 없이 봅니다', style: TextStyle(fontSize: 22, fontWeight: FontWeight.w900, height: 1.35, letterSpacing: -0.5)),
          const SizedBox(height: 12),
          for (final p in perks) Padding(padding: const EdgeInsets.symmetric(vertical: 7), child: Row(children: [
            Expanded(child: Text(p, style: TextStyle(fontSize: 13))), Icon(Icons.check, size: 16, color: AppColors.turf),
          ])),
          const SizedBox(height: 8),
          _plan('월간', null, '₩9,900', !yearly, () => setState(() => yearly = false)),
          const SizedBox(height: 8),
          _plan('연간', '월 6,583원', '₩79,000', yearly, () => setState(() => yearly = true)),
          const SizedBox(height: 14),
          FilledButton(
            onPressed: () {
              Navigator.pop(context);
              ScaffoldMessenger.of(context).showSnackBar(const SnackBar(content: Text('스토어 결제는 준비 중입니다. (B17)')));
            },
            child: Text('${yearly ? '연간' : '월간'} 구독 시작'),
          ),
          const SizedBox(height: 8),
          Center(child: Text('스토어 계정으로 결제·해지됩니다. 구독 복원', style: TextStyle(fontSize: 11, color: AppColors.ink3))),
        ]),
      );

  Widget _plan(String name, String? sub, String price, bool on, VoidCallback tap) => Card2(
        onTap: tap, color: on ? AppColors.turf2 : null, border: on ? AppColors.turf : null,
        child: Row(children: [
          Expanded(child: Text.rich(TextSpan(children: [
            TextSpan(text: name, style: AppText.name), if (sub != null) TextSpan(text: '  $sub', style: AppText.meta),
          ]))),
          Text(price, style: TextStyle(fontSize: 15, fontWeight: FontWeight.w700, color: on ? AppColors.turf : AppColors.ink)),
        ]),
      );
}
