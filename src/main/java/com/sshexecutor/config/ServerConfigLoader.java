package com.sshexecutor.config;

import com.fasterxml.jackson.core.type.TypeReference;
import com.fasterxml.jackson.databind.ObjectMapper;
import jakarta.annotation.PostConstruct;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;

import java.io.File;
import java.io.IOException;
import java.util.Collections;
import java.util.Map;

@Component
public class ServerConfigLoader {

    private static final Logger log = LoggerFactory.getLogger(ServerConfigLoader.class);

    @Value("${servers.config.path:servers.json}")
    private String configPath;

    private Map<String, ServerInfo> servers = Collections.emptyMap();

    @PostConstruct
    public void load() {
        try {
            ObjectMapper mapper = new ObjectMapper();
            File file = new File(configPath);
            if (!file.exists()) {
                log.warn("Server config file not found: {}", configPath);
                return;
            }
            servers = mapper.readValue(file, new TypeReference<Map<String, ServerInfo>>() {});
            log.info("Loaded {} server(s) from {}", servers.size(), configPath);
        } catch (IOException e) {
            log.error("Failed to load server config from {}: {}", configPath, e.getMessage());
        }
    }

    public ServerInfo getServer(String name) {
        return servers.get(name);
    }

    public Map<String, ServerInfo> getAllServers() {
        return Collections.unmodifiableMap(servers);
    }
}
