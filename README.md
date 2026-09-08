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
