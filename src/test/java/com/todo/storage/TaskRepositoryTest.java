package com.todo.storage;

import com.todo.model.Task;
import com.todo.model.TaskList;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;

import java.nio.file.Path;
import java.util.List;

import static org.junit.jupiter.api.Assertions.*;

class TaskRepositoryTest {

    @TempDir
    Path tempDir;

    private TaskRepository repo() {
        return new TaskRepository(tempDir.resolve("tasks.json"));
    }

    @Test
    void load_whenFileAbsent_returnsEmptyList() {
        TaskList result = repo().load();
        assertTrue(result.getAll().isEmpty());
    }

    @Test
    void saveAndLoad_roundtrip_preservesAllFields() {
        TaskRepository repository = repo();
        TaskList original = new TaskList(List.of());
        original.add("köp mjölk");
        original.add("ring kalle");
        original.toggleDone(1);

        repository.save(original);
        TaskList loaded = repository.load();

        List<Task> tasks = loaded.getAll();
        assertEquals(2, tasks.size());

        assertEquals("köp mjölk", tasks.get(0).text());
        assertFalse(tasks.get(0).done());
        assertNotNull(tasks.get(0).createdAt());

        assertEquals("ring kalle", tasks.get(1).text());
        assertTrue(tasks.get(1).done());
    }

    @Test
    void saveAndLoad_preservesOrderOfTasks() {
        TaskRepository repository = repo();
        TaskList original = new TaskList(List.of());
        original.add("ett");
        original.add("två");
        original.add("tre");

        repository.save(original);
        TaskList loaded = repository.load();

        List<Task> tasks = loaded.getAll();
        assertEquals("ett", tasks.get(0).text());
        assertEquals("två", tasks.get(1).text());
        assertEquals("tre", tasks.get(2).text());
    }

    @Test
    void save_multipleTimes_overwritesPreviousData() {
        TaskRepository repository = repo();
        TaskList list = new TaskList(List.of());
        list.add("gammal");
        repository.save(list);

        list.delete(0);
        list.add("ny");
        repository.save(list);

        TaskList loaded = repository.load();
        assertEquals(1, loaded.getAll().size());
        assertEquals("ny", loaded.getAll().get(0).text());
    }
}
