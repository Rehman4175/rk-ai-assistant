package com.aistudio.rkaiassistant.ui

import com.aistudio.rkaiassistant.ui.theme.*
import com.aistudio.rkaiassistant.data.*
import com.aistudio.rkaiassistant.viewmodel.AssistantViewModel
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.Delete
import androidx.compose.material.icons.filled.TrendingDown
import androidx.compose.material.icons.filled.TrendingUp
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import java.text.SimpleDateFormat
import java.util.*

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun ExpenseScreen(viewModel: AssistantViewModel) {
    val expensesList by viewModel.expenses.collectAsStateWithLifecycle()

    var showDialog by remember { mutableStateOf(false) }
    var exAmount by remember { mutableStateOf("") }
    var exTitle by remember { mutableStateOf("") }
    var isIncome by remember { mutableStateOf(false) }
    var exCategory by remember { mutableStateOf("Food") }

    val currentMonthStr = SimpleDateFormat("yyyy-MM", Locale.US).format(Date())

    val totalIncome = expensesList.filter { it.isIncome && it.dateString.startsWith(currentMonthStr) }.sumOf { it.amount }
    val totalExpense = expensesList.filter { !it.isIncome && it.dateString.startsWith(currentMonthStr) }.sumOf { it.amount }
    val netBalance = totalIncome - totalExpense

    val maxBudget = viewModel.prefs.getExpenseBudget()

    // Process category totals for Donut dynamic canvas
    val filteredExpenses = expensesList.filter { !it.isIncome && it.dateString.startsWith(currentMonthStr) }
    val categoryTotals = filteredExpenses.groupBy { it.category }.mapValues { entry -> entry.value.sumOf { it.amount } }
    val grandTotal = categoryTotals.values.sum()

    Box(
        modifier = Modifier
            .fillMaxSize()
            .background(SlateDarkBackground)
    ) {
        Column(modifier = Modifier.fillMaxSize()) {
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(16.dp),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Text(
                    text = "FINANCIAL LEDGER",
                    fontSize = 24.sp,
                    fontWeight = FontWeight.ExtraBold,
                    color = NeonCyan
                )

                IconButton(
                    onClick = { showDialog = true },
                    modifier = Modifier
                        .size(44.dp)
                        .background(NeonCyan.copy(alpha = 0.2f), CircleShape)
                ) {
                    Icon(Icons.Default.Add, null, tint = NeonCyan)
                }
            }

            // Summary Card Row
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 16.dp),
                horizontalArrangement = Arrangement.spacedBy(12.dp)
            ) {
                Column(
                    modifier = Modifier
                        .weight(1f)
                        .clip(RoundedCornerShape(12.dp))
                        .background(CardBackgroundGlass)
                        .padding(12.dp)
                ) {
                    Text("Income", color = SoftTextGray, fontSize = 11.sp)
                    Text("+₹${totalIncome.toInt()}", color = NeonGreen, fontSize = 18.sp, fontWeight = FontWeight.Bold)
                }
                Column(
                    modifier = Modifier
                        .weight(1f)
                        .clip(RoundedCornerShape(12.dp))
                        .background(CardBackgroundGlass)
                        .padding(12.dp)
                ) {
                    Text("Expense", color = SoftTextGray, fontSize = 11.sp)
                    Text("-₹${totalExpense.toInt()}", color = NeonPink, fontSize = 18.sp, fontWeight = FontWeight.Bold)
                }
                Column(
                    modifier = Modifier
                        .weight(1f)
                        .clip(RoundedCornerShape(12.dp))
                        .background(CardBackgroundGlass)
                        .padding(12.dp)
                ) {
                    Text("Net Cash", color = SoftTextGray, fontSize = 11.sp)
                    Text("₹${netBalance.toInt()}", color = if (netBalance >= 0) NeonCyan else Color.Red, fontSize = 18.sp, fontWeight = FontWeight.Bold)
                }
            }

            // Donut Section inside canvas (If values available)
            if (grandTotal > 0) {
                Spacer(modifier = Modifier.height(16.dp))
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(horizontal = 16.dp)
                        .clip(RoundedCornerShape(16.dp))
                        .background(CardBackgroundGlass)
                        .padding(16.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    // Donut Canvas drawing
                    Box(modifier = Modifier.size(100.dp), contentAlignment = Alignment.Center) {
                        Canvas(modifier = Modifier.size(80.dp)) {
                            var startAngle = -90f
                            categoryTotals.forEach { (cat, tot) ->
                                val sweep = (tot / grandTotal * 360f).toFloat()
                                drawArc(
                                    color = getCategoryColor(cat),
                                    startAngle = startAngle,
                                    sweepAngle = sweep,
                                    useCenter = false,
                                    style = Stroke(width = 16.dp.toPx(), cap = StrokeCap.Round)
                                )
                                startAngle += sweep
                            }
                        }
                        Text(
                            text = "${((totalExpense / maxBudget) * 100).toInt()}%",
                            color = NeonCyan,
                            fontSize = 14.sp,
                            fontWeight = FontWeight.Black,
                            fontFamily = FontFamily.Monospace
                        )
                    }

                    Spacer(modifier = Modifier.width(24.dp))

                    // Legend values
                    Column(verticalArrangement = Arrangement.spacedBy(4.dp)) {
                        categoryTotals.keys.take(4).forEach { cat ->
                            Row(verticalAlignment = Alignment.CenterVertically) {
                                Box(modifier = Modifier.size(8.dp).clip(CircleShape).background(getCategoryColor(cat)))
                                Spacer(modifier = Modifier.width(8.dp))
                                Text(text = "$cat: ₹${categoryTotals[cat]?.toInt()}", color = Color.White, fontSize = 11.sp)
                            }
                        }
                    }
                }
            }

            Spacer(modifier = Modifier.height(16.dp))

            // Expenses Lists
            LazyColumn(
                modifier = Modifier
                    .weight(1f)
                    .fillMaxWidth()
                    .padding(horizontal = 16.dp),
                contentPadding = PaddingValues(bottom = 90.dp),
                verticalArrangement = Arrangement.spacedBy(10.dp)
            ) {
                items(expensesList) { ex ->
                    ExpenseItemCard(ex = ex, onDelete = { viewModel.deleteExpense(ex) })
                }
            }
        }

        // Add Transaction Dialog
        if (showDialog) {
            AlertDialog(
                onDismissRequest = { showDialog = false },
                containerColor = CardBackgroundGlass,
                modifier = Modifier.border(1.dp, BorderColor, RoundedCornerShape(24.dp)),
                title = { Text("RECORD TRANSACTION", color = NeonCyan, fontWeight = FontWeight.Bold) },
                text = {
                    Column(verticalArrangement = Arrangement.spacedBy(12.dp)) {
                        OutlinedTextField(
                            value = exAmount,
                            onValueChange = { exAmount = it },
                            label = { Text("Amount (₹)", color = SoftTextGray) },
                            colors = OutlinedTextFieldDefaults.colors(
                                focusedBorderColor = NeonCyan,
                                unfocusedBorderColor = BorderColor,
                                focusedTextColor = Color.White,
                                unfocusedTextColor = Color.White
                            ),
                            modifier = Modifier.fillMaxWidth()
                        )

                        OutlinedTextField(
                            value = exTitle,
                            onValueChange = { exTitle = it },
                            label = { Text("Description (e.g. Starbucks)", color = SoftTextGray) },
                            colors = OutlinedTextFieldDefaults.colors(
                                focusedBorderColor = NeonCyan,
                                unfocusedBorderColor = BorderColor,
                                focusedTextColor = Color.White,
                                unfocusedTextColor = Color.White
                            ),
                            modifier = Modifier.fillMaxWidth()
                        )

                        // Income / Expense Toggle
                        Row(
                            verticalAlignment = Alignment.CenterVertically,
                            modifier = Modifier.clickable { isIncome = !isIncome }
                        ) {
                            Checkbox(
                                checked = isIncome,
                                onCheckedChange = { isIncome = it },
                                colors = CheckboxDefaults.colors(checkedColor = NeonCyan)
                            )
                            Spacer(modifier = Modifier.width(8.dp))
                            Text("This is an Income source", color = Color.White, fontSize = 14.sp)
                        }

                        // Category grid selectors
                        Text("Category", color = SoftTextGray, fontSize = 12.sp)
                        val catList = listOf("Food", "Salary", "Fuel", "Rent", "Shopping", "Other")
                        Row(horizontalArrangement = Arrangement.spacedBy(4.dp)) {
                            catList.take(3).forEach { cat ->
                                val isSelected = exCategory == cat
                                Box(
                                    modifier = Modifier
                                        .weight(1f)
                                        .clip(RoundedCornerShape(8.dp))
                                        .background(if (isSelected) NeonCyan else Color(0xFF141624))
                                        .clickable { exCategory = cat }
                                        .padding(vertical = 8.dp),
                                    contentAlignment = Alignment.Center
                                ) {
                                    Text(cat, color = if (isSelected) Color.Black else SoftTextGray, fontSize = 11.sp, fontWeight = FontWeight.Bold)
                                }
                            }
                        }
                        Row(horizontalArrangement = Arrangement.spacedBy(4.dp)) {
                            catList.takeLast(3).forEach { cat ->
                                val isSelected = exCategory == cat
                                Box(
                                    modifier = Modifier
                                        .weight(1f)
                                        .clip(RoundedCornerShape(8.dp))
                                        .background(if (isSelected) NeonCyan else Color(0xFF141624))
                                        .clickable { exCategory = cat }
                                        .padding(vertical = 8.dp),
                                    contentAlignment = Alignment.Center
                                ) {
                                    Text(cat, color = if (isSelected) Color.Black else SoftTextGray, fontSize = 11.sp, fontWeight = FontWeight.Bold)
                                }
                            }
                        }
                    }
                },
                confirmButton = {
                    Button(
                        onClick = {
                            val amount = exAmount.toDoubleOrNull() ?: 0.0
                            if (amount > 0 && exTitle.isNotBlank()) {
                                val todaySimple = SimpleDateFormat("yyyy-MM-dd", Locale.US).format(Date())
                                viewModel.addExpense(amount, exTitle, isIncome, exCategory, todaySimple)
                                exAmount = ""
                                exTitle = ""
                                showDialog = false
                            }
                        },
                        colors = ButtonDefaults.buttonColors(containerColor = NeonCyan)
                    ) {
                        Text("RECORD", color = Color.Black)
                    }
                },
                dismissButton = {
                    TextButton(onClick = { showDialog = false }) {
                        Text("CANCEL", color = SoftTextGray)
                    }
                }
            )
        }
    }
}

@Composable
fun ExpenseItemCard(ex: Expense, onDelete: () -> Unit) {
    Box(
        modifier = Modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(12.dp))
            .background(CardBackgroundGlass)
            .border(1.dp, BorderColor, RoundedCornerShape(12.dp))
            .padding(12.dp)
    ) {
        Row(
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.SpaceBetween,
            modifier = Modifier.fillMaxWidth()
        ) {
            Row(verticalAlignment = Alignment.CenterVertically) {
                Box(
                    modifier = Modifier
                        .size(36.dp)
                        .background(
                            color = if (ex.isIncome) NeonGreen.copy(alpha = 0.2f) else NeonPink.copy(alpha = 0.2f),
                            shape = CircleShape
                        ),
                    contentAlignment = Alignment.Center
                ) {
                    Icon(
                        imageVector = if (ex.isIncome) Icons.Default.TrendingUp else Icons.Default.TrendingDown,
                        contentDescription = null,
                        tint = if (ex.isIncome) NeonGreen else NeonPink,
                        modifier = Modifier.size(18.dp)
                    )
                }

                Spacer(modifier = Modifier.width(16.dp))

                Column {
                    Text(text = ex.title, color = Color.White, fontWeight = FontWeight.Bold)
                    Text(text = "${ex.category} • ${ex.dateString}", color = SoftTextGray, fontSize = 11.sp)
                }
            }

            Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                Text(
                    text = "${if (ex.isIncome) "+" else "-"}₹${ex.amount.toInt()}",
                    color = if (ex.isIncome) NeonGreen else NeonPink,
                    fontWeight = FontWeight.Black,
                    fontSize = 16.sp
                )
                IconButton(onClick = onDelete, modifier = Modifier.size(24.dp)) {
                    Icon(Icons.Default.Delete, null, tint = SoftTextGray, modifier = Modifier.size(16.dp))
                }
            }
        }
    }
}

private fun getCategoryColor(category: String): Color {
    return when (category) {
        "Food" -> NeonCyan
        "Salary" -> NeonGreen
        "Fuel" -> NeonPurple
        "Rent" -> NeonPink
        "Shopping" -> Color(0xFFFF9100)
        else -> SoftTextGray
    }
}
