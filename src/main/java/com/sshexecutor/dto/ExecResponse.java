package com.sshexecutor.dto;

public class ExecResponse {

    private String result;

    public ExecResponse() {
    }

    public ExecResponse(String result) {
        this.result = result;
    }

    public String getResult() {
        return result;
    }

    public void setResult(String result) {
        this.result = result;
    }
}
