package com.bonvic.bonager.data

import android.content.Context
import android.database.sqlite.SQLiteDatabase
import android.database.sqlite.SQLiteOpenHelper
import java.io.File

class BonagerDatabase(context: Context) : SQLiteOpenHelper(
    context,
    migrateLegacyDatabase(context),
    null,
    VERSION,
) {
    override fun onConfigure(db: SQLiteDatabase) {
        super.onConfigure(db)
        db.setForeignKeyConstraintsEnabled(true)
    }

    override fun onCreate(db: SQLiteDatabase) {
        db.execSQL(
            """CREATE TABLE IF NOT EXISTS clients (
                id INTEGER PRIMARY KEY AUTOINCREMENT,
                name TEXT NOT NULL UNIQUE,
                default_rate REAL NOT NULL DEFAULT 0,
                created_at TEXT NOT NULL,
                updated_at TEXT NOT NULL
            )""".trimIndent(),
        )
        db.execSQL(
            """CREATE TABLE IF NOT EXISTS tasks (
                id INTEGER PRIMARY KEY AUTOINCREMENT,
                title TEXT NOT NULL,
                notes TEXT,
                status TEXT NOT NULL DEFAULT 'open',
                priority TEXT NOT NULL DEFAULT 'medium',
                due_at TEXT,
                reminder_at TEXT,
                notification_id TEXT,
                client_id INTEGER,
                is_billable INTEGER NOT NULL DEFAULT 0,
                hourly_rate REAL NOT NULL DEFAULT 0,
                estimated_minutes INTEGER NOT NULL DEFAULT 0,
                created_at TEXT NOT NULL,
                updated_at TEXT NOT NULL,
                completed_at TEXT,
                FOREIGN KEY (client_id) REFERENCES clients(id) ON DELETE SET NULL
            )""".trimIndent(),
        )
        db.execSQL(
            """CREATE TABLE IF NOT EXISTS task_logs (
                id INTEGER PRIMARY KEY AUTOINCREMENT,
                task_id INTEGER,
                client_id INTEGER,
                title TEXT NOT NULL,
                minutes INTEGER NOT NULL DEFAULT 0,
                amount REAL NOT NULL DEFAULT 0,
                paid INTEGER NOT NULL DEFAULT 0,
                logged_at TEXT NOT NULL,
                notes TEXT,
                created_at TEXT NOT NULL,
                updated_at TEXT NOT NULL,
                FOREIGN KEY (task_id) REFERENCES tasks(id) ON DELETE SET NULL,
                FOREIGN KEY (client_id) REFERENCES clients(id) ON DELETE SET NULL
            )""".trimIndent(),
        )
        db.execSQL(
            """CREATE TABLE IF NOT EXISTS notes (
                id INTEGER PRIMARY KEY AUTOINCREMENT,
                title TEXT NOT NULL,
                body TEXT NOT NULL,
                kind TEXT NOT NULL,
                date_key TEXT,
                mood TEXT,
                created_at TEXT NOT NULL,
                updated_at TEXT NOT NULL
            )""".trimIndent(),
        )
        db.execSQL(
            """CREATE UNIQUE INDEX IF NOT EXISTS idx_journal_by_day
               ON notes(kind, date_key)
               WHERE kind = 'journal' AND date_key IS NOT NULL""".trimIndent(),
        )
        db.execSQL(
            """CREATE TABLE IF NOT EXISTS goals (
                id INTEGER PRIMARY KEY AUTOINCREMENT,
                title TEXT NOT NULL,
                cadence TEXT NOT NULL DEFAULT 'daily',
                color TEXT NOT NULL DEFAULT '#0C7C72',
                created_at TEXT NOT NULL,
                updated_at TEXT NOT NULL,
                archived INTEGER NOT NULL DEFAULT 0
            )""".trimIndent(),
        )
        db.execSQL(
            """CREATE TABLE IF NOT EXISTS goal_checkins (
                id INTEGER PRIMARY KEY AUTOINCREMENT,
                goal_id INTEGER NOT NULL,
                date_key TEXT NOT NULL,
                created_at TEXT NOT NULL,
                UNIQUE(goal_id, date_key),
                FOREIGN KEY (goal_id) REFERENCES goals(id) ON DELETE CASCADE
            )""".trimIndent(),
        )
        db.execSQL(
            """CREATE TABLE IF NOT EXISTS finance_transactions (
                id INTEGER PRIMARY KEY AUTOINCREMENT,
                kind TEXT NOT NULL,
                amount REAL NOT NULL,
                category TEXT NOT NULL,
                note TEXT,
                client_id INTEGER,
                happened_at TEXT NOT NULL,
                created_at TEXT NOT NULL,
                updated_at TEXT NOT NULL,
                label_id INTEGER REFERENCES finance_labels(id) ON DELETE SET NULL,
                debt_id INTEGER REFERENCES debts(id) ON DELETE SET NULL,
                savings_goal_id INTEGER REFERENCES savings_goals(id) ON DELETE SET NULL,
                bank_account_id INTEGER REFERENCES bank_accounts(id) ON DELETE SET NULL,
                FOREIGN KEY (client_id) REFERENCES clients(id) ON DELETE SET NULL
            )""".trimIndent(),
        )
        db.execSQL(
            """CREATE TABLE IF NOT EXISTS finance_labels (
                id INTEGER PRIMARY KEY AUTOINCREMENT,
                name TEXT NOT NULL,
                color TEXT NOT NULL DEFAULT '#637370',
                created_at TEXT NOT NULL,
                updated_at TEXT NOT NULL
            )""".trimIndent(),
        )
        db.execSQL(
            """CREATE TABLE IF NOT EXISTS debts (
                id INTEGER PRIMARY KEY AUTOINCREMENT,
                name TEXT NOT NULL,
                total_amount REAL NOT NULL DEFAULT 0,
                paid_amount REAL NOT NULL DEFAULT 0,
                due_date TEXT,
                interest_rate REAL NOT NULL DEFAULT 0,
                notes TEXT,
                created_at TEXT NOT NULL,
                updated_at TEXT NOT NULL
            )""".trimIndent(),
        )
        db.execSQL(
            """CREATE TABLE IF NOT EXISTS savings_goals (
                id INTEGER PRIMARY KEY AUTOINCREMENT,
                name TEXT NOT NULL,
                target_amount REAL NOT NULL DEFAULT 0,
                current_amount REAL NOT NULL DEFAULT 0,
                color TEXT NOT NULL DEFAULT '#0C7C72',
                created_at TEXT NOT NULL,
                updated_at TEXT NOT NULL
            )""".trimIndent(),
        )
        db.execSQL(
            """CREATE TABLE IF NOT EXISTS bank_accounts (
                id INTEGER PRIMARY KEY AUTOINCREMENT,
                name TEXT NOT NULL,
                account_type TEXT NOT NULL DEFAULT 'checking',
                balance REAL NOT NULL DEFAULT 0,
                overdraft_limit REAL NOT NULL DEFAULT 0,
                created_at TEXT NOT NULL,
                updated_at TEXT NOT NULL
            )""".trimIndent(),
        )
    }

    override fun onUpgrade(db: SQLiteDatabase, oldVersion: Int, newVersion: Int) {
        if (oldVersion < 2) applyV2(db)
    }

    private fun applyV2(db: SQLiteDatabase) {
        db.execSQL(
            """CREATE TABLE IF NOT EXISTS finance_labels (
                id INTEGER PRIMARY KEY AUTOINCREMENT,
                name TEXT NOT NULL,
                color TEXT NOT NULL DEFAULT '#637370',
                created_at TEXT NOT NULL,
                updated_at TEXT NOT NULL
            )""".trimIndent(),
        )
        db.execSQL(
            """CREATE TABLE IF NOT EXISTS debts (
                id INTEGER PRIMARY KEY AUTOINCREMENT,
                name TEXT NOT NULL,
                total_amount REAL NOT NULL DEFAULT 0,
                paid_amount REAL NOT NULL DEFAULT 0,
                due_date TEXT,
                interest_rate REAL NOT NULL DEFAULT 0,
                notes TEXT,
                created_at TEXT NOT NULL,
                updated_at TEXT NOT NULL
            )""".trimIndent(),
        )
        db.execSQL(
            """CREATE TABLE IF NOT EXISTS savings_goals (
                id INTEGER PRIMARY KEY AUTOINCREMENT,
                name TEXT NOT NULL,
                target_amount REAL NOT NULL DEFAULT 0,
                current_amount REAL NOT NULL DEFAULT 0,
                color TEXT NOT NULL DEFAULT '#0C7C72',
                created_at TEXT NOT NULL,
                updated_at TEXT NOT NULL
            )""".trimIndent(),
        )
        db.execSQL(
            """CREATE TABLE IF NOT EXISTS bank_accounts (
                id INTEGER PRIMARY KEY AUTOINCREMENT,
                name TEXT NOT NULL,
                account_type TEXT NOT NULL DEFAULT 'checking',
                balance REAL NOT NULL DEFAULT 0,
                overdraft_limit REAL NOT NULL DEFAULT 0,
                created_at TEXT NOT NULL,
                updated_at TEXT NOT NULL
            )""".trimIndent(),
        )
        db.execSQL("ALTER TABLE finance_transactions ADD COLUMN label_id INTEGER")
        db.execSQL("ALTER TABLE finance_transactions ADD COLUMN debt_id INTEGER")
        db.execSQL("ALTER TABLE finance_transactions ADD COLUMN savings_goal_id INTEGER")
        db.execSQL("ALTER TABLE finance_transactions ADD COLUMN bank_account_id INTEGER")
    }

    companion object {
        const val DATABASE_NAME = "bonager.db"
        private const val VERSION = 2

        /** Moves data from the previous database location on first native launch. */
        @Synchronized
        private fun migrateLegacyDatabase(context: Context): String {
            val native = context.getDatabasePath(DATABASE_NAME)
            val legacy = File(context.filesDir, "SQLite/$DATABASE_NAME")
            if (native.exists() || !legacy.exists()) return DATABASE_NAME

            native.parentFile?.mkdirs()
            listOf("", "-wal", "-shm", "-journal").forEach { suffix ->
                val source = File(legacy.path + suffix)
                if (!source.exists()) return@forEach
                val target = File(native.path + suffix)
                if (!source.renameTo(target)) source.copyTo(target, overwrite = true)
            }
            return DATABASE_NAME
        }
    }
}
