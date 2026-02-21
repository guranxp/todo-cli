package com.todo.tui;

import com.googlecode.lanterna.TerminalSize;
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
import com.todo.storage.StorageException;
import com.todo.storage.TaskRepository;
import lombok.NonNull;
import lombok.RequiredArgsConstructor;

import java.io.IOException;
import java.time.LocalDate;
import java.time.format.DateTimeFormatter;
import java.util.ArrayList;
import java.util.List;

@RequiredArgsConstructor
public class ListScreen {
    private static final DateTimeFormatter FMT        = DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm");
    private static final int               WIDTH      = 120;
    private static final int               HEIGHT     = 24;
    private static final int               TEXT_START = 10;  // "  99. [ ] " prefix width
    private static final int               TEXT_WIDTH = WIDTH - TEXT_START;
    private static final int               TASK_START = 3;   // first row for tasks
    private static final int               TASK_END   = HEIGHT - 4; // last row for tasks

    @NonNull private final TaskList       taskList;
    @NonNull private final TaskRepository repository;
             private final boolean        showAll;

    public void run() {
        resizeTerminalWindow();
        try {
            final Terminal terminal = new DefaultTerminalFactory()
                    .setInitialTerminalSize(new TerminalSize(WIDTH, HEIGHT))
                    .createTerminal();
            final Screen screen = new TerminalScreen(terminal);
            screen.startScreen();
            screen.setCursorPosition(null);

            int     cursor         = 0;
            int     scrollOffset   = 0;
            boolean showTimestamps = false;

            boolean saveError = false;
            try {
                while (true) {
                    final List<Task> tasks = showAll ? taskList.getAll() : taskList.getOpen();
                    if (!tasks.isEmpty() && cursor >= tasks.size()) cursor = tasks.size() - 1;
                    if (cursor < 0) cursor = 0;
                    scrollOffset = adjustScroll(tasks, cursor, scrollOffset);

                    draw(screen, tasks, cursor, showTimestamps, scrollOffset);

                    final KeyStroke key  = screen.readInput();
                    final KeyType   type = key.getKeyType();

                    if (type == KeyType.ArrowUp && key.isShiftDown() && !tasks.isEmpty() && cursor > 0) {
                        final Task t = tasks.get(cursor);
                        if (taskList.moveUp(taskList.getAll().indexOf(t))) cursor--;
                    } else if (type == KeyType.ArrowDown && key.isShiftDown() && !tasks.isEmpty() && cursor < tasks.size() - 1) {
                        final Task t = tasks.get(cursor);
                        if (taskList.moveDown(taskList.getAll().indexOf(t))) cursor++;
                    } else if (type == KeyType.ArrowUp) {
                        if (cursor > 0) cursor--;
                    } else if (type == KeyType.ArrowDown) {
                        if (cursor < tasks.size() - 1) cursor++;
                    } else if (type == KeyType.Enter && !tasks.isEmpty()) {
                        final Task t = tasks.get(cursor);
                        taskList.toggleDone(taskList.getAll().indexOf(t));
                        repository.save(taskList);
                    } else if (type == KeyType.Character && key.getCharacter() == 'a') {
                        final String text = addInline(screen, tasks);
                        if (text != null && !text.isBlank()) {
                            taskList.add(text.trim());
                            repository.save(taskList);
                            cursor = showAll ? taskList.getAll().size() - 1 : taskList.getOpen().size() - 1;
                        }
                    } else if (type == KeyType.Character && key.getCharacter() == 'd' && !tasks.isEmpty()) {
                        final Task t = tasks.get(cursor);
                        taskList.delete(taskList.getAll().indexOf(t));
                        if (!t.done()) repository.saveDeleted(t);
                        repository.save(taskList);
                        if (cursor > 0 && cursor >= tasks.size() - 1) cursor--;
                    } else if (type == KeyType.Character && key.getCharacter() == 'e' && !tasks.isEmpty()) {
                        final Task   t      = tasks.get(cursor);
                        final String edited = editInline(screen, tasks, cursor, t.text(), scrollOffset);
                        if (edited != null && !edited.isBlank()) {
                            taskList.updateText(taskList.getAll().indexOf(t), edited.trim());
                            repository.save(taskList);
                        }
                    } else if (type == KeyType.Character && key.getCharacter() == 't') {
                        showTimestamps = !showTimestamps;
                    } else if (type == KeyType.Character && key.isCtrlDown() && key.getCharacter() == 's') {
                        repository.save(taskList);
                        showSaved(screen, tasks, cursor, showTimestamps, scrollOffset);
                    } else if (type == KeyType.Character && key.getCharacter() == 'q') {
                        break;
                    } else if (type == KeyType.Escape) {
                        break;
                    } else if (type == KeyType.EOF) {
                        break;
                    }
                }
            } catch (StorageException e) {
                saveError = true;
                try { showError(screen, e.getMessage()); } catch (IOException ignored) {}
            } finally {
                screen.stopScreen();
                terminal.close();
                if (!saveError) {
                    try { repository.save(taskList); } catch (StorageException ignored) {}
                }
            }
        } catch (IOException e) {
            System.err.println("TUI error: " + e.getMessage());
        }
    }

    private int adjustScroll(final List<Task> tasks, final int cursor, int scrollOffset) {
        if (tasks.isEmpty()) return 0;
        if (cursor < scrollOffset) return cursor;
        final int available = TASK_END - TASK_START + 1;
        while (scrollOffset <= cursor) {
            int rows = 0;
            for (int i = scrollOffset; i < tasks.size(); i++) {
                rows += wrapText(tasks.get(i).text(), TEXT_WIDTH).size();
                if (i == cursor) return scrollOffset;
                if (rows >= available) break;
            }
            scrollOffset++;
        }
        return Math.min(scrollOffset, cursor);
    }

    private void drawHeader(final TextGraphics g) {
        final long   done      = taskList.getAll().stream().filter(Task::done).count();
        final long   remaining = taskList.getAll().size() - done;
        final String date      = LocalDate.now().format(DateTimeFormatter.ofPattern("yyyy-MM-dd"));
        final String right     = done + " done · " + remaining + " remaining · " + date;

        g.setForegroundColor(TextColor.ANSI.CYAN);
        g.putString(0, 1, " Todo");
        g.setForegroundColor(TextColor.ANSI.WHITE);
        g.putString(WIDTH - right.length() - 1, 1, right);
        g.setForegroundColor(TextColor.ANSI.DEFAULT);
        g.putString(0, 2, "─".repeat(WIDTH));
    }

    // Draws the task list with scrolling and line numbers. Returns the next free row.
    private int drawTaskList(final TextGraphics g, final List<Task> tasks, final int cursor,
                              final int startRow, final boolean showTimestamps, final int scrollOffset) {
        return drawTaskList(g, tasks, cursor, startRow, showTimestamps, scrollOffset, TASK_END);
    }

    private int drawTaskList(final TextGraphics g, final List<Task> tasks, final int cursor,
                              final int startRow, final boolean showTimestamps, final int scrollOffset, final int maxRow) {
        int row = startRow;
        for (int i = scrollOffset; i < tasks.size(); i++) {
            if (row > maxRow) break;
            final Task         t      = tasks.get(i);
            final boolean      active = (i == cursor);
            final String       prefix = (active ? "▶ " : "  ") + String.format("%2d", i + 1) + ". " + (t.done() ? "[x] " : "[ ] ");
            final String       ts     = showTimestamps && t.createdAt() != null ? "  " + FMT.format(t.createdAt()) : "";
            final List<String> lines  = wrapWithTimestamp(t.text(), ts);

            g.setForegroundColor(t.done() ? TextColor.ANSI.GREEN : TextColor.ANSI.WHITE);
            g.putString(0, row, prefix + lines.get(0));
            for (int j = 1; j < lines.size(); j++) {
                if (row + j > maxRow) break;
                g.putString(0, row + j, "          " + lines.get(j));
            }
            row += lines.size();
        }
        return row;
    }

    private void draw(final Screen screen, final List<Task> tasks, final int cursor,
                      final boolean showTimestamps, final int scrollOffset) throws IOException {
        screen.clear();
        final TextGraphics g = screen.newTextGraphics();

        drawHeader(g);

        if (tasks.isEmpty()) {
            g.setForegroundColor(TextColor.ANSI.WHITE);
            g.putString(0, TASK_START, "  (No tasks – press 'a' to add one)");
        } else {
            drawTaskList(g, tasks, cursor, TASK_START, showTimestamps, scrollOffset);
        }

        g.setForegroundColor(TextColor.ANSI.DEFAULT);
        g.putString(0, HEIGHT - 3, "─".repeat(WIDTH));
        drawHints(g, HEIGHT - 2, "↑↓", "navigate", "Enter", "toggle", "a", "add", "d", "delete", "e", "edit", "t", "timestamps", "Shift+↑↓", "move", "q", "quit");

        screen.refresh();
    }

    private String addInline(final Screen screen, final List<Task> tasks) throws IOException {
        final StringBuilder buf = new StringBuilder();

        while (true) {
            screen.clear();
            final TextGraphics g = screen.newTextGraphics();

            drawHeader(g);

            final int nextRow = Math.min(drawTaskList(g, tasks, -1, TASK_START, false, 0, TASK_END - 1), TASK_END);

            g.setForegroundColor(TextColor.ANSI.CYAN);
            g.putString(0, nextRow, "  [+] " + buf + "_");

            g.setForegroundColor(TextColor.ANSI.DEFAULT);
            g.putString(0, HEIGHT - 3, "─".repeat(WIDTH));
            drawHints(g, HEIGHT - 2, "Enter", "confirm", "Esc", "cancel");
            screen.refresh();

            final KeyStroke key  = screen.readInput();
            final KeyType   type = key.getKeyType();

            if (type == KeyType.Enter) {
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

    private String editInline(final Screen screen, final List<Task> tasks, final int cursor,
                               final String current, final int scrollOffset) throws IOException {
        final StringBuilder buf = new StringBuilder(current);

        while (true) {
            screen.clear();
            final TextGraphics g = screen.newTextGraphics();

            drawHeader(g);

            int row = TASK_START;
            for (int i = scrollOffset; i < tasks.size(); i++) {
                if (row > TASK_END) break;
                final String num = String.format("%2d", i + 1);
                if (i == cursor) {
                    final List<String> lines = wrapText(buf.toString(), TEXT_WIDTH);
                    g.setForegroundColor(TextColor.ANSI.CYAN);
                    g.putString(0, row, "▶ " + num + ". [e] " + lines.get(0) + (lines.size() == 1 ? "_" : ""));
                    for (int j = 1; j < lines.size(); j++) {
                        if (row + j > TASK_END) break;
                        g.putString(0, row + j, "          " + lines.get(j) + (j == lines.size() - 1 ? "_" : ""));
                    }
                    row += lines.size();
                } else {
                    final Task         t     = tasks.get(i);
                    final List<String> lines = wrapWithTimestamp(t.text(),
                            t.createdAt() != null ? "  " + FMT.format(t.createdAt()) : "");
                    g.setForegroundColor(t.done() ? TextColor.ANSI.GREEN : TextColor.ANSI.WHITE);
                    g.putString(0, row, "  " + num + ". " + (t.done() ? "[x] " : "[ ] ") + lines.get(0));
                    for (int j = 1; j < lines.size(); j++) {
                        if (row + j > TASK_END) break;
                        g.putString(0, row + j, "          " + lines.get(j));
                    }
                    row += lines.size();
                }
            }

            g.setForegroundColor(TextColor.ANSI.DEFAULT);
            g.putString(0, HEIGHT - 3, "─".repeat(WIDTH));
            drawHints(g, HEIGHT - 2, "Enter", "confirm", "Esc", "cancel");
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

    private void showSaved(final Screen screen, final List<Task> tasks, final int cursor,
                            final boolean showTimestamps, final int scrollOffset) throws IOException {
        screen.clear();
        final TextGraphics g = screen.newTextGraphics();
        g.setForegroundColor(TextColor.ANSI.GREEN);
        g.putString(0, 0, "✔ Saved.");
        screen.refresh();
        try { Thread.sleep(700); } catch (InterruptedException ignored) {}
        draw(screen, tasks, cursor, showTimestamps, scrollOffset);
    }

    private void showError(final Screen screen, final String message) throws IOException {
        screen.clear();
        final TextGraphics g = screen.newTextGraphics();
        g.setForegroundColor(TextColor.ANSI.RED);
        g.putString(0, 0, " Error: " + message);
        g.setForegroundColor(TextColor.ANSI.YELLOW);
        g.putString(0, 2, " Press any key to exit.");
        screen.refresh();
        try { screen.readInput(); } catch (IOException ignored) {}
    }

    // Renders key-description pairs with │ separators: keys in yellow, descriptions in white.
    private void drawHints(final TextGraphics g, final int row, final String... pairs) {
        int col = 2;
        for (int i = 0; i < pairs.length; i += 2) {
            if (i > 0) {
                g.setForegroundColor(TextColor.ANSI.DEFAULT);
                g.putString(col, row, " │ ");
                col += 3;
            }
            g.setForegroundColor(TextColor.ANSI.YELLOW);
            g.putString(col, row, pairs[i]);
            col += pairs[i].length();
            g.setForegroundColor(TextColor.ANSI.WHITE);
            g.putString(col, row, " " + pairs[i + 1]);
            col += 1 + pairs[i + 1].length();
        }
    }

    // Wraps text + timestamp: text wraps at TEXT_WIDTH, timestamp appended to last line if it fits.
    private List<String> wrapWithTimestamp(final String text, final String ts) {
        final List<String> lines    = wrapText(text, TEXT_WIDTH);
        final String       lastLine = lines.get(lines.size() - 1);
        if ((lastLine + ts).length() <= TEXT_WIDTH) {
            lines.set(lines.size() - 1, lastLine + ts);
        } else if (!ts.isBlank()) {
            lines.add(ts.strip());
        }
        return lines;
    }

    // Wraps text into lines of at most maxWidth characters, breaking at word boundaries.
    private List<String> wrapText(String text, final int maxWidth) {
        final List<String> lines = new ArrayList<>();
        while (text.length() > maxWidth) {
            int breakAt   = maxWidth;
            int lastSpace = text.lastIndexOf(' ', maxWidth);
            if (lastSpace > maxWidth / 2) breakAt = lastSpace;
            lines.add(text.substring(0, breakAt).stripTrailing());
            text = text.substring(breakAt).stripLeading();
        }
        lines.add(text);
        return lines;
    }

    private static void resizeTerminalWindow() {
        // Resize escape sequence only works on xterm-compatible terminals (macOS/Linux)
        if (System.getProperty("os.name", "").toLowerCase().contains("win")) return;
        System.out.print("\033[8;" + HEIGHT + ";" + WIDTH + "t");
        System.out.flush();
        try { Thread.sleep(100); } catch (InterruptedException ignored) {}
    }
}
