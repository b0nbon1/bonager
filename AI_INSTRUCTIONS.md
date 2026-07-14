# AI_INSTRUCTIONS.md — Bonager Navigation Guide

> Start here. This file is the single source of truth for AI agents working in this repo.
> Every code change must include an update to the relevant section of this file.

## Related Docs

| File | Purpose |
|------|---------|
| [CLAUDE.md](CLAUDE.md) | Claude Code entry point — loads this file and AGENTS.md |
| [AGENTS.md](AGENTS.md) | Platform summary and build command |
| [README.md](README.md) | Feature overview for humans |

---

## Quick Start

**Platform:** Native Android — Kotlin + Jetpack Compose, API 36 target  
**Entry point:** [android/app/src/main/java/com/bonvic/bonager/MainActivity.kt](android/app/src/main/java/com/bonvic/bonager/MainActivity.kt)  
**Build & verify (run from `android/` directory):**
```
./gradlew testDebugUnitTest assembleDebug lintDebug
```
**Debug APK output:** `android/app/build/outputs/apk/debug/app-debug.apk`  
**Package ID:** `com.bonvic.bonager`  
**Database:** Local SQLite at `bonager.db` (11 tables, no network)

---

## Working Tree

```
Bonager/
├── AI_INSTRUCTIONS.md          ← you are here (update on every change)
├── CLAUDE.md                   ← Claude Code entry (loads AGENTS.md + AI_INSTRUCTIONS.md)
├── AGENTS.md                   ← build command & platform summary
├── README.md                   ← human-facing feature overview
└── android/
    └── app/src/main/java/com/bonvic/bonager/
        ├── MainActivity.kt                 ← app entry: edge-to-edge, notifications, BonagerApp()
        ├── data/
        │   ├── Models.kt                   ← ALL domain types (Task, Note, Goal, Finance…)
        │   ├── BonagerDatabase.kt          ← SQLite schema (11 tables) + migrations
        │   ├── BonagerRepository.kt        ← ALL data access: CRUD + summary queries
        │   └── Dates.kt                    ← ISO 8601 date utilities
        ├── ui/
        │   ├── BonagerApp.kt               ← navigation hub: bottom bar + drawer
        │   ├── BonagerViewModel.kt         ← app state (AppSnapshot); bridges UI ↔ Repository
        │   ├── screens/
        │   │   ├── TodayScreen.kt          ← dashboard / home
        │   │   ├── TasksScreen.kt          ← task list & management
        │   │   ├── CalendarScreen.kt       ← calendar view (tasks + goals + finance)
        │   │   ├── FinanceScreen.kt        ← income / expenses / debts / savings / accounts
        │   │   ├── NotesScreen.kt          ← notes & daily journal
        │   │   └── GoalsScreen.kt          ← daily goals & streak tracking
        │   ├── components/
        │   │   └── Components.kt           ← shared Jetpack Compose components
        │   └── theme/
        │       └── Theme.kt                ← Material3 theme
        └── notifications/
            ├── ReminderManager.kt          ← schedules / cancels task reminders
            └── ReminderReceiver.kt         ← BroadcastReceiver for fired reminders
```

---

## Architecture Overview

```
UI (Screens) → BonagerViewModel (AppSnapshot state) → BonagerRepository → BonagerDatabase (SQLite)
                                                     ↘ ReminderManager (Android notifications)
```

- **State is read-only snapshots.** `AppSnapshot` in `BonagerViewModel` holds all UI data; screens observe it.
- **All DB access through `BonagerRepository`.** Never write SQL outside this file.
- **Models are the contract.** Add/change domain types in `Models.kt` first, then propagate to DB + Repository.
- **No network.** Everything is local SQLite. No API clients, no auth, no remote calls.

---

## Key Files Reference

| File | What it owns |
|------|-------------|
| `Models.kt` | Task, TaskDraft, TaskStatus, TaskPriority, Note, NoteKind, Goal, GoalCheckin, FinanceTransaction, TransactionKind, FinanceLabel, Debt, SavingsGoal, BankAccount, FinanceSummary, DashboardStats, CalendarMonth, AppSnapshot |
| `BonagerDatabase.kt` | Tables: clients, tasks, task_logs, notes, goals, goal_checkins, finance_transactions, finance_labels, debts, savings_goals, bank_accounts. Version 2 with migration. |
| `BonagerRepository.kt` | loadSnapshot(), saveTask(), toggleTask(), listTasks(), getJournal(), finance CRUD, calendar queries |
| `BonagerApp.kt` | Bottom navigation destinations, drawer items, Compose NavHost |
| `BonagerViewModel.kt` | `AppSnapshot` state holder, coroutine scope, exposes methods that call Repository |
| `Dates.kt` | parseDate(), formatDate(), monthKey(), ISO 8601 helpers |
| `ReminderManager.kt` | scheduleReminder(), cancelReminder(), createNotificationChannel() |

---

## Workflow for AI Agents

Follow these steps on every task — skipping plan mode wastes tokens on wrong approaches.

### 1. Orient (read this file + working tree above)
- Identify which files are relevant. Read only those — not the whole repo.
- Check `Models.kt` for existing types before defining new ones.
- Check `Components.kt` for existing UI primitives before building new ones.

### 2. Plan before coding
- Enter plan mode (`/plan` in Claude Code, or `EnterPlanMode` tool).
- Write a concise plan: what changes, which files, what pattern to follow.
- Get user approval before touching code.

### 3. Implement following established patterns

**Adding a new data type:**
1. Define model in `Models.kt`
2. Add table + migration in `BonagerDatabase.kt`
3. Add CRUD methods in `BonagerRepository.kt`
4. Expose via `AppSnapshot` in `BonagerViewModel.kt`

**Adding a new screen:**
1. Create `ui/screens/NewScreen.kt`
2. Add navigation destination in `BonagerApp.kt`
3. Add ViewModel methods if needed in `BonagerViewModel.kt`

**Adding a shared UI component:**
- Add to `Components.kt` (not inline in a screen file)

**Adding a notification type:**
- Extend `ReminderManager.kt`

### 4. Build & verify
```bash
cd android
./gradlew testDebugUnitTest assembleDebug lintDebug
```
Fix all lint and test failures before considering the task done.

### 5. Update this file
After every change, update `AI_INSTRUCTIONS.md`:
- Add new files to the **Working Tree** section
- Add new types to the **Key Files Reference** table
- Update patterns in the **Workflow** section if a new convention was established

---

## Token-Saving Conventions

- **Read `Models.kt` before any data work** — all types live there, no need to grep screens.
- **Read `BonagerRepository.kt` method list** (first ~50 lines) before writing new queries — it likely exists.
- **`Components.kt` is the only shared component file** — no components scattered in screen files.
- **Navigation is entirely in `BonagerApp.kt`** — one file, one place.
- **Dates always go through `Dates.kt`** — no raw `SimpleDateFormat` or `LocalDate` elsewhere.
- **Skip `android/build/`, `.gradle/`, `.idea/`** — generated, never edit.
