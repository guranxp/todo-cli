package com.todo.command;

import com.todo.model.TaskList;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import java.io.ByteArrayOutputStream;
import java.io.PrintStream;
import java.util.List;

import static org.junit.jupiter.api.Assertions.*;

class CommandHandlerTest {

    private CommandHandler handler;
    private TaskList taskList;

    private final PrintStream originalOut = System.out;
    private ByteArrayOutputStream captured;

    @BeforeEach
    void setUp() {
        taskList = new TaskList(List.of());
        handler = new CommandHandler(taskList);

        captured = new ByteArrayOutputStream();
        System.setOut(new PrintStream(captured));
    }

    @AfterEach
    void tearDown() {
        System.setOut(originalOut);
    }

    @Test
    void handleAdd_validText_addsTaskToList() {
        handler.handleAdd("ring kalle");
        assertEquals(1, taskList.getAll().size());
        assertEquals("ring kalle", taskList.getAll().get(0).text());
    }

    @Test
    void handleAdd_validText_printConfirmation() {
        handler.handleAdd("ring kalle");
        assertTrue(captured.toString().contains("ring kalle"));
    }

    @Test
    void handleAdd_emptyText_doesNotAddTask() {
        handler.handleAdd("");
        assertTrue(taskList.getAll().isEmpty());
    }

    @Test
    void handleAdd_emptyText_printsError() {
        handler.handleAdd("");
        assertFalse(captured.toString().isBlank());
    }
}
