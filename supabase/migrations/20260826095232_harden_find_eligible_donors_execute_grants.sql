-- Harden EXECUTE on the donor-matching RPC.
--
-- Postgres grants EXECUTE on new functions to the PUBLIC pseudo-role by default,
-- and the anon (unauthenticated) role inherits from PUBLIC. So before this
-- migration, an anonymous client could call find_eligible_donors_for_request and
-- read donor phone_masked / verification / distance-derived data for any
-- broadcasting request. Revoking FROM anon alone is a no-op while the grant lives
-- on PUBLIC, so revoke from PUBLIC and anon, then grant only to the roles that
-- legitimately run the match.
--
-- Applied to live project glbxtvuanufqjsyjllcq on 2026-08-26 via Supabase MCP.

revoke execute on function public.find_eligible_donors_for_request(uuid, double precision) from public, anon;
grant execute on function public.find_eligible_donors_for_request(uuid, double precision) to authenticated, service_role;
