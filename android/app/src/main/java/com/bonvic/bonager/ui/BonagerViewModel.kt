package com.bonvic.bonager.ui

import android.app.Application
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.viewModelScope
import com.bonvic.bonager.data.AppSnapshot
import com.bonvic.bonager.data.BonagerRepository
import com.bonvic.bonager.data.CalendarMonth
import com.bonvic.bonager.data.Dates
import com.bonvic.bonager.data.Task
import com.bonvic.bonager.data.TaskDraft
import com.bonvic.bonager.data.TaskLogDraft
import com.bonvic.bonager.data.TransactionKind
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext

data class BonagerUiState(
    val loading: Boolean = true,
    val snapshot: AppSnapshot = AppSnapshot(),
    val calendarMonth: CalendarMonth = CalendarMonth(Dates.toMonthKey()),
    val calendarSelectedDate: String = Dates.toDateKey(),
    val error: String? = null,
)

class BonagerViewModel(application: Application) : AndroidViewModel(application) {
    private val repository = BonagerRepository(application)
    private val _state = MutableStateFlow(BonagerUiState())
    val state: StateFlow<BonagerUiState> = _state.asStateFlow()

    init {
        refresh()
    }

    fun refresh() {
        viewModelScope.launch {
            _state.value = _state.value.copy(loading = true)
            runCatching {
                withContext(Dispatchers.IO) {
                    val snapshot = repository.loadSnapshot()
                    val calendar = repository.loadCalendarMonth(Dates.toMonthKey())
                    snapshot to calendar
                }
            }
                .onSuccess { (snapshot, calendar) ->
                    _state.value = BonagerUiState(loading = false, snapshot = snapshot, calendarMonth = calendar)
                }
                .onFailure { error ->
                    _state.value = _state.value.copy(
                        loading = false,
                        error = error.message ?: "Something went wrong.",
                    )
                }
        }
    }

    fun selectCalendarDate(dateKey: String) {
        _state.value = _state.value.copy(calendarSelectedDate = dateKey)
    }

    fun navigateCalendarMonth(monthKey: String) {
        viewModelScope.launch {
            _state.value = _state.value.copy(loading = true)
            runCatching { withContext(Dispatchers.IO) { repository.loadCalendarMonth(monthKey) } }
                .onSuccess { _state.value = _state.value.copy(loading = false, calendarMonth = it) }
                .onFailure { error ->
                    _state.value = _state.value.copy(
                        loading = false,
                        error = error.message ?: "Something went wrong.",
                    )
                }
        }
    }

    fun clearError() {
        _state.value = _state.value.copy(error = null)
    }

    fun saveTask(draft: TaskDraft) = mutate { repository.saveTask(draft) }
    fun toggleTask(task: Task) = mutate { repository.toggleTask(task) }
    fun deleteTask(task: Task) = mutate { repository.deleteTask(task) }
    fun saveTaskLog(draft: TaskLogDraft) = mutate { repository.saveTaskLog(draft) }
    fun deleteTaskLog(id: Long) = mutate { repository.deleteTaskLog(id) }
    fun saveNote(id: Long?, title: String, body: String) = mutate { repository.saveNote(id, title, body) }
    fun saveJournal(body: String, mood: String) = mutate { repository.saveJournal(body, mood) }
    fun deleteNote(id: Long) = mutate { repository.deleteNote(id) }
    fun saveGoal(title: String, color: String) = mutate { repository.saveGoal(title, color) }
    fun checkInGoal(id: Long) = mutate { repository.checkInGoal(id) }
    fun deleteGoal(id: Long) = mutate { repository.deleteGoal(id) }

    fun saveTransaction(
        kind: TransactionKind,
        amount: Double,
        category: String,
        note: String,
        client: String,
        happenedAt: String,
        labelId: Long? = null,
        debtId: Long? = null,
        savingsGoalId: Long? = null,
        bankAccountId: Long? = null,
    ) = mutate {
        repository.saveTransaction(kind, amount, category, note, client, happenedAt, labelId, debtId, savingsGoalId, bankAccountId)
    }

    fun deleteTransaction(id: Long) = mutate { repository.deleteTransaction(id) }
    fun saveLabel(name: String, color: String) = mutate { repository.saveLabel(name, color) }
    fun deleteLabel(id: Long) = mutate { repository.deleteLabel(id) }
    fun saveDebt(name: String, totalAmount: Double, dueDate: String?, interestRate: Double, notes: String?) =
        mutate { repository.saveDebt(name, totalAmount, dueDate, interestRate, notes) }
    fun deleteDebt(id: Long) = mutate { repository.deleteDebt(id) }
    fun saveSavingsGoal(name: String, targetAmount: Double, color: String) =
        mutate { repository.saveSavingsGoal(name, targetAmount, color) }
    fun deleteSavingsGoal(id: Long) = mutate { repository.deleteSavingsGoal(id) }
    fun saveBankAccount(name: String, accountType: String, initialBalance: Double, overdraftLimit: Double) =
        mutate { repository.saveBankAccount(name, accountType, initialBalance, overdraftLimit) }
    fun deleteBankAccount(id: Long) = mutate { repository.deleteBankAccount(id) }
    fun completePomodoro(task: Task?, minutes: Int) = mutate { repository.completePomodoro(task, minutes) }

    private fun mutate(block: () -> Unit) {
        viewModelScope.launch {
            val prevState = _state.value
            _state.value = prevState.copy(loading = true, error = null)
            runCatching {
                withContext(Dispatchers.IO) {
                    block()
                    repository.loadSnapshot() to repository.loadCalendarMonth(prevState.calendarMonth.monthKey)
                }
            }.onSuccess { (snapshot, calendar) ->
                _state.value = BonagerUiState(
                    loading = false,
                    snapshot = snapshot,
                    calendarMonth = calendar,
                    calendarSelectedDate = prevState.calendarSelectedDate,
                )
            }.onFailure { error ->
                _state.value = _state.value.copy(
                    loading = false,
                    error = error.message ?: "Something went wrong.",
                )
            }
        }
    }
}
