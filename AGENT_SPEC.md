# SSH Executor Agent — Specification

## Overview
HTTP service for executing commands on remote servers via SSH.

## API

### POST /exec
Execute a command on a remote server.

**Request:**
```json
{
  "server": "core01",
  "command": "uname -a"
}
```

**Response:**
```json
{
  "result": "=== EXECUTION RESULT ===\nserver: core01\nexit_code: 0\nduration_ms: 142\n\n--- STDOUT ---\nLinux core01 5.15.0\n\n--- STDERR ---\n\n\n=== END ==="
}
```

### GET /servers
List all configured servers.

**Response:**
```json
{
  "result": "core01 192.168.1.250\nvps01 10.10.10.2"
}
```

## Constraints
- Command execution timeout: 30 seconds
- Output limit (stdout + stderr): 10 KB each
- If output exceeds limit, it is truncated with `[OUTPUT TRUNCATED]` marker

## servers.json Format
```json
{
  "server_name": {
    "host": "192.168.1.250",
    "user": "root",
    "port": 22,
    "authType": "key",
    "keyPath": "/keys/server_ed25519"
  }
}
```

Supported `authType` values:
- `key` — authenticate using a private key file specified in `keyPath`
- `password` — authenticate using `password` field

## Extending the Project
1. Add new server entries to `servers.json`
2. Add new endpoints in `controller/` package
3. Add new services in `service/` package
4. Use `@RestControllerAdvice` for centralized error handling
