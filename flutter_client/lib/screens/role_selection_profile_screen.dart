import 'package:flutter/material.dart';

class RoleSelectionProfileScreen extends StatefulWidget {
  const RoleSelectionProfileScreen({super.key});

  @override
  State<RoleSelectionProfileScreen> createState() => _RoleSelectionProfileScreenState();
}

class _RoleSelectionProfileScreenState extends State<RoleSelectionProfileScreen> {
  final _nameController = TextEditingController();
  final _ageController = TextEditingController();
  final _cnicController = TextEditingController();
  
  String _selectedGender = 'M';
  String _selectedDistrict = 'Karachi South';

  final List<String> _districts = [
    'Karachi South', 'Karachi East', 'Karachi West', 'Karachi Central', 'Malir', 'Korangi'
  ];

  void _saveProfile() {
    // In a full implementation, this would save the profile to the backend.
    // For this slice, we just show a success message.
    ScaffoldMessenger.of(context).showSnackBar(
      const SnackBar(content: Text('Profile saved! Side-by-side test slice complete.')),
    );
  }

  @override
  Widget build(BuildContext context) {
    return Scaffold(
      appBar: AppBar(title: const Text('Seeker Profile Setup')),
      body: SingleChildScrollView(
        padding: const EdgeInsets.all(24.0),
        child: Column(
          crossAxisAlignment: CrossAxisAlignment.stretch,
          children: [
            Text(
              'Complete your profile',
              style: Theme.of(context).textTheme.headlineSmall,
              textAlign: TextAlign.center,
            ),
            const SizedBox(height: 24),
            TextField(
              controller: _nameController,
              decoration: const InputDecoration(
                labelText: 'Full Name',
                border: OutlineInputBorder(),
              ),
            ),
            const SizedBox(height: 16),
            Row(
              children: [
                Expanded(
                  child: TextField(
                    controller: _ageController,
                    keyboardType: TextInputType.number,
                    decoration: const InputDecoration(
                      labelText: 'Age',
                      border: OutlineInputBorder(),
                    ),
                  ),
                ),
                const SizedBox(width: 16),
                Expanded(
                  child: DropdownButtonFormField<String>(
                    value: _selectedGender,
                    decoration: const InputDecoration(
                      labelText: 'Gender',
                      border: OutlineInputBorder(),
                    ),
                    items: const [
                      DropdownMenuItem(value: 'M', child: Text('Male')),
                      DropdownMenuItem(value: 'F', child: Text('Female')),
                      DropdownMenuItem(value: 'O', child: Text('Other')),
                    ],
                    onChanged: (val) {
                      if (val != null) setState(() => _selectedGender = val);
                    },
                  ),
                ),
              ],
            ),
            const SizedBox(height: 16),
            TextField(
              controller: _cnicController,
              keyboardType: TextInputType.number,
              decoration: const InputDecoration(
                labelText: 'CNIC (e.g. 42101-1234567-1)',
                border: OutlineInputBorder(),
              ),
            ),
            const SizedBox(height: 16),
            DropdownButtonFormField<String>(
              value: _selectedDistrict,
              decoration: const InputDecoration(
                labelText: 'District',
                border: OutlineInputBorder(),
              ),
              items: _districts.map((d) => DropdownMenuItem(value: d, child: Text(d))).toList(),
              onChanged: (val) {
                if (val != null) setState(() => _selectedDistrict = val);
              },
            ),
            const SizedBox(height: 32),
            ElevatedButton(
              onPressed: _saveProfile,
              style: ElevatedButton.styleFrom(
                backgroundColor: Theme.of(context).colorScheme.primary,
                foregroundColor: Theme.of(context).colorScheme.onPrimary,
                padding: const EdgeInsets.symmetric(vertical: 16),
              ),
              child: const Text('Save Profile & Continue'),
            ),
          ],
        ),
      ),
    );
  }
}
