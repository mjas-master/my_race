import 'package:flutter/material.dart';

/// KRA 마번 안장색 단일 소스. 1~14번.
class GateColors {
  static const _bg = <int, Color>{
    1: Color(0xFFFFFFFF), 2: Color(0xFF1B1B1B), 3: Color(0xFFD42A2A), 4: Color(0xFF2457C5), 5: Color(0xFFF2C400),
    6: Color(0xFF2E8B57), 7: Color(0xFFF07C1C), 8: Color(0xFFF29BC0), 9: Color(0xFF7A3EB1), 10: Color(0xFF9A9A9A),
    11: Color(0xFF7ED957), 12: Color(0xFFC51E7A), 13: Color(0xFF8B5A2B), 14: Color(0xFF00B5CC),
  };
  static const _lightFg = {1, 5, 8, 11, 14};

  static Color bg(int gate) => _bg[gate] ?? const Color(0xFFDDDDDD);
  static Color fg(int gate) => _lightFg.contains(gate) ? Colors.black : Colors.white;
}

class GateBadge extends StatelessWidget {
  final int gate;
  final double size;
  const GateBadge(this.gate, {super.key, this.size = 24});

  @override
  Widget build(BuildContext context) => Container(
        width: size, height: size, alignment: Alignment.center,
        decoration: BoxDecoration(
          color: GateColors.bg(gate), borderRadius: BorderRadius.circular(size * 0.22),
          border: Border.all(color: Colors.black.withOpacity(0.15)),
        ),
        child: Text('$gate', style: TextStyle(fontSize: size * 0.48, fontWeight: FontWeight.w700, color: GateColors.fg(gate))),
      );
}
