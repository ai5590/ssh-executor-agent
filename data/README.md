# Runtime Data Directory

This folder contains runtime configuration for ssh-executor-agent.

## Setup

1. Copy a template to create your config:
   ```bash
   cp data/servers.template.two.json data/servers.json
   ```

2. Edit `data/servers.json` with your actual server details.

3. Place SSH private keys into `data/keys/`:
   ```bash
   cp ~/.ssh/my_key data/keys/agent-ssh-key
   chmod 600 data/keys/agent-ssh-key
   ```

## Important

- **DO NOT** commit `data/servers.json` (it contains real credentials)
- **DO NOT** commit real SSH keys from `data/keys/`
- These paths are already in `.gitignore`
