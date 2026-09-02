import 'package:flutter/material.dart';
import 'package:flutter_riverpod/flutter_riverpod.dart';
import 'package:go_router/go_router.dart';

import '../../core/theme/app_theme.dart';
import '../../core/theme/gate_colors.dart';
import '../../providers/data_providers.dart';
import '../common/widgets.dart';

/// S06 기수 상세 / S07 조교사 상세 (레이아웃 공유)
class PersonPage extends ConsumerWidget {
  final String no; final bool isJockey;
  const PersonPage(this.no, {super.key, required this.isJockey});

  @override
  Widget build(BuildContext context, WidgetRef ref) {
    final prov = isJockey ? jockeyProvider(no) : trainerProvider(no);
    final async = ref.watch(prov);
    final role = isJockey ? '기수' : '조교사';
    return Scaffold(
      appBar: AppBar(title: Text(async.valueOrNull == null ? role : '${async.value!.name} $role')),
      body: AsyncBody(async, onRetry: () => ref.invalidate(prov), builder: (p) => ListView(padding: const EdgeInsets.all(16), children: [
        Text('${p.meetName}${p.debutYear != null ? ' · 데뷔 ${p.debutYear}' : ''} · 시즌 ${p.season.starts}전 ${p.season.wins}승', style: AppText.meta),
        const SizedBox(height: 12),
        KVGrid([
          ('올해 승률', '${(p.season.winRate * 100).toStringAsFixed(1)}%'),
          ('올해 복승률', '${(p.season.placeRate * 100).toStringAsFixed(1)}%'),
          ('최근 30일 출전', '${p.last30d.starts}회'),
          ('최근 30일 승', '${p.last30d.wins}승'),
        ]),
        const SizedBox(height: 16),
        Text('주별 승률 추이(12주)', style: AppText.label), SizedBox(height: 6),
        if (p.weeklyWinRate.isNotEmpty) RateBars(p.weeklyWinRate.map((w) => (w.weekStart.substring(5).replaceAll('-', '/'), w.rate)).toList())
        else Text('최근 12주 출전 기록이 없습니다.', style: AppText.meta),
        const SizedBox(height: 16),
        if (p.thisWeekEntries.isNotEmpty) ...[
          Text('이번 주 출전', style: AppText.label), SizedBox(height: 6),
          for (final e in p.thisWeekEntries) InkWell(
            onTap: () => context.push('/race/${e.raceId}'),
            child: Padding(padding: const EdgeInsets.symmetric(vertical: 6), child: Row(children: [
              GateBadge(e.gateNo, size: 22), const SizedBox(width: 8),
              Expanded(child: Text('${e.date.substring(5).replaceAll('-', '/')} ${e.raceNo}경주 ${e.horseName}', style: const TextStyle(fontSize: 12))),
              Text(e.ourRank == null ? '' : '추천 ${e.ourRank}위', style: e.ourRank == 1 ? AppText.score : AppText.meta),
            ])),
          ),
        ],
        if (!isJockey) ...[const SizedBox(height: 8), const LockCard('기수별 조합 성적, 월별 추이')],
      ])),
    );
  }
}
