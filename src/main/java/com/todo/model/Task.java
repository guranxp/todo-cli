package com.todo.model;

import java.time.LocalDateTime;

import static java.util.Objects.requireNonNull;

public record Task(long id, String text, boolean done, LocalDateTime createdAt) {

    public Task {
        requireNonNull(text, "text is null");
        requireNonNull(createdAt, "createdAt is null");
    }

    public static Task create(final long id, final String text) {
        return new Task(id, text, false, LocalDateTime.now());
    }
}
