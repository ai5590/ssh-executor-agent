package com.sshexecutor.controller;

import com.sshexecutor.config.ServerConfigLoader;
import com.sshexecutor.config.ServerInfo;
import com.sshexecutor.dto.DirectExecRequest;
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

    /**
     * Новый основной endpoint: выполняет команду по host/port/user/command,
     * авторизация только key, ключ берётся из настроек (по умолчанию ./keys/agent-ssh-key).
     */
    @PostMapping("/exec")
    public ResponseEntity<ExecResponse> execDirect(@RequestBody DirectExecRequest request) {
        if (request.getHost() == null || request.getHost().isBlank()) {
            throw new IllegalArgumentException("host is required");
        }
        if (request.getUser() == null || request.getUser().isBlank()) {
            throw new IllegalArgumentException("user is required");
        }
        if (request.getCommand() == null || request.getCommand().isBlank()) {
            throw new IllegalArgumentException("command is required");
        }

        int port = (request.getPort() == null || request.getPort() <= 0) ? 22 : request.getPort();

        log.info("Executing direct command on {}@{}:{}", request.getUser(), request.getHost(), port);
        String result = sshExecutionService.executeDirect(request.getHost(), port, request.getUser(), request.getCommand());
        return ResponseEntity.ok(new ExecResponse(result));
    }

    /**
     * Старый режим: выполнить команду по имени сервера из servers.json.
     * Было POST /exec, стало POST /exec_on_server.
     */
    @PostMapping("/exec_on_server")
    public ResponseEntity<ExecResponse> execOnServer(@RequestBody ExecRequest request) {
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