package com.idk.anypay.ui.screens

import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.material3.TabRowDefaults.tabIndicatorOffset
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import com.idk.anypay.data.model.*
import com.idk.anypay.ui.theme.*

enum class HistoryFilter { ALL, SENT, BALANCE }

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun HistoryScreen(
    transactions: List<Transaction>,
    totalSpent: Double,
    averageTransaction: Double,
    categoryStats: Map<PaymentCategory, Pair<Int, Double>>
) {
    var selectedTab    by remember { mutableIntStateOf(0) }
    var selectedFilter by remember { mutableStateOf(HistoryFilter.ALL) }

    val filteredTransactions = remember(transactions, selectedFilter) {
        when (selectedFilter) {
            HistoryFilter.ALL     -> transactions
            HistoryFilter.SENT    -> transactions.filter { it.type == TransactionType.SEND }
            HistoryFilter.BALANCE -> transactions.filter { it.type == TransactionType.BALANCE_CHECK }
        }
    }
    val groupedTransactions = remember(filteredTransactions) {
        filteredTransactions.groupBy { it.relativeDate }
    }

    Column(
        modifier = Modifier
            .fillMaxSize()
            .background(MaterialTheme.colorScheme.background)
    ) {
        // ── Tab Row ───────────────────────────────────────────────────────
        TabRow(
            selectedTabIndex = selectedTab,
            containerColor   = MaterialTheme.colorScheme.background,
            contentColor     = MaterialTheme.colorScheme.onBackground,
            indicator        = { tabPositions ->
                TabRowDefaults.SecondaryIndicator(
                    modifier = Modifier.tabIndicatorOffset(tabPositions[selectedTab]),
                    color    = MaterialTheme.colorScheme.primary
                )
            },
            divider = { HorizontalDivider(color = MaterialTheme.colorScheme.outline) }
        ) {
            Tab(
                selected = selectedTab == 0,
                onClick  = { selectedTab = 0 },
                text     = { Text("Transactions", style = MaterialTheme.typography.labelLarge) },
                selectedContentColor   = MaterialTheme.colorScheme.onBackground,
                unselectedContentColor = MaterialTheme.colorScheme.onSurfaceVariant
            )
            Tab(
                selected = selectedTab == 1,
                onClick  = { selectedTab = 1 },
                text     = { Text("Analytics", style = MaterialTheme.typography.labelLarge) },
                selectedContentColor   = MaterialTheme.colorScheme.onBackground,
                unselectedContentColor = MaterialTheme.colorScheme.onSurfaceVariant
            )
        }

        when (selectedTab) {
            0 -> TransactionsTab(
                groupedTransactions = groupedTransactions,
                selectedFilter      = selectedFilter,
                onFilterChange      = { selectedFilter = it }
            )
            1 -> AnalyticsTab(
                totalSpent         = totalSpent,
                averageTransaction = averageTransaction,
                categoryStats      = categoryStats
            )
        }
    }
}

// ─── Transactions Tab ─────────────────────────────────────────────────────────

@OptIn(ExperimentalMaterial3Api::class)
@Composable
private fun TransactionsTab(
    groupedTransactions: Map<String, List<Transaction>>,
    selectedFilter: HistoryFilter,
    onFilterChange: (HistoryFilter) -> Unit
) {
    LazyColumn(
        modifier        = Modifier.fillMaxSize(),
        contentPadding  = PaddingValues(horizontal = 16.dp, vertical = 16.dp),
        verticalArrangement = Arrangement.spacedBy(8.dp)
    ) {
        // Filter chips
        item {
            Row(
                horizontalArrangement = Arrangement.spacedBy(8.dp),
                modifier = Modifier.padding(bottom = 8.dp)
            ) {
                listOf(
                    HistoryFilter.ALL     to "All",
                    HistoryFilter.SENT    to "Sent",
                    HistoryFilter.BALANCE to "Balance"
                ).forEach { (filter, label) ->
                    FilterChip(
                        selected = selectedFilter == filter,
                        onClick  = { onFilterChange(filter) },
                        label    = { Text(label, style = MaterialTheme.typography.labelMedium) },
                        shape    = MaterialTheme.shapes.extraSmall,
                        colors   = FilterChipDefaults.filterChipColors(
                            selectedContainerColor = MaterialTheme.colorScheme.primary,
                            selectedLabelColor     = MaterialTheme.colorScheme.onPrimary,
                            containerColor         = MaterialTheme.colorScheme.surface,
                            labelColor             = MaterialTheme.colorScheme.onSurfaceVariant
                        ),
                        border   = FilterChipDefaults.filterChipBorder(
                            enabled               = true,
                            selected              = selectedFilter == filter,
                            borderColor           = MaterialTheme.colorScheme.outline,
                            selectedBorderColor   = MaterialTheme.colorScheme.primary
                        )
                    )
                }
            }
        }

        if (groupedTransactions.isEmpty()) {
            item { EmptyTransactionsList() }
        } else {
            groupedTransactions.forEach { (date, txList) ->
                item {
                    Text(
                        text     = date,
                        style    = MaterialTheme.typography.labelMedium,
                        color    = MaterialTheme.colorScheme.onSurfaceVariant,
                        modifier = Modifier.padding(vertical = 8.dp)
                    )
                }
                items(txList) { TransactionItem(transaction = it) }
            }
        }
    }
}

@Composable
private fun EmptyTransactionsList() {
    Column(
        modifier = Modifier
            .fillMaxWidth()
            .clip(MaterialTheme.shapes.medium)
            .border(BorderStroke(1.dp, MaterialTheme.colorScheme.outline), MaterialTheme.shapes.medium)
            .padding(vertical = 40.dp),
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.spacedBy(8.dp)
    ) {
        Icon(
            Icons.Default.Receipt,
            contentDescription = null,
            modifier = Modifier.size(36.dp),
            tint = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.4f)
        )
        Text("No transactions found", style = MaterialTheme.typography.titleSmall,
            color = MaterialTheme.colorScheme.onSurface)
        Text("Your transactions will appear here", style = MaterialTheme.typography.bodySmall,
            color = MaterialTheme.colorScheme.onSurfaceVariant)
    }
}

// ─── Analytics Tab ────────────────────────────────────────────────────────────

@Composable
private fun AnalyticsTab(
    totalSpent: Double,
    averageTransaction: Double,
    categoryStats: Map<PaymentCategory, Pair<Int, Double>>
) {
    val sortedCategories = remember(categoryStats) {
        categoryStats.entries.sortedByDescending { it.value.second }.take(6)
    }
    val totalAmount = remember(categoryStats) { categoryStats.values.sumOf { it.second } }

    LazyColumn(
        modifier        = Modifier.fillMaxSize(),
        contentPadding  = PaddingValues(horizontal = 16.dp, vertical = 16.dp),
        verticalArrangement = Arrangement.spacedBy(12.dp)
    ) {
        // Overview stat cards
        item {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(12.dp)
            ) {
                StatCard(
                    title    = "Total Spent",
                    value    = "₹${String.format("%,.0f", totalSpent)}",
                    icon     = Icons.Default.TrendingDown,
                    color    = SendRed,
                    modifier = Modifier.weight(1f)
                )
                StatCard(
                    title    = "Average",
                    value    = "₹${String.format("%,.0f", averageTransaction)}",
                    icon     = Icons.Default.Analytics,
                    color    = BalanceBlue,
                    modifier = Modifier.weight(1f)
                )
            }
        }

        item {
            Text(
                text  = "Spending by Category",
                style = MaterialTheme.typography.titleMedium,
                color = MaterialTheme.colorScheme.onBackground
            )
        }

        if (sortedCategories.isEmpty()) {
            item {
                Column(
                    modifier = Modifier
                        .fillMaxWidth()
                        .clip(MaterialTheme.shapes.medium)
                        .border(BorderStroke(1.dp, MaterialTheme.colorScheme.outline), MaterialTheme.shapes.medium)
                        .padding(vertical = 32.dp),
                    horizontalAlignment = Alignment.CenterHorizontally
                ) {
                    Icon(
                        Icons.Default.PieChart,
                        contentDescription = null,
                        modifier = Modifier.size(36.dp),
                        tint = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.4f)
                    )
                    Spacer(Modifier.height(8.dp))
                    Text("No spending data yet", style = MaterialTheme.typography.bodyMedium,
                        color = MaterialTheme.colorScheme.onSurfaceVariant)
                }
            }
        } else {
            items(sortedCategories) { (category, stats) ->
                val (count, amount) = stats
                val pct = if (totalAmount > 0) (amount / totalAmount * 100) else 0.0
                CategoryStatItem(category = category, count = count, amount = amount, percentage = pct)
            }
        }
    }
}

// ─── Stat Card ────────────────────────────────────────────────────────────────

@Composable
private fun StatCard(
    title: String,
    value: String,
    icon: ImageVector,
    color: Color,
    modifier: Modifier = Modifier
) {
    Column(
        modifier = modifier
            .clip(MaterialTheme.shapes.medium)
            .border(BorderStroke(1.dp, MaterialTheme.colorScheme.outline), MaterialTheme.shapes.medium)
            .background(MaterialTheme.colorScheme.surface)
            .padding(16.dp)
    ) {
        Row(verticalAlignment = Alignment.CenterVertically) {
            Box(
                modifier = Modifier
                    .size(30.dp)
                    .clip(MaterialTheme.shapes.extraSmall)
                    .background(color.copy(alpha = 0.1f)),
                contentAlignment = Alignment.Center
            ) {
                Icon(icon, contentDescription = null, tint = color, modifier = Modifier.size(16.dp))
            }
            Spacer(Modifier.width(8.dp))
            Text(text = title, style = MaterialTheme.typography.labelMedium,
                color = MaterialTheme.colorScheme.onSurfaceVariant)
        }
        Spacer(Modifier.height(10.dp))
        Text(text = value, style = MaterialTheme.typography.headlineSmall,
            color = MaterialTheme.colorScheme.onSurface)
    }
}

// ─── Category Stat Item ───────────────────────────────────────────────────────

@Composable
private fun CategoryStatItem(
    category: PaymentCategory,
    count: Int,
    amount: Double,
    percentage: Double
) {
    Column(
        modifier = Modifier
            .fillMaxWidth()
            .clip(MaterialTheme.shapes.medium)
            .border(BorderStroke(1.dp, MaterialTheme.colorScheme.outline), MaterialTheme.shapes.medium)
            .background(MaterialTheme.colorScheme.surface)
            .padding(14.dp)
    ) {
        Row(verticalAlignment = Alignment.CenterVertically) {
            Box(
                modifier = Modifier
                    .size(38.dp)
                    .clip(MaterialTheme.shapes.small)
                    .background(Color(category.color).copy(alpha = 0.1f)),
                contentAlignment = Alignment.Center
            ) {
                Text(text = category.icon, style = MaterialTheme.typography.titleSmall)
            }
            Spacer(Modifier.width(12.dp))
            Column(modifier = Modifier.weight(1f)) {
                Text(category.label, style = MaterialTheme.typography.bodyMedium,
                    color = MaterialTheme.colorScheme.onSurface)
                Text("$count transaction${if (count != 1) "s" else ""}",
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant)
            }
            Column(horizontalAlignment = Alignment.End) {
                Text("₹${String.format("%,.0f", amount)}",
                    style = MaterialTheme.typography.titleSmall,
                    color = MaterialTheme.colorScheme.onSurface)
                Text("${String.format("%.1f", percentage)}%",
                    style = MaterialTheme.typography.labelSmall,
                    color = Color(category.color))
            }
        }
        Spacer(Modifier.height(10.dp))
        LinearProgressIndicator(
            progress    = { (percentage / 100).toFloat() },
            modifier    = Modifier.fillMaxWidth().clip(MaterialTheme.shapes.extraSmall),
            color       = Color(category.color),
            trackColor  = Color(category.color).copy(alpha = 0.1f)
        )
    }
}

// ─── Previews ─────────────────────────────────────────────────────────────────

private val previewTransactions = listOf(
    Transaction(
        type = TransactionType.SEND, amount = 250.0,
        recipientVpa = "zomato@upi", status = TransactionStatus.SUCCESS, message = "zomato order"
    ),
    Transaction(
        type = TransactionType.SEND, amount = 1500.0,
        recipientVpa = "electricity@upi", status = TransactionStatus.SUCCESS, message = "electricity bill"
    ),
    Transaction(
        type = TransactionType.BALANCE_CHECK, amount = 0.0,
        balance = 9876.0, status = TransactionStatus.SUCCESS
    )
)

@Preview(showBackground = true, name = "History – Light Transactions")
@Composable
private fun HistoryScreenTransactionsPreview() {
    AnyPayTheme(darkTheme = false) {
        HistoryScreen(
            transactions = previewTransactions,
            totalSpent = 1750.0,
            averageTransaction = 875.0,
            categoryStats = mapOf(
                PaymentCategory.FOOD_DINING    to Pair(3, 750.0),
                PaymentCategory.BILLS_UTILITIES to Pair(2, 1000.0)
            )
        )
    }
}

@Preview(showBackground = true, name = "History – Dark Transactions")
@Composable
private fun HistoryScreenDarkPreview() {
    AnyPayTheme(darkTheme = true) {
        HistoryScreen(
            transactions = previewTransactions,
            totalSpent = 1750.0,
            averageTransaction = 875.0,
            categoryStats = mapOf(
                PaymentCategory.FOOD_DINING    to Pair(3, 750.0),
                PaymentCategory.BILLS_UTILITIES to Pair(2, 1000.0)
            )
        )
    }
}

@Preview(showBackground = true, name = "History – Empty")
@Composable
private fun HistoryScreenEmptyPreview() {
    AnyPayTheme(darkTheme = false) {
        HistoryScreen(
            transactions = emptyList(),
            totalSpent = 0.0,
            averageTransaction = 0.0,
            categoryStats = emptyMap()
        )
    }
}

