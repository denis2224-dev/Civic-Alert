# CivicAlert

CivicAlert is a full-stack university project for election-process rumor checking and reporting.

It helps citizens:
- check suspicious election-related claims,
- report possible rumors,
- track report status.

It helps validators:
- review reports by risk,
- mark reports under review,
- publish verified corrections for the public.

## 1. Project Description

The platform has:
- Angular frontend (`frontend-angular`)
- Spring Boot backend (`backend-spring`)
- PostgreSQL database (existing Docker container `spring-postgres`)
- C algorithm engine (`c-engine`) for rumor-pattern detection

## 2. Why This Is Not a Voting System

CivicAlert is **not** a voting system:
- it does not count votes,
- it does not register voters,
- it does not support candidates,
- it does not decide election winners,
- it does not automatically decide truth.

It only flags suspicious election-process claims and supports human validation.

## 3. Public User Workflow

1. User checks a claim on `/`.
2. Backend checks published verified claims.
3. If no verified match, backend calls C engine.
4. User can submit a suspicious claim report on `/report`.
5. User tracks report by code on `/report-status`.
6. User can view `/verified-claims` and `/official-info`.

## 4. Validator Workflow

1. Validator opens `/validator`.
2. Reviews prioritized reports by risk level and score.
3. Opens report detail `/validator/reports/:id`.
4. Marks report `UNDER_REVIEW`.
5. Applies decision:
   - `VERIFIED_TRUE`
   - `VERIFIED_FALSE`
   - `MISLEADING`
   - `NEEDS_CONTEXT`
   - `REJECTED`
6. Adds correction and official source.
7. Publishes correction (creates/updates `verified_claims` with `published=true`).

Note: real authentication is intentionally not implemented in this academic version.  
Production version should add role-based authentication and audit controls.

## 5. Data Structures and Algorithms

The C engine uses:
- **Trie**: stores rumor phrases and matches phrases inside normalized claims.
- **Text Normalization**: lowercase, punctuation cleanup, space normalization.
- **Risk Scoring**:
  - base `severity * 15`
  - `+10` if matched phrase exists
  - `+10` for categories `voter_suppression` or `fake_voting_method`
- **Priority Queue (Max Heap)**:
  - used to rank multiple phrase matches and keep highest-risk match.

Risk levels:
- `score >= 80` -> `CRITICAL`
- `score >= 55` -> `HIGH`
- `score >= 30` -> `MEDIUM`
- otherwise `LOW`

## 6. Database Tables

- `official_info`
- `rumor_patterns`
- `verified_claims`
- `public_reports`
- `detection_logs`

Seeded on startup (without duplication checks):
- official voting facts,
- known rumor patterns,
- initial verified claims.

## 7. PostgreSQL (Existing `spring-postgres` Container)

Use the existing container only:

```bash
docker ps --filter name=spring-postgres
```

If DB/user are missing:

```bash
docker exec -it spring-postgres psql -U postgres
CREATE DATABASE civicalert;
CREATE USER civicalert_user WITH PASSWORD 'civicalert_pass';
GRANT ALL PRIVILEGES ON DATABASE civicalert TO civicalert_user;
\c civicalert
GRANT ALL ON SCHEMA public TO civicalert_user;
ALTER SCHEMA public OWNER TO civicalert_user;
```

Important for this local environment:
- container exposes PostgreSQL as `localhost:55432` (mapped from container `5432`).
- backend uses env-configurable defaults:
  - `DB_HOST=localhost`
  - `DB_PORT=55432`
  - `DB_NAME=civicalert`
  - `DB_USER=civicalert_user`
  - `DB_PASSWORD=civicalert_pass`

If your container maps to `5432`, set `DB_PORT=5432`.

## 8. Run Backend

```bash
cd backend-spring
mvn spring-boot:run
```

Backend API base:
- `http://localhost:8080/api`

## 9. Run Frontend

```bash
cd frontend-angular
npm install
ng serve
```

Frontend URL:
- `http://localhost:4200`

## 10. How Spring Boot Calls the C Engine

- `CEngineService` invokes `../c-engine/civic_alert_engine` using `ProcessBuilder`.
- If binary is missing, backend runs `make` in `c-engine/`.
- Engine JSON output is parsed into backend DTOs.
- If engine compile/run fails, backend stays up and returns a manual-review oriented fallback.

Build engine manually (optional):

```bash
cd c-engine
make
./civic_alert_engine "The election was cancelled"
```

## 11. Example Claims to Test

- `"The election was cancelled."`
- `"You can vote by SMS."`
- `"The polling station moved."`
- `"Candidate X is good."` -> should return `NO_MATCH_FOUND` for election-process rumor detection.

## API Endpoints Summary

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

