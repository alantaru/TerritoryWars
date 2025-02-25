package com.alantaru.territorywars.exception;

/**
 * Exception thrown when a clan is not found.
 */
public class ClanNotFoundException extends RuntimeException {
    
    /**
     * Creates a new ClanNotFoundException with the specified message.
     * @param message The error message
     */
    public ClanNotFoundException(String message) {
        super(message);
    }
    
    /**
     * Creates a new ClanNotFoundException with the specified message and cause.
     * @param message The error message
     * @param cause The cause of the exception
     */
    public ClanNotFoundException(String message, Throwable cause) {
        super(message, cause);
    }
}
