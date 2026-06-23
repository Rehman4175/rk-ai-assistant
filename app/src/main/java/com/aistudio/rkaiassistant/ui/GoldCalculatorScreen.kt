package com.aistudio.rkaiassistant.ui

import com.aistudio.rkaiassistant.ui.theme.*
import com.aistudio.rkaiassistant.viewmodel.AssistantViewModel

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Calculate
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.lifecycle.compose.collectAsStateWithLifecycle

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun GoldCalculatorScreen(viewModel: AssistantViewModel) {
    val weight by viewModel.goldWeight.collectAsStateWithLifecycle()
    val karat by viewModel.goldKarat.collectAsStateWithLifecycle()
    val price by viewModel.goldPricePerGram.collectAsStateWithLifecycle()
    val makingCharge by viewModel.makingChargePerGram.collectAsStateWithLifecycle()
    val gstRate by viewModel.goldGstRate.collectAsStateWithLifecycle()
    val result by viewModel.goldTotalEstimate.collectAsStateWithLifecycle()

    Column(
        modifier = Modifier
            .fillMaxSize()
            .background(SlateDarkBackground)
    ) {
        TopAppBar(
            title = { Text("GOLD ESTIMATE CALCULATOR", fontWeight = FontWeight.ExtraBold, color = NeonCyan, letterSpacing = 1.sp) },
            colors = TopAppBarDefaults.topAppBarColors(containerColor = SlateDarkBackground)
        )

        LazyColumn(
            modifier = Modifier.padding(16.dp),
            verticalArrangement = Arrangement.spacedBy(20.dp)
        ) {
            // Input Section
            item {
                Card(
                    colors = CardDefaults.cardColors(containerColor = CardBackgroundGlass),
                    shape = RoundedCornerShape(20.dp),
                    modifier = Modifier.border(1.dp, BorderColor, RoundedCornerShape(20.dp))
                ) {
                    Column(modifier = Modifier.padding(20.dp), verticalArrangement = Arrangement.spacedBy(16.dp)) {
                        
                        CalculatorTextField(
                            value = weight,
                            onValueChange = { viewModel.goldWeight.value = it },
                            label = "Gold Weight (Grams)",
                            placeholder = "e.g. 10.0"
                        )

                        Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(12.dp)) {
                            Box(modifier = Modifier.weight(1f)) {
                                CalculatorTextField(
                                    value = karat,
                                    onValueChange = { viewModel.goldKarat.value = it },
                                    label = "Purity (Karat)",
                                    placeholder = "22"
                                )
                            }
                            Box(modifier = Modifier.weight(1f)) {
                                CalculatorTextField(
                                    value = gstRate,
                                    onValueChange = { viewModel.goldGstRate.value = it },
                                    label = "GST (%)",
                                    placeholder = "3.0"
                                )
                            }
                        }

                        CalculatorTextField(
                            value = price,
                            onValueChange = { viewModel.goldPricePerGram.value = it },
                            label = "Current Gold Rate (Per 1g)",
                            placeholder = "e.g. 7200"
                        )

                        CalculatorTextField(
                            value = makingCharge,
                            onValueChange = { viewModel.makingChargePerGram.value = it },
                            label = "Making Charges (Per 1g)",
                            placeholder = "e.g. 500"
                        )
                    }
                }
            }

            // Results Section
            item {
                Text(
                    "ESTIMATION SUMMARY",
                    color = SoftTextGray,
                    fontSize = 12.sp,
                    fontWeight = FontWeight.Bold,
                    letterSpacing = 1.sp
                )
                
                Spacer(modifier = Modifier.height(12.dp))

                Card(
                    colors = CardDefaults.cardColors(containerColor = Color(0xFF1B1D30)),
                    shape = RoundedCornerShape(24.dp),
                    modifier = Modifier.border(1.dp, NeonCyan.copy(alpha = 0.3f), RoundedCornerShape(24.dp))
                ) {
                    Column(modifier = Modifier.padding(24.dp)) {
                        ResultRow("Pure Gold Value", "₹${String.format(java.util.Locale.US, "%.2f", result.goldValue)}")
                        ResultRow("Making Charges", "₹${String.format(java.util.Locale.US, "%.2f", result.totalMakingCharges)}")
                        ResultRow("GST Amount (${gstRate}%)", "₹${String.format(java.util.Locale.US, "%.2f", result.gstAmount)}")
                        
                        HorizontalDivider(modifier = Modifier.padding(vertical = 16.dp), color = BorderColor)
                        
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.SpaceBetween,
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Text("TOTAL ESTIMATE", color = NeonCyan, fontWeight = FontWeight.Black, fontSize = 18.sp)
                            Text(
                                "₹${String.format(java.util.Locale.US, "%.2f", result.finalPrice)}",
                                color = Color.White,
                                fontSize = 24.sp,
                                fontWeight = FontWeight.Black
                            )
                        }
                    }
                }
            }

            item {
                Column(
                    modifier = Modifier
                        .fillMaxWidth()
                        .clip(RoundedCornerShape(16.dp))
                        .background(Color.Yellow.copy(alpha = 0.05f))
                        .padding(16.dp)
                ) {
                    Text(
                        "Note: This is an estimated calculation based on the provided inputs. Market rates and actual jeweller charges may vary.",
                        color = SoftTextGray,
                        fontSize = 11.sp,
                        lineHeight = 16.sp
                    )
                }
            }
            
            item { Spacer(modifier = Modifier.height(100.dp)) }
        }
    }
}

@Composable
fun CalculatorTextField(
    value: String,
    onValueChange: (String) -> Unit,
    label: String,
    placeholder: String
) {
    Column {
        Text(label, color = NeonCyan, fontSize = 11.sp, fontWeight = FontWeight.Bold, modifier = Modifier.padding(start = 4.dp, bottom = 4.dp))
        OutlinedTextField(
            value = value,
            onValueChange = onValueChange,
            placeholder = { Text(placeholder, color = SoftTextGray.copy(alpha = 0.5f)) },
            modifier = Modifier.fillMaxWidth(),
            shape = RoundedCornerShape(12.dp),
            keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Decimal),
            colors = OutlinedTextFieldDefaults.colors(
                focusedTextColor = Color.White,
                unfocusedTextColor = Color.White,
                focusedBorderColor = NeonCyan,
                unfocusedBorderColor = BorderColor,
                focusedContainerColor = Color.Black.copy(alpha = 0.2f),
                unfocusedContainerColor = Color.Black.copy(alpha = 0.2f)
            ),
            singleLine = true
        )
    }
}

@Composable
fun ResultRow(label: String, value: String) {
    Row(
        modifier = Modifier.fillMaxWidth().padding(vertical = 4.dp),
        horizontalArrangement = Arrangement.SpaceBetween
    ) {
        Text(label, color = SoftTextGray, fontSize = 14.sp)
        Text(value, color = Color.White, fontSize = 14.sp, fontWeight = FontWeight.Bold)
    }
}
