<p align="center">
  <img src="media/logo.png" alt="QATRA logo" width="120" />
</p>

```text
                  ██████╗   █████╗  ████████╗ ██████╗   █████╗ 
                ██╔═══██╗ ██╔══██╗ ╚══██╔══╝ ██╔══██╗ ██╔══██╗
                ██║   ██║ ███████║    ██║    ██████╔╝ ███████║
                ██║▄▄ ██║ ██╔══██║    ██║    ██╔══██╗ ██╔══██║
                ╚██████╔╝ ██║  ██║    ██║    ██║  ██║ ██║  ██║
                  ╚══▀▀═╝  ╚═╝  ╚═╝    ╚═╝    ╚═╝  ╚═╝ ╚═╝  ╚═╝
      Connecting Verified Seekers to Eligible Donors — in Minutes, Not Hours
```

![Android](https://img.shields.io/badge/Android-Flutter-3DDC84?style=for-the-badge&logo=android&logoColor=white)
![Flutter](https://img.shields.io/badge/Flutter-Dart-02569B?style=for-the-badge&logo=flutter&logoColor=white)
![FastAPI](https://img.shields.io/badge/FastAPI-Python%203.12-009688?style=for-the-badge&logo=fastapi&logoColor=white)
![Postgres](https://img.shields.io/badge/PostgreSQL-PostGIS-4169E1?style=for-the-badge&logo=postgresql&logoColor=white)
![Supabase](https://img.shields.io/badge/Supabase-DB%20%2F%20Storage-3ECF8E?style=for-the-badge&logo=supabase&logoColor=white)
![Firebase](https://img.shields.io/badge/Firebase-Auth%20%2F%20FCM-FFCA28?style=for-the-badge&logo=firebase&logoColor=black)
![Docker](https://img.shields.io/badge/Docker-Deployed%20on%20AWS%20EC2-2496ED?style=for-the-badge&logo=docker&logoColor=white)
![Status](https://img.shields.io/badge/status-closed--pilot--candidate-orange?style=for-the-badge)
![License](https://img.shields.io/badge/License-MIT-green?style=for-the-badge)

---

> **Read this before anything else:** this README documents the project's real, current, verified state — not an aspirational end state. Every "implemented," "mocked," and "not yet verified" label below reflects an actual code check, not a plan. See [Current Status & Known Gaps](#-current-status--known-gaps) before demoing this to anyone, and [Security Notes](#-security-notes) before handling this repository as a full export/zip rather than a clean `git clone`.

---

## 🩸 What is QATRA?

QATRA is an **emergency blood donation matching platform** built for **Alkhidmat Foundation Pakistan**, replacing the informal WhatsApp-broadcast model that currently governs urgent blood requests across Karachi with something structured, verified, and fast.

When a patient urgently needs blood, the default today is a WhatsApp message forwarded across group chats — a process that loses context, arrives too late, and gives seekers no way to verify a request is even real, and gives donors no way to confirm a request before showing up to a hospital. QATRA connects three groups that currently operate through disconnected, informal channels:

- **Emergency blood seekers** — patients' families and hospital staff, who need a verified donor fast.
- **Verified donors** — pre-screened, cooldown-tracked, and geo-matched to nearby emergencies.
- **Campus & community drive organizers** — running Alkhidmat's donor pipelines and pre-screened attendee pools.

**Version 1.0's core goal:** verified request in → matched, eligible donor out, in under 15 minutes. Scoped to Karachi trauma centers and Alkhidmat's existing campus chapters — not a nationwide rollout, not yet.

This repository documents its own history candidly, including a full mid-project technology migration (Kotlin/Jetpack Compose + Supabase RLS → Flutter + FastAPI) and several real security and authorization bugs that were found and fixed along the way. That history is preserved deliberately — see [Project History & Lessons Learned](#-project-history--lessons-learned) — because a fresh-looking codebase with no memory of its own past mistakes is a worse foundation than one that's honest about them.

---

## 🌐 Application Details

| Attribute | Details |
|---|---|
| **Platform** | Android 7.0+ (distributed via GitHub Releases, not Google Play — see [Distribution](#-distribution--no-google-play)) |
| **Client Framework** | Flutter (Dart) |
| **Backend Framework** | FastAPI (Python 3.12+) |
| **Database** | Supabase-hosted PostgreSQL + PostGIS |
| **File Storage** | Supabase Storage (S3-compatible, private buckets) |
| **Authentication** | Firebase Phone Auth, bridged to backend-issued JWT sessions |
| **Push Notifications** | Firebase Cloud Messaging (FCM) |
| **Backend Hosting** | Docker container on AWS EC2 (free tier) |
| **CI/CD** | GitHub Actions — tag-triggered, tested, signed, auto-released |

---

## ✨ Feature List

### 🗺️ Live Map & Proximity Matching
- Every verified request is geo-plotted at its hospital's coordinates (never the seeker's personal location).
- Real **PostGIS `ST_DWithin` / `ST_Distance`** query, not a naive full-table scan, returning donors ranked by distance.
- Full eligibility filtering in one pass: blood-group compatibility, active-cooldown exclusion, CNIC-format-check passed, availability toggled on, and all five pre-screening checks passed.
- Donor location is written only while "Available to Donate" is on, on a throttled interval — not continuous background tracking — and stops immediately when toggled off.

### 🔐 Authentication, Verification & Eligibility
- **Firebase Phone Auth** handles SMS delivery and OTP verification; a dedicated backend endpoint (`/auth/verify-firebase-phone`) bridges the verified Firebase identity into a backend-issued session.
- **CNIC format + district-code validation** — deliberately scoped and honestly labeled. Pakistani CNICs have no published checksum digit (the 13th digit indicates gender only), so this validator does **not** claim cryptographic identity verification, and the UI says "CNIC Format Check," not "Identity Verified." Real identity confirmation would require NADRA Verisys integration, which is explicitly out of scope for this build.
- Real camera/gallery capture for hospital admission slips and CNIC front/back images, with **on-device OCR** extracting hospital name, MRN, and unit count.
- Low-confidence or incomplete OCR extractions route to a manual admin verification queue instead of silently auto-approving.
- **90-day donor cooldown engine**, tracked server-side and enforced in every matching query — a donor mid-cooldown is never returned as eligible.
- **4-step interactive pre-screening checklist** (age, weight, recent illness/medication, travel/tattoo history) gating donor eligibility before they can be matched.

### 📡 Social Feed & Request Lifecycle
- Structured, filterable feed replacing WhatsApp forwards — filter by blood group, urgency, and location.
- One-tap "I Can Donate" response path.
- A dedicated, tested authorization rule ensures the public/donor feed (`GET /requests/feed`) only ever returns requests with status `BROADCASTING` — never requests still pending verification, rejected, or fraud-flagged. This specific rule was the subject of a real bug found and fixed during development (see [Project History](#-project-history--lessons-learned)) and now has an automated regression test guarding it.
- Seekers see their own full request history (`GET /requests/me`) regardless of status.

### 📞 Contact Exchange — Direct Calling (documented scope change)
- Phone numbers are **never** exposed on the public feed or map.
- A donor's real number becomes visible to a seeker (and vice versa) **only** once an active, non-closed match exists between them — enforced by a relationship-scoped authorization check, not a blanket "matched users can see each other" rule.
- **This is a deliberate, documented departure from the original PRD**, which specified masked/proxy phone calling (via a paid third-party provider). That was replaced with direct `Intent`-based dialing for cost reasons. This means the original PRD's "zero direct phone exposure" KPI is no longer met by design — flagged here plainly, and pending explicit sign-off from the Alkhidmat supervisor.

### 🎓 Awareness & Eligibility Module
- Educational content library: blood donation basics, myth-vs-fact content, health/donation awareness, social & cultural awareness material.
- Interactive eligibility self-checker, explicitly disclaimed as a preliminary check, not a medical determination.
- Campus/community blood drive scheduling, registration, and QR-code attendee check-in.
- Post-donation health-screening feedback storage.

### 🛡️ Admin / Verification Desk
- Real, working **TOTP-based two-factor authentication** (`pyotp`) gating admin access — not a decorative pass-through. This specific gap (an unwired mock admin login) existed for a long stretch of this project's history and was one of the last things fixed; it now has a dedicated regression test proving a bad credential or bad TOTP code is rejected.
- Hospital-slip review queue with side-by-side OCR-extracted metadata and confidence scores.
- Fraud audit table for duplicate MRN detection and CNIC blacklisting.
- Campus drive management dashboard.

### 🔔 Push Notifications
- Firebase Cloud Messaging, triggered by a backend service (`geo_alert_service.py`) that runs the same geo-matching logic used elsewhere, on every newly verified request.
- Device token registration and rotation handled server-side.

---

## 🏗️ Architecture

```text
┌──────────────────────────────────────────────────────────────────────────┐
│                              MOBILE CLIENT                               │
│                                                                          │
│   Flutter / Dart, distributed as signed split-per-ABI APKs via           │
│   GitHub Releases (no Google Play)                                       │
│                                                                          │
│   ├─ splash_onboarding_screen.dart      Dual-choice entry: Seeker/Donor  │
│   ├─ phone_verification_screen.dart     Firebase Phone Auth OTP flow     │
│   ├─ role_selection_profile_screen.dart Profile setup                    │
│   ├─ request_creation_screen.dart       Blood group, urgency, hospital   │
│   ├─ live_request_feed_status_screen.dart  Seeker's own request status   │
│   ├─ matched_donors_screen.dart         Proximity-ranked donor list      │
│   ├─ cnic_upload_screen.dart            Camera capture + OCR pipeline    │
│   ├─ donor_dashboard_screen.dart        Availability toggle, alerts      │
│   ├─ donor/pre_screening_checklist_screen.dart  4-step eligibility quiz  │
│   ├─ donor/donation_confirmation_screen.dart    Cooldown activation      │
│   ├─ geo_alert_modal.dart               Real-time FCM-driven alert       │
│   ├─ awareness/awareness_hub_screen.dart Educational content library     │
│   └─ admin/ (login, verification_queue, fraud_audit, drive_management)   │
└──────────────────────────────────┬───────────────────────────────────────┘
                                   │ HTTPS (JWT bearer auth)
┌──────────────────────────────────▼───────────────────────────────────────┐
│                         FASTAPI BACKEND (Docker / EC2)                   │
│                                                                          │
│   ├─ api/authorization.py     Centralized role & ownership policy layer  │
│   ├─ api/endpoints/auth.py    Firebase-bridge login + admin TOTP login   │
│   ├─ api/endpoints/requests.py    Request lifecycle, slip upload/OCR     │
│   ├─ api/endpoints/matching.py    PostGIS geo-matching RPC-equivalent    │
│   ├─ api/endpoints/contact.py     Match-gated phone number exchange      │
│   ├─ api/endpoints/donor.py       Availability toggle, location writes   │
│   ├─ api/endpoints/drives.py      Campus drive scheduling & check-in     │
│   ├─ api/endpoints/prescreening.py  4-step eligibility answers           │
│   ├─ api/endpoints/awareness.py   Educational content CRUD               │
│   ├─ api/endpoints/feedback.py    Post-donation health feedback          │
│   ├─ api/endpoints/system.py      Live version-check (GitHub Releases)   │
│   ├─ services/geo_alert_service.py  FCM dispatch on new verified reqs    │
│   ├─ services/fcm_service.py        Firebase Cloud Messaging client      │
│   ├─ core/security.py             JWT issuance/validation (env-only      │
│   │                                secret, no insecure fallback)         │
│   ├─ core/storage.py              S3-compatible client (Supabase         │
│   │                                Storage), configurable endpoint       │
│   └─ db/models.py, alembic/       SQLAlchemy models + real migrations    │
└──────────┬──────────────────────────────────┬────────────────────────────┘
           │                                  │
           ▼                                  ▼
┌────────────────────────────┐   ┌─────────────────────────────────────┐
│   SUPABASE                 │   │   FIREBASE                          │
│   ├─ Postgres + PostGIS    │   │   ├─ Phone Auth (SMS OTP delivery)  │
│   ├─ hospital-slips bucket │   │   ├─ Cloud Messaging (push)         │
│   │   (private)            │   │   └─ Admin SDK (server-side)        │
│   └─ cnic-documents bucket │   └─────────────────────────────────────┘
│       (private)            │
└────────────────────────────┘
```

> The original architecture proposal (Section 8 of the PRD) specified a different stack — Kotlin/Jetpack Compose with a Supabase-Postgres-RLS-enforced backend and native Supabase phone OTP. That version was built first, then fully migrated to the Flutter/FastAPI architecture shown above partway through the project, for reasons documented in [Project History](#-project-history--lessons-learned). The PRD's functional requirements and KPIs still apply; only the implementation stack changed.

---

## 🛠️ Tech Stack

### Client

| Technology | Role |
|---|---|
| **Flutter (Dart)** | Cross-platform-capable UI framework; Android is the current target |
| **Material 3** | Visual design system, red/white QATRA theme |
| **`image_picker`** | Real camera/gallery capture for slip and CNIC images |
| **Google ML Kit (on-device)** | Text recognition for hospital slip OCR |
| **`firebase_messaging`** | FCM push notification handling, including cold-start alert delivery |
| **`url_launcher`** | `tel:` scheme for direct calling between matched parties |
| **`package_info_plus`** | Reads the installed app's real version for update-check comparison |

### Backend

| Technology | Role |
|---|---|
| **FastAPI** | Async Python web framework |
| **SQLAlchemy + Alembic** | ORM and versioned database migrations |
| **GeoAlchemy2** | PostGIS-aware spatial queries (`ST_DWithin`, `ST_Distance`) from Python |
| **`python-jose` / PyJWT** | JWT session issuance and validation |
| **`pyotp`** | Real TOTP two-factor authentication for admin accounts |
| **`firebase-admin`** | Server-side Firebase Phone Auth token verification and FCM dispatch |
| **`boto3`** | S3-compatible client, configured against Supabase Storage's endpoint |
| **`bcrypt` / `cryptography`** | Password hashing and cryptographic primitives |

### Infrastructure

| Technology | Role |
|---|---|
| **Docker** (multi-stage build) | Backend containerization — build stage compiles wheels, runtime stage stays minimal |
| **Docker Compose** | Local development environment, including a Postgres+PostGIS service |
| **AWS EC2 (free tier)** | Backend hosting, behind a reverse proxy with TLS |
| **Supabase** | Managed Postgres + PostGIS, and S3-compatible private object storage |
| **Firebase** | Phone Auth, Cloud Messaging, Admin SDK |
| **GitHub Actions** | Tag-triggered CI/CD: test gate → signed split-per-ABI APK build → auto-published GitHub Release with auto-generated notes |

---

## ⚙️ How It Works

### 1. Geo-Matching Engine
When a request is verified and moves to `BROADCASTING` status, the backend's matching endpoint runs a single PostGIS query filtering eligible donors by: blood-group compatibility (a full compatibility matrix, not just exact-type matching), active-cooldown exclusion, CNIC-format-check passed, `availability` toggled on, and all five pre-screening checks passed — sorted by `ST_Distance` ascending. This replaces the "who actually saw the WhatsApp message" guesswork with a deterministic, ranked, eligibility-filtered dispatch list.

### 2. Verification Pipeline
A seeker captures a hospital admission/requisition slip through the app's camera. On-device OCR extracts hospital name, MRN, and unit count. If extraction confidence is high and fields are complete, the request can proceed toward broadcast; if confidence is low or fields are missing, it's routed into the admin verification queue for manual review — the same slip image is uploaded to a private storage bucket either way, so a human reviewer always has the source document to check against.

### 3. Authorization Model
Rather than relying on database-level row-security policies (the original Supabase-RLS approach), the current backend centralizes every access-control decision in `api/authorization.py` — a single module of `apply_*_policy` (query-scoping) and `verify_*` (write-authorization) functions that every endpoint calls into. This was a deliberate design choice made during the migration specifically so that access-control logic lives in one reviewable, testable place rather than being re-derived ad hoc in every route.

### 4. Contact Exchange
A phone number is only ever returned by `GET /requests/{request_id}/contact/{target_user_id}` if a real, active `MatchedDonorRequest` row exists linking the requesting user to the target — checked fresh on every call, not cached, not logged, and automatically re-denied once the request is closed or cancelled.

### 5. Cooldown & Pre-Screening
On confirmed donation completion, a donor's cooldown timer starts server-side and is read directly by the matching query — there's no separate "sync" step where cooldown status could drift out of date with what the matching engine actually sees.

---

## 📁 Project Structure

```text
QATRA/
├── backend/
│   ├── app/
│   │   ├── api/
│   │   │   ├── authorization.py        Centralized access-control policy layer
│   │   │   ├── deps.py                 Shared FastAPI dependencies (current_user, db session)
│   │   │   └── endpoints/
│   │   │       ├── auth.py             Firebase-bridge login, admin TOTP login
│   │   │       ├── requests.py         Request lifecycle, slip upload/OCR
│   │   │       ├── matching.py         Geo-matching / eligible-donors lookup
│   │   │       ├── contact.py          Match-gated phone number exchange
│   │   │       ├── donor.py            Availability toggle, location updates
│   │   │       ├── drives.py           Campus drive scheduling & check-in
│   │   │       ├── prescreening.py     Pre-screening checklist answers
│   │   │       ├── awareness.py        Educational content
│   │   │       ├── feedback.py         Post-donation health feedback
│   │   │       └── system.py           Live version-check endpoint
│   │   ├── core/
│   │   │   ├── security.py             JWT handling (env-sourced secret, no fallback)
│   │   │   └── storage.py              S3-compatible storage client
│   │   ├── db/
│   │   │   ├── database.py             SQLAlchemy engine/session setup
│   │   │   └── models.py               ORM models mirroring database/qatra_postgres_schema.sql
│   │   ├── schemas/                    Pydantic request/response schemas, one file per domain
│   │   ├── services/
│   │   │   ├── geo_alert_service.py    FCM dispatch on new verified requests
│   │   │   └── fcm_service.py          Firebase Cloud Messaging client
│   │   └── main.py                     App entry point, router registration
│   ├── alembic/versions/               Versioned database migrations
│   ├── tests/                          pytest suite: authorization, auth, matching
│   ├── Dockerfile                      Multi-stage build (wheels → minimal runtime)
│   ├── docker-compose.yml              Local dev: backend + Postgres/PostGIS
│   ├── requirements.txt                Runtime dependencies (pinned)
│   └── requirements-dev.txt            + test/dev-only dependencies
│
├── flutter_client/
│   ├── lib/
│   │   ├── core/
│   │   │   ├── api_client.dart         Backend HTTP client
│   │   │   └── theme.dart              Material 3 red/white QATRA theme
│   │   ├── screens/                    One file per wireframe screen (seeker, donor, admin, awareness)
│   │   ├── services/fcm_service.dart   Push notification handling
│   │   └── utils/cnic_validator.dart   Format + district-range CNIC check
│   └── android/                        Signing config reads env vars set by CI
│
├── database/                           Historical: original Postgres schema + RLS policies
│                                        from the pre-migration Supabase-RLS architecture
├── docs/
│   ├── PRD.pdf                         Full Project Requirement Document
│   ├── Wireframes.pdf                  22-screen wireframe set (Seeker/Donor/Admin)
│   ├── privacy-policy.md               User-facing privacy policy
│   └── vault/                          Obsidian knowledge base: decisions, gaps, architecture notes
│
├── media/                              Logo, architecture diagram
└── .github/workflows/release.yml       Tag-triggered CI/CD pipeline
```

---

## 🚀 Local Setup

### Prerequisites
- Python 3.12+, Flutter SDK (recent stable), Docker + Docker Compose
- A Supabase project with the PostGIS extension enabled
- A Firebase project with Phone Auth and Cloud Messaging enabled

### Step 1 — Clone the repository
```bash
git clone https://github.com/abdulhayykhan/QATRA.git
cd QATRA
```

### Step 2 — Configure environment
```bash
cp .env.example backend/.env
# fill in real values from your own Supabase and Firebase projects
```
Never commit `.env`, the Firebase Admin SDK service-account JSON, or any keystore file — see [Security Notes](#-security-notes).

### Step 3 — Backend: database and local run
```bash
cd backend
docker compose up -d --build     # starts backend + local Postgres/PostGIS
alembic upgrade head             # apply migrations
python -m pytest tests/          # confirm the test suite passes locally
```

### Step 4 — Flutter client
```bash
cd flutter_client
flutter pub get
flutter run                      # point core/api_client.dart at your local backend URL
```

---

## 📡 Build & Deployment

### Signed release APK (split-per-ABI)
```bash
cd flutter_client
flutter build apk --split-per-abi --release
```
Produces separate `arm64-v8a`, `armeabi-v7a`, and `x86_64` APKs — the `arm64-v8a` build is the correct download for the overwhelming majority of modern Android devices, and is meaningfully smaller than a universal APK.

### Backend container
```bash
cd backend
docker build -t qatra-backend .
docker run -d --env-file .env -p 8000:8000 qatra-backend
```

### CI/CD — tag-triggered release
Pushing a version tag (`vX.Y.Z`) to GitHub Actions runs the full pipeline:
1. **Test gate** — `flutter analyze` + `flutter test`, and `pytest` against the backend. The build/release job explicitly depends on this (`needs: test`) and will not run if it fails.
2. **Signed build** — split-per-ABI release APKs, signed using a keystore decoded from GitHub Secrets (never committed to the repo).
3. **Auto-published GitHub Release** — all three APK variants attached, with auto-generated release notes.

---

## 📲 Distribution — No Google Play

QATRA is currently distributed via **direct APK download through GitHub Releases**, not the Play Store. On first install, Android's Play Protect will likely show an "Unrecognized app" warning — this is expected for any app distributed outside Play, not a sign of a problem with this specific build. The app includes an in-app version-check (`GET /system/version`, backed live by the GitHub Releases API) that prompts users when a newer release is available.

---

## 🩺 Current Status & Known Gaps

This section is kept current deliberately, and updated as gaps are actually closed — not left as a static disclaimer.

**Resolved, verified in code:**
- The hardcoded JWT secret fallback that once existed in `core/security.py` is gone — the app now fails to start if `SECRET_KEY` isn't set.
- Admin login uses real TOTP verification with a passing regression test proving bad credentials are rejected.
- `requirements.txt` reflects real, pinned runtime dependencies (previously reduced to a single incomplete line during the migration).
- The public/donor request feed correctly excludes non-`BROADCASTING` requests, with an automated test guarding this specific rule after it was once found broken.

**Still open:**
- CORS middleware is imported in `main.py` but never actually configured — low risk for the mobile-only client today, but will silently block any future web build.
- Full live, on-device end-to-end verification of the complete seeker → donor → admin loop against the real deployed AWS/Supabase/Firebase stack is still pending as of this writing.
- Firebase ID token replay protection is not yet implemented in the auth bridge — acceptable for a small closed pilot, worth hardening before wider rollout.
- `docs/vault/`'s known-gap notes are not fully synchronized with the current codebase state (see the note at the top of this README) — treat this README as the more current source until the vault is updated to match.

**Explicit scope changes from the original PRD, pending formal sign-off:**
- Masked/proxy calling (PRD §4.4) was replaced with direct dialing for cost reasons, which means the "zero direct phone exposure" KPI (PRD §7.4) is no longer met by design.

---

## 📜 Project History & Lessons Learned

This project shipped an initial Kotlin/Jetpack Compose + Supabase implementation first, then underwent a full, deliberate migration to Flutter + FastAPI. That history includes several real bugs that were found and fixed rather than shipped silently:

- An early CNIC validator implemented a **fabricated checksum algorithm** — Pakistani CNICs have no published checksum digit. It was found, removed, and replaced with an honestly-scoped format/district check.
- An early Postgres RLS policy on the public request feed had **no status filter**, briefly meaning any caller — including anonymous ones — could see requests still pending verification or already rejected. Found, fixed, and the fix is now protected by a specific regression test in the current FastAPI authorization layer.
- Admin login went unauthenticated (a decorative pass-through with no real check) for a long stretch of this project's timeline before finally being wired to real TOTP verification.
- Credentials were exposed via full-workspace zip exports on more than one occasion during development — a lesson that shaped this repo's current, careful `.gitignore` and its explicit distinction between what a `git clone` exposes versus what a full folder export can expose.

These are documented here on purpose. A project's history of caught mistakes is more informative — and more trustworthy — than a codebase that presents itself as having been correct from the start.

---

## 🔒 Security Notes

`.env`, `local.properties`, `*.jks` keystores, and any service-account JSON files are excluded via `.gitignore` and must **never** be committed or included in any workspace export/zip shared outside the core dev team — `.gitignore` only protects against `git add`, not against zipping the whole project folder. If you're ever handed this project as a full folder rather than a fresh `git clone`, check for these files before doing anything else, and treat any credential found inside as compromised.

---

## 📚 Documentation

- 📄 [**Project Requirement Document**](docs/PRD.pdf) — goals, personas, functional requirements, KPIs, original system architecture
- 🎨 [**Wireframes**](docs/Wireframes.pdf) — full UI wireframe set across Seeker (10 screens), Donor (8 screens), and Admin/Verification Desk (4 screens)
- 🔐 [**Privacy Policy**](docs/privacy-policy.md)
- 🧠 [**Obsidian Vault**](docs/vault/) — decisions log, architecture notes, known-gaps tracking (currently lagging the codebase in a few places — see [Known Gaps](#-current-status--known-gaps))

---

## 👥 Team

Built by a 6-member group for the Alkhidmat Foundation Pakistan IT internship:

| Role | Member |
|---|---|
| Live Map & Proximity Matching | Hareem Israr |
| Auth, Verification & Eligibility | Saghir Ahmed |
| Social Feed | Mahrukh Baig |
| Awareness & Eligibility Module | Yumna Abbasi |
| Non-Functional Requirements & KPIs | Nimra Iftikhar |
| Team Lead / Architecture / Migration | Abdul Hayy Khan |

## 📄 License

This project is open-source and available for educational and commercial use under the MIT License.

---

**Made with ❤️ by [Abdul Hayy Khan](https://www.linkedin.com/in/abdulhayykhan/)**