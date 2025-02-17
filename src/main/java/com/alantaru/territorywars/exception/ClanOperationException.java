package com.alantaru.territorywars.exception;

public class ClanOperationException extends Exception {
    public ClanOperationException(String message) {
        super(message);
    }

    public ClanOperationException(String message, Throwable cause) {
        super(message, cause);
    }
}
