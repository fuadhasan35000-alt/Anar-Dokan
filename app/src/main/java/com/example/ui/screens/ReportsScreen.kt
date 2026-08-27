package com.example.ui.screens

import android.content.Intent
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.ui.theme.*
import com.example.ui.viewmodel.MainViewModel
import com.example.ui.viewmodel.ReportPeriod
import com.example.ui.viewmodel.ReportSummary
import com.example.ui.viewmodel.ReportViewModel

@Composable
fun ReportsScreen(
    mainViewModel: MainViewModel,
    modifier: Modifier = Modifier
) {
    val context = LocalContext.current
    val reportViewModel = remember { ReportViewModel(mainViewModel.db) }

    val currentBusiness by mainViewModel.currentBusiness.collectAsState()
    val currentBranch by mainViewModel.currentBranch.collectAsState()

    val businessId = currentBusiness?.id ?: ""
    val branchId = currentBranch?.id ?: ""

    val selectedPeriod by reportViewModel.selectedPeriod.collectAsState()

    val summary by remember(businessId, branchId, selectedPeriod) {
        if (businessId.isNotEmpty() && branchId.isNotEmpty()) {
            reportViewModel.getReportSummary(businessId, branchId)
        } else {
            kotlinx.coroutines.flow.flowOf(
                ReportSummary(0.0, 0.0, 0.0, 0.0, 0.0, 0.0, 0)
            )
        }
    }.collectAsState(initial = ReportSummary(0.0, 0.0, 0.0, 0.0, 0.0, 0.0, 0))

    val branchComparison by remember(businessId, selectedPeriod) {
        if (businessId.isNotEmpty()) {
            reportViewModel.getBranchComparison(businessId)
        } else {
            kotlinx.coroutines.flow.flowOf(emptyList())
        }
    }.collectAsState(initial = emptyList())

    LazyColumn(
        modifier = modifier
            .fillMaxSize()
            .padding(horizontal = 16.dp),
        contentPadding = PaddingValues(top = 12.dp, bottom = 90.dp),
        verticalArrangement = Arrangement.spacedBy(16.dp)
    ) {
        // Period Selector
        item {
            LazyRow(
                horizontalArrangement = Arrangement.spacedBy(8.dp),
                modifier = Modifier.fillMaxWidth()
            ) {
                items(ReportPeriod.values()) { period ->
                    FilterChip(
                        selected = selectedPeriod == period,
                        onClick = { reportViewModel.setPeriod(period) },
                        label = { Text(period.banglaTitle, fontWeight = if (selectedPeriod == period) FontWeight.Bold else FontWeight.Normal) }
                    )
                }
            }
        }

        // Main Analytics Cards
        item {
            Card(
                shape = RoundedCornerShape(18.dp),
                colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.primaryContainer),
                modifier = Modifier.fillMaxWidth()
            ) {
                Column(modifier = Modifier.padding(18.dp)) {
                    Text(
                        text = "মোট বিক্রি (${selectedPeriod.banglaTitle})",
                        style = MaterialTheme.typography.labelLarge.copy(
                            color = MaterialTheme.colorScheme.onPrimaryContainer.copy(alpha = 0.8f)
                        )
                    )
                    Text(
                        text = "৳${summary.totalSalesAmount.toInt()}",
                        style = MaterialTheme.typography.headlineLarge.copy(
                            fontWeight = FontWeight.ExtraBold,
                            color = MaterialTheme.colorScheme.onPrimaryContainer
                        )
                    )
                    Spacer(modifier = Modifier.height(4.dp))
                    Text(
                        text = "মোট সম্পন্ন মেমো: ${summary.salesCount} টি",
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onPrimaryContainer.copy(alpha = 0.9f)
                    )
                }
            }
        }

        // Financial Grid
        item {
            Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(10.dp)) {
                ReportStatCard(
                    title = "নগদ আদায়",
                    value = "৳${summary.totalPaidAmount.toInt()}",
                    color = SuccessGreen,
                    containerColor = ProfitGreen,
                    modifier = Modifier.weight(1f)
                )
                ReportStatCard(
                    title = "বকেয়া বাকি",
                    value = "৳${summary.totalDueAmount.toInt()}",
                    color = DueTextRed,
                    containerColor = DueCardRed,
                    modifier = Modifier.weight(1f)
                )
            }
        }

        item {
            Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(10.dp)) {
                ReportStatCard(
                    title = "মোট খরচ",
                    value = "৳${summary.totalExpensesAmount.toInt()}",
                    color = ErrorRed,
                    containerColor = MaterialTheme.colorScheme.errorContainer.copy(alpha = 0.3f),
                    modifier = Modifier.weight(1f)
                )
                ReportStatCard(
                    title = "আনুমানিক নীট লাভ",
                    value = "৳${summary.estimatedNetProfit.toInt()}",
                    color = EmeraldPrimary,
                    containerColor = EmeraldPrimaryContainer.copy(alpha = 0.4f),
                    modifier = Modifier.weight(1f)
                )
            }
        }

        // Branch Comparison (Multi-branch)
        if (branchComparison.size > 1) {
            item {
                Text(
                    text = "শাখাভিত্তিক তুলনামূলক রিপোর্ট",
                    style = MaterialTheme.typography.titleMedium.copy(fontWeight = FontWeight.Bold)
                )
            }

            items(branchComparison) { branchRep ->
                Card(
                    shape = RoundedCornerShape(12.dp),
                    colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
                    elevation = CardDefaults.cardElevation(defaultElevation = 1.dp),
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(14.dp),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Column {
                            Text(branchRep.branch.name, fontWeight = FontWeight.Bold)
                            Text("মেমো: ${branchRep.salesCount} টি", style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
                        }
                        Column(horizontalAlignment = Alignment.End) {
                            Text("৳${branchRep.totalSales.toInt()}", fontWeight = FontWeight.Bold, color = MaterialTheme.colorScheme.primary)
                            Text("বাকি: ৳${branchRep.totalDue.toInt()}", fontSize = 12.sp, color = DueTextRed)
                        }
                    }
                }
            }
        }

        // Export Report Button
        item {
            Spacer(modifier = Modifier.height(10.dp))
            OutlinedButton(
                onClick = {
                    val csvData = reportViewModel.generateCsvReport(businessId, branchId, summary, selectedPeriod)
                    val sendIntent = Intent(Intent.ACTION_SEND).apply {
                        type = "text/plain"
                        putExtra(Intent.EXTRA_TEXT, csvData)
                        putExtra(Intent.EXTRA_SUBJECT, "আমার দোকান - রিপোর্ট (${selectedPeriod.banglaTitle})")
                    }
                    context.startActivity(Intent.createChooser(sendIntent, "রিপোর্ট শেয়ার করুন"))
                },
                modifier = Modifier
                    .fillMaxWidth()
                    .height(48.dp)
                    .testTag("export_report_button")
            ) {
                Icon(Icons.Default.Share, contentDescription = null)
                Spacer(modifier = Modifier.width(8.dp))
                Text("রিপোর্ট শেয়ার / এক্সপোর্ট করুন")
            }
        }
    }
}

@Composable
fun ReportStatCard(
    title: String,
    value: String,
    color: Color,
    containerColor: Color,
    modifier: Modifier = Modifier
) {
    Card(
        shape = RoundedCornerShape(14.dp),
        colors = CardDefaults.cardColors(containerColor = containerColor),
        modifier = modifier
    ) {
        Column(modifier = Modifier.padding(14.dp)) {
            Text(text = title, style = MaterialTheme.typography.labelSmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
            Spacer(modifier = Modifier.height(4.dp))
            Text(text = value, style = MaterialTheme.typography.titleMedium.copy(fontWeight = FontWeight.ExtraBold, color = color))
        }
    }
}
