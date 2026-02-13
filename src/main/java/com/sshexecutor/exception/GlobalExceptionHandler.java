package com.sshexecutor.exception;

import com.sshexecutor.dto.ExecResponse;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.RestControllerAdvice;

@RestControllerAdvice
public class GlobalExceptionHandler {

    private static final Logger log = LoggerFactory.getLogger(GlobalExceptionHandler.class);

    @ExceptionHandler(IllegalArgumentException.class)
    public ResponseEntity<ExecResponse> handleIllegalArgument(IllegalArgumentException e) {
        log.warn("Bad request: {}", e.getMessage());
        return ResponseEntity
                .status(HttpStatus.BAD_REQUEST)
                .body(new ExecResponse(e.getMessage()));
    }

    @ExceptionHandler(ServerNotFoundException.class)
    public ResponseEntity<ExecResponse> handleServerNotFound(ServerNotFoundException e) {
        log.warn("Server not found: {}", e.getMessage());
        return ResponseEntity
                .status(HttpStatus.NOT_FOUND)
                .body(new ExecResponse(e.getMessage()));
    }

    @ExceptionHandler(SshExecutionException.class)
    public ResponseEntity<ExecResponse> handleSshExecution(SshExecutionException e) {
        log.error("SSH execution error: {}", e.getMessage());
        return ResponseEntity
                .status(HttpStatus.INTERNAL_SERVER_ERROR)
                .body(new ExecResponse(e.getMessage()));
    }

    @ExceptionHandler(Exception.class)
    public ResponseEntity<ExecResponse> handleGeneral(Exception e) {
        log.error("Unexpected error: {}", e.getMessage(), e);
        return ResponseEntity
                .status(HttpStatus.INTERNAL_SERVER_ERROR)
                .body(new ExecResponse("Internal server error"));
    }
}
