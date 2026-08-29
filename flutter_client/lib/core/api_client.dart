import 'dart:convert';
import 'package:http/http.dart' as http;
import 'package:shared_preferences/shared_preferences.dart';

class ApiClient {
  // Use nip.io domain for production backend
  static const String baseUrl = 'https://13.60.227.174.nip.io';

  /// Exchanges a Firebase ID token for a FastAPI JWT
  static Future<bool> verifyFirebasePhone(String firebaseIdToken) async {
    try {
      final response = await http.post(
        Uri.parse('$baseUrl/auth/verify-firebase-phone'),
        headers: {'Content-Type': 'application/json'},
        body: jsonEncode({'firebase_id_token': firebaseIdToken}),
      );

      if (response.statusCode == 200) {
        final data = jsonDecode(response.body);
        final accessToken = data['access_token'];
        if (accessToken != null) {
          // Store token securely
          final prefs = await SharedPreferences.getInstance();
          await prefs.setString('jwt_token', accessToken);
          return true;
        }
      } else {
        print('Backend verification failed: ${response.body}');
      }
    } catch (e) {
      print('Network error verifying firebase token: $e');
    }
    return false;
  }

  static Future<Map<String, String>> _getHeaders() async {
    final prefs = await SharedPreferences.getInstance();
    final token = prefs.getString('jwt_token');
    return {
      'Content-Type': 'application/json',
      if (token != null) 'Authorization': 'Bearer $token',
    };
  }

  /// Upserts the donor's location (requires verified_donor role)
  static Future<bool> updateLocation(double lat, double lon) async {
    try {
      final response = await http.put(
        Uri.parse('$baseUrl/api/donors/me/location'),
        headers: await _getHeaders(),
        body: jsonEncode({
          'latitude': lat,
          'longitude': lon,
        }),
      );
      return response.statusCode == 200;
    } catch (e) {
      print('Network error updating location: $e');
    }
    return false;
  }

  /// Creates a blood request and returns its ID
  static Future<String?> createRequest(Map<String, dynamic> data) async {
    try {
      final response = await http.post(
        Uri.parse('$baseUrl/api/requests'),
        headers: await _getHeaders(),
        body: jsonEncode(data),
      );
      if (response.statusCode == 200 || response.statusCode == 201) {
        final body = jsonDecode(response.body);
        return body['request_id'];
      }
    } catch (e) {
      print('Network error creating request: $e');
    }
    return null;
  }

  /// Fetches eligible donors for a request within radius
  static Future<List<dynamic>> getEligibleDonors(String requestId, {double radiusKm = 10.0}) async {
    try {
      final response = await http.get(
        Uri.parse('$baseUrl/api/requests/$requestId/eligible-donors?radius_km=$radiusKm'),
        headers: await _getHeaders(),
      );
      if (response.statusCode == 200) {
        return jsonDecode(response.body) as List<dynamic>;
      }
    } catch (e) {
      print('Network error fetching donors: $e');
    }
    return [];
  }

  /// Uploads hospital slip with OCR results
  static Future<bool> uploadHospitalSlip(String requestId, String filePath, String ocrText, int ocrConfidence) async {
    try {
      final headers = await _getHeaders();
      var request = http.MultipartRequest('POST', Uri.parse('$baseUrl/api/requests/$requestId/hospital-slip'));
      if (headers.containsKey('Authorization')) {
        request.headers['Authorization'] = headers['Authorization']!;
      }
      
      request.files.add(await http.MultipartFile.fromPath('file', filePath));
      request.fields['ocr_text'] = ocrText;
      request.fields['ocr_confidence'] = ocrConfidence.toString();
      
      var response = await request.send();
      return response.statusCode == 200;
    } catch (e) {
      print('Network error uploading slip: $e');
    }
    return false;
  }

  static Future<bool> registerDeviceToken(String token, String platform) async {
    try {
      final response = await http.post(
        Uri.parse('$baseUrl/api/donors/me/device-token'),
        headers: await _getHeaders(),
        body: jsonEncode({
          'token': token,
          'platform': platform,
        }),
      );
      return response.statusCode == 200;
    } catch (e) {
      print('Token registration error: $e');
      return false;
    }
  }

  static Future<String?> fetchContactNumber(String requestId, String targetUserId) async {
    try {
      final response = await http.get(
        Uri.parse('$baseUrl/api/requests/$requestId/contact/$targetUserId'),
        headers: await _getHeaders(),
      );
      if (response.statusCode == 200) {
        final data = jsonDecode(response.body);
        return data['phone_number'];
      }
      return null;
    } catch (e) {
      print('Contact fetch error: $e');
      return null;
    }
  }

  /// Uploads CNIC document (FRONT or BACK)
  static Future<bool> uploadCnicDocument(String filePath, String documentKind) async {
    try {
      final headers = await _getHeaders();
      var request = http.MultipartRequest('POST', Uri.parse('$baseUrl/api/donors/me/cnic-document'));
      if (headers.containsKey('Authorization')) {
        request.headers['Authorization'] = headers['Authorization']!;
      }
      
      request.files.add(await http.MultipartFile.fromPath('file', filePath));
      request.fields['document_kind'] = documentKind;
      
      var response = await request.send();
      return response.statusCode == 200;
    } catch (e) {
      print('Network error uploading CNIC: $e');
    }
    return false;
  }

  // --- Admin API Methods ---

  static Future<bool> adminLogin(String email, String password, String totpCode) async {
    try {
      final response = await http.post(
        Uri.parse('$baseUrl/api/admin-login'),
        headers: {'Content-Type': 'application/json'},
        body: jsonEncode({
          'email': email,
          'password': password,
          'totp_code': totpCode,
        }),
      );
      if (response.statusCode == 200) {
        final data = jsonDecode(response.body);
        final token = data['access_token'];
        if (token != null) {
          final prefs = await SharedPreferences.getInstance();
          await prefs.setString('jwt_token', token);
        }
        return true;
      }
      return false;
    } catch (e) {
      print('Admin login error: $e');
      return false;
    }
  }

  static Future<List<dynamic>> fetchVerificationQueue() async {
    try {
      final response = await http.get(
        Uri.parse('$baseUrl/api/requests/queue'),
        headers: await _getHeaders(),
      );
      if (response.statusCode == 200) {
        return jsonDecode(response.body) as List<dynamic>;
      }
      return [];
    } catch (e) {
      print('Fetch verification queue error: $e');
      return [];
    }
  }

  static Future<List<dynamic>> fetchFraudAudits() async {
    try {
      final response = await http.get(
        Uri.parse('$baseUrl/api/requests/fraud-audits'),
        headers: await _getHeaders(),
      );
      if (response.statusCode == 200) {
        return jsonDecode(response.body) as List<dynamic>;
      }
      return [];
    } catch (e) {
      print('Fetch fraud audits error: $e');
      return [];
    }
  }

  static Future<List<dynamic>> fetchCampusDrives() async {
    try {
      final response = await http.get(
        Uri.parse('$baseUrl/api/drives/'),
        headers: await _getHeaders(),
      );
      if (response.statusCode == 200) {
        return jsonDecode(response.body) as List<dynamic>;
      }
      return [];
    } catch (e) {
      print('Fetch campus drives error: $e');
      return [];
    }
  }

  // --- Final Migration Modules ---

  static Future<List<dynamic>> fetchAwarenessArticles() async {
    try {
      final response = await http.get(
        Uri.parse('$baseUrl/api/awareness/'),
        headers: await _getHeaders(),
      );
      if (response.statusCode == 200) {
        return jsonDecode(response.body) as List<dynamic>;
      }
      return [];
    } catch (e) {
      print('Fetch awareness articles error: $e');
      return [];
    }
  }

  static Future<List<dynamic>> fetchMyRequests() async {
    try {
      final response = await http.get(
        Uri.parse('$baseUrl/api/requests/me'),
        headers: await _getHeaders(),
      );
      if (response.statusCode == 200) {
        return jsonDecode(response.body) as List<dynamic>;
      }
      return [];
    } catch (e) {
      print('Fetch my requests error: $e');
      return [];
    }
  }

  static Future<List<dynamic>> fetchRequestFeed() async {
    try {
      final response = await http.get(
        Uri.parse('$baseUrl/api/requests/feed'),
        headers: await _getHeaders(),
      );
      if (response.statusCode == 200) {
        return jsonDecode(response.body) as List<dynamic>;
      }
      return [];
    } catch (e) {
      print('Fetch request feed error: $e');
      return [];
    }
  }

  static Future<bool> submitPreScreening(Map<String, bool> answers) async {
    try {
      final response = await http.post(
        Uri.parse('$baseUrl/api/prescreening/me'),
        headers: await _getHeaders(),
        body: jsonEncode(answers),
      );
      return response.statusCode == 200;
    } catch (e) {
      print('Submit pre-screening error: $e');
      return false;
    }
  }

  static Future<bool> scheduleCampusDrive(Map<String, dynamic> driveData) async {
    try {
      final response = await http.post(
        Uri.parse('$baseUrl/api/drives/'),
        headers: await _getHeaders(),
        body: jsonEncode(driveData),
      );
      return response.statusCode == 200;
    } catch (e) {
      print('Schedule campus drive error: $e');
      return false;
    }
  }

  static Future<bool> registerForDrive(String driveId, Map<String, dynamic> attendeeData) async {
    try {
      final response = await http.post(
        Uri.parse('$baseUrl/api/drives/$driveId/attendees'),
        headers: await _getHeaders(),
        body: jsonEncode(attendeeData),
      );
      return response.statusCode == 200;
    } catch (e) {
      print('Register for drive error: $e');
      return false;
    }
  }

  static Future<bool> checkInAttendee(String attendeeId) async {
    try {
      final response = await http.post(
        Uri.parse('$baseUrl/api/drives/check-in/$attendeeId'),
        headers: await _getHeaders(),
      );
      return response.statusCode == 200;
    } catch (e) {
      print('Check-in attendee error: $e');
      return false;
    }
  }

  static Future<bool> submitFeedback(String requestId, int rating, String note) async {
    try {
      final response = await http.post(
        Uri.parse('$baseUrl/api/feedback/$requestId'),
        headers: await _getHeaders(),
        body: jsonEncode({'rating': rating, 'note': note}),
      );
      return response.statusCode == 200;
    } catch (e) {
      print('Submit feedback error: $e');
      return false;
    }
  }

  static Future<Map<String, dynamic>?> fetchLatestVersion() async {
    try {
      final response = await http.get(
        Uri.parse('$baseUrl/api/system/version'),
        // No auth required for this endpoint ideally, but we'll use base headers just in case
        headers: {'Content-Type': 'application/json'},
      );
      if (response.statusCode == 200) {
        return jsonDecode(response.body) as Map<String, dynamic>;
      }
    } catch (e) {
      print('Fetch latest version error: $e');
    }
    return null;
  }
}
