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

### macOS / Linux

```
java -jar todo.jar
```

The terminal window resizes automatically to 120×24.

### Windows

Open **Windows Terminal** and run:

```powershell
irm https://raw.githubusercontent.com/guranxp/todo-cli/main/setup.ps1 | iex
```

This installs Java 21 (if needed) and sets up todo-cli in `%USERPROFILE%\todo\` automatically.

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

| Platform | Location |
|---|---|
| macOS / Linux | `~/.todo/` |
| Windows | `%USERPROFILE%\.todo\` |

- `tasks.json` — active tasks
- `deleted.json` — deleted tasks that were not done

Changes to add, edit, toggle and delete are saved immediately. Reordering is saved on quit or `Ctrl+S`.
