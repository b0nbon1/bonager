package com.bonvic.bonager.ui.components

import androidx.compose.animation.animateColorAsState
import androidx.compose.animation.core.Spring
import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.animation.core.spring
import androidx.compose.animation.core.tween
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.horizontalScroll
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.interaction.collectIsPressedAsState
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.ColumnScope
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.RowScope
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.imePadding
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.OutlinedTextFieldDefaults
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.scale
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextDecoration
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.bonvic.bonager.ui.theme.BonagerColors

@Composable
fun Page(
    title: String,
    subtitle: String,
    content: @Composable ColumnScope.() -> Unit,
) {
    Column(
        modifier = Modifier
            .fillMaxSize()
            .background(BonagerColors.Background)
            .verticalScroll(rememberScrollState())
            .imePadding()
            .padding(horizontal = 16.dp)
            .padding(top = 20.dp),
        verticalArrangement = Arrangement.spacedBy(16.dp),
    ) {
        Column(verticalArrangement = Arrangement.spacedBy(6.dp)) {
            Text(
                title,
                style = MaterialTheme.typography.headlineMedium,
                color = BonagerColors.Ink,
            )
            Text(
                subtitle,
                style = MaterialTheme.typography.bodyMedium,
                color = BonagerColors.Muted,
                lineHeight = 20.sp,
            )
        }
        content()
        Spacer(Modifier.height(24.dp))
    }
}

@Composable
fun BonagerCard(content: @Composable ColumnScope.() -> Unit) {
    Card(
        modifier = Modifier.fillMaxWidth(),
        shape = RoundedCornerShape(16.dp),
        colors = CardDefaults.cardColors(containerColor = BonagerColors.Surface),
        border = BorderStroke(1.dp, BonagerColors.BorderLight),
        elevation = CardDefaults.cardElevation(defaultElevation = 0.dp),
    ) {
        Column(
            modifier = Modifier.padding(20.dp),
            verticalArrangement = Arrangement.spacedBy(14.dp),
            content = content,
        )
    }
}

@Composable
fun SectionTitle(text: String) {
    Text(
        text,
        style = MaterialTheme.typography.titleLarge,
        color = BonagerColors.Ink,
    )
}

@Composable
fun Eyebrow(text: String) {
    Text(
        text.uppercase(),
        style = MaterialTheme.typography.labelSmall,
        color = BonagerColors.Muted,
        letterSpacing = 0.8.sp,
    )
}

@Composable
fun BonagerField(
    label: String,
    value: String,
    onValueChange: (String) -> Unit,
    modifier: Modifier = Modifier,
    placeholder: String = "",
    multiline: Boolean = false,
    keyboardOptions: KeyboardOptions = KeyboardOptions.Default,
) {
    Column(modifier, verticalArrangement = Arrangement.spacedBy(6.dp)) {
        Eyebrow(label)
        OutlinedTextField(
            value = value,
            onValueChange = onValueChange,
            modifier = Modifier.fillMaxWidth(),
            placeholder = {
                if (placeholder.isNotEmpty()) {
                    Text(placeholder, style = MaterialTheme.typography.bodyMedium, color = BonagerColors.Muted)
                }
            },
            textStyle = MaterialTheme.typography.bodyMedium.copy(color = BonagerColors.Ink),
            minLines = if (multiline) 4 else 1,
            maxLines = if (multiline) 7 else 1,
            keyboardOptions = keyboardOptions,
            shape = RoundedCornerShape(12.dp),
            colors = OutlinedTextFieldDefaults.colors(
                focusedBorderColor = BonagerColors.Primary,
                unfocusedBorderColor = BonagerColors.Border,
                focusedContainerColor = BonagerColors.Surface,
                unfocusedContainerColor = BonagerColors.SurfaceElevated,
                cursorColor = BonagerColors.Primary,
            ),
        )
    }
}

@Composable
fun <T> SegmentedControl(
    selected: T,
    options: List<Pair<String, T>>,
    onSelected: (T) -> Unit,
) {
    Row(
        Modifier
            .fillMaxWidth()
            .background(BonagerColors.SurfaceAlt, RoundedCornerShape(12.dp))
            .border(1.dp, BonagerColors.BorderLight, RoundedCornerShape(12.dp))
            .padding(4.dp),
        horizontalArrangement = Arrangement.spacedBy(4.dp),
    ) {
        options.forEach { (label, value) ->
            val active = value == selected
            val bgColor by animateColorAsState(
                targetValue = if (active) BonagerColors.Surface else Color.Transparent,
                animationSpec = tween(200),
                label = "segBg",
            )
            val textColor by animateColorAsState(
                targetValue = if (active) BonagerColors.Ink else BonagerColors.Muted,
                animationSpec = tween(200),
                label = "segText",
            )
            TextButton(
                onClick = { onSelected(value) },
                modifier = Modifier.weight(1f),
                colors = ButtonDefaults.textButtonColors(
                    containerColor = bgColor,
                    contentColor = textColor,
                ),
                shape = RoundedCornerShape(9.dp),
            ) {
                Text(label, style = MaterialTheme.typography.labelLarge, maxLines = 1)
            }
        }
    }
}

@Composable
fun PrimaryButton(
    label: String,
    onClick: () -> Unit,
    modifier: Modifier = Modifier,
    enabled: Boolean = true,
) {
    val interactionSource = remember { MutableInteractionSource() }
    val isPressed by interactionSource.collectIsPressedAsState()
    val scale by animateFloatAsState(
        targetValue = if (isPressed) 0.97f else 1f,
        animationSpec = spring(stiffness = Spring.StiffnessMedium),
        label = "btnScale",
    )
    Button(
        onClick = onClick,
        modifier = modifier.height(50.dp).scale(scale),
        enabled = enabled,
        interactionSource = interactionSource,
        shape = RoundedCornerShape(12.dp),
        colors = ButtonDefaults.buttonColors(
            containerColor = BonagerColors.Primary,
            contentColor = BonagerColors.Surface,
            disabledContainerColor = BonagerColors.Border,
        ),
        elevation = ButtonDefaults.buttonElevation(defaultElevation = 0.dp, pressedElevation = 0.dp),
    ) {
        Text(label, style = MaterialTheme.typography.labelLarge)
    }
}

@Composable
fun SecondaryButton(label: String, onClick: () -> Unit, modifier: Modifier = Modifier) {
    OutlinedButton(
        onClick = onClick,
        modifier = modifier.height(50.dp),
        shape = RoundedCornerShape(12.dp),
        border = BorderStroke(1.dp, BonagerColors.Border),
        colors = ButtonDefaults.outlinedButtonColors(
            containerColor = BonagerColors.SurfaceElevated,
            contentColor = BonagerColors.Ink,
        ),
    ) {
        Text(label, style = MaterialTheme.typography.labelLarge)
    }
}

@Composable
fun CompactButton(
    label: String,
    onClick: () -> Unit,
    active: Boolean = false,
    enabled: Boolean = true,
) {
    val bgColor by animateColorAsState(
        targetValue = if (active) BonagerColors.PrimaryLight else BonagerColors.Surface,
        animationSpec = tween(180),
        label = "compactBg",
    )
    val borderColor by animateColorAsState(
        targetValue = if (active) BonagerColors.Primary else BonagerColors.Border,
        animationSpec = tween(180),
        label = "compactBorder",
    )
    val textColor by animateColorAsState(
        targetValue = if (active) BonagerColors.PrimaryDark else BonagerColors.InkSecondary,
        animationSpec = tween(180),
        label = "compactText",
    )
    OutlinedButton(
        onClick = onClick,
        enabled = enabled,
        shape = RoundedCornerShape(10.dp),
        border = BorderStroke(1.dp, borderColor),
        colors = ButtonDefaults.outlinedButtonColors(containerColor = bgColor, contentColor = textColor),
        contentPadding = androidx.compose.foundation.layout.PaddingValues(horizontal = 14.dp, vertical = 8.dp),
    ) {
        Text(label, style = MaterialTheme.typography.labelMedium, maxLines = 1)
    }
}

@Composable
fun DeleteButton(onClick: () -> Unit) {
    TextButton(
        onClick = onClick,
        contentPadding = androidx.compose.foundation.layout.PaddingValues(horizontal = 8.dp, vertical = 4.dp),
    ) {
        Text("Delete", style = MaterialTheme.typography.labelMedium, color = BonagerColors.Danger)
    }
}

@Composable
fun MetricTile(label: String, value: String, tone: Color, modifier: Modifier = Modifier) {
    val toneLight = when (tone) {
        BonagerColors.Primary -> BonagerColors.PrimaryLight
        BonagerColors.Accent -> BonagerColors.AccentLight
        BonagerColors.Danger -> BonagerColors.DangerLight
        BonagerColors.Blue -> BonagerColors.BlueLight
        BonagerColors.Plum -> BonagerColors.PlumLight
        BonagerColors.Success -> BonagerColors.SuccessLight
        else -> BonagerColors.SurfaceAlt
    }
    Surface(
        modifier = modifier,
        shape = RoundedCornerShape(14.dp),
        color = toneLight,
        tonalElevation = 0.dp,
    ) {
        Row(
            modifier = Modifier.padding(14.dp),
            horizontalArrangement = Arrangement.spacedBy(12.dp),
            verticalAlignment = Alignment.CenterVertically,
        ) {
            Surface(
                modifier = Modifier.width(4.dp).height(44.dp),
                shape = RoundedCornerShape(4.dp),
                color = tone,
            ) {}
            Column(verticalArrangement = Arrangement.spacedBy(3.dp)) {
                Text(value, style = MaterialTheme.typography.titleLarge, color = BonagerColors.Ink)
                Eyebrow(label)
            }
        }
    }
}

@Composable
fun EmptyState(title: String, body: String = "") {
    Column(
        Modifier.padding(vertical = 16.dp, horizontal = 4.dp),
        verticalArrangement = Arrangement.spacedBy(6.dp),
    ) {
        Text(title, style = MaterialTheme.typography.titleSmall, color = BonagerColors.Ink)
        if (body.isNotEmpty()) {
            Text(body, style = MaterialTheme.typography.bodySmall, color = BonagerColors.Muted, lineHeight = 18.sp)
        }
    }
}

@Composable
fun HorizontalPills(content: @Composable RowScope.() -> Unit) {
    Row(
        Modifier.fillMaxWidth().horizontalScroll(rememberScrollState()),
        horizontalArrangement = Arrangement.spacedBy(8.dp),
        content = content,
    )
}

@Composable
fun ListCopy(title: String, detail: String, modifier: Modifier = Modifier, strike: Boolean = false) {
    Column(modifier, verticalArrangement = Arrangement.spacedBy(3.dp)) {
        Text(
            title,
            style = MaterialTheme.typography.titleSmall,
            color = if (strike) BonagerColors.Muted else BonagerColors.Ink,
            maxLines = 2,
            overflow = TextOverflow.Ellipsis,
            textDecoration = if (strike) TextDecoration.LineThrough else null,
        )
        if (detail.isNotEmpty()) {
            Text(
                detail,
                style = MaterialTheme.typography.bodySmall,
                color = BonagerColors.Muted,
                maxLines = 2,
                overflow = TextOverflow.Ellipsis,
            )
        }
    }
}

@Composable
fun StatusChip(label: String, color: Color, backgroundColor: Color) {
    Surface(
        shape = RoundedCornerShape(6.dp),
        color = backgroundColor,
    ) {
        Text(
            label,
            modifier = Modifier.padding(horizontal = 8.dp, vertical = 3.dp),
            style = MaterialTheme.typography.labelSmall,
            color = color,
        )
    }
}
