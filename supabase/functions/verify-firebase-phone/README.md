# verify-firebase-phone

Exchanges a verified Firebase phone-auth identity for a Supabase Auth session.
The Firebase ID token is verified server-side and is never stored or returned.

## Required secrets

Reuse the existing Edge Function secret names:

- `SUPABASE_URL`: Supabase project URL.
- `SUPABASE_SERVICE_ROLE_KEY`: Supabase service-role key. Server-side only; never ship it in Android.
- `FIREBASE_PROJECT_ID`: Firebase project ID.
- `FIREBASE_CLIENT_EMAIL`: Firebase Admin service-account client email.
- `FIREBASE_PRIVATE_KEY`: Firebase Admin service-account private key, with escaped `\\n` accepted.

## Request

```json
{
  "firebase_id_token": "<Firebase ID token from PhoneAuthProvider>"
}
```

## Successful response

```json
{
  "access_token": "<Supabase access token>",
  "refresh_token": "<Supabase refresh token>",
  "expires_in": 3600,
  "user": {
    "id": "<Supabase auth user UUID>",
    "phone": "+923001234567"
  }
}
```

## Error response

Invalid, expired, malformed, or phone-less Firebase tokens return HTTP `401`:

```json
{
  "error": "Invalid, expired, or unusable Firebase ID token"
}
```

Missing `firebase_id_token` returns HTTP `400`.

A token that has already been exchanged returns HTTP `409`:

```json
{
  "error": "This verification token has already been used"
}
```

## Manual curl test

Use a freshly issued Firebase ID token. Do not put a service-role key in this request.

```bash
curl -i -X POST "https://<project-ref>.supabase.co/functions/v1/verify-firebase-phone" \
  -H "apikey: <supabase-anon-key>" \
  -H "Authorization: Bearer <supabase-anon-key>" \
  -H "Content-Type: application/json" \
  --data '{"firebase_id_token":"<fresh-firebase-id-token>"}'
```

## Session bridge and replay considerations

Supabase Auth does not expose a clean Admin API call to mint a session for an existing
passwordless user without credentials. The function therefore creates a user with a
random password when needed, or rotates an existing user to a random password, then
performs a server-side phone/password sign-in. The random password never leaves the
function; the client receives only the normal Supabase session.

Firebase ID tokens are bearer tokens and are valid until their Firebase expiry. Firebase
Admin verification rejects invalid and expired tokens, and is called here with revocation
checking enabled (`verifyIdToken(token, true)`), so tokens for sessions Firebase has revoked
are also rejected.

Firebase ID tokens are not single-use on Firebase's side, so this function enforces single
use itself. It records the SHA-256 of each accepted token in the
`public.firebase_phone_token_ledger` table (`token_hash` primary key) **before** minting a
Supabase session — fail-closed, so there is no window in which a replay could succeed. A
second exchange of the same token hits the primary-key unique constraint and returns HTTP
`409`. Rows whose `expires_at` has passed are pruned opportunistically on each call (best
effort; a failed prune never blocks a valid login), which keeps the ledger bounded without a
scheduled job. The table has RLS enabled with no policies, so only the service-role function
can read or write it.
