# SSH Executor Agent

## Overview
HTTP service for executing commands on remote servers via SSH. Built with Java 17, Spring Boot, and Apache MINA SSHD.

## Recent Changes
- 2026-02-13: Initial project creation with full structure

## Project Architecture

### Tech Stack
- Java 17 (targeting), GraalVM runtime
- Gradle (Groovy DSL) build system
- Spring Boot 3.2.5 (Web)
- Apache MINA SSHD 2.12.1 (SSH client)
- Jackson (JSON)

### Code Structure
```
src/main/java/com/sshexecutor/
├── SshExecutorAgentApplication.java   # Main entry point
├── controller/
│   └── ExecController.java            # REST endpoints (/exec, /servers)
├── service/
│   └── SshExecutionService.java       # SSH command execution logic
├── config/
│   ├── ServerConfigLoader.java        # Loads servers.json at startup
│   └── ServerInfo.java                # Server configuration POJO
├── dto/
│   ├── ExecRequest.java               # Request DTO for /exec
│   └── ExecResponse.java              # Response DTO (result field)
├── util/
│   └── OutputLimiter.java             # Truncates output at 10KB
└── exception/
    ├── GlobalExceptionHandler.java    # @RestControllerAdvice error handler
    ├── ServerNotFoundException.java   # 404 for unknown servers
    └── SshExecutionException.java     # 500 for SSH failures
```

### Key Files
- `servers.json` — Server connection configuration (root of project)
- `build.gradle` — Gradle build with Spring Boot plugin
- `Dockerfile` — Multi-stage build for production
- `docker-compose.example.yml` — Docker Compose example

### API Endpoints
- `POST /exec` — Execute SSH command on a server
- `GET /servers` — List configured servers

### Build & Run
```bash
gradle bootJar          # Build
java -jar build/libs/app.jar  # Run (port 5000)
```

### Constraints
- Command timeout: 30 seconds
- Output limit: 10 KB per stream (stdout/stderr)
- Server port: 5000 (configurable via PORT env var)

## User Preferences
- Clean, readable code
- No unnecessary dependencies
- SLF4J logging
- No state, no history, no auth
