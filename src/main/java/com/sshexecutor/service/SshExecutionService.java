package com.sshexecutor.service;

import com.sshexecutor.config.ServerConfigLoader;
import com.sshexecutor.config.ServerInfo;
import com.sshexecutor.exception.ServerNotFoundException;
import com.sshexecutor.exception.SshExecutionException;
import com.sshexecutor.util.OutputLimiter;
import org.apache.sshd.client.SshClient;
import org.apache.sshd.client.channel.ClientChannel;
import org.apache.sshd.client.channel.ClientChannelEvent;
import org.apache.sshd.client.session.ClientSession;
import org.apache.sshd.common.keyprovider.FileKeyPairProvider;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;

import java.io.ByteArrayOutputStream;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.EnumSet;
import java.util.concurrent.TimeUnit;

@Service
public class SshExecutionService {

    private static final Logger log = LoggerFactory.getLogger(SshExecutionService.class);

    private final ServerConfigLoader configLoader;

    @Value("${ssh.command.timeout:30}")
    private int commandTimeoutSeconds;

    @Value("${ssh.output.limit:10240}")
    private int outputLimitBytes;

    @Value("${ssh.keys.dir:./keys}")
    private String keysDir;

    @Value("${ssh.default.key:agent-ssh-key}")
    private String defaultKeyName;

    public SshExecutionService(ServerConfigLoader configLoader) {
        this.configLoader = configLoader;
    }

    /**
     * Старый режим: сервер берём из servers.json
     */
    public String execute(String serverName, String command) {
        ServerInfo server = configLoader.getServer(serverName);
        if (server == null) {
            throw new ServerNotFoundException("Server not found: " + serverName);
        }
        return executeInternal(serverName, server, command);
    }

    /**
     * Новый режим: параметры приходят напрямую, всегда key auth,
     * ключ берём из keysDir/defaultKeyName.
     */
    public String executeDirect(String host, int port, String user, String command) {
        ServerInfo server = new ServerInfo();
        server.setHost(host);
        server.setPort(port);
        server.setUser(user);
        server.setAuthType("key");
        server.setKeyPath(resolveDefaultKeyPath().toString());

        String label = user + "@" + host + ":" + port;
        return executeInternal(label, server, command);
    }

    private String executeInternal(String serverLabel, ServerInfo server, String command) {
        long startTime = System.currentTimeMillis();

        try (SshClient client = SshClient.setUpDefaultClient()) {
            client.start();

            try (ClientSession session = client.connect(
                    server.getUser(),
                    server.getHost(),
                    server.getPort()
            ).verify(commandTimeoutSeconds, TimeUnit.SECONDS).getSession()) {

                authenticate(session, server);
                session.auth().verify(commandTimeoutSeconds, TimeUnit.SECONDS);

                try (ClientChannel channel = session.createExecChannel(command)) {
                    ByteArrayOutputStream stdoutStream = new ByteArrayOutputStream();
                    ByteArrayOutputStream stderrStream = new ByteArrayOutputStream();
                    channel.setOut(stdoutStream);
                    channel.setErr(stderrStream);

                    channel.open().verify(commandTimeoutSeconds, TimeUnit.SECONDS);
                    channel.waitFor(
                            EnumSet.of(ClientChannelEvent.CLOSED),
                            TimeUnit.SECONDS.toMillis(commandTimeoutSeconds)
                    );

                    long durationMs = System.currentTimeMillis() - startTime;
                    Integer exitCode = channel.getExitStatus();

                    String stdout = OutputLimiter.limit(stdoutStream.toString(), outputLimitBytes);
                    String stderr = OutputLimiter.limit(stderrStream.toString(), outputLimitBytes);

                    return formatResult(serverLabel, exitCode, durationMs, stdout, stderr);
                }
            }
        } catch (ServerNotFoundException e) {
            throw e;
        } catch (SshExecutionException e) {
            throw e;
        } catch (Exception e) {
            long durationMs = System.currentTimeMillis() - startTime;
            log.error("SSH execution failed for {} ({} ms): {}", serverLabel, durationMs, e.getMessage());
            throw new SshExecutionException("SSH execution failed: " + e.getMessage(), e);
        }
    }

    private void authenticate(ClientSession session, ServerInfo server) throws Exception {
        if ("key".equalsIgnoreCase(server.getAuthType())) {
            if (server.getKeyPath() == null || server.getKeyPath().isBlank()) {
                throw new SshExecutionException("Key auth selected but keyPath is empty");
            }
            Path keyPath = Path.of(server.getKeyPath());
            if (!Files.exists(keyPath)) {
                throw new SshExecutionException("SSH key file not found: " + keyPath.toAbsolutePath()
                        + ". Put your key there or change ssh.keys.dir / ssh.default.key.");
            }
            FileKeyPairProvider keyPairProvider = new FileKeyPairProvider(keyPath);
            keyPairProvider.loadKeys(null).forEach(session::addPublicKeyIdentity);

        } else if ("password".equalsIgnoreCase(server.getAuthType())) {
            session.addPasswordIdentity(server.getPassword());
        } else {
            throw new SshExecutionException("Unknown auth type: " + server.getAuthType());
        }
    }

    private Path resolveDefaultKeyPath() {
        if (defaultKeyName == null || defaultKeyName.isBlank()) {
            throw new SshExecutionException("ssh.default.key is empty");
        }
        Path baseDir = Path.of(keysDir == null || keysDir.isBlank() ? "./keys" : keysDir).toAbsolutePath().normalize();
        Path keyPath = baseDir.resolve(defaultKeyName).toAbsolutePath().normalize();

        // safety: key must be inside keysDir
        if (!keyPath.startsWith(baseDir)) {
            throw new SshExecutionException("Invalid default key path (must be inside " + baseDir + "): " + keyPath);
        }

        // explicit error if key missing (as you requested)
        if (!Files.exists(keyPath)) {
            throw new SshExecutionException("SSH key file not found: " + keyPath
                    + ". Expected default key at ./keys/agent-ssh-key (or override ssh.keys.dir / ssh.default.key).");
        }

        return keyPath;
    }

    private String formatResult(String serverName, Integer exitCode, long durationMs,
                                String stdout, String stderr) {
        StringBuilder sb = new StringBuilder();
        sb.append("=== EXECUTION RESULT ===\n");
        sb.append("server: ").append(serverName).append("\n");
        sb.append("exit_code: ").append(exitCode != null ? exitCode : "N/A").append("\n");
        sb.append("duration_ms: ").append(durationMs).append("\n");
        sb.append("\n");
        sb.append("--- STDOUT ---\n");
        sb.append(stdout).append("\n");
        sb.append("\n");
        sb.append("--- STDERR ---\n");
        sb.append(stderr).append("\n");
        sb.append("\n");
        sb.append("=== END ===");
        return sb.toString();
    }
}