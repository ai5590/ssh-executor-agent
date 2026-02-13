# API Contract

## POST /exec

Execute a command on a configured remote server via SSH.

### Request
```
POST /exec
Content-Type: application/json
```

```json
{
  "server": "core01",
  "command": "uname -a"
}
```

### Success Response (200)
```json
{
  "result": "=== EXECUTION RESULT ===\nserver: core01\nexit_code: 0\nduration_ms: 142\n\n--- STDOUT ---\nLinux core01 5.15.0-generic #1 SMP x86_64 GNU/Linux\n\n--- STDERR ---\n\n\n=== END ==="
}
```

### Error: Server Not Found (404)
```json
{
  "result": "Server not found: unknown_server"
}
```

### Error: Missing Command (400)
```json
{
  "result": "Command is required"
}
```

### Error: SSH Failure (500)
```json
{
  "result": "SSH execution failed: Connection refused"
}
```

---

## GET /servers

List all configured servers with their hostnames.

### Request
```
GET /servers
```

### Success Response (200)
```json
{
  "result": "core01 192.168.1.250\nvps01 10.10.10.2"
}
```
