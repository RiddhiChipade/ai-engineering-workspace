# AI Engineering Workspace

A Spring Boot 3.5.16 application with PostgreSQL persistence and REST endpoints for tasks.

## Run with Docker

### Prerequisites

- Docker Desktop
- Docker Compose

### Start the application

```powershell
docker compose up --build
```

### Run in background

```powershell
docker compose up -d
```

### View logs

```powershell
docker compose logs -f
```

### Stop containers

```powershell
docker compose down
```

### Reset the database

```powershell
docker compose down -v
```

This removes the local PostgreSQL volume and deletes stored data.

### Run tests

```powershell
mvn test
```

## Configuration

The application reads environment variables for database, Redis, and server configuration.

Required variables include:

```text
DB_HOST
DB_PORT
DB_NAME
DB_USERNAME
DB_PASSWORD
REDIS_HOST
REDIS_PORT
SERVER_PORT
SPRING_PROFILES_ACTIVE
JWT_SECRET
JWT_EXPIRATION
```

Local development uses the default `dev` profile. Docker uses the `docker` profile, which keeps PostgreSQL connectivity on the Compose service name `postgres` and Redis on the Compose service name `redis`.

## Redis cache

The application enables Spring Cache backed by Redis for the Task list query endpoint.

- Cache name: `task-query`
- TTL: 60 seconds
- Cached data: filtered paginated Task list responses
- Invalidation: create, update, and delete task operations clear the Task cache entries after a successful database mutation

This keeps PostgreSQL as the source of truth while using Redis only as a read-through cache for the list endpoint.

## Swagger / OpenAPI

The API documentation is available at:

- Swagger UI: http://localhost:8080/swagger-ui/index.html
- OpenAPI JSON: http://localhost:8080/v3/api-docs

## CI/CD

This project includes a GitHub Actions workflow for continuous integration.

- CI runs on every push and pull request
- Maven tests are executed automatically
- The application package is built with Maven
- The Docker image is built from the existing Dockerfile

### Rebuild after code changes

```powershell
docker compose up --build
```

## Application URL

After startup, the application is available at:

```text
http://localhost:8080
```

## Available API endpoints

```text
GET    /tasks
GET    /tasks/{id}
POST   /tasks
PUT    /tasks/{id}
DELETE /tasks/{id}
```

The `/tasks` endpoint supports optional query parameters such as:

```text
search
status
priority
assignedUserId
page
size
sort
sortBy
sortDirection
```

## Docker architecture

```text
Host machine
   |
   | HTTP
   v
Spring Boot app container
   |
   | internal Docker network
   v
PostgreSQL container
```

The application connects to PostgreSQL using the Compose service name `postgres`, not `localhost`.

