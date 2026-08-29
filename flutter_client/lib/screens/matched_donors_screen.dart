import 'package:flutter/material.dart';
import 'package:url_launcher/url_launcher.dart';
import '../core/api_client.dart';

class MatchedDonorsScreen extends StatefulWidget {
  final String requestId;

  const MatchedDonorsScreen({Key? key, required this.requestId}) : super(key: key);

  @override
  _MatchedDonorsScreenState createState() => _MatchedDonorsScreenState();
}

class _MatchedDonorsScreenState extends State<MatchedDonorsScreen> {
  List<dynamic> _donors = [];
  bool _isLoading = true;

  @override
  void initState() {
    super.initState();
    _fetchDonors();
  }

  Future<void> _fetchDonors() async {
    final donors = await ApiClient.getEligibleDonors(widget.requestId);
    setState(() {
      _donors = donors;
      _isLoading = false;
    });
  }

  @override
  Widget build(BuildContext context) {
    return Scaffold(
      appBar: AppBar(
        title: Text('Matched Donors'),
        backgroundColor: Theme.of(context).colorScheme.primary,
        foregroundColor: Colors.white,
      ),
      body: _isLoading
          ? Center(child: CircularProgressIndicator())
          : _donors.isEmpty
              ? Center(
                  child: Padding(
                    padding: const EdgeInsets.all(24.0),
                    child: Column(
                      mainAxisAlignment: MainAxisAlignment.center,
                      children: [
                        Icon(Icons.search_off, size: 64, color: Colors.grey),
                        SizedBox(height: 16),
                        Text(
                          'No eligible donors found in your area yet.',
                          textAlign: TextAlign.center,
                          style: TextStyle(fontSize: 18, color: Colors.grey),
                        ),
                        SizedBox(height: 8),
                        Text(
                          'The request is active. Donors will be notified when they enter the area.',
                          textAlign: TextAlign.center,
                          style: TextStyle(color: Colors.grey),
                        ),
                      ],
                    ),
                  ),
                )
              : ListView.builder(
                  padding: EdgeInsets.all(16),
                  itemCount: _donors.length,
                  itemBuilder: (context, index) {
                    final donor = _donors[index];
                    final distanceKm = donor['distance_km'] as double?;
                    
                    return Card(
                      elevation: 2,
                      margin: EdgeInsets.only(bottom: 12),
                      shape: RoundedRectangleBorder(borderRadius: BorderRadius.circular(12)),
                      child: ListTile(
                        leading: CircleAvatar(
                          backgroundColor: Colors.red.shade100,
                          child: Text(
                            donor['blood_group'] ?? 'O+',
                            style: TextStyle(color: Colors.red.shade900, fontWeight: FontWeight.bold),
                          ),
                        ),
                        title: Text(
                          donor['display_name'] ?? 'Anonymous Donor',
                          style: TextStyle(fontWeight: FontWeight.bold),
                        ),
                        trailing: OutlinedButton.icon(
                          icon: Icon(Icons.phone, size: 18),
                          label: Text('Call'),
                          onPressed: () => _callDonor(context, donor['donor_id']),
                        ),
                        subtitle: Column(
                          crossAxisAlignment: CrossAxisAlignment.start,
                          children: [
                            Text(distanceKm != null 
                                ? '${distanceKm.toStringAsFixed(1)} km away'
                                : 'Distance unknown'),
                            SizedBox(height: 4),
                            Text(
                              'Direct call — your number will be visible to the other party',
                              style: TextStyle(fontSize: 10, color: Colors.grey[600]),
                            ),
                          ],
                        ),
                      ),
                    );
                  },
                ),
    );
  }

  Future<void> _callDonor(BuildContext context, String targetUserId) async {
    // Show a loading dialog
    showDialog(
      context: context,
      barrierDismissible: false,
      builder: (BuildContext context) {
        return Center(child: CircularProgressIndicator());
      },
    );

    // Fetch the real, unmasked phone number on-demand
    final phone = await ApiClient.fetchContactNumber(widget.requestId, targetUserId);
    
    // Close the loading dialog
    Navigator.of(context).pop();

    if (phone != null && phone.isNotEmpty) {
      final Uri launchUri = Uri(
        scheme: 'tel',
        path: phone,
      );
      if (await canLaunchUrl(launchUri)) {
        await launchUrl(launchUri);
      } else {
        ScaffoldMessenger.of(context).showSnackBar(
          SnackBar(content: Text('Could not launch the dialer.')),
        );
      }
    } else {
      ScaffoldMessenger.of(context).showSnackBar(
        SnackBar(content: Text('Cannot call: Contact information is protected until match is accepted.')),
      );
    }
  }
}
