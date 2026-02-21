package com.todo.app;

import com.todo.command.CommandHandler;
import com.todo.command.CommandParser;
import com.todo.model.TaskList;
import com.todo.storage.TaskRepository;
import com.todo.tui.ListScreen;
import com.todo.ui.Ansi;
import lombok.AccessLevel;
import lombok.NonNull;
import lombok.RequiredArgsConstructor;

import java.util.Scanner;

@RequiredArgsConstructor(access = AccessLevel.PRIVATE)
public class TodoApp {
    @NonNull private final TaskList       taskList;
    @NonNull private final TaskRepository repository;
    @NonNull private final CommandParser  parser;
    @NonNull private final CommandHandler handler;

    public static TodoApp create() {
        final TaskRepository repository = new TaskRepository();
        final TaskList       taskList   = repository.load();
        final CommandParser  parser     = new CommandParser();
        final CommandHandler handler    = new CommandHandler(taskList);
        return new TodoApp(taskList, repository, parser, handler);
    }

    public void run() {
        System.out.println(Ansi.CYAN + "todo-cli · add <text> | list | list all | exit" + Ansi.RESET);

        try (final Scanner scanner = new Scanner(System.in)) {
            while (true) {
                System.out.print("> ");
                if (!scanner.hasNextLine()) break;
                final String input = scanner.nextLine();

                final CommandParser.ParsedCommand cmd = parser.parse(input);

                switch (cmd.command()) {
                    case ADD      -> handler.handleAdd(cmd.args());
                    case LIST     -> new ListScreen(taskList, repository, false).run();
                    case LIST_ALL -> new ListScreen(taskList, repository, true).run();
                    case EXIT     -> {
                        System.out.println(Ansi.CYAN + "Goodbye." + Ansi.RESET);
                        return;
                    }
                    case UNKNOWN  -> {
                        if (!cmd.args().isEmpty()) {
                            System.out.println(Ansi.RED + "Unknown command: " + cmd.args() + Ansi.RESET);
                        }
                    }
                }
            }
        }
    }
}
