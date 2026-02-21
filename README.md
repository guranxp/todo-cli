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

Open **Windows Terminal** (press `Win` → type "Terminal" → Enter) and run:

```powershell
powershell -ExecutionPolicy Bypass -Command "irm https://raw.githubusercontent.com/guranxp/todo-cli/main/setup.ps1 | iex"
```

This handles everything automatically:
- Bypasses PowerShell execution policy
- Installs Java 21 per-user via winget (no admin rights needed)
- Downloads `todo.jar` to `%USERPROFILE%\todo\`
- Creates `todo.bat` that sets the window size and launches the app

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
