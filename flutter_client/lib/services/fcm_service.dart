import 'package:firebase_messaging/firebase_messaging.dart';
import 'package:flutter/foundation.dart';
import '../core/api_client.dart';

class FCMService {
  static final FirebaseMessaging _messaging = FirebaseMessaging.instance;

  static Future<void> initialize() async {
    // Request permissions for iOS
    NotificationSettings settings = await _messaging.requestPermission(
      alert: true,
      badge: true,
      sound: true,
    );

    if (settings.authorizationStatus == AuthorizationStatus.authorized) {
      print('User granted permission');
      await _registerDeviceToken();
    } else {
      print('User declined or has not accepted permission');
    }

    // Handle token refresh
    _messaging.onTokenRefresh.listen((String token) {
      print("FCM Token refreshed: $token");
      ApiClient.registerDeviceToken(token, _getPlatform());
    });
  }

  static Future<void> _registerDeviceToken() async {
    try {
      String? token = await _messaging.getToken();
      if (token != null) {
        print("FCM Token: $token");
        await ApiClient.registerDeviceToken(token, _getPlatform());
      }
    } catch (e) {
      print("Error getting FCM token: $e");
    }
  }

  static String _getPlatform() {
    if (kIsWeb) return 'web';
    if (defaultTargetPlatform == TargetPlatform.iOS) return 'ios';
    if (defaultTargetPlatform == TargetPlatform.android) return 'android';
    return 'unknown';
  }
}
