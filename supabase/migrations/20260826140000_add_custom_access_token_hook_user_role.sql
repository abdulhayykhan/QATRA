-- Custom access token hook: lift user_role from app_metadata to a TOP-LEVEL
-- JWT claim so RLS policies that read auth.jwt() ->> 'user_role' work.
--
-- Every QATRA RLS policy gates on a top-level user_role claim, but Supabase
-- keeps custom data under app_metadata, so without this hook the claim is NULL
-- for every authenticated user and all role-gated policies fail closed. Users
-- with no role default to 'guest' (the least-privileged role), which matches
-- the app's onboarding state before seeker/donor verification is approved.
--
-- Applied to live project glbxtvuanufqjsyjllcq on 2026-08-26 via Supabase MCP.
--
-- MANUAL STEP REQUIRED to activate on the live project: the function is only
-- invoked once the hook is enabled in GoTrue config, which is not settable via
-- SQL. In the Supabase dashboard go to Authentication -> Hooks -> "Customize
-- Access Token (JWT) Claims", enable it, and select public.custom_access_token_hook.
-- (Equivalently, PATCH the project auth config via the Management API with
--  hook_custom_access_token_enabled=true and
--  hook_custom_access_token_uri=pg-functions://postgres/public/custom_access_token_hook.)
create or replace function public.custom_access_token_hook(event jsonb)
returns jsonb
language plpgsql
stable
as $$
declare
  claims jsonb;
  resolved_role text;
begin
  select coalesce(u.raw_app_meta_data ->> 'user_role', 'guest')
    into resolved_role
    from auth.users u
    where u.id = (event ->> 'user_id')::uuid;

  claims := coalesce(event -> 'claims', '{}'::jsonb);
  claims := jsonb_set(claims, '{user_role}', to_jsonb(coalesce(resolved_role, 'guest')));
  return jsonb_set(event, '{claims}', claims);
end;
$$;

-- Only GoTrue may run the hook; keep it off limits to client roles.
grant execute on function public.custom_access_token_hook(jsonb) to supabase_auth_admin;
revoke execute on function public.custom_access_token_hook(jsonb) from authenticated, anon, public;
