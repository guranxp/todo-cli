package com.todo.command;

import com.todo.model.Task;
import com.todo.model.TaskList;
import com.todo.ui.Ansi;
import lombok.NonNull;
import lombok.RequiredArgsConstructor;

@RequiredArgsConstructor
public class CommandHandler {
    @NonNull private final TaskList taskList;

    public void handleAdd(@NonNull final String text) {
        if (text.isEmpty()) {
            System.out.println(Ansi.RED + "Please provide task text." + Ansi.RESET);
            return;
        }
        final Task task = taskList.add(text);
        System.out.println(Ansi.CYAN + "✔ Added: " + task.text() + Ansi.RESET);
    }
}
