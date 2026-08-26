-- Donor-side write RPCs: emergency-dispatch acceptance and donation completion.
--
-- Both are SECURITY DEFINER and derive the acting donor from auth.uid() (never a
-- client-supplied id), then touch only rows that donor owns. This mirrors the
-- existing sensitive-path RPC find_eligible_donors_for_request and keeps each
-- multi-row transition atomic. The accept path is the FR 1.4 "broadcast lock":
-- the first responder flips the request to DONOR_MATCHED. Because they key on
-- auth.uid() rather than the user_role JWT claim, they enforce ownership even
-- before the custom access-token hook is enabled.
--
-- Applied to live project glbxtvuanufqjsyjllcq on 2026-08-26 via Supabase MCP;
-- the DML was self-verified live with a seeded donor/request that asserts the
-- DONOR_MATCHED transition, idempotent re-accept, and the cooldown write, then
-- rolls the seed back.

-- Donor accepts an emergency dispatch: records the responder in
-- matched_donor_requests and locks the request to DONOR_MATCHED. Idempotent — a
-- second accept by the same donor is a no-op and never double-counts. Returns
-- the request's resulting status.
create or replace function public.accept_emergency_dispatch(p_request_id uuid)
returns text
language plpgsql
volatile
security definer
set search_path = public, extensions
as $$
declare
  v_donor_id uuid;
  v_blood_group text;
  v_status text;
  v_rows int;
begin
  select id into v_donor_id
    from public.donor_profiles
    where auth_user_id = auth.uid();
  if v_donor_id is null then
    raise exception 'No donor profile for the current user' using errcode = '42501';
  end if;

  select blood_group, status into v_blood_group, v_status
    from public.blood_requests
    where id = p_request_id;
  if not found then
    raise exception 'Blood request not found' using errcode = 'P0002';
  end if;

  -- ponytail: idempotency via not-exists guard; add a unique (request_id, donor_id)
  -- index if concurrent double-accepts ever become a real contention point.
  insert into public.matched_donor_requests (request_id, donor_id, blood_group, status_text)
  select p_request_id, v_donor_id, v_blood_group, 'Accepted'
  where not exists (
    select 1 from public.matched_donor_requests
    where request_id = p_request_id and donor_id = v_donor_id
  );
  get diagnostics v_rows = row_count;

  if v_rows > 0 then
    update public.blood_requests
      set responded_donors_count = responded_donors_count + 1,
          status = case when status in ('VERIFYING', 'BROADCASTING') then 'DONOR_MATCHED' else status end,
          updated_at = now()
      where id = p_request_id
      returning status into v_status;
  end if;

  return v_status;
end;
$$;

-- Donor marks a donation complete: starts the 90-day cooldown and increments the
-- lifetime count for the caller's own donor profile. (Rating/thank-you note have
-- no backend sink yet, so they are not persisted here — see the Kotlin caller.)
create or replace function public.complete_donation()
returns void
language plpgsql
volatile
security definer
set search_path = public, extensions
as $$
declare
  v_donor_id uuid;
begin
  select id into v_donor_id
    from public.donor_profiles
    where auth_user_id = auth.uid();
  if v_donor_id is null then
    raise exception 'No donor profile for the current user' using errcode = '42501';
  end if;

  update public.donor_profiles
    set is_available_to_donate = false,
        is_eligible = false,
        cooldown_days_remaining = 90,
        lifetime_donations = lifetime_donations + 1,
        updated_at = now()
    where id = v_donor_id;
end;
$$;

-- Client roles call these through PostgREST; keep them off anon/public.
revoke execute on function public.accept_emergency_dispatch(uuid) from anon, public;
grant  execute on function public.accept_emergency_dispatch(uuid) to authenticated;
revoke execute on function public.complete_donation() from anon, public;
grant  execute on function public.complete_donation() to authenticated;
