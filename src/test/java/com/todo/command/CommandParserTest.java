package com.todo.command;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import static com.todo.command.CommandParser.Command.*;
import static org.junit.jupiter.api.Assertions.*;

class CommandParserTest {

    private CommandParser parser;

    @BeforeEach
    void setUp() {
        parser = new CommandParser();
    }

    @Test
    void add_parsesTextAfterKeyword() {
        var result = parser.parse("add ring kalle");
        assertEquals(ADD, result.command());
        assertEquals("ring kalle", result.args());
    }

    @Test
    void add_trimsExtraWhitespace() {
        var result = parser.parse("add   städa   ");
        assertEquals(ADD, result.command());
        assertEquals("städa", result.args());
    }

    @Test
    void add_withNoText_returnsAddWithEmptyArgs() {
        var result = parser.parse("add ");
        assertEquals(ADD, result.command());
        assertEquals("", result.args());
    }

    @Test
    void list_returnsLIST() {
        assertEquals(LIST, parser.parse("list").command());
    }

    @Test
    void listAll_returnsLIST_ALL() {
        assertEquals(LIST_ALL, parser.parse("list all").command());
    }

    @Test
    void exit_returnsEXIT() {
        assertEquals(EXIT, parser.parse("exit").command());
    }

    @Test
    void quit_returnsEXIT() {
        assertEquals(EXIT, parser.parse("quit").command());
    }

    @Test
    void unknownCommand_returnsUNKNOWN() {
        var result = parser.parse("foobar");
        assertEquals(UNKNOWN, result.command());
        assertEquals("foobar", result.args());
    }

    @Test
    void blankInput_returnsUNKNOWN() {
        assertEquals(UNKNOWN, parser.parse("").command());
        assertEquals(UNKNOWN, parser.parse("   ").command());
    }

    @Test
    void nullInput_returnsUNKNOWN() {
        assertEquals(UNKNOWN, parser.parse(null).command());
    }

    @Test
    void commandsAreCaseInsensitive() {
        assertEquals(LIST,     parser.parse("LIST").command());
        assertEquals(LIST_ALL, parser.parse("LIST ALL").command());
        assertEquals(EXIT,     parser.parse("EXIT").command());
    }
}
