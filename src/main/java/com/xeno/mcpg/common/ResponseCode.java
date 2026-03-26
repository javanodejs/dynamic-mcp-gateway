package com.xeno.mcpg.common;

/**
 * Standard response codes for API responses.
 */
public enum ResponseCode {

    OK_REQUEST(200, "success"),
    BAD_REQUEST(400, "bad request"),
    NOT_FOUND(404, "not found"),
    INTERNAL_ERROR(500, "internal error");

    private final int code;
    private final String message;

    ResponseCode(int code, String message) {
        this.code = code;
        this.message = message;
    }

    public int getCode() {
        return code;
    }

    public String getMessage() {
        return message;
    }
}