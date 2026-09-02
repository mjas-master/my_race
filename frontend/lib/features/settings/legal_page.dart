import 'package:flutter/material.dart';

import '../../core/theme/app_theme.dart';

/// S12 데이터 출처 · 면책. 01_requirements §5 금지어(공인/제휴/공식) 사용 금지.
class LegalPage extends StatelessWidget {
  const LegalPage({super.key});

  static const sections = [
    ('데이터 출처', '경주 편성·출마표·결과·배당 정보는 공공데이터포털에 공개된 한국마사회 OpenAPI를 이용합니다. 본 앱은 한국마사회와 제휴·공인 관계가 없습니다.'),
    ('예측 정보', '추천 순위는 과거 기록을 통계적으로 점수화한 참고 정보입니다. 결과를 보장하지 않으며, 이용에 따른 손실에 대해 책임지지 않습니다.'),
    ('이용 제한', '만 19세 이상만 이용할 수 있습니다. 본 앱은 마권 구매·배팅 대행·사설 경마 정보를 제공하지 않습니다.'),
    ('데이터 지연', '공공데이터 갱신 시점에 따라 실제와 차이가 있을 수 있습니다. 화면의 갱신 시각을 확인하세요.'),
    ('개인정보', '회원가입 없이 익명의 기기 식별자만 사용합니다. 구독 정보는 스토어 영수증만 보관합니다. 상세 내용은 개인정보 처리방침(C02)에서 확인할 수 있습니다.'),
  ];

  @override
  Widget build(BuildContext context) => Scaffold(
        appBar: AppBar(title: const Text('데이터 출처와 면책')),
        body: ListView(padding: const EdgeInsets.all(20), children: [
          for (final (t, b) in sections) Padding(padding: const EdgeInsets.only(bottom: 18), child: Column(crossAxisAlignment: CrossAxisAlignment.start, children: [
            Text(t, style: const TextStyle(fontSize: 14, fontWeight: FontWeight.w700)),
            const SizedBox(height: 4),
            Text(b, style: TextStyle(fontSize: 13, height: 1.75, color: AppColors.ink2)),
          ])),
        ]),
      );
}
