package com.sshexecutor.config;

import com.fasterxml.jackson.core.type.TypeReference;
import com.fasterxml.jackson.databind.ObjectMapper;
import jakarta.annotation.PostConstruct;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Component;

import java.io.File;
import java.io.IOException;
import java.util.Collections;
import java.util.Map;

@Component
public class ServerConfigLoader {

    private static final Logger log = LoggerFactory.getLogger(ServerConfigLoader.class);
    private static final String DEFAULT_CONFIG_PATH = "./data/servers.json";

    private final String configPath;
    private Map<String, ServerInfo> servers = Collections.emptyMap();

    public ServerConfigLoader() {
        String envPath = System.getenv("SERVERS_CONFIG_PATH");
        this.configPath = (envPath != null && !envPath.isBlank()) ? envPath : DEFAULT_CONFIG_PATH;
    }

    @PostConstruct
    public void load() {
        log.info("Using server config path: {}", configPath);
        File file = new File(configPath);
        if (!file.exists()) {
            throw new IllegalStateException(
                    "Server config file not found: " + file.getAbsolutePath()
                    + ". Copy a template: cp data/servers.template.two.json data/servers.json");
        }
        try {
            ObjectMapper mapper = new ObjectMapper();
            servers = mapper.readValue(file, new TypeReference<Map<String, ServerInfo>>() {});
            log.info("Loaded {} server(s) from {}", servers.size(), configPath);
        } catch (IOException e) {
            throw new IllegalStateException("Failed to load server config from " + configPath + ": " + e.getMessage(), e);
        }
    }

    public ServerInfo getServer(String name) {
        return servers.get(name);
    }

    public Map<String, ServerInfo> getAllServers() {
        return Collections.unmodifiableMap(servers);
    }
}
