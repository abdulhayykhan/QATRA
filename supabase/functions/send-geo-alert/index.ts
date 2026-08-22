import { createClient } from "https://esm.sh/@supabase/supabase-js@2";

const supabaseUrl = Deno.env.get("SUPABASE_URL")!;
const serviceRoleKey = Deno.env.get("SUPABASE_SERVICE_ROLE_KEY")!;
const firebaseProjectId = Deno.env.get("FIREBASE_PROJECT_ID")!;
const firebaseClientEmail = Deno.env.get("FIREBASE_CLIENT_EMAIL")!;
const firebasePrivateKey = Deno.env.get("FIREBASE_PRIVATE_KEY")!;
const webhookSecret = Deno.env.get("GEO_ALERT_WEBHOOK_SECRET");
const supabase = createClient(supabaseUrl, serviceRoleKey);

const base64UrlEncode = (value: string): string =>
  btoa(value).replaceAll("+", "-").replaceAll("/", "_").replaceAll("=", "");

const pemToBytes = (pem: string): Uint8Array => {
  const base64 = pem.replace(/-----BEGIN PRIVATE KEY-----|-----END PRIVATE KEY-----|\s/g, "");
  const binary = atob(base64);
  return Uint8Array.from(binary, (character) => character.charCodeAt(0));
};

async function createGoogleAccessToken(): Promise<string> {
  const now = Math.floor(Date.now() / 1000);
  const header = base64UrlEncode(JSON.stringify({ alg: "RS256", typ: "JWT" }));
  const claims = base64UrlEncode(JSON.stringify({
    iss: firebaseClientEmail,
    scope: "https://www.googleapis.com/auth/firebase.messaging",
    aud: "https://oauth2.googleapis.com/token",
    iat: now,
    exp: now + 3600
  }));
  const signingInput = `${header}.${claims}`;
  const key = await crypto.subtle.importKey(
    "pkcs8",
    pemToBytes(firebasePrivateKey.replaceAll("\\n", "\n")),
    { name: "RSASSA-PKCS1-v1_5", hash: "SHA-256" },
    false,
    ["sign"]
  );
  const signature = await crypto.subtle.sign(
    "RSASSA-PKCS1-v1_5",
    key,
    new TextEncoder().encode(signingInput)
  );
  const signatureString = base64UrlEncode(String.fromCharCode(...new Uint8Array(signature)));
  const assertion = `${signingInput}.${signatureString}`;
  const response = await fetch("https://oauth2.googleapis.com/token", {
    method: "POST",
    headers: { "content-type": "application/x-www-form-urlencoded" },
    body: new URLSearchParams({
      grant_type: "urn:ietf:params:oauth:grant-type:jwt-bearer",
      assertion
    })
  });
  if (!response.ok) throw new Error(`FCM OAuth token request failed: ${await response.text()}`);
  const tokenResponse = await response.json();
  return tokenResponse.access_token;
}

Deno.serve(async (request) => {
  if (webhookSecret && request.headers.get("x-webhook-secret") !== webhookSecret) {
    return new Response("Unauthorized", { status: 401 });
  }

  const webhook = await request.json();
  const record = webhook.record ?? webhook;
  if (!record.id || record.is_verified !== true || record.status !== "BROADCASTING") {
    return Response.json({ skipped: true });
  }

  const radiusKm = 10;
  const { data: matches, error: matchError } = await supabase.rpc(
    "find_eligible_donors_for_request",
    { p_request_id: record.id, p_radius_km: radiusKm }
  );
  if (matchError) throw matchError;

  const donorIds = (matches ?? []).map((match: { donor_id: string }) => match.donor_id);
  if (donorIds.length === 0) return Response.json({ sent: 0, matched: 0 });

  const [{ data: hospital, error: hospitalError }, { data: tokenRows, error: tokenError }] = await Promise.all([
    supabase.from("hospitals").select("name, short_name").eq("id", record.hospital_id).single(),
    supabase.from("donor_device_tokens").select("donor_id, token").in("donor_id", donorIds)
  ]);
  if (hospitalError) throw hospitalError;
  if (tokenError) throw tokenError;

  const accessToken = await createGoogleAccessToken();
  let sent = 0;
  for (const row of tokenRows ?? []) {
    const match = (matches ?? []).find((candidate: { donor_id: string }) => candidate.donor_id === row.donor_id);
    const data = {
      type: "GEO_FENCED_BLOOD_REQUEST",
      request_id: String(record.id),
      blood_group: String(record.blood_group),
      hospital_name: String(hospital?.short_name ?? hospital?.name ?? "Hospital"),
      urgency: String(record.urgency),
      distance_km: Number(match?.distance_km ?? 0).toFixed(1),
      units: String(record.units_required ?? 1),
      component: String(record.component ?? "PRBC"),
      eta_minutes: String(match?.eta_minutes ?? 0)
    };
    const response = await fetch(`https://fcm.googleapis.com/v1/projects/${firebaseProjectId}/messages:send`, {
      method: "POST",
      headers: {
        authorization: `Bearer ${accessToken}`,
        "content-type": "application/json"
      },
      body: JSON.stringify({
        message: {
          token: row.token,
          data,
          android: { priority: "HIGH" }
        }
      })
    });
    if (response.ok) sent += 1;
  }

  return Response.json({ sent, matched: donorIds.length });
});
