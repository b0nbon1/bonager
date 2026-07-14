package com.bonvic.bonager.ui

import androidx.compose.animation.AnimatedContent
import androidx.compose.animation.core.EaseInOut
import androidx.compose.animation.core.RepeatMode
import androidx.compose.animation.core.animateFloat
import androidx.compose.animation.core.infiniteRepeatable
import androidx.compose.animation.core.rememberInfiniteTransition
import androidx.compose.animation.core.tween
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.slideInVertically
import androidx.compose.animation.slideOutVertically
import androidx.compose.animation.togetherWith
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.offset
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.Note
import androidx.compose.material.icons.automirrored.outlined.Note
import androidx.compose.material.icons.filled.AttachMoney
import androidx.compose.material.icons.filled.CalendarMonth
import androidx.compose.material.icons.filled.CheckCircle
import androidx.compose.material.icons.filled.EmojiEvents
import androidx.compose.material.icons.filled.Menu
import androidx.compose.material.icons.filled.WbSunny
import androidx.compose.material.icons.outlined.AttachMoney
import androidx.compose.material.icons.outlined.CalendarMonth
import androidx.compose.material.icons.outlined.CheckCircle
import androidx.compose.material.icons.outlined.EmojiEvents
import androidx.compose.material.icons.outlined.WbSunny
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.DrawerValue
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.FloatingActionButton
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.ModalDrawerSheet
import androidx.compose.material3.ModalNavigationDrawer
import androidx.compose.material3.NavigationBar
import androidx.compose.material3.NavigationBarItem
import androidx.compose.material3.NavigationBarItemDefaults
import androidx.compose.material3.NavigationDrawerItem
import androidx.compose.material3.NavigationDrawerItemDefaults
import androidx.compose.material3.Scaffold
import androidx.compose.material3.SnackbarHost
import androidx.compose.material3.SnackbarHostState
import androidx.compose.material3.Text
import androidx.compose.material3.TopAppBar
import androidx.compose.material3.TopAppBarDefaults
import androidx.compose.material3.rememberDrawerState
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.drawBehind
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.lifecycle.viewmodel.compose.viewModel
import com.bonvic.bonager.ui.screens.CalendarScreen
import com.bonvic.bonager.ui.screens.FinanceScreen
import com.bonvic.bonager.ui.screens.GoalsScreen
import com.bonvic.bonager.ui.screens.NotesScreen
import com.bonvic.bonager.ui.screens.TasksScreen
import com.bonvic.bonager.ui.screens.TodayScreen
import com.bonvic.bonager.ui.theme.BonagerColors
import com.bonvic.bonager.ui.theme.BonagerTheme
import kotlinx.coroutines.launch

private enum class Tab(
    val label: String,
    val icon: ImageVector,
    val iconSelected: ImageVector,
    val inBottomBar: Boolean,
) {
    TODAY("Today", Icons.Outlined.WbSunny, Icons.Filled.WbSunny, inBottomBar = false),
    TASKS("Tasks", Icons.Outlined.CheckCircle, Icons.Filled.CheckCircle, inBottomBar = true),
    CALENDAR("Calendar", Icons.Outlined.CalendarMonth, Icons.Filled.CalendarMonth, inBottomBar = false),
    NOTES("Notes", Icons.AutoMirrored.Outlined.Note, Icons.AutoMirrored.Filled.Note, inBottomBar = false),
    GOALS("Goals", Icons.Outlined.EmojiEvents, Icons.Filled.EmojiEvents, inBottomBar = false),
    FINANCE("Finance", Icons.Outlined.AttachMoney, Icons.Filled.AttachMoney, inBottomBar = true),
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun BonagerApp(viewModel: BonagerViewModel = viewModel()) {
    BonagerTheme {
        val state by viewModel.state.collectAsStateWithLifecycle()
        var selected by remember { mutableStateOf(Tab.TODAY) }
        val snackbar = remember { SnackbarHostState() }
        val drawerState = rememberDrawerState(DrawerValue.Closed)
        val scope = rememberCoroutineScope()

        LaunchedEffect(state.error) {
            state.error?.let {
                snackbar.showSnackbar(it)
                viewModel.clearError()
            }
        }

        ModalNavigationDrawer(
            drawerState = drawerState,
            drawerContent = {
                ModalDrawerSheet(
                    drawerContainerColor = BonagerColors.Tab,
                    drawerContentColor = BonagerColors.Ink,
                ) {
                    Spacer(Modifier.height(24.dp))
                    Text(
                        "Bonager",
                        modifier = Modifier.padding(horizontal = 24.dp),
                        style = MaterialTheme.typography.titleLarge,
                        color = BonagerColors.Primary,
                    )
                    Spacer(Modifier.height(8.dp))
                    HorizontalDivider(color = BonagerColors.Border)
                    Spacer(Modifier.height(8.dp))
                    Tab.entries.forEach { tab ->
                        val isSelected = selected == tab
                        NavigationDrawerItem(
                            icon = {
                                Icon(
                                    imageVector = if (isSelected) tab.iconSelected else tab.icon,
                                    contentDescription = tab.label,
                                    modifier = Modifier.size(22.dp),
                                )
                            },
                            label = {
                                Text(tab.label, style = MaterialTheme.typography.labelLarge)
                            },
                            selected = isSelected,
                            onClick = {
                                selected = tab
                                scope.launch { drawerState.close() }
                            },
                            colors = NavigationDrawerItemDefaults.colors(
                                selectedContainerColor = BonagerColors.PrimaryLight,
                                selectedIconColor = BonagerColors.Primary,
                                selectedTextColor = BonagerColors.Primary,
                                unselectedIconColor = BonagerColors.Muted,
                                unselectedTextColor = BonagerColors.Muted,
                            ),
                            modifier = Modifier.padding(NavigationDrawerItemDefaults.ItemPadding),
                        )
                    }
                }
            },
        ) {
            Scaffold(
                containerColor = BonagerColors.Background,
                snackbarHost = { SnackbarHost(snackbar) },
                topBar = {
                    TopAppBar(
                        title = {
                            Text(
                                selected.label,
                                style = MaterialTheme.typography.titleMedium,
                                color = BonagerColors.Ink,
                            )
                        },
                        navigationIcon = {
                            IconButton(onClick = { scope.launch { drawerState.open() } }) {
                                Icon(
                                    imageVector = Icons.Filled.Menu,
                                    contentDescription = "Open menu",
                                    tint = BonagerColors.Ink,
                                    modifier = Modifier.size(22.dp),
                                )
                            }
                        },
                        colors = TopAppBarDefaults.topAppBarColors(
                            containerColor = BonagerColors.Tab,
                            scrolledContainerColor = BonagerColors.Tab,
                        ),
                    )
                },
                bottomBar = {
                    BonagerBottomBar(
                        selected = selected,
                        onSelect = { selected = it },
                    )
                },
            ) { padding ->
                Box(Modifier.fillMaxSize().padding(padding)) {
                    AnimatedContent(
                        targetState = selected,
                        transitionSpec = {
                            (fadeIn() + slideInVertically { it / 30 }) togetherWith
                                (fadeOut() + slideOutVertically { -it / 30 })
                        },
                        label = "screenTransition",
                    ) { tab ->
                        when (tab) {
                            Tab.TODAY -> TodayScreen(state.snapshot, viewModel)
                            Tab.TASKS -> TasksScreen(state.snapshot, viewModel)
                            Tab.CALENDAR -> CalendarScreen(state.calendarMonth, state.calendarSelectedDate, viewModel)
                            Tab.NOTES -> NotesScreen(state.snapshot, viewModel)
                            Tab.GOALS -> GoalsScreen(state.snapshot, viewModel)
                            Tab.FINANCE -> FinanceScreen(state.snapshot, viewModel)
                        }
                    }
                    if (state.loading) {
                        Box(
                            Modifier.fillMaxSize(),
                            contentAlignment = Alignment.TopCenter,
                        ) {
                            CircularProgressIndicator(
                                modifier = Modifier.padding(top = 16.dp),
                                color = BonagerColors.Primary,
                                strokeWidth = 2.dp,
                            )
                        }
                    }
                }
            }
        }
    }
}

@Composable
private fun BonagerBottomBar(
    selected: Tab,
    onSelect: (Tab) -> Unit,
) {
    val infiniteTransition = rememberInfiniteTransition(label = "fabPulse")
    val pulse by infiniteTransition.animateFloat(
        initialValue = 0.92f,
        targetValue = 1.05f,
        animationSpec = infiniteRepeatable(
            animation = tween(900, easing = EaseInOut),
            repeatMode = RepeatMode.Reverse,
        ),
        label = "fabScale",
    )
    val notesActive = selected == Tab.NOTES
    val glowAlpha = if (notesActive) 0.45f else 0.28f

    Box(contentAlignment = Alignment.TopCenter) {
        NavigationBar(
            containerColor = BonagerColors.Tab,
            tonalElevation = 0.dp,
        ) {
            Tab.entries.filter { it.inBottomBar }.forEach { tab ->
                val isSelected = selected == tab
                NavigationBarItem(
                    selected = isSelected,
                    onClick = { onSelect(tab) },
                    icon = {
                        Icon(
                            imageVector = if (isSelected) tab.iconSelected else tab.icon,
                            contentDescription = tab.label,
                            modifier = Modifier.size(22.dp),
                        )
                    },
                    label = {
                        Text(
                            tab.label,
                            style = MaterialTheme.typography.labelSmall,
                            fontWeight = if (isSelected) FontWeight.Bold else FontWeight.Normal,
                        )
                    },
                    colors = NavigationBarItemDefaults.colors(
                        selectedIconColor = BonagerColors.Primary,
                        selectedTextColor = BonagerColors.Primary,
                        unselectedIconColor = BonagerColors.Muted,
                        unselectedTextColor = BonagerColors.Muted,
                        indicatorColor = BonagerColors.PrimaryLight,
                    ),
                )
            }
            Spacer(Modifier.weight(1f))
        }

        val glowColor = BonagerColors.Primary.copy(alpha = glowAlpha)
        FloatingActionButton(
            onClick = { onSelect(Tab.NOTES) },
            modifier = Modifier
                .offset(y = (-20).dp)
                .size(60.dp)
                .graphicsLayer { scaleX = pulse; scaleY = pulse }
                .drawBehind {
                    drawCircle(
                        color = glowColor,
                        radius = 52.dp.toPx(),
                    )
                },
            containerColor = BonagerColors.Primary,
            contentColor = BonagerColors.Surface,
        ) {
            Icon(
                imageVector = Icons.AutoMirrored.Filled.Note,
                contentDescription = "Quick note",
                modifier = Modifier.size(28.dp),
            )
        }
    }
}
