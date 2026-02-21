package com.todo.command;

public class CommandParser {

    public enum Command {
        ADD, LIST, LIST_ALL, EXIT, UNKNOWN
    }

    public record ParsedCommand(Command command, String args) {}

    public ParsedCommand parse(final String input) {
        if (input == null || input.isBlank()) {
            return new ParsedCommand(Command.UNKNOWN, "");
        }
        final String trimmed = input.trim();
        final String lower   = trimmed.toLowerCase();

        if (lower.equals("exit") || lower.equals("quit")) {
            return new ParsedCommand(Command.EXIT, "");
        }
        if (lower.equals("list all")) {
            return new ParsedCommand(Command.LIST_ALL, "");
        }
        if (lower.equals("list")) {
            return new ParsedCommand(Command.LIST, "");
        }
        if (lower.startsWith("add ")) {
            return new ParsedCommand(Command.ADD, trimmed.substring(4).trim());
        }
        if (lower.equals("add")) {
            return new ParsedCommand(Command.ADD, "");
        }
        return new ParsedCommand(Command.UNKNOWN, trimmed);
    }
}
