package com.sshexecutor.controller;

import com.sshexecutor.config.ServerConfigLoader;
import com.sshexecutor.config.ServerInfo;
import com.sshexecutor.dto.ExecRequest;
import com.sshexecutor.dto.ExecResponse;
import com.sshexecutor.service.SshExecutionService;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.Map;
import java.util.stream.Collectors;

@RestController
public class ExecController {

    private static final Logger log = LoggerFactory.getLogger(ExecController.class);

    private final SshExecutionService sshExecutionService;
    private final ServerConfigLoader serverConfigLoader;

    public ExecController(SshExecutionService sshExecutionService, ServerConfigLoader serverConfigLoader) {
        this.sshExecutionService = sshExecutionService;
        this.serverConfigLoader = serverConfigLoader;
    }

    @PostMapping("/exec")
    public ResponseEntity<ExecResponse> exec(@RequestBody ExecRequest request) {
        if (request.getServer() == null || request.getServer().isBlank()) {
            throw new IllegalArgumentException("Server name is required");
        }
        if (request.getCommand() == null || request.getCommand().isBlank()) {
            throw new IllegalArgumentException("Command is required");
        }

        log.info("Executing command on server: {}", request.getServer());
        String result = sshExecutionService.execute(request.getServer(), request.getCommand());
        return ResponseEntity.ok(new ExecResponse(result));
    }

    @GetMapping("/servers")
    public ResponseEntity<ExecResponse> servers() {
        Map<String, ServerInfo> allServers = serverConfigLoader.getAllServers();
        String result = allServers.entrySet().stream()
                .map(e -> e.getKey() + " " + e.getValue().getHost())
                .collect(Collectors.joining("\n"));
        return ResponseEntity.ok(new ExecResponse(result));
    }
}
