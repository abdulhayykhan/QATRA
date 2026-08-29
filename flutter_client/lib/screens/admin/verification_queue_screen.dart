import 'package:flutter/material.dart';
import '../../core/api_client.dart';

class VerificationQueueScreen extends StatefulWidget {
  @override
  _VerificationQueueScreenState createState() => _VerificationQueueScreenState();
}

class _VerificationQueueScreenState extends State<VerificationQueueScreen> {
  List<dynamic> _queue = [];
  bool _isLoading = true;

  @override
  void initState() {
    super.initState();
    _fetchQueue();
  }

  Future<void> _fetchQueue() async {
    final queue = await ApiClient.fetchVerificationQueue();
    setState(() {
      _queue = queue;
      _isLoading = false;
    });
  }

  @override
  Widget build(BuildContext context) {
    return Scaffold(
      appBar: AppBar(
        title: Text('Verification Queue'),
        backgroundColor: Theme.of(context).colorScheme.primary,
        foregroundColor: Colors.white,
      ),
      body: _isLoading
          ? Center(child: CircularProgressIndicator())
          : ListView.builder(
              padding: EdgeInsets.all(16),
              itemCount: _queue.length,
              itemBuilder: (context, index) {
                final item = _queue[index];
                return Card(
                  elevation: 2,
                  margin: EdgeInsets.only(bottom: 12),
                  child: ListTile(
                    title: Text('Request ID: ${item['request_id']}'),
                    subtitle: Text('Status: ${item['status']}'),
                    trailing: Icon(Icons.arrow_forward_ios),
                  ),
                );
              },
            ),
    );
  }
}
