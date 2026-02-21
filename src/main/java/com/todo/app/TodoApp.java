package com.todo.app;

import com.todo.model.TaskList;
import com.todo.storage.StorageException;
import com.todo.storage.TaskRepository;
import com.todo.tui.ListScreen;
import lombok.AccessLevel;
import lombok.NonNull;
import lombok.RequiredArgsConstructor;

@RequiredArgsConstructor(access = AccessLevel.PRIVATE)
public class TodoApp {
    @NonNull private final TaskList       taskList;
    @NonNull private final TaskRepository repository;

    public static TodoApp create() {
        final TaskRepository repository = new TaskRepository();
        final TaskList taskList;
        try {
            taskList = repository.load();
        } catch (StorageException e) {
            System.err.println("Error: " + e.getMessage());
            System.exit(1);
            return null;
        }
        return new TodoApp(taskList, repository);
    }

    public void run() {
        new ListScreen(taskList, repository, true).run();
    }
}
