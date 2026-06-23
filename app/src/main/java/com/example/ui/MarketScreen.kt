package com.aistudio.rkaiassistant.ui

import com.aistudio.rkaiassistant.ui.theme.*
import com.aistudio.rkaiassistant.data.*
import com.aistudio.rkaiassistant.viewmodel.AssistantViewModel

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Refresh
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import kotlinx.coroutines.launch

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun MarketScreen(viewModel: AssistantViewModel) {
    val goldRate by viewModel.goldRate.collectAsState()
    val silverRate by viewModel.silverRate.collectAsState()
    val stocks by viewModel.stockRates.collectAsState()
    val news by viewModel.newsHeadlines.collectAsState()
    val isLoading by viewModel.isMarketLoading.collectAsState()
    val scope = rememberCoroutineScope()

    LaunchedEffect(Unit) {
        viewModel.fetchMarketData()
    }

    Column(
        modifier = Modifier
            .fillMaxSize()
            .background(SlateDarkBackground)
    ) {
        TopAppBar(
            title = { Text("FINANCE & NEWS", fontWeight = FontWeight.Bold, color = NeonCyan) },
            colors = TopAppBarDefaults.topAppBarColors(containerColor = SlateDarkBackground),
            actions = {
                IconButton(onClick = { scope.launch { viewModel.fetchMarketData() } }) {
                    Icon(Icons.Default.Refresh, contentDescription = "Refresh", tint = NeonCyan)
                }
            }
        )

        LazyColumn(
            modifier = Modifier.padding(16.dp),
            verticalArrangement = Arrangement.spacedBy(16.dp)
        ) {
            // Metals Section
            item {
                Text("METALS (DELHI ESTIMATE)", color = SoftTextGray, fontSize = 12.sp, fontWeight = FontWeight.Bold)
                Row(modifier = Modifier.fillMaxWidth().padding(top = 8.dp), horizontalArrangement = Arrangement.spacedBy(16.dp)) {
                    MarketCard(Modifier.weight(1f), "GOLD 24K", goldRate, Color(0xFFFFD700))
                    MarketCard(Modifier.weight(1f), "SILVER", silverRate, Color(0xFFC0C0C0))
                }
            }

            // Stocks Section
            item {
                Text("STOCKS & INDICES", color = SoftTextGray, fontSize = 12.sp, fontWeight = FontWeight.Bold)
            }
            items(stocks.toList()) { (symbol, price) ->
                StockRow(symbol, price)
            }

            // News Section
            item {
                Text("LATEST HEADLINES", color = SoftTextGray, fontSize = 12.sp, fontWeight = FontWeight.Bold)
            }
            items(news) { item ->
                NewsCard(item)
            }
            
            item { Spacer(modifier = Modifier.height(80.dp)) }
        }
    }
}

@Composable
fun MarketCard(modifier: Modifier, title: String, value: String, color: Color) {
    Box(
        modifier = modifier
            .clip(RoundedCornerShape(16.dp))
            .background(CardBackgroundGlass)
            .border(1.dp, color.copy(alpha = 0.5f), RoundedCornerShape(16.dp))
            .padding(16.dp)
    ) {
        Column {
            Text(title, color = color, fontSize = 10.sp, fontWeight = FontWeight.Bold)
            Text(value, color = Color.White, fontSize = 16.sp, fontWeight = FontWeight.ExtraBold)
        }
    }
}

@Composable
fun StockRow(symbol: String, price: String) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(12.dp))
            .background(CardBackgroundGlass)
            .padding(16.dp),
        horizontalArrangement = Arrangement.SpaceBetween,
        verticalAlignment = Alignment.CenterVertically
    ) {
        Text(symbol, color = Color.White, fontWeight = FontWeight.Bold)
        Text(price, color = NeonGreen, fontWeight = FontWeight.ExtraBold)
    }
}

@Composable
fun NewsCard(item: AssistantViewModel.NewsItem) {
    Column(
        modifier = Modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(12.dp))
            .background(CardBackgroundGlass)
            .padding(16.dp)
    ) {
        Text(item.source.uppercase(), color = NeonPink, fontSize = 9.sp, fontWeight = FontWeight.Bold)
        Text(item.title, color = Color.White, fontSize = 14.sp, fontWeight = FontWeight.Bold)
        Text(item.description, color = SoftTextGray, fontSize = 12.sp, maxLines = 2)
    }
}
