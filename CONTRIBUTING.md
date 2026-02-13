# Contributing

## Prerequisites
- Java 17+
- Gradle 8+

## Running Locally
```bash
gradle bootRun
```
The server starts on port 5000 by default (configurable via `PORT` env var or `server.port` in `application.properties`).

## Building
```bash
gradle bootJar
```
The JAR is produced at `build/libs/app.jar`.

## Building with Docker
```bash
docker build -t ssh-executor-agent .
docker run -p 8080:8080 -v ./servers.json:/app/servers.json:ro -v ./keys:/keys:ro ssh-executor-agent
```

## Testing
```bash
# List servers
curl http://localhost:5000/servers

# Execute command
curl -X POST http://localhost:5000/exec \
  -H "Content-Type: application/json" \
  -d '{"server": "core01", "command": "uname -a"}'
```

## Adding New Endpoints
1. Create a new method in `ExecController` or a new controller in `controller/` package
2. Create request/response DTOs in `dto/` package if needed
3. Add service logic in `service/` package
4. Handle errors using exceptions caught by `GlobalExceptionHandler`
