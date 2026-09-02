import 'package:flutter/material.dart';
import 'package:flutter_riverpod/flutter_riverpod.dart';

import '../../core/anim/animations.dart';
import '../../core/theme/app_theme.dart';
import '../../providers/data_providers.dart';
import '../common/widgets.dart';

/// S09 적중 이력
class HitsPage extends ConsumerStatefulWidget {
  const HitsPage({super.key});
  @override
  ConsumerState<HitsPage> createState() => _HitsState();
}

class _HitsState extends ConsumerState<HitsPage> {
  int weeks = 4;
  static const options = [4, 8, 12, 26];

  String pct(double v) => '${(v * 100).toStringAsFixed(1)}%';

  @override
  Widget build(BuildContext context) {
    final q = (meet: null as int?, weeks: weeks);
    final async = ref.watch(hitSummaryProvider(q));
    return Scaffold(
      appBar: AppBar(title: const Text('적중 이력'), actions: [
        PopupMenuButton<int>(
          onSelected: (w) => setState(() => weeks = w),
          itemBuilder: (_) => options.map((w) => PopupMenuItem(value: w, child: Text('최근 $w주'))).toList(),
          child: Padding(padding: EdgeInsets.symmetric(horizontal: 16), child: Text('최근 $weeks주 ▾', style: TextStyle(fontSize: 12, color: AppColors.turf))),
        ),
      ]),
      body: AsyncBody(async, onRetry: () => ref.invalidate(hitSummaryProvider(q)), builder: (s) => ListView(padding: const EdgeInsets.all(16), children: [
        Container(padding: EdgeInsets.all(16), decoration: BoxDecoration(color: AppColors.turf, borderRadius: BorderRadius.circular(14)),
          child: Column(crossAxisAlignment: CrossAxisAlignment.start, children: [
            const Text('추천 1위 → 1착 적중률', style: TextStyle(fontSize: 11, color: Color(0xFFCFE3D7))),
            const SizedBox(height: 4),
            s.races == 0 ? Text('—', style: AppText.big.copyWith(color: Colors.white))
                         : CountUpText(s.winRate * 100, format: (v) => '${v.toStringAsFixed(1)}%', style: AppText.big.copyWith(color: Colors.white)),
            const SizedBox(height: 8),
            Text(s.races == 0 ? '아직 평가된 경주가 없습니다.' : '복승(1~2착 내) ${pct(s.placeRate)} · 삼복 ${pct(s.showRate)} · 총 ${s.races}경주',
                style: const TextStyle(fontSize: 11, color: Color(0xFFCFE3D7))),
          ])),
        const SizedBox(height: 10),
        KVGrid([
          for (final m in s.byMeet) (m.meetName, '${pct(m.winRate)} (${m.races})'),
          ('엔진', s.engineVersion),
        ]),
        const SizedBox(height: 16),
        Text('주별 1착 적중률', style: AppText.label), SizedBox(height: 6),
        if (s.weekly.isNotEmpty) RateBars(s.weekly.map((w) => (w.weekStart.substring(5).replaceAll('-', '/'), w.winRate)).toList()),
        const SizedBox(height: 12),
        if (s.detailLocked) const LockCard('경주별 상세 이력·등급별 적중률'),
        const Disclaimer(text: '과거 적중률은 미래 결과를 보장하지 않습니다.'),
      ])),
    );
  }
}
