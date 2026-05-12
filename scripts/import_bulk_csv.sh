#!/usr/bin/env bash
set -euo pipefail

ROOT_DIR="$(cd "$(dirname "${BASH_SOURCE[0]}")/.." && pwd)"
DATA_DIR="${1:-$ROOT_DIR/db-import}"

OFFICIAL_CSV="${OFFICIAL_CSV:-$DATA_DIR/official_info.csv}"
RUMOR_CSV="${RUMOR_CSV:-$DATA_DIR/rumor_patterns.csv}"
VERIFIED_CSV="${VERIFIED_CSV:-$DATA_DIR/verified_claims.csv}"

DB_HOST="${DB_HOST:-localhost}"
DB_PORT="${DB_PORT:-55432}"
DB_NAME="${DB_NAME:-civicalert}"
DB_USER="${DB_USER:-civicalert_user}"
DB_PASSWORD="${DB_PASSWORD:-civicalert_pass}"

for file in "$OFFICIAL_CSV" "$RUMOR_CSV" "$VERIFIED_CSV"; do
  if [[ ! -f "$file" ]]; then
    echo "Missing CSV file: $file" >&2
    exit 1
  fi
done

export PGPASSWORD="$DB_PASSWORD"

psql \
  -h "$DB_HOST" \
  -p "$DB_PORT" \
  -U "$DB_USER" \
  -d "$DB_NAME" \
  -v ON_ERROR_STOP=1 <<SQL
\echo Importing CSV data into CivicAlert...

BEGIN;

CREATE TEMP TABLE official_info_stage (
  topic text,
  content text,
  source_name text,
  source_url text,
  language text
);

\copy official_info_stage (topic, content, source_name, source_url, language) FROM '$OFFICIAL_CSV' WITH (FORMAT csv, HEADER true, ENCODING 'UTF8')

INSERT INTO official_info (topic, content, source_name, source_url, language, updated_at)
SELECT
  trim(s.topic),
  trim(s.content),
  trim(s.source_name),
  nullif(trim(s.source_url), ''),
  lower(trim(s.language)),
  now()
FROM official_info_stage s
WHERE s.topic IS NOT NULL
  AND s.language IS NOT NULL
  AND NOT EXISTS (
    SELECT 1
    FROM official_info oi
    WHERE oi.topic = trim(s.topic)
      AND oi.language = lower(trim(s.language))
  );

CREATE TEMP TABLE rumor_patterns_stage (
  phrase text,
  normalized_phrase text,
  category text,
  severity integer,
  language text,
  active text
);

\copy rumor_patterns_stage (phrase, normalized_phrase, category, severity, language, active) FROM '$RUMOR_CSV' WITH (FORMAT csv, HEADER true, ENCODING 'UTF8')

INSERT INTO rumor_patterns (phrase, normalized_phrase, category, severity, language, active, created_at)
SELECT
  trim(s.phrase),
  trim(s.normalized_phrase),
  trim(s.category),
  COALESCE(s.severity, 1),
  lower(trim(s.language)),
  CASE WHEN lower(trim(COALESCE(s.active, 'true'))) IN ('true', 't', '1', 'yes', 'y') THEN true ELSE false END,
  now()
FROM rumor_patterns_stage s
WHERE s.phrase IS NOT NULL
  AND s.category IS NOT NULL
  AND s.language IS NOT NULL
  AND NOT EXISTS (
    SELECT 1
    FROM rumor_patterns rp
    WHERE rp.normalized_phrase = trim(s.normalized_phrase)
      AND rp.category = trim(s.category)
      AND rp.language = lower(trim(s.language))
  );

CREATE TEMP TABLE verified_claims_stage (
  claim_text text,
  normalized_claim text,
  category text,
  status text,
  correction_text text,
  official_source text,
  official_source_url text,
  language text,
  region text,
  published text
);

\copy verified_claims_stage (claim_text, normalized_claim, category, status, correction_text, official_source, official_source_url, language, region, published) FROM '$VERIFIED_CSV' WITH (FORMAT csv, HEADER true, ENCODING 'UTF8')

INSERT INTO verified_claims (
  claim_text,
  normalized_claim,
  category,
  status,
  correction_text,
  official_source,
  official_source_url,
  language,
  region,
  published,
  created_at,
  updated_at
)
SELECT
  trim(s.claim_text),
  trim(s.normalized_claim),
  trim(s.category),
  upper(trim(s.status)),
  nullif(trim(s.correction_text), ''),
  nullif(trim(s.official_source), ''),
  nullif(trim(s.official_source_url), ''),
  lower(trim(s.language)),
  COALESCE(nullif(trim(s.region), ''), 'national'),
  CASE WHEN lower(trim(COALESCE(s.published, 'false'))) IN ('true', 't', '1', 'yes', 'y') THEN true ELSE false END,
  now(),
  now()
FROM verified_claims_stage s
WHERE s.claim_text IS NOT NULL
  AND s.normalized_claim IS NOT NULL
  AND s.category IS NOT NULL
  AND s.status IS NOT NULL
  AND s.language IS NOT NULL
  AND NOT EXISTS (
    SELECT 1
    FROM verified_claims vc
    WHERE vc.normalized_claim = trim(s.normalized_claim)
      AND vc.language = lower(trim(s.language))
  );

COMMIT;

\echo Import completed.
SQL

echo "Done."
