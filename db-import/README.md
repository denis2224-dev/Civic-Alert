# CivicAlert Bulk CSV Import

This folder contains ready CSV files for fast PostgreSQL import:

- `official_info.csv`
- `rumor_patterns.csv`
- `verified_claims.csv`

Run import (default CivicAlert local DB settings):

```bash
./scripts/import_bulk_csv.sh
```

Optional custom connection:

```bash
DB_HOST=localhost DB_PORT=55432 DB_NAME=civicalert DB_USER=civicalert_user DB_PASSWORD=civicalert_pass ./scripts/import_bulk_csv.sh
```

The import script uses staging temp tables and inserts only missing records:

- `official_info`: dedupe by `(topic, language)`
- `rumor_patterns`: dedupe by `(normalized_phrase, category, language)`
- `verified_claims`: dedupe by `(normalized_claim, language)`
