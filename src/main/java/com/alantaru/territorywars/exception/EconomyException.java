package com.alantaru.territorywars.exception;

public class EconomyException extends Exception {
    public EconomyException(String message) {
        super(message);
    }

    public EconomyException(String message, Throwable cause) {
        super(message, cause);
    }
}
