package com.bonvic.bonager.data

enum class TaskStatus(val storage: String) {
    OPEN("open"),
    IN_PROGRESS("in_progress"),
    DONE("done");

    companion object {
        fun from(value: String?) = entries.firstOrNull { it.storage == value } ?: OPEN
    }
}

enum class TaskPriority(val storage: String) {
    LOW("low"),
    MEDIUM("medium"),
    HIGH("high");

    companion object {
        fun from(value: String?) = entries.firstOrNull { it.storage == value } ?: MEDIUM
    }
}

enum class NoteKind(val storage: String) { NOTE("note"), JOURNAL("journal") }
enum class TransactionKind(val storage: String) {
    EXPENSE("expense"),
    INCOME("income"),
    DEBT_PAYMENT("debt_payment"),
    SAVINGS_DEPOSIT("savings_deposit"),
    TRANSFER("transfer");

    companion object {
        fun from(value: String?) = entries.firstOrNull { it.storage == value } ?: EXPENSE
    }
}

data class Task(
    val id: Long,
    val title: String,
    val notes: String?,
    val status: TaskStatus,
    val priority: TaskPriority,
    val dueAt: String?,
    val reminderAt: String?,
    val notificationId: String?,
    val clientId: Long?,
    val clientName: String?,
    val isBillable: Boolean,
    val hourlyRate: Double,
    val estimatedMinutes: Int,
    val createdAt: String,
    val updatedAt: String,
    val completedAt: String?,
)

data class TaskDraft(
    val id: Long? = null,
    val title: String,
    val notes: String = "",
    val priority: TaskPriority = TaskPriority.MEDIUM,
    val dueAt: String? = null,
    val reminderAt: String? = null,
    val clientName: String = "",
    val isBillable: Boolean = false,
    val hourlyRate: Double = 0.0,
    val estimatedMinutes: Int = 0,
)

data class TaskLog(
    val id: Long,
    val taskId: Long?,
    val taskTitle: String?,
    val clientId: Long?,
    val clientName: String?,
    val title: String,
    val minutes: Int,
    val amount: Double,
    val paid: Boolean,
    val loggedAt: String,
    val notes: String?,
)

data class TaskLogDraft(
    val taskId: Long? = null,
    val clientName: String = "",
    val title: String,
    val minutes: Int,
    val amount: Double,
    val paid: Boolean,
    val notes: String = "",
)

data class Note(
    val id: Long,
    val title: String,
    val body: String,
    val kind: NoteKind,
    val dateKey: String?,
    val mood: String?,
    val updatedAt: String,
)

data class Goal(
    val id: Long,
    val title: String,
    val color: String,
    val currentStreak: Int,
    val totalCheckIns: Int,
    val checkedToday: Boolean,
)

data class FinanceLabel(
    val id: Long,
    val name: String,
    val color: String,
)

data class Debt(
    val id: Long,
    val name: String,
    val totalAmount: Double,
    val paidAmount: Double,
    val dueDate: String?,
    val interestRate: Double,
    val notes: String?,
    val createdAt: String,
) {
    val remainingAmount: Double get() = totalAmount - paidAmount
    val progressFraction: Float get() = if (totalAmount <= 0) 0f else (paidAmount / totalAmount).toFloat().coerceIn(0f, 1f)
}

data class SavingsGoal(
    val id: Long,
    val name: String,
    val targetAmount: Double,
    val currentAmount: Double,
    val color: String,
    val createdAt: String,
) {
    val progressFraction: Float get() = if (targetAmount <= 0) 0f else (currentAmount / targetAmount).toFloat().coerceIn(0f, 1f)
}

data class BankAccount(
    val id: Long,
    val name: String,
    val accountType: String,
    val balance: Double,
    val overdraftLimit: Double,
) {
    val isOverdrawn: Boolean get() = balance < 0
    val availableBalance: Double get() = balance + overdraftLimit
}

data class FinanceTransaction(
    val id: Long,
    val kind: TransactionKind,
    val amount: Double,
    val category: String,
    val note: String?,
    val clientName: String?,
    val happenedAt: String,
    val labelId: Long? = null,
    val debtId: Long? = null,
    val savingsGoalId: Long? = null,
    val bankAccountId: Long? = null,
    val label: FinanceLabel? = null,
)

data class CategorySummary(
    val category: String,
    val expense: Double,
    val income: Double,
)

data class FinanceSummary(
    val monthKey: String,
    val income: Double,
    val expense: Double,
    val byCategory: List<CategorySummary>,
) {
    val net: Double get() = income - expense
}

data class DashboardStats(
    val openTasks: Int = 0,
    val dueToday: Int = 0,
    val billableUnpaid: Double = 0.0,
    val monthIncome: Double = 0.0,
    val monthExpense: Double = 0.0,
    val activeStreaks: Int = 0,
)

data class AppSnapshot(
    val tasks: List<Task> = emptyList(),
    val taskLogs: List<TaskLog> = emptyList(),
    val notes: List<Note> = emptyList(),
    val journal: Note? = null,
    val goals: List<Goal> = emptyList(),
    val transactions: List<FinanceTransaction> = emptyList(),
    val financeSummary: FinanceSummary = FinanceSummary(Dates.toMonthKey(), 0.0, 0.0, emptyList()),
    val dashboard: DashboardStats = DashboardStats(),
    val labels: List<FinanceLabel> = emptyList(),
    val debts: List<Debt> = emptyList(),
    val savingsGoals: List<SavingsGoal> = emptyList(),
    val bankAccounts: List<BankAccount> = emptyList(),
)

data class CalendarMonth(
    val monthKey: String,
    val tasksByDate: Map<String, List<Task>> = emptyMap(),
    val checkInsByDate: Map<String, List<GoalCheckin>> = emptyMap(),
    val journalByDate: Map<String, Note> = emptyMap(),
    val transactionsByDate: Map<String, List<FinanceTransaction>> = emptyMap(),
)

data class GoalCheckin(
    val goalId: Long,
    val goalTitle: String,
    val goalColor: String,
    val streak: Int,
)
