import 'package:flutter/material.dart';
import 'package:flutter_riverpod/flutter_riverpod.dart';
import 'package:go_router/go_router.dart';

import '../../core/network/api_client.dart';
import '../../core/anim/animations.dart';
import '../../core/theme/app_theme.dart';
import '../../providers/data_providers.dart';
import '../settings/premium_sheet.dart';

class Card2 extends StatelessWidget {
  final Widget child;
  final EdgeInsets padding;
  final VoidCallback? onTap;
  final Color? color, border;
  const Card2(
      {super.key,
      required this.child,
      this.padding = const EdgeInsets.fromLTRB(12, 10, 12, 10),
      this.onTap,
      this.color,
      this.border});
  @override
  Widget build(BuildContext context) => Material(
        color: color ?? AppColors.paper,
        borderRadius: BorderRadius.circular(12),
        child: InkWell(
          onTap: onTap,
          borderRadius: BorderRadius.circular(12),
          child: Container(
            padding: padding,
            decoration: BoxDecoration(
                borderRadius: BorderRadius.circular(12),
                border: Border.all(color: border ?? AppColors.line)),
            child: child,
          ),
        ),
      );
}

/// 프리미엄 잠금 카드(02_screens .lock)
class LockCard extends StatelessWidget {
  final String text;
  const LockCard(this.text, {super.key});
  @override
  Widget build(BuildContext context) => Card2(
        color: AppColors.tan2,
        border: AppColors.tan,
        onTap: () => showPremiumSheet(context),
        child: Row(children: [
          Expanded(
              child: Text(text,
                  style:
                      const TextStyle(fontSize: 12, color: Color(0xFF6E5330)))),
          Icon(Icons.lock_outline, size: 14, color: AppColors.tan),
          SizedBox(width: 4),
          Text('프리미엄',
              style: TextStyle(
                  fontSize: 12,
                  fontWeight: FontWeight.w700,
                  color: AppColors.tan)),
        ]),
      );
}

class Chips extends StatelessWidget {
  final List<String> labels;
  final int selected;
  final ValueChanged<int> onSelected;
  const Chips(
      {super.key,
      required this.labels,
      required this.selected,
      required this.onSelected});
  @override
  Widget build(BuildContext context) => Wrap(
        spacing: 6,
        children: [
          for (var i = 0; i < labels.length; i++)
            ChoiceChip(
                label: Text(labels[i]),
                selected: i == selected,
                onSelected: (_) => onSelected(i),
                labelStyle: TextStyle(
                    fontSize: 12,
                    color: i == selected ? AppColors.paper : AppColors.ink2),
                padding: const EdgeInsets.symmetric(horizontal: 6, vertical: 0),
                visualDensity: VisualDensity.compact)
        ],
      );
}

class KV extends StatelessWidget {
  final String label, value;
  const KV(this.label, this.value, {super.key});
  @override
  Widget build(BuildContext context) => Container(
        padding: const EdgeInsets.all(8),
        decoration: BoxDecoration(
            color: const Color(0xFFF7F9F6),
            borderRadius: BorderRadius.circular(8)),
        child: Column(crossAxisAlignment: CrossAxisAlignment.start, children: [
          Text(label, style: AppText.label),
          SizedBox(height: 2),
          Text(value,
              style:
                  const TextStyle(fontSize: 15, fontWeight: FontWeight.w700)),
        ]),
      );
}

class KVGrid extends StatelessWidget {
  final List<(String, String)> items;
  const KVGrid(this.items, {super.key});
  @override
  Widget build(BuildContext context) => GridView.count(
        crossAxisCount: 2,
        shrinkWrap: true,
        physics: const NeverScrollableScrollPhysics(),
        mainAxisSpacing: 6,
        crossAxisSpacing: 6,
        childAspectRatio: 2.9,
        children: [for (final (k, v) in items) KV(k, v)],
      );
}

/// 착순 막대 (낮을수록 좋음 → 막대 높을수록 좋음으로 반전). 0에서 자라나는 애니메이션.
class PositionBars extends StatelessWidget {
  final List<(int pos, int field)> runs; // 최신순
  const PositionBars(this.runs, {super.key});
  @override
  Widget build(BuildContext context) {
    final list = runs.reversed.toList();
    return SizedBox(
      height: 64,
      child: Row(crossAxisAlignment: CrossAxisAlignment.end, children: [
        for (var i = 0; i < list.length; i++)
          Expanded(
              child: Padding(
            padding: const EdgeInsets.symmetric(horizontal: 2),
            child: GrowBar(
              factor: (1 - (list[i].$1 - 1) / list[i].$2.clamp(2, 20))
                  .clamp(0.12, 1.0)
                  .toDouble(),
              color: list[i].$1 <= 3 ? AppColors.turf : AppColors.turf2,
              delay: Duration(milliseconds: 40 * i),
            ),
          )),
      ]),
    );
  }
}

class RateBars extends StatelessWidget {
  final List<(String label, double rate)> items;
  const RateBars(this.items, {super.key});
  @override
  Widget build(BuildContext context) {
    final max = items
        .fold(0.0, (m, e) => e.$2 > m ? e.$2 : m)
        .clamp(0.01, 1.0)
        .toDouble();
    return SizedBox(
        height: 72,
        child: Row(crossAxisAlignment: CrossAxisAlignment.end, children: [
          for (var i = 0; i < items.length; i++)
            Expanded(
                child:
                    Column(mainAxisAlignment: MainAxisAlignment.end, children: [
              CountUpText(items[i].$2 * 100,
                  format: (v) => '${v.round()}%',
                  style: TextStyle(fontSize: 9, color: AppColors.ink3)),
              SizedBox(
                  height: 48,
                  child: Padding(
                      padding: const EdgeInsets.symmetric(horizontal: 3),
                      child: GrowBar(
                          factor: items[i].$2 / max,
                          color: items[i].$2 >= max
                              ? AppColors.turf
                              : AppColors.turf2,
                          delay: Duration(milliseconds: 50 * i)))),
              const SizedBox(height: 2),
              Text(items[i].$1,
                  style: TextStyle(fontSize: 9, color: AppColors.ink3)),
            ])),
        ]));
  }
}

/// 과거조회 캘린더: 경주가 있는 날만 선택 가능. [available] 은 yyyy-MM-dd 집합(null 이면 전부 허용).
Future<DateTime?> pickRaceDate(BuildContext context,
    {required DateTime initial,
    Set<String>? available,
    DateTime? first,
    DateTime? last}) {
  String iso(DateTime d) =>
      '${d.year.toString().padLeft(4, '0')}-${d.month.toString().padLeft(2, '0')}-${d.day.toString().padLeft(2, '0')}';
  final now = DateTime.now();
  final f = first ?? now.subtract(const Duration(days: 365));
  final l = last ?? now.add(const Duration(days: 7));
  var init = initial.isBefore(f) ? f : (initial.isAfter(l) ? l : initial);
  if (available != null &&
      available.isNotEmpty &&
      !available.contains(iso(init))) {
    // 초기값이 선택 불가면 가장 가까운 가능일로
    final sorted = available.map(DateTime.parse).toList()
      ..sort((a, b) =>
          (a.difference(init).abs()).compareTo(b.difference(init).abs()));
    init = sorted.first;
  }
  return showDatePicker(
    context: context,
    initialDate: init,
    firstDate: f,
    lastDate: l,
    locale: const Locale('ko'),
    helpText: '경주 날짜 선택',
    confirmText: '보기',
    cancelText: '취소',
    selectableDayPredicate: available == null || available.isEmpty
        ? null
        : (d) => available.contains(iso(d)),
  );
}

/// AsyncValue 공통 처리. 에러는 원인과 다음 행동을 말한다(frontend-design 규칙).
class AsyncBody<T> extends StatelessWidget {
  final AsyncValue<T> value;
  final Widget Function(T) builder;
  final VoidCallback? onRetry;

  /// true 면 로딩 중 리스트 스켈레톤(쉬머) 표시
  final bool skeleton;
  const AsyncBody(this.value,
      {super.key, required this.builder, this.onRetry, this.skeleton = true});
  @override
  Widget build(BuildContext context) => value.when(
        data: builder,
        loading: () => skeleton
            ? const ShimmerList()
            : const Center(
                child: Padding(
                    padding: EdgeInsets.all(32),
                    child: CircularProgressIndicator(strokeWidth: 2))),
        error: (e, _) {
          final msg = e is ApiException ? e.message : '데이터를 불러오지 못했습니다.';
          return Center(
              child: Padding(
                  padding: const EdgeInsets.all(24),
                  child: Column(mainAxisSize: MainAxisSize.min, children: [
                    Text(msg,
                        textAlign: TextAlign.center,
                        style: TextStyle(color: AppColors.ink2)),
                    if (onRetry != null)
                      TextButton(
                          onPressed: onRetry, child: const Text('다시 시도')),
                    if (e is ApiException && e.adultRequired)
                      TextButton(
                          onPressed: () => context.go('/onboarding'),
                          child: const Text('성인 확인으로 이동')),
                  ])));
        },
      );
}

/// 무료 사용자 배너 자리(B18에서 google_mobile_ads 연결). 원격 플래그 adsEnabled=false 면 사라진다.
class AdSlot extends ConsumerWidget {
  const AdSlot({super.key});
  @override
  Widget build(BuildContext context, WidgetRef ref) {
    if (!ref.watch(adsEnabledProvider)) return const SizedBox.shrink();
    return Container(
        height: 50,
        alignment: Alignment.center,
        color: AppColors.shimmer,
        child:
            Text('광고', style: TextStyle(fontSize: 10, color: AppColors.ink3)));
  }
}

/// 화면 하단 면책 문구(01_requirements §5 표현 규칙 준수). [text] 생략 또는 null 이면 기본 문구.
class Disclaimer extends StatelessWidget {
  static const defaultText =
      '본 정보는 한국마사회 공공데이터를 바탕으로 자동 산출한 참고 자료이며, 경주 결과나 수익을 보장하지 않습니다.';
  final String? text;
  const Disclaimer({super.key, this.text});
  @override
  Widget build(BuildContext context) => Padding(
        padding: const EdgeInsets.only(top: 16, bottom: 8),
        child: Text(text ?? defaultText,
            textAlign: TextAlign.center, style: AppText.meta),
      );
}
