package org.delicias.exception;

import lombok.Getter;

public class ShoppingBusinessException extends RuntimeException {

    @Getter
    private final int status;

    private final ShoppingErrorCode errorCode;

    public ShoppingBusinessException(String message, ShoppingErrorCode errorCode, int status) {
        super(message);
        this.errorCode = errorCode;
        this.status = status;
    }

    public String getErrorCode() { return errorCode.name(); }
}