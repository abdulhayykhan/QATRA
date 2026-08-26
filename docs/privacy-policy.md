# QATRA Privacy Policy

**Last Updated:** August 27, 2026

**Effective Date:** August 27, 2026

---

## Introduction

QATRA ("the App") is an emergency blood donation matching platform developed and operated by **Alkhidmat Foundation Pakistan**. This Privacy Policy describes how we collect, use, share, and protect your personal information when you use our mobile application.

By using QATRA, you agree to the practices described in this policy. If you do not agree, please discontinue use of the App.

---

## 1. Information We Collect

To provide emergency blood donation matching services, we collect the following categories of personal data:

### 1.1 Account Information
- **Phone Number** — Used as your primary identifier and for OTP-based authentication via Firebase Phone Auth.

### 1.2 Identity Verification
- **CNIC Number** — Collected for donor identity verification. Your CNIC is stored as a **one-way cryptographic hash** and is never stored in plain text. The original CNIC value cannot be reconstructed from the stored hash.

### 1.3 Location Data
- **GPS Location** — Collected when you use the App to find nearby donors or hospitals. Location data enables proximity-based donor matching and geo-fenced push alerts. You may opt out of location tracking at any time through your device settings.

### 1.4 Medical Information
- **Blood Group** — Required for matching donors with seekers.
- **Medical Documents (Hospital Slips/Requisition Forms)** — Uploaded by seekers to verify the legitimacy of blood requests. These images are processed using on-device OCR and stored securely for verification purposes.

### 1.5 Automatically Collected Data
- Device identifiers and push notification tokens (via Firebase Cloud Messaging)
- App usage analytics and crash reports
- Timestamps of requests and donations

---

## 2. How We Use Your Information

Your information is used exclusively for the following purposes:

- **Emergency Matching** — Connecting blood seekers with eligible nearby donors based on blood group, location proximity, and availability.
- **Identity Verification** — Verifying donor identity to prevent fraud and ensure safety.
- **Push Notifications** — Sending geo-fenced emergency alerts to eligible donors in the vicinity of a blood request.
- **Request Verification** — Reviewing uploaded hospital requisition slips to validate the authenticity of blood requests.
- **Service Improvement** — Analyzing aggregated, anonymized usage data to improve response times and platform reliability.

---

## 3. Data Sharing

QATRA is designed to minimize data exposure:

- **Matched Donors** receive only the seeker's blood group, hospital name, urgency level, and approximate location — never the seeker's phone number, CNIC, or full address. Communication between matched parties occurs through the App's masked calling feature.
- **Seekers** receive only the matched donor's blood group and availability status — never the donor's CNIC, phone number, or exact location.
- **Alkhidmat Foundation** administrators may access verification documents (CNIC images, hospital slips) solely for the purpose of approving or rejecting donor registrations and verifying blood requests.
- **No data is sold, rented, or shared with advertisers or marketing companies.**

---

## 4. Third-Party Services

The App relies on the following third-party service providers:

| Service | Provider | Purpose |
|---|---|---|
| Firebase Authentication (Phone Auth) | Google LLC | OTP-based phone number verification |
| Firebase Cloud Messaging | Google LLC | Push notifications and geo-fenced alerts |
| Firebase App Check | Google LLC | Bot and abuse prevention |
| Supabase (PostgreSQL + PostGIS) | Supabase Inc. | Encrypted data storage, geospatial queries, and row-level security |

Each provider operates under its own privacy policy. Google's data processing is governed by the [Google Privacy Policy](https://policies.google.com/privacy). Supabase's data processing is governed by the [Supabase Privacy Policy](https://supabase.com/privacy).

---

## 5. Data Security

We implement industry-standard security measures to protect your data:

- **Encryption in Transit** — All data transmitted between the App and our servers is encrypted using TLS 1.2+.
- **Encryption at Rest** — All stored data is encrypted using Supabase's built-in encryption mechanisms.
- **CNIC Hashing** — CNIC numbers are stored exclusively as one-way cryptographic hashes. Plain-text CNIC values are never persisted.
- **Row-Level Security (RLS)** — Database access is enforced through PostgreSQL Row-Level Security policies, ensuring users can only access data they are authorized to view.
- **Masked Communication** — Direct calls between matched donors and seekers use proxy numbers to prevent phone number exposure.

---

## 6. Data Retention

- **Account Data** — Retained for the duration your account remains active.
- **Request Data** — Blood request details, uploaded hospital slips, and matching records are retained for **90 days** after request completion, then permanently deleted.
- **Push Tokens** — Retained while your account is active; deleted upon account deactivation.
- **Verification Documents** — CNIC images uploaded during donor registration are retained while the donor account remains active and for 30 days after deactivation, then permanently deleted.

---

## 7. Your Rights

You have the following rights regarding your personal data:

### 7.1 Access Your Data
You may request a copy of all personal data we hold about you by contacting support@qatra.app.

### 7.2 Data Deletion
You may request the deletion of your account and all associated personal data at any time by contacting support@qatra.app. Deletion requests are processed within 30 days.

### 7.3 Location Opt-Out
You may disable GPS location tracking at any time through your device's location settings. Note that disabling location will prevent the App from providing proximity-based donor matching and geo-fenced alerts.

### 7.4 Notification Opt-Out
You may disable push notifications through your device's notification settings. This will prevent you from receiving emergency blood request alerts.

---

## 8. Children's Privacy

QATRA is **not intended for users under the age of 18**. We do not knowingly collect personal information from minors. If we become aware that a minor has provided us with personal data, we will take steps to delete such information promptly.

---

## 9. Changes to This Policy

We may update this Privacy Policy from time to time. When material changes are made:

- Users will be **notified via an in-app notification** before the changes take effect.
- The "Last Updated" date at the top of this document will be revised accordingly.
- Continued use of the App after changes take effect constitutes acceptance of the updated policy.

---

## 10. Terms of Service

By using QATRA, you agree to:

- Provide accurate and truthful information during registration and blood requests.
- Use the App solely for legitimate emergency blood donation purposes.
- Not misuse the platform, submit fraudulent requests, or attempt to circumvent security measures.
- Comply with all applicable laws and regulations in Pakistan.
- Accept that Alkhidmat Foundation is not liable for the outcome of any blood donation arrangement facilitated through the App.

---

## 11. Contact Us

For privacy-related inquiries, data access requests, or deletion requests:

- **Email:** support@qatra.app
- **Organization:** Alkhidmat Foundation Pakistan

---

*This Privacy Policy is governed by the laws of the Islamic Republic of Pakistan.*
