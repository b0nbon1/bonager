package com.bonvic.bonager.data

import android.content.ContentValues
import android.content.Context
import android.database.Cursor
import android.database.sqlite.SQLiteDatabase
import com.bonvic.bonager.notifications.ReminderManager
import kotlin.math.round

class BonagerRepository(context: Context) {
    private val database = BonagerDatabase(context.applicationContext)
    private val reminders = ReminderManager(context.applicationContext)

    fun loadSnapshot(): AppSnapshot {
        val tasks = listTasks()
        val logs = listTaskLogs()
        val notes = listNotes()
        val journal = getJournal(Dates.toDateKey())
        val goals = listGoals()
        val transactions = listTransactions()
        val finance = summarize(transactions)
        val labels = listLabels()
        val debts = listDebts()
        val savingsGoals = listSavingsGoals()
        val bankAccounts = listBankAccounts()
        val dashboard = DashboardStats(
            openTasks = tasks.count { it.status != TaskStatus.DONE },
            dueToday = tasks.count {
                it.status != TaskStatus.DONE && (Dates.isToday(it.dueAt) || Dates.isToday(it.reminderAt))
            },
            billableUnpaid = logs.filterNot(TaskLog::paid).sumOf(TaskLog::amount),
            monthIncome = finance.income,
            monthExpense = finance.expense,
            activeStreaks = goals.count { it.currentStreak > 0 },
        )
        return AppSnapshot(
            tasks = tasks,
            taskLogs = logs,
            notes = notes,
            journal = journal,
            goals = goals,
            transactions = transactions,
            financeSummary = finance,
            dashboard = dashboard,
            labels = labels,
            debts = debts,
            savingsGoals = savingsGoals,
            bankAccounts = bankAccounts,
        )
    }

    fun saveTask(draft: TaskDraft): Long {
        val title = draft.title.trim()
        require(title.isNotEmpty()) { "Task title is required." }

        val db = database.writableDatabase
        draft.id?.let(reminders::cancel)
        val now = Dates.nowIso()
        val clientId = if (draft.isBillable) {
            findOrCreateClient(db, draft.clientName, draft.hourlyRate)
        } else null
        val values = ContentValues().apply {
            put("title", title)
            putNullable("notes", draft.notes.trim().ifEmpty { null })
            put("priority", draft.priority.storage)
            putNullable("due_at", draft.dueAt)
            putNullable("reminder_at", draft.reminderAt)
            putNull("notification_id")
            putNullable("client_id", clientId)
            put("is_billable", if (draft.isBillable) 1 else 0)
            put("hourly_rate", draft.hourlyRate)
            put("estimated_minutes", draft.estimatedMinutes)
            put("updated_at", now)
        }

        val taskId = if (draft.id != null) {
            db.update("tasks", values, "id = ?", arrayOf(draft.id.toString()))
            draft.id
        } else {
            values.put("status", TaskStatus.OPEN.storage)
            values.put("created_at", now)
            db.insertOrThrow("tasks", null, values)
        }

        val reminderDate = Dates.parse(draft.reminderAt)
        if (reminderDate != null && reminderDate.time > System.currentTimeMillis() + 30_000L) {
            reminders.schedule(taskId, title, reminderDate.time)
            db.update(
                "tasks",
                ContentValues().apply { put("notification_id", taskId.toString()) },
                "id = ?",
                arrayOf(taskId.toString()),
            )
        }
        return taskId
    }

    fun toggleTask(task: Task) {
        val next = if (task.status == TaskStatus.DONE) TaskStatus.OPEN else TaskStatus.DONE
        if (next == TaskStatus.DONE) reminders.cancel(task.id)
        database.writableDatabase.update(
            "tasks",
            ContentValues().apply {
                put("status", next.storage)
                putNullable("completed_at", if (next == TaskStatus.DONE) Dates.nowIso() else null)
                put("updated_at", Dates.nowIso())
                if (next == TaskStatus.DONE) putNull("notification_id")
            },
            "id = ?",
            arrayOf(task.id.toString()),
        )
    }

    fun deleteTask(task: Task) {
        reminders.cancel(task.id)
        database.writableDatabase.delete("tasks", "id = ?", arrayOf(task.id.toString()))
    }

    fun saveTaskLog(draft: TaskLogDraft) {
        val title = draft.title.trim()
        require(title.isNotEmpty()) { "Log title is required." }
        require(draft.minutes >= 0) { "Minutes cannot be negative." }

        val db = database.writableDatabase
        val task = draft.taskId?.let(::getTask)
        val explicitClient = draft.clientName.trim()
        val clientId = when {
            explicitClient.isNotEmpty() -> findOrCreateClient(db, explicitClient, task?.hourlyRate ?: 0.0)
            else -> task?.clientId
        }
        val computed = if (draft.amount > 0) draft.amount else {
            if (task?.isBillable == true && task.hourlyRate > 0) {
                round(task.hourlyRate * draft.minutes / 60.0 * 100.0) / 100.0
            } else 0.0
        }
        val now = Dates.nowIso()
        db.insertOrThrow("task_logs", null, ContentValues().apply {
            putNullable("task_id", draft.taskId)
            putNullable("client_id", clientId)
            put("title", title)
            put("minutes", draft.minutes)
            put("amount", computed)
            put("paid", if (draft.paid) 1 else 0)
            put("logged_at", now)
            putNullable("notes", draft.notes.trim().ifEmpty { null })
            put("created_at", now)
            put("updated_at", now)
        })
    }

    fun deleteTaskLog(id: Long) {
        database.writableDatabase.delete("task_logs", "id = ?", arrayOf(id.toString()))
    }

    fun saveNote(id: Long?, title: String, body: String) {
        val cleanTitle = title.trim().ifEmpty { "Untitled" }
        val now = Dates.nowIso()
        val values = ContentValues().apply {
            put("title", cleanTitle)
            put("body", body.trim())
            put("updated_at", now)
        }
        if (id != null) {
            database.writableDatabase.update("notes", values, "id = ?", arrayOf(id.toString()))
        } else {
            values.put("kind", NoteKind.NOTE.storage)
            values.put("created_at", now)
            database.writableDatabase.insertOrThrow("notes", null, values)
        }
    }

    fun saveJournal(body: String, mood: String) {
        val db = database.writableDatabase
        val dateKey = Dates.toDateKey()
        val existingId = db.rawQuery(
            "SELECT id FROM notes WHERE kind = ? AND date_key = ? LIMIT 1",
            arrayOf(NoteKind.JOURNAL.storage, dateKey),
        ).use { cursor -> if (cursor.moveToFirst()) cursor.getLong(0) else null }
        val now = Dates.nowIso()
        val values = ContentValues().apply {
            put("title", "Journal $dateKey")
            put("body", body.trim())
            putNullable("mood", mood.trim().ifEmpty { null })
            put("updated_at", now)
        }
        if (existingId != null) {
            db.update("notes", values, "id = ?", arrayOf(existingId.toString()))
        } else {
            values.put("kind", NoteKind.JOURNAL.storage)
            values.put("date_key", dateKey)
            values.put("created_at", now)
            db.insertOrThrow("notes", null, values)
        }
    }

    fun deleteNote(id: Long) {
        database.writableDatabase.delete("notes", "id = ?", arrayOf(id.toString()))
    }

    fun saveGoal(title: String, color: String) {
        val clean = title.trim()
        require(clean.isNotEmpty()) { "Goal title is required." }
        val now = Dates.nowIso()
        database.writableDatabase.insertOrThrow("goals", null, ContentValues().apply {
            put("title", clean)
            put("cadence", "daily")
            put("color", color)
            put("created_at", now)
            put("updated_at", now)
            put("archived", 0)
        })
    }

    fun checkInGoal(id: Long) {
        val db = database.writableDatabase
        db.insertWithOnConflict("goal_checkins", null, ContentValues().apply {
            put("goal_id", id)
            put("date_key", Dates.toDateKey())
            put("created_at", Dates.nowIso())
        }, SQLiteDatabase.CONFLICT_IGNORE)
        db.update(
            "goals",
            ContentValues().apply { put("updated_at", Dates.nowIso()) },
            "id = ?",
            arrayOf(id.toString()),
        )
    }

    fun deleteGoal(id: Long) {
        database.writableDatabase.delete("goals", "id = ?", arrayOf(id.toString()))
    }

    fun saveTransaction(
        kind: TransactionKind,
        amount: Double,
        category: String,
        note: String,
        clientName: String,
        happenedAt: String,
        labelId: Long? = null,
        debtId: Long? = null,
        savingsGoalId: Long? = null,
        bankAccountId: Long? = null,
    ) {
        require(amount > 0) { "Amount must be greater than zero." }
        val db = database.writableDatabase
        val now = Dates.nowIso()
        val clientId = findOrCreateClient(db, clientName, 0.0)
        val resolvedCategory = category.trim().ifEmpty {
            when (kind) {
                TransactionKind.INCOME -> "Income"
                TransactionKind.DEBT_PAYMENT -> "Debt Payment"
                TransactionKind.SAVINGS_DEPOSIT -> "Savings"
                TransactionKind.TRANSFER -> "Transfer"
                else -> "General"
            }
        }
        db.beginTransaction()
        try {
            db.insertOrThrow("finance_transactions", null, ContentValues().apply {
                put("kind", kind.storage)
                put("amount", amount)
                put("category", resolvedCategory)
                putNullable("note", note.trim().ifEmpty { null })
                putNullable("client_id", clientId)
                put("happened_at", happenedAt)
                put("created_at", now)
                put("updated_at", now)
                putNullable("label_id", labelId)
                putNullable("debt_id", debtId)
                putNullable("savings_goal_id", savingsGoalId)
                putNullable("bank_account_id", bankAccountId)
            })
            if (debtId != null && kind == TransactionKind.DEBT_PAYMENT) {
                incrementDebtPaidAmount(db, debtId, amount)
            }
            if (savingsGoalId != null && kind == TransactionKind.SAVINGS_DEPOSIT) {
                incrementSavingsCurrentAmount(db, savingsGoalId, amount)
            }
            if (bankAccountId != null) {
                adjustBankAccountBalance(db, bankAccountId, amount, kind)
            }
            db.setTransactionSuccessful()
        } finally {
            db.endTransaction()
        }
    }

    fun deleteTransaction(id: Long) {
        val db = database.writableDatabase

        data class TxRow(val kind: TransactionKind, val amount: Double, val debtId: Long?, val savingsGoalId: Long?, val bankAccountId: Long?)

        val row = db.rawQuery(
            "SELECT kind, amount, debt_id, savings_goal_id, bank_account_id FROM finance_transactions WHERE id = ? LIMIT 1",
            arrayOf(id.toString()),
        ).use { cursor ->
            if (cursor.moveToFirst()) TxRow(
                kind = TransactionKind.from(cursor.getString(0)),
                amount = cursor.getDouble(1),
                debtId = if (cursor.isNull(2)) null else cursor.getLong(2),
                savingsGoalId = if (cursor.isNull(3)) null else cursor.getLong(3),
                bankAccountId = if (cursor.isNull(4)) null else cursor.getLong(4),
            ) else null
        } ?: return

        db.beginTransaction()
        try {
            db.delete("finance_transactions", "id = ?", arrayOf(id.toString()))
            if (row.debtId != null && row.kind == TransactionKind.DEBT_PAYMENT) {
                incrementDebtPaidAmount(db, row.debtId, -row.amount)
            }
            if (row.savingsGoalId != null && row.kind == TransactionKind.SAVINGS_DEPOSIT) {
                incrementSavingsCurrentAmount(db, row.savingsGoalId, -row.amount)
            }
            if (row.bankAccountId != null) {
                val delta = when (row.kind) {
                    TransactionKind.INCOME, TransactionKind.TRANSFER -> -row.amount
                    else -> row.amount
                }
                db.execSQL(
                    "UPDATE bank_accounts SET balance = balance + ?, updated_at = ? WHERE id = ?",
                    arrayOf<Any>(delta, Dates.nowIso(), row.bankAccountId),
                )
            }
            db.setTransactionSuccessful()
        } finally {
            db.endTransaction()
        }
    }

    fun saveLabel(name: String, color: String) {
        val clean = name.trim()
        require(clean.isNotEmpty()) { "Label name is required." }
        val now = Dates.nowIso()
        database.writableDatabase.insertOrThrow("finance_labels", null, ContentValues().apply {
            put("name", clean)
            put("color", color)
            put("created_at", now)
            put("updated_at", now)
        })
    }

    fun deleteLabel(id: Long) {
        database.writableDatabase.delete("finance_labels", "id = ?", arrayOf(id.toString()))
    }

    fun saveDebt(name: String, totalAmount: Double, dueDate: String?, interestRate: Double, notes: String?) {
        val clean = name.trim()
        require(clean.isNotEmpty()) { "Debt name is required." }
        require(totalAmount > 0) { "Total amount must be greater than zero." }
        val now = Dates.nowIso()
        database.writableDatabase.insertOrThrow("debts", null, ContentValues().apply {
            put("name", clean)
            put("total_amount", totalAmount)
            put("paid_amount", 0.0)
            putNullable("due_date", dueDate?.trim()?.ifEmpty { null })
            put("interest_rate", interestRate)
            putNullable("notes", notes?.trim()?.ifEmpty { null })
            put("created_at", now)
            put("updated_at", now)
        })
    }

    fun deleteDebt(id: Long) {
        database.writableDatabase.delete("debts", "id = ?", arrayOf(id.toString()))
    }

    fun saveSavingsGoal(name: String, targetAmount: Double, color: String) {
        val clean = name.trim()
        require(clean.isNotEmpty()) { "Goal name is required." }
        require(targetAmount > 0) { "Target amount must be greater than zero." }
        val now = Dates.nowIso()
        database.writableDatabase.insertOrThrow("savings_goals", null, ContentValues().apply {
            put("name", clean)
            put("target_amount", targetAmount)
            put("current_amount", 0.0)
            put("color", color)
            put("created_at", now)
            put("updated_at", now)
        })
    }

    fun deleteSavingsGoal(id: Long) {
        database.writableDatabase.delete("savings_goals", "id = ?", arrayOf(id.toString()))
    }

    fun saveBankAccount(name: String, accountType: String, initialBalance: Double, overdraftLimit: Double) {
        val clean = name.trim()
        require(clean.isNotEmpty()) { "Account name is required." }
        val now = Dates.nowIso()
        database.writableDatabase.insertOrThrow("bank_accounts", null, ContentValues().apply {
            put("name", clean)
            put("account_type", accountType)
            put("balance", initialBalance)
            put("overdraft_limit", overdraftLimit)
            put("created_at", now)
            put("updated_at", now)
        })
    }

    fun deleteBankAccount(id: Long) {
        database.writableDatabase.delete("bank_accounts", "id = ?", arrayOf(id.toString()))
    }

    fun completePomodoro(task: Task?, minutes: Int) {
        if (task != null) {
            saveTaskLog(
                TaskLogDraft(
                    taskId = task.id,
                    clientName = task.clientName.orEmpty(),
                    title = "Pomodoro: ${task.title}",
                    minutes = minutes,
                    amount = 0.0,
                    paid = false,
                    notes = "Logged from Pomodoro timer",
                ),
            )
        }
        reminders.showPomodoroComplete(task?.title)
    }

    fun loadCalendarMonth(monthKey: String): CalendarMonth {
        val db = database.readableDatabase

        // Tasks: filter all tasks whose dueAt or reminderAt falls in this month
        val allTasks = listTasks()
        val tasksByDate = mutableMapOf<String, MutableList<Task>>()
        for (task in allTasks) {
            val dueKey = Dates.parse(task.dueAt)?.let(Dates::toDateKey)
            val remKey = Dates.parse(task.reminderAt)?.let(Dates::toDateKey)
            if (dueKey != null && dueKey.startsWith(monthKey)) {
                tasksByDate.getOrPut(dueKey) { mutableListOf() }.add(task)
            } else if (remKey != null && remKey.startsWith(monthKey)) {
                tasksByDate.getOrPut(remKey) { mutableListOf() }.add(task)
            }
        }

        // Goal check-ins for this month
        val allGoals = listGoals()
        val goalStreakMap = allGoals.associateBy { it.id }
        val checkInsByDate = mutableMapOf<String, MutableList<GoalCheckin>>()
        db.rawQuery(
            """SELECT gc.date_key, g.id, g.title, g.color
               FROM goal_checkins gc
               JOIN goals g ON g.id = gc.goal_id
               WHERE substr(gc.date_key, 1, 7) = ?""",
            arrayOf(monthKey),
        ).use { cursor ->
            while (cursor.moveToNext()) {
                val dateKey = cursor.getString(0)
                val goalId = cursor.getLong(1)
                val checkin = GoalCheckin(
                    goalId = goalId,
                    goalTitle = cursor.getString(2),
                    goalColor = cursor.getString(3),
                    streak = goalStreakMap[goalId]?.currentStreak ?: 0,
                )
                checkInsByDate.getOrPut(dateKey) { mutableListOf() }.add(checkin)
            }
        }

        // Journal entries for this month
        val journalByDate = mutableMapOf<String, Note>()
        db.rawQuery(
            "SELECT * FROM notes WHERE kind = ? AND substr(date_key, 1, 7) = ?",
            arrayOf(NoteKind.JOURNAL.storage, monthKey),
        ).use { cursor ->
            while (cursor.moveToNext()) {
                val note = cursor.toNote()
                note.dateKey?.let { journalByDate[it] = note }
            }
        }

        // Transactions for this month
        val transactions = listTransactions(monthKey)
        val transactionsByDate = mutableMapOf<String, MutableList<FinanceTransaction>>()
        for (tx in transactions) {
            val dateKey = tx.happenedAt.take(10)
            transactionsByDate.getOrPut(dateKey) { mutableListOf() }.add(tx)
        }

        return CalendarMonth(
            monthKey = monthKey,
            tasksByDate = tasksByDate,
            checkInsByDate = checkInsByDate,
            journalByDate = journalByDate,
            transactionsByDate = transactionsByDate,
        )
    }

    fun rescheduleReminders() {
        listTasks()
            .filter { it.status != TaskStatus.DONE }
            .forEach { task ->
                val at = Dates.parse(task.reminderAt)?.time ?: return@forEach
                if (at > System.currentTimeMillis()) reminders.schedule(task.id, task.title, at)
            }
    }

    private fun getTask(id: Long): Task? = database.readableDatabase.rawQuery(
        "$TASK_SELECT WHERE tasks.id = ? LIMIT 1",
        arrayOf(id.toString()),
    ).use { cursor -> if (cursor.moveToFirst()) cursor.toTask() else null }

    private fun listTasks(): List<Task> = database.readableDatabase.rawQuery(
        """$TASK_SELECT
           ORDER BY
             CASE tasks.status WHEN 'in_progress' THEN 0 WHEN 'open' THEN 1 ELSE 2 END,
             CASE tasks.priority WHEN 'high' THEN 0 WHEN 'medium' THEN 1 ELSE 2 END,
             COALESCE(tasks.due_at, tasks.reminder_at, tasks.created_at) ASC""".trimIndent(),
        null,
    ).use { cursor -> cursor.mapRows(Cursor::toTask) }

    private fun listTaskLogs(): List<TaskLog> = database.readableDatabase.rawQuery(
        """SELECT task_logs.*, tasks.title AS task_title, clients.name AS client_name
           FROM task_logs
           LEFT JOIN tasks ON tasks.id = task_logs.task_id
           LEFT JOIN clients ON clients.id = task_logs.client_id
           ORDER BY task_logs.logged_at DESC, task_logs.created_at DESC""".trimIndent(),
        null,
    ).use { cursor ->
        cursor.mapRows {
            TaskLog(
                id = long("id"),
                taskId = nullableLong("task_id"),
                taskTitle = nullableString("task_title"),
                clientId = nullableLong("client_id"),
                clientName = nullableString("client_name"),
                title = string("title"),
                minutes = int("minutes"),
                amount = double("amount"),
                paid = int("paid") != 0,
                loggedAt = string("logged_at"),
                notes = nullableString("notes"),
            )
        }
    }

    private fun listNotes(): List<Note> = database.readableDatabase.rawQuery(
        "SELECT * FROM notes WHERE kind = ? ORDER BY updated_at DESC",
        arrayOf(NoteKind.NOTE.storage),
    ).use { cursor -> cursor.mapRows(Cursor::toNote) }

    private fun getJournal(dateKey: String): Note? = database.readableDatabase.rawQuery(
        "SELECT * FROM notes WHERE kind = ? AND date_key = ? LIMIT 1",
        arrayOf(NoteKind.JOURNAL.storage, dateKey),
    ).use { cursor -> if (cursor.moveToFirst()) cursor.toNote() else null }

    private fun listGoals(): List<Goal> {
        val db = database.readableDatabase
        return db.rawQuery("SELECT * FROM goals WHERE archived = 0 ORDER BY updated_at DESC", null).use { goals ->
            goals.mapRows {
                val goalId = long("id")
                val dates = db.rawQuery(
                    "SELECT date_key FROM goal_checkins WHERE goal_id = ? ORDER BY date_key DESC",
                    arrayOf(goalId.toString()),
                ).use { checks -> checks.mapRows { string("date_key") } }
                Goal(
                    id = goalId,
                    title = string("title"),
                    color = string("color"),
                    currentStreak = calculateStreak(dates),
                    totalCheckIns = dates.size,
                    checkedToday = Dates.toDateKey() in dates,
                )
            }
        }
    }

    private fun listTransactions(monthKey: String = Dates.toMonthKey()): List<FinanceTransaction> = database.readableDatabase.rawQuery(
        """SELECT finance_transactions.*, clients.name AS client_name,
                  finance_labels.name AS label_name, finance_labels.color AS label_color
           FROM finance_transactions
           LEFT JOIN clients ON clients.id = finance_transactions.client_id
           LEFT JOIN finance_labels ON finance_labels.id = finance_transactions.label_id
           WHERE substr(finance_transactions.happened_at, 1, 7) = ?
           ORDER BY finance_transactions.happened_at DESC, finance_transactions.created_at DESC""".trimIndent(),
        arrayOf(monthKey),
    ).use { cursor ->
        cursor.mapRows {
            val labelId = nullableLong("label_id")
            val label = if (labelId != null) {
                FinanceLabel(
                    id = labelId,
                    name = nullableString("label_name") ?: "",
                    color = nullableString("label_color") ?: "#637370",
                )
            } else null
            FinanceTransaction(
                id = long("id"),
                kind = TransactionKind.from(nullableString("kind")),
                amount = double("amount"),
                category = string("category"),
                note = nullableString("note"),
                clientName = nullableString("client_name"),
                happenedAt = string("happened_at"),
                labelId = labelId,
                debtId = nullableLong("debt_id"),
                savingsGoalId = nullableLong("savings_goal_id"),
                bankAccountId = nullableLong("bank_account_id"),
                label = label,
            )
        }
    }

    private fun listLabels(): List<FinanceLabel> = database.readableDatabase.rawQuery(
        "SELECT * FROM finance_labels ORDER BY name ASC",
        null,
    ).use { cursor ->
        cursor.mapRows {
            FinanceLabel(id = long("id"), name = string("name"), color = string("color"))
        }
    }

    private fun listDebts(): List<Debt> = database.readableDatabase.rawQuery(
        "SELECT * FROM debts ORDER BY created_at DESC",
        null,
    ).use { cursor ->
        cursor.mapRows {
            Debt(
                id = long("id"),
                name = string("name"),
                totalAmount = double("total_amount"),
                paidAmount = double("paid_amount"),
                dueDate = nullableString("due_date"),
                interestRate = double("interest_rate"),
                notes = nullableString("notes"),
                createdAt = string("created_at"),
            )
        }
    }

    private fun listSavingsGoals(): List<SavingsGoal> = database.readableDatabase.rawQuery(
        "SELECT * FROM savings_goals ORDER BY created_at DESC",
        null,
    ).use { cursor ->
        cursor.mapRows {
            SavingsGoal(
                id = long("id"),
                name = string("name"),
                targetAmount = double("target_amount"),
                currentAmount = double("current_amount"),
                color = string("color"),
                createdAt = string("created_at"),
            )
        }
    }

    private fun listBankAccounts(): List<BankAccount> = database.readableDatabase.rawQuery(
        "SELECT * FROM bank_accounts ORDER BY name ASC",
        null,
    ).use { cursor ->
        cursor.mapRows {
            BankAccount(
                id = long("id"),
                name = string("name"),
                accountType = string("account_type"),
                balance = double("balance"),
                overdraftLimit = double("overdraft_limit"),
            )
        }
    }

    private fun incrementDebtPaidAmount(db: android.database.sqlite.SQLiteDatabase, debtId: Long, delta: Double) {
        db.execSQL(
            "UPDATE debts SET paid_amount = MAX(0, paid_amount + ?), updated_at = ? WHERE id = ?",
            arrayOf<Any>(delta, Dates.nowIso(), debtId),
        )
    }

    private fun incrementSavingsCurrentAmount(db: android.database.sqlite.SQLiteDatabase, savingsGoalId: Long, delta: Double) {
        db.execSQL(
            "UPDATE savings_goals SET current_amount = MAX(0, current_amount + ?), updated_at = ? WHERE id = ?",
            arrayOf<Any>(delta, Dates.nowIso(), savingsGoalId),
        )
    }

    private fun adjustBankAccountBalance(db: android.database.sqlite.SQLiteDatabase, bankAccountId: Long, amount: Double, kind: TransactionKind) {
        val delta = when (kind) {
            TransactionKind.INCOME, TransactionKind.TRANSFER -> amount
            else -> -amount
        }
        db.execSQL(
            "UPDATE bank_accounts SET balance = balance + ?, updated_at = ? WHERE id = ?",
            arrayOf<Any>(delta, Dates.nowIso(), bankAccountId),
        )
    }

    private fun summarize(transactions: List<FinanceTransaction>): FinanceSummary {
        val categories = linkedMapOf<String, Pair<Double, Double>>()
        transactions.forEach { transaction ->
            if (transaction.kind == TransactionKind.TRANSFER) return@forEach
            val current = categories[transaction.category] ?: (0.0 to 0.0)
            categories[transaction.category] = if (transaction.kind == TransactionKind.INCOME) {
                current.first to current.second + transaction.amount
            } else {
                current.first + transaction.amount to current.second
            }
        }
        return FinanceSummary(
            monthKey = Dates.toMonthKey(),
            income = transactions.filter { it.kind == TransactionKind.INCOME }.sumOf(FinanceTransaction::amount),
            expense = transactions.filter {
                it.kind == TransactionKind.EXPENSE ||
                    it.kind == TransactionKind.DEBT_PAYMENT ||
                    it.kind == TransactionKind.SAVINGS_DEPOSIT
            }.sumOf(FinanceTransaction::amount),
            byCategory = categories.map { (category, totals) ->
                CategorySummary(category, expense = totals.first, income = totals.second)
            }.sortedByDescending { it.expense + it.income },
        )
    }

    private fun findOrCreateClient(db: SQLiteDatabase, rawName: String, rate: Double): Long? {
        val name = rawName.trim()
        if (name.isEmpty()) return null
        val existing = db.rawQuery(
            "SELECT id, default_rate FROM clients WHERE LOWER(name) = LOWER(?) LIMIT 1",
            arrayOf(name),
        ).use { cursor ->
            if (cursor.moveToFirst()) cursor.getLong(0) to cursor.getDouble(1) else null
        }
        if (existing != null) {
            if (rate > 0 && existing.second == 0.0) {
                db.update("clients", ContentValues().apply {
                    put("default_rate", rate)
                    put("updated_at", Dates.nowIso())
                }, "id = ?", arrayOf(existing.first.toString()))
            }
            return existing.first
        }
        val now = Dates.nowIso()
        return db.insertOrThrow("clients", null, ContentValues().apply {
            put("name", name)
            put("default_rate", rate)
            put("created_at", now)
            put("updated_at", now)
        })
    }

    companion object {
        private const val TASK_SELECT = """SELECT tasks.*, clients.name AS client_name
            FROM tasks LEFT JOIN clients ON clients.id = tasks.client_id"""

        fun calculateStreak(dateKeys: List<String>, today: String = Dates.toDateKey()): Int {
            val checked = dateKeys.toSet()
            var cursor = if (today in checked) today else Dates.addDays(today, -1)
            var streak = 0
            while (cursor in checked) {
                streak += 1
                cursor = Dates.addDays(cursor, -1)
            }
            return streak
        }
    }
}

private fun Cursor.toTask() = Task(
    id = long("id"),
    title = string("title"),
    notes = nullableString("notes"),
    status = TaskStatus.from(nullableString("status")),
    priority = TaskPriority.from(nullableString("priority")),
    dueAt = nullableString("due_at"),
    reminderAt = nullableString("reminder_at"),
    notificationId = nullableString("notification_id"),
    clientId = nullableLong("client_id"),
    clientName = nullableString("client_name"),
    isBillable = int("is_billable") != 0,
    hourlyRate = double("hourly_rate"),
    estimatedMinutes = int("estimated_minutes"),
    createdAt = string("created_at"),
    updatedAt = string("updated_at"),
    completedAt = nullableString("completed_at"),
)

private fun Cursor.toNote() = Note(
    id = long("id"),
    title = string("title"),
    body = string("body"),
    kind = if (string("kind") == NoteKind.JOURNAL.storage) NoteKind.JOURNAL else NoteKind.NOTE,
    dateKey = nullableString("date_key"),
    mood = nullableString("mood"),
    updatedAt = string("updated_at"),
)

private fun <T> Cursor.mapRows(transform: Cursor.() -> T): List<T> = buildList {
    while (moveToNext()) add(transform())
}

private fun Cursor.string(name: String): String = getString(getColumnIndexOrThrow(name))
private fun Cursor.nullableString(name: String): String? =
    getColumnIndexOrThrow(name).let { index -> if (isNull(index)) null else getString(index) }
private fun Cursor.long(name: String): Long = getLong(getColumnIndexOrThrow(name))
private fun Cursor.nullableLong(name: String): Long? =
    getColumnIndexOrThrow(name).let { index -> if (isNull(index)) null else getLong(index) }
private fun Cursor.int(name: String): Int = getInt(getColumnIndexOrThrow(name))
private fun Cursor.double(name: String): Double = getDouble(getColumnIndexOrThrow(name))

private fun ContentValues.putNullable(key: String, value: String?) {
    if (value == null) putNull(key) else put(key, value)
}

private fun ContentValues.putNullable(key: String, value: Long?) {
    if (value == null) putNull(key) else put(key, value)
}
