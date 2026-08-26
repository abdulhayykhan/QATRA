-- Single-use replay ledger for Firebase phone-auth ID tokens.
--
-- verify-firebase-phone exchanges a Firebase ID token for a Supabase session.
-- A Firebase ID token is a bearer JWT valid for ~1 hour, so a leaked token
-- could be replayed repeatedly within that window to mint fresh sessions for
-- the victim's phone. This ledger records the SHA-256 of each token the moment
-- it is accepted; the primary key makes a second exchange of the same token a
-- unique-violation, which the function turns into a 409. Rows are pruned once
-- the token's own expiry passes (it is useless after that anyway).
--
-- RLS is enabled with no policies: the anon/authenticated roles get no access,
-- and the Edge Function writes with the service_role key, which bypasses RLS.
--
-- Applied to live project glbxtvuanufqjsyjllcq on 2026-08-26 via Supabase MCP.

create table if not exists public.firebase_phone_token_ledger (
  token_hash   text primary key,
  firebase_uid text        not null,
  phone        text        not null,
  expires_at   timestamptz not null,
  consumed_at  timestamptz not null default now()
);

create index if not exists firebase_phone_token_ledger_expires_at_idx
  on public.firebase_phone_token_ledger (expires_at);

alter table public.firebase_phone_token_ledger enable row level security;
