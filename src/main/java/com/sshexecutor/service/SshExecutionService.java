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

    public SshExecutionService(ServerConfigLoader configLoader) {
        this.configLoader = configLoader;
    }

    public String execute(String serverName, String command) {
        ServerInfo server = configLoader.getServer(serverName);
        if (server == null) {
            throw new ServerNotFoundException("Server not found: " + serverName);
        }

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

                    return formatResult(serverName, exitCode, durationMs, stdout, stderr);
                }
            }
        } catch (ServerNotFoundException e) {
            throw e;
        } catch (Exception e) {
            long durationMs = System.currentTimeMillis() - startTime;
            log.error("SSH execution failed for server {}: {}", serverName, e.getMessage());
            throw new SshExecutionException("SSH execution failed: " + e.getMessage(), e);
        }
    }

    private void authenticate(ClientSession session, ServerInfo server) throws Exception {
        if ("key".equalsIgnoreCase(server.getAuthType())) {
            FileKeyPairProvider keyPairProvider = new FileKeyPairProvider(Path.of(server.getKeyPath()));
            keyPairProvider.loadKeys(null).forEach(session::addPublicKeyIdentity);
        } else if ("password".equalsIgnoreCase(server.getAuthType())) {
            session.addPasswordIdentity(server.getPassword());
        } else {
            throw new SshExecutionException("Unknown auth type: " + server.getAuthType());
        }
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
