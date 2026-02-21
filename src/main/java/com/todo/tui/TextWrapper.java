package com.todo.tui;

import java.util.ArrayList;
import java.util.List;

public class TextWrapper {
    private TextWrapper() {}

    public static List<String> wrapText(String text, final int maxWidth) {
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

    public static List<String> wrapWithTimestamp(final String text, final String ts, final int maxWidth) {
        final List<String> lines    = wrapText(text, maxWidth);
        final String       lastLine = lines.get(lines.size() - 1);
        if ((lastLine + ts).length() <= maxWidth) {
            lines.set(lines.size() - 1, lastLine + ts);
        } else if (!ts.isBlank()) {
            lines.add(ts.strip());
        }
        return lines;
    }
}
