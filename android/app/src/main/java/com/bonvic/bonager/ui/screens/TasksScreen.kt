package com.bonvic.bonager.ui.screens

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.CheckCircle
import androidx.compose.material.icons.outlined.Circle
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.unit.dp
import com.bonvic.bonager.data.AppSnapshot
import com.bonvic.bonager.data.Dates
import com.bonvic.bonager.data.Task
import com.bonvic.bonager.data.TaskDraft
import com.bonvic.bonager.data.TaskLogDraft
import com.bonvic.bonager.data.TaskPriority
import com.bonvic.bonager.data.TaskStatus
import com.bonvic.bonager.data.formatMinutes
import com.bonvic.bonager.data.formatMoney
import com.bonvic.bonager.ui.BonagerViewModel
import com.bonvic.bonager.ui.components.BonagerCard
import com.bonvic.bonager.ui.components.BonagerField
import com.bonvic.bonager.ui.components.CompactButton
import com.bonvic.bonager.ui.components.DeleteButton
import com.bonvic.bonager.ui.components.EmptyState
import com.bonvic.bonager.ui.components.HorizontalPills
import com.bonvic.bonager.ui.components.ListCopy
import com.bonvic.bonager.ui.components.Page
import com.bonvic.bonager.ui.components.PrimaryButton
import com.bonvic.bonager.ui.components.SecondaryButton
import com.bonvic.bonager.ui.components.SectionTitle
import com.bonvic.bonager.ui.components.SegmentedControl
import com.bonvic.bonager.ui.components.StatusChip
import com.bonvic.bonager.ui.theme.BonagerColors

private fun priorityColor(p: TaskPriority) = when (p) {
    TaskPriority.HIGH -> BonagerColors.Danger
    TaskPriority.MEDIUM -> BonagerColors.Accent
    TaskPriority.LOW -> BonagerColors.Success
}

private fun priorityBg(p: TaskPriority) = when (p) {
    TaskPriority.HIGH -> BonagerColors.DangerLight
    TaskPriority.MEDIUM -> BonagerColors.AccentLight
    TaskPriority.LOW -> BonagerColors.SuccessLight
}

@Composable
fun TasksScreen(snapshot: AppSnapshot, viewModel: BonagerViewModel) {
    var editingId by rememberSaveable { mutableStateOf<Long?>(null) }
    var title by rememberSaveable { mutableStateOf("") }
    var notes by rememberSaveable { mutableStateOf("") }
    var priority by rememberSaveable { mutableStateOf(TaskPriority.MEDIUM) }
    var dueAt by rememberSaveable { mutableStateOf("") }
    var reminderAt by rememberSaveable { mutableStateOf("") }
    var clientName by rememberSaveable { mutableStateOf("") }
    var billable by rememberSaveable { mutableStateOf(false) }
    var hourlyRate by rememberSaveable { mutableStateOf("") }
    var estimate by rememberSaveable { mutableStateOf("") }

    var logTaskId by rememberSaveable { mutableStateOf<Long?>(null) }
    var logClient by rememberSaveable { mutableStateOf("") }
    var logTitle by rememberSaveable { mutableStateOf("") }
    var logMinutes by rememberSaveable { mutableStateOf("60") }
    var logAmount by rememberSaveable { mutableStateOf("") }
    var logPaid by rememberSaveable { mutableStateOf(false) }

    fun resetTaskForm() {
        editingId = null; title = ""; notes = ""; priority = TaskPriority.MEDIUM
        dueAt = ""; reminderAt = ""; clientName = ""; billable = false
        hourlyRate = ""; estimate = ""
    }

    fun editTask(task: Task) {
        editingId = task.id; title = task.title; notes = task.notes.orEmpty()
        priority = task.priority; dueAt = Dates.toLocalInput(task.dueAt)
        reminderAt = Dates.toLocalInput(task.reminderAt); clientName = task.clientName.orEmpty()
        billable = task.isBillable
        hourlyRate = task.hourlyRate.takeIf { it > 0 }?.toString().orEmpty()
        estimate = task.estimatedMinutes.takeIf { it > 0 }?.toString().orEmpty()
    }

    val billableTasks = snapshot.tasks.filter { it.isBillable && it.status != TaskStatus.DONE }
    val clientTotals = snapshot.taskLogs.groupBy { it.clientName ?: "No client" }.entries.take(4)

    Page("Tasks", "Plan work, set reminders, and track client time.") {
        BonagerCard {
            SectionTitle(if (editingId == null) "New task" else "Edit task")
            BonagerField("Task", title, { title = it })
            BonagerField("Notes", notes, { notes = it }, multiline = true)
            SegmentedControl(
                priority,
                listOf("Low" to TaskPriority.LOW, "Medium" to TaskPriority.MEDIUM, "High" to TaskPriority.HIGH),
                { priority = it },
            )
            Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(12.dp)) {
                BonagerField("Due", dueAt, { dueAt = it }, Modifier.weight(1f), "YYYY-MM-DD HH:mm")
                BonagerField("Reminder", reminderAt, { reminderAt = it }, Modifier.weight(1f), "YYYY-MM-DD HH:mm")
            }
            HorizontalPills {
                CompactButton("+3h", { reminderAt = Dates.quickReminder(3) })
                CompactButton("Tomorrow", { reminderAt = Dates.quickReminder(24) })
                CompactButton("Clear", { reminderAt = "" })
            }
            SecondaryButton(
                if (billable) "Billable client task ✓" else "Mark as billable",
                { billable = !billable },
                Modifier.fillMaxWidth(),
            )
            if (billable) {
                BonagerField("Client", clientName, { clientName = it })
                Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(12.dp)) {
                    BonagerField(
                        "Hourly rate", hourlyRate, { hourlyRate = it }, Modifier.weight(1f),
                        keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Decimal),
                    )
                    BonagerField(
                        "Estimate min", estimate, { estimate = it }, Modifier.weight(1f),
                        keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
                    )
                }
            }
            Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(12.dp)) {
                PrimaryButton(
                    if (editingId == null) "Add task" else "Update task",
                    {
                        viewModel.saveTask(
                            TaskDraft(
                                id = editingId,
                                title = title,
                                notes = notes,
                                priority = priority,
                                dueAt = Dates.inputToStorage(dueAt),
                                reminderAt = Dates.inputToStorage(reminderAt),
                                clientName = clientName,
                                isBillable = billable,
                                hourlyRate = hourlyRate.toDoubleOrNull() ?: 0.0,
                                estimatedMinutes = estimate.toIntOrNull() ?: 0,
                            ),
                        )
                        resetTaskForm()
                    },
                    Modifier.weight(1f),
                )
                if (editingId != null) SecondaryButton("Cancel", ::resetTaskForm, Modifier.weight(1f))
            }
        }

        BonagerCard {
            SectionTitle("Log client work")
            HorizontalPills {
                billableTasks.take(5).forEach { task ->
                    CompactButton(task.title, {
                        logTaskId = task.id
                        logClient = task.clientName.orEmpty()
                        logTitle = task.title
                        if (task.hourlyRate > 0 && (logMinutes.toIntOrNull() ?: 0) > 0) {
                            logAmount = "%.2f".format(task.hourlyRate * (logMinutes.toIntOrNull() ?: 0) / 60.0)
                        }
                    }, active = logTaskId == task.id)
                }
            }
            BonagerField("Client", logClient, { logClient = it })
            BonagerField("Work done", logTitle, { logTitle = it })
            Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(12.dp)) {
                BonagerField(
                    "Minutes", logMinutes, { logMinutes = it }, Modifier.weight(1f),
                    keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
                )
                BonagerField(
                    "Amount", logAmount, { logAmount = it }, Modifier.weight(1f),
                    keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Decimal),
                )
            }
            SecondaryButton(if (logPaid) "Paid ✓" else "Mark as paid", { logPaid = !logPaid }, Modifier.fillMaxWidth())
            PrimaryButton("Log work", {
                viewModel.saveTaskLog(
                    TaskLogDraft(
                        taskId = logTaskId,
                        clientName = logClient,
                        title = logTitle,
                        minutes = logMinutes.toIntOrNull() ?: 0,
                        amount = logAmount.toDoubleOrNull() ?: 0.0,
                        paid = logPaid,
                    ),
                )
                logTaskId = null; logClient = ""; logTitle = ""
                logMinutes = "60"; logAmount = ""; logPaid = false
            }, Modifier.fillMaxWidth())
            if (clientTotals.isNotEmpty()) {
                Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                    clientTotals.forEach { (client, logs) ->
                        Column(
                            Modifier
                                .fillMaxWidth()
                                .background(BonagerColors.SurfaceElevated, RoundedCornerShape(10.dp))
                                .padding(12.dp),
                            verticalArrangement = Arrangement.spacedBy(4.dp),
                        ) {
                            Text(client, style = MaterialTheme.typography.titleSmall, color = BonagerColors.Ink)
                            Text(
                                "${formatMoney(logs.sumOf { it.amount })} · ${formatMinutes(logs.sumOf { it.minutes })}",
                                style = MaterialTheme.typography.bodySmall,
                                color = BonagerColors.Muted,
                            )
                            Text(
                                "${formatMoney(logs.filterNot { it.paid }.sumOf { it.amount })} unpaid",
                                style = MaterialTheme.typography.labelSmall,
                                color = BonagerColors.Danger,
                                fontWeight = FontWeight.SemiBold,
                            )
                        }
                    }
                }
            }
        }

        BonagerCard {
            SectionTitle("Task list")
            if (snapshot.tasks.isEmpty()) {
                EmptyState("No tasks yet", "Add one task with a reminder, then use Today to focus on it.")
            } else {
                Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                    snapshot.tasks.forEach { task ->
                        Row(
                            Modifier
                                .fillMaxWidth()
                                .background(BonagerColors.SurfaceElevated, RoundedCornerShape(10.dp))
                                .padding(start = 4.dp, end = 8.dp, top = 8.dp, bottom = 8.dp),
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.spacedBy(4.dp),
                        ) {
                            IconButton(onClick = { viewModel.toggleTask(task) }) {
                                Icon(
                                    imageVector = if (task.status == TaskStatus.DONE) Icons.Filled.CheckCircle else Icons.Outlined.Circle,
                                    contentDescription = "Toggle",
                                    tint = if (task.status == TaskStatus.DONE) BonagerColors.Primary else BonagerColors.Muted,
                                )
                            }
                            Column(Modifier.weight(1f), verticalArrangement = Arrangement.spacedBy(4.dp)) {
                                ListCopy(
                                    task.title,
                                    "${task.clientName ?: "Personal"} · ${Dates.formatDateTime(task.reminderAt ?: task.dueAt)}",
                                    strike = task.status == TaskStatus.DONE,
                                )
                                StatusChip(
                                    task.priority.storage,
                                    priorityColor(task.priority),
                                    priorityBg(task.priority),
                                )
                            }
                            CompactButton("Edit", { editTask(task) })
                            DeleteButton { viewModel.deleteTask(task) }
                        }
                    }
                }
            }
        }

        BonagerCard {
            SectionTitle("Recent work logs")
            if (snapshot.taskLogs.isEmpty()) {
                EmptyState("No client work logged")
            } else {
                Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                    snapshot.taskLogs.take(8).forEach { log ->
                        Row(
                            Modifier
                                .fillMaxWidth()
                                .background(BonagerColors.SurfaceElevated, RoundedCornerShape(10.dp))
                                .padding(12.dp),
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.spacedBy(8.dp),
                        ) {
                            Column(Modifier.weight(1f)) {
                                ListCopy(
                                    log.title,
                                    "${log.clientName ?: "No client"} · ${formatMinutes(log.minutes)}",
                                )
                                if (!log.paid) {
                                    StatusChip("Unpaid", BonagerColors.Danger, BonagerColors.DangerLight)
                                }
                            }
                            Text(formatMoney(log.amount), style = MaterialTheme.typography.titleSmall, color = BonagerColors.Ink)
                            DeleteButton { viewModel.deleteTaskLog(log.id) }
                        }
                    }
                }
            }
        }
    }
}
