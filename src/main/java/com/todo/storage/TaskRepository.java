package com.todo.storage;

import com.fasterxml.jackson.core.type.TypeReference;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.SerializationFeature;
import com.fasterxml.jackson.datatype.jsr310.JavaTimeModule;
import com.todo.model.Task;
import com.todo.model.TaskList;
import com.todo.ui.Ansi;
import lombok.NonNull;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.List;

public class TaskRepository {
    private static final Path DEFAULT_FILE = Path.of(System.getProperty("user.home"), ".todo", "tasks.json");

    private final Path         dataFile;
    private final Path         deletedFile;
    private final ObjectMapper mapper;

    public TaskRepository() {
        this(DEFAULT_FILE);
    }

    public TaskRepository(@NonNull final Path dataFile) {
        this.dataFile    = dataFile;
        this.deletedFile = dataFile.resolveSibling("deleted.json");
        this.mapper      = new ObjectMapper();
        this.mapper.registerModule(new JavaTimeModule());
        this.mapper.disable(SerializationFeature.WRITE_DATES_AS_TIMESTAMPS);
        this.mapper.enable(SerializationFeature.INDENT_OUTPUT);
    }

    public TaskList load() {
        if (!Files.exists(dataFile)) {
            return new TaskList(new ArrayList<>());
        }
        try {
            final List<Task> tasks = mapper.readValue(dataFile.toFile(), new TypeReference<>() {});
            return new TaskList(tasks);
        } catch (IOException e) {
            System.err.println(Ansi.RED + "Error reading file: " + e.getMessage() + Ansi.RESET);
            return new TaskList(new ArrayList<>());
        }
    }

    public void saveDeleted(@NonNull final Task task) {
        try {
            Files.createDirectories(deletedFile.getParent());
            final List<Task> existing = Files.exists(deletedFile)
                    ? new ArrayList<>(mapper.readValue(deletedFile.toFile(), new TypeReference<>() {}))
                    : new ArrayList<>();
            existing.add(task);
            mapper.writeValue(deletedFile.toFile(), existing);
        } catch (IOException e) {
            System.err.println(Ansi.RED + "Error saving deleted task: " + e.getMessage() + Ansi.RESET);
        }
    }

    public List<Task> loadDeleted() {
        if (!Files.exists(deletedFile)) return new ArrayList<>();
        try {
            return mapper.readValue(deletedFile.toFile(), new TypeReference<>() {});
        } catch (IOException e) {
            System.err.println(Ansi.RED + "Error reading deleted file: " + e.getMessage() + Ansi.RESET);
            return new ArrayList<>();
        }
    }

    public void save(@NonNull final TaskList taskList) {
        try {
            Files.createDirectories(dataFile.getParent());
            mapper.writeValue(dataFile.toFile(), taskList.getAll());
        } catch (IOException e) {
            System.err.println(Ansi.RED + "Error saving file: " + e.getMessage() + Ansi.RESET);
        }
    }
}
