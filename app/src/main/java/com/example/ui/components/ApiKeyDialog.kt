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
import androidx.compose.material.icons.filled.Refresh
import androidx.compose.material.icons.filled.Settings
import androidx.compose.material.icons.filled.Share
import androidx.compose.material.icons.filled.Speed
import androidx.compose.material.icons.filled.Tune
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.OutlinedTextFieldDefaults
import androidx.compose.material3.Surface
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
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.data.models.DecisionMode
import com.example.data.models.EngineMode
import com.example.network.HttpTradeRelay
import com.example.network.WebSocketTradeRelay

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
    val selectedDecisionMode by remember { mutableStateOf(currentDecisionMode) }
    val selectedAntiGlitch by remember { mutableStateOf(currentAntiGlitch) }
    val selectedAdaptiveCpu by remember { mutableStateOf(currentAdaptiveCpu) }

    var webhookUrlText by remember {
        mutableStateOf(
            if (HttpTradeRelay.webhookUrl.isNotBlank()) HttpTradeRelay.webhookUrl
            else HttpTradeRelay.DEFAULT_HTTP_URL
        )
    }
    var wsUrlText by remember {
        mutableStateOf(
            if (WebSocketTradeRelay.serverUrl.isNotBlank()) WebSocketTradeRelay.serverUrl
            else WebSocketTradeRelay.DEFAULT_SERVER_URL
        )
    }

    val darkBackground = Color(0xFF161A1D)
    val inputCardBackground = Color(0xFF101418)
    val activeBorderColor = Color(0xFF22C55E)
    val activeBackgroundColor = Color(0xFF0E2316)
    val activeTextColor = Color(0xFF4ADE80)
    val inactiveBorderColor = Color(0xFF1E252B)
    val inactiveTextColor = Color(0xFF94A3B8)
    val cyanAccent = Color(0xFF00E5FF)
    val greenAccent = Color(0xFF00E676)

    AlertDialog(
        onDismissRequest = onDismiss,
        containerColor = darkBackground,
        shape = RoundedCornerShape(20.dp),
        title = {
            Row(
                modifier = Modifier.fillMaxWidth(),
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(10.dp)
            ) {
                Icon(
                    imageVector = Icons.Default.Settings,
                    contentDescription = "Settings",
                    tint = cyanAccent,
                    modifier = Modifier.size(22.dp)
                )
                Text(
                    text = "Q Scan Settings",
                    fontSize = 17.5.sp,
                    fontWeight = FontWeight.Bold,
                    color = Color.White
                )
            }
        },
        text = {
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .heightIn(max = 520.dp)
                    .verticalScroll(rememberScrollState()),
                verticalArrangement = Arrangement.spacedBy(16.dp)
            ) {
                // 1. SCAN SPEED SECTION
                Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                    Row(
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.spacedBy(6.dp)
                    ) {
                        Icon(
                            imageVector = Icons.Default.Speed,
                            contentDescription = "Scan Speed",
                            tint = greenAccent,
                            modifier = Modifier.size(15.dp)
                        )
                        Text(
                            text = "SCAN SPEED",
                            fontSize = 10.5.sp,
                            fontWeight = FontWeight.Bold,
                            color = inactiveTextColor,
                            letterSpacing = 0.5.sp
                        )
                    }

                    // Row 1: 20ms and 50ms
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.spacedBy(8.dp)
                    ) {
                        val is20Selected = selectedInterval == 20L || selectedInterval < 20L
                        Surface(
                            onClick = { selectedInterval = 20L },
                            modifier = Modifier
                                .weight(1f)
                                .height(46.dp)
                                .testTag("scan_interval_20ms"),
                            shape = RoundedCornerShape(10.dp),
                            color = if (is20Selected) activeBackgroundColor else inputCardBackground,
                            border = BorderStroke(
                                if (is20Selected) 1.2.dp else 0.5.dp,
                                if (is20Selected) activeBorderColor else inactiveBorderColor
                            )
                        ) {
                            Box(
                                modifier = Modifier.padding(horizontal = 6.dp),
                                contentAlignment = Alignment.Center
                            ) {
                                Text(
                                    text = "20ms • 50 FPS (Ultra)",
                                    fontSize = 10.sp,
                                    fontWeight = if (is20Selected) FontWeight.Bold else FontWeight.Medium,
                                    color = if (is20Selected) activeTextColor else inactiveTextColor,
                                    textAlign = TextAlign.Center
                                )
                            }
                        }

                        val is50Selected = selectedInterval == 50L
                        Surface(
                            onClick = { selectedInterval = 50L },
                            modifier = Modifier
                                .weight(1f)
                                .height(46.dp)
                                .testTag("scan_interval_50ms"),
                            shape = RoundedCornerShape(10.dp),
                            color = if (is50Selected) activeBackgroundColor else inputCardBackground,
                            border = BorderStroke(
                                if (is50Selected) 1.2.dp else 0.5.dp,
                                if (is50Selected) activeBorderColor else inactiveBorderColor
                            )
                        ) {
                            Box(
                                modifier = Modifier.padding(horizontal = 6.dp),
                                contentAlignment = Alignment.Center
                            ) {
                                Text(
                                    text = "50ms • 20 FPS (Fast)",
                                    fontSize = 10.sp,
                                    fontWeight = if (is50Selected) FontWeight.Bold else FontWeight.Medium,
                                    color = if (is50Selected) activeTextColor else inactiveTextColor,
                                    textAlign = TextAlign.Center
                                )
                            }
                        }
                    }

                    // Row 2: 100ms and 250ms
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.spacedBy(8.dp)
                    ) {
                        val is100Selected = selectedInterval == 100L
                        Surface(
                            onClick = { selectedInterval = 100L },
                            modifier = Modifier
                                .weight(1f)
                                .height(50.dp)
                                .testTag("scan_interval_100ms"),
                            shape = RoundedCornerShape(10.dp),
                            color = if (is100Selected) activeBackgroundColor else inputCardBackground,
                            border = BorderStroke(
                                if (is100Selected) 1.2.dp else 0.5.dp,
                                if (is100Selected) activeBorderColor else inactiveBorderColor
                            )
                        ) {
                            Column(
                                modifier = Modifier.padding(horizontal = 4.dp),
                                horizontalAlignment = Alignment.CenterHorizontally,
                                verticalArrangement = Arrangement.Center
                            ) {
                                Text(
                                    text = "100ms • 10 FPS",
                                    fontSize = 10.sp,
                                    fontWeight = if (is100Selected) FontWeight.Bold else FontWeight.Medium,
                                    color = if (is100Selected) activeTextColor else inactiveTextColor
                                )
                                Text(
                                    text = "(Balanced)",
                                    fontSize = 9.sp,
                                    color = if (is100Selected) activeTextColor.copy(alpha = 0.85f) else inactiveTextColor.copy(alpha = 0.7f)
                                )
                            }
                        }

                        val is250Selected = selectedInterval >= 250L
                        Surface(
                            onClick = { selectedInterval = 250L },
                            modifier = Modifier
                                .weight(1f)
                                .height(50.dp)
                                .testTag("scan_interval_250ms"),
                            shape = RoundedCornerShape(10.dp),
                            color = if (is250Selected) activeBackgroundColor else inputCardBackground,
                            border = BorderStroke(
                                if (is250Selected) 1.2.dp else 0.5.dp,
                                if (is250Selected) activeBorderColor else inactiveBorderColor
                            )
                        ) {
                            Column(
                                modifier = Modifier.padding(horizontal = 4.dp),
                                horizontalAlignment = Alignment.CenterHorizontally,
                                verticalArrangement = Arrangement.Center
                            ) {
                                Text(
                                    text = "250ms • 4 FPS",
                                    fontSize = 10.sp,
                                    fontWeight = if (is250Selected) FontWeight.Bold else FontWeight.Medium,
                                    color = if (is250Selected) activeTextColor else inactiveTextColor
                                )
                                Text(
                                    text = "(Economy)",
                                    fontSize = 9.sp,
                                    color = if (is250Selected) activeTextColor.copy(alpha = 0.85f) else inactiveTextColor.copy(alpha = 0.7f)
                                )
                            }
                        }
                    }
                }

                // 2. ANALYSIS ENGINE SECTION
                Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                    Row(
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.spacedBy(6.dp)
                    ) {
                        Icon(
                            imageVector = Icons.Default.Tune,
                            contentDescription = "Analysis Engine",
                            tint = cyanAccent,
                            modifier = Modifier.size(15.dp)
                        )
                        Text(
                            text = "ANALYSIS ENGINE",
                            fontSize = 10.5.sp,
                            fontWeight = FontWeight.Bold,
                            color = inactiveTextColor,
                            letterSpacing = 0.5.sp
                        )
                    }

                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.spacedBy(8.dp)
                    ) {
                        // Local (Offline)
                        val isLocal = selectedEngineMode == EngineMode.LOCAL
                        Surface(
                            onClick = { selectedEngineMode = EngineMode.LOCAL },
                            modifier = Modifier
                                .weight(1f)
                                .height(50.dp)
                                .testTag("engine_mode_local"),
                            shape = RoundedCornerShape(10.dp),
                            color = if (isLocal) activeBackgroundColor else inputCardBackground,
                            border = BorderStroke(
                                if (isLocal) 1.2.dp else 0.5.dp,
                                if (isLocal) activeBorderColor else inactiveBorderColor
                            )
                        ) {
                            Column(
                                modifier = Modifier.padding(horizontal = 2.dp),
                                horizontalAlignment = Alignment.CenterHorizontally,
                                verticalArrangement = Arrangement.Center
                            ) {
                                Text(
                                    text = "Local",
                                    fontSize = 10.5.sp,
                                    fontWeight = if (isLocal) FontWeight.Bold else FontWeight.Medium,
                                    color = if (isLocal) activeTextColor else inactiveTextColor
                                )
                                Text(
                                    text = "(Offline)",
                                    fontSize = 9.sp,
                                    color = if (isLocal) activeTextColor.copy(alpha = 0.85f) else inactiveTextColor.copy(alpha = 0.7f)
                                )
                            }
                        }

                        // Auto Fallback
                        val isAuto = selectedEngineMode == EngineMode.AUTO
                        Surface(
                            onClick = { selectedEngineMode = EngineMode.AUTO },
                            modifier = Modifier
                                .weight(1f)
                                .height(50.dp)
                                .testTag("engine_mode_auto"),
                            shape = RoundedCornerShape(10.dp),
                            color = if (isAuto) activeBackgroundColor else inputCardBackground,
                            border = BorderStroke(
                                if (isAuto) 1.2.dp else 0.5.dp,
                                if (isAuto) activeBorderColor else inactiveBorderColor
                            )
                        ) {
                            Column(
                                modifier = Modifier.padding(horizontal = 2.dp),
                                horizontalAlignment = Alignment.CenterHorizontally,
                                verticalArrangement = Arrangement.Center
                            ) {
                                Text(
                                    text = "Auto",
                                    fontSize = 10.5.sp,
                                    fontWeight = if (isAuto) FontWeight.Bold else FontWeight.Medium,
                                    color = if (isAuto) activeTextColor else inactiveTextColor
                                )
                                Text(
                                    text = "Fallback",
                                    fontSize = 9.sp,
                                    color = if (isAuto) activeTextColor.copy(alpha = 0.85f) else inactiveTextColor.copy(alpha = 0.7f)
                                )
                            }
                        }

                        // Cloud AI
                        val isCloud = selectedEngineMode == EngineMode.CLOUD
                        Surface(
                            onClick = { selectedEngineMode = EngineMode.CLOUD },
                            modifier = Modifier
                                .weight(1f)
                                .height(50.dp)
                                .testTag("engine_mode_cloud"),
                            shape = RoundedCornerShape(10.dp),
                            color = if (isCloud) activeBackgroundColor else inputCardBackground,
                            border = BorderStroke(
                                if (isCloud) 1.2.dp else 0.5.dp,
                                if (isCloud) activeBorderColor else inactiveBorderColor
                            )
                        ) {
                            Box(
                                modifier = Modifier.padding(horizontal = 4.dp),
                                contentAlignment = Alignment.Center
                            ) {
                                Text(
                                    text = "Cloud AI",
                                    fontSize = 10.5.sp,
                                    fontWeight = if (isCloud) FontWeight.Bold else FontWeight.Medium,
                                    color = if (isCloud) activeTextColor else inactiveTextColor,
                                    textAlign = TextAlign.Center
                                )
                            }
                        }
                    }
                }

                // 3. AUTO-TRADE WEBHOOK URL SECTION
                Column(verticalArrangement = Arrangement.spacedBy(6.dp)) {
                    Row(
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.spacedBy(6.dp)
                    ) {
                        Icon(
                            imageVector = Icons.Default.Share,
                            contentDescription = "Webhook URL",
                            tint = cyanAccent,
                            modifier = Modifier.size(15.dp)
                        )
                        Text(
                            text = "AUTO-TRADE WEBHOOK URL",
                            fontSize = 10.5.sp,
                            fontWeight = FontWeight.Bold,
                            color = inactiveTextColor,
                            letterSpacing = 0.5.sp
                        )
                    }

                    OutlinedTextField(
                        value = webhookUrlText,
                        onValueChange = {
                            webhookUrlText = it
                            HttpTradeRelay.webhookUrl = it
                        },
                        placeholder = {
                            Text(
                                text = "http://192.168.0.117:5000/trade",
                                fontSize = 12.sp,
                                color = inactiveTextColor.copy(alpha = 0.5f)
                            )
                        },
                        singleLine = true,
                        modifier = Modifier
                            .fillMaxWidth()
                            .testTag("auto_trade_webhook_input"),
                        shape = RoundedCornerShape(10.dp),
                        colors = OutlinedTextFieldDefaults.colors(
                            focusedContainerColor = inputCardBackground,
                            unfocusedContainerColor = inputCardBackground,
                            focusedBorderColor = activeBorderColor,
                            unfocusedBorderColor = inactiveBorderColor,
                            focusedTextColor = Color.White,
                            unfocusedTextColor = Color.White
                        )
                    )
                }

                // 4. WEBSOCKET SERVER URL SECTION
                Column(verticalArrangement = Arrangement.spacedBy(6.dp)) {
                    Row(
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.spacedBy(6.dp)
                    ) {
                        Icon(
                            imageVector = Icons.Default.Refresh,
                            contentDescription = "WebSocket URL",
                            tint = greenAccent,
                            modifier = Modifier.size(15.dp)
                        )
                        Text(
                            text = "WEBSOCKET SERVER URL",
                            fontSize = 10.5.sp,
                            fontWeight = FontWeight.Bold,
                            color = inactiveTextColor,
                            letterSpacing = 0.5.sp
                        )
                    }

                    OutlinedTextField(
                        value = wsUrlText,
                        onValueChange = {
                            wsUrlText = it
                            WebSocketTradeRelay.serverUrl = it
                        },
                        placeholder = {
                            Text(
                                text = "ws://192.168.0.117:8765",
                                fontSize = 12.sp,
                                color = inactiveTextColor.copy(alpha = 0.5f)
                            )
                        },
                        singleLine = true,
                        modifier = Modifier
                            .fillMaxWidth()
                            .testTag("websocket_server_url_input"),
                        shape = RoundedCornerShape(10.dp),
                        colors = OutlinedTextFieldDefaults.colors(
                            focusedContainerColor = inputCardBackground,
                            unfocusedContainerColor = inputCardBackground,
                            focusedBorderColor = activeBorderColor,
                            unfocusedBorderColor = inactiveBorderColor,
                            focusedTextColor = Color.White,
                            unfocusedTextColor = Color.White
                        )
                    )
                }
            }
        },
        confirmButton = {
            Button(
                onClick = {
                    WebSocketTradeRelay.updateEndpointsAndReconnect(wsUrlText, webhookUrlText)
                    onSave(
                        apiKeyText,
                        selectedInterval,
                        selectedEngineMode,
                        selectedDecisionMode,
                        selectedAntiGlitch,
                        selectedAdaptiveCpu
                    )
                    onDismiss()
                },
                modifier = Modifier
                    .height(42.dp)
                    .testTag("settings_save_button"),
                shape = RoundedCornerShape(12.dp),
                colors = ButtonDefaults.buttonColors(
                    containerColor = Color(0xFF22C55E),
                    contentColor = Color.Black
                )
            ) {
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(6.dp)
                ) {
                    Icon(
                        imageVector = Icons.Default.Check,
                        contentDescription = "Save",
                        tint = Color.Black,
                        modifier = Modifier.size(17.dp)
                    )
                    Text(
                        text = "Save",
                        fontWeight = FontWeight.Bold,
                        fontSize = 13.5.sp,
                        color = Color.Black
                    )
                }
            }
        },
        dismissButton = {
            TextButton(
                onClick = onDismiss,
                modifier = Modifier.testTag("settings_cancel_button")
            ) {
                Text(
                    text = "Cancel",
                    color = inactiveTextColor,
                    fontSize = 13.5.sp,
                    fontWeight = FontWeight.Medium
                )
            }
        }
    )
}
