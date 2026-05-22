# Local Development Environment (Phase 1)

## Prerequisites

- Docker Engine or Docker Desktop
- Java 17+
- Maven 3.9+

## Start Local Infrastructure

```bash
docker compose up -d
```

Expected exposed ports:
- MySQL: `localhost:3306`
- Redis: `localhost:6379`
- Kafka: `localhost:9092`

## Verify Container Status

```bash
docker compose ps
```

Expected status:
- MySQL is `healthy`
- Redis is `healthy`
- Kafka is `running`

## Run Application With Local Profile

```bash
mvn spring-boot:run -Dspring-boot.run.profiles=local
```

## Verify Health Endpoint

```bash
curl http://localhost:8080/actuator/health
```

Expected result:
- JSON contains top-level `"status":"UP"` when dependencies are reachable.

## Stop Local Infrastructure

```bash
docker compose down
```

Remove volumes if needed:

```bash
docker compose down -v
```

## Common Issues

1. Port collision on `3306`, `6379`, or `9092`:
   - stop conflicting local process/container and restart.
2. MySQL health not ready yet:
   - wait and re-run `docker compose ps`.
3. App startup fails on datasource:
   - check MySQL health and local credentials in `application-local.yml`.
