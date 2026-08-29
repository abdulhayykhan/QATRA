import 'package:flutter/material.dart';
import '../../core/api_client.dart';

class PreScreeningChecklistScreen extends StatefulWidget {
  @override
  _PreScreeningChecklistScreenState createState() => _PreScreeningChecklistScreenState();
}

class _PreScreeningChecklistScreenState extends State<PreScreeningChecklistScreen> {
  bool _ageValid = true;
  bool _weightValid = true;
  bool _noRecentIllness = true;
  bool _noRecentDonation = true;
  bool _noRecentTattooOrSurgery = true;
  bool _isSubmitting = false;

  Future<void> _submit() async {
    setState(() => _isSubmitting = true);
    
    final answers = {
      'age_valid': _ageValid,
      'weight_valid': _weightValid,
      'no_recent_illness': _noRecentIllness,
      'no_recent_donation': _noRecentDonation,
      'no_recent_tattoo_or_surgery': _noRecentTattooOrSurgery,
    };

    final success = await ApiClient.submitPreScreening(answers);
    
    setState(() => _isSubmitting = false);

    if (success) {
      ScaffoldMessenger.of(context).showSnackBar(
        SnackBar(content: Text('Eligibility status updated successfully!')),
      );
      Navigator.pop(context);
    } else {
      ScaffoldMessenger.of(context).showSnackBar(
        SnackBar(content: Text('Failed to update. Ensure you are a verified donor.')),
      );
    }
  }

  @override
  Widget build(BuildContext context) {
    return Scaffold(
      appBar: AppBar(
        title: Text('Pre-Screening Checklist'),
        backgroundColor: Theme.of(context).colorScheme.primary,
        foregroundColor: Colors.white,
      ),
      body: ListView(
        padding: EdgeInsets.all(16),
        children: [
          Text(
            'Please answer the following questions truthfully. Your honesty ensures the safety of both you and the recipient.',
            style: Theme.of(context).textTheme.bodyLarge,
          ),
          SizedBox(height: 24),
          SwitchListTile(
            title: Text('Are you between 18 and 65 years old?'),
            value: _ageValid,
            onChanged: (val) => setState(() => _ageValid = val),
          ),
          SwitchListTile(
            title: Text('Do you weigh at least 50 kg (110 lbs)?'),
            value: _weightValid,
            onChanged: (val) => setState(() => _weightValid = val),
          ),
          SwitchListTile(
            title: Text('Are you currently feeling healthy and well?'),
            subtitle: Text('No fever, cold, or flu symptoms.'),
            value: _noRecentIllness,
            onChanged: (val) => setState(() => _noRecentIllness = val),
          ),
          SwitchListTile(
            title: Text('Has it been at least 90 days since your last donation?'),
            value: _noRecentDonation,
            onChanged: (val) => setState(() => _noRecentDonation = val),
          ),
          SwitchListTile(
            title: Text('Have you avoided tattoos or major surgeries in the last 6 months?'),
            value: _noRecentTattooOrSurgery,
            onChanged: (val) => setState(() => _noRecentTattooOrSurgery = val),
          ),
          SizedBox(height: 32),
          _isSubmitting
              ? Center(child: CircularProgressIndicator())
              : SizedBox(
                  width: double.infinity,
                  height: 50,
                  child: ElevatedButton(
                    onPressed: _submit,
                    style: ElevatedButton.styleFrom(
                      backgroundColor: Theme.of(context).colorScheme.primary,
                      foregroundColor: Colors.white,
                    ),
                    child: Text('SUBMIT ELIGIBILITY', style: TextStyle(fontSize: 16)),
                  ),
                ),
        ],
      ),
    );
  }
}
