import 'package:flutter/material.dart';
import 'package:flutter_riverpod/flutter_riverpod.dart';
import 'package:go_router/go_router.dart';
import 'package:intl/intl.dart';

import '../../core/anim/animations.dart';
import '../../core/theme/app_theme.dart';
import '../../core/theme/gate_colors.dart';
import '../../models/models.dart';
import '../../providers/data_providers.dart';
import '../../providers/settings_provider.dart';
import '../common/widgets.dart';

/// S08 경주 결과
class ResultsPage extends ConsumerStatefulWidget {
  const ResultsPage({super.key});
  @override
  ConsumerState<ResultsPage> createState() => _ResultsState();
}

class _ResultsState extends ConsumerState<ResultsPage> {
  static const meets = [1, 3, 2];
  int meetIdx = 0;
  late DateTime date;

  @override
  void initState() {
    super.initState();
    meetIdx = meets.indexOf(ref.read(settingsProvider).preferredMeet).clamp(0, 2);
    // 가장 최근 지난 일요일(또는 어제가 경마일이면 어제)
    final t = DateTime.now();
    date = t.subtract(Duration(days: t.weekday == DateTime.sunday ? 7 : t.weekday % 7));
  }

  Future<void> _pick() async {
    final d = await showDatePicker(context: context, initialDate: date, firstDate: DateTime.now().subtract(const Duration(days: 365)), lastDate: DateTime.now());
    if (d != null) setState(() => date = d);
  }

  @override
  Widget build(BuildContext context) {
    final q = (meet: meets[meetIdx], date: DateFormat('yyyy-MM-dd').format(date));
    final async = ref.watch(resultsProvider(q));
    return Scaffold(
      appBar: AppBar(title: const Text('경주 결과'), actions: [
        TextButton(onPressed: _pick, child: Text('${DateFormat('M월 d일(E)', 'ko').format(date)} ▾', style: TextStyle(fontSize: 12, color: AppColors.turf))),
      ]),
      body: Column(children: [
        Padding(padding: const EdgeInsets.fromLTRB(16, 8, 16, 0), child: Align(alignment: Alignment.centerLeft,
            child: Chips(labels: const ['서울', '부경', '제주'], selected: meetIdx, onSelected: (i) => setState(() => meetIdx = i)))),
        Expanded(child: AsyncBody(async, onRetry: () => ref.invalidate(resultsProvider(q)), builder: (races) {
          if (races.isEmpty) return Center(child: Text('이 날의 결과가 없습니다. 날짜를 바꿔보세요.', style: TextStyle(color: AppColors.ink2)));
          return ListView.separated(padding: const EdgeInsets.all(16), itemCount: races.length, separatorBuilder: (_, __) => const SizedBox(height: 10),
              itemBuilder: (_, i) => FadeSlideIn(index: i, child: _ResultCard(races[i])));
        })),
        const AdSlot(),
      ]),
    );
  }
}

class _ResultCard extends StatelessWidget {
  final RaceResultView r;
  const _ResultCard(this.r);
  @override
  Widget build(BuildContext context) {
    final h = r.hit;
    return Card2(onTap: () => context.push('/race/${r.id}'), child: Column(crossAxisAlignment: CrossAxisAlignment.start, children: [
      Row(children: [
        Expanded(child: Text('${r.raceNo}경주 · ${r.distance}m', style: AppText.name)),
        if (h != null) Container(padding: const EdgeInsets.symmetric(horizontal: 6, vertical: 2),
            decoration: BoxDecoration(color: h.showHit ? AppColors.turf2 : AppColors.red2, borderRadius: BorderRadius.circular(4)),
            child: Text(h.label, style: TextStyle(fontSize: 10, fontWeight: FontWeight.w700, color: h.showHit ? AppColors.turf : AppColors.red))),
      ]),
      const SizedBox(height: 6),
      for (final t in r.top3) Padding(padding: const EdgeInsets.symmetric(vertical: 2), child: Row(children: [
        SizedBox(width: 18, child: Text('${t.finishPos}', style: const TextStyle(fontWeight: FontWeight.w900, fontSize: 12))),
        GateBadge(t.gateNo, size: 20), const SizedBox(width: 8),
        Expanded(child: Text(t.horseName, style: const TextStyle(fontSize: 12))),
        Text(_odds(t, r), style: AppText.meta),
      ])),
    ]));
  }

  String _odds(ResultTop t, RaceResultView r) {
    final parts = <String>[];
    if (t.finishPos == 1 && t.finishTime != null) parts.add(t.finishTime!);
    if (t.winOdds != null) parts.add('단 ${t.winOdds}');
    if (t.placeOdds != null && t.finishPos != 1) parts.add('복 ${t.placeOdds}');
    if (r.ourTopPick?.gateNo == t.gateNo && t.finishPos != 1) parts.add('우리 예측 1위');
    return parts.join(' · ');
  }
}
