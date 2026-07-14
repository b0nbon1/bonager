package com.bonvic.bonager.ui.screens

import androidx.compose.animation.animateColorAsState
import androidx.compose.animation.core.tween
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.LocalFireDepartment
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.core.graphics.toColorInt
import com.bonvic.bonager.data.AppSnapshot
import com.bonvic.bonager.ui.BonagerViewModel
import com.bonvic.bonager.ui.components.BonagerCard
import com.bonvic.bonager.ui.components.BonagerField
import com.bonvic.bonager.ui.components.CompactButton
import com.bonvic.bonager.ui.components.DeleteButton
import com.bonvic.bonager.ui.components.EmptyState
import com.bonvic.bonager.ui.components.Page
import com.bonvic.bonager.ui.components.PrimaryButton
import com.bonvic.bonager.ui.components.SectionTitle
import com.bonvic.bonager.ui.theme.BonagerColors

private val goalColors = listOf("#0C7C72", "#3568A6", "#7C5C86", "#E5A11A", "#2E7D4F")

@Composable
fun GoalsScreen(snapshot: AppSnapshot, viewModel: BonagerViewModel) {
    var title by rememberSaveable { mutableStateOf("") }
    var color by rememberSaveable { mutableStateOf(goalColors.first()) }

    Page("Goals", "Track the things you want to repeat and protect your streaks.") {
        BonagerCard {
            SectionTitle("New daily goal")
            BonagerField("Goal", title, { title = it }, placeholder = "Read, exercise, ship one thing...")
            Row(horizontalArrangement = Arrangement.spacedBy(10.dp), verticalAlignment = Alignment.CenterVertically) {
                goalColors.forEach { swatch ->
                    val selected = swatch == color
                    val borderColor by animateColorAsState(
                        targetValue = if (selected) BonagerColors.Ink else Color.Transparent,
                        animationSpec = tween(180),
                        label = "swatchBorder",
                    )
                    Box(
                        Modifier
                            .size(if (selected) 38.dp else 34.dp)
                            .background(Color(swatch.toColorInt()), CircleShape)
                            .border(2.5.dp, borderColor, CircleShape)
                            .clickable { color = swatch },
                    )
                }
            }
            PrimaryButton("Add goal", {
                viewModel.saveGoal(title, color)
                title = ""
                color = goalColors.first()
            }, Modifier.fillMaxWidth())
        }

        BonagerCard {
            SectionTitle("Streaks")
            if (snapshot.goals.isEmpty()) {
                EmptyState("No goals yet", "Add a daily habit, then check it off once per day to build a streak.")
            } else {
                Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                    snapshot.goals.forEach { goal ->
                        val goalColor = Color(goal.color.toColorInt())
                        val bgColor = goalColor.copy(alpha = 0.08f)
                        Row(
                            Modifier
                                .fillMaxWidth()
                                .background(bgColor, RoundedCornerShape(12.dp))
                                .padding(12.dp),
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.spacedBy(12.dp),
                        ) {
                            Surface(
                                modifier = Modifier
                                    .width(4.dp)
                                    .size(width = 4.dp, height = 48.dp),
                                shape = RoundedCornerShape(4.dp),
                                color = goalColor,
                            ) {}
                            Column(Modifier.weight(1f), verticalArrangement = Arrangement.spacedBy(4.dp)) {
                                Text(goal.title, style = MaterialTheme.typography.titleSmall, color = BonagerColors.Ink)
                                Row(
                                    verticalAlignment = Alignment.CenterVertically,
                                    horizontalArrangement = Arrangement.spacedBy(4.dp),
                                ) {
                                    if (goal.currentStreak > 0) {
                                        Icon(
                                            Icons.Filled.LocalFireDepartment,
                                            contentDescription = null,
                                            tint = BonagerColors.Accent,
                                            modifier = Modifier.size(14.dp),
                                        )
                                    }
                                    Text(
                                        "${goal.currentStreak} day streak · ${goal.totalCheckIns} check-ins",
                                        style = MaterialTheme.typography.bodySmall,
                                        color = BonagerColors.Muted,
                                    )
                                }
                            }
                            CompactButton(
                                if (goal.checkedToday) "Done ✓" else "Check in",
                                { viewModel.checkInGoal(goal.id) },
                                active = goal.checkedToday,
                                enabled = !goal.checkedToday,
                            )
                            DeleteButton { viewModel.deleteGoal(goal.id) }
                        }
                    }
                }
            }
        }
    }
}
