# StudyZen Backend

This package is the backend part of StudyZen for local self-hosting.

It is intended for people who want to run their own personal StudyZen stack on their machine without building the backend from source.

## What Is Included

The backend package is expected to contain:

- `docker-compose.yml`
- `.env.example`
- this README

The backend itself is pulled as a prebuilt container image from:

```text
ghcr.io/frandm16/studytracker-backend:v2.0.1
```

## What It Starts

Running the compose file starts:

- PostgreSQL
- the StudyZen backend API

The default backend URL is:

```text
http://localhost:8080/api
```

## Quick Start

### 1. Install Docker Desktop

Make sure Docker Desktop is installed and running.

### 2. Create your environment file

Copy:

```bash
cp .env.example .env
```

Example default values:

```env
BACKEND_PUBLIC_PORT=8080
BACKEND_PRIVATE_PORT=8080
DB_PORT=5432
DB_NAME=studytracker
DB_USER=your_user
DB_PASSWORD=your_password
API_URL=http://localhost:8080/api
```

At minimum, you should set:

- `DB_USER`
- `DB_PASSWORD`
- `DB_NAME`

### 3. Start the services

From the folder containing the package:

```bash
docker compose up -d
```

### 4. Connect the frontend

Open the StudyZen desktop app and use:

```text
http://localhost:8080/api
```

If you changed `BACKEND_PUBLIC_PORT`, use that port instead.

## Stop The Backend

```bash
docker compose down
```

To stop and remove volumes as well:

```bash
docker compose down -v
```

Use the second command only if you want to remove the local database data.

## Files

```text
studyzen-backend/
├── docker-compose.yml
├── .env.example
└── README-backend.md
```

## Recommended Release Name

For the backend package, a clearer release asset name would be:

```text
StudyZen-backend-v2.0.1.zip
```

## Notes

- This package does not include a `Dockerfile`
- It is designed to use the published backend image directly
- The standard setup is local-first and self-hosted
