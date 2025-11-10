package com.example.skillsplugin.data;

/**
 * Exception thrown when data storage operations fail.
 */
public class DataStorageException extends Exception {
    
    public DataStorageException(String message) {
        super(message);
    }
    
    public DataStorageException(String message, Throwable cause) {
        super(message, cause);
    }
}
