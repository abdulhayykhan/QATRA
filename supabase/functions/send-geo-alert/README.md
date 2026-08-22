# send-geo-alert

Deploy this function and configure a Supabase Database Webhook for `INSERT` on `public.blood_requests`.
The webhook should send the inserted row as `{ "record": { ... } }` and include the `x-webhook-secret` header.

Required Supabase secrets:

- `SUPABASE_URL`
- `SUPABASE_SERVICE_ROLE_KEY`
- `FIREBASE_PROJECT_ID`
- `FIREBASE_CLIENT_EMAIL`
- `FIREBASE_PRIVATE_KEY` (the service-account PEM; escaped newlines are accepted)
- `GEO_ALERT_WEBHOOK_SECRET`

The function only processes rows where `is_verified = true` and `status = 'BROADCASTING'`.
It calls `find_eligible_donors_for_request` with the fixed base radius of 10 km, reads tokens from
`donor_device_tokens`, and sends data-only high-priority FCM messages. Radius expansion,
rare-blood bypass, and masked calling remain outside this function.
