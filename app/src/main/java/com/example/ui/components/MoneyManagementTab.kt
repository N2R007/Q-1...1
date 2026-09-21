package com.example.ui.components

import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.AccountBalanceWallet
import androidx.compose.material.icons.filled.Check
import androidx.compose.material.icons.filled.Close
import androidx.compose.material.icons.filled.Refresh
import androidx.compose.material.icons.filled.TrendingUp
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.ui.theme.*
import java.util.Locale

data class MartingaleStep(
    val stepNumber: Int,
    val tradeAmount: Int,
    val cumulativeLoss: Int,
    val payoutReturn: Double,
    val netProfit: Double
)

val STANDARD_7_STEPS = listOf(
    MartingaleStep(1, 1, 1, 1.95, 0.95),
    MartingaleStep(2, 2, 3, 3.90, 0.90),
    MartingaleStep(3, 5, 8, 9.75, 1.75),
    MartingaleStep(4, 11, 19, 21.45, 2.45),
    MartingaleStep(5, 24, 43, 46.80, 3.80),
    MartingaleStep(6, 52, 95, 101.40, 6.40),
    MartingaleStep(7, 114, 209, 222.30, 13.30)
)

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun MoneyManagementTab(
    modifier: Modifier = Modifier
) {
    var capitalInput by remember { mutableStateOf("210") }
    var currentStepIndex by remember { mutableIntStateOf(0) } // 0 = Step 1, 6 = Step 7
    var sessionWinCount by remember { mutableIntStateOf(0) }
    var statusFeedback by remember { mutableStateOf<String?>(null) }

    val userCapital = capitalInput.toDoubleOrNull() ?: 210.0
    val activeStep = STANDARD_7_STEPS[currentStepIndex]

    LazyColumn(
        modifier = modifier.fillMaxSize(),
        contentPadding = PaddingValues(bottom = 60.dp),
        verticalArrangement = Arrangement.spacedBy(10.dp)
    ) {
        // 1. Header Card
        item {
            Surface(
                shape = RoundedCornerShape(10.dp),
                color = DarkCard,
                border = BorderStroke(1.dp, BorderStrokeLight),
                modifier = Modifier.fillMaxWidth()
            ) {
                Column(
                    modifier = Modifier.padding(12.dp),
                    verticalArrangement = Arrangement.spacedBy(4.dp)
                ) {
                    Row(
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.spacedBy(8.dp)
                    ) {
                        Icon(
                            imageVector = Icons.Default.AccountBalanceWallet,
                            contentDescription = "Money Management",
                            tint = NeonGreen,
                            modifier = Modifier.size(18.dp)
                        )
                        Text(
                            text = "Money Management & Recovery Matrix",
                            fontSize = 13.sp,
                            fontWeight = FontWeight.Bold,
                            color = Color.White
                        )
                    }
                    Text(
                        text = "৭-ধাপ পূর্ণসংখ্যা রিকভারি কৌশল ও ৩০ দিনের প্রজেকশন প্ল্যান (৯৫% ব্রোকার পে-আউট)",
                        fontSize = 10.sp,
                        color = TextSecondary
                    )
                }
            }
        }

        // 2. Capital Input & Quick Presets
        item {
            Surface(
                shape = RoundedCornerShape(10.dp),
                color = DarkCard,
                border = BorderStroke(1.dp, BorderStrokeLight),
                modifier = Modifier.fillMaxWidth()
            ) {
                Column(
                    modifier = Modifier.padding(12.dp),
                    verticalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    Text(
                        text = "১. আপনার মূলধন / ব্যালেন্স ইনপুট করুন:",
                        fontSize = 11.5.sp,
                        fontWeight = FontWeight.Bold,
                        color = AccentCyan
                    )

                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.spacedBy(8.dp)
                    ) {
                        OutlinedTextField(
                            value = capitalInput,
                            onValueChange = { capitalInput = it.filter { ch -> ch.isDigit() || ch == '.' } },
                            label = { Text("ব্যালেন্স ($)", fontSize = 10.sp) },
                            prefix = { Text("$", color = NeonGreen, fontWeight = FontWeight.Bold) },
                            keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
                            singleLine = true,
                            modifier = Modifier.weight(1f),
                            shape = RoundedCornerShape(8.dp),
                            colors = OutlinedTextFieldDefaults.colors(
                                focusedBorderColor = NeonGreen,
                                unfocusedBorderColor = DarkSurfaceVariant,
                                focusedTextColor = Color.White,
                                unfocusedTextColor = Color.White
                            )
                        )

                        // Reset button
                        Surface(
                            onClick = {
                                capitalInput = "210"
                                currentStepIndex = 0
                                statusFeedback = "মূলধন $210 এ রিসেট করা হয়েছে।"
                            },
                            shape = RoundedCornerShape(8.dp),
                            color = DarkSurfaceVariant,
                            border = BorderStroke(1.dp, BorderStrokeLight)
                        ) {
                            Row(
                                modifier = Modifier.padding(horizontal = 10.dp, vertical = 12.dp),
                                verticalAlignment = Alignment.CenterVertically,
                                horizontalArrangement = Arrangement.spacedBy(4.dp)
                            ) {
                                Icon(
                                    imageVector = Icons.Default.Refresh,
                                    contentDescription = "Reset",
                                    tint = TextSecondary,
                                    modifier = Modifier.size(14.dp)
                                )
                                Text("রিসেট", fontSize = 11.sp, color = TextSecondary)
                            }
                        }
                    }

                    // Quick Presets
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.spacedBy(6.dp)
                    ) {
                        listOf("100", "210", "500", "1000").forEach { preset ->
                            val isSelected = capitalInput == preset
                            Surface(
                                onClick = { capitalInput = preset },
                                shape = RoundedCornerShape(6.dp),
                                color = if (isSelected) NeonGreen.copy(alpha = 0.2f) else DarkSurfaceVariant,
                                border = BorderStroke(0.8.dp, if (isSelected) NeonGreen else BorderStrokeLight),
                                modifier = Modifier.weight(1f)
                            ) {
                                Text(
                                    text = "$$preset",
                                    fontSize = 10.5.sp,
                                    fontWeight = if (isSelected) FontWeight.Bold else FontWeight.Normal,
                                    color = if (isSelected) NeonGreen else TextSecondary,
                                    modifier = Modifier
                                        .padding(vertical = 5.dp)
                                        .wrapContentWidth(Alignment.CenterHorizontally)
                                )
                            }
                        }
                    }

                    Text(
                        text = "💡 টিপস: পুরো ৭টি ধাপের নিরাপদ ব্যাকআপের জন্য অন্তত $২০৯ ডলার ক্যাপিটাল থাকা নিরাপদ।",
                        fontSize = 9.5.sp,
                        color = AccentAmber
                    )
                }
            }
        }

        // 3. Live Step Tracker & Interactive Auto Next-Trade Advisor
        item {
            Surface(
                shape = RoundedCornerShape(10.dp),
                color = DarkCard,
                border = BorderStroke(1.2.dp, NeonGreen.copy(alpha = 0.6f)),
                modifier = Modifier.fillMaxWidth()
            ) {
                Column(
                    modifier = Modifier.padding(12.dp),
                    verticalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Text(
                            text = "২. পরবর্তী ট্রেড সাইজ অ্যাডভাইজর:",
                            fontSize = 11.5.sp,
                            fontWeight = FontWeight.Bold,
                            color = NeonGreenLight
                        )
                        Surface(
                            shape = RoundedCornerShape(4.dp),
                            color = DarkBackground,
                            border = BorderStroke(0.5.dp, BorderStrokeLight)
                        ) {
                            Text(
                                text = "ধাপ ${activeStep.stepNumber} / ৭",
                                fontSize = 10.sp,
                                fontWeight = FontWeight.Bold,
                                fontFamily = FontFamily.Monospace,
                                color = AccentAmber,
                                modifier = Modifier.padding(horizontal = 6.dp, vertical = 2.dp)
                            )
                        }
                    }

                    // Highlight Current Trade Amount
                    Surface(
                        shape = RoundedCornerShape(8.dp),
                        color = DarkSurfaceVariant,
                        border = BorderStroke(1.dp, NeonGreen),
                        modifier = Modifier.fillMaxWidth()
                    ) {
                        Column(
                            modifier = Modifier.padding(vertical = 10.dp, horizontal = 12.dp),
                            horizontalAlignment = Alignment.CenterHorizontally,
                            verticalArrangement = Arrangement.spacedBy(3.dp)
                        ) {
                            Text(
                                text = "বর্তমান ট্রেড অ্যামাউন্ট:",
                                fontSize = 10.sp,
                                color = TextSecondary
                            )
                            Text(
                                text = "$${activeStep.tradeAmount}.00",
                                fontSize = 28.sp,
                                fontWeight = FontWeight.Black,
                                fontFamily = FontFamily.Monospace,
                                color = NeonGreen
                            )
                            Text(
                                text = "৯৫% পে-আউটে উইন রিটার্ন: $${String.format(Locale.US, "%.2f", activeStep.payoutReturn)} | নিট লাভ: +$${String.format(Locale.US, "%.2f", activeStep.netProfit)}",
                                fontSize = 10.sp,
                                fontWeight = FontWeight.SemiBold,
                                color = AccentCyan
                            )
                        }
                    }

                    // Interactive Action Buttons
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.spacedBy(8.dp)
                    ) {
                        // Loss Button -> Go to next step
                        Button(
                            onClick = {
                                if (currentStepIndex < STANDARD_7_STEPS.size - 1) {
                                    currentStepIndex++
                                    statusFeedback = "❌ লস হয়েছে! পরবর্তী ধাপে যান: $${STANDARD_7_STEPS[currentStepIndex].tradeAmount} (ধাপ ${currentStepIndex + 1}/৭)"
                                } else {
                                    statusFeedback = "⚠️ ৭ম ধাপ সম্পন্ন হয়েছে। মূলধন সুরক্ষার জন্য সেশনটি সাময়িক বিরতি দিন।"
                                }
                            },
                            colors = ButtonDefaults.buttonColors(containerColor = NeonRed.copy(alpha = 0.25f)),
                            border = BorderStroke(1.dp, NeonRed),
                            shape = RoundedCornerShape(8.dp),
                            modifier = Modifier.weight(1f)
                        ) {
                            Row(
                                verticalAlignment = Alignment.CenterVertically,
                                horizontalArrangement = Arrangement.spacedBy(4.dp)
                            ) {
                                Icon(
                                    imageVector = Icons.Default.Close,
                                    contentDescription = "Loss",
                                    tint = NeonRedLight,
                                    modifier = Modifier.size(14.dp)
                                )
                                Text(
                                    text = "লস (পরের ধাপ)",
                                    fontSize = 11.sp,
                                    fontWeight = FontWeight.Bold,
                                    color = NeonRedLight
                                )
                            }
                        }

                        // Win Button -> Reset to Step 1 and record win
                        Button(
                            onClick = {
                                val profitWon = activeStep.netProfit
                                sessionWinCount++
                                currentStepIndex = 0
                                statusFeedback = "✅ অভিনন্দন! ট্রেড উইন হয়েছে! আগের সমস্ত লস কভার হয়ে +$${String.format(Locale.US, "%.2f", profitWon)} নিট লাভ অর্জিত হয়েছে। ধাপ আবার $১ এ রিসেট করা হলো।"
                            },
                            colors = ButtonDefaults.buttonColors(containerColor = NeonGreen.copy(alpha = 0.25f)),
                            border = BorderStroke(1.dp, NeonGreen),
                            shape = RoundedCornerShape(8.dp),
                            modifier = Modifier.weight(1f)
                        ) {
                            Row(
                                verticalAlignment = Alignment.CenterVertically,
                                horizontalArrangement = Arrangement.spacedBy(4.dp)
                            ) {
                                Icon(
                                    imageVector = Icons.Default.Check,
                                    contentDescription = "Win",
                                    tint = NeonGreenLight,
                                    modifier = Modifier.size(14.dp)
                                )
                                Text(
                                    text = "উইন (রিসেট $১)",
                                    fontSize = 11.sp,
                                    fontWeight = FontWeight.Bold,
                                    color = NeonGreenLight
                                )
                            }
                        }
                    }

                    if (statusFeedback != null) {
                        Text(
                            text = statusFeedback ?: "",
                            fontSize = 10.5.sp,
                            fontWeight = FontWeight.Medium,
                            color = if (statusFeedback?.contains("✅") == true) NeonGreenLight else AccentAmber
                        )
                    }
                }
            }
        }

        // 4. Complete 7-Step Recovery Table
        item {
            Surface(
                shape = RoundedCornerShape(10.dp),
                color = DarkCard,
                border = BorderStroke(1.dp, BorderStrokeLight),
                modifier = Modifier.fillMaxWidth()
            ) {
                Column(
                    modifier = Modifier.padding(12.dp),
                    verticalArrangement = Arrangement.spacedBy(6.dp)
                ) {
                    Text(
                        text = "৩. ৭-ধাপ রিকভারি ম্যাট্রিক্স টেবিল:",
                        fontSize = 11.5.sp,
                        fontWeight = FontWeight.Bold,
                        color = Color.White
                    )

                    // Table Header
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .background(DarkSurfaceVariant, RoundedCornerShape(4.dp))
                            .padding(horizontal = 6.dp, vertical = 5.dp),
                        horizontalArrangement = Arrangement.SpaceBetween
                    ) {
                        Text("ধাপ", fontSize = 9.5.sp, fontWeight = FontWeight.Bold, color = TextSecondary, modifier = Modifier.weight(0.7f))
                        Text("ট্রেড", fontSize = 9.5.sp, fontWeight = FontWeight.Bold, color = TextSecondary, modifier = Modifier.weight(0.9f))
                        Text("জমা লস", fontSize = 9.5.sp, fontWeight = FontWeight.Bold, color = TextSecondary, modifier = Modifier.weight(1.0f))
                        Text("রিটার্ন", fontSize = 9.5.sp, fontWeight = FontWeight.Bold, color = TextSecondary, modifier = Modifier.weight(1.0f))
                        Text("নিট লাভ", fontSize = 9.5.sp, fontWeight = FontWeight.Bold, color = NeonGreenLight, modifier = Modifier.weight(1.1f))
                    }

                    // Table Rows
                    STANDARD_7_STEPS.forEach { step ->
                        val isCurrent = step.stepNumber == activeStep.stepNumber
                        Surface(
                            shape = RoundedCornerShape(4.dp),
                            color = if (isCurrent) NeonGreen.copy(alpha = 0.15f) else DarkBackground,
                            border = BorderStroke(
                                width = if (isCurrent) 1.dp else 0.5.dp,
                                color = if (isCurrent) NeonGreen else BorderStrokeLight
                            ),
                            modifier = Modifier.fillMaxWidth()
                        ) {
                            Row(
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .padding(horizontal = 6.dp, vertical = 4.5.dp),
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                Text(
                                    text = if (isCurrent) "▶ ${step.stepNumber}" else "${step.stepNumber}",
                                    fontSize = 10.sp,
                                    fontWeight = if (isCurrent) FontWeight.Bold else FontWeight.Normal,
                                    color = if (isCurrent) NeonGreen else TextSecondary,
                                    modifier = Modifier.weight(0.7f)
                                )
                                Text(
                                    text = "$${step.tradeAmount}",
                                    fontSize = 10.sp,
                                    fontWeight = FontWeight.Bold,
                                    fontFamily = FontFamily.Monospace,
                                    color = Color.White,
                                    modifier = Modifier.weight(0.9f)
                                )
                                Text(
                                    text = "$${step.cumulativeLoss}",
                                    fontSize = 10.sp,
                                    fontFamily = FontFamily.Monospace,
                                    color = NeonRedLight,
                                    modifier = Modifier.weight(1.0f)
                                )
                                Text(
                                    text = "$${String.format(Locale.US, "%.2f", step.payoutReturn)}",
                                    fontSize = 10.sp,
                                    fontFamily = FontFamily.Monospace,
                                    color = TextSecondary,
                                    modifier = Modifier.weight(1.0f)
                                )
                                Text(
                                    text = "+$${String.format(Locale.US, "%.2f", step.netProfit)}",
                                    fontSize = 10.sp,
                                    fontWeight = FontWeight.Bold,
                                    fontFamily = FontFamily.Monospace,
                                    color = NeonGreen,
                                    modifier = Modifier.weight(1.1f)
                                )
                            }
                        }
                    }
                }
            }
        }

        // 5. 30-Day Growth & Earning Projection Chart
        item {
            Surface(
                shape = RoundedCornerShape(10.dp),
                color = DarkCard,
                border = BorderStroke(1.dp, BorderStrokeLight),
                modifier = Modifier.fillMaxWidth()
            ) {
                Column(
                    modifier = Modifier.padding(12.dp),
                    verticalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Row(
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.spacedBy(6.dp)
                        ) {
                            Icon(
                                imageVector = Icons.Default.TrendingUp,
                                contentDescription = "30-Day Projection",
                                tint = AccentCyan,
                                modifier = Modifier.size(16.dp)
                            )
                            Text(
                                text = "৪. ৩০ দিনের আর্নিং প্রজেকশন প্ল্যান:",
                                fontSize = 11.5.sp,
                                fontWeight = FontWeight.Bold,
                                color = Color.White
                            )
                        }
                        Text(
                            text = "দৈনিক টার্গেট: ~$২ লাভ",
                            fontSize = 9.5.sp,
                            color = AccentAmber,
                            fontWeight = FontWeight.SemiBold
                        )
                    }

                    // 30 Days Summary Cards
                    val dailyTarget = 2.0
                    val finalProjectedBalance = userCapital + (30 * dailyTarget)
                    val totalProfit = 30 * dailyTarget
                    val roiPercent = if (userCapital > 0) (totalProfit / userCapital) * 100.0 else 0.0

                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.spacedBy(6.dp)
                    ) {
                        // Starting Capital
                        Surface(
                            shape = RoundedCornerShape(6.dp),
                            color = DarkSurfaceVariant,
                            border = BorderStroke(0.5.dp, BorderStrokeLight),
                            modifier = Modifier.weight(1f)
                        ) {
                            Column(
                                modifier = Modifier.padding(6.dp),
                                horizontalAlignment = Alignment.CenterHorizontally
                            ) {
                                Text("শুরুর ক্যাপিটাল", fontSize = 8.5.sp, color = TextMuted)
                                Text(
                                    text = "$${String.format(Locale.US, "%.0f", userCapital)}",
                                    fontSize = 12.sp,
                                    fontWeight = FontWeight.Bold,
                                    color = Color.White
                                )
                            }
                        }

                        // Projected Balance after 30 days
                        Surface(
                            shape = RoundedCornerShape(6.dp),
                            color = DarkSurfaceVariant,
                            border = BorderStroke(0.8.dp, NeonGreen.copy(alpha = 0.6f)),
                            modifier = Modifier.weight(1.3f)
                        ) {
                            Column(
                                modifier = Modifier.padding(6.dp),
                                horizontalAlignment = Alignment.CenterHorizontally
                            ) {
                                Text("৩০ দিন পর আনুমানিক ব্যালেন্স", fontSize = 8.5.sp, color = TextMuted)
                                Text(
                                    text = "$${String.format(Locale.US, "%.2f", finalProjectedBalance)}",
                                    fontSize = 13.sp,
                                    fontWeight = FontWeight.Black,
                                    color = NeonGreen
                                )
                            }
                        }

                        // Net Profit
                        Surface(
                            shape = RoundedCornerShape(6.dp),
                            color = DarkSurfaceVariant,
                            border = BorderStroke(0.5.dp, BorderStrokeLight),
                            modifier = Modifier.weight(1f)
                        ) {
                            Column(
                                modifier = Modifier.padding(6.dp),
                                horizontalAlignment = Alignment.CenterHorizontally
                            ) {
                                Text("মোট আনুমানিক লাভ", fontSize = 8.5.sp, color = TextMuted)
                                Text(
                                    text = "+$${String.format(Locale.US, "%.0f", totalProfit)}",
                                    fontSize = 12.sp,
                                    fontWeight = FontWeight.Bold,
                                    color = AccentCyan
                                )
                            }
                        }
                    }

                    // Scrollable 30 Days Breakdown List
                    Text(
                        text = "দিনভিত্তিক বিস্তারিত হিসাব (Day 1 - Day 30):",
                        fontSize = 10.sp,
                        fontWeight = FontWeight.SemiBold,
                        color = TextSecondary
                    )

                    // Complete 30 Days Breakdown List (Scroll to view Day 1 to Day 30)
                    Column(
                        modifier = Modifier.fillMaxWidth(),
                        verticalArrangement = Arrangement.spacedBy(3.dp)
                    ) {
                        (1..30).forEach { day ->
                            val startBal = userCapital + ((day - 1) * dailyTarget)
                            val endBal = userCapital + (day * dailyTarget)
                            val isMilestone = day % 5 == 0 || day == 30

                            Surface(
                                shape = RoundedCornerShape(4.dp),
                                color = when {
                                    day == 30 -> NeonGreen.copy(alpha = 0.15f)
                                    isMilestone -> DarkSurfaceVariant
                                    else -> DarkBackground
                                },
                                border = BorderStroke(
                                    width = if (day == 30) 1.dp else 0.5.dp,
                                    color = when {
                                        day == 30 -> NeonGreen
                                        isMilestone -> AccentCyan.copy(alpha = 0.5f)
                                        else -> BorderStrokeLight
                                    }
                                ),
                                modifier = Modifier.fillMaxWidth()
                            ) {
                                Row(
                                    modifier = Modifier
                                        .fillMaxWidth()
                                        .padding(horizontal = 8.dp, vertical = 4.dp),
                                    horizontalArrangement = Arrangement.SpaceBetween,
                                    verticalAlignment = Alignment.CenterVertically
                                ) {
                                    Text(
                                        text = if (day == 30) "★ দিন ৩০" else "দিন %02d".format(day),
                                        fontSize = 10.sp,
                                        fontWeight = FontWeight.Bold,
                                        color = when {
                                            day == 30 -> NeonGreen
                                            isMilestone -> AccentCyan
                                            else -> TextSecondary
                                        }
                                    )
                                    Text(
                                        text = "শুরু: $${String.format(Locale.US, "%.1f", startBal)}",
                                        fontSize = 9.sp,
                                        fontFamily = FontFamily.Monospace,
                                        color = TextMuted
                                    )
                                    Text(
                                        text = "প্রফিট: +$$dailyTarget",
                                        fontSize = 9.sp,
                                        fontWeight = FontWeight.SemiBold,
                                        fontFamily = FontFamily.Monospace,
                                        color = NeonGreenLight
                                    )
                                    Text(
                                        text = "দিন শেষে: $${String.format(Locale.US, "%.1f", endBal)}",
                                        fontSize = 10.sp,
                                        fontWeight = FontWeight.Bold,
                                        fontFamily = FontFamily.Monospace,
                                        color = if (day == 30) NeonGreen else Color.White
                                    )
                                }
                            }
                        }
                    }
                }
            }
        }
    }
}
