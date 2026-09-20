package com.example.ui.components

import androidx.compose.foundation.BorderStroke
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
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Check
import androidx.compose.material.icons.filled.Close
import androidx.compose.material.icons.filled.Info
import androidx.compose.material.icons.filled.Save
import androidx.compose.material.icons.filled.Settings
import androidx.compose.material.icons.filled.Timer
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.OutlinedTextFieldDefaults
import androidx.compose.material3.Surface
import androidx.compose.material3.Switch
import androidx.compose.material3.SwitchDefaults
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableLongStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.data.models.DecisionMode
import com.example.data.models.EngineMode
import com.example.ui.theme.AccentCyan
import com.example.ui.theme.DarkBackground
import com.example.ui.theme.DarkCard
import com.example.ui.theme.DarkSurface
import com.example.ui.theme.NeonGreen
import com.example.ui.theme.NeonGreenDim
import com.example.ui.theme.NeonGreenLight
import com.example.ui.theme.TextMuted
import com.example.ui.theme.TextPrimary
import com.example.ui.theme.TextSecondary

@Composable
fun ApiKeyDialog(
    currentApiKey: String,
    currentIntervalMs: Long,
    currentEngineMode: EngineMode = EngineMode.AUTO,
    currentDecisionMode: DecisionMode = DecisionMode.LEGACY_MULTILAYER,
    currentAntiGlitch: Boolean = true,
    currentAdaptiveCpu: Boolean = true,
    onSave: (apiKey: String, intervalMs: Long, engineMode: EngineMode, decisionMode: DecisionMode, antiGlitch: Boolean, adaptiveCpu: Boolean) -> Unit,
    onDismiss: () -> Unit
) {
    var apiKeyText by remember { mutableStateOf(currentApiKey) }
    var selectedInterval by remember { mutableLongStateOf(currentIntervalMs) }
    var selectedEngineMode by remember { mutableStateOf(currentEngineMode) }
    var selectedDecisionMode by remember { mutableStateOf(currentDecisionMode) }
    var selectedAntiGlitch by remember { mutableStateOf(currentAntiGlitch) }
    var selectedAdaptiveCpu by remember { mutableStateOf(currentAdaptiveCpu) }

    // Streamlined interval options as per guidelines: 20ms (50 FPS), 50ms (20 FPS), 100ms (10 FPS), 250ms (4 FPS)
    val streamlinedIntervals = listOf(
        20L to "20ms (50 FPS • Ultra)",
        50L to "50ms (20 FPS • Fast)",
        100L to "100ms (10 FPS • Balanced)",
        250L to "250ms (4 FPS • Economy)"
    )

    AlertDialog(
        onDismissRequest = onDismiss,
        containerColor = DarkCard,
        titleContentColor = TextPrimary,
        textContentColor = TextSecondary,
        shape = RoundedCornerShape(16.dp),
        title = {
            Row(
                modifier = Modifier.fillMaxWidth(),
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.SpaceBetween
            ) {
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    Icon(
                        imageVector = Icons.Default.Settings,
                        contentDescription = "Settings",
                        tint = AccentCyan,
                        modifier = Modifier.size(19.dp)
                    )
                    Text(
                        text = "Settings & Engine Config",
                        fontSize = 14.5.sp,
                        fontWeight = FontWeight.Bold,
                        color = Color.White
                    )
                }

                // 100% Offline Badge
                Surface(
                    shape = RoundedCornerShape(5.dp),
                    color = Color(0xFF102618),
                    border = BorderStroke(0.5.dp, Color.Black)
                ) {
                    Text(
                        text = "100% OFFLINE",
                        fontSize = 9.sp,
                        fontWeight = FontWeight.Bold,
                        fontFamily = FontFamily.Monospace,
                        color = NeonGreenLight,
                        modifier = Modifier.padding(horizontal = 6.dp, vertical = 2.dp)
                    )
                }
            }
        },
        text = {
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .heightIn(max = 480.dp)
                    .verticalScroll(rememberScrollState()),
                verticalArrangement = Arrangement.spacedBy(14.dp)
            ) {
                // SECTION 1: Frame Interval / Scan Speed
                Column(
                    modifier = Modifier
                        .fillMaxWidth()
                        .clip(RoundedCornerShape(10.dp))
                        .background(DarkBackground)
                        .border(0.5.dp, Color.Black, RoundedCornerShape(10.dp))
                        .padding(10.dp)
                ) {
                    Row(
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.spacedBy(5.dp)
                    ) {
                        Icon(
                            imageVector = Icons.Default.Timer,
                            contentDescription = "Interval",
                            tint = NeonGreenLight,
                            modifier = Modifier.size(14.dp)
                        )
                        Text(
                            text = "SCAN SPEED & FRAME RATE",
                            fontSize = 10.sp,
                            fontWeight = FontWeight.Bold,
                            fontFamily = FontFamily.Monospace,
                            color = TextMuted,
                            letterSpacing = 0.5.sp
                        )
                    }

                    Spacer(modifier = Modifier.height(8.dp))

                    Column(verticalArrangement = Arrangement.spacedBy(6.dp)) {
                        streamlinedIntervals.chunked(2).forEach { rowIntervals ->
                            Row(
                                modifier = Modifier.fillMaxWidth(),
                                horizontalArrangement = Arrangement.spacedBy(6.dp)
                            ) {
                                rowIntervals.forEach { (ms, label) ->
                                    val isSelected = selectedInterval == ms || (ms == 20L && selectedInterval < 20L)
                                    Surface(
                                        onClick = { selectedInterval = ms },
                                        shape = RoundedCornerShape(8.dp),
                                        color = if (isSelected) Color(0xFF102618) else Color(0xFF1A1F29),
                                        border = BorderStroke(
                                            if (isSelected) 0.8.dp else 0.5.dp,
                                            if (isSelected) NeonGreen else Color.Black
                                        ),
                                        modifier = Modifier.weight(1f)
                                    ) {
                                        Text(
                                            text = label,
                                            fontSize = 10.5.sp,
                                            fontWeight = if (isSelected) FontWeight.Bold else FontWeight.Medium,
                                            color = if (isSelected) NeonGreenLight else TextSecondary,
                                            modifier = Modifier.padding(horizontal = 8.dp, vertical = 7.dp)
                                        )
                                    }
                                }
                            }
                        }
                    }
                }

                // SECTION 2: Engine Mode
                Column(
                    modifier = Modifier
                        .fillMaxWidth()
                        .clip(RoundedCornerShape(10.dp))
                        .background(DarkBackground)
                        .border(0.5.dp, Color.Black, RoundedCornerShape(10.dp))
                        .padding(10.dp)
                ) {
                    Text(
                        text = "ANALYSIS ENGINE MODE",
                        fontSize = 10.sp,
                        fontWeight = FontWeight.Bold,
                        fontFamily = FontFamily.Monospace,
                        color = TextMuted,
                        letterSpacing = 0.5.sp
                    )

                    Spacer(modifier = Modifier.height(8.dp))

                    Column(verticalArrangement = Arrangement.spacedBy(6.dp)) {
                        EngineMode.entries.forEach { mode ->
                            val isSelected = selectedEngineMode == mode
                            Surface(
                                onClick = { selectedEngineMode = mode },
                                shape = RoundedCornerShape(8.dp),
                                color = if (isSelected) Color(0xFF0C2436) else Color(0xFF1A1F29),
                                border = BorderStroke(
                                    if (isSelected) 0.8.dp else 0.5.dp,
                                    if (isSelected) AccentCyan else Color.Black
                                ),
                                modifier = Modifier.fillMaxWidth()
                            ) {
                                Row(
                                    modifier = Modifier.padding(horizontal = 10.dp, vertical = 7.dp),
                                    verticalAlignment = Alignment.CenterVertically,
                                    horizontalArrangement = Arrangement.SpaceBetween
                                ) {
                                    Column(modifier = Modifier.weight(1f)) {
                                        Text(
                                            text = mode.bengaliTitle,
                                            fontSize = 11.sp,
                                            fontWeight = if (isSelected) FontWeight.Bold else FontWeight.Medium,
                                            color = if (isSelected) Color.White else TextSecondary
                                        )
                                        Text(
                                            text = mode.subtitle,
                                            fontSize = 9.sp,
                                            color = if (isSelected) AccentCyan else TextMuted
                                        )
                                    }
                                    if (isSelected) {
                                        Icon(
                                            imageVector = Icons.Default.Check,
                                            contentDescription = "Selected",
                                            tint = AccentCyan,
                                            modifier = Modifier.size(15.dp)
                                        )
                                    }
                                }
                            }
                        }
                    }
                }

                // SECTION 3: Decision Mode
                Column(
                    modifier = Modifier
                        .fillMaxWidth()
                        .clip(RoundedCornerShape(10.dp))
                        .background(DarkBackground)
                        .border(0.5.dp, Color.Black, RoundedCornerShape(10.dp))
                        .padding(10.dp)
                ) {
                    Text(
                        text = "DECISION MATRIX HIERARCHY",
                        fontSize = 10.sp,
                        fontWeight = FontWeight.Bold,
                        fontFamily = FontFamily.Monospace,
                        color = TextMuted,
                        letterSpacing = 0.5.sp
                    )

                    Spacer(modifier = Modifier.height(8.dp))

                    Column(verticalArrangement = Arrangement.spacedBy(6.dp)) {
                        DecisionMode.values().forEach { mode ->
                            val isSelected = selectedDecisionMode == mode
                            Surface(
                                onClick = { selectedDecisionMode = mode },
                                shape = RoundedCornerShape(8.dp),
                                color = if (isSelected) Color(0xFF102618) else Color(0xFF1A1F29),
                                border = BorderStroke(
                                    if (isSelected) 0.8.dp else 0.5.dp,
                                    if (isSelected) NeonGreen else Color.Black
                                ),
                                modifier = Modifier.fillMaxWidth()
                            ) {
                                Row(
                                    modifier = Modifier.padding(horizontal = 10.dp, vertical = 7.dp),
                                    verticalAlignment = Alignment.CenterVertically,
                                    horizontalArrangement = Arrangement.SpaceBetween
                                ) {
                                    Column(modifier = Modifier.weight(1f)) {
                                        Text(
                                            text = mode.titleBengali,
                                            fontSize = 11.sp,
                                            fontWeight = if (isSelected) FontWeight.Bold else FontWeight.Medium,
                                            color = if (isSelected) Color.White else TextSecondary
                                        )
                                        Text(
                                            text = when (mode) {
                                                DecisionMode.THREE_TIMEFRAME_PRESSURE -> "5m/60m/1D Energy Arithmetic Confluence"
                                                DecisionMode.SHORT_TERM_STRENGTH -> "Kinetic Momentum & Vector Pressure"
                                                else -> "165 Deterministic Matrices (13-Priority Guard)"
                                            },
                                            fontSize = 9.sp,
                                            color = if (isSelected) NeonGreenLight else TextMuted
                                        )
                                    }
                                    if (isSelected) {
                                        Icon(
                                            imageVector = Icons.Default.Check,
                                            contentDescription = "Selected",
                                            tint = NeonGreen,
                                            modifier = Modifier.size(15.dp)
                                        )
                                    }
                                }
                            }
                        }
                    }
                }

                // SECTION 4: Laptop Automation Server (Webhook URL)
                Column(
                    modifier = Modifier
                        .fillMaxWidth()
                        .clip(RoundedCornerShape(10.dp))
                        .background(DarkBackground)
                        .border(0.5.dp, Color.Black, RoundedCornerShape(10.dp))
                        .padding(10.dp)
                ) {
                    Text(
                        text = "DESKTOP BOT RELAY (WEBHOOK)",
                        fontSize = 10.sp,
                        fontWeight = FontWeight.Bold,
                        fontFamily = FontFamily.Monospace,
                        color = TextMuted,
                        letterSpacing = 0.5.sp
                    )

                    Spacer(modifier = Modifier.height(6.dp))

                    var webhookUrlText by remember { mutableStateOf(com.example.network.HttpTradeRelay.webhookUrl) }
                    OutlinedTextField(
                        value = webhookUrlText,
                        onValueChange = {
                            webhookUrlText = it
                            com.example.network.HttpTradeRelay.webhookUrl = it
                        },
                        label = { Text("Webhook URL (e.g. http://192.168.0.102:5000/trade)", fontSize = 10.sp) },
                        singleLine = true,
                        modifier = Modifier
                            .fillMaxWidth()
                            .testTag("webhook_url_input"),
                        colors = OutlinedTextFieldDefaults.colors(
                            focusedBorderColor = AccentCyan,
                            unfocusedBorderColor = Color.Black,
                            focusedTextColor = TextPrimary,
                            unfocusedTextColor = TextSecondary,
                            focusedContainerColor = DarkSurface,
                            unfocusedContainerColor = DarkSurface
                        ),
                        shape = RoundedCornerShape(8.dp)
                    )

                    Spacer(modifier = Modifier.height(4.dp))

                    Text(
                        text = "• Dispatches {\"signal\":\"UP\"|\"DOWN\"} to local Flask/FastAPI/ngrok bot.",
                        fontSize = 9.sp,
                        color = TextMuted
                    )
                }

                // SECTION 5: Stability & Glitch Protection
                Column(
                    modifier = Modifier
                        .fillMaxWidth()
                        .clip(RoundedCornerShape(10.dp))
                        .background(DarkBackground)
                        .border(0.5.dp, Color.Black, RoundedCornerShape(10.dp))
                        .padding(10.dp)
                ) {
                    Text(
                        text = "STABILITY & DEVICE GUARDS",
                        fontSize = 10.sp,
                        fontWeight = FontWeight.Bold,
                        fontFamily = FontFamily.Monospace,
                        color = TextMuted,
                        letterSpacing = 0.5.sp
                    )

                    Spacer(modifier = Modifier.height(8.dp))

                    // 2-Frame Anti-Glitch Confirmation
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .clip(RoundedCornerShape(7.dp))
                            .background(DarkSurface)
                            .border(0.5.dp, Color.Black, RoundedCornerShape(7.dp))
                            .padding(horizontal = 9.dp, vertical = 6.dp),
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.SpaceBetween
                    ) {
                        Column(modifier = Modifier.weight(1f)) {
                            Text(
                                text = "2-Frame Anti-Glitch Guard",
                                fontSize = 11.sp,
                                fontWeight = FontWeight.SemiBold,
                                color = if (selectedAntiGlitch) NeonGreenLight else TextPrimary
                            )
                            Text(
                                text = "Camera flicker & glare suppression",
                                fontSize = 9.sp,
                                color = TextMuted
                            )
                        }
                        Switch(
                            checked = selectedAntiGlitch,
                            onCheckedChange = { selectedAntiGlitch = it },
                            colors = SwitchDefaults.colors(
                                checkedThumbColor = NeonGreen,
                                checkedTrackColor = DarkCard,
                                uncheckedThumbColor = TextMuted,
                                uncheckedTrackColor = DarkBackground
                            ),
                            modifier = Modifier.testTag("toggle_anti_glitch")
                        )
                    }

                    Spacer(modifier = Modifier.height(6.dp))

                    // Adaptive CPU Protection
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .clip(RoundedCornerShape(7.dp))
                            .background(DarkSurface)
                            .border(0.5.dp, Color.Black, RoundedCornerShape(7.dp))
                            .padding(horizontal = 9.dp, vertical = 6.dp),
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.SpaceBetween
                    ) {
                        Column(modifier = Modifier.weight(1f)) {
                            Text(
                                text = "Adaptive CPU & Thermal Protection",
                                fontSize = 11.sp,
                                fontWeight = FontWeight.SemiBold,
                                color = if (selectedAdaptiveCpu) NeonGreenLight else TextPrimary
                            )
                            Text(
                                text = "Static screen rest (25ms) / 0ms active backoff",
                                fontSize = 9.sp,
                                color = TextMuted
                            )
                        }
                        Switch(
                            checked = selectedAdaptiveCpu,
                            onCheckedChange = { selectedAdaptiveCpu = it },
                            colors = SwitchDefaults.colors(
                                checkedThumbColor = NeonGreen,
                                checkedTrackColor = DarkCard,
                                uncheckedThumbColor = TextMuted,
                                uncheckedTrackColor = DarkBackground
                            ),
                            modifier = Modifier.testTag("toggle_adaptive_cpu")
                        )
                    }
                }
            }
        },
        confirmButton = {
            Button(
                onClick = {
                    onSave(apiKeyText, selectedInterval, selectedEngineMode, selectedDecisionMode, selectedAntiGlitch, selectedAdaptiveCpu)
                    onDismiss()
                },
                colors = ButtonDefaults.buttonColors(
                    containerColor = NeonGreen,
                    contentColor = DarkBackground
                ),
                shape = RoundedCornerShape(8.dp),
                modifier = Modifier.testTag("save_settings_button")
            ) {
                Icon(
                    imageVector = Icons.Default.Save,
                    contentDescription = "Save",
                    modifier = Modifier.size(15.dp)
                )
                Spacer(modifier = Modifier.width(5.dp))
                Text("Save Changes", fontWeight = FontWeight.Bold, fontSize = 11.5.sp)
            }
        },
        dismissButton = {
            TextButton(onClick = onDismiss) {
                Text("Cancel", color = TextMuted, fontSize = 11.5.sp)
            }
        }
    )
}
