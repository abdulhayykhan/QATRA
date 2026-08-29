import 'package:flutter/material.dart';
import '../core/api_client.dart';

class LiveRequestFeedStatusScreen extends StatefulWidget {
  @override
  _LiveRequestFeedStatusScreenState createState() => _LiveRequestFeedStatusScreenState();
}

class _LiveRequestFeedStatusScreenState extends State<LiveRequestFeedStatusScreen> {
  List<dynamic> _myRequests = [];
  bool _isLoading = true;

  @override
  void initState() {
    super.initState();
    _fetchMyRequests();
  }

  Future<void> _fetchMyRequests() async {
    final requests = await ApiClient.fetchMyRequests();
    setState(() {
      _myRequests = requests;
      _isLoading = false;
    });
  }

  @override
  Widget build(BuildContext context) {
    return Scaffold(
      appBar: AppBar(
        title: Text('My Blood Requests'),
        backgroundColor: Theme.of(context).colorScheme.primary,
        foregroundColor: Colors.white,
      ),
      body: _isLoading
          ? Center(child: CircularProgressIndicator())
          : _myRequests.isEmpty
              ? Center(child: Text("You haven't made any requests yet."))
              : ListView.builder(
                  padding: EdgeInsets.all(16),
                  itemCount: _myRequests.length,
                  itemBuilder: (context, index) {
                    final req = _myRequests[index];
                    return Card(
                      elevation: 3,
                      margin: EdgeInsets.only(bottom: 12),
                      child: Padding(
                        padding: const EdgeInsets.all(16.0),
                        child: Column(
                          crossAxisAlignment: CrossAxisAlignment.start,
                          children: [
                            Row(
                              mainAxisAlignment: MainAxisAlignment.spaceBetween,
                              children: [
                                Text(
                                  '${req['units_required']} Units of ${req['blood_group']}',
                                  style: Theme.of(context).textTheme.titleLarge,
                                ),
                                Chip(
                                  label: Text(req['status']),
                                  backgroundColor: req['status'] == 'BROADCASTING'
                                      ? Colors.green.shade100
                                      : Colors.orange.shade100,
                                ),
                              ],
                            ),
                            SizedBox(height: 8),
                            Text('Urgency: ${req['urgency']}'),
                            Text('Component: ${req['component']}'),
                            SizedBox(height: 16),
                            Row(
                              mainAxisAlignment: MainAxisAlignment.spaceAround,
                              children: [
                                Column(
                                  children: [
                                    Text('${req['active_donors_in_radius'] ?? 0}',
                                        style: TextStyle(fontSize: 24, fontWeight: FontWeight.bold)),
                                    Text('Donors Nearby', style: TextStyle(fontSize: 12)),
                                  ],
                                ),
                                Column(
                                  children: [
                                    Text('${req['responded_donors_count'] ?? 0}',
                                        style: TextStyle(fontSize: 24, fontWeight: FontWeight.bold, color: Colors.green)),
                                    Text('Responded', style: TextStyle(fontSize: 12)),
                                  ],
                                ),
                              ],
                            )
                          ],
                        ),
                      ),
                    );
                  },
                ),
    );
  }
}
