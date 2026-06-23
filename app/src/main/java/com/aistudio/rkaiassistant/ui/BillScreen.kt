package com.aistudio.rkaiassistant.ui

import com.aistudio.rkaiassistant.ui.theme.*
import com.aistudio.rkaiassistant.data.*
import com.aistudio.rkaiassistant.viewmodel.AssistantViewModel
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
import androidx.compose.material.icons.filled.Cached
import androidx.compose.material.icons.filled.CheckCircle
import androidx.compose.material.icons.filled.Delete
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import java.text.SimpleDateFormat
import java.util.*

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun BillScreen(viewModel: AssistantViewModel) {
    val billsList by viewModel.bills.collectAsStateWithLifecycle()

    var showDialog by remember { mutableStateOf(false) }
    var billName by remember { mutableStateOf("") }
    var billAmount by remember { mutableStateOf("") }
    var billCategory by remember { mutableStateOf("Mobile") }
    var billDueDay by remember { mutableStateOf("10") }

    val currentMonthStr = SimpleDateFormat("yyyy-MM", Locale.US).format(Date())

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
                    text = "BILLS & PAYMENTS",
                    fontSize = 24.sp,
                    fontWeight = FontWeight.ExtraBold,
                    color = NeonPink
                )

                IconButton(
                    onClick = { showDialog = true },
                    modifier = Modifier
                        .size(44.dp)
                        .background(NeonPink.copy(alpha = 0.2f), CircleShape)
                ) {
                    Icon(Icons.Default.Add, null, tint = NeonPink)
                }
            }

            // Subtitle
            Text(
                text = "Keep track of active monthly contracts and essential utility dues.",
                color = SoftTextGray,
                fontSize = 12.sp,
                modifier = Modifier.padding(horizontal = 16.dp, vertical = 2.dp)
            )

            Spacer(modifier = Modifier.height(8.dp))

            // Bills List
            LazyColumn(
                modifier = Modifier
                    .weight(1f)
                    .fillMaxWidth()
                    .padding(horizontal = 16.dp),
                contentPadding = PaddingValues(bottom = 90.dp),
                verticalArrangement = Arrangement.spacedBy(12.dp)
            ) {
                if (billsList.isEmpty()) {
                    item {
                        Box(
                            modifier = Modifier
                                .fillMaxWidth()
                                .padding(vertical = 48.dp),
                            contentAlignment = Alignment.Center
                        ) {
                            Text(text = "No bills registered yet.", color = SoftTextGray, fontSize = 14.sp)
                        }
                    }
                }

                items(billsList) { bill ->
                    val isPaidThisMonth = bill.paidMonthsCommaSeparated.split(",").contains(currentMonthStr)
                    BillCard(
                        bill = bill,
                        isPaid = isPaidThisMonth,
                        onPay = { viewModel.payBillThisMonth(bill) },
                        onDelete = { viewModel.deleteBill(bill) }
                    )
                }
            }
        }

        // Add Bill Dialog
        if (showDialog) {
            AlertDialog(
                onDismissRequest = { showDialog = false },
                containerColor = CardBackgroundGlass,
                modifier = Modifier.border(1.dp, BorderColor, RoundedCornerShape(24.dp)),
                title = { Text("REGISTER BILL CONTRACT", color = NeonPink, fontWeight = FontWeight.Bold) },
                text = {
                    Column(verticalArrangement = Arrangement.spacedBy(12.dp)) {
                        OutlinedTextField(
                            value = billName,
                            onValueChange = { billName = it },
                            label = { Text("Provider (e.g. Netflix, PGE)", color = SoftTextGray) },
                            colors = OutlinedTextFieldDefaults.colors(
                                focusedBorderColor = NeonPink,
                                unfocusedBorderColor = BorderColor,
                                focusedTextColor = Color.White,
                                unfocusedTextColor = Color.White
                            ),
                            modifier = Modifier.fillMaxWidth()
                        )

                        OutlinedTextField(
                            value = billAmount,
                            onValueChange = { billAmount = it },
                            label = { Text("Standard Amount (₹)", color = SoftTextGray) },
                            colors = OutlinedTextFieldDefaults.colors(
                                focusedBorderColor = NeonPink,
                                unfocusedBorderColor = BorderColor,
                                focusedTextColor = Color.White,
                                unfocusedTextColor = Color.White
                            ),
                            modifier = Modifier.fillMaxWidth()
                        )

                        OutlinedTextField(
                            value = billDueDay,
                            onValueChange = { billDueDay = it },
                            label = { Text("Due Day of Month (e.g. 15)", color = SoftTextGray) },
                            colors = OutlinedTextFieldDefaults.colors(
                                focusedBorderColor = NeonPink,
                                unfocusedBorderColor = BorderColor,
                                focusedTextColor = Color.White,
                                unfocusedTextColor = Color.White
                            ),
                            modifier = Modifier.fillMaxWidth()
                        )

                        // Categories selector
                        Text("Category", color = SoftTextGray, fontSize = 12.sp)
                        val cats = listOf("Mobile", "Electricity", "Internet", "Rent", "Gas", "Credit Card")
                        Row(horizontalArrangement = Arrangement.spacedBy(4.dp)) {
                            cats.take(3).forEach { cat ->
                                val isSel = billCategory == cat
                                Box(
                                    modifier = Modifier
                                        .weight(1f)
                                        .clip(RoundedCornerShape(8.dp))
                                        .background(if (isSel) NeonPink else Color(0xFF141624))
                                        .clickable { billCategory = cat }
                                        .padding(vertical = 8.dp),
                                    contentAlignment = Alignment.Center
                                ) {
                                    Text(cat, color = if (isSel) Color.Black else SoftTextGray, fontSize = 10.sp, fontWeight = FontWeight.Bold)
                                }
                            }
                        }
                        Row(horizontalArrangement = Arrangement.spacedBy(4.dp)) {
                            cats.takeLast(3).forEach { cat ->
                                val isSel = billCategory == cat
                                Box(
                                    modifier = Modifier
                                        .weight(1f)
                                        .clip(RoundedCornerShape(8.dp))
                                        .background(if (isSel) NeonPink else Color(0xFF141624))
                                        .clickable { billCategory = cat }
                                        .padding(vertical = 8.dp),
                                    contentAlignment = Alignment.Center
                                ) {
                                    Text(cat, color = if (isSel) Color.Black else SoftTextGray, fontSize = 10.sp, fontWeight = FontWeight.Bold)
                                }
                            }
                        }
                    }
                },
                confirmButton = {
                    Button(
                        onClick = {
                            val amount = billAmount.toDoubleOrNull() ?: 0.0
                            val dueVal = billDueDay.toIntOrNull() ?: 1
                            if (billName.isNotBlank() && amount > 0) {
                                viewModel.addBill(billName, amount, billCategory, dueVal)
                                billName = ""
                                billAmount = ""
                                showDialog = false
                            }
                        },
                        colors = ButtonDefaults.buttonColors(containerColor = NeonPink)
                    ) {
                        Text("REGISTER", color = Color.Black, fontWeight = FontWeight.Bold)
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
fun BillCard(
    bill: Bill,
    isPaid: Boolean,
    onPay: () -> Unit,
    onDelete: () -> Unit
) {
    Box(
        modifier = Modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(16.dp))
            .background(CardBackgroundGlass)
            .border(1.dp, BorderColor, RoundedCornerShape(16.dp))
            .padding(16.dp)
    ) {
        Row(
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.SpaceBetween,
            modifier = Modifier.fillMaxWidth()
        ) {
            Row(verticalAlignment = Alignment.CenterVertically) {
                Box(
                    modifier = Modifier
                        .size(40.dp)
                        .background(
                            color = if (isPaid) NeonGreen.copy(alpha = 0.2f) else NeonPink.copy(alpha = 0.2f),
                            shape = CircleShape
                        ),
                    contentAlignment = Alignment.Center
                ) {
                    Icon(
                        imageVector = if (isPaid) Icons.Default.CheckCircle else Icons.Default.Cached,
                        contentDescription = null,
                        tint = if (isPaid) NeonGreen else NeonPink,
                        modifier = Modifier.size(20.dp)
                    )
                }

                Spacer(modifier = Modifier.width(16.dp))

                Column {
                    Text(text = bill.name, color = Color.White, fontWeight = FontWeight.Bold, fontSize = 16.sp)
                    Text(
                        text = "${bill.category} • Standard due: ${bill.dueDayOfMonth}th day",
                        color = SoftTextGray,
                        fontSize = 11.sp
                    )
                }
            }

            Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                Column(horizontalAlignment = Alignment.End) {
                    Text(
                        text = "₹${bill.amount.toInt()}",
                        color = Color.White,
                        fontWeight = FontWeight.Black,
                        fontSize = 18.sp
                    )
                    Text(
                        text = if (isPaid) "PAID" else "UNPAID",
                        color = if (isPaid) NeonGreen else NeonPink,
                        fontSize = 10.sp,
                        fontWeight = FontWeight.Bold
                    )
                }

                if (!isPaid) {
                    Button(
                        onClick = onPay,
                        colors = ButtonDefaults.buttonColors(containerColor = NeonGreen),
                        modifier = Modifier.padding(horizontal = 4.dp),
                        contentPadding = PaddingValues(horizontal = 10.dp, vertical = 2.dp)
                    ) {
                        Text("PAY", color = Color.Black, fontSize = 11.sp, fontWeight = FontWeight.Bold)
                    }
                }

                IconButton(onClick = onDelete) {
                    Icon(Icons.Default.Delete, "Delete", tint = SoftTextGray)
                }
            }
        }
    }
}
