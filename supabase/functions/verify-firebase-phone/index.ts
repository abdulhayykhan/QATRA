import { createClient } from "https://esm.sh/@supabase/supabase-js@2";
import { cert, getApps, initializeApp } from "npm:firebase-admin/app";
import { getAuth } from "npm:firebase-admin/auth";

const supabaseUrl = Deno.env.get("SUPABASE_URL")!;
const serviceRoleKey = Deno.env.get("SUPABASE_SERVICE_ROLE_KEY")!;
const firebaseProjectId = Deno.env.get("FIREBASE_PROJECT_ID")!;
const firebaseClientEmail = Deno.env.get("FIREBASE_CLIENT_EMAIL")!;
const firebasePrivateKey = Deno.env.get("FIREBASE_PRIVATE_KEY")!.replaceAll("\\n", "\n");

const supabase = createClient(supabaseUrl, serviceRoleKey);
const firebaseApp = getApps().length > 0
  ? getApps()[0]
  : initializeApp({
      credential: cert({
        projectId: firebaseProjectId,
        clientEmail: firebaseClientEmail,
        privateKey: firebasePrivateKey
      })
    });
const firebaseAuth = getAuth(firebaseApp);

function randomPassword(): string {
  return `${crypto.randomUUID()}-${crypto.randomUUID()}!`;
}

async function findUserByPhone(phone: string) {
  for (let pageNumber = 1; ; pageNumber += 1) {
    const page = await supabase.auth.admin.listUsers({ page: pageNumber, perPage: 1000 });
    const match = page.data.users.find((user) => user.phone === phone);
    if (match || page.data.users.length < 1000) return match ?? null;
  }
}

Deno.serve(async (request) => {
  if (request.method !== "POST") {
    return Response.json({ error: "Method not allowed" }, { status: 405 });
  }

  try {
    const body = await request.json();
    const firebaseIdToken = body?.firebase_id_token;
    if (typeof firebaseIdToken !== "string" || firebaseIdToken.length === 0) {
      return Response.json({ error: "firebase_id_token is required" }, { status: 400 });
    }

    const decodedToken = await firebaseAuth.verifyIdToken(firebaseIdToken, false);
    const phone = decodedToken.phone_number;
    if (typeof phone !== "string" || phone.length === 0) {
      return Response.json({ error: "Firebase token has no phone_number claim" }, { status: 401 });
    }

    const password = randomPassword();
    let user = await findUserByPhone(phone);
    if (!user) {
      const created = await supabase.auth.admin.createUser({
        phone,
        phone_confirm: true,
        password
      });
      if (created.error || !created.data.user) throw created.error ?? new Error("Supabase user creation failed");
      user = created.data.user;
    } else {
      const updated = await supabase.auth.admin.updateUserById(user.id, { password });
      if (updated.error) throw updated.error;
    }

    const session = await supabase.auth.signInWithPassword({ phone, password });
    if (session.error || !session.data.session) throw session.error ?? new Error("Supabase session creation failed");

    return Response.json({
      access_token: session.data.session.access_token,
      refresh_token: session.data.session.refresh_token,
      expires_in: session.data.session.expires_in,
      user: {
        id: user.id,
        phone
      }
    });
  } catch (error) {
    console.error("Firebase phone exchange failed", error);
    return Response.json({ error: "Invalid, expired, or unusable Firebase ID token" }, { status: 401 });
  }
});
