package com.todo.model;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import java.util.List;

import static org.junit.jupiter.api.Assertions.*;

class TaskListTest {

    private TaskList list;

    @BeforeEach
    void setUp() {
        list = new TaskList(List.of());
    }

    // --- add ---

    @Test
    void add_appendsTaskWithCorrectText() {
        Task t = list.add("köp mjölk");
        assertEquals("köp mjölk", t.text());
        assertEquals(1, list.getAll().size());
    }

    @Test
    void add_assignsIncrementingIds() {
        Task t1 = list.add("ett");
        Task t2 = list.add("två");
        assertNotEquals(t1.id(), t2.id());
        assertTrue(t2.id() > t1.id());
    }

    @Test
    void add_newTaskIsNotDone() {
        assertFalse(list.add("test").done());
    }

    // --- delete ---

    @Test
    void delete_removesTaskAtIndex() {
        list.add("a");
        list.add("b");
        list.delete(0);
        assertEquals(1, list.getAll().size());
        assertEquals("b", list.getAll().get(0).text());
    }

    @Test
    void delete_returnsDeletedTask() {
        list.add("a");
        Task deleted = list.delete(0);
        assertNotNull(deleted);
        assertEquals("a", deleted.text());
    }

    @Test
    void delete_outOfBounds_returnsNull() {
        list.add("a");
        assertNull(list.delete(5));
        assertNull(list.delete(-1));
    }

    // --- toggleDone ---

    @Test
    void toggleDone_marksOpenTaskAsDone() {
        list.add("a");
        list.toggleDone(0);
        assertTrue(list.getAll().get(0).done());
    }

    @Test
    void toggleDone_marksDoneTaskAsOpen() {
        list.add("a");
        list.toggleDone(0);
        list.toggleDone(0);
        assertFalse(list.getAll().get(0).done());
    }

    @Test
    void toggleDone_outOfBounds_returnsFalse() {
        assertFalse(list.toggleDone(0));
    }

    // --- moveUp ---

    @Test
    void moveUp_swapsWithPrevious() {
        list.add("a");
        list.add("b");
        list.moveUp(1);
        assertEquals("b", list.getAll().get(0).text());
        assertEquals("a", list.getAll().get(1).text());
    }

    @Test
    void moveUp_atFirstIndex_returnsFalse() {
        list.add("a");
        assertFalse(list.moveUp(0));
    }

    // --- moveDown ---

    @Test
    void moveDown_swapsWithNext() {
        list.add("a");
        list.add("b");
        list.moveDown(0);
        assertEquals("b", list.getAll().get(0).text());
        assertEquals("a", list.getAll().get(1).text());
    }

    @Test
    void moveDown_atLastIndex_returnsFalse() {
        list.add("a");
        assertFalse(list.moveDown(0));
    }

    // --- getOpen ---

    @Test
    void getOpen_excludesDoneTasks() {
        list.add("a");
        list.add("b");
        list.toggleDone(0);
        List<Task> open = list.getOpen();
        assertEquals(1, open.size());
        assertEquals("b", open.get(0).text());
    }

    @Test
    void getOpen_emptyList_returnsEmpty() {
        assertTrue(list.getOpen().isEmpty());
    }
}
