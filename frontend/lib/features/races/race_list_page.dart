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

/// S03 이번 주 경주
class RaceListPage extends ConsumerStatefulWidget {
  const RaceListPage({super.key});
  @override
  ConsumerState<RaceListPage> createState() => _RaceListState();
}

class _RaceListState extends ConsumerState<RaceListPage> {
  static const meets = [1, 3, 2, null]; // 서울 부경 제주 전체
  int meetIdx = 0; int dayIdx = 0;
  late List<DateTime> days;
  /// 과거조회 모드: 캘린더로 고른 날짜(이번 주 칩 대신 사용)
  DateTime? custom;

  @override
  void initState() {
    super.initState();
    final pref = ref.read(settingsProvider).preferredMeet;
    meetIdx = meets.indexOf(pref).clamp(0, 3);
    // 이번 주 금·토·일 (이미 지난 요일은 다음 주로)
    final today = DateTime.now();
    var fri = today.subtract(Duration(days: (today.weekday - DateTime.friday) % 7));
    if (today.weekday == DateTime.monday || today.weekday == DateTime.tuesday) fri = fri.add(const Duration(days: 7));
    days = [fri, fri.add(const Duration(days: 1)), fri.add(const Duration(days: 2))];
    if (today.weekday == DateTime.saturday) dayIdx = 1; else if (today.weekday == DateTime.sunday) dayIdx = 2;
  }

  String _iso(DateTime d) => DateFormat('yyyy-MM-dd').format(d);
  String _label(DateTime d) => DateFormat('E d', 'ko').format(d);
  DateTime get _date => custom ?? days[dayIdx];
  bool get _isPast => _date.isBefore(DateTime(DateTime.now().year, DateTime.now().month, DateTime.now().day));

  Future<void> _pickDate() async {
    final available = ref.read(raceDatesProvider(meets[meetIdx])).valueOrNull;
    final d = await pickRaceDate(context, initial: _date, available: available);
    if (d == null) return;
    final idx = days.indexWhere((x) => _iso(x) == _iso(d));
    setState(() { if (idx >= 0) { dayIdx = idx; custom = null; } else { custom = d; } });
  }

  @override
  Widget build(BuildContext context) {
    ref.watch(raceDatesProvider(meets[meetIdx])); // 캘린더 선택 가능일 미리 로드
    final q = (meet: meets[meetIdx], date: _iso(_date));
    final list = ref.watch(raceListProvider(q));
    final anim = ref.watch(settingsProvider.select((s) => s.animations));
    return Scaffold(
      appBar: AppBar(
        title: Text(custom == null ? '이번 주 경주' : (_isPast ? '지난 경주' : '경주 일정')),
        actions: [
          TextButton.icon(onPressed: _pickDate, icon: Icon(Icons.calendar_month_outlined, size: 16, color: AppColors.turf),
            label: Text(DateFormat('M월 d일(E)', 'ko').format(_date), style: TextStyle(fontSize: 12, color: AppColors.turf, fontWeight: FontWeight.w500))),
        ]),
      body: Column(children: [
        Padding(padding: const EdgeInsets.fromLTRB(16, 8, 16, 0), child: Column(crossAxisAlignment: CrossAxisAlignment.start, children: [
          Chips(labels: const ['서울', '부경', '제주', '전체'], selected: meetIdx, onSelected: (i) => setState(() => meetIdx = i)),
          const SizedBox(height: 6),
          Row(children: [
            Expanded(child: Chips(labels: days.map(_label).toList(), selected: custom == null ? dayIdx : -1, onSelected: (i) => setState(() { dayIdx = i; custom = null; }))),
            if (custom != null) ActionChip(
              avatar: Icon(Icons.history, size: 14, color: AppColors.ink), label: Text(DateFormat('M/d').format(custom!), style: const TextStyle(fontSize: 12)),
              onPressed: _pickDate),
          ]),
          if (_isPast) Padding(padding: const EdgeInsets.only(top: 6),
            child: Text('지난 경주입니다. 당시 예측 순위와 실제 착순을 함께 보여드립니다.', style: AppText.meta)),
        ])),
        Expanded(child: RefreshIndicator(
          onRefresh: () => ref.refresh(raceListProvider(q).future),
          child: AsyncBody(list, onRetry: () => ref.invalidate(raceListProvider(q)), builder: (races) {
            if (races.isEmpty) return Center(child: Column(mainAxisSize: MainAxisSize.min, children: [
              const Padding(padding: EdgeInsets.symmetric(horizontal: 48), child: GallopTrack()),
              Text('이 날은 편성된 경주가 없습니다.', style: TextStyle(color: AppColors.ink2)),
              TextButton(onPressed: _pickDate, child: const Text('다른 날짜 보기')),
            ]));
            return crossFade(key: q, child: ListView.separated(
              padding: const EdgeInsets.all(16), itemCount: races.length, separatorBuilder: (_, __) => const SizedBox(height: 10),
              itemBuilder: (_, i) {
                final card = _RaceCard(races[i], showMeet: meets[meetIdx] == null);
                return anim ? FadeSlideIn(index: i, child: card) : card;
              },
            ));
          }),
        )),
        const AdSlot(),
      ]),
    );
  }
}

class _RaceCard extends StatelessWidget {
  final RaceSummary r; final bool showMeet;
  const _RaceCard(this.r, {required this.showMeet});

  @override
  Widget build(BuildContext context) {
    final fixed = r.fixed;
    return Opacity(opacity: fixed ? 1 : 0.6, child: Card2(
      onTap: fixed ? () => context.push('/race/${r.id}') : null,
      child: Column(crossAxisAlignment: CrossAxisAlignment.start, children: [
        Row(children: [
          Expanded(child: Text('${showMeet ? '${r.meetName} ' : ''}${r.raceNo}경주 · ${r.distance}m · ${r.grade}', style: AppText.name)),
          Text(r.startTime ?? '', style: AppText.meta),
        ]),
        const SizedBox(height: 8),
        if (r.topPick != null) Row(children: [
          Text('추천 1위', style: AppText.label), SizedBox(width: 8),
          PulseRing(child: GateBadge(r.topPick!.gateNo, size: 22)), const SizedBox(width: 8),
          Expanded(child: Text(r.topPick!.horseName, style: AppText.name)),
          Text(r.topPick!.score.toStringAsFixed(1), style: AppText.score),
        ]) else Text('출마표 대기 중 — 목요일 오후 확정', style: AppText.meta),
        const SizedBox(height: 4),
        Text(fixed ? '${r.entryCount}두 출전 · ${r.status == 'FINISHED' ? '경주 종료' : '출마표 확정'}' : '편성 예정', style: AppText.meta),
      ]),
    ));
  }
}
