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
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Pause
import androidx.compose.material.icons.filled.PlayArrow
import androidx.compose.material.icons.filled.Refresh
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.OutlinedTextFieldDefaults
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.hapticfeedback.HapticFeedbackType
import androidx.compose.ui.platform.LocalHapticFeedback
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.network.HttpTradeRelay
import com.example.network.TradeExecutionDispatcher
import com.example.network.WebSocketTradeRelay
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch

@Composable
fun WebSocketAutoTradeTab(
    isAutoTradeEnabled: Boolean = false,
    onToggleAutoTrade: () -> Unit = {},
    onResetTradeLock: () -> Unit = {},
    modifier: Modifier = Modifier
) {
    val isConnected by WebSocketTradeRelay.connectionState.collectAsState()
    val autoConnectState by WebSocketTradeRelay.autoConnectState.collectAsState()
    val logs by WebSocketTradeRelay.logs.collectAsState()
    val haptic = LocalHapticFeedback.current
    val coroutineScope = rememberCoroutineScope()
    var activeClickedButton by remember { mutableStateOf<String?>(null) }

    var inputUrl by remember {
        mutableStateOf(
            if (WebSocketTradeRelay.serverUrl.isNotBlank()) WebSocketTradeRelay.serverUrl
            else WebSocketTradeRelay.DEFAULT_SERVER_URL
        )
    }
    var inputWebhookUrl by remember {
        mutableStateOf(
            if (HttpTradeRelay.webhookUrl.isNotBlank()) HttpTradeRelay.webhookUrl
            else HttpTradeRelay.DEFAULT_HTTP_URL
        )
    }

    val isScanningOrConnected = autoConnectState != WebSocketTradeRelay.AutoConnectState.PAUSED

    val cardBackground = Color(0xFF161A1D)
    val inputBackground = Color(0xFF0D1114)
    val inputBorderColor = Color(0xFF1E252B)
    val labelColor = Color(0xFF94A3B8)
    val skyBlueAccent = Color(0xFF38BDF8)
    val brightGreen = Color(0xFF22C55E)
    val brightRed = Color(0xFFEF4444)

    LazyColumn(
        modifier = modifier
            .fillMaxSize()
            .testTag("websocket_autotrade_tab"),
        verticalArrangement = Arrangement.spacedBy(12.dp)
    ) {
        // CARD 1: DESKTOP WEBSOCKET SERVER
        item {
            Surface(
                modifier = Modifier
                    .fillMaxWidth()
                    .testTag("tab_desktop_server_card"),
                shape = RoundedCornerShape(16.dp),
                color = cardBackground,
                border = BorderStroke(0.8.dp, Color(0xFF222830))
            ) {
                Column(
                    modifier = Modifier.padding(14.dp),
                    verticalArrangement = Arrangement.spacedBy(10.dp)
                ) {
                    // Header Row: Status Dot + Title + DISCONNECTED / CONNECTED Badge
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Row(
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.spacedBy(8.dp)
                        ) {
                            Box(
                                modifier = Modifier
                                    .size(9.dp)
                                    .clip(CircleShape)
                                    .background(if (isConnected) brightGreen else brightRed)
                            )
                            Text(
                                text = "DESKTOP WEBSOCKET SERVER",
                                fontSize = 11.5.sp,
                                fontWeight = FontWeight.Bold,
                                color = labelColor,
                                letterSpacing = 0.5.sp
                            )
                        }

                        // Badge Pill: DISCONNECTED / AUTO-PROBING / CONNECTED
                        Surface(
                            shape = RoundedCornerShape(6.dp),
                            color = if (isConnected) Color(0xFF102618) else if (isScanningOrConnected) Color(0xFF132338) else Color(0xFF2A1518),
                            border = BorderStroke(
                                1.dp,
                                if (isConnected) brightGreen.copy(alpha = 0.8f) else if (isScanningOrConnected) skyBlueAccent.copy(alpha = 0.8f) else brightRed.copy(alpha = 0.8f)
                            )
                        ) {
                            Text(
                                text = if (isConnected) "CONNECTED (<1ms)" else if (isScanningOrConnected) "AUTO-PROBING..." else "DISCONNECTED",
                                fontSize = 10.sp,
                                fontWeight = FontWeight.Bold,
                                color = if (isConnected) Color(0xFF4ADE80) else if (isScanningOrConnected) Color(0xFF38BDF8) else Color(0xFFF87171),
                                modifier = Modifier.padding(horizontal = 8.dp, vertical = 3.dp),
                                letterSpacing = 0.5.sp
                            )
                        }
                    }

                    // Field 1: WebSocket Server URL
                    Column(verticalArrangement = Arrangement.spacedBy(4.dp)) {
                        Text(
                            text = "WebSocket Server URL",
                            fontSize = 11.sp,
                            color = labelColor
                        )
                        OutlinedTextField(
                            value = inputUrl,
                            onValueChange = {
                                inputUrl = it
                                WebSocketTradeRelay.serverUrl = it
                            },
                            placeholder = {
                                Text(
                                    text = "ws://192.168.0.117:8765",
                                    fontSize = 12.sp,
                                    color = labelColor.copy(alpha = 0.5f)
                                )
                            },
                            singleLine = true,
                            modifier = Modifier
                                .fillMaxWidth()
                                .testTag("ws_server_url_input"),
                            shape = RoundedCornerShape(8.dp),
                            colors = OutlinedTextFieldDefaults.colors(
                                focusedContainerColor = inputBackground,
                                unfocusedContainerColor = inputBackground,
                                focusedBorderColor = skyBlueAccent,
                                unfocusedBorderColor = inputBorderColor,
                                focusedTextColor = Color.White,
                                unfocusedTextColor = Color.White
                            )
                        )
                    }

                    // Field 2: HTTP Webhook Endpoint (Flask/FastAPI)
                    Column(verticalArrangement = Arrangement.spacedBy(4.dp)) {
                        Text(
                            text = "HTTP Webhook Endpoint (Flask/FastAPI)",
                            fontSize = 11.sp,
                            color = labelColor
                        )
                        OutlinedTextField(
                            value = inputWebhookUrl,
                            onValueChange = {
                                inputWebhookUrl = it
                                HttpTradeRelay.webhookUrl = it
                            },
                            placeholder = {
                                Text(
                                    text = "http://192.168.0.117:5000/trade",
                                    fontSize = 12.sp,
                                    color = labelColor.copy(alpha = 0.5f)
                                )
                            },
                            singleLine = true,
                            modifier = Modifier
                                .fillMaxWidth()
                                .testTag("http_webhook_url_input"),
                            shape = RoundedCornerShape(8.dp),
                            colors = OutlinedTextFieldDefaults.colors(
                                focusedContainerColor = inputBackground,
                                unfocusedContainerColor = inputBackground,
                                focusedBorderColor = skyBlueAccent,
                                unfocusedBorderColor = inputBorderColor,
                                focusedTextColor = Color.White,
                                unfocusedTextColor = Color.White
                            )
                        )
                    }

                    // Helper note
                    Text(
                        text = "Auto-detects and connects to laptop at ws://192.168.0.117:8765 continuously in real-time.",
                        fontSize = 10.sp,
                        color = labelColor,
                        lineHeight = 14.sp
                    )

                    Spacer(modifier = Modifier.height(2.dp))

                    // Connect Now + Refresh Action Row
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.spacedBy(8.dp),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Button(
                            onClick = {
                                haptic.performHapticFeedback(HapticFeedbackType.LongPress)
                                if (isConnected) {
                                    WebSocketTradeRelay.disconnectByUser()
                                } else {
                                    WebSocketTradeRelay.updateEndpointsAndReconnect(inputUrl, inputWebhookUrl)
                                    if (!isAutoTradeEnabled) {
                                        onToggleAutoTrade()
                                    }
                                }
                            },
                            modifier = Modifier
                                .weight(1f)
                                .height(46.dp)
                                .testTag("ws_toggle_connect_button"),
                            shape = RoundedCornerShape(8.dp),
                            colors = ButtonDefaults.buttonColors(
                                containerColor = if (isConnected) brightRed else skyBlueAccent,
                                contentColor = if (isConnected) Color.White else Color.Black
                            )
                        ) {
                            Row(
                                verticalAlignment = Alignment.CenterVertically,
                                horizontalArrangement = Arrangement.spacedBy(6.dp)
                            ) {
                                Icon(
                                    imageVector = if (isConnected) Icons.Default.Pause else Icons.Default.PlayArrow,
                                    contentDescription = null,
                                    modifier = Modifier.size(16.dp),
                                    tint = if (isConnected) Color.White else Color.Black
                                )
                                Text(
                                    text = if (isConnected) "DISCONNECT" else "CONNECT NOW",
                                    fontSize = 13.sp,
                                    fontWeight = FontWeight.Black,
                                    color = if (isConnected) Color.White else Color.Black,
                                    letterSpacing = 0.5.sp
                                )
                            }
                        }

                        // Force Reconnect / Refresh Button
                        Surface(
                            onClick = {
                                haptic.performHapticFeedback(HapticFeedbackType.TextHandleMove)
                                WebSocketTradeRelay.updateEndpointsAndReconnect(inputUrl, inputWebhookUrl)
                            },
                            modifier = Modifier
                                .size(46.dp)
                                .testTag("ws_reconnect_icon_button"),
                            shape = RoundedCornerShape(8.dp),
                            color = Color(0xFF141920),
                            border = BorderStroke(1.dp, Color(0xFF2C343D))
                        ) {
                            Box(contentAlignment = Alignment.Center) {
                                Icon(
                                    imageVector = Icons.Default.Refresh,
                                    contentDescription = "Force Reconnect",
                                    tint = skyBlueAccent,
                                    modifier = Modifier.size(18.dp)
                                )
                            }
                        }
                    }
                }
            }
        }

        // CARD 2: MANUAL TRADING TRIGGER ACTIONS
        item {
            Surface(
                modifier = Modifier
                    .fillMaxWidth()
                    .testTag("manual_triggers_card"),
                shape = RoundedCornerShape(16.dp),
                color = cardBackground,
                border = BorderStroke(0.8.dp, Color(0xFF222830))
            ) {
                Column(
                    modifier = Modifier.padding(14.dp),
                    verticalArrangement = Arrangement.spacedBy(12.dp)
                ) {
                    Text(
                        text = "MANUAL TRADING TRIGGER ACTIONS",
                        fontSize = 11.sp,
                        fontWeight = FontWeight.Bold,
                        color = labelColor,
                        letterSpacing = 0.5.sp
                    )

                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.spacedBy(10.dp)
                    ) {
                        // BUY / CALL (UP) Button
                        val isBuyActive = activeClickedButton == "BUY"
                        Button(
                            onClick = {
                                haptic.performHapticFeedback(HapticFeedbackType.LongPress)
                                activeClickedButton = "BUY"
                                coroutineScope.launch {
                                    delay(400)
                                    if (activeClickedButton == "BUY") activeClickedButton = null
                                }
                                val currentWebhook = if (inputWebhookUrl.isNotBlank()) inputWebhookUrl.trim() else HttpTradeRelay.DEFAULT_HTTP_URL
                                val currentWs = if (inputUrl.isNotBlank()) inputUrl.trim() else WebSocketTradeRelay.DEFAULT_SERVER_URL
                                HttpTradeRelay.webhookUrl = currentWebhook
                                WebSocketTradeRelay.serverUrl = currentWs
                                TradeExecutionDispatcher.dispatchManualTrade("CLICK_BUY", 100.0)
                            },
                            modifier = Modifier
                                .weight(1f)
                                .height(54.dp)
                                .testTag("manual_buy_button"),
                            shape = RoundedCornerShape(10.dp),
                            colors = ButtonDefaults.buttonColors(
                                containerColor = if (isBuyActive) Color(0xFF4ADE80) else brightGreen
                            )
                        ) {
                            Column(
                                horizontalAlignment = Alignment.CenterHorizontally,
                                verticalArrangement = Arrangement.Center
                            ) {
                                Text(
                                    text = "BUY / CALL (UP)",
                                    fontSize = 13.5.sp,
                                    fontWeight = FontWeight.Black,
                                    color = Color.Black
                                )
                                Text(
                                    text = "CLICK_BUY",
                                    fontSize = 9.5.sp,
                                    fontWeight = FontWeight.Bold,
                                    color = Color.Black.copy(alpha = 0.75f)
                                )
                            }
                        }

                        // SELL / PUT (DOWN) Button
                        val isSellActive = activeClickedButton == "SELL"
                        Button(
                            onClick = {
                                haptic.performHapticFeedback(HapticFeedbackType.LongPress)
                                activeClickedButton = "SELL"
                                coroutineScope.launch {
                                    delay(400)
                                    if (activeClickedButton == "SELL") activeClickedButton = null
                                }
                                val currentWebhook = if (inputWebhookUrl.isNotBlank()) inputWebhookUrl.trim() else HttpTradeRelay.DEFAULT_HTTP_URL
                                val currentWs = if (inputUrl.isNotBlank()) inputUrl.trim() else WebSocketTradeRelay.DEFAULT_SERVER_URL
                                HttpTradeRelay.webhookUrl = currentWebhook
                                WebSocketTradeRelay.serverUrl = currentWs
                                TradeExecutionDispatcher.dispatchManualTrade("CLICK_SELL", 100.0)
                            },
                            modifier = Modifier
                                .weight(1f)
                                .height(54.dp)
                                .testTag("manual_sell_button"),
                            shape = RoundedCornerShape(10.dp),
                            colors = ButtonDefaults.buttonColors(
                                containerColor = if (isSellActive) Color(0xFFF87171) else brightRed
                            )
                        ) {
                            Column(
                                horizontalAlignment = Alignment.CenterHorizontally,
                                verticalArrangement = Arrangement.Center
                            ) {
                                Text(
                                    text = "SELL / PUT (DOWN)",
                                    fontSize = 13.5.sp,
                                    fontWeight = FontWeight.Black,
                                    color = Color.White
                                )
                                Text(
                                    text = "CLICK_SELL",
                                    fontSize = 9.5.sp,
                                    fontWeight = FontWeight.Bold,
                                    color = Color.White.copy(alpha = 0.85f)
                                )
                            }
                        }
                    }
                }
            }
        }

        // CARD 3: LIVE DIAGNOSTICS & EVENT LOGS
        item {
            Surface(
                modifier = Modifier
                    .fillMaxWidth()
                    .testTag("ws_diagnostics_card"),
                shape = RoundedCornerShape(16.dp),
                color = cardBackground,
                border = BorderStroke(0.8.dp, Color(0xFF222830))
            ) {
                Column(
                    modifier = Modifier.padding(14.dp),
                    verticalArrangement = Arrangement.spacedBy(10.dp)
                ) {
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Text(
                            text = "LIVE DIAGNOSTICS & EVENT LOGS",
                            fontSize = 11.sp,
                            fontWeight = FontWeight.Bold,
                            color = labelColor,
                            letterSpacing = 0.5.sp
                        )

                        Text(
                            text = "Clear",
                            fontSize = 11.5.sp,
                            fontWeight = FontWeight.Bold,
                            color = skyBlueAccent,
                            modifier = Modifier
                                .testTag("ws_clear_logs_button")
                                .padding(horizontal = 4.dp, vertical = 2.dp)
                                .clickable { WebSocketTradeRelay.clearLogs() }
                        )
                    }

                    // Monospace Log Box
                    Box(
                        modifier = Modifier
                            .fillMaxWidth()
                            .height(220.dp)
                            .clip(RoundedCornerShape(10.dp))
                            .background(Color(0xFF0C0F12))
                            .border(0.5.dp, Color(0xFF1E252B), RoundedCornerShape(10.dp))
                            .padding(10.dp)
                    ) {
                        if (logs.isEmpty()) {
                            Column(verticalArrangement = Arrangement.spacedBy(4.dp)) {
                                Text(
                                    text = "[09:52:42] Sentinel: Probing network for laptop server...",
                                    fontSize = 9.5.sp,
                                    fontFamily = FontFamily.Monospace,
                                    color = labelColor
                                )
                                Text(
                                    text = "[09:52:46] Connection error: Failed to connect to /192.168.0.117:8765 • Auto-reconnecting...",
                                    fontSize = 9.5.sp,
                                    fontFamily = FontFamily.Monospace,
                                    color = Color(0xFFF87171)
                                )
                            }
                        } else {
                            LazyColumn(
                                modifier = Modifier.fillMaxSize(),
                                verticalArrangement = Arrangement.spacedBy(4.dp)
                            ) {
                                items(logs) { logLine ->
                                    val isError = logLine.contains("error", ignoreCase = true) ||
                                            logLine.contains("failed", ignoreCase = true)
                                    val isSuccess = logLine.contains("Sent Signal", ignoreCase = true) ||
                                            logLine.contains("Connected", ignoreCase = true) ||
                                            logLine.contains("Delivered", ignoreCase = true)
                                    val isSentinel = logLine.contains("Sentinel", ignoreCase = true) ||
                                            logLine.contains("Probing", ignoreCase = true)

                                    val logColor = when {
                                        isError -> Color(0xFFF87171)
                                        isSuccess -> Color(0xFF4ADE80)
                                        isSentinel -> labelColor
                                        else -> Color(0xFFCBD5E1)
                                    }

                                    Text(
                                        text = logLine,
                                        fontSize = 9.5.sp,
                                        fontFamily = FontFamily.Monospace,
                                        color = logColor,
                                        lineHeight = 14.sp
                                    )
                                }
                            }
                        }
                    }
                }
            }
        }

        item {
            Spacer(modifier = Modifier.height(16.dp))
        }
    }
}
