import 'package:flutter/material.dart';
import '../../core/api_client.dart';

class FraudAuditScreen extends StatefulWidget {
  @override
  _FraudAuditScreenState createState() => _FraudAuditScreenState();
}

class _FraudAuditScreenState extends State<FraudAuditScreen> {
  List<dynamic> _audits = [];
  bool _isLoading = true;

  @override
  void initState() {
    super.initState();
    _fetchAudits();
  }

  Future<void> _fetchAudits() async {
    final audits = await ApiClient.fetchFraudAudits();
    setState(() {
      _audits = audits;
      _isLoading = false;
    });
  }

  @override
  Widget build(BuildContext context) {
    return Scaffold(
      appBar: AppBar(
        title: Text('Fraud Audit Log'),
        backgroundColor: Theme.of(context).colorScheme.primary,
        foregroundColor: Colors.white,
      ),
      body: _isLoading
          ? Center(child: CircularProgressIndicator())
          : ListView.builder(
              padding: EdgeInsets.all(16),
              itemCount: _audits.length,
              itemBuilder: (context, index) {
                final item = _audits[index];
                return Card(
                  elevation: 2,
                  margin: EdgeInsets.only(bottom: 12),
                  child: ListTile(
                    leading: Icon(Icons.warning, color: Colors.amber),
                    title: Text('Reason: ${item['flag_reason']}'),
                    subtitle: Text('Status: ${item['action_status']}'),
                  ),
                );
              },
            ),
    );
  }
}
