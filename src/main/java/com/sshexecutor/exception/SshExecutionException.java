package com.sshexecutor.exception;

public class SshExecutionException extends RuntimeException {

    public SshExecutionException(String message) {
        super(message);
    }

    public SshExecutionException(String message, Throwable cause) {
        super(message, cause);
    }
}
