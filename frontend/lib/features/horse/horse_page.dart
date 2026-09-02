import 'package:flutter/material.dart';
import 'package:flutter_riverpod/flutter_riverpod.dart';
import 'package:go_router/go_router.dart';

import '../../core/theme/app_theme.dart';
import '../../models/models.dart';
import '../../providers/data_providers.dart';
import '../common/widgets.dart';

/// S05 경주마 상세
class HorsePage extends ConsumerWidget {
  final String hrNo;
  const HorsePage(this.hrNo, {super.key});

  @override
  Widget build(BuildContext context, WidgetRef ref) {
    final async = ref.watch(horseProvider(hrNo));
    return Scaffold(
      appBar: AppBar(title: Text(async.valueOrNull?.name ?? '경주마')),
      body: AsyncBody(async, onRetry: () => ref.invalidate(horseProvider(hrNo)), builder: (h) {
        final best = h.byDistance.isEmpty ? null : (h.byDistance.toList()..sort((a, b) => (b.wins / (b.starts == 0 ? 1 : b.starts)).compareTo(a.wins / (a.starts == 0 ? 1 : a.starts)))).first;
        return ListView(padding: const EdgeInsets.all(16), children: [
          Text('${h.country} · ${h.age}세 · ${h.sex} · 레이팅 ${h.rating} · ${h.starts}전 ${h.wins}승 ${h.seconds}준', style: AppText.meta),
          const SizedBox(height: 12),
          KVGrid([
            ('통산 승률', h.starts == 0 ? '-' : '${(h.wins / h.starts * 100).toStringAsFixed(1)}%'),
            ('통산 복승률', h.starts == 0 ? '-' : '${((h.wins + h.seconds) / h.starts * 100).toStringAsFixed(1)}%'),
            ('최근 ${h.recentRuns.length}전 평균', h.recentRuns.isEmpty ? '-' : '${(h.recentRuns.map((r) => r.finishPos).reduce((a, b) => a + b) / h.recentRuns.length).toStringAsFixed(1)}위'),
            ('강한 거리', best == null ? '-' : '${best.distance}m ${best.starts}전 ${best.wins}승'),
          ]),
          const SizedBox(height: 16),
          Text('최근 착순', style: AppText.label), SizedBox(height: 6),
          if (h.recentRuns.isNotEmpty) PositionBars(h.recentRuns.map((r) => (r.finishPos, r.fieldSize)).toList()),
          const SizedBox(height: 12),
          for (final r in h.recentRuns) InkWell(
            onTap: () => context.push('/jockey/${r.jkNo}'),
            child: Padding(padding: const EdgeInsets.symmetric(vertical: 6), child: Row(children: [
              SizedBox(width: 20, child: Text('${r.finishPos}', style: TextStyle(fontWeight: FontWeight.w900, color: r.finishPos <= 3 ? AppColors.red : AppColors.ink))),
              Expanded(child: Text('${r.date.substring(5).replaceAll('-', '/')} ${meetName(r.meet)} ${r.distance}m · ${r.jockeyName} · ${r.finishTime ?? ''}', style: const TextStyle(fontSize: 12))),
              if (r.winOdds != null) Text('${r.winOdds}배', style: AppText.meta),
            ])),
          ),
          if (h.runsLocked) ...[const SizedBox(height: 8), const LockCard('전체 출전 기록과 배당 이력')],
          const Disclaimer(),
        ]);
      }),
    );
  }
}
