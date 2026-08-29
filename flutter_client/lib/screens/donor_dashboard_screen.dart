import 'dart:async';
import 'package:flutter/material.dart';
import 'package:geolocator/geolocator.dart';
import '../core/api_client.dart';

class DonorDashboardScreen extends StatefulWidget {
  @override
  _DonorDashboardScreenState createState() => _DonorDashboardScreenState();
}

class _DonorDashboardScreenState extends State<DonorDashboardScreen> {
  bool _isAvailable = false;
  StreamSubscription<Position>? _positionStreamSubscription;
  List<dynamic> _feedRequests = [];
  bool _isLoadingFeed = true;

  @override
  void initState() {
    super.initState();
    _fetchFeed();
  }

  Future<void> _fetchFeed() async {
    final requests = await ApiClient.fetchRequestFeed();
    setState(() {
      _feedRequests = requests;
      _isLoadingFeed = false;
    });
  }

  @override
  void dispose() {
    _positionStreamSubscription?.cancel();
    super.dispose();
  }

  Future<void> _toggleAvailability(bool value) async {
    setState(() {
      _isAvailable = value;
    });

    if (value) {
      // Requested to turn ON
      bool serviceEnabled = await Geolocator.isLocationServiceEnabled();
      if (!serviceEnabled) {
        _showError('Location services are disabled.');
        setState(() => _isAvailable = false);
        return;
      }

      LocationPermission permission = await Geolocator.checkPermission();
      if (permission == LocationPermission.denied) {
        permission = await Geolocator.requestPermission();
        if (permission == LocationPermission.denied) {
          _showError('Location permissions are denied');
          setState(() => _isAvailable = false);
          return;
        }
      }

      if (permission == LocationPermission.deniedForever) {
        _showError('Location permissions are permanently denied, we cannot request permissions.');
        setState(() => _isAvailable = false);
        return;
      }

      // Start stream
      _startLocationUpdates();
    } else {
      // Requested to turn OFF
      _stopLocationUpdates();
    }
  }

  void _startLocationUpdates() {
    final LocationSettings locationSettings = LocationSettings(
      accuracy: LocationAccuracy.high,
      distanceFilter: 100, // Significant movement: 100 meters
    );

    _positionStreamSubscription = Geolocator.getPositionStream(locationSettings: locationSettings)
        .listen((Position position) {
      // Send location update to backend
      ApiClient.updateLocation(position.latitude, position.longitude);
      print("Location sent to backend: ${position.latitude}, ${position.longitude}");
    });
  }

  void _stopLocationUpdates() {
    _positionStreamSubscription?.cancel();
    _positionStreamSubscription = null;
    print("Location stream stopped");
  }

  void _showError(String message) {
    ScaffoldMessenger.of(context).showSnackBar(SnackBar(content: Text(message)));
  }

  @override
  Widget build(BuildContext context) {
    return Scaffold(
      appBar: AppBar(
        title: Text('Donor Dashboard'),
        backgroundColor: Theme.of(context).colorScheme.primary,
        foregroundColor: Colors.white,
      ),
      body: Padding(
        padding: const EdgeInsets.all(24.0),
        child: Column(
          crossAxisAlignment: CrossAxisAlignment.start,
          children: [
            Text(
              'Your Status',
              style: Theme.of(context).textTheme.headlineSmall,
            ),
            SizedBox(height: 16),
            Card(
              elevation: 4,
              shape: RoundedRectangleBorder(borderRadius: BorderRadius.circular(16)),
              child: Padding(
                padding: const EdgeInsets.all(16.0),
                child: Row(
                  mainAxisAlignment: MainAxisAlignment.spaceBetween,
                  children: [
                    Column(
                      crossAxisAlignment: CrossAxisAlignment.start,
                      children: [
                        Text(
                          'Available to Donate',
                          style: TextStyle(fontSize: 18, fontWeight: FontWeight.bold),
                        ),
                        SizedBox(height: 4),
                        Text(
                          _isAvailable ? 'Location tracking is active' : 'Offline',
                          style: TextStyle(
                            color: _isAvailable ? Colors.green : Colors.grey,
                          ),
                        ),
                      ],
                    ),
                    Switch(
                      value: _isAvailable,
                      onChanged: _toggleAvailability,
                      activeColor: Colors.green,
                    ),
                  ],
                ),
              ),
            ),
            SizedBox(height: 32),
            Text(
              'When you turn this on, QATRA will track your location in the background to match you with nearby emergencies. Turn it off when you are not available.',
              style: TextStyle(color: Colors.black54),
            ),
            SizedBox(height: 32),
            Text(
              'Live Emergency Feed',
              style: Theme.of(context).textTheme.headlineSmall,
            ),
            SizedBox(height: 16),
            Expanded(
              child: _isLoadingFeed
                  ? Center(child: CircularProgressIndicator())
                  : _feedRequests.isEmpty
                      ? Center(child: Text("No active emergencies right now."))
                      : ListView.builder(
                          itemCount: _feedRequests.length,
                          itemBuilder: (context, index) {
                            final req = _feedRequests[index];
                            return Card(
                              elevation: 2,
                              margin: EdgeInsets.only(bottom: 12),
                              child: ListTile(
                                leading: Icon(Icons.water_drop, color: Colors.red),
                                title: Text('${req['units_required']} Units of ${req['blood_group']}'),
                                subtitle: Text('Urgency: ${req['urgency']}\nStatus: ${req['status']}'),
                                trailing: ElevatedButton(
                                  onPressed: () {
                                    // Normally navigate to request details or match
                                  },
                                  child: Text('Respond'),
                                ),
                                isThreeLine: true,
                              ),
                            );
                          },
                        ),
            ),
          ],
        ),
      ),
    );
  }
}
