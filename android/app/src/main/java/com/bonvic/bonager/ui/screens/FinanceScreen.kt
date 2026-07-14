package com.bonvic.bonager.ui.screens

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.unit.dp
import androidx.core.graphics.toColorInt
import com.bonvic.bonager.data.AppSnapshot
import com.bonvic.bonager.data.BankAccount
import com.bonvic.bonager.data.Dates
import com.bonvic.bonager.data.Debt
import com.bonvic.bonager.data.FinanceLabel
import com.bonvic.bonager.data.SavingsGoal
import com.bonvic.bonager.data.TransactionKind
import com.bonvic.bonager.data.formatMoney
import com.bonvic.bonager.ui.BonagerViewModel
import com.bonvic.bonager.ui.components.BonagerCard
import com.bonvic.bonager.ui.components.BonagerField
import com.bonvic.bonager.ui.components.CompactButton
import com.bonvic.bonager.ui.components.DeleteButton
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
import com.bonvic.bonager.ui.components.StatusChip
import com.bonvic.bonager.ui.theme.BonagerColors

private enum class FinanceTab(val label: String) {
    OVERVIEW("Overview"),
    DEBTS("Debts"),
    SAVINGS("Savings"),
    ACCOUNTS("Accounts"),
}

private val savingsColors = listOf("#0C7C72", "#3568A6", "#7C5C86", "#E5A11A", "#2E7D4F")

@Composable
fun FinanceScreen(snapshot: AppSnapshot, viewModel: BonagerViewModel) {
    var activeTab by rememberSaveable { mutableStateOf(FinanceTab.OVERVIEW) }
    val summary = snapshot.financeSummary

    Page("Finance", "Track spending, earnings, debts, savings, and accounts.") {
        Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(12.dp)) {
            MetricTile("Income", formatMoney(summary.income), BonagerColors.Success, Modifier.weight(1f))
            MetricTile("Spending", formatMoney(summary.expense), BonagerColors.Danger, Modifier.weight(1f))
        }
        MetricTile("Net this month", formatMoney(summary.net), BonagerColors.Blue, Modifier.fillMaxWidth())

        SegmentedControl(
            activeTab,
            FinanceTab.entries.map { it.label to it },
            { activeTab = it },
        )

        when (activeTab) {
            FinanceTab.OVERVIEW -> OverviewSection(snapshot, viewModel)
            FinanceTab.DEBTS -> DebtsSection(snapshot, viewModel)
            FinanceTab.SAVINGS -> SavingsSection(snapshot, viewModel)
            FinanceTab.ACCOUNTS -> AccountsSection(snapshot, viewModel)
        }
    }
}

@Composable
private fun OverviewSection(snapshot: AppSnapshot, viewModel: BonagerViewModel) {
    var kind by rememberSaveable { mutableStateOf(TransactionKind.EXPENSE) }
    var amount by rememberSaveable { mutableStateOf("") }
    var dateKey by rememberSaveable { mutableStateOf(Dates.toDateKey()) }
    var category by rememberSaveable { mutableStateOf("") }
    var client by rememberSaveable { mutableStateOf("") }
    var note by rememberSaveable { mutableStateOf("") }
    var labelId by remember { mutableStateOf<Long?>(-1L) }
    var debtId by remember { mutableStateOf<Long?>(-1L) }
    var savingsGoalId by remember { mutableStateOf<Long?>(-1L) }
    var bankAccountId by remember { mutableStateOf<Long?>(-1L) }
    var showLabelForm by remember { mutableStateOf(false) }
    var newLabelName by remember { mutableStateOf("") }
    var newLabelColor by remember { mutableStateOf(savingsColors.first()) }

    val resolvedLabelId = if (labelId == -1L) null else labelId
    val resolvedDebtId = if (debtId == -1L) null else debtId
    val resolvedSavingsGoalId = if (savingsGoalId == -1L) null else savingsGoalId
    val resolvedBankAccountId = if (bankAccountId == -1L) null else bankAccountId

    BonagerCard {
        SectionTitle("New entry")

        val typeOptions = listOf(
            "Expense" to TransactionKind.EXPENSE,
            "Income" to TransactionKind.INCOME,
            "Debt" to TransactionKind.DEBT_PAYMENT,
            "Savings" to TransactionKind.SAVINGS_DEPOSIT,
            "Transfer" to TransactionKind.TRANSFER,
        )
        HorizontalPills {
            typeOptions.forEach { (label, value) ->
                CompactButton(label, { kind = value }, active = kind == value)
            }
        }

        Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(12.dp)) {
            BonagerField(
                "Amount", amount, { amount = it }, Modifier.weight(1f),
                keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Decimal),
            )
            BonagerField("Date", dateKey, { dateKey = it }, Modifier.weight(1f), "YYYY-MM-DD")
        }
        BonagerField("Category", category, { category = it }, placeholder = "Food, rent, client...")
        BonagerField("Client", client, { client = it }, placeholder = "Optional")
        BonagerField("Note", note, { note = it }, multiline = true)

        if (kind == TransactionKind.DEBT_PAYMENT && snapshot.debts.isNotEmpty()) {
            Eyebrow("Link to debt")
            HorizontalPills {
                CompactButton("None", { debtId = -1L }, active = debtId == -1L)
                snapshot.debts.forEach { debt ->
                    CompactButton(debt.name, { debtId = debt.id }, active = debtId == debt.id)
                }
            }
        }

        if (kind == TransactionKind.SAVINGS_DEPOSIT && snapshot.savingsGoals.isNotEmpty()) {
            Eyebrow("Link to savings goal")
            HorizontalPills {
                CompactButton("None", { savingsGoalId = -1L }, active = savingsGoalId == -1L)
                snapshot.savingsGoals.forEach { goal ->
                    CompactButton(goal.name, { savingsGoalId = goal.id }, active = savingsGoalId == goal.id)
                }
            }
        }

        if (snapshot.bankAccounts.isNotEmpty()) {
            Eyebrow("Account")
            HorizontalPills {
                CompactButton("None", { bankAccountId = -1L }, active = bankAccountId == -1L)
                snapshot.bankAccounts.forEach { account ->
                    CompactButton(account.name, { bankAccountId = account.id }, active = bankAccountId == account.id)
                }
            }
        }

        Eyebrow("Label")
        HorizontalPills {
            CompactButton("None", { labelId = -1L }, active = labelId == -1L)
            snapshot.labels.forEach { lbl ->
                LabelPill(lbl, active = labelId == lbl.id, onClick = { labelId = lbl.id })
            }
            CompactButton("+ Add", { showLabelForm = !showLabelForm })
        }

        if (showLabelForm) {
            BonagerField("Label name", newLabelName, { newLabelName = it })
            Eyebrow("Label color")
            Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                savingsColors.forEach { hex ->
                    val color = Color(hex.toColorInt())
                    Box(
                        Modifier
                            .size(28.dp)
                            .clip(CircleShape)
                            .background(color)
                            .then(
                                if (newLabelColor == hex) Modifier.border(2.dp, BonagerColors.Ink, CircleShape)
                                else Modifier,
                            )
                            .clickable { newLabelColor = hex },
                    )
                }
            }
            SecondaryButton("Save label", onClick = {
                if (newLabelName.isNotBlank()) {
                    viewModel.saveLabel(newLabelName.trim(), newLabelColor)
                    newLabelName = ""
                    showLabelForm = false
                }
            })
        }

        PrimaryButton("Add entry", {
            viewModel.saveTransaction(
                kind = kind,
                amount = amount.toDoubleOrNull() ?: 0.0,
                category = category,
                note = note,
                client = client,
                happenedAt = "${dateKey}T12:00:00",
                labelId = resolvedLabelId,
                debtId = resolvedDebtId,
                savingsGoalId = resolvedSavingsGoalId,
                bankAccountId = resolvedBankAccountId,
            )
            amount = ""
            dateKey = Dates.toDateKey()
            category = ""
            client = ""
            note = ""
            labelId = -1L
            debtId = -1L
            savingsGoalId = -1L
            bankAccountId = -1L
        }, Modifier.fillMaxWidth())
    }

    BonagerCard {
        SectionTitle("Monthly summary")
        if (snapshot.financeSummary.byCategory.isEmpty()) {
            EmptyState("No entries this month", "Add daily spending or earnings to build your monthly summary.")
        } else {
            Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                snapshot.financeSummary.byCategory.forEach { item ->
                    Row(
                        Modifier
                            .fillMaxWidth()
                            .background(BonagerColors.SurfaceElevated, RoundedCornerShape(10.dp))
                            .padding(12.dp),
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.spacedBy(8.dp),
                    ) {
                        ListCopy(
                            item.category,
                            "Income ${formatMoney(item.income)} · Spending ${formatMoney(item.expense)}",
                            Modifier.weight(1f),
                        )
                        val net = item.income - item.expense
                        Text(
                            formatMoney(net),
                            style = MaterialTheme.typography.titleSmall,
                            color = if (net >= 0) BonagerColors.Success else BonagerColors.Danger,
                        )
                    }
                }
            }
        }
    }

    BonagerCard {
        SectionTitle("Daily ledger")
        if (snapshot.transactions.isEmpty()) {
            EmptyState("No ledger entries yet")
        } else {
            Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                snapshot.transactions.forEach { transaction ->
                    Row(
                        Modifier
                            .fillMaxWidth()
                            .background(BonagerColors.SurfaceElevated, RoundedCornerShape(10.dp))
                            .padding(12.dp),
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.spacedBy(10.dp),
                    ) {
                        val dotColor = when (transaction.kind) {
                            TransactionKind.INCOME -> BonagerColors.Success
                            TransactionKind.EXPENSE -> BonagerColors.Danger
                            TransactionKind.DEBT_PAYMENT -> BonagerColors.Accent
                            TransactionKind.SAVINGS_DEPOSIT -> BonagerColors.Blue
                            TransactionKind.TRANSFER -> BonagerColors.Plum
                        }
                        Surface(modifier = Modifier.size(8.dp), shape = CircleShape, color = dotColor) {}
                        if (transaction.label != null) {
                            val labelColor = Color(transaction.label.color.toColorInt())
                            StatusChip(
                                transaction.label.name,
                                labelColor,
                                labelColor.copy(alpha = 0.15f),
                            )
                        }
                        ListCopy(
                            transaction.category,
                            "${Dates.formatShortDate(transaction.happenedAt)} · ${transaction.clientName ?: transaction.note ?: "No note"}",
                            Modifier.weight(1f),
                        )
                        val sign = if (transaction.kind == TransactionKind.INCOME) "+" else "-"
                        val amountColor = when (transaction.kind) {
                            TransactionKind.INCOME -> BonagerColors.Success
                            else -> BonagerColors.Danger
                        }
                        Text(
                            "$sign${formatMoney(transaction.amount)}",
                            style = MaterialTheme.typography.titleSmall,
                            color = amountColor,
                        )
                        DeleteButton { viewModel.deleteTransaction(transaction.id) }
                    }
                }
            }
        }
    }
}

@Composable
private fun DebtsSection(snapshot: AppSnapshot, viewModel: BonagerViewModel) {
    var name by rememberSaveable { mutableStateOf("") }
    var totalAmount by rememberSaveable { mutableStateOf("") }
    var interestRate by rememberSaveable { mutableStateOf("") }
    var dueDate by rememberSaveable { mutableStateOf("") }
    var notes by rememberSaveable { mutableStateOf("") }

    BonagerCard {
        SectionTitle("New debt")
        BonagerField("Name", name, { name = it }, placeholder = "Car loan, mortgage...")
        Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(12.dp)) {
            BonagerField(
                "Total amount", totalAmount, { totalAmount = it }, Modifier.weight(1f),
                keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Decimal),
            )
            BonagerField(
                "Interest %", interestRate, { interestRate = it }, Modifier.weight(1f),
                keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Decimal),
                placeholder = "0",
            )
        }
        BonagerField("Due date", dueDate, { dueDate = it }, placeholder = "YYYY-MM-DD or blank")
        BonagerField("Notes", notes, { notes = it }, multiline = true)
        PrimaryButton("Add debt", {
            val total = totalAmount.toDoubleOrNull() ?: 0.0
            if (name.isNotBlank() && total > 0) {
                viewModel.saveDebt(
                    name = name.trim(),
                    totalAmount = total,
                    dueDate = dueDate.trim().ifEmpty { null },
                    interestRate = interestRate.toDoubleOrNull() ?: 0.0,
                    notes = notes.trim().ifEmpty { null },
                )
                name = ""
                totalAmount = ""
                interestRate = ""
                dueDate = ""
                notes = ""
            }
        }, Modifier.fillMaxWidth())
    }

    BonagerCard {
        SectionTitle("Active debts")
        if (snapshot.debts.isEmpty()) {
            EmptyState("No debts", "Add a debt to track payments and progress.")
        } else {
            Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                snapshot.debts.forEach { debt ->
                    DebtRow(debt, onDelete = { viewModel.deleteDebt(debt.id) })
                }
            }
        }
    }
}

@Composable
private fun DebtRow(debt: Debt, onDelete: () -> Unit) {
    Column(
        Modifier
            .fillMaxWidth()
            .background(BonagerColors.SurfaceElevated, RoundedCornerShape(12.dp))
            .padding(14.dp),
        verticalArrangement = Arrangement.spacedBy(6.dp),
    ) {
        Row(Modifier.fillMaxWidth(), verticalAlignment = Alignment.CenterVertically) {
            Text(debt.name, style = MaterialTheme.typography.titleSmall, modifier = Modifier.weight(1f))
            Text(
                formatMoney(debt.remainingAmount),
                style = MaterialTheme.typography.titleSmall,
                color = BonagerColors.Danger,
            )
        }
        val duePart = if (debt.dueDate != null) " · Due ${Dates.formatShortDate(debt.dueDate)}" else ""
        Text(
            "${formatMoney(debt.paidAmount)} paid of ${formatMoney(debt.totalAmount)}$duePart",
            style = MaterialTheme.typography.bodySmall,
            color = BonagerColors.Muted,
        )
        ProgressBar(fraction = debt.progressFraction, color = BonagerColors.Success)
        Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.End) {
            DeleteButton(onDelete)
        }
    }
}

@Composable
private fun SavingsSection(snapshot: AppSnapshot, viewModel: BonagerViewModel) {
    var name by rememberSaveable { mutableStateOf("") }
    var targetAmount by rememberSaveable { mutableStateOf("") }
    var color by rememberSaveable { mutableStateOf(savingsColors.first()) }

    BonagerCard {
        SectionTitle("New savings goal")
        BonagerField("Name", name, { name = it }, placeholder = "Emergency fund, vacation...")
        BonagerField(
            "Target", targetAmount, { targetAmount = it },
            keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Decimal),
        )
        Eyebrow("Color")
        Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
            savingsColors.forEach { hex ->
                val c = Color(hex.toColorInt())
                Box(
                    Modifier
                        .size(28.dp)
                        .clip(CircleShape)
                        .background(c)
                        .then(
                            if (color == hex) Modifier.border(2.dp, BonagerColors.Ink, CircleShape)
                            else Modifier,
                        )
                        .clickable { color = hex },
                )
            }
        }
        PrimaryButton("Add goal", {
            val target = targetAmount.toDoubleOrNull() ?: 0.0
            if (name.isNotBlank() && target > 0) {
                viewModel.saveSavingsGoal(name.trim(), target, color)
                name = ""
                targetAmount = ""
                color = savingsColors.first()
            }
        }, Modifier.fillMaxWidth())
    }

    BonagerCard {
        SectionTitle("Goals")
        if (snapshot.savingsGoals.isEmpty()) {
            EmptyState("No savings goals yet", "Add a goal to start tracking your savings.")
        } else {
            Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                snapshot.savingsGoals.forEach { goal ->
                    SavingsGoalRow(goal, onDelete = { viewModel.deleteSavingsGoal(goal.id) })
                }
            }
        }
    }
}

@Composable
private fun SavingsGoalRow(goal: SavingsGoal, onDelete: () -> Unit) {
    val goalColor = Color(goal.color.toColorInt())
    Row(
        Modifier
            .fillMaxWidth()
            .background(goalColor.copy(alpha = 0.08f), RoundedCornerShape(12.dp))
            .padding(14.dp),
        verticalAlignment = Alignment.Top,
        horizontalArrangement = Arrangement.spacedBy(12.dp),
    ) {
        Box(
            Modifier
                .width(4.dp)
                .height(60.dp)
                .background(goalColor, RoundedCornerShape(2.dp)),
        )
        Column(Modifier.weight(1f), verticalArrangement = Arrangement.spacedBy(4.dp)) {
            Row(Modifier.fillMaxWidth(), verticalAlignment = Alignment.CenterVertically) {
                Text(goal.name, style = MaterialTheme.typography.titleSmall, modifier = Modifier.weight(1f))
                if (goal.progressFraction >= 1f) {
                    StatusChip("Complete", BonagerColors.Success, BonagerColors.SuccessLight)
                }
            }
            Text(
                "${formatMoney(goal.currentAmount)} of ${formatMoney(goal.targetAmount)}",
                style = MaterialTheme.typography.bodySmall,
                color = BonagerColors.Muted,
            )
            ProgressBar(fraction = goal.progressFraction, color = goalColor)
            Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.End) {
                DeleteButton(onDelete)
            }
        }
    }
}

@Composable
private fun AccountsSection(snapshot: AppSnapshot, viewModel: BonagerViewModel) {
    var name by rememberSaveable { mutableStateOf("") }
    var accountType by rememberSaveable { mutableStateOf("checking") }
    var balance by rememberSaveable { mutableStateOf("") }
    var overdraftLimit by rememberSaveable { mutableStateOf("") }

    BonagerCard {
        SectionTitle("New account")
        BonagerField("Account name", name, { name = it }, placeholder = "KCB Savings, M-Pesa...")
        val typeOptions = listOf("Checking" to "checking", "Savings" to "savings", "M-Pesa" to "mpesa", "Wallet" to "wallet")
        HorizontalPills {
            typeOptions.forEach { (label, value) ->
                CompactButton(label, { accountType = value }, active = accountType == value)
            }
        }
        Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(12.dp)) {
            BonagerField(
                "Opening balance", balance, { balance = it }, Modifier.weight(1f),
                keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Decimal),
                placeholder = "0",
            )
            BonagerField(
                "Overdraft limit", overdraftLimit, { overdraftLimit = it }, Modifier.weight(1f),
                keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Decimal),
                placeholder = "0",
            )
        }
        PrimaryButton("Add account", {
            if (name.isNotBlank()) {
                viewModel.saveBankAccount(
                    name = name.trim(),
                    accountType = accountType,
                    initialBalance = balance.toDoubleOrNull() ?: 0.0,
                    overdraftLimit = overdraftLimit.toDoubleOrNull() ?: 0.0,
                )
                name = ""
                balance = ""
                overdraftLimit = ""
                accountType = "checking"
            }
        }, Modifier.fillMaxWidth())
    }

    if (snapshot.bankAccounts.isEmpty()) {
        BonagerCard {
            EmptyState("No accounts", "Add a bank account or wallet to track balances per transaction.")
        }
    } else {
        Column(verticalArrangement = Arrangement.spacedBy(12.dp)) {
            snapshot.bankAccounts.forEach { account ->
                BankAccountCard(account, onDelete = { viewModel.deleteBankAccount(account.id) })
            }
        }
    }
}

@Composable
private fun BankAccountCard(account: BankAccount, onDelete: () -> Unit) {
    BonagerCard {
        Row(Modifier.fillMaxWidth(), verticalAlignment = Alignment.CenterVertically) {
            Column(Modifier.weight(1f)) {
                Text(account.name, style = MaterialTheme.typography.titleSmall)
                Text(
                    account.accountType.replaceFirstChar { it.uppercase() },
                    style = MaterialTheme.typography.bodySmall,
                    color = BonagerColors.Muted,
                )
            }
            if (account.isOverdrawn) {
                StatusChip("Overdrawn", BonagerColors.Danger, BonagerColors.DangerLight)
                Spacer(Modifier.width(8.dp))
            }
            DeleteButton(onDelete)
        }
        Text(
            formatMoney(account.balance),
            style = MaterialTheme.typography.headlineSmall,
            color = if (account.isOverdrawn) BonagerColors.Danger else BonagerColors.Ink,
        )
        if (account.overdraftLimit > 0) {
            Text(
                "Available: ${formatMoney(account.availableBalance)}",
                style = MaterialTheme.typography.bodySmall,
                color = BonagerColors.Muted,
            )
        }
    }
}

@Composable
private fun ProgressBar(fraction: Float, color: Color) {
    Box(
        Modifier
            .fillMaxWidth()
            .height(6.dp)
            .background(BonagerColors.BorderLight, RoundedCornerShape(3.dp)),
    ) {
        Box(
            Modifier
                .fillMaxWidth(fraction.coerceIn(0f, 1f))
                .height(6.dp)
                .background(color, RoundedCornerShape(3.dp))
                .align(Alignment.CenterStart),
        )
    }
}

@Composable
private fun LabelPill(label: FinanceLabel, active: Boolean, onClick: () -> Unit) {
    val labelColor = Color(label.color.toColorInt())
    CompactButton(
        label = label.name,
        onClick = onClick,
        active = active,
    )
}
