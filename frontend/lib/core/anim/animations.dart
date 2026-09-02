import 'dart:math' as math;

import 'package:flutter/material.dart';

import '../theme/app_theme.dart';

/// 리스트 항목 등장: 아래→위 슬라이드 + 페이드, index 에 따라 스태거.
class FadeSlideIn extends StatelessWidget {
  final int index; final Widget child; final Duration base; final int maxStagger;
  const FadeSlideIn({super.key, required this.index, required this.child, this.base = const Duration(milliseconds: 320), this.maxStagger = 8});

  @override
  Widget build(BuildContext context) {
    final delay = Duration(milliseconds: 45 * math.min(index, maxStagger));
    return TweenAnimationBuilder<double>(
      tween: Tween(begin: 0, end: 1),
      duration: base + delay,
      curve: Interval(delay.inMilliseconds / (base + delay).inMilliseconds, 1, curve: Curves.easeOutCubic),
      builder: (_, t, c) => Opacity(opacity: t, child: Transform.translate(offset: Offset(0, 14 * (1 - t)), child: c)),
      child: child,
    );
  }
}

/// 숫자 카운트업(적중률·점수). value 가 바뀌면 이전 값에서 새 값으로 굴러간다.
class CountUpText extends ImplicitlyAnimatedWidget {
  final double value; final String Function(double) format; final TextStyle? style;
  const CountUpText(this.value, {super.key, required this.format, this.style, super.duration = const Duration(milliseconds: 700), super.curve = Curves.easeOutCubic});
  @override
  AnimatedWidgetBaseState<CountUpText> createState() => _CountUpState();
}

class _CountUpState extends AnimatedWidgetBaseState<CountUpText> {
  Tween<double>? _t;
  @override
  void forEachTween(TweenVisitor<dynamic> visitor) {
    _t = visitor(_t, widget.value, (v) => Tween<double>(begin: v as double)) as Tween<double>?;
  }
  @override
  Widget build(BuildContext context) => Text(widget.format(_t?.evaluate(animation) ?? widget.value), style: widget.style);
}

/// 세로 막대: 높이가 0→목표로 자라나는 효과.
class GrowBar extends StatelessWidget {
  final double factor; final Color color; final double width; final Duration duration; final Duration delay;
  const GrowBar({super.key, required this.factor, required this.color, this.width = double.infinity,
    this.duration = const Duration(milliseconds: 600), this.delay = Duration.zero});

  @override
  Widget build(BuildContext context) => TweenAnimationBuilder<double>(
        tween: Tween(begin: 0, end: factor.clamp(0.0, 1.0)),
        duration: duration + delay,
        curve: Interval(delay.inMilliseconds / (duration + delay).inMilliseconds, 1, curve: Curves.easeOutBack),
        builder: (_, t, __) => FractionallySizedBox(heightFactor: t, alignment: Alignment.bottomCenter,
            child: Container(width: width, decoration: BoxDecoration(color: color, borderRadius: const BorderRadius.vertical(top: Radius.circular(3))))),
      );
}

/// 가로 진행바(점수 시각화). 0→factor.
class ScoreBar extends StatelessWidget {
  final double factor; final Color? color; final double height; final Duration delay;
  const ScoreBar({super.key, required this.factor, this.color, this.height = 4, this.delay = Duration.zero});

  @override
  Widget build(BuildContext context) => ClipRRect(
        borderRadius: BorderRadius.circular(height),
        child: Container(height: height, color: AppColors.turf2, alignment: Alignment.centerLeft,
          child: TweenAnimationBuilder<double>(
            tween: Tween(begin: 0, end: factor.clamp(0.0, 1.0)),
            duration: const Duration(milliseconds: 650) + delay,
            curve: Interval(delay.inMilliseconds / (650 + delay.inMilliseconds), 1, curve: Curves.easeOutCubic),
            builder: (_, t, __) => FractionallySizedBox(widthFactor: t, child: Container(color: color ?? AppColors.turf)),
          )),
      );
}

/// 로딩 스켈레톤(쉬머). 리스트 카드 형태를 흐릿하게 반복 표시.
class ShimmerList extends StatefulWidget {
  final int count; final double height;
  const ShimmerList({super.key, this.count = 5, this.height = 84});
  @override
  State<ShimmerList> createState() => _ShimmerState();
}

class _ShimmerState extends State<ShimmerList> with SingleTickerProviderStateMixin {
  late final AnimationController _c = AnimationController(vsync: this, duration: const Duration(milliseconds: 1300))..repeat();
  @override
  void dispose() { _c.dispose(); super.dispose(); }

  @override
  Widget build(BuildContext context) => AnimatedBuilder(
        animation: _c,
        builder: (_, __) {
          final base = AppColors.shimmer; final hi = AppColors.paper;
          final dx = -1 + 2 * _c.value;
          return ListView.separated(
            padding: const EdgeInsets.all(16), physics: const NeverScrollableScrollPhysics(),
            itemCount: widget.count, separatorBuilder: (_, __) => const SizedBox(height: 10),
            itemBuilder: (_, i) => Container(
              height: widget.height,
              decoration: BoxDecoration(
                borderRadius: BorderRadius.circular(12), border: Border.all(color: AppColors.line),
                gradient: LinearGradient(begin: Alignment(dx - 1, 0), end: Alignment(dx + 1, 0), colors: [base, hi, base], stops: const [0.2, 0.5, 0.8]),
              ),
            ),
          );
        },
      );
}

/// 추천 1위 강조: 은은한 펄스 링(반복).
class PulseRing extends StatefulWidget {
  final Widget child; final Color? color;
  const PulseRing({super.key, required this.child, this.color});
  @override
  State<PulseRing> createState() => _PulseState();
}

class _PulseState extends State<PulseRing> with SingleTickerProviderStateMixin {
  late final AnimationController _c = AnimationController(vsync: this, duration: const Duration(milliseconds: 1600))..repeat();
  @override
  void dispose() { _c.dispose(); super.dispose(); }
  @override
  Widget build(BuildContext context) => AnimatedBuilder(
        animation: _c,
        builder: (_, child) => Stack(alignment: Alignment.center, children: [
          Container(width: 22 + 14 * _c.value, height: 22 + 14 * _c.value,
              decoration: BoxDecoration(shape: BoxShape.circle, border: Border.all(color: (widget.color ?? AppColors.turf).withOpacity(0.55 * (1 - _c.value)), width: 1.5))),
          child!,
        ]),
        child: widget.child,
      );
}

/// 순위 갱신 시 살짝 튀는 효과(내용이 바뀔 때 스케일).
class PopOnChange extends StatelessWidget {
  final Object keyValue; final Widget child;
  const PopOnChange({super.key, required this.keyValue, required this.child});
  @override
  Widget build(BuildContext context) => AnimatedSwitcher(
        duration: const Duration(milliseconds: 380), switchInCurve: Curves.easeOutBack,
        transitionBuilder: (c, a) => ScaleTransition(scale: Tween(begin: 0.85, end: 1.0).animate(a), child: FadeTransition(opacity: a, child: c)),
        child: KeyedSubtree(key: ValueKey(keyValue), child: child),
      );
}

/// 순위/게이트 토글 등 리스트 전체 전환.
Widget crossFade({required Object key, required Widget child}) => AnimatedSwitcher(
      duration: const Duration(milliseconds: 260),
      transitionBuilder: (c, a) => FadeTransition(opacity: a, child: SlideTransition(position: Tween(begin: const Offset(0, 0.02), end: Offset.zero).animate(a), child: c)),
      child: KeyedSubtree(key: ValueKey(key), child: child),
    );

/// 말 달리기 로티 대용: 트랙 위를 달리는 마커(결과 없음/빈 상태 등에 사용).
class GallopTrack extends StatefulWidget {
  final double height;
  const GallopTrack({super.key, this.height = 48});
  @override
  State<GallopTrack> createState() => _GallopState();
}

class _GallopState extends State<GallopTrack> with SingleTickerProviderStateMixin {
  late final AnimationController _c = AnimationController(vsync: this, duration: const Duration(milliseconds: 2400))..repeat();
  @override
  void dispose() { _c.dispose(); super.dispose(); }
  @override
  Widget build(BuildContext context) => SizedBox(height: widget.height, child: AnimatedBuilder(
        animation: _c,
        builder: (_, __) => CustomPaint(painter: _TrackPainter(_c.value, AppColors.turf, AppColors.line, AppColors.tan)),
      ));
}

class _TrackPainter extends CustomPainter {
  final double t; final Color turf, line, tan;
  _TrackPainter(this.t, this.turf, this.line, this.tan);
  @override
  void paint(Canvas canvas, Size s) {
    final y = s.height * 0.7;
    canvas.drawLine(Offset(0, y), Offset(s.width, y), Paint()..color = line..strokeWidth = 2);
    for (var i = 0; i < 12; i++) {
      final x = ((i / 12 + t) % 1) * s.width;
      canvas.drawLine(Offset(x, y - 3), Offset(x, y + 3), Paint()..color = line..strokeWidth = 1);
    }
    final x = Curves.easeInOut.transform(t) * (s.width - 24) + 12;
    final bob = math.sin(t * math.pi * 8).abs() * 3;
    final body = Rect.fromCenter(center: Offset(x, y - 10 - bob), width: 20, height: 10);
    canvas.drawRRect(RRect.fromRectAndRadius(body, const Radius.circular(5)), Paint()..color = turf);
    canvas.drawCircle(Offset(x + 12, y - 15 - bob), 4, Paint()..color = tan);
  }
  @override
  bool shouldRepaint(covariant _TrackPainter o) => o.t != t;
}
