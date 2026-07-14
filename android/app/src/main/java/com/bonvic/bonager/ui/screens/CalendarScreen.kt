package com.bonvic.bonager.ui.screens

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.aspectRatio
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.KeyboardArrowLeft
import androidx.compose.material.icons.automirrored.filled.KeyboardArrowRight
import androidx.compose.material.icons.filled.CheckCircle
import androidx.compose.material.icons.filled.EmojiEvents
import androidx.compose.material.icons.filled.LocalAtm
import androidx.compose.material.icons.filled.Task
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.bonvic.bonager.data.CalendarMonth
import com.bonvic.bonager.data.Dates
import com.bonvic.bonager.data.GoalCheckin
import com.bonvic.bonager.data.Task
import com.bonvic.bonager.data.TaskPriority
import com.bonvic.bonager.data.TaskStatus
import com.bonvic.bonager.data.FinanceTransaction
import com.bonvic.bonager.data.Note
import com.bonvic.bonager.data.TransactionKind
import com.bonvic.bonager.data.formatMoney
import com.bonvic.bonager.ui.BonagerViewModel
import com.bonvic.bonager.ui.theme.BonagerColors
import java.text.SimpleDateFormat
import java.util.Calendar
import java.util.Date
import java.util.Locale

@Composable
fun CalendarScreen(
    calendarMonth: CalendarMonth,
    selectedDate: String,
    viewModel: BonagerViewModel,
) {
    val monthLabel = remember(calendarMonth.monthKey) { formatMonthLabel(calendarMonth.monthKey) }
    val daysInGrid = remember(calendarMonth.monthKey) { buildMonthGrid(calendarMonth.monthKey) }

    val tasksForDay = calendarMonth.tasksByDate[selectedDate].orEmpty()
    val checkinsForDay = calendarMonth.checkInsByDate[selectedDate].orEmpty()
    val journalForDay = calendarMonth.journalByDate[selectedDate]
    val transactionsForDay = calendarMonth.transactionsByDate[selectedDate].orEmpty()
    val today = remember { Dates.toDateKey() }

    LazyColumn(
        modifier = Modifier
            .fillMaxSize()
            .background(BonagerColors.Background),
    ) {
        item {
            // Month navigation header
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 8.dp, vertical = 8.dp),
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.SpaceBetween,
            ) {
                IconButton(onClick = { viewModel.navigateCalendarMonth(prevMonth(calendarMonth.monthKey)) }) {
                    Icon(
                        Icons.AutoMirrored.Filled.KeyboardArrowLeft,
                        contentDescription = "Previous month",
                        tint = BonagerColors.Primary,
                    )
                }
                Text(
                    text = monthLabel,
                    style = MaterialTheme.typography.titleLarge,
                    color = BonagerColors.Ink,
                )
                IconButton(onClick = { viewModel.navigateCalendarMonth(nextMonth(calendarMonth.monthKey)) }) {
                    Icon(
                        Icons.AutoMirrored.Filled.KeyboardArrowRight,
                        contentDescription = "Next month",
                        tint = BonagerColors.Primary,
                    )
                }
            }

            // Day-of-week labels
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 12.dp),
                horizontalArrangement = Arrangement.SpaceAround,
            ) {
                listOf("Su", "Mo", "Tu", "We", "Th", "Fr", "Sa").forEach { label ->
                    Box(
                        modifier = Modifier.weight(1f),
                        contentAlignment = Alignment.Center,
                    ) {
                        Text(
                            text = label,
                            style = MaterialTheme.typography.labelSmall,
                            color = BonagerColors.Muted,
                            fontWeight = FontWeight.SemiBold,
                        )
                    }
                }
            }
            Spacer(Modifier.height(4.dp))

            // Calendar grid — 7 columns
            daysInGrid.chunked(7).forEach { week ->
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(horizontal = 12.dp),
                    horizontalArrangement = Arrangement.SpaceAround,
                ) {
                    week.forEach { dateKey ->
                        val inMonth = dateKey != null && dateKey.startsWith(calendarMonth.monthKey)
                        DayCell(
                            dateKey = dateKey,
                            isToday = dateKey == today,
                            isSelected = dateKey == selectedDate,
                            inCurrentMonth = inMonth,
                            hasTasks = inMonth && calendarMonth.tasksByDate[dateKey].orEmpty().isNotEmpty(),
                            hasCheckins = inMonth && calendarMonth.checkInsByDate[dateKey].orEmpty().isNotEmpty(),
                            hasJournal = inMonth && calendarMonth.journalByDate.containsKey(dateKey),
                            hasTransactions = inMonth && calendarMonth.transactionsByDate[dateKey].orEmpty().isNotEmpty(),
                            modifier = Modifier.weight(1f),
                            onClick = { if (dateKey != null && inMonth) viewModel.selectCalendarDate(dateKey) },
                        )
                    }
                    // pad remaining cells if week is short
                    repeat(7 - week.size) {
                        Box(Modifier.weight(1f))
                    }
                }
            }

            Spacer(Modifier.height(8.dp))
            HorizontalDivider(color = BonagerColors.BorderLight)
            Spacer(Modifier.height(8.dp))

            // Selected date header
            val displayDate = remember(selectedDate) { formatSelectedDate(selectedDate) }
            Text(
                text = displayDate,
                style = MaterialTheme.typography.titleMedium,
                color = BonagerColors.Ink,
                modifier = Modifier.padding(horizontal = 16.dp, vertical = 4.dp),
            )
        }

        if (tasksForDay.isEmpty() && checkinsForDay.isEmpty() && journalForDay == null && transactionsForDay.isEmpty()) {
            item {
                Box(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(vertical = 32.dp),
                    contentAlignment = Alignment.Center,
                ) {
                    Text(
                        "No activity on this day",
                        style = MaterialTheme.typography.bodyMedium,
                        color = BonagerColors.Muted,
                    )
                }
            }
        }

        if (tasksForDay.isNotEmpty()) {
            item {
                SectionLabel("Tasks")
            }
            items(tasksForDay) { task ->
                TaskEventRow(task)
            }
        }

        if (checkinsForDay.isNotEmpty()) {
            item {
                SectionLabel("Goals")
            }
            items(checkinsForDay) { checkin ->
                GoalCheckinRow(checkin)
            }
        }

        if (journalForDay != null) {
            item {
                SectionLabel("Journal")
                JournalRow(journalForDay)
            }
        }

        if (transactionsForDay.isNotEmpty()) {
            item {
                SectionLabel("Finance")
            }
            items(transactionsForDay) { tx ->
                TransactionEventRow(tx)
            }
        }

        item { Spacer(Modifier.height(24.dp)) }
    }
}

@Composable
private fun DayCell(
    dateKey: String?,
    isToday: Boolean,
    isSelected: Boolean,
    inCurrentMonth: Boolean,
    hasTasks: Boolean,
    hasCheckins: Boolean,
    hasJournal: Boolean,
    hasTransactions: Boolean,
    modifier: Modifier = Modifier,
    onClick: () -> Unit,
) {
    val dayNumber = dateKey?.takeLast(2)?.trimStart('0') ?: ""
    val bgColor = when {
        isSelected -> BonagerColors.Primary
        isToday -> BonagerColors.PrimaryLight
        else -> Color.Transparent
    }
    val textColor = when {
        isSelected -> BonagerColors.Surface
        inCurrentMonth -> BonagerColors.Ink
        else -> BonagerColors.BorderLight
    }

    Column(
        modifier = modifier
            .aspectRatio(0.75f)
            .clip(RoundedCornerShape(8.dp))
            .background(bgColor)
            .clickable(enabled = inCurrentMonth, onClick = onClick)
            .padding(vertical = 4.dp),
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.Center,
    ) {
        Text(
            text = dayNumber,
            style = MaterialTheme.typography.labelMedium,
            color = textColor,
            fontWeight = if (isToday || isSelected) FontWeight.Bold else FontWeight.Normal,
        )
        // Activity dots
        if (hasTasks || hasCheckins || hasJournal || hasTransactions) {
            Row(
                horizontalArrangement = Arrangement.spacedBy(2.dp),
                modifier = Modifier.padding(top = 2.dp),
            ) {
                if (hasTasks) ActivityDot(if (isSelected) BonagerColors.Surface else BonagerColors.Primary)
                if (hasCheckins) ActivityDot(if (isSelected) BonagerColors.Surface else BonagerColors.Plum)
                if (hasJournal) ActivityDot(if (isSelected) BonagerColors.Surface else BonagerColors.Success)
                if (hasTransactions) ActivityDot(if (isSelected) BonagerColors.Surface else BonagerColors.Accent)
            }
        }
    }
}

@Composable
private fun ActivityDot(color: Color) {
    Box(
        Modifier
            .size(4.dp)
            .clip(CircleShape)
            .background(color),
    )
}

@Composable
private fun SectionLabel(text: String) {
    Text(
        text = text.uppercase(),
        style = MaterialTheme.typography.labelSmall,
        color = BonagerColors.Muted,
        letterSpacing = 0.8.sp,
        modifier = Modifier.padding(start = 16.dp, end = 16.dp, top = 12.dp, bottom = 4.dp),
    )
}

@Composable
private fun TaskEventRow(task: Task) {
    Surface(
        modifier = Modifier
            .fillMaxWidth()
            .padding(horizontal = 12.dp, vertical = 3.dp),
        shape = RoundedCornerShape(10.dp),
        color = BonagerColors.Surface,
        tonalElevation = 0.dp,
        shadowElevation = 0.dp,
    ) {
        Row(
            modifier = Modifier.padding(horizontal = 12.dp, vertical = 10.dp),
            verticalAlignment = Alignment.CenterVertically,
        ) {
            Icon(
                Icons.Filled.Task,
                contentDescription = null,
                tint = priorityColor(task.priority),
                modifier = Modifier.size(18.dp),
            )
            Spacer(Modifier.width(10.dp))
            Column(Modifier.weight(1f)) {
                Text(
                    text = task.title,
                    style = MaterialTheme.typography.bodyMedium,
                    color = BonagerColors.Ink,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis,
                )
                val subtitle = buildTaskSubtitle(task)
                if (subtitle.isNotEmpty()) {
                    Text(
                        text = subtitle,
                        style = MaterialTheme.typography.labelSmall,
                        color = BonagerColors.Muted,
                    )
                }
            }
            Spacer(Modifier.width(8.dp))
            StatusChip(task.status)
        }
    }
}

@Composable
private fun GoalCheckinRow(checkin: GoalCheckin) {
    val goalColor = runCatching { Color(android.graphics.Color.parseColor(checkin.goalColor)) }
        .getOrDefault(BonagerColors.Plum)

    Surface(
        modifier = Modifier
            .fillMaxWidth()
            .padding(horizontal = 12.dp, vertical = 3.dp),
        shape = RoundedCornerShape(10.dp),
        color = BonagerColors.Surface,
        tonalElevation = 0.dp,
    ) {
        Row(
            modifier = Modifier.padding(horizontal = 12.dp, vertical = 10.dp),
            verticalAlignment = Alignment.CenterVertically,
        ) {
            Box(
                Modifier
                    .size(10.dp)
                    .clip(CircleShape)
                    .background(goalColor),
            )
            Spacer(Modifier.width(10.dp))
            Icon(
                Icons.Filled.EmojiEvents,
                contentDescription = null,
                tint = BonagerColors.Plum,
                modifier = Modifier.size(16.dp),
            )
            Spacer(Modifier.width(8.dp))
            Text(
                text = checkin.goalTitle,
                style = MaterialTheme.typography.bodyMedium,
                color = BonagerColors.Ink,
                modifier = Modifier.weight(1f),
                maxLines = 1,
                overflow = TextOverflow.Ellipsis,
            )
            if (checkin.streak > 0) {
                Text(
                    text = "🔥 ${checkin.streak}",
                    style = MaterialTheme.typography.labelSmall,
                    color = BonagerColors.Accent,
                )
            }
        }
    }
}

@Composable
private fun JournalRow(note: Note) {
    Surface(
        modifier = Modifier
            .fillMaxWidth()
            .padding(horizontal = 12.dp, vertical = 3.dp),
        shape = RoundedCornerShape(10.dp),
        color = BonagerColors.Surface,
        tonalElevation = 0.dp,
    ) {
        Column(Modifier.padding(horizontal = 12.dp, vertical = 10.dp)) {
            Row(verticalAlignment = Alignment.CenterVertically) {
                if (!note.mood.isNullOrEmpty()) {
                    Text(note.mood, style = MaterialTheme.typography.bodyMedium)
                    Spacer(Modifier.width(6.dp))
                }
                Text(
                    text = "Journal",
                    style = MaterialTheme.typography.labelMedium,
                    color = BonagerColors.Success,
                    fontWeight = FontWeight.SemiBold,
                )
            }
            if (note.body.isNotEmpty()) {
                Spacer(Modifier.height(2.dp))
                Text(
                    text = note.body.take(80),
                    style = MaterialTheme.typography.bodySmall,
                    color = BonagerColors.Muted,
                    maxLines = 2,
                    overflow = TextOverflow.Ellipsis,
                )
            }
        }
    }
}

@Composable
private fun TransactionEventRow(tx: FinanceTransaction) {
    val amountColor = when (tx.kind) {
        TransactionKind.INCOME -> BonagerColors.Success
        TransactionKind.EXPENSE -> BonagerColors.Danger
        else -> BonagerColors.Muted
    }
    val sign = when (tx.kind) {
        TransactionKind.INCOME -> "+"
        TransactionKind.EXPENSE -> "-"
        else -> ""
    }

    Surface(
        modifier = Modifier
            .fillMaxWidth()
            .padding(horizontal = 12.dp, vertical = 3.dp),
        shape = RoundedCornerShape(10.dp),
        color = BonagerColors.Surface,
        tonalElevation = 0.dp,
    ) {
        Row(
            modifier = Modifier.padding(horizontal = 12.dp, vertical = 10.dp),
            verticalAlignment = Alignment.CenterVertically,
        ) {
            Icon(
                Icons.Filled.LocalAtm,
                contentDescription = null,
                tint = amountColor,
                modifier = Modifier.size(18.dp),
            )
            Spacer(Modifier.width(10.dp))
            Column(Modifier.weight(1f)) {
                Text(
                    text = tx.category,
                    style = MaterialTheme.typography.bodyMedium,
                    color = BonagerColors.Ink,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis,
                )
                if (!tx.note.isNullOrEmpty()) {
                    Text(
                        text = tx.note,
                        style = MaterialTheme.typography.labelSmall,
                        color = BonagerColors.Muted,
                        maxLines = 1,
                        overflow = TextOverflow.Ellipsis,
                    )
                }
            }
            Spacer(Modifier.width(8.dp))
            Text(
                text = "$sign${formatMoney(tx.amount)}",
                style = MaterialTheme.typography.labelMedium,
                color = amountColor,
                fontWeight = FontWeight.SemiBold,
            )
        }
    }
}

@Composable
private fun StatusChip(status: TaskStatus) {
    val (bg, fg, label) = when (status) {
        TaskStatus.DONE -> Triple(BonagerColors.SuccessLight, BonagerColors.Success, "Done")
        TaskStatus.IN_PROGRESS -> Triple(BonagerColors.BlueLight, BonagerColors.Blue, "In progress")
        TaskStatus.OPEN -> Triple(BonagerColors.BorderLight, BonagerColors.Muted, "Open")
    }
    Box(
        modifier = Modifier
            .clip(RoundedCornerShape(20.dp))
            .background(bg)
            .padding(horizontal = 8.dp, vertical = 3.dp),
    ) {
        Text(
            text = label,
            style = MaterialTheme.typography.labelSmall,
            color = fg,
            fontWeight = FontWeight.SemiBold,
        )
    }
}

private fun priorityColor(priority: TaskPriority): Color = when (priority) {
    TaskPriority.HIGH -> BonagerColors.Danger
    TaskPriority.MEDIUM -> BonagerColors.Accent
    TaskPriority.LOW -> BonagerColors.Muted
}

private fun buildTaskSubtitle(task: Task): String {
    val parts = mutableListOf<String>()
    task.dueAt?.let { parts.add("Due ${Dates.formatShortDate(it)}") }
    task.clientName?.let { parts.add(it) }
    return parts.joinToString(" · ")
}

/** Returns a list of 35 or 42 nullable date keys (nulls = padding cells before first day). */
private fun buildMonthGrid(monthKey: String): List<String?> {
    val cal = Calendar.getInstance().apply {
        val parts = monthKey.split("-")
        set(Calendar.YEAR, parts[0].toInt())
        set(Calendar.MONTH, parts[1].toInt() - 1)
        set(Calendar.DAY_OF_MONTH, 1)
    }
    val firstDayOfWeek = cal.get(Calendar.DAY_OF_WEEK) - 1 // 0=Sun
    val daysInMonth = cal.getActualMaximum(Calendar.DAY_OF_MONTH)
    val total = firstDayOfWeek + daysInMonth
    val cells = ArrayList<String?>(total + (7 - total % 7) % 7)
    repeat(firstDayOfWeek) { cells.add(null) }
    for (day in 1..daysInMonth) {
        cells.add("$monthKey-${day.toString().padStart(2, '0')}")
    }
    // pad to complete last week
    while (cells.size % 7 != 0) cells.add(null)
    return cells
}

private fun formatMonthLabel(monthKey: String): String {
    return runCatching {
        val date = SimpleDateFormat("yyyy-MM", Locale.getDefault()).parse(monthKey)!!
        SimpleDateFormat("MMMM yyyy", Locale.getDefault()).format(date)
    }.getOrElse { monthKey }
}

private fun formatSelectedDate(dateKey: String): String {
    return runCatching {
        val date = SimpleDateFormat("yyyy-MM-dd", Locale.getDefault()).parse(dateKey)!!
        SimpleDateFormat("EEEE, MMMM d", Locale.getDefault()).format(date)
    }.getOrElse { dateKey }
}

private fun prevMonth(monthKey: String): String {
    val cal = Calendar.getInstance().apply {
        val parts = monthKey.split("-")
        set(Calendar.YEAR, parts[0].toInt())
        set(Calendar.MONTH, parts[1].toInt() - 1)
        add(Calendar.MONTH, -1)
    }
    return SimpleDateFormat("yyyy-MM", Locale.US).format(cal.time)
}

private fun nextMonth(monthKey: String): String {
    val cal = Calendar.getInstance().apply {
        val parts = monthKey.split("-")
        set(Calendar.YEAR, parts[0].toInt())
        set(Calendar.MONTH, parts[1].toInt() - 1)
        add(Calendar.MONTH, 1)
    }
    return SimpleDateFormat("yyyy-MM", Locale.US).format(cal.time)
}
