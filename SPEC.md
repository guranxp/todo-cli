# Teknisk Kravspecifikation

## 1. Syfte

En terminalbaserad applikation (CLI) för personlig uppgiftshantering. Användaren kan snabbt anteckna saker de behöver göra eller kolla upp, direkt från kommandoraden.

**Plattform:** Java 21
**Gränssnitt:** Terminalen (CLI)

---

## 2. Interaktion & arbetsflöde

Appen körs som ett **interaktivt skal** — startas en gång på morgonen och hålls öppen hela dagen i ett terminalfönster.

**Typiskt arbetsflöde:**
1. Starta appen på morgonen (`java -jar todo.jar`)
2. Under dagen: `cmd+tab` till terminalfönstret → skriv `add ring kalle` → `Enter` → byt tillbaka
3. `add` ger **minimal feedback** (t.ex. `✔ Tillagd: ring kalle`) — listan visas inte automatiskt
4. När man vill se uppgifterna: skriv `list` eller `list all`
5. Därifrån kan man markera klara, ta bort, flytta uppgifter i ordning

**Exempelflöde:**
```
> add ring kalle
✔ Tillagd: ring kalle

> add kolla nytt API
✔ Tillagd: kolla nytt API

> list
  [ ] ring kalle          2026-02-21 09:14
▶ [ ] kolla nytt API      2026-02-21 10:02

  (↑↓ navigera  Enter toggla  d radera  e redigera  K/J flytta  q avsluta)

> _
```

---

## 3. Funktionalitet

Appen har två lägen:

### 3.1 Kommandoläge (alltid aktivt)
Användaren skriver kommandon i en prompt:

| Kommando | Beskrivning |
|---|---|
| `add <text>` | Lägger till ny uppgift, ger kort bekräftelse |
| `list` | Öppnar listläget med enbart öppna uppgifter |
| `list all` | Öppnar listläget med alla uppgifter (inkl. avklarade) |
| `exit` | Avslutar programmet |

### 3.2 Listläge (interaktiv TUI-navigering)
Aktiveras av `list` / `list all`. Uppgifterna visas med en markör och användaren navigerar med tangentbordet:

| Tangent | Åtgärd |
|---|---|
| `↑` / `↓` | Flytta markören upp/ned i listan |
| `Enter` | Toggla markerad uppgift mellan öppen/klar |
| `d` | Ta bort markerad uppgift |
| `e` | Redigera texten på markerad uppgift (inline, bekräfta med Enter eller Ctrl+S) |
| `Space+↑` | Flytta markerad uppgift ett steg uppåt i ordningen |
| `Space+↓` | Flytta markerad uppgift ett steg nedåt i ordningen |
| `Ctrl+S` | Spara alla ändringar till disk |
| `q` / `Esc` | Lämna listläget, återgå till kommandoläget |

---

## 4. Persistens

Uppgifterna sparas i en **lokal JSON-fil** så att de finns kvar mellan sessioner.

- Standardsökväg: `~/.todo/tasks.json` (hemkatalog, dold mapp)
- **Sparstrategi:** Ändringar hålls i minnet och skrivs till disk **endast när användaren trycker `Ctrl+S`**. Osparade ändringar går förlorade om appen avslutas oväntat — detta är avsiktligt beteende.
- `Ctrl+S` fungerar i **båda lägena** (kommandoläget och listläget)
- Format:
```json
[
  { "id": 1, "text": "Köp mjölk", "done": false, "createdAt": "2026-02-21T10:00:00" },
  { "id": 2, "text": "Kolla upp nytt API", "done": true, "createdAt": "2026-02-21T10:01:00" }
]
```

---

## 5. Sortering och ordning

Uppgifterna har en **manuellt styrd ordning** som styrs i listläget genom att hålla `Space` och trycka `↑`/`↓`. Markören följer med uppgiften när den flyttas. Ordningen persisteras i JSON-filen — arrayens ordning speglar visningsordningen direkt.

---

## 6. Byggverktyg & distribution

- **Byggverktyg:** Maven (`pom.xml`)
- **Java-version:** 21 (Temurin)
- Byggs till en körbar JAR med `mvn package`
- **JSON-bibliotek:** Jackson (`com.fasterxml.jackson.core`)
- **TUI-bibliotek:** Lanterna 3 (`com.googlecode.lanterna`) — hanterar piltangenter, raw terminal-mode och terminalstorlek
- Inga andra externa beroenden

---

## 7. Utseende i terminalen

Appen använder **ANSI-färgkoder** (inbyggt i Java, inga extra bibliotek) för att göra utdata tydligare:

| Element | Färg |
|---|---|
| Öppna uppgifter | Vit/standard |
| Avklarade uppgifter (`done`) | Grön + genomstruken text |
| Felmeddelanden | Röd |
| Bekräftelsemeddelanden | Cyan/grön |

---

## 8. Arkitektur

### Paketstruktur

```
com.todo/
├── Main.java                  # Entry point – skapar och startar TodoApp
├── app/
│   └── TodoApp.java           # Huvudloop – växlar mellan kommandoläge och listläge
├── command/
│   ├── CommandParser.java     # Tolkar råinput ("add ring kalle") → Command + args
│   └── CommandHandler.java    # Exekverar kommandon mot TaskList och TaskRepository
├── model/
│   ├── Task.java              # Datamodell: id, text, done, createdAt
│   └── TaskList.java          # Samling av Task – hanterar add, delete, toggle, move
├── storage/
│   └── TaskRepository.java    # Läser/skriver ~/.todo/tasks.json via Jackson
├── tui/
│   └── ListScreen.java        # Lanterna-baserad interaktiv listvy med tangenthantering
└── ui/
    └── Ansi.java              # ANSI-färgkonstanter och hjälpmetoder
```

### Nyckelklasser

**`Task`** — Enkel datamodell (record eller POJO):
```java
long id, String text, boolean done, LocalDateTime createdAt
```

**`TaskList`** — Håller `List<Task>` i minnet med metoderna `add`, `delete`, `toggleDone`, `moveUp`, `moveDown`. Denna lista är sanningskällan under en session.

**`TaskRepository`** — Ansvarar för all disk-I/O. Laddar listan vid start med `load()`, skriver hela listan vid `save()` (triggas av `Ctrl+S`). Använder Jackson `ObjectMapper`.

**`CommandParser`** — Tolkar en inmatad sträng till ett `Command`-enum (`ADD`, `LIST`, `LIST_ALL`, `EXIT`) plus eventuella argument. Returnerar ett `ParsedCommand`-objekt.

**`TodoApp`** — Kärnan i applikationen. Kör en oändlig loop i kommandoläget (läser rader via `Scanner`). När `list`/`list all` skrivs, delegerar till `ListScreen`. Lyssnar globalt på `Ctrl+S` för att trigga `TaskRepository.save()`.

**`ListScreen`** — Tar över terminalen med Lanterna (`DefaultTerminalFactory`). Ritar om listan efter varje tangenttryckning. Hanterar:
- `↑`/`↓` → flytta markör
- `Enter` → `TaskList.toggleDone()`
- `d` → `TaskList.delete()`
- `e` → inline redigering (byt till inputläge på den raden)
- `Space`+`↑`/`↓` → `TaskList.moveUp()` / `moveDown()`
- `Ctrl+S` → `TaskRepository.save()`
- `q`/`Esc` → återlämna kontrollen till `TodoApp`

**`Ansi`** — Konstanter som `RESET`, `GREEN`, `RED`, `STRIKETHROUGH` m.fl. för att färgsätta utdata i kommandoläget.