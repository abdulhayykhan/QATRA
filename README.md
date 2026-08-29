<p align="center">
  <img src="media/logo.png" alt="QATRA logo" width="120" />
</p>

<h1 align="center">QATRA</h1>
<p align="center"><em>Connecting Verified Seekers to Eligible Donors — in Minutes, Not Hours</em></p>

<p align="center">
  <img src="https://img.shields.io/badge/status-closed--pilot--candidate-orange" alt="status" />
  <img src="https://img.shields.io/badge/platform-Android%20%26%20iOS%20(Flutter)-3DDC84" alt="platform" />
  <img src="https://img.shields.io/badge/backend-FastAPI%20%2B%20PostgreSQL-3ECF8E" alt="backend" />
  <img src="https://img.shields.io/badge/org-Alkhidmat%20Foundation%20Pakistan-8B1E1E" alt="org" />
</p>

---

> **Before anything else:** if you're setting this project up from a workspace export rather than a fresh `git clone`, read [Security Notes](#security-notes) first. Credentials have leaked from workspace exports of this project before.

## Status

**This is a closed-pilot candidate, not a production-ready application.** The core Seeker → Donor → Admin loop is implemented against a real Supabase/Firebase backend, but several pieces are authored-and-reviewed without being verified against a live environment, and a few are still intentionally mocked. See [What's Actually Implemented](#whats-actually-implemented) and [Known Gaps & Open Risks](#known-gaps--open-risks) below for the honest, current breakdown — this section is kept up to date as the project progresses, rather than describing an aspirational end state.

---

## About

QATRA (formerly documented as the **Emergency Blood Response Platform**) is a project built for **Alkhidmat Foundation Pakistan**, replacing the informal WhatsApp-broadcast model of emergency blood requests with a structured, verified, and fast digital system.

When a patient urgently needs blood, families currently forward WhatsApp messages across group chats — a process that loses context, arrives too late, and offers no way to verify whether a request is even real. QATRA connects three groups that today operate through disconnected, informal channels:

* **Emergency blood seekers** — patients' families and hospital staff
* **Verified donors** — pre-screened and eligibility-tracked
* **Campus & community drive organizers** — running Alkhidmat's donor pipelines

**Version 1.0 goal:** verified request in → matched, eligible donor out, in under 15 minutes. Scoped to Karachi trauma centers and Alkhidmat's existing campus chapters.

## Core Problems Addressed

* **Emergency search bottlenecks** — WhatsApp chains cause hours of delay and duplicated/contradictory information exactly when speed matters most.
* **Donor trust & verification gaps** — no reliable way to tell a legitimate hospital request from a scam or commercial reseller.
* **Social & cultural awareness barriers** — myths about donor eligibility and unfamiliarity with the process shrink the usable donor pool.

## Key Features

| Module | Description | Build status |
|---|---|---|
| **Live Map & Proximity Matching** | Plots verified requests at hospital coordinates; geo-fenced dispatch to eligible donors within a configurable radius, via a real PostGIS `ST_DWithin`/`ST_Distance` RPC. | Implemented, not yet verified live |
| **Auth, Verification & Eligibility** | Firebase Phone Auth bridged to a FastAPI session, CNIC format/district-range checks, real camera capture + on-device ML Kit OCR for hospital slips, 90-day donor cooldown tracking, 4-step pre-screening checklist. | Implemented, not yet verified live |
| **Social Feed** | Structured, filterable posts replacing WhatsApp spam — filter by blood group, urgency, and location, with one-tap "I Can Donate" response. | Implemented (mocked data) |
| **Awareness & Eligibility Module** | Educational content library, myth-busting resources, an interactive eligibility checker, and campus/community blood drive registration with QR check-in. | Implemented (mocked data) |
| **Push Notifications** | Firebase Cloud Messaging, triggered by a FastAPI background task on new verified requests. | Implemented, not yet verified live |
| **Row-Level Security** | 15 tables, 50+ Postgres RLS policies enforcing per-role and per-ownership access. | Authored and reviewed; **never applied or run against a live database** |
| **Admin / Verification Desk** | Hospital-slip review queue, fraud audit table, campus drive management. | UI implemented; **admin login is still a mock with no real auth gate** |
| **Direct Calling** | `Intent.ACTION_DIAL` to a real matched party's number, gated by a relationship-scoped RLS policy. | Implemented, not yet verified live — **note this is a documented scope change from the PRD's masked-calling design, see below** |

## Project Goals (per PRD)

1. Reduce emergency search-to-connection time to under **15 minutes** for urgent requests.
2. Ensure **100%** of live requests are verified via hospital admission slip + CNIC before broadcast.
3. Maintain a pre-screened, cooldown-tracked donor pool to eliminate avoidable on-site drive disqualifications.
4. Give community/campus drive leads a centralized tool to run their own blood drives.

**Out of scope for V1 (per PRD):** physical blood delivery/logistics, any paid or commercial blood transactions, direct hospital EMR integration, and geographic expansion beyond Karachi.

**Additional scope change made during implementation, not in the original PRD:** masked/proxy phone calling (PRD Section 4.4) was replaced with direct dialing between matched parties, for cost reasons — proxy-calling providers are paid, per-minute services outside the current budget. This means the PRD's Section 7.4 KPI ("Contact Privacy Breaches: Zero direct phone exposures") is no longer met by design. **This needs explicit sign-off from the Alkhidmat supervisor**, since it changes a requirement the team already signed off on in the PRD. See [Known Gaps & Open Risks](#known-gaps--open-risks).

---

## What's Actually Implemented

A Flutter app implementing the Seeker (10-screen), Donor (8-screen), and Admin/Verification-Desk (4-screen) journeys from the wireframe set, backed by a real FastAPI and PostgreSQL backend for auth, geo-matching, and file storage, with Firebase Cloud Messaging for push notifications.

The project started as an AI-generated, fully-mocked UI prototype and has been converted, module by module, to real backend integrations. Not every module has made that transition yet, and — importantly — **not every "real" integration has been verified against a live, running environment.** The distinction matters: code that's been authored, reviewed, and passes static/editor diagnostics is not the same as code that's been proven to work.

### Tech stack (as actually implemented)

| Layer | Technology |
|---|---|
| Mobile client | Flutter |
| Backend | FastAPI (Python) |
| Database | PostgreSQL + PostGIS |
| Auth | Firebase Phone Auth + FastAPI custom session management |
| File storage | Local / Cloud storage abstractions |
| Geo-matching | PostGIS `ST_DWithin` / `ST_Distance` |
| Push notifications | Firebase Cloud Messaging + FastAPI background tasks |
| Row-level security | Handled via FastAPI dependency injection (`authorization.py`) |
| Calling | Direct `url_launcher` `tel:` intent |
| Tests | Pytest |

### Why some of these design choices look unusual

**Auth is two-hop (Firebase Phone Auth → Edge Function → FastAPI session), not native Supabase phone OTP.** This was a mid-build pivot — Supabase's native phone OTP requires configuring a paid SMS provider per project, while Firebase Phone Auth handles SMS delivery natively. The `verify-firebase-phone` function mints a FastAPI session by creating or rotating a user to a random password server-side and immediately signing in with it, working around the backend API not exposing a clean "mint session for existing passwordless user" call. Its own documentation flags that Firebase ID tokens aren't single-use and the function doesn't yet implement replay protection — acceptable for a small closed pilot, worth hardening before wider rollout.

**Sensitive data lives in dedicated tables, not inline.** `donor_private_contacts`, `hospital_slip_documents`, and `request_sensitive_data` are separate from the public-facing `blood_requests` / `donor_profiles` tables, so fast public map/feed queries never touch CNIC or phone data, and RLS on the sensitive tables can be tight and auditable independent of the public ones.

**CNIC validation is honest about its limits.** An earlier iteration implemented a fabricated checksum algorithm on the CNIC's 13th digit — Pakistani CNICs have no published checksum (the last digit indicates gender only), so that check was invented and misleading. It has been removed. Current validation confirms 13 numeric digits and a real district/area prefix range — nothing more. Real identity confirmation would require NADRA Verisys integration, which is out of scope here (no API access). UI copy says "CNIC Format Check," not "Identity Verified," deliberately.

---

## Known Gaps & Open Risks

Ordered by how much they'd hurt if missed before real usage, not by when they were found.

### 1. Nothing has been verified against a live environment yet
Every "real" backend integration — the auth bridge, OCR/upload, the geo-matching RPC, push notifications, and every RLS policy — has been authored and reviewed, and passes local editor diagnostics, but has never run against an actual PostgreSQL database or a real/emulated device with network access. The development environment used to build this has had no Gradle wrapper, no Gradle CLI, no `psql`, and no backend environment available at various points, so verification to date has largely been static review. **Before this reaches a real user: apply the migrations to a live project in order, run the RLS verification queries under all five simulated roles, and manually exercise the full loop on a device.**

### 2. RLS is authored correctly (after one real bug was caught and fixed) but still unapplied
`backend/app/api/authorization.py` covers all 15 tables with 50+ policies. One genuine bug was caught during development: an earlier `blood_requests` guest-access policy had no `status` filter, which — because Postgres OR's multiple permissive SELECT policies together — silently exposed every request row, including pending-review, rejected, and fraud-flagged ones, to any caller regardless of the more careful policies sitting right next to it. This is fixed in the current file, but since nothing has been applied to a live database yet, **this fix is unverified in practice.** Also note: JWT custom-claim roles don't update instantly — with Supabase's default 1-hour token expiry, a role change (e.g. suspending a donor) can take up to an hour to take effect for an already-issued token. Pair high-stakes checks with a database-backed status column, not the JWT claim alone.

### 3. Admin login has no real authentication gate
The admin sign-in screen currently advances straight to the verification queue on tap, with no credential or TOTP check at all — flagged and intentionally deferred early in development since the seeker/donor path was prioritized first. Fix this before any non-developer touches the admin surface, and definitely before any deployment reachable outside a local dev build.

### 4. Direct calling is a real scope change from the PRD, and needs explicit sign-off
Covered above — repeating here because it's a compliance issue, not just a technical one. Document this as an approved V1 change with whoever owns the PRD, rather than letting the code quietly diverge from a document that's already been signed off.

### 5. Test coverage is targeted at known past bugs, not comprehensive
Current tests cover the CNIC validator and an OTP-flow regression test (written specifically because that exact bug existed once). A SQL fixture exists for the geo-matching RPC but requires a disposable Postgres/PostGIS instance and hasn't been run. There's no coverage yet for RLS behavior itself, the OCR/upload flow, push notifications, or calling.

### 6. Firebase-token replay isn't hardened yet
Documented candidly in `supabase/functions/verify-firebase-phone/README.md`: tokens aren't single-use and there's no replay ledger yet. Low risk for a small closed pilot, worth fixing before wider rollout.

---

## System Architecture

QATRA follows a modular architecture: a mobile client communicates through an API gateway to core backend modules — **Auth & Verify**, **Map & Feed**, **Awareness & Quiz**, and **Notifications** — each backed by shared storage (encrypted vault, geo-indexed Postgres/PostGIS, cache layer) and external services (SMS/phone auth, OCR extraction, maps/geocoding, push notifications).

<p align="center">
  <img src="media/system-architecture.jpeg" alt="QATRA system architecture diagram" width="700" />
</p>

> The original architecture proposal is documented in the PRD (Section 8). The implementation described in this README reflects what was actually built, which has diverged from that proposal in a few places (documented above) as real engineering constraints (SMS provider cost, calling-proxy cost, Supabase Admin API limitations) came up during the build.

---

## Repository Structure

```
QATRA/
├── flutter_client/
│   ├── lib/
│   │   ├── main.dart                   App entry point, navigation host
│   │   ├── ui/                         Seeker, Donor, Admin journeys
│   │   ├── data/                       Backend API client
│   │   └── theme/                      Color, typography theme
├── backend/
│   ├── app/
│   │   ├── api/                        FastAPI endpoints & authorization
│   │   ├── core/                       Security & settings
│   │   ├── db/                         SQLAlchemy models & database setup
│   │   └── schemas/                    Pydantic models
│   ├── tests/                          Pytest suite
│   ├── requirements.txt
│   └── alembic/                        Database migrations
├── docs/
│   ├── PRD.pdf                         Full Project Requirement Document
│   └── Wireframes.pdf                  Seeker, Donor & Admin journey wireframes (22 screens)
└── media/
    ├── logo.png
    └── system-architecture.jpeg
```

---

## Setting Up a Development Environment

**Prerequisites:** Flutter SDK, Python 3.10+, PostgreSQL with PostGIS extension, a Firebase project with Phone Auth and Cloud Messaging enabled.

1. Copy `backend/.env.example` to `backend/.env` and fill in real values. Never commit this file.
2. Run backend migrations: `cd backend && alembic upgrade head`.
3. Start backend: `cd backend && uvicorn app.main:app --reload`.
4. Add your own `google-services.json` for Firebase to `flutter_client/android/app/`.
5. Run Flutter client: `cd flutter_client && flutter run`.

---

## Security Notes

`.env`, `local.properties`, `*.jks` keystores, and any service-account JSON files are excluded via `.gitignore` and must **never** be committed or included in any workspace export/zip shared outside the core dev team — `.gitignore` only protects against `git add`, not against zipping the whole folder. If you're ever handed a copy of this project as a full folder/zip rather than a fresh `git clone`, check it for these files before doing anything else, and if found, treat every credential inside as compromised and rotate it immediately (Supabase service_role key, Firebase Admin service-account key, release keystore, webhook secrets).

Report any credential exposure to the team lead immediately rather than assuming someone else will notice.

---

## Documentation

* 📄 [**Project Requirement Document**](docs/PRD.pdf) — goals, personas, functional requirements per module, non-functional requirements & KPIs, and system architecture
* 🎨 [**Wireframes**](docs/Wireframes.pdf) — complete UI wireframes across the Seeker (10 screens), Donor (8 screens), and Admin/Verification Desk (4 screens) journeys

---

## Suggested Next Steps

1. Rotate any credential that has ever appeared in a shared export of this project, if not already done.
2. Apply the three SQL migrations to a real PostgreSQL database and actually run the RLS verification queries — the single highest-value unverified piece of work in the codebase right now.
3. Wire real admin authentication (credentials + TOTP) — currently a bare mock.
4. Manually test the full seeker → donor → fulfilled loop end-to-end on a real device against the real backend.
5. Get explicit sign-off from the Alkhidmat supervisor on the direct-calling scope change versus the PRD's original masked-calling commitment.
6. Add replay protection to `verify-firebase-phone` before moving beyond a small closed pilot.
7. Expand test coverage — particularly something that exercises RLS behavior automatically rather than a manually-run SQL script.
8. Confirm a real Gradle build succeeds in CI, not just editor-level diagnostics.

Once items 1–4 are done, this is a reasonable foundation for a **closed pilot** — one campus chapter, one trauma center. It is not yet the PRD's full 10,000-concurrent-user, NADRA-verified, penetration-tested Karachi-wide rollout, and shouldn't be represented as such until the gaps above are closed.

---

## Team

Built by a 6-member group for the Alkhidmat Foundation Pakistan IT internship:

| Role | Member |
|---|---|
| Live Map & Proximity Matching | Hareem Israr |
| Auth, Verification & Eligibility | Saghir Ahmed |
| Social Feed | Mahrukh Baig |
| Awareness & Eligibility Module | Yumna Abbasi |
| Non-Functional Requirements & KPIs | Nimra Iftikhar |
| Team Lead / Architecture | Abdul Hayy Khan |

## License
This project is licensed under the MIT License. Developed by [Abdul Hayy Khan](https://www.linkedin.com/in/abdul-hayy-khan/).
