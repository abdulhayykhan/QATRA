import 'package:flutter/material.dart';
import 'package:firebase_core/firebase_core.dart';
import 'package:firebase_messaging/firebase_messaging.dart';
import 'package:qatra_flutter/core/theme.dart';
import 'package:qatra_flutter/screens/phone_verification_screen.dart';
import 'package:qatra_flutter/screens/geo_alert_modal.dart';
import 'package:qatra_flutter/services/fcm_service.dart';
import 'package:package_info_plus/package_info_plus.dart';
import 'package:url_launcher/url_launcher.dart';
import 'package:qatra_flutter/core/api_client.dart';
// Top-level background message handler
@pragma('vm:entry-point')
Future<void> _firebaseMessagingBackgroundHandler(RemoteMessage message) async {
  await Firebase.initializeApp();
  print("Handling a background message: ${message.messageId}");
}

void main() async {
  WidgetsFlutterBinding.ensureInitialized();
  await Firebase.initializeApp();
  FirebaseMessaging.onBackgroundMessage(_firebaseMessagingBackgroundHandler);
  
  // Initialize FCM service to request permissions and save token
  await FCMService.initialize();
  
  runApp(const QatraApp());
}

class QatraApp extends StatefulWidget {
  const QatraApp({super.key});

  @override
  _QatraAppState createState() => _QatraAppState();
}

class _QatraAppState extends State<QatraApp> {
  final GlobalKey<NavigatorState> _navigatorKey = GlobalKey<NavigatorState>();

  @override
  void initState() {
    super.initState();
    _setupInteractedMessage();
    _checkForUpdates();
  }

  Future<void> _checkForUpdates() async {
    try {
      final updateData = await ApiClient.fetchLatestVersion();
      if (updateData != null) {
        final latestVersion = updateData['latest_version'] as String;
        final downloadUrl = updateData['download_url'] as String;
        
        final packageInfo = await PackageInfo.fromPlatform();
        final currentVersion = packageInfo.version;
        
        if (_isUpdateAvailable(currentVersion, latestVersion)) {
          // Wait for a short duration to ensure navigator is ready
          Future.delayed(const Duration(seconds: 1), () {
            if (mounted) {
              _showUpdateDialog(latestVersion, downloadUrl);
            }
          });
        }
      }
    } catch (e) {
      print("Update check failed: $e");
    }
  }

  bool _isUpdateAvailable(String current, String latest) {
    try {
      List<int> currentParts = current.split('.').map(int.parse).toList();
      List<int> latestParts = latest.split('.').map(int.parse).toList();
      
      for (int i = 0; i < latestParts.length; i++) {
        if (i >= currentParts.length) return true;
        if (latestParts[i] > currentParts[i]) return true;
        if (latestParts[i] < currentParts[i]) return false;
      }
    } catch (e) {
      return current != latest && latest.compareTo(current) > 0;
    }
    return false;
  }

  void _showUpdateDialog(String latestVersion, String downloadUrl) {
    if (_navigatorKey.currentState != null) {
      showDialog(
        context: _navigatorKey.currentState!.overlay!.context,
        barrierDismissible: true, // Non-blocking
        builder: (BuildContext context) {
          return AlertDialog(
            title: const Text('Update Available'),
            content: Text('A new version ($latestVersion) is available. Please update to continue using the latest features.'),
            actions: <Widget>[
              TextButton(
                child: const Text('Later'),
                onPressed: () {
                  Navigator.of(context).pop();
                },
              ),
              ElevatedButton(
                child: const Text('Update Now'),
                onPressed: () async {
                  final url = Uri.parse(downloadUrl);
                  if (await canLaunchUrl(url)) {
                    await launchUrl(url, mode: LaunchMode.externalApplication);
                  }
                },
              ),
            ],
          );
        },
      );
    }
  }

  // Handle push notification tap actions
  Future<void> _setupInteractedMessage() async {
    // 1. App killed (Cold Start)
    RemoteMessage? initialMessage = await FirebaseMessaging.instance.getInitialMessage();
    if (initialMessage != null) {
      _handleMessage(initialMessage);
    }

    // 2. App in background
    FirebaseMessaging.onMessageOpenedApp.listen(_handleMessage);
    
    // 3. App in foreground (show modal directly)
    FirebaseMessaging.onMessage.listen((RemoteMessage message) {
      if (message.data['type'] == 'geo_alert') {
        _showGeoAlertModal(message.data['request_id'], message.data['blood_group']);
      }
    });
  }

  void _handleMessage(RemoteMessage message) {
    if (message.data['type'] == 'geo_alert') {
      // Delay to ensure navigator is mounted
      Future.delayed(const Duration(milliseconds: 500), () {
        _showGeoAlertModal(message.data['request_id'], message.data['blood_group']);
      });
    }
  }

  void _showGeoAlertModal(String? requestId, String? bloodGroup) {
    if (requestId != null && bloodGroup != null && _navigatorKey.currentState != null) {
      showGeneralDialog(
        context: _navigatorKey.currentState!.overlay!.context,
        pageBuilder: (context, animation, secondaryAnimation) => GeoAlertModal(
          requestId: requestId,
          bloodGroup: bloodGroup,
        ),
      );
    }
  }

  @override
  Widget build(BuildContext context) {
    return MaterialApp(
      navigatorKey: _navigatorKey,
      title: 'QATRA',
      theme: QatraTheme.lightTheme,
      darkTheme: QatraTheme.darkTheme,
      themeMode: ThemeMode.system,
      home: const PhoneVerificationScreen(),
      debugShowCheckedModeBanner: false,
    );
  }
}
