import 'package:flutter/material.dart';
import 'package:flutter_riverpod/flutter_riverpod.dart';
import 'package:go_router/go_router.dart';

import '../../core/anim/animations.dart';
import '../../core/theme/app_theme.dart';
import '../../core/theme/gate_colors.dart';
import '../../models/models.dart';
import '../../providers/data_providers.dart';
import '../../providers/settings_provider.dart';
import '../common/widgets.dart';

/// S04 경주 상세 · 추천 순위 / 출마표 토글
class RaceDetailPage extends ConsumerStatefulWidget {
  final int raceId;
  const RaceDetailPage(this.raceId, {super.key});
  @override
  ConsumerState<RaceDetailPage> createState() => _RaceDetailState();
}

class _RaceDetailState extends ConsumerState<RaceDetailPage> {
  bool byGate = false;

  @override
  Widget build(BuildContext context) {
    final async = ref.watch(raceDetailProvider(widget.raceId));
    final anim = ref.watch(settingsProvider.select((s) => s.animations));
    return Scaffold(
      appBar: AppBar(
        title: Text(async.valueOrNull == null ? '경주' : '${async.value!.meetName} ${async.value!.raceNo}경주'),
        actions: [TextButton(onPressed: () => setState(() => byGate = !byGate),
            child: Text(byGate ? '추천 순위' : '출마표', style: TextStyle(fontSize: 12, color: AppColors.turf)))],
      ),
      body: AsyncBody(async, onRetry: () => ref.invalidate(raceDetailProvider(widget.raceId)), builder: (d) {
        final entries = [...d.entries];
        if (byGate) entries.sort((a, b) => a.gateNo.compareTo(b.gateNo));
        final lockedCount = entries.where((e) => e.locked).length;
        final visible = byGate ? entries : entries.where((e) => !e.locked).toList();
        final list = ListView(padding: const EdgeInsets.fromLTRB(16, 8, 16, 24), children: [
          Row(children: [
            Expanded(child: Text('${d.date.substring(5).replaceAll('-', '/')} ${d.startTime ?? ''} · ${d.distance}m · ${d.track} · ${d.grade} · ${d.entries.where((e) => !e.scratched).length}두', style: AppText.meta)),
            if (d.hit != null) _HitTag(d.hit!),
          ]),
          const SizedBox(height: 6),
          for (final (i, e) in visible.indexed)
            anim ? FadeSlideIn(index: i, child: _EntryRow(e, finished: d.finished, showRank: !byGate, anim: true)) : _EntryRow(e, finished: d.finished, showRank: !byGate, anim: false),
          if (!byGate && lockedCount > 0) ...[
            const SizedBox(height: 8),
            LockCard('${d.freeRankLimit + 1}위 이하 순위와 근거 지표 보기'),
          ],
          if (byGate && !d.premium) ...[const SizedBox(height: 8), const LockCard('모든 출전마의 점수와 근거 지표 보기')],
          Disclaimer(text: d.disclaimer),
          if (d.engineVersion != null) Text('엔진 ${d.engineVersion}', style: AppText.label),
        ]);
        return anim ? crossFade(key: byGate, child: list) : list;
      }),
    );
  }
}

class _EntryRow extends StatelessWidget {
  final EntryView e; final bool finished, showRank, anim;
  const _EntryRow(this.e, {required this.finished, required this.showRank, required this.anim});

  @override
  Widget build(BuildContext context) {
    final top = (e.rank ?? 99) <= 3;
    return InkWell(
      onTap: () => context.push('/horse/${e.hrNo}'),
      child: Container(
        padding: const EdgeInsets.symmetric(vertical: 8),
        decoration: BoxDecoration(border: Border(bottom: BorderSide(color: AppColors.line))),
        child: Row(crossAxisAlignment: CrossAxisAlignment.center, children: [
          SizedBox(width: 22, child: Text(
            finished && e.result != null ? '${e.result!.finishPos}' : (showRank ? '${e.rank ?? '-'}' : ''),
            textAlign: TextAlign.center,
            style: TextStyle(fontSize: 15, fontWeight: FontWeight.w900, color: (finished ? (e.result?.finishPos ?? 9) <= 3 : top) ? AppColors.red : AppColors.ink))),
          const SizedBox(width: 8),
          if (anim && !finished && e.rank == 1) PulseRing(child: GateBadge(e.gateNo)) else GateBadge(e.gateNo),
          const SizedBox(width: 10),
          Expanded(child: Column(crossAxisAlignment: CrossAxisAlignment.start, children: [
            Text(e.horseName, style: AppText.name.copyWith(decoration: e.scratched ? TextDecoration.lineThrough : null)),
            Text('${e.jockeyName} · ${e.trainerName} · ${e.weightCarried.toStringAsFixed(0)}kg${e.scratched ? ' · 출전취소' : ''}', style: AppText.meta),
            if (e.score != null && !finished) Padding(padding: const EdgeInsets.only(top: 5, right: 24),
              child: ScoreBar(factor: e.score! / 100, delay: Duration(milliseconds: anim ? 60 * ((e.rank ?? 1) - 1) : 0))),
            if (e.factors != null) Padding(padding: const EdgeInsets.only(top: 4), child: Wrap(spacing: 6, runSpacing: 2,
              children: e.factors!.values.map((t) => Text(t, style: TextStyle(fontSize: 10, color: AppColors.turf))).toList())),
          ])),
          const SizedBox(width: 8),
          if (finished && e.result != null)
            Text(e.result!.finishTime ?? '', style: AppText.meta)
          else if (e.score != null)
            (anim ? CountUpText(e.score!, format: (v) => v.toStringAsFixed(1), style: AppText.score) : Text(e.score!.toStringAsFixed(1), style: AppText.score))
          else if (e.locked)
            Icon(Icons.lock_outline, size: 14, color: AppColors.tan),
        ]),
      ),
    );
  }
}

class _HitTag extends StatelessWidget {
  final HitFlags h;
  const _HitTag(this.h);
  @override
  Widget build(BuildContext context) {
    final ok = h.showHit;
    return Container(padding: const EdgeInsets.symmetric(horizontal: 6, vertical: 2),
        decoration: BoxDecoration(color: ok ? AppColors.turf2 : AppColors.red2, borderRadius: BorderRadius.circular(4)),
        child: Text(h.label, style: TextStyle(fontSize: 10, fontWeight: FontWeight.w700, color: ok ? AppColors.turf : AppColors.red)));
  }
}
