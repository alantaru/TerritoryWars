package com.alantaru.territorywars.exception;

public class TerritoryDataException extends Exception {
    public TerritoryDataException(String message) {
        super(message);
    }

    public TerritoryDataException(String message, Throwable cause) {
        super(message, cause);
    }
}
