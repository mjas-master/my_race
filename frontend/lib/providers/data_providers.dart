import 'package:flutter_riverpod/flutter_riverpod.dart';

import '../core/network/api_client.dart';
import '../models/models.dart';
import 'settings_provider.dart';

final apiClientProvider = Provider<ApiClient>((ref) => ApiClient(ref.watch(settingsProvider.select((s) => s.deviceId))));

/// 기기 등록. 온보딩 완료 후 및 설정 변경 시 호출.
final deviceRegisterProvider = FutureProvider<DeviceInfo>((ref) async {
  final s = ref.watch(settingsProvider);
  final api = ref.watch(apiClientProvider);
  final j = await api.post('/devices', {
    'deviceId': s.deviceId, 'platform': 'ANDROID', 'adultConfirmed': s.adultConfirmed,
    'preferredMeet': s.preferredMeet, 'prefs': s.prefsJson,
  });
  return DeviceInfo.fromJson(j);
});

final premiumProvider = Provider<bool>((ref) => ref.watch(deviceRegisterProvider).valueOrNull?.premium ?? false);
final adsEnabledProvider = Provider<bool>((ref) {
  final d = ref.watch(deviceRegisterProvider).valueOrNull;
  return (d?.adsEnabled ?? true) && !(d?.premium ?? false);
});

typedef MeetDate = ({int? meet, String date});

final raceListProvider = FutureProvider.family<List<RaceSummary>, MeetDate>((ref, q) async {
  final j = await ref.watch(apiClientProvider).get('/races', query: {'meet': q.meet, 'date': q.date});
  return (j['races'] as List).map((e) => RaceSummary.fromJson(e)).toList();
});

/// 과거조회용: 경주가 있는 날짜 목록(최근 1년 + 향후 1주). 캘린더에서 선택 가능일 표시.
final raceDatesProvider = FutureProvider.family<Set<String>, int?>((ref, meet) async {
  final j = await ref.watch(apiClientProvider).get('/races/dates', query: {'meet': meet});
  return (j['dates'] as List).map((e) => '$e').toSet();
});

final raceDetailProvider = FutureProvider.family<RaceDetail, int>((ref, id) async {
  ref.watch(premiumProvider); // 프리미엄 상태 바뀌면 재조회
  return RaceDetail.fromJson(await ref.watch(apiClientProvider).get('/races/$id'));
});

final resultsProvider = FutureProvider.family<List<RaceResultView>, MeetDate>((ref, q) async {
  final j = await ref.watch(apiClientProvider).get('/results', query: {'meet': q.meet, 'date': q.date});
  return (j['races'] as List).map((e) => RaceResultView.fromJson(e)).toList();
});

final hitSummaryProvider = FutureProvider.family<HitSummary, ({int? meet, int weeks})>((ref, q) async =>
    HitSummary.fromJson(await ref.watch(apiClientProvider).get('/hits/summary', query: {'meet': q.meet, 'weeks': q.weeks})));

final horseProvider = FutureProvider.family<HorseDetail, String>((ref, hrNo) async {
  ref.watch(premiumProvider);
  return HorseDetail.fromJson(await ref.watch(apiClientProvider).get('/horses/$hrNo'));
});
final jockeyProvider = FutureProvider.family<PersonDetail, String>((ref, no) async =>
    PersonDetail.fromJson(await ref.watch(apiClientProvider).get('/jockeys/$no')));
final trainerProvider = FutureProvider.family<PersonDetail, String>((ref, no) async =>
    PersonDetail.fromJson(await ref.watch(apiClientProvider).get('/trainers/$no')));
