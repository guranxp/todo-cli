# Teknisk Kravspecifikation

> **Underhåll:** När funktionalitet läggs till eller ändras skall denna spec uppdateras om den berör något som dokumenteras här. Specen beskriver funktionalitet på en lagom abstrakt nivå — inte implementationsdetaljer.

## 1. Syfte

En terminalbaserad applikation (CLI) för personlig uppgiftshantering. Användaren kan snabbt anteckna saker de behöver göra eller kolla upp, direkt från kommandoraden.

**Plattform:** Java 21
**Gränssnitt:** Terminalen (TUI via Lanterna)

---

## 2. Interaktion & arbetsflöde

Appen startar direkt i ett interaktivt TUI-läge (ingen REPL-prompt). Terminalfönstret ändrar storlek automatiskt till 120×24 tecken vid start.

**Typiskt arbetsflöde:**
1. Starta appen (`java -jar todo.jar`)
2. Under dagen: `cmd+tab` till terminalfönstret → tryck `a` → skriv uppgiften → `Enter`
3. Uppgiften sparas direkt till disk
4. Navigera listan med piltangenter, markera klara med `Enter`, flytta med `Space+↑↓`

---

## 3. Funktionalitet

Appen har ett enda TUI-läge som alltid är aktivt:

| Tangent | Åtgärd |
|---|---|
| `↑` / `↓` | Flytta markören upp/ned i listan |
| `Enter` | Toggla markerad uppgift mellan öppen/klar |
| `a` | Lägg till ny uppgift (inline-input längst ner i listan) |
| `d` | Ta bort markerad uppgift |
| `e` | Redigera texten på markerad uppgift (inline, bekräfta med Enter) |
| `t` | Visa/dölj tidsstämplar (dolda som standard) |
| `Shift+↑/↓` | Flytta markerad uppgift uppåt/nedåt ett steg per knapptryckning |
| `Ctrl+S` | Spara manuellt |
| `q` / `Esc` | Avsluta appen (sparar automatiskt) |

Uppgifter vars text är längre än fönsterbredden bryts automatiskt och fortsätter på nästa rad.

---

## 4. Persistens

Uppgifterna sparas i **lokala JSON-filer** så att de finns kvar mellan sessioner.

- Aktiva uppgifter: `~/.todo/tasks.json`
- Raderade uppgifter (ej avklarade): `~/.todo/deleted.json`

**Sparstrategi:**
- Lägg till, redigera, toggle done/undone och radera sparas direkt
- Flytt av uppgifter sparas vid avslut (`q`/`Esc`) eller via `Ctrl+S`

Avklarade uppgifter som raderas sparas **inte** i `deleted.json`.

---

## 5. Sortering och ordning

Uppgifterna har en **manuellt styrd ordning**. Tryck `Shift+↑`/`Shift+↓` för att flytta markerad uppgift ett steg i taget. Markören följer med uppgiften. Ordningen persisteras i JSON-filen.

---

## 6. Byggverktyg & distribution

- **Byggverktyg:** Maven (`pom.xml`)
- **Java-version:** 21 (Temurin)
- Byggs till en körbar JAR med `mvn package`
- **JSON-bibliotek:** Jackson (`com.fasterxml.jackson.core`)
- **TUI-bibliotek:** Lanterna 3 (`com.googlecode.lanterna`)

---

## 7. Språk

All text i applikationens gränssnitt (etiketter, hjälptexter, bekräftelsemeddelanden, felmeddelanden) ska vara på **engelska**.

---

## 8. Utseende i terminalen

- Fönsterstorlek: 120×24 tecken
- Aktiv uppgift markeras med `▶`
- Avklarade uppgifter visas i grönt med `[x]`
- Öppna uppgifter visas i vitt med `[ ]`
- Hjälprad visas längst ner i fönstret

---

## 9. Arkitektur

### Paketstruktur

```
com.todo/
├── Main.java                  # Entry point
├── app/
│   └── TodoApp.java           # Startar TUI direkt
├── command/
│   ├── CommandParser.java     # Tolkar kommandon
│   └── CommandHandler.java    # Exekverar kommandon
├── model/
│   ├── Task.java              # Datamodell: id, text, done, createdAt
│   └── TaskList.java          # Hanterar add, delete, toggle, move
├── storage/
│   └── TaskRepository.java    # Läser/skriver tasks.json och deleted.json
├── tui/
│   └── ListScreen.java        # Lanterna-baserad TUI med tangenthantering
└── ui/
    └── Ansi.java              # ANSI-färgkonstanter
```
