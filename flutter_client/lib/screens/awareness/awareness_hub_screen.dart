import 'package:flutter/material.dart';
import '../../core/api_client.dart';
import '../donor/pre_screening_checklist_screen.dart';

class AwarenessHubScreen extends StatefulWidget {
  @override
  _AwarenessHubScreenState createState() => _AwarenessHubScreenState();
}

class _AwarenessHubScreenState extends State<AwarenessHubScreen> {
  List<dynamic> _articles = [];
  bool _isLoading = true;

  @override
  void initState() {
    super.initState();
    _fetchArticles();
  }

  Future<void> _fetchArticles() async {
    final articles = await ApiClient.fetchAwarenessArticles();
    setState(() {
      _articles = articles;
      _isLoading = false;
    });
  }

  @override
  Widget build(BuildContext context) {
    return Scaffold(
      appBar: AppBar(
        title: Text('Awareness Hub'),
        backgroundColor: Theme.of(context).colorScheme.primary,
        foregroundColor: Colors.white,
      ),
      body: Column(
        children: [
          Container(
            width: double.infinity,
            padding: EdgeInsets.all(16),
            color: Theme.of(context).colorScheme.surfaceVariant,
            child: Column(
              children: [
                Text(
                  'Are you eligible to donate?',
                  style: Theme.of(context).textTheme.titleMedium,
                ),
                SizedBox(height: 8),
                ElevatedButton(
                  onPressed: () {
                    Navigator.push(
                      context,
                      MaterialPageRoute(builder: (context) => PreScreeningChecklistScreen()),
                    );
                  },
                  child: Text('Take the Eligibility Quiz'),
                ),
              ],
            ),
          ),
          Expanded(
            child: _isLoading
                ? Center(child: CircularProgressIndicator())
                : ListView.builder(
                    padding: EdgeInsets.all(16),
                    itemCount: _articles.length,
                    itemBuilder: (context, index) {
                      final article = _articles[index];
                      return Card(
                        elevation: 2,
                        margin: EdgeInsets.only(bottom: 12),
                        child: ExpansionTile(
                          title: Text(article['title']),
                          subtitle: Text('${article['category']} • ${article['read_time']}'),
                          children: [
                            Padding(
                              padding: const EdgeInsets.all(16.0),
                              child: Text(
                                article['full_content'],
                                style: TextStyle(height: 1.5),
                              ),
                            ),
                          ],
                        ),
                      );
                    },
                  ),
          ),
        ],
      ),
    );
  }
}
