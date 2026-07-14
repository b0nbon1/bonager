package com.bonvic.bonager.ui.screens

import androidx.compose.animation.AnimatedContent
import androidx.compose.animation.animateColorAsState
import androidx.compose.animation.core.tween
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.togetherWith
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.CheckCircle
import androidx.compose.material.icons.outlined.Circle
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableLongStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.bonvic.bonager.data.AppSnapshot
import com.bonvic.bonager.data.Dates
import com.bonvic.bonager.data.TaskStatus
import com.bonvic.bonager.data.formatMoney
import com.bonvic.bonager.ui.BonagerViewModel
import com.bonvic.bonager.ui.components.BonagerCard
import com.bonvic.bonager.ui.components.CompactButton
import com.bonvic.bonager.ui.components.EmptyState
import com.bonvic.bonager.ui.components.Eyebrow
import com.bonvic.bonager.ui.components.HorizontalPills
import com.bonvic.bonager.ui.components.ListCopy
import com.bonvic.bonager.ui.components.MetricTile
import com.bonvic.bonager.ui.components.Page
import com.bonvic.bonager.ui.components.PrimaryButton
import com.bonvic.bonager.ui.components.SecondaryButton
import com.bonvic.bonager.ui.components.SectionTitle
import com.bonvic.bonager.ui.components.SegmentedControl
import com.bonvic.bonager.ui.theme.BonagerColors
import kotlinx.coroutines.delay

private enum class TimerMode(val seconds: Int, val label: String) {
    FOCUS(25 * 60, "Work block"),
    SHORT(5 * 60, "Short break"),
    LONG(15 * 60, "Long break"),
}

@Composable
fun TodayScreen(snapshot: AppSnapshot, viewModel: BonagerViewModel) {
    var mode by rememberSaveable { mutableStateOf(TimerMode.FOCUS) }
    var secondsLeft by rememberSaveable { mutableIntStateOf(mode.seconds) }
    var running by rememberSaveable { mutableStateOf(false) }
    var selectedTaskId by rememberSaveable { mutableLongStateOf(-1L) }
    val activeTasks = remember(snapshot.tasks) { snapshot.tasks.filter { it.status != TaskStatus.DONE } }
    val selectedTask = activeTasks.firstOrNull { it.id == selectedTaskId } ?: activeTasks.firstOrNull()
    val todayTasks = snapshot.tasks.filter {
        it.status != TaskStatus.DONE && (Dates.isToday(it.dueAt) || Dates.isToday(it.reminderAt))
    }

    LaunchedEffect(mode) {
        running = false
        secondsLeft = mode.seconds
    }
    LaunchedEffect(running) {
        if (!running) return@LaunchedEffect
        while (running && secondsLeft > 0) {
            delay(1_000)
            secondsLeft -= 1
        }
        if (running && secondsLeft == 0) {
            running = false
            if (mode == TimerMode.FOCUS) {
                viewModel.completePomodoro(selectedTask, mode.seconds / 60)
                mode = TimerMode.SHORT
            }
        }
    }

    val timerBg by animateColorAsState(
        targetValue = when (mode) {
            TimerMode.FOCUS -> BonagerColors.PrimaryLight
            TimerMode.SHORT -> BonagerColors.SuccessLight
            TimerMode.LONG -> BonagerColors.BlueLight
        },
        animationSpec = tween(400),
        label = "timerBg",
    )
    val timerTextColor by animateColorAsState(
        targetValue = when (mode) {
            TimerMode.FOCUS -> BonagerColors.PrimaryDark
            TimerMode.SHORT -> BonagerColors.Success
            TimerMode.LONG -> BonagerColors.Blue
        },
        animationSpec = tween(400),
        label = "timerText",
    )

    Page(
        title = "Today",
        subtitle = "Focus timer, urgent reminders, and the numbers that matter now.",
    ) {
        Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(12.dp)) {
            MetricTile("Open tasks", snapshot.dashboard.openTasks.toString(), BonagerColors.Primary, Modifier.weight(1f))
            MetricTile("Due today", snapshot.dashboard.dueToday.toString(), BonagerColors.Accent, Modifier.weight(1f))
        }
        Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(12.dp)) {
            MetricTile("Unpaid work", formatMoney(snapshot.dashboard.billableUnpaid), BonagerColors.Blue, Modifier.weight(1f))
            MetricTile("Goal streaks", snapshot.dashboard.activeStreaks.toString(), BonagerColors.Plum, Modifier.weight(1f))
        }

        BonagerCard {
            Column(verticalArrangement = Arrangement.spacedBy(2.dp)) {
                Eyebrow("Pomodoro")
                SectionTitle(selectedTask?.title ?: "Focus session")
            }
            SegmentedControl(
                selected = mode,
                options = listOf("Focus" to TimerMode.FOCUS, "Short" to TimerMode.SHORT, "Long" to TimerMode.LONG),
                onSelected = { mode = it },
            )
            Surface(
                modifier = Modifier.fillMaxWidth(),
                shape = RoundedCornerShape(14.dp),
                color = timerBg,
            ) {
                Column(
                    modifier = Modifier.padding(vertical = 28.dp),
                    horizontalAlignment = Alignment.CenterHorizontally,
                    verticalArrangement = Arrangement.spacedBy(6.dp),
                ) {
                    AnimatedContent(
                        targetState = formatTimer(secondsLeft),
                        transitionSpec = { fadeIn(tween(120)) togetherWith fadeOut(tween(120)) },
                        label = "timer",
                    ) { timeStr ->
                        Text(
                            timeStr,
                            color = timerTextColor,
                            fontSize = 60.sp,
                            fontWeight = FontWeight.Black,
                            letterSpacing = (-1).sp,
                        )
                    }
                    Text(mode.label, style = MaterialTheme.typography.labelLarge, color = timerTextColor.copy(alpha = 0.7f))
                }
            }
            Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(12.dp)) {
                PrimaryButton(if (running) "Pause" else "Start", { running = !running }, Modifier.weight(1f))
                SecondaryButton("Reset", {
                    running = false
                    secondsLeft = mode.seconds
                }, Modifier.weight(1f))
            }
            if (activeTasks.isNotEmpty()) {
                HorizontalPills {
                    activeTasks.take(5).forEach { task ->
                        val active = task.id == selectedTask?.id
                        CompactButton(task.title, { selectedTaskId = task.id }, active = active)
                    }
                }
            } else {
                Text("Add tasks to attach focus time.", style = MaterialTheme.typography.bodySmall, color = BonagerColors.Muted)
            }
        }

        BonagerCard {
            Column(verticalArrangement = Arrangement.spacedBy(2.dp)) {
                Eyebrow("Now")
                SectionTitle("Due today")
            }
            if (todayTasks.isEmpty()) {
                EmptyState("Nothing due today", "Your day is clear here. Pull in one open task and start the timer.")
            } else {
                Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                    todayTasks.forEach { task ->
                        Row(
                            Modifier
                                .fillMaxWidth()
                                .background(BonagerColors.SurfaceElevated, RoundedCornerShape(10.dp))
                                .clickable { viewModel.toggleTask(task) }
                                .padding(12.dp),
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.spacedBy(12.dp),
                        ) {
                            Icon(
                                imageVector = Icons.Filled.CheckCircle,
                                contentDescription = "Complete",
                                tint = BonagerColors.Primary,
                                modifier = Modifier.size(20.dp),
                            )
                            ListCopy(task.title, Dates.formatDateTime(task.reminderAt ?: task.dueAt), Modifier.weight(1f))
                        }
                    }
                }
            }
        }
    }
}

private fun formatTimer(totalSeconds: Int): String =
    "%02d:%02d".format(totalSeconds / 60, totalSeconds % 60)
