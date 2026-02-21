package com.todo.storage;

public class StorageException extends RuntimeException {
    public StorageException(final String message, final Throwable cause) {
        super(message, cause);
    }
}
