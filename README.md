# CivicAlert

CivicAlert is a full-stack university demo platform for checking and reporting election-process rumors.

- Frontend: Angular (`frontend-angular`)
- Backend: Spring Boot (`backend-spring`)
- Database: PostgreSQL (`spring-postgres` existing container)
- Algorithm engine: C (`c-engine`)

## 1) What CivicAlert Does

Public users can:
- check a suspicious election-process claim,
- report suspicious claims,
- track report status,
- read published verified corrections,
- read official election-process information.

Validators can:
- review submitted reports,
- mark under review,
- issue decisions (`VERIFIED_TRUE`, `VERIFIED_FALSE`, `MISLEADING`, `NEEDS_CONTEXT`, `REJECTED`),
- publish corrections to public verified claims.

## 2) Why It Is Not a Voting System

CivicAlert is not a voting system.

- It does not register voters.
- It does not collect or count votes.
- It does not support candidates.
- It does not determine election results.
- It does not auto-decide truth.

It is a rumor-checking and correction-publication platform for election-process information.

## 3) Public User Workflow

1. Open `/` and submit a claim.
2. Backend checks published verified claims.
3. If needed, backend checks DB rumor patterns and C detection engine output.
4. User receives status and risk result.
5. User can report suspicious claims on `/report`.
6. User can track report status on `/report-status`.
7. User can browse `/verified-claims` and `/official-info`.

## 4) Validator Workflow

1. Open `/validator`.
2. Review prioritized reports by risk.
3. Open `/validator/reports/:id`.
4. Mark as `UNDER_REVIEW`.
5. Submit decision and optional correction/source.
6. If `publish = true`, backend creates/updates a published `verified_claims` record.

Important demo safeguard:
- SMS-voting claims cannot be published as `VERIFIED_TRUE`.

## 5) PostgreSQL Seed Data (from TXT file)

Seed source file:
- `civicalert_seed_data_requirements.txt` (project root)

Sections:
- `OFFICIAL_INFO`
- `RUMOR_PATTERNS`
- `VERIFIED_CLAIMS`

Seeder behavior:
- Loads and parses the TXT file on backend startup.
- Inserts without duplication:
  - `official_info`: skip if `(topic, language)` exists.
  - `rumor_patterns`: skip if `(normalized_phrase, category, language)` exists.
  - `verified_claims`: skip if `(normalized_claim, language)` exists.
- Uses `TextNormalizer.normalize()` for normalized fields.
- Runs SMS cleanup to force SMS-voting verified claims to safe values (`VERIFIED_FALSE`, `fake_voting_method`, `published=true`).

## 6) How to Add More Seed Data

1. Edit `civicalert_seed_data_requirements.txt`.
2. Keep format exactly:
   - `topic|content|source_name|source_url|language`
   - `phrase|category|severity|language`
   - `claim_text|category|status|correction_text|official_source|official_source_url|language|region|published`
3. Restart backend. Seeder inserts only missing rows.

## 7) C Algorithms Used

The C engine includes:
- Text normalization
- Trie
- Aho-Corasick multi-pattern matching
- Levenshtein edit distance (fuzzy similarity)
- Hash table (category/frequency counting)
- Max heap priority queue
- Risk scoring

Risk scoring formula:
- `score = severity * 15`
- `+10` exact pattern match
- `+8` fuzzy/edit-distance match
- `+10` category `voter_suppression` or `fake_voting_method`
- `+5` category `voting_date` or `polling_location`

Risk levels:
- `>= 80`: `CRITICAL`
- `>= 55`: `HIGH`
- `>= 30`: `MEDIUM`
- otherwise `LOW`

## 8) How Spring Boot Calls the C Engine

`CEngineService`:
- Compiles C engine with `make` if binary is missing.
- Exports active `rumor_patterns` from PostgreSQL to a temporary pattern file.
- Calls engine with:
  - `--claim`
  - `--patterns <temp_file>`
  - `--language <lang>`
- Parses JSON stdout.
- Uses timeout and stderr/stdout handling.
- If engine fails or JSON is invalid, backend returns safe fallback (no hang).

## 9) Database Tables

- `official_info`
- `rumor_patterns`
- `verified_claims`
- `public_reports`
- `detection_logs`

## 10) Run PostgreSQL Using Existing `spring-postgres`

Use existing container (do not create a new DB container):

```bash
docker ps --filter name=spring-postgres
```

If DB/user are missing:

```bash
docker exec -it spring-postgres psql -U postgres
CREATE DATABASE civicalert;
CREATE USER civicalert_user WITH PASSWORD 'civicalert_pass';
GRANT ALL PRIVILEGES ON DATABASE civicalert TO civicalert_user;
```

Note:
- Some local setups map host port `55432` -> container `5432`.
- If your host exposes `5432`, set `DB_PORT=5432`.

## 11) Run Backend

```bash
cd backend-spring
mvn spring-boot:run
```

Backend API base:
- `http://localhost:8080/api`

## 12) Run Frontend

```bash
cd frontend-angular
npm install
ng serve
```

Frontend URL:
- `http://localhost:4200`

## 13) One-command Docker Run

```bash
docker compose up --build
```

URLs:
- Frontend: `http://localhost:4200`
- Backend: `http://localhost:8080/api`

## 14) Fix/Restart if Checking Stays Loading

1. Confirm backend is running on port `8080`.
2. Confirm frontend API base is `http://localhost:8080/api`.
3. Restart backend and frontend.
4. Check browser network tab for `POST /api/public/check-claim`.

Current frontend behavior:
- Stops loading on success, timeout, and error.
- Shows backend-down message if API is unreachable.
- Shows generic retry message on server errors.

## 15) Demo Claims

Romanian:
- `pot sa votez prin sms?` -> `VERIFIED_FALSE` or `NEEDS_REVIEW`, category `fake_voting_method`
- `pot să votez prin sms?` -> `VERIFIED_FALSE` or `NEEDS_REVIEW`, category `fake_voting_method`
- `poți vota prin sms` -> `VERIFIED_FALSE`, category `fake_voting_method`
- `vot prin sms` -> `VERIFIED_FALSE` or `NEEDS_REVIEW`, category `fake_voting_method`
- `alegerile au fost anulate` -> `VERIFIED_FALSE` or `NEEDS_REVIEW`
- `secția de votare s-a mutat` -> `NEEDS_REVIEW`
- `secțiile sunt deschise între 07:00 și 21:00` -> `VERIFIED_TRUE`

English:
- `can I vote by SMS?` -> `VERIFIED_FALSE` or `NEEDS_REVIEW`
- `you can vote by SMS` -> `VERIFIED_FALSE`
- `the election was cancelled` -> `VERIFIED_FALSE`
- `voting is online` -> `VERIFIED_FALSE`
- `polling stations are open from 07:00 to 21:00` -> `VERIFIED_TRUE`

Russian:
- `можно голосовать по смс` -> `VERIFIED_FALSE` or `NEEDS_REVIEW`
- `выборы отменены` -> `VERIFIED_FALSE` or `NEEDS_REVIEW`

Neutral:
- `Candidate X is good` -> `NO_MATCH_FOUND` (neutral, not true)

## 16) API Summary

Public:
- `POST /api/public/check-claim`
- `POST /api/public/reports`
- `GET /api/public/reports/{trackingCode}`
- `GET /api/public/verified-claims`
- `GET /api/public/official-info`

Validator:
- `GET /api/validator/reports`
- `GET /api/validator/reports/{id}`
- `PATCH /api/validator/reports/{id}/under-review`
- `POST /api/validator/reports/{id}/decision`

## 17) Security Note

This demo intentionally uses no real authentication yet.
Production should add authentication, authorization, audit logs, and stricter moderation controls.
