# todo-cli

A terminal-based personal task manager. Stays open all day in a terminal window — switch to it, add a task, switch back.

## Requirements

- Java 21
- macOS / Linux / Windows (Windows Terminal recommended)

## Build

```
mvn package
```

## Download

Download the latest `todo.jar` from [Releases](https://github.com/guranxp/todo-cli/releases/latest).

## Run

```
java -jar todo.jar
```

On macOS/Linux the terminal window resizes automatically to 120×24. On Windows, set your terminal window to at least 120×24 before launching.

## Keybindings

| Key | Action |
|---|---|
| `↑` / `↓` | Navigate |
| `Enter` | Toggle done/undone |
| `a` | Add task |
| `d` | Delete task |
| `e` | Edit task (confirm with Enter) |
| `t` | Toggle timestamps |
| `Shift+↑` / `Shift+↓` | Move task up/down |
| `Ctrl+S` | Save manually |
| `q` / `Esc` | Quit (auto-saves) |

## Data

Tasks are stored in `~/.todo/tasks.json`. Deleted tasks (that were not done) are archived to `~/.todo/deleted.json`.

Changes to add, edit, toggle and delete are saved immediately. Reordering is saved on quit or `Ctrl+S`.
