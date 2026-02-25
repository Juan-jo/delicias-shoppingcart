package org.delicias.exception;

public class ErrorResponse {
    public String message;
    public String code;
    public long timestamp;

    public ErrorResponse(String message, String code) {
        this.message = message;
        this.code = code;
        this.timestamp = System.currentTimeMillis();
    }
}