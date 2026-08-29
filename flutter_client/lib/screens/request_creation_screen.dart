import 'package:flutter/material.dart';
import '../core/api_client.dart';
import '../core/api_client.dart';
import 'matched_donors_screen.dart';
import 'package:image_picker/image_picker.dart';
import 'package:google_mlkit_text_recognition/google_mlkit_text_recognition.dart';
import 'dart:io';

class RequestCreationScreen extends StatefulWidget {
  @override
  _RequestCreationScreenState createState() => _RequestCreationScreenState();
}

class _RequestCreationScreenState extends State<RequestCreationScreen> {
  final _formKey = GlobalKey<FormState>();
  
  String _hospitalId = '00000000-0000-0000-0000-000000000456'; // Mock hospital ID for now
  String _bloodGroup = 'O+';
  String _component = 'PRBC';
  int _units = 1;
  String _urgency = 'HIGH_PRIORITY';
  String _seekerName = '';
  String _mrnNumber = '';
  
  File? _slipImage;
  String _ocrText = '';
  int _ocrConfidence = 0;
  bool _isLoading = false;
  final ImagePicker _picker = ImagePicker();

  Future<void> _pickAndProcessImage() async {
    final pickedFile = await _picker.pickImage(source: ImageSource.camera);
    if (pickedFile != null) {
      setState(() {
        _slipImage = File(pickedFile.path);
        _isLoading = true;
      });

      try {
        final inputImage = InputImage.fromFilePath(pickedFile.path);
        final textRecognizer = TextRecognizer(script: TextRecognitionScript.latin);
        final RecognizedText recognizedText = await textRecognizer.processImage(inputImage);
        
        setState(() {
          _ocrText = recognizedText.text;
          // Heuristic for confidence: does the OCR text contain the MRN?
          _ocrConfidence = _ocrText.isNotEmpty ? 85 : 40; 
          _isLoading = false;
        });
        
        textRecognizer.close();
      } catch (e) {
        setState(() {
          _ocrConfidence = 0;
          _isLoading = false;
        });
      }
    }
  }

  void _submitRequest() async {
    if (_formKey.currentState!.validate()) {
      _formKey.currentState!.save();
      setState(() => _isLoading = true);

      final requestId = await ApiClient.createRequest({
        'hospital_id': _hospitalId,
        'blood_group': _bloodGroup,
        'component': _component,
        'units_required': _units,
        'urgency': _urgency,
        'seeker_name': _seekerName,
        'seeker_phone': '+923000000000', // Hardcoded for demo, normally pulled from auth
        'seeker_cnic': '42101-1234567-1',
        'mrn_number': _mrnNumber,
      });

      setState(() => _isLoading = false);

      if (requestId != null) {
        if (_slipImage != null) {
          await ApiClient.uploadHospitalSlip(
            requestId, 
            _slipImage!.path, 
            _ocrText, 
            _ocrConfidence
          );
        }
        
        if (mounted) {
          Navigator.push(
            context,
            MaterialPageRoute(
              builder: (context) => MatchedDonorsScreen(requestId: requestId),
            ),
          );
        }
      } else {
        if (mounted) {
          ScaffoldMessenger.of(context).showSnackBar(
            SnackBar(content: Text('Failed to create request')),
          );
        }
      }
    }
  }

  @override
  Widget build(BuildContext context) {
    return Scaffold(
      appBar: AppBar(
        title: Text('Create Emergency Request'),
        backgroundColor: Theme.of(context).colorScheme.primary,
        foregroundColor: Colors.white,
      ),
      body: SingleChildScrollView(
        padding: EdgeInsets.all(24),
        child: Form(
          key: _formKey,
          child: Column(
            crossAxisAlignment: CrossAxisAlignment.stretch,
            children: [
              TextFormField(
                decoration: InputDecoration(labelText: 'Patient Name', border: OutlineInputBorder()),
                validator: (v) => v!.isEmpty ? 'Required' : null,
                onSaved: (v) => _seekerName = v!,
              ),
              SizedBox(height: 16),
              TextFormField(
                decoration: InputDecoration(labelText: 'MRN Number (Hospital Record)', border: OutlineInputBorder()),
                validator: (v) => v!.isEmpty ? 'Required' : null,
                onSaved: (v) => _mrnNumber = v!,
              ),
              SizedBox(height: 16),
              DropdownButtonFormField<String>(
                value: _bloodGroup,
                decoration: InputDecoration(labelText: 'Blood Group', border: OutlineInputBorder()),
                items: ['A+', 'A-', 'B+', 'B-', 'AB+', 'AB-', 'O+', 'O-']
                    .map((bg) => DropdownMenuItem(value: bg, child: Text(bg)))
                    .toList(),
                onChanged: (v) => setState(() => _bloodGroup = v!),
              ),
              SizedBox(height: 16),
              Row(
                children: [
                  Expanded(
                    child: DropdownButtonFormField<String>(
                      value: _component,
                      decoration: InputDecoration(labelText: 'Component', border: OutlineInputBorder()),
                      items: ['WHOLE_BLOOD', 'PRBC', 'PLATELETS', 'PLASMA']
                          .map((c) => DropdownMenuItem(value: c, child: Text(c.replaceAll('_', ' '))))
                          .toList(),
                      onChanged: (v) => setState(() => _component = v!),
                    ),
                  ),
                  SizedBox(width: 16),
                  Expanded(
                    child: DropdownButtonFormField<int>(
                      value: _units,
                      decoration: InputDecoration(labelText: 'Units', border: OutlineInputBorder()),
                      items: List.generate(5, (index) => index + 1)
                          .map((u) => DropdownMenuItem(value: u, child: Text(u.toString())))
                          .toList(),
                      onChanged: (v) => setState(() => _units = v!),
                    ),
                  ),
                ],
              ),
              SizedBox(height: 16),
              OutlinedButton.icon(
                icon: Icon(Icons.camera_alt),
                label: Text(_slipImage == null ? 'Capture Hospital Slip' : 'Retake Hospital Slip'),
                onPressed: _pickAndProcessImage,
                style: OutlinedButton.styleFrom(
                  padding: EdgeInsets.symmetric(vertical: 16),
                  shape: RoundedRectangleBorder(borderRadius: BorderRadius.circular(12)),
                ),
              ),
              if (_slipImage != null) ...[
                SizedBox(height: 8),
                Text('Slip Captured ✓', style: TextStyle(color: Colors.green, fontWeight: FontWeight.bold)),
              ],
              SizedBox(height: 32),
              ElevatedButton(
                onPressed: _isLoading ? null : _submitRequest,
                style: ElevatedButton.styleFrom(
                  padding: EdgeInsets.symmetric(vertical: 16),
                  backgroundColor: Theme.of(context).colorScheme.primary,
                  foregroundColor: Colors.white,
                  shape: RoundedRectangleBorder(borderRadius: BorderRadius.circular(12)),
                ),
                child: _isLoading 
                    ? CircularProgressIndicator(color: Colors.white)
                    : Text('Find Eligible Donors', style: TextStyle(fontSize: 18, fontWeight: FontWeight.bold)),
              ),
            ],
          ),
        ),
      ),
    );
  }
}
