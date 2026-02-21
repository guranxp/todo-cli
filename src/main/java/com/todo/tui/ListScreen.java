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
import java.util.List;

@RequiredArgsConstructor
public class ListScreen {
    @NonNull private final TaskList       taskList;
    @NonNull private final TaskRepository repository;
             private final boolean        showAll;

    public void run() {
        resizeTerminalWindow();
        try {
            final Terminal terminal = new DefaultTerminalFactory()
                    .setInitialTerminalSize(new TerminalSize(ScreenRenderer.WIDTH, ScreenRenderer.HEIGHT))
                    .createTerminal();
            final Screen         screen   = new TerminalScreen(terminal);
            final ScreenRenderer renderer = new ScreenRenderer(taskList);
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

                    renderer.draw(screen, tasks, cursor, showTimestamps, scrollOffset);

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
                        final String text = addInline(screen, renderer, tasks);
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
                        final String edited = editInline(screen, renderer, tasks, cursor, t.text(), scrollOffset);
                        if (edited != null && !edited.isBlank()) {
                            taskList.updateText(taskList.getAll().indexOf(t), edited.trim());
                            repository.save(taskList);
                        }
                    } else if (type == KeyType.Character && key.getCharacter() == 't') {
                        showTimestamps = !showTimestamps;
                    } else if (type == KeyType.Character && key.isCtrlDown() && key.getCharacter() == 's') {
                        repository.save(taskList);
                        renderer.showSaved(screen, tasks, cursor, showTimestamps, scrollOffset);
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
                try { renderer.showError(screen, e.getMessage()); } catch (IOException ignored) {}
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
        final int available = ScreenRenderer.TASK_END - ScreenRenderer.TASK_START + 1;
        while (scrollOffset <= cursor) {
            int rows = 0;
            for (int i = scrollOffset; i < tasks.size(); i++) {
                rows += TextWrapper.wrapText(tasks.get(i).text(), ScreenRenderer.TEXT_WIDTH).size();
                if (i == cursor) return scrollOffset;
                if (rows >= available) break;
            }
            scrollOffset++;
        }
        return Math.min(scrollOffset, cursor);
    }

    private String addInline(final Screen screen, final ScreenRenderer renderer,
                              final List<Task> tasks) throws IOException {
        final StringBuilder buf = new StringBuilder();

        while (true) {
            screen.clear();
            final TextGraphics g = screen.newTextGraphics();

            renderer.drawHeader(g);

            final int nextRow = Math.min(
                    renderer.drawTaskList(g, tasks, -1, ScreenRenderer.TASK_START, false, 0, ScreenRenderer.TASK_END - 1),
                    ScreenRenderer.TASK_END);

            g.setForegroundColor(TextColor.ANSI.CYAN);
            g.putString(0, nextRow, "  [+] " + buf + "_");

            g.setForegroundColor(TextColor.ANSI.DEFAULT);
            g.putString(0, ScreenRenderer.HEIGHT - 3, "─".repeat(ScreenRenderer.WIDTH));
            renderer.drawHints(g, ScreenRenderer.HEIGHT - 2, "Enter", "confirm", "Esc", "cancel");
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

    private String editInline(final Screen screen, final ScreenRenderer renderer, final List<Task> tasks,
                               final int cursor, final String current, final int scrollOffset) throws IOException {
        final StringBuilder buf = new StringBuilder(current);

        while (true) {
            screen.clear();
            final TextGraphics g = screen.newTextGraphics();

            renderer.drawHeader(g);

            int row = ScreenRenderer.TASK_START;
            for (int i = scrollOffset; i < tasks.size(); i++) {
                if (row > ScreenRenderer.TASK_END) break;
                final String num = String.format("%2d", i + 1);
                if (i == cursor) {
                    final List<String> lines = TextWrapper.wrapText(buf.toString(), ScreenRenderer.TEXT_WIDTH);
                    g.setForegroundColor(TextColor.ANSI.CYAN);
                    g.putString(0, row, "▶ " + num + ". [e] " + lines.get(0) + (lines.size() == 1 ? "_" : ""));
                    for (int j = 1; j < lines.size(); j++) {
                        if (row + j > ScreenRenderer.TASK_END) break;
                        g.putString(0, row + j, "          " + lines.get(j) + (j == lines.size() - 1 ? "_" : ""));
                    }
                    row += lines.size();
                } else {
                    final Task         t     = tasks.get(i);
                    final List<String> lines = TextWrapper.wrapWithTimestamp(t.text(),
                            t.createdAt() != null ? "  " + ScreenRenderer.FMT.format(t.createdAt()) : "",
                            ScreenRenderer.TEXT_WIDTH);
                    g.setForegroundColor(t.done() ? TextColor.ANSI.GREEN : TextColor.ANSI.WHITE);
                    g.putString(0, row, "  " + num + ". " + (t.done() ? "[x] " : "[ ] ") + lines.get(0));
                    for (int j = 1; j < lines.size(); j++) {
                        if (row + j > ScreenRenderer.TASK_END) break;
                        g.putString(0, row + j, "          " + lines.get(j));
                    }
                    row += lines.size();
                }
            }

            g.setForegroundColor(TextColor.ANSI.DEFAULT);
            g.putString(0, ScreenRenderer.HEIGHT - 3, "─".repeat(ScreenRenderer.WIDTH));
            renderer.drawHints(g, ScreenRenderer.HEIGHT - 2, "Enter", "confirm", "Esc", "cancel");
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

    private static void resizeTerminalWindow() {
        if (System.getProperty("os.name", "").toLowerCase().contains("win")) return;
        System.out.print("\033[8;" + ScreenRenderer.HEIGHT + ";" + ScreenRenderer.WIDTH + "t");
        System.out.flush();
        try { Thread.sleep(100); } catch (InterruptedException ignored) {}
    }
}
