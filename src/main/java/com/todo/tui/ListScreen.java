package com.todo.tui;

import com.googlecode.lanterna.TextColor;
import com.googlecode.lanterna.graphics.TextGraphics;
import com.googlecode.lanterna.input.KeyStroke;
import com.googlecode.lanterna.input.KeyType;
import com.googlecode.lanterna.screen.Screen;
import com.googlecode.lanterna.screen.TerminalScreen;
import com.googlecode.lanterna.terminal.DefaultTerminalFactory;
import com.googlecode.lanterna.terminal.Terminal;
import com.todo.model.Task;
import com.todo.model.TaskList;
import com.todo.storage.TaskRepository;
import lombok.NonNull;
import lombok.RequiredArgsConstructor;

import java.io.IOException;
import java.time.format.DateTimeFormatter;
import java.util.List;

@RequiredArgsConstructor
public class ListScreen {
    private static final DateTimeFormatter FMT = DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm");

    @NonNull private final TaskList       taskList;
    @NonNull private final TaskRepository repository;
             private final boolean        showAll;

    public void run() {
        try {
            final Terminal terminal = new DefaultTerminalFactory().createTerminal();
            final Screen   screen   = new TerminalScreen(terminal);
            screen.startScreen();
            screen.setCursorPosition(null);

            int     cursor    = 0;
            boolean spaceHeld = false;

            try {
                while (true) {
                    final List<Task> tasks = showAll ? taskList.getAll() : taskList.getOpen();
                    if (!tasks.isEmpty() && cursor >= tasks.size()) cursor = tasks.size() - 1;
                    if (cursor < 0) cursor = 0;

                    draw(screen, tasks, cursor);

                    final KeyStroke key  = screen.readInput();
                    final KeyType   type = key.getKeyType();

                    // Space — prime for move-mode
                    if (type == KeyType.Character && key.getCharacter() == ' ') {
                        spaceHeld = true;
                        continue;
                    }

                    // Space + arrow → move task
                    if (spaceHeld) {
                        if (type == KeyType.ArrowUp && cursor > 0) {
                            final Task t = tasks.get(cursor);
                            if (taskList.moveUp(taskList.getAll().indexOf(t))) cursor--;
                        } else if (type == KeyType.ArrowDown && cursor < tasks.size() - 1) {
                            final Task t = tasks.get(cursor);
                            if (taskList.moveDown(taskList.getAll().indexOf(t))) cursor++;
                        }
                        spaceHeld = false;
                        continue;
                    }
                    spaceHeld = false;

                    if (type == KeyType.ArrowUp) {
                        if (cursor > 0) cursor--;
                    } else if (type == KeyType.ArrowDown) {
                        if (cursor < tasks.size() - 1) cursor++;
                    } else if (type == KeyType.Enter && !tasks.isEmpty()) {
                        final Task t = tasks.get(cursor);
                        taskList.toggleDone(taskList.getAll().indexOf(t));
                    } else if (type == KeyType.Character && key.getCharacter() == 'd' && !tasks.isEmpty()) {
                        final Task t = tasks.get(cursor);
                        taskList.delete(taskList.getAll().indexOf(t));
                        if (!t.done()) repository.saveDeleted(t);
                        if (cursor > 0 && cursor >= tasks.size() - 1) cursor--;
                    } else if (type == KeyType.Character && key.getCharacter() == 'e' && !tasks.isEmpty()) {
                        final Task   t      = tasks.get(cursor);
                        final String edited = editInline(screen, tasks, cursor, t.text());
                        if (edited != null && !edited.isBlank()) {
                            taskList.updateText(taskList.getAll().indexOf(t), edited.trim());
                        }
                    } else if (type == KeyType.Character && key.isCtrlDown() && key.getCharacter() == 's') {
                        repository.save(taskList);
                        showSaved(screen, tasks, cursor);
                    } else if (type == KeyType.Character && key.getCharacter() == 'q') {
                        break;
                    } else if (type == KeyType.Escape) {
                        break;
                    } else if (type == KeyType.EOF) {
                        break;
                    }
                }
            } finally {
                screen.stopScreen();
                terminal.close();
            }
        } catch (IOException e) {
            System.err.println("TUI error: " + e.getMessage());
        }
    }

    private void draw(final Screen screen, final List<Task> tasks, final int cursor) throws IOException {
        screen.clear();
        final TextGraphics g = screen.newTextGraphics();

        if (tasks.isEmpty()) {
            g.setForegroundColor(TextColor.ANSI.WHITE);
            g.putString(0, 0, "  (No tasks)");
        } else {
            for (int i = 0; i < tasks.size(); i++) {
                final Task   t        = tasks.get(i);
                final String prefix   = (i == cursor) ? "▶ " : "  ";
                final String checkbox = t.done() ? "[x] " : "[ ] ";
                final String ts       = t.createdAt() != null ? "  " + FMT.format(t.createdAt()) : "";
                final String line     = prefix + checkbox + t.text() + ts;

                g.setForegroundColor(t.done() ? TextColor.ANSI.GREEN : TextColor.ANSI.WHITE);
                g.putString(0, i, line);
            }
        }

        final int helpRow = Math.max(tasks.size() + 1, 2);
        g.setForegroundColor(TextColor.ANSI.YELLOW);
        g.putString(0, helpRow, "  (↑↓ navigate  Enter toggle  d delete  e edit  Space+↑↓ move  Ctrl+S save  q quit)");

        screen.refresh();
    }

    private String editInline(final Screen screen, final List<Task> tasks, final int cursor, final String current) throws IOException {
        final StringBuilder buf = new StringBuilder(current);

        while (true) {
            screen.clear();
            final TextGraphics g = screen.newTextGraphics();

            for (int i = 0; i < tasks.size(); i++) {
                if (i == cursor) {
                    g.setForegroundColor(TextColor.ANSI.CYAN);
                    g.putString(0, i, "▶ [e] " + buf + "_");
                } else {
                    final Task t = tasks.get(i);
                    g.setForegroundColor(t.done() ? TextColor.ANSI.GREEN : TextColor.ANSI.WHITE);
                    g.putString(0, i, "  " + (t.done() ? "[x] " : "[ ] ") + t.text());
                }
            }
            g.setForegroundColor(TextColor.ANSI.YELLOW);
            g.putString(0, tasks.size() + 1, "  (Enter/Ctrl+S confirm  Esc cancel)");
            screen.refresh();

            final KeyStroke key  = screen.readInput();
            final KeyType   type = key.getKeyType();

            if (type == KeyType.Enter) {
                return buf.toString();
            } else if (type == KeyType.Character && key.isCtrlDown() && key.getCharacter() == 's') {
                return buf.toString();
            } else if (type == KeyType.Escape) {
                return null;
            } else if (type == KeyType.Backspace) {
                if (!buf.isEmpty()) buf.deleteCharAt(buf.length() - 1);
            } else if (type == KeyType.Character && !key.isCtrlDown()) {
                buf.append(key.getCharacter());
            }
        }
    }

    private void showSaved(final Screen screen, final List<Task> tasks, final int cursor) throws IOException {
        screen.clear();
        final TextGraphics g = screen.newTextGraphics();
        g.setForegroundColor(TextColor.ANSI.GREEN);
        g.putString(0, 0, "✔ Saved.");
        screen.refresh();
        try { Thread.sleep(700); } catch (InterruptedException ignored) {}
        draw(screen, tasks, cursor);
    }
}
