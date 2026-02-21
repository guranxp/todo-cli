package com.todo.tui;

import com.googlecode.lanterna.TextColor;
import com.googlecode.lanterna.graphics.TextGraphics;
import com.googlecode.lanterna.screen.Screen;
import com.todo.model.Task;
import com.todo.model.TaskList;
import lombok.NonNull;
import lombok.RequiredArgsConstructor;

import java.io.IOException;
import java.time.LocalDate;
import java.time.format.DateTimeFormatter;
import java.util.List;

@RequiredArgsConstructor
class ScreenRenderer {
    static final int               WIDTH      = 120;
    static final int               HEIGHT     = 24;
    static final int               TEXT_START = 10;
    static final int               TEXT_WIDTH = WIDTH - TEXT_START;
    static final int               TASK_START = 3;
    static final int               TASK_END   = HEIGHT - 4;
    static final DateTimeFormatter FMT        = DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm");

    @NonNull private final TaskList taskList;

    void drawHeader(final TextGraphics g) {
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

    int drawTaskList(final TextGraphics g, final List<Task> tasks, final int cursor,
                     final int startRow, final boolean showTimestamps, final int scrollOffset) {
        return drawTaskList(g, tasks, cursor, startRow, showTimestamps, scrollOffset, TASK_END);
    }

    int drawTaskList(final TextGraphics g, final List<Task> tasks, final int cursor,
                     final int startRow, final boolean showTimestamps, final int scrollOffset, final int maxRow) {
        int row = startRow;
        for (int i = scrollOffset; i < tasks.size(); i++) {
            if (row > maxRow) break;
            final Task         t      = tasks.get(i);
            final boolean      active = (i == cursor);
            final String       prefix = (active ? "▶ " : "  ") + String.format("%2d", i + 1) + ". " + (t.done() ? "[x] " : "[ ] ");
            final String       ts     = showTimestamps && t.createdAt() != null ? "  " + FMT.format(t.createdAt()) : "";
            final List<String> lines  = TextWrapper.wrapWithTimestamp(t.text(), ts, TEXT_WIDTH);

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

    void draw(final Screen screen, final List<Task> tasks, final int cursor,
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

    void showSaved(final Screen screen, final List<Task> tasks, final int cursor,
                   final boolean showTimestamps, final int scrollOffset) throws IOException {
        screen.clear();
        final TextGraphics g = screen.newTextGraphics();
        g.setForegroundColor(TextColor.ANSI.GREEN);
        g.putString(0, 0, "✔ Saved.");
        screen.refresh();
        try { Thread.sleep(700); } catch (InterruptedException ignored) {}
        draw(screen, tasks, cursor, showTimestamps, scrollOffset);
    }

    void showError(final Screen screen, final String message) throws IOException {
        screen.clear();
        final TextGraphics g = screen.newTextGraphics();
        g.setForegroundColor(TextColor.ANSI.RED);
        g.putString(0, 0, " Error: " + message);
        g.setForegroundColor(TextColor.ANSI.YELLOW);
        g.putString(0, 2, " Press any key to exit.");
        screen.refresh();
        try { screen.readInput(); } catch (IOException ignored) {}
    }

    void drawHints(final TextGraphics g, final int row, final String... pairs) {
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
}
