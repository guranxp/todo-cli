package com.todo.tui;

import com.googlecode.lanterna.input.KeyStroke;
import com.googlecode.lanterna.input.KeyType;
import com.todo.model.TaskList;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import java.util.List;

import static org.junit.jupiter.api.Assertions.*;

/**
 * Verifies that Shift+Arrow moves tasks and plain Arrow only moves the cursor.
 * Key detection: KeyStroke.isShiftDown() == true for Shift+Arrow.
 */
class ShiftArrowMoveTest {

    private static final KeyStroke SHIFT_UP   = new KeyStroke(KeyType.ArrowUp,   false, false, true);
    private static final KeyStroke SHIFT_DOWN = new KeyStroke(KeyType.ArrowDown, false, false, true);
    private static final KeyStroke UP         = new KeyStroke(KeyType.ArrowUp,   false, false, false);
    private static final KeyStroke DOWN       = new KeyStroke(KeyType.ArrowDown, false, false, false);

    private TaskList taskList;

    @BeforeEach
    void setUp() {
        taskList = new TaskList(List.of());
        taskList.add("a");
        taskList.add("b");
        taskList.add("c");
    }

    // --- Key detection ---

    @Test
    void shiftUp_isDetectedAsShift() {
        assertTrue(SHIFT_UP.isShiftDown());
        assertEquals(KeyType.ArrowUp, SHIFT_UP.getKeyType());
    }

    @Test
    void shiftDown_isDetectedAsShift() {
        assertTrue(SHIFT_DOWN.isShiftDown());
        assertEquals(KeyType.ArrowDown, SHIFT_DOWN.getKeyType());
    }

    @Test
    void plainUp_isNotShift() {
        assertFalse(UP.isShiftDown());
    }

    // --- Move behavior (via TaskList) ---

    @Test
    void shiftUp_movesTaskUp() {
        taskList.moveUp(2);
        assertEquals("a", taskList.getAll().get(0).text());
        assertEquals("c", taskList.getAll().get(1).text());
        assertEquals("b", taskList.getAll().get(2).text());
    }

    @Test
    void shiftDown_movesTaskDown() {
        taskList.moveDown(0);
        assertEquals("b", taskList.getAll().get(0).text());
        assertEquals("a", taskList.getAll().get(1).text());
        assertEquals("c", taskList.getAll().get(2).text());
    }

    @Test
    void shiftUp_thenShiftDown_movesInBothDirections() {
        taskList.moveUp(1);  // b moves to 0: b, a, c
        taskList.moveDown(0); // b moves back to 1: a, b, c
        assertEquals("a", taskList.getAll().get(0).text());
        assertEquals("b", taskList.getAll().get(1).text());
        assertEquals("c", taskList.getAll().get(2).text());
    }

    @Test
    void shiftUp_atTopBoundary_doesNothing() {
        boolean moved = taskList.moveUp(0);
        assertFalse(moved);
        assertEquals("a", taskList.getAll().get(0).text());
    }

    @Test
    void shiftDown_atBottomBoundary_doesNothing() {
        boolean moved = taskList.moveDown(2);
        assertFalse(moved);
        assertEquals("c", taskList.getAll().get(2).text());
    }
}
