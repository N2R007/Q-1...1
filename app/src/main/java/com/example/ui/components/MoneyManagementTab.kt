package com.example.ui.components

import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.text.style.TextAlign
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
    var capitalInput by remember { mutableStateOf("100") }
    var percentageInput by remember { mutableStateOf("5") }
    var currentStepIndex by remember { mutableIntStateOf(0) } // 0 = Step 1, 6 = Step 7
    var sessionWinCount by remember { mutableIntStateOf(0) }
    var statusFeedback by remember { mutableStateOf<String?>(null) }
    var showAll30Days by remember { mutableStateOf(false) }

    val userCapital = capitalInput.toDoubleOrNull() ?: 0.0
    val userPercent = percentageInput.toDoubleOrNull() ?: 0.0
    val dailyTarget = if (userCapital > 0.0 && userPercent > 0.0) {
        (userCapital * userPercent) / 100.0
    } else {
        0.0
    }
    val activeStep = STANDARD_7_STEPS[currentStepIndex]

    LazyColumn(
        modifier = modifier.fillMaxSize(),
        contentPadding = PaddingValues(horizontal = 4.dp, vertical = 6.dp),
        verticalArrangement = Arrangement.spacedBy(10.dp)
    ) {
        // ==========================================
        // 1. Capital & Daily Target Configuration Card
        // ==========================================
        item {
            Surface(
                shape = RoundedCornerShape(12.dp),
                color = DarkCard,
                border = BorderStroke(1.dp, BorderStrokeLight),
                modifier = Modifier.fillMaxWidth()
            ) {
                Column(
                    modifier = Modifier.padding(12.dp),
                    verticalArrangement = Arrangement.spacedBy(10.dp)
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
                                imageVector = Icons.Default.Tune,
                                contentDescription = "Config",
                                tint = AccentCyan,
                                modifier = Modifier.size(16.dp)
                            )
                            Text(
                                text = "Capital & Target",
                                fontSize = 12.5.sp,
                                fontWeight = FontWeight.Bold,
                                color = TextPrimary
                            )
                        }

                        Row(
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.spacedBy(6.dp)
                        ) {
                            // Broker Payout Indicator Badge
                            Surface(
                                shape = RoundedCornerShape(6.dp),
                                color = AccentCyan.copy(alpha = 0.12f),
                                border = BorderStroke(0.6.dp, AccentCyan.copy(alpha = 0.4f))
                            ) {
                                Text(
                                    text = "৯৫% পে-আউট",
                                    fontSize = 9.sp,
                                    fontWeight = FontWeight.Bold,
                                    color = AccentCyan,
                                    modifier = Modifier.padding(horizontal = 6.dp, vertical = 2.dp)
                                )
                            }

                            if (userCapital in 1.0..208.0) {
                                Surface(
                                    shape = RoundedCornerShape(4.dp),
                                    color = AccentAmber.copy(alpha = 0.15f),
                                    border = BorderStroke(0.5.dp, AccentAmber.copy(alpha = 0.5f))
                                ) {
                                    Text(
                                        text = "৭ম ধাপে $২০৯ কুশন প্রয়োজন",
                                        fontSize = 8.5.sp,
                                        fontWeight = FontWeight.Bold,
                                        color = AccentAmber,
                                        modifier = Modifier.padding(horizontal = 5.dp, vertical = 2.dp)
                                    )
                                }
                            }
                        }
                    }

                    // Input Text Fields Row
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.spacedBy(8.dp)
                    ) {
                        // 1st Box: Balance Input ($)
                        OutlinedTextField(
                            value = capitalInput,
                            onValueChange = { input ->
                                capitalInput = input.filter { ch -> ch.isDigit() || ch == '.' }
                            },
                            label = { Text("ব্যালেন্স / মূলধন", fontSize = 10.sp) },
                            prefix = { Text("$ ", color = NeonGreen, fontWeight = FontWeight.Bold, fontSize = 13.sp) },
                            placeholder = { Text("100", fontSize = 11.sp, color = TextMuted) },
                            trailingIcon = if (capitalInput.isNotEmpty()) {
                                {
                                    IconButton(
                                        onClick = { capitalInput = "" },
                                        modifier = Modifier.size(24.dp)
                                    ) {
                                        Icon(
                                            imageVector = Icons.Default.Close,
                                            contentDescription = "Clear Balance",
                                            tint = TextMuted,
                                            modifier = Modifier.size(13.dp)
                                        )
                                    }
                                }
                            } else null,
                            keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Decimal),
                            singleLine = true,
                            modifier = Modifier.weight(1f),
                            shape = RoundedCornerShape(8.dp),
                            colors = OutlinedTextFieldDefaults.colors(
                                focusedBorderColor = NeonGreen,
                                unfocusedBorderColor = BorderStrokeLight,
                                focusedTextColor = Color.White,
                                unfocusedTextColor = Color.White,
                                focusedContainerColor = DarkSurfaceVariant,
                                unfocusedContainerColor = DarkSurfaceVariant,
                                cursorColor = NeonGreen
                            )
                        )

                        // 2nd Box: Percentage Input (%)
                        OutlinedTextField(
                            value = percentageInput,
                            onValueChange = { input ->
                                percentageInput = input.filter { ch -> ch.isDigit() || ch == '.' }
                            },
                            label = { Text("দৈনিক টার্গেট", fontSize = 10.sp) },
                            prefix = { Text("% ", color = AccentCyan, fontWeight = FontWeight.Bold, fontSize = 13.sp) },
                            placeholder = { Text("5", fontSize = 11.sp, color = TextMuted) },
                            trailingIcon = if (percentageInput.isNotEmpty()) {
                                {
                                    IconButton(
                                        onClick = { percentageInput = "" },
                                        modifier = Modifier.size(24.dp)
                                    ) {
                                        Icon(
                                            imageVector = Icons.Default.Close,
                                            contentDescription = "Clear Percentage",
                                            tint = TextMuted,
                                            modifier = Modifier.size(13.dp)
                                        )
                                    }
                                }
                            } else null,
                            keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Decimal),
                            singleLine = true,
                            modifier = Modifier.weight(1f),
                            shape = RoundedCornerShape(8.dp),
                            colors = OutlinedTextFieldDefaults.colors(
                                focusedBorderColor = AccentCyan,
                                unfocusedBorderColor = BorderStrokeLight,
                                focusedTextColor = Color.White,
                                unfocusedTextColor = Color.White,
                                focusedContainerColor = DarkSurfaceVariant,
                                unfocusedContainerColor = DarkSurfaceVariant,
                                cursorColor = AccentCyan
                            )
                        )
                    }

                    // Dynamic Daily Target Highlight Banner
                    if (userCapital > 0.0 && userPercent > 0.0) {
                        Surface(
                            shape = RoundedCornerShape(8.dp),
                            color = NeonGreenDim,
                            border = BorderStroke(0.8.dp, NeonGreen.copy(alpha = 0.5f)),
                            modifier = Modifier.fillMaxWidth()
                        ) {
                            Row(
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .padding(horizontal = 12.dp, vertical = 8.dp),
                                verticalAlignment = Alignment.CenterVertically,
                                horizontalArrangement = Arrangement.SpaceBetween
                            ) {
                                Column(verticalArrangement = Arrangement.spacedBy(1.dp)) {
                                    Text(
                                        text = "দৈনিক লক্ষ্যমাত্রা অর্জন (${String.format(Locale.US, "%.1f", userPercent)}%):",
                                        fontSize = 10.sp,
                                        color = TextSecondary
                                    )
                                    Text(
                                        text = "প্রতিদিন এই নিট লাভ অর্জিত হলেই ট্রেডিং সেশন বন্ধ রাখুন",
                                        fontSize = 8.5.sp,
                                        color = TextMuted
                                    )
                                }
                                Surface(
                                    shape = RoundedCornerShape(6.dp),
                                    color = NeonGreen.copy(alpha = 0.2f)
                                ) {
                                    Text(
                                        text = "+$${String.format(Locale.US, "%.2f", dailyTarget)} / দিন",
                                        fontSize = 12.sp,
                                        fontWeight = FontWeight.ExtraBold,
                                        fontFamily = FontFamily.Monospace,
                                        color = NeonGreen,
                                        modifier = Modifier.padding(horizontal = 8.dp, vertical = 4.dp)
                                    )
                                }
                            }
                        }
                    }
                }
            }
        }

        // ==========================================
        // 3. Smart Next-Trade Execution Card
        // ==========================================
        item {
            Surface(
                shape = RoundedCornerShape(12.dp),
                color = DarkCard,
                border = BorderStroke(1.2.dp, if (activeStep.stepNumber == 1) NeonGreen.copy(alpha = 0.6f) else AccentAmber.copy(alpha = 0.8f)),
                modifier = Modifier.fillMaxWidth()
            ) {
                Column(
                    modifier = Modifier.padding(12.dp),
                    verticalArrangement = Arrangement.spacedBy(10.dp)
                ) {
                    // Card Top Bar with Step Dots & Reset
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
                                contentDescription = "Trade Size",
                                tint = if (activeStep.stepNumber == 1) NeonGreen else AccentAmber,
                                modifier = Modifier.size(16.dp)
                            )
                            Text(
                                text = "২. পরবর্তী ট্রেড সাইজ অ্যাডভাইজর",
                                fontSize = 12.sp,
                                fontWeight = FontWeight.Bold,
                                color = if (activeStep.stepNumber == 1) NeonGreenLight else AccentAmber
                            )
                        }

                        // Reset to Step 1 Button
                        Surface(
                            onClick = {
                                currentStepIndex = 0
                                statusFeedback = "ধাপ ১ এ রিসেট করা হয়েছে ($১ ট্রেড)"
                            },
                            shape = RoundedCornerShape(6.dp),
                            color = DarkSurfaceVariant,
                            border = BorderStroke(0.5.dp, BorderStrokeLight)
                        ) {
                            Row(
                                modifier = Modifier.padding(horizontal = 6.dp, vertical = 3.dp),
                                verticalAlignment = Alignment.CenterVertically,
                                horizontalArrangement = Arrangement.spacedBy(3.dp)
                            ) {
                                Icon(
                                    imageVector = Icons.Default.Refresh,
                                    contentDescription = "Reset Step",
                                    tint = TextSecondary,
                                    modifier = Modifier.size(11.dp)
                                )
                                Text(
                                    text = "রিসেট $১",
                                    fontSize = 9.sp,
                                    fontWeight = FontWeight.Bold,
                                    color = TextSecondary
                                )
                            }
                        }
                    }

                    // 7-Step Visual Progress Track
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        STANDARD_7_STEPS.forEachIndexed { idx, step ->
                            val isCurrent = idx == currentStepIndex
                            val isPassed = idx < currentStepIndex
                            Surface(
                                onClick = {
                                    currentStepIndex = idx
                                    statusFeedback = "ম্যানুয়ালি ধাপ ${step.stepNumber} ($${step.tradeAmount}) নির্বাচন করা হয়েছে।"
                                },
                                shape = CircleShape,
                                color = when {
                                    isCurrent -> if (step.stepNumber == 1) NeonGreen else AccentAmber
                                    isPassed -> NeonRed.copy(alpha = 0.35f)
                                    else -> DarkSurfaceVariant
                                },
                                border = BorderStroke(
                                    1.dp,
                                    when {
                                        isCurrent -> Color.White
                                        isPassed -> NeonRedLight.copy(alpha = 0.6f)
                                        else -> BorderStrokeLight
                                    }
                                ),
                                modifier = Modifier.size(if (isCurrent) 28.dp else 24.dp)
                            ) {
                                Box(contentAlignment = Alignment.Center) {
                                    Text(
                                        text = "${step.stepNumber}",
                                        fontSize = if (isCurrent) 11.sp else 9.5.sp,
                                        fontWeight = FontWeight.ExtraBold,
                                        color = when {
                                            isCurrent -> DarkBackground
                                            isPassed -> NeonRedLight
                                            else -> TextSecondary
                                        }
                                    )
                                }
                            }
                        }
                    }

                    // Hero Next Trade Amount Display
                    Surface(
                        shape = RoundedCornerShape(10.dp),
                        color = Color(0xFF0F172A),
                        border = BorderStroke(1.dp, if (activeStep.stepNumber == 1) NeonGreen.copy(alpha = 0.5f) else AccentAmber.copy(alpha = 0.6f)),
                        modifier = Modifier.fillMaxWidth()
                    ) {
                        Column(
                            modifier = Modifier.padding(vertical = 12.dp, horizontal = 12.dp),
                            horizontalAlignment = Alignment.CenterHorizontally,
                            verticalArrangement = Arrangement.spacedBy(4.dp)
                        ) {
                            Row(
                                verticalAlignment = Alignment.CenterVertically,
                                horizontalArrangement = Arrangement.spacedBy(6.dp)
                            ) {
                                Surface(
                                    shape = RoundedCornerShape(4.dp),
                                    color = if (activeStep.stepNumber == 1) NeonGreenDim else AccentAmber.copy(alpha = 0.15f)
                                ) {
                                    Text(
                                        text = if (activeStep.stepNumber == 1) "বেস ট্রেড (ধাপ ১/৭)" else "রিকভারি মোড (ধাপ ${activeStep.stepNumber}/৭)",
                                        fontSize = 9.sp,
                                        fontWeight = FontWeight.Bold,
                                        color = if (activeStep.stepNumber == 1) NeonGreenLight else AccentAmber,
                                        modifier = Modifier.padding(horizontal = 6.dp, vertical = 2.dp)
                                    )
                                }
                                Text(
                                    text = "সেশন উইন: $sessionWinCount টি",
                                    fontSize = 9.sp,
                                    color = TextMuted
                                )
                            }

                            // Big Trade Amount
                            Text(
                                text = "$${activeStep.tradeAmount}.00",
                                fontSize = 34.sp,
                                fontWeight = FontWeight.Black,
                                fontFamily = FontFamily.Monospace,
                                color = if (activeStep.stepNumber == 1) NeonGreen else AccentAmber
                            )

                            // Payout & Net Profit Breakdown
                            Row(
                                horizontalArrangement = Arrangement.spacedBy(10.dp),
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                Text(
                                    text = "৯৫% উইন রিটার্ন: $${String.format(Locale.US, "%.2f", activeStep.payoutReturn)}",
                                    fontSize = 10.sp,
                                    fontWeight = FontWeight.Medium,
                                    color = TextSecondary
                                )
                                Text(
                                    text = "নিট লাভ: +$${String.format(Locale.US, "%.2f", activeStep.netProfit)}",
                                    fontSize = 10.sp,
                                    fontWeight = FontWeight.Bold,
                                    fontFamily = FontFamily.Monospace,
                                    color = NeonGreen
                                )
                            }
                        }
                    }

                    // Interactive Action Buttons Row
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.spacedBy(8.dp)
                    ) {
                        // Loss Button -> Advances to next step
                        Button(
                            onClick = {
                                if (currentStepIndex < STANDARD_7_STEPS.size - 1) {
                                    currentStepIndex++
                                    val nextStep = STANDARD_7_STEPS[currentStepIndex]
                                    statusFeedback = "❌ লস হয়েছে! পরবর্তী রিকভারি ধাপ: $${nextStep.tradeAmount} (ধাপ ${nextStep.stepNumber}/৭)"
                                } else {
                                    statusFeedback = "⚠️ ৭ম ধাপ সম্পন্ন! মূলধন সুরক্ষার জন্য ট্রেডিং সাময়িক বিরতি দিন।"
                                }
                            },
                            colors = ButtonDefaults.buttonColors(
                                containerColor = NeonRed.copy(alpha = 0.2f),
                                contentColor = NeonRedLight
                            ),
                            border = BorderStroke(1.dp, NeonRed),
                            shape = RoundedCornerShape(8.dp),
                            modifier = Modifier
                                .weight(1f)
                                .height(42.dp)
                        ) {
                            Row(
                                verticalAlignment = Alignment.CenterVertically,
                                horizontalArrangement = Arrangement.spacedBy(5.dp)
                            ) {
                                Icon(
                                    imageVector = Icons.Default.Close,
                                    contentDescription = "Loss",
                                    tint = NeonRedLight,
                                    modifier = Modifier.size(15.dp)
                                )
                                Text(
                                    text = "লস (পরের ধাপ)",
                                    fontSize = 11.sp,
                                    fontWeight = FontWeight.Bold,
                                    color = NeonRedLight
                                )
                            }
                        }

                        // Win Button -> Resets to Step 1
                        Button(
                            onClick = {
                                val profitWon = activeStep.netProfit
                                sessionWinCount++
                                currentStepIndex = 0
                                statusFeedback = "✅ ট্রেড উইন! লস কভার হয়ে +$${String.format(Locale.US, "%.2f", profitWon)} নিট লাভ অর্জিত। ধাপ $১ এ রিসেট।"
                            },
                            colors = ButtonDefaults.buttonColors(
                                containerColor = NeonGreen.copy(alpha = 0.25f),
                                contentColor = NeonGreenLight
                            ),
                            border = BorderStroke(1.dp, NeonGreen),
                            shape = RoundedCornerShape(8.dp),
                            modifier = Modifier
                                .weight(1f)
                                .height(42.dp)
                        ) {
                            Row(
                                verticalAlignment = Alignment.CenterVertically,
                                horizontalArrangement = Arrangement.spacedBy(5.dp)
                            ) {
                                Icon(
                                    imageVector = Icons.Default.Check,
                                    contentDescription = "Win",
                                    tint = NeonGreenLight,
                                    modifier = Modifier.size(15.dp)
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

                    // Live Status Feedback Alert
                    if (statusFeedback != null) {
                        Surface(
                            shape = RoundedCornerShape(6.dp),
                            color = DarkBackground,
                            border = BorderStroke(
                                0.5.dp,
                                if (statusFeedback?.contains("✅") == true) NeonGreen else AccentAmber
                            ),
                            modifier = Modifier.fillMaxWidth()
                        ) {
                            Text(
                                text = statusFeedback ?: "",
                                fontSize = 10.sp,
                                fontWeight = FontWeight.Medium,
                                color = if (statusFeedback?.contains("✅") == true) NeonGreenLight else AccentAmber,
                                textAlign = TextAlign.Center,
                                modifier = Modifier.padding(horizontal = 8.dp, vertical = 6.dp)
                            )
                        }
                    }
                }
            }
        }

        // ==========================================
        // 3. 30-Day Growth & Earning Projection Chart
        // ==========================================
        item {
            Surface(
                shape = RoundedCornerShape(12.dp),
                color = DarkCard,
                border = BorderStroke(1.dp, BorderStrokeLight),
                modifier = Modifier.fillMaxWidth()
            ) {
                Column(
                    modifier = Modifier.padding(12.dp),
                    verticalArrangement = Arrangement.spacedBy(10.dp)
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
                                text = "৩. ৩০ দিনের আর্নিং প্রজেকশন প্ল্যান",
                                fontSize = 12.sp,
                                fontWeight = FontWeight.Bold,
                                color = Color.White
                            )
                        }

                        // Toggle Milestone vs Full View
                        Surface(
                            onClick = { showAll30Days = !showAll30Days },
                            shape = RoundedCornerShape(6.dp),
                            color = DarkSurfaceVariant,
                            border = BorderStroke(0.5.dp, BorderStrokeLight)
                        ) {
                            Text(
                                text = if (showAll30Days) "মাইলস্টোন ভিউ" else "সব ৩০ দিন দেখুন",
                                fontSize = 8.5.sp,
                                fontWeight = FontWeight.Bold,
                                color = AccentCyan,
                                modifier = Modifier.padding(horizontal = 6.dp, vertical = 3.dp)
                            )
                        }
                    }

                    // 30 Days Summary Cards
                    val finalProjectedBalance = userCapital + (30 * dailyTarget)
                    val totalProfit = 30 * dailyTarget
                    val roiPercent = if (userCapital > 0) (totalProfit / userCapital) * 100.0 else 0.0

                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.spacedBy(6.dp)
                    ) {
                        // Starting Capital
                        Surface(
                            shape = RoundedCornerShape(8.dp),
                            color = DarkSurfaceVariant,
                            border = BorderStroke(0.5.dp, BorderStrokeLight),
                            modifier = Modifier.weight(1f)
                        ) {
                            Column(
                                modifier = Modifier.padding(8.dp),
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
                            shape = RoundedCornerShape(8.dp),
                            color = DarkSurfaceVariant,
                            border = BorderStroke(0.8.dp, NeonGreen.copy(alpha = 0.6f)),
                            modifier = Modifier.weight(1.3f)
                        ) {
                            Column(
                                modifier = Modifier.padding(8.dp),
                                horizontalAlignment = Alignment.CenterHorizontally
                            ) {
                                Text("৩০ দিন পর ব্যালেন্স", fontSize = 8.5.sp, color = TextMuted)
                                Text(
                                    text = "$${String.format(Locale.US, "%.2f", finalProjectedBalance)}",
                                    fontSize = 13.5.sp,
                                    fontWeight = FontWeight.Black,
                                    fontFamily = FontFamily.Monospace,
                                    color = NeonGreen
                                )
                            }
                        }

                        // Net Profit
                        Surface(
                            shape = RoundedCornerShape(8.dp),
                            color = DarkSurfaceVariant,
                            border = BorderStroke(0.5.dp, BorderStrokeLight),
                            modifier = Modifier.weight(1f)
                        ) {
                            Column(
                                modifier = Modifier.padding(8.dp),
                                horizontalAlignment = Alignment.CenterHorizontally
                            ) {
                                Text("মোট লাভ (+${String.format(Locale.US, "%.0f", roiPercent)}%)", fontSize = 8.5.sp, color = TextMuted)
                                Text(
                                    text = "+$${String.format(Locale.US, "%.0f", totalProfit)}",
                                    fontSize = 12.sp,
                                    fontWeight = FontWeight.Bold,
                                    color = AccentCyan
                                )
                            }
                        }
                    }

                    // Days List (Filtered based on showAll30Days toggle)
                    val displayedDays = if (showAll30Days) {
                        (1..30).toList()
                    } else {
                        listOf(1, 5, 10, 15, 20, 25, 30)
                    }

                    Column(
                        modifier = Modifier.fillMaxWidth(),
                        verticalArrangement = Arrangement.spacedBy(3.dp)
                    ) {
                        displayedDays.forEach { day ->
                            val startBal = userCapital + ((day - 1) * dailyTarget)
                            val endBal = userCapital + (day * dailyTarget)
                            val isMilestone = day % 5 == 0 || day == 30

                            Surface(
                                shape = RoundedCornerShape(5.dp),
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
                                        .padding(horizontal = 8.dp, vertical = 5.dp),
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
                                        text = "লাভ: +$${String.format(Locale.US, "%.1f", dailyTarget)}",
                                        fontSize = 9.sp,
                                        fontWeight = FontWeight.SemiBold,
                                        fontFamily = FontFamily.Monospace,
                                        color = NeonGreenLight
                                    )
                                    Text(
                                        text = "শেষ: $${String.format(Locale.US, "%.1f", endBal)}",
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
