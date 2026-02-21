package com.todo.model;

import lombok.NonNull;

import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.atomic.AtomicLong;

public class TaskList {
    private final List<Task>  tasks;
    private final AtomicLong  nextId;

    public TaskList(@NonNull final List<Task> tasks) {
        this.tasks  = new ArrayList<>(tasks);
        this.nextId = new AtomicLong(tasks.stream().mapToLong(Task::id).max().orElse(0) + 1);
    }

    public Task add(@NonNull final String text) {
        final Task task = Task.create(nextId.getAndIncrement(), text);
        tasks.add(task);
        return task;
    }

    public boolean delete(final int index) {
        if (index < 0 || index >= tasks.size()) return false;
        tasks.remove(index);
        return true;
    }

    public boolean toggleDone(final int index) {
        if (index < 0 || index >= tasks.size()) return false;
        final Task old = tasks.get(index);
        tasks.set(index, new Task(old.id(), old.text(), !old.done(), old.createdAt()));
        return true;
    }

    public boolean updateText(final int index, @NonNull final String newText) {
        if (index < 0 || index >= tasks.size()) return false;
        final Task old = tasks.get(index);
        tasks.set(index, new Task(old.id(), newText, old.done(), old.createdAt()));
        return true;
    }

    public boolean moveUp(final int index) {
        if (index <= 0 || index >= tasks.size()) return false;
        final Task t = tasks.remove(index);
        tasks.add(index - 1, t);
        return true;
    }

    public boolean moveDown(final int index) {
        if (index < 0 || index >= tasks.size() - 1) return false;
        final Task t = tasks.remove(index);
        tasks.add(index + 1, t);
        return true;
    }

    public List<Task> getAll()  { return tasks; }

    public List<Task> getOpen() {
        return tasks.stream().filter(t -> !t.done()).toList();
    }
}
