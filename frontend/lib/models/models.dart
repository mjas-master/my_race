/// API 응답 모델. 04_api_spec.md 와 1:1. 수동 fromJson(코드 생성 의존 없음).
library;

String meetName(int m) => switch (m) { 1 => '서울', 2 => '제주', 3 => '부경', _ => '?' };

int _i(dynamic v, [int d = 0]) => v == null ? d : (v is int ? v : int.tryParse('$v') ?? d);
double? _d(dynamic v) => v == null ? null : (v is num ? v.toDouble() : double.tryParse('$v'));
String _s(dynamic v, [String d = '']) => v?.toString() ?? d;

class TopPick {
  final int gateNo; final String horseName; final String hrNo; final double score;
  TopPick.fromJson(Map<String, dynamic> j)
      : gateNo = _i(j['gateNo']), horseName = _s(j['horseName']), hrNo = _s(j['hrNo']), score = _d(j['score']) ?? 0;
}

class RaceSummary {
  final int id, meet, raceNo, distance, entryCount;
  final String meetName, track, grade, status;
  final String? startTime;
  final TopPick? topPick;
  RaceSummary.fromJson(Map<String, dynamic> j)
      : id = _i(j['id']), meet = _i(j['meet']), meetName = _s(j['meetName']), raceNo = _i(j['raceNo']), distance = _i(j['distance']),
        entryCount = _i(j['entryCount']), track = _s(j['track']), grade = _s(j['grade']), status = _s(j['status']),
        startTime = j['startTime'] == null ? null : _s(j['startTime']).substring(0, 5),
        topPick = j['topPick'] == null ? null : TopPick.fromJson(j['topPick']);
  bool get fixed => status == 'ENTRIES_FIXED' || status == 'FINISHED';
}

class EntryResult {
  final int finishPos; final String? finishTime; final double? winOdds, placeOdds;
  EntryResult.fromJson(Map<String, dynamic> j)
      : finishPos = _i(j['finishPos']), finishTime = j['finishTime']?.toString(), winOdds = _d(j['winOdds']), placeOdds = _d(j['placeOdds']);
}

class EntryView {
  final int? rank; final int gateNo; final String hrNo, horseName, jkNo, jockeyName, trNo, trainerName;
  final double weightCarried; final double? horseWeight, score; final bool scratched, locked;
  final Map<String, String>? factors; final EntryResult? result;
  EntryView.fromJson(Map<String, dynamic> j)
      : rank = j['rank'] == null ? null : _i(j['rank']), gateNo = _i(j['gateNo']), hrNo = _s(j['hrNo']), horseName = _s(j['horseName']),
        jkNo = _s(j['jkNo']), jockeyName = _s(j['jockeyName']), trNo = _s(j['trNo']), trainerName = _s(j['trainerName']),
        weightCarried = _d(j['weightCarried']) ?? 0, horseWeight = _d(j['horseWeight']), score = _d(j['score']),
        scratched = j['scratched'] == true, locked = j['locked'] == true,
        factors = j['factors'] == null ? null : (j['factors'] as Map).map((k, v) => MapEntry('$k', '$v')),
        result = j['result'] == null ? null : EntryResult.fromJson(j['result']);
}

class HitFlags {
  final bool winHit, placeHit, showHit;
  HitFlags.fromJson(Map<String, dynamic> j) : winHit = j['winHit'] == true, placeHit = j['placeHit'] == true, showHit = j['showHit'] == true;
  String get label => winHit ? '1착 적중' : placeHit ? '복승 적중' : showHit ? '3착 내' : '미적중';
}

class RaceDetail {
  final int id, meet, raceNo, distance, freeRankLimit; final String meetName, date, track, grade, status, disclaimer;
  final String? startTime, engineVersion; final bool premium; final List<EntryView> entries; final HitFlags? hit;
  RaceDetail.fromJson(Map<String, dynamic> j)
      : id = _i(j['id']), meet = _i(j['meet']), meetName = _s(j['meetName']), date = _s(j['date']), raceNo = _i(j['raceNo']),
        distance = _i(j['distance']), track = _s(j['track']), grade = _s(j['grade']), status = _s(j['status']),
        startTime = j['startTime'] == null ? null : _s(j['startTime']).substring(0, 5), engineVersion = j['engineVersion']?.toString(),
        premium = j['premium'] == true, freeRankLimit = _i(j['freeRankLimit'], 3),
        entries = (j['entries'] as List? ?? []).map((e) => EntryView.fromJson(e)).toList(),
        hit = j['hit'] == null ? null : HitFlags.fromJson(j['hit']), disclaimer = _s(j['disclaimer']);
  bool get finished => status == 'FINISHED';
}

class ResultTop {
  final int finishPos, gateNo; final String horseName; final String? finishTime; final double? winOdds, placeOdds;
  ResultTop.fromJson(Map<String, dynamic> j)
      : finishPos = _i(j['finishPos']), gateNo = _i(j['gateNo']), horseName = _s(j['horseName']), finishTime = j['finishTime']?.toString(),
        winOdds = _d(j['winOdds']), placeOdds = _d(j['placeOdds']);
}

class RaceResultView {
  final int id, raceNo, distance; final HitFlags? hit; final TopPick? ourTopPick; final List<ResultTop> top3;
  RaceResultView.fromJson(Map<String, dynamic> j)
      : id = _i(j['id']), raceNo = _i(j['raceNo']), distance = _i(j['distance']),
        hit = j['hit'] == null ? null : HitFlags.fromJson(j['hit']), ourTopPick = j['ourTopPick'] == null ? null : TopPick.fromJson(j['ourTopPick']),
        top3 = (j['top3'] as List? ?? []).map((e) => ResultTop.fromJson(e)).toList();
}

class MeetHit { final int meet, races; final String meetName; final double winRate;
  MeetHit.fromJson(Map<String, dynamic> j) : meet = _i(j['meet']), races = _i(j['races']), meetName = _s(j['meetName']), winRate = _d(j['winRate']) ?? 0; }
class WeekHit { final String weekStart; final int races; final double winRate;
  WeekHit.fromJson(Map<String, dynamic> j) : weekStart = _s(j['weekStart']), races = _i(j['races']), winRate = _d(j['winRate']) ?? 0; }

class HitSummary {
  final String from, to, engineVersion; final int races; final double winRate, placeRate, showRate;
  final List<MeetHit> byMeet; final List<WeekHit> weekly; final bool detailLocked;
  HitSummary.fromJson(Map<String, dynamic> j)
      : from = _s(j['from']), to = _s(j['to']), engineVersion = _s(j['engineVersion']), races = _i(j['races']),
        winRate = _d(j['winRate']) ?? 0, placeRate = _d(j['placeRate']) ?? 0, showRate = _d(j['showRate']) ?? 0,
        byMeet = (j['byMeet'] as List? ?? []).map((e) => MeetHit.fromJson(e)).toList(),
        weekly = (j['weekly'] as List? ?? []).map((e) => WeekHit.fromJson(e)).toList(), detailLocked = j['detailLocked'] == true;
}

class HorseRun {
  final String date, jockeyName, jkNo; final int meet, raceNo, distance, finishPos, fieldSize; final String? finishTime; final double? winOdds;
  HorseRun.fromJson(Map<String, dynamic> j)
      : date = _s(j['date']), meet = _i(j['meet']), raceNo = _i(j['raceNo']), distance = _i(j['distance']), finishPos = _i(j['finishPos']),
        fieldSize = _i(j['fieldSize'], 10), finishTime = j['finishTime']?.toString(), jockeyName = _s(j['jockeyName']), jkNo = _s(j['jkNo']), winOdds = _d(j['winOdds']);
}
class DistanceStat { final int distance, starts, wins, seconds;
  DistanceStat.fromJson(Map<String, dynamic> j) : distance = _i(j['distance']), starts = _i(j['starts']), wins = _i(j['wins']), seconds = _i(j['seconds']); }

class HorseDetail {
  final String hrNo, name, country, sex; final int age, rating, starts, wins, seconds, thirds, runsLimit;
  final List<DistanceStat> byDistance; final List<HorseRun> recentRuns; final bool runsLocked;
  HorseDetail.fromJson(Map<String, dynamic> j)
      : hrNo = _s(j['hrNo']), name = _s(j['name']), country = _s(j['country']), sex = _s(j['sex']), age = _i(j['age']), rating = _i(j['rating']),
        starts = _i(j['career']?['starts']), wins = _i(j['career']?['wins']), seconds = _i(j['career']?['seconds']), thirds = _i(j['career']?['thirds']),
        byDistance = (j['byDistance'] as List? ?? []).map((e) => DistanceStat.fromJson(e)).toList(),
        recentRuns = (j['recentRuns'] as List? ?? []).map((e) => HorseRun.fromJson(e)).toList(),
        runsLocked = j['runsLocked'] == true, runsLimit = _i(j['runsLimit'], 3);
}

class SeasonStat { final int starts, wins; final double winRate, placeRate;
  SeasonStat.fromJson(Map<String, dynamic>? j) : starts = _i(j?['starts']), wins = _i(j?['wins']), winRate = _d(j?['winRate']) ?? 0, placeRate = _d(j?['placeRate']) ?? 0; }
class WeekRate { final String weekStart; final int starts; final double rate;
  WeekRate.fromJson(Map<String, dynamic> j) : weekStart = _s(j['weekStart']), starts = _i(j['starts']), rate = _d(j['rate']) ?? 0; }
class UpcomingEntry { final int raceId, raceNo, gateNo; final int? ourRank; final String date, horseName, hrNo;
  UpcomingEntry.fromJson(Map<String, dynamic> j)
      : raceId = _i(j['raceId']), raceNo = _i(j['raceNo']), gateNo = _i(j['gateNo']), ourRank = j['ourRank'] == null ? null : _i(j['ourRank']),
        date = _s(j['date']), horseName = _s(j['horseName']), hrNo = _s(j['hrNo']); }

class PersonDetail {
  final String no, name, meetName; final int meet; final int? debutYear; final SeasonStat season, last30d;
  final List<WeekRate> weeklyWinRate; final List<UpcomingEntry> thisWeekEntries;
  PersonDetail.fromJson(Map<String, dynamic> j)
      : no = _s(j['no']), name = _s(j['name']), meet = _i(j['meet']), meetName = _s(j['meetName']),
        debutYear = j['debutYear'] == null ? null : _i(j['debutYear']), season = SeasonStat.fromJson(j['season']), last30d = SeasonStat.fromJson(j['last30d']),
        weeklyWinRate = (j['weeklyWinRate'] as List? ?? []).map((e) => WeekRate.fromJson(e)).toList(),
        thisWeekEntries = (j['thisWeekEntries'] as List? ?? []).map((e) => UpcomingEntry.fromJson(e)).toList();
}

class DeviceInfo {
  final bool premium, adsEnabled; final int preferredMeet, freeRankLimit; final String? premiumUntil;
  DeviceInfo.fromJson(Map<String, dynamic> j)
      : premium = j['premium'] == true, adsEnabled = j['adsEnabled'] != false, preferredMeet = _i(j['preferredMeet'], 1),
        freeRankLimit = _i(j['freeRankLimit'], 3), premiumUntil = j['premiumUntil']?.toString();
}
