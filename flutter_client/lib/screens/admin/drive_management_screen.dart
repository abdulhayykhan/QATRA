import 'package:flutter/material.dart';
import '../../core/api_client.dart';

class DriveManagementScreen extends StatefulWidget {
  @override
  _DriveManagementScreenState createState() => _DriveManagementScreenState();
}

class _DriveManagementScreenState extends State<DriveManagementScreen> {
  List<dynamic> _drives = [];
  bool _isLoading = true;

  @override
  void initState() {
    super.initState();
    _fetchDrives();
  }

  Future<void> _fetchDrives() async {
    final drives = await ApiClient.fetchCampusDrives();
    setState(() {
      _drives = drives;
      _isLoading = false;
    });
  }

  void _showScheduleDriveDialog() {
    final titleCtrl = TextEditingController();
    final venueCtrl = TextEditingController();
    final quotaCtrl = TextEditingController();
    final dateCtrl = TextEditingController();
    final timeCtrl = TextEditingController();

    showDialog(
      context: context,
      builder: (context) => AlertDialog(
        title: Text('Schedule Campus Drive'),
        content: SingleChildScrollView(
          child: Column(
            mainAxisSize: MainAxisSize.min,
            children: [
              TextField(controller: titleCtrl, decoration: InputDecoration(labelText: 'Title')),
              TextField(controller: venueCtrl, decoration: InputDecoration(labelText: 'University/Venue')),
              TextField(controller: quotaCtrl, decoration: InputDecoration(labelText: 'Target Quota (Units)'), keyboardType: TextInputType.number),
              TextField(controller: dateCtrl, decoration: InputDecoration(labelText: 'Date (YYYY-MM-DD)')),
              TextField(controller: timeCtrl, decoration: InputDecoration(labelText: 'Time (HH:MM)')),
            ],
          ),
        ),
        actions: [
          TextButton(onPressed: () => Navigator.pop(context), child: Text('Cancel')),
          ElevatedButton(
            onPressed: () async {
              Navigator.pop(context);
              setState(() => _isLoading = true);
              final success = await ApiClient.scheduleCampusDrive({
                'title': titleCtrl.text,
                'university_venue': venueCtrl.text,
                'target_quota_units': int.tryParse(quotaCtrl.text) ?? 50,
                'date_str': dateCtrl.text,
                'time_str': timeCtrl.text,
              });
              if (success) _fetchDrives();
              else {
                setState(() => _isLoading = false);
                ScaffoldMessenger.of(context).showSnackBar(SnackBar(content: Text('Failed to schedule drive')));
              }
            },
            child: Text('Schedule'),
          ),
        ],
      ),
    );
  }



  @override
  Widget build(BuildContext context) {
    return Scaffold(
      appBar: AppBar(
        title: Text('Drive Management'),
        backgroundColor: Theme.of(context).colorScheme.primary,
        foregroundColor: Colors.white,
        actions: [
          IconButton(
            icon: Icon(Icons.add),
            onPressed: _showScheduleDriveDialog,
            tooltip: 'Schedule Drive',
          ),
        ],
      ),
      body: _isLoading
          ? Center(child: CircularProgressIndicator())
          : ListView.builder(
              padding: EdgeInsets.all(16),
              itemCount: _drives.length,
              itemBuilder: (context, index) {
                final item = _drives[index];
                return Card(
                  elevation: 2,
                  margin: EdgeInsets.only(bottom: 12),
                  child: ListTile(
                    leading: Icon(Icons.campaign, color: Theme.of(context).colorScheme.primary),
                    title: Text(item['title'] ?? 'Campus Drive'),
                    subtitle: Text('Venue: ${item['university_venue']}\nStatus: ${item['status']}'),
                    isThreeLine: true,
                    trailing: IconButton(
                      icon: Icon(Icons.qr_code_scanner),
                      tooltip: 'Simulate Check-In',
                      onPressed: () {
                        // Normally this would open a camera. We simulate by sending a hardcoded or prompted ID.
                        // For the demo, we assume we scanned an attendee UUID.
                        ScaffoldMessenger.of(context).showSnackBar(SnackBar(content: Text('Simulating QR Scan for Drive...')));
                        // _simulateQrCheckIn('attendee-uuid');
                      },
                    ),
                  ),
                );
              },
            ),
    );
  }
}
