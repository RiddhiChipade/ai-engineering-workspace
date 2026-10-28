# AI Engineering Workspace

Spring Boot 3.5 / Java 21 task management API using PostgreSQL, Redis, Kafka, and stateless JWT authentication. PostgreSQL is the source of truth; Redis caches filtered task list responses; Kafka receives best-effort task change events.

## Start the full stack

Prerequisites: Docker Desktop with Compose v2. From the repository root:

```powershell
docker compose up -d --build
```

Compose starts PostgreSQL, Redis, and Kafka, waits for each service health check, then starts the app. The app uses the `docker` Spring profile. Existing named volumes are reused; startup does not reset the database. Keep the Compose project name and volume declarations stable to continue using the same PostgreSQL data.

| Service | Host address | Health / status |
| --- | --- | --- |
| API | http://localhost:8080 | `docker compose ps` |
| PostgreSQL | `localhost:5432` | Compose `pg_isready` check |
| Redis | `localhost:6379` | Compose `redis-cli ping` check |
| Kafka | `localhost:9092` | Compose broker TCP check |

Kafka advertises `kafka:9094` to containers and `localhost:9092` to host clients. Change the host-side ports with `POSTGRES_PORT`, `REDIS_PORT`, `APP_PORT`; the container ports stay fixed. PostgreSQL persists in `postgres_data`; Redis and Kafka use separate named volumes.

## Operate the stack

```powershell
docker compose ps
docker compose logs -f app
docker compose logs -f postgres redis kafka
docker compose logs --tail 200 app
docker compose stop
docker compose start
```

To stop and remove containers and the Compose network while retaining all named volumes and database data:

```powershell
docker compose down
```

**DESTRUCTIVE — deletes persisted database, Redis, and Kafka data:** `docker compose down -v` removes the named volumes. Do not run it when the existing PostgreSQL data must be preserved. Back up the database before any intentional volume removal.

## Configuration and local defaults

Compose reads a root `.env` file. Copy `.env.example` to `.env`, add your `OPENAI_API_KEY`, and change passwords/secrets before exposing the service beyond a local development machine. The checked-in defaults are only for local development. `JWT_SECRET` must be at least 32 bytes for HS256. Do not commit `.env` or production credentials. Without an OpenAI key, direct task tools work but `/assistant/chat` returns HTTP 503.

| Variable | Default | Purpose |
| --- | --- | --- |
| `POSTGRES_DB` | `ai_engineering_workspace` | Database name |
| `POSTGRES_USER` / `POSTGRES_PASSWORD` | `postgres` / `postgres` | Local database credentials |
| `POSTGRES_PORT` | `5432` | Host PostgreSQL port |
| `APP_PORT` | `8080` | Host API port |
| `REDIS_PORT` | `6379` | Host Redis port |
| `JWT_SECRET` | local placeholder | Signing key; replace for any shared deployment |
| `JWT_EXPIRATION` | `3600000` | Token lifetime in milliseconds |
| `KAFKA_TASK_TOPIC` | `task-events` | Task event topic |
| `KAFKA_CONSUMER_GROUP` | `ai-engineering-workspace` | Event consumer group |
| `KAFKA_ENABLED` | profile dependent | Enables the event consumer (`true` in Docker, `false` in `.env.example` local profile) |
| `OPENAI_API_KEY` | empty | Enables natural-language assistant chat; keep it in `.env` or a secret store |
| `OPENAI_MODEL` | `gpt-6-astra` | OpenAI Responses API model |
| `OPENAI_BASE_URL` | `https://api.openai.com/v1` | OpenAI API base URL |
| `ASSISTANT_TIME_ZONE` | `UTC` | Time zone used to determine whether a task due date is overdue |

The `dev` profile connects to localhost PostgreSQL, Redis, and Kafka. The `docker` profile uses Compose DNS names. The `prod` profile requires explicit `JWT_SECRET`, database, Redis, and Kafka environment variables instead of local credential defaults. Override Spring settings with `SPRING_PROFILES_ACTIVE`, `DB_HOST`, `DB_NAME`, `DB_USERNAME`, `DB_PASSWORD`, `REDIS_HOST`, `KAFKA_BOOTSTRAP_SERVERS`, `KAFKA_TASK_TOPIC`, and `SERVER_PORT` when running outside Compose. Inside Compose, database and Redis ports remain `5432` and `6379`; host port overrides do not change container-to-container ports.

## API

Swagger UI: http://localhost:8080/swagger-ui/index.html

OpenAPI document: http://localhost:8080/v3/api-docs
### Web frontend

Open http://localhost:8080/ for the Dayline task workspace. Register or sign in, then create, edit, complete, delete, search, filter, sort, and page through tasks. The UI uses the authenticated task API and backend ownership checks; admins also get an assignee picker. The JWT is kept in tab-scoped browser session storage and cleared when you sign out. Static assets are served by Spring Boot, so no separate frontend server or build step is needed. AI chat is intentionally not part of this UI yet.

Registration and login are public. Send the returned token as `Authorization: Bearer <token>` for protected routes.

```powershell
$registration = Invoke-RestMethod -Method Post -Uri http://localhost:8080/auth/register `
  -ContentType 'application/json' `
  -Body '{"username":"alice","email":"alice@example.com","password":"LocalPassword123"}'
$token = $registration.token
Invoke-RestMethod -Uri 'http://localhost:8080/tasks?status=TODO&page=0&size=20&sort=createdAt,desc' `
  -Headers @{ Authorization = "Bearer $token" }
Invoke-RestMethod -Method Post -Uri http://localhost:8080/assistant/chat `
  -Headers @{ Authorization = "Bearer $token" } -ContentType 'application/json' `
  -Body '{"message":"What should I work on today?"}'
```

### Routes

| Method | Path | Access |
| --- | --- | --- |
| `POST` | `/auth/register` | Public; creates a `USER` and returns a JWT |
| `POST` | `/auth/login` | Public; returns a JWT |
| `GET` | `/tasks` | Authenticated; users see their assigned tasks, admins can filter all tasks |
| `GET` | `/tasks/{id}` | Authenticated; task owner or admin |
| `POST` | `/tasks` | Authenticated; users create tasks assigned to themselves, admins may assign tasks |
| `PUT` | `/tasks/{id}` | Task owner or admin; accepts title, description, priority, optional status and assignee |
| `DELETE` | `/tasks/{id}` | Task owner or admin |
| `GET`, `POST` | `/users` | Admin role |
| `GET`, `PUT`, `DELETE` | `/users/{id}` | Admin role |
| `GET` | `/users/{id}/tasks` | Admin role |
| `GET` | `/ai/tools` | Authenticated tool manifest |
| `POST` | `/ai/tools/getPendingTasks?page=0&size=100` | Authenticated; returns only caller's TODO and IN_PROGRESS tasks |
| `POST` | `/ai/tools/getOverdueTasks?page=0&size=100` | Authenticated; returns only caller's incomplete tasks due before today |
| `POST` | `/assistant/chat` | Authenticated; sends a natural-language question through the OpenAI tool loop |

Task create/update requests may set `dueDate` as an ISO date such as `2026-10-31`. Existing task rows remain intact when Hibernate adds the nullable `due_date` column; their due date stays empty until set. Overdue means the due date is earlier than today in `ASSISTANT_TIME_ZONE`, and completed tasks are excluded. Tasks due today and tasks without a date are not overdue.

Task list query parameters: `search`, `status` (`TODO`, `IN_PROGRESS`, `DONE`), `priority` (`LOW`, `MEDIUM`, `HIGH`), `assignedUserId`, `page` (zero-based), `size` (1–100), `sort` (`field,direction`) or `sortBy` and `sortDirection`. Allowed sort fields: `id`, `title`, `status`, `priority`, `createdAt`, `updatedAt`. Invalid enum, JSON, pagination, and sort values return HTTP 400 with `{ "message": "...", "statusCode": 400 }`.

Task and user persistence entities are mapped to response DTOs; password hashes are not returned. Registration passwords are stored with BCrypt. JWTs use the configured secret and expiration. `USER` accounts can only read and change assigned tasks; `ADMIN` can manage tasks across users and access user administration.

## AI task tool boundary

`POST /assistant/chat` accepts `{ "message": "Which tasks are overdue?" }`. The Spring service sends the question and two read-only function definitions to OpenAI's Responses API. If the model requests a task function, the application validates the tool name and empty arguments, executes it through `TaskService` using the JWT-authenticated identity, returns the tool result to the model, and returns the final text response. This follows OpenAI's documented function-calling loop: [Function calling | OpenAI API](https://developers.openai.com/api/docs/guides/function-calling).

The only model tools are `getPendingTasks` and `getOverdueTasks`; both have no arguments and return at most 100 of the authenticated user's tasks. The model cannot pass a user ID or execute writes. Tool results omit assignee email. The integration caps tool rounds and output tokens, disables parallel calls, uses a 45-second HTTP read timeout, and sets `store: false`. The user's message and permitted task titles/descriptions are still sent to OpenAI for response generation, so configure this only for data your organization permits sending to that provider. API keys are read from environment variables and never returned in API responses or logs.

## Redis and Kafka behavior

- Cache: `task-query`, 60-second TTL, with caller identity included in each query key. Successful create, update, and delete operations evict task-list entries.
- Redis is a cache only. If a cache read/write/eviction fails, the API logs the failure and continues with PostgreSQL; an eviction outage can leave old results visible until the 60-second TTL expires.
- Kafka receives `CREATED`, `UPDATED`, and `DELETED` JSON events keyed by task ID. Producer send failures are logged and do not roll back the database mutation. The consumer currently logs events only; Kafka redelivery is harmless because it has no side effects. There is no outbox, so delivery is best-effort rather than guaranteed.
- If Kafka is temporarily unavailable, the app retries broker connections and the producer logs send failures. In local development set `KAFKA_ENABLED=false` when Kafka is intentionally not running. In Compose Kafka is enabled and is a startup dependency.

## Tests and CI

```powershell
mvn test
mvn package
docker compose config --quiet
```

The Testcontainers integration tests require a running Docker engine. The existing GitHub Actions workflow runs Maven tests and builds the Docker image.

## Troubleshooting

- `docker compose ps` should show PostgreSQL, Redis, Kafka, and app as running; inspect `docker compose logs -f <service>` for startup errors.
- If the app exits before startup, verify each dependency's health check and confirm ports `5432`, `6379`, `9092`, and `8080` are available.
- If Kafka is reachable from a host client, use `localhost:9092`; from the app container, use `kafka:9094`.
- If port overrides are used, `APP_PORT`, `POSTGRES_PORT`, and `REDIS_PORT` affect host bindings only.
- If authentication fails after changing `JWT_SECRET`, log in again to obtain a token signed with the current key.
- If `/v3/api-docs` fails, check `docker compose logs app` and verify that the app profile hasn't disabled springdoc; `/v3/api-docs/**` and Swagger UI are public.
- Back up PostgreSQL before modifying Compose project names or volume names; a renamed project may create a new empty volume while the original data remains in the old volume.
