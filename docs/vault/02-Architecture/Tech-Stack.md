---
tags: [architecture, tech-stack]
status: active
last-updated: 2026-08-23
---
# Tech Stack

- **Mobile Client**: Kotlin + Jetpack Compose, Material 3. See [app/](file:///c:/Users/USER/OneDrive%20-%20Dawood%20University%20of%20Engineering%20Technology/Desktop/AKK-SSIP/QATRA/app/)
- **Auth**: Supabase Auth, bridged via a custom Edge Function (`verify-firebase-phone`). See [verify-firebase-phone](file:///c:/Users/USER/OneDrive%20-%20Dawood%20University%20of%20Engineering%20Technology/Desktop/AKK-SSIP/QATRA/supabase/functions/verify-firebase-phone/)
- **Database**: Supabase Postgres + PostGIS. See [qatra_postgres_schema.sql](file:///c:/Users/USER/OneDrive%20-%20Dawood%20University%20of%20Engineering%20Technology/Desktop/AKK-SSIP/QATRA/database/qatra_postgres_schema.sql)
- **File Storage**: Supabase Storage, two private buckets (`hospital-slips`, `cnic-documents`).
- **Geo-Matching**: PostGIS `ST_DWithin` / `ST_Distance` via a Supabase RPC (`find_eligible_donors_for_request`). See [qatra_proximity_rpc_test.sql](file:///c:/Users/USER/OneDrive%20-%20Dawood%20University%20of%20Engineering%20Technology/Desktop/AKK-SSIP/QATRA/database/qatra_proximity_rpc_test.sql)
- **Push Notifications**: Firebase Cloud Messaging + Supabase Edge Function (`send-geo-alert`). See [send-geo-alert](file:///c:/Users/USER/OneDrive%20-%20Dawood%20University%20of%20Engineering%20Technology/Desktop/AKK-SSIP/QATRA/supabase/functions/send-geo-alert/)
