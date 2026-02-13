# SSH Executor Agent

HTTP service for executing commands on remote servers via SSH.

## Quick Start

### 1. Create config from template

```bash
cp data/servers.template.two.json data/servers.json
```

Edit `data/servers.json` with your actual server details.

### 2. Add SSH keys (for key-based auth)

```bash
cp ~/.ssh/my_key data/keys/agent-ssh-key
chmod 600 data/keys/agent-ssh-key
```

### 3. Run locally

```bash
gradle bootJar
java -jar build/libs/app.jar
```

The service starts on port 5000 by default.

### 4. Run with Docker

```bash
docker build -t ssh-executor-agent:latest .
docker run -d --name ssh-executor-agent \
  -p 25005:8080 \
  -v $(pwd)/data:/data:ro \
  -e SERVERS_CONFIG_PATH=/data/servers.json \
  ssh-executor-agent:latest
```

Or with Docker Compose:

```bash
docker compose -f docker-compose.example.yml up -d
```

## Configuration

Server config is loaded from the path specified by the `SERVERS_CONFIG_PATH` environment variable.
If not set, defaults to `./data/servers.json`.

Templates are available in `data/`:

| Template | Description |
|---|---|
| `servers.template.two.json` | Two servers: one key auth, one password auth |
| `servers.template.key.json` | One server with key auth |
| `servers.template.pass.json` | One server with password auth |

## API Usage

### List servers

```bash
curl http://localhost:5000/servers
```

Response:
```json
{
  "result": "core01 192.168.1.250\nvps01 10.10.10.2"
}
```

### Execute command

```bash
curl -X POST http://localhost:5000/exec \
  -H "Content-Type: application/json" \
  -d '{"server": "core01", "command": "uname -a"}'
```

Response:
```json
{
  "result": "=== EXECUTION RESULT ===\nserver: core01\nexit_code: 0\nduration_ms: 142\n\n--- STDOUT ---\nLinux core01 5.15.0\n\n--- STDERR ---\n\n\n=== END ==="
}
```

## Constraints

- Command timeout: 30 seconds
- Output limit: 10 KB per stream (stdout/stderr)
- Truncated output is marked with `[OUTPUT TRUNCATED]`
