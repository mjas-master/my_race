import 'package:dio/dio.dart';

class ApiException implements Exception {
  final int status;
  final String code;
  final String message;
  ApiException(this.status, this.code, this.message);
  bool get premiumRequired => code == 'PREMIUM_REQUIRED';
  bool get adultRequired => code == 'ADULT_NOT_CONFIRMED';
  @override
  String toString() => '$code: $message';
}

/// 04_api_spec.md 공통 규칙: X-Device-Id 헤더, 에러 포맷 매핑.
class ApiClient {
  static const baseUrl = String.fromEnvironment('API_BASE_URL', defaultValue: 'http://10.0.2.2:8080');
  final Dio _dio;

  ApiClient(String deviceId)
      : _dio = Dio(BaseOptions(
          baseUrl: '$baseUrl/v1',
          connectTimeout: const Duration(seconds: 8),
          receiveTimeout: const Duration(seconds: 12),
          headers: {'X-Device-Id': deviceId, 'Accept': 'application/json'},
        ));

  Future<Map<String, dynamic>> get(String path, {Map<String, dynamic>? query}) async =>
      _run(() => _dio.get(path, queryParameters: query?..removeWhere((_, v) => v == null)));

  Future<Map<String, dynamic>> post(String path, Map<String, dynamic> body) async =>
      _run(() => _dio.post(path, data: body));

  Future<Map<String, dynamic>> _run(Future<Response> Function() call) async {
    try {
      final res = await call();
      return (res.data as Map).cast<String, dynamic>();
    } on DioException catch (e) {
      final data = e.response?.data;
      if (data is Map && data['code'] != null) {
        throw ApiException(e.response?.statusCode ?? 0, data['code'] as String, data['message']?.toString() ?? '');
      }
      if (e.type == DioExceptionType.connectionTimeout || e.type == DioExceptionType.connectionError) {
        throw ApiException(0, 'NETWORK', '서버에 연결할 수 없습니다. 네트워크를 확인하세요.');
      }
      throw ApiException(e.response?.statusCode ?? 0, 'UNKNOWN', '요청을 처리하지 못했습니다.');
    }
  }
}
