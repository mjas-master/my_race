import 'package:flutter/material.dart' show ThemeMode;
import 'package:flutter_riverpod/flutter_riverpod.dart';
import 'package:shared_preferences/shared_preferences.dart';
import 'package:uuid/uuid.dart';

final sharedPrefsProvider = Provider<SharedPreferences>((_) => throw UnimplementedError('main 에서 override'));

class AppSettings {
  final String deviceId;
  final bool adultConfirmed;
  final int preferredMeet;
  final bool notifyEntries, notifyRankUpdate, notifyPreRace;
  final bool onboarded;
  /// 화면 모드: system(기본, 시스템 설정 따라감) / light / dark
  final ThemeMode themeMode;
  /// 애니메이션 효과 켜기(저사양·접근성용 오프 스위치)
  final bool animations;

  const AppSettings({
    required this.deviceId, this.adultConfirmed = false, this.preferredMeet = 1,
    this.notifyEntries = true, this.notifyRankUpdate = true, this.notifyPreRace = false, this.onboarded = false,
    this.themeMode = ThemeMode.system, this.animations = true,
  });

  AppSettings copyWith({bool? adultConfirmed, int? preferredMeet, bool? notifyEntries, bool? notifyRankUpdate, bool? notifyPreRace, bool? onboarded,
      ThemeMode? themeMode, bool? animations}) =>
      AppSettings(
        deviceId: deviceId, adultConfirmed: adultConfirmed ?? this.adultConfirmed, preferredMeet: preferredMeet ?? this.preferredMeet,
        notifyEntries: notifyEntries ?? this.notifyEntries, notifyRankUpdate: notifyRankUpdate ?? this.notifyRankUpdate,
        notifyPreRace: notifyPreRace ?? this.notifyPreRace, onboarded: onboarded ?? this.onboarded,
        themeMode: themeMode ?? this.themeMode, animations: animations ?? this.animations,
      );

  Map<String, dynamic> get prefsJson => {'notifyEntries': notifyEntries, 'notifyRankUpdate': notifyRankUpdate, 'notifyPreRace': notifyPreRace};
}

class SettingsNotifier extends StateNotifier<AppSettings> {
  final SharedPreferences _p;
  SettingsNotifier(this._p) : super(_load(_p));

  static AppSettings _load(SharedPreferences p) {
    var id = p.getString('deviceId');
    if (id == null) {
      const fromEnv = String.fromEnvironment('DEVICE_ID');
      id = fromEnv.isNotEmpty ? fromEnv : const Uuid().v4();
      p.setString('deviceId', id);
    }
    return AppSettings(
      deviceId: id, adultConfirmed: p.getBool('adultConfirmed') ?? false, preferredMeet: p.getInt('preferredMeet') ?? 1,
      notifyEntries: p.getBool('notifyEntries') ?? true, notifyRankUpdate: p.getBool('notifyRankUpdate') ?? true,
      notifyPreRace: p.getBool('notifyPreRace') ?? false, onboarded: p.getBool('onboarded') ?? false,
      themeMode: ThemeMode.values.firstWhere((m) => m.name == p.getString('themeMode'), orElse: () => ThemeMode.system),
      animations: p.getBool('animations') ?? true,
    );
  }

  Future<void> update(AppSettings s) async {
    state = s;
    await Future.wait([
      _p.setBool('adultConfirmed', s.adultConfirmed), _p.setInt('preferredMeet', s.preferredMeet),
      _p.setBool('notifyEntries', s.notifyEntries), _p.setBool('notifyRankUpdate', s.notifyRankUpdate),
      _p.setBool('notifyPreRace', s.notifyPreRace), _p.setBool('onboarded', s.onboarded),
      _p.setString('themeMode', s.themeMode.name), _p.setBool('animations', s.animations),
    ]);
  }
}

final settingsProvider = StateNotifierProvider<SettingsNotifier, AppSettings>((ref) => SettingsNotifier(ref.watch(sharedPrefsProvider)));
