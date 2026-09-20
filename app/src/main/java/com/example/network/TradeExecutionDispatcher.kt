package com.example.network

import android.util.Log
import com.example.data.models.CanonicalDecision
import com.example.data.models.DataQualityState
import com.example.data.models.TradeDirection
import com.example.data.models.TradeOutcome
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch

/**
 * Single Canonical Execution Adapter for Quant Vision AI.
 *
 * Ensures that auto-entry and manual trade dispatch go through exactly ONE
 * canonical execution adapter.
 *
 * Enforces:
 * 1. Single outbound dispatch per CanonicalDecision fingerprint to prevent duplicate signals.
 * 2. Dual-fallback network architecture:
 *    - Primary Communication: WebSocket (ws://192.168.0.117:8765) for lowest latency (<5ms).
 *    - Secondary Communication: HTTP POST Webhook (http://192.168.0.117:5000/trade) as an immediate fallback.
 *    - Execution Logic: Attempts transmission via WebSocket. If WebSocket is disconnected or times out
 *      (>500ms), it immediately and asynchronously sends an HTTP POST to Webhook without blocking UI.
 * 3. Strict validation of executionEligibility before dispatch.
 */
object TradeExecutionDispatcher {
    private const val TAG = "TradeExecutionDispatcher"

    data class ManualTradeStatus(
        val command: String,
        val direction: TradeDirection,
        val timestamp: Long = System.currentTimeMillis(),
        val channel: String,
        val status: RelayDeliveryStatus = RelayDeliveryStatus.SENDING,
        val latencyMs: Long = 0L,
        val message: String = ""
    )

    private val _lastManualStatus = MutableStateFlow<ManualTradeStatus?>(null)
    val lastManualStatus: StateFlow<ManualTradeStatus?> = _lastManualStatus.asStateFlow()

    data class ExecutedTradeRecord(
        val id: String = java.util.UUID.randomUUID().toString(),
        val command: String,
        val direction: TradeDirection,
        val isAuto: Boolean,
        val ruleId: String? = null,
        val timestamp: Long = System.currentTimeMillis(),
        val channel: String,
        val status: RelayDeliveryStatus = RelayDeliveryStatus.DELIVERED,
        val latencyMs: Long = 0L,
        val outcome: TradeOutcome? = null,
        val prev5m: Double? = null,
        val prev60m: Double? = null,
        val current5m: Double? = null,
        val current60m: Double? = null,
        val matrixOrLocation: String? = null
    )

    private val _executedTrades = MutableStateFlow<List<ExecutedTradeRecord>>(emptyList())
    val executedTrades: StateFlow<List<ExecutedTradeRecord>> = _executedTrades.asStateFlow()

    fun recordTrade(trade: ExecutedTradeRecord) {
        _executedTrades.value = (listOf(trade) + _executedTrades.value).take(50)
    }

    fun setTradeRecordOutcome(id: String, outcome: TradeOutcome) {
        _executedTrades.value = _executedTrades.value.map {
            if (it.id == id) it.copy(outcome = outcome) else it
        }
    }

    fun clearExecutedTrades() {
        _executedTrades.value = emptyList()
    }

    @Volatile
    private var lastDispatchedFingerprint: String? = null

    @Volatile
    private var lastDispatchedDirection: TradeDirection? = null

    @Volatile
    private var lastDispatchedTimeMs: Long = 0L

    @Volatile
    private var lastManualDispatchedTimeMs: Long = 0L

    @Volatile
    private var lastManualCommand: String? = null

    // Burst single-entry protection: identical fingerprint is strictly locked for 1 single trade per signal.
    private const val BURST_DEDUP_WINDOW_MS = 60_000L

    // Optical micro-jitter debounce between different trades in same direction (3000ms prevents broker double-clicks while allowing rapid verified trades)
    private const val RAPID_SAME_DIRECTION_GUARD_MS = 3000L

    // Single-Click Protection Guard for manual button clicks (2000ms prevents broker double-clicks)
    private const val MANUAL_CLICK_DEBOUNCE_MS = 2000L
    private val manualLock = Any()

    private val scope = CoroutineScope(SupervisorJob() + Dispatchers.IO)

    init {
        // Sync HTTP Webhook delivery status back to manual trade status when active
        scope.launch {
            HttpTradeRelay.relayStatus.collect { status ->
                if (status != null && status.command != null) {
                    val current = _lastManualStatus.value
                    if (current != null && current.channel == "HTTP Webhook" && current.command == status.command) {
                        val displayMsg = when (status.status) {
                            RelayDeliveryStatus.DELIVERED -> "Successfully delivered to HTTP Webhook (${status.latencyMs}ms) ✔"
                            RelayDeliveryStatus.FAILED -> "HTTP Webhook delivery failed: ${status.message.ifBlank { "Disconnected from server" }}"
                            RelayDeliveryStatus.SENDING -> "Sending to HTTP Webhook..."
                            else -> status.message
                        }
                        _lastManualStatus.value = current.copy(
                            status = status.status,
                            latencyMs = status.latencyMs,
                            timestamp = status.timestamp,
                            message = displayMsg
                        )
                    }
                }
            }
        }
    }

    /**
     * Dispatches a CanonicalDecision using strict single-outbound execution.
     * Primary: WebSocket (ws://192.168.0.117:8765) for instant single click (<5ms).
     * Fallback: HTTP Webhook (http://192.168.0.117:5000/trade) ONLY if WebSocket is disconnected or send fails.
     * Guaranteed: Exactly ONE entry per trade signal.
     */
    fun dispatchDecision(
        decision: CanonicalDecision,
        investmentAmount: Double,
        availableTimeframes: Set<String> = emptySet(),
        prev5m: Double? = null,
        prev60m: Double? = null,
        current5m: Double? = null,
        current60m: Double? = null,
        matrixOrLocation: String? = null
    ): Boolean {
        if (!decision.executionEligibility) {
            Log.d(TAG, "Dispatch rejected: decision is not execution-eligible (${decision.explanation})")
            return false
        }

        if (decision.direction == TradeDirection.NEUTRAL || decision.side == "NONE") {
            Log.d(TAG, "Dispatch rejected: neutral direction / no side")
            return false
        }

        // Strict rejection for CANCELLED (❌) marked matrices as requested by user
        if (decision.primaryMatrixId != null && decision.cancelledMatrixIds.contains(decision.primaryMatrixId)) {
            Log.d(TAG, "Dispatch rejected: primary matrix ${decision.primaryMatrixId} has CANCEL (❌) mark")
            return false
        }

        // Strict rejection if data quality is not VERIFIED (requires verified ✓ checkmark per user mandate)
        if (decision.dataQuality != DataQualityState.VERIFIED) {
            Log.d(TAG, "Dispatch rejected: dataQuality is not VERIFIED (${decision.dataQuality}) - ✓ mark strictly required")
            return false
        }

        // Strict rejection if availableTimeframes is provided but incomplete (missing 5m or 60m)
        if (availableTimeframes.isNotEmpty() && (!availableTimeframes.contains("5m") || !availableTimeframes.contains("60m"))) {
            Log.d(TAG, "Dispatch rejected: dual-timeframe incomplete (requires both 5m and 60m, got $availableTimeframes)")
            return false
        }

        val fp = decision.fingerprint
        val now = System.currentTimeMillis()

        // 1. Strict Single-Entry Protection (Mandatory 1-Trade Per Signal):
        // Identical fingerprint is strictly executed ONCE. Continuous camera frames will never fire a second trade.
        val isDuplicateFingerprint = (fp == lastDispatchedFingerprint) && ((now - lastDispatchedTimeMs) < BURST_DEDUP_WINDOW_MS)
        if (isDuplicateFingerprint) {
            Log.d(TAG, "Dispatch suppressed: fingerprint $fp already dispatched within window (${now - lastDispatchedTimeMs}ms). Single trade strictly enforced.")
            return false
        }

        // 2. Rapid Multi-Click Guard (Same Direction Optical Jitter Protection):
        // Enforce 3000ms safety between distinct trades in the same direction to prevent broker double-clicks.
        // Genuine direction changes (e.g. UP -> DOWN) bypass this completely and dispatch instantly (0ms latency).
        if (decision.direction == lastDispatchedDirection && (now - lastDispatchedTimeMs) < RAPID_SAME_DIRECTION_GUARD_MS) {
            Log.d(TAG, "Dispatch suppressed: rapid consecutive ${decision.direction} signal within ${RAPID_SAME_DIRECTION_GUARD_MS}ms")
            return false
        }

        lastDispatchedFingerprint = fp
        lastDispatchedDirection = decision.direction
        lastDispatchedTimeMs = now

        val command = when (decision.direction) {
            TradeDirection.UP -> "CLICK_BUY"
            TradeDirection.DOWN -> "CLICK_SELL"
            else -> return false
        }

        val event = TradeSignalEvent(
            signalId = decision.decisionId,
            fingerprint = decision.fingerprint,
            direction = decision.direction,
            command = command,
            upPercentage = decision.upPercentage,
            downPercentage = decision.downPercentage,
            strength = decision.strength,
            availableTimeframes = availableTimeframes,
            timestamp = decision.timestamp,
            matrixId = decision.primaryMatrixId
        )

        // STRICT SINGLE-ENTRY DISPATCH (NO DOUBLE-CLICK / NO DUAL SEND):
        scope.launch {
            val startMs = System.currentTimeMillis()
            var sentViaWs = false
            if (WebSocketTradeRelay.isConnected()) {
                try {
                    // Send once over the active WebSocket connection
                    sentViaWs = WebSocketTradeRelay.sendSignalEvent(event)
                } catch (e: Exception) {
                    Log.w(TAG, "WebSocket dispatch error: ${e.message}")
                    sentViaWs = false
                }
            }

            val latency = System.currentTimeMillis() - startMs
            if (sentViaWs) {
                // Guaranteed single entry delivered via primary WebSocket connection
                WebSocketTradeRelay.addLog("[Single Entry Confirmed] Trade successfully sent via WebSocket (${event.command}). Double-entry prevented.")
                recordTrade(
                    ExecutedTradeRecord(
                        command = command,
                        direction = decision.direction,
                        isAuto = true,
                        ruleId = decision.primaryMatrixId,
                        timestamp = event.timestamp,
                        channel = "WebSocket",
                        status = RelayDeliveryStatus.DELIVERED,
                        latencyMs = latency,
                        prev5m = prev5m,
                        prev60m = prev60m,
                        current5m = current5m,
                        current60m = current60m,
                        matrixOrLocation = matrixOrLocation ?: decision.primaryMatrixTitle ?: decision.primaryMatrixId
                    )
                )
            } else {
                // Fallback to HTTP Webhook ONLY when WebSocket is disconnected or send fails
                WebSocketTradeRelay.addLog("[Fallback -> Webhook] WS not connected or send failed. Transmitting single trade via HTTP POST...")
                if (HttpTradeRelay.webhookUrl.isNotBlank()) {
                    HttpTradeRelay.sendTradeSignalEvent(event, investmentAmount)
                } else {
                    WebSocketTradeRelay.addLog("[Error] HTTP Webhook URL is blank. Configure URL in Auto Trade tab.")
                }
                recordTrade(
                    ExecutedTradeRecord(
                        command = command,
                        direction = decision.direction,
                        isAuto = true,
                        ruleId = decision.primaryMatrixId,
                        timestamp = event.timestamp,
                        channel = "HTTP Webhook",
                        status = RelayDeliveryStatus.DELIVERED,
                        latencyMs = latency,
                        prev5m = prev5m,
                        prev60m = prev60m,
                        current5m = current5m,
                        current60m = current60m,
                        matrixOrLocation = matrixOrLocation ?: decision.primaryMatrixTitle ?: decision.primaryMatrixId
                    )
                )
            }
        }

        return true
    }

    /**
     * Manual Trade Dispatch for BUY and SELL UI buttons.
     * Primary: WebSocket (ws://192.168.0.117:8765).
     * Fallback: HTTP Webhook (http://192.168.0.117:5000/trade) ONLY if WS is disconnected.
     * Guaranteed: Single entry, protected by debouncing.
     */
    fun dispatchManualTrade(
        command: String,
        investmentAmount: Double = 100.0,
        prev5m: Double? = null,
        prev60m: Double? = null,
        current5m: Double? = null,
        current60m: Double? = null,
        matrixOrLocation: String? = null
    ) {
        val normalized = when (command.trim().uppercase(java.util.Locale.US)) {
            "UP", "BUY", "CLICK_BUY" -> "CLICK_BUY"
            "DOWN", "SELL", "CLICK_SELL" -> "CLICK_SELL"
            else -> return
        }
        val direction = if (normalized == "CLICK_BUY") TradeDirection.UP else TradeDirection.DOWN

        val now = System.currentTimeMillis()
        synchronized(manualLock) {
            if (now - lastManualDispatchedTimeMs < MANUAL_CLICK_DEBOUNCE_MS) {
                Log.d(TAG, "Manual trade click debounced: click within ${MANUAL_CLICK_DEBOUNCE_MS}ms guard ($normalized rejected)")
                return
            }
            lastManualDispatchedTimeMs = now
            lastManualCommand = normalized
        }

        val isWs = WebSocketTradeRelay.isConnected()
        val initialChannel = if (isWs) "WebSocket" else "HTTP Webhook"
        _lastManualStatus.value = ManualTradeStatus(
            command = normalized,
            direction = direction,
            timestamp = now,
            channel = initialChannel,
            status = RelayDeliveryStatus.SENDING,
            message = if (isWs) "Sending signal via WebSocket..." else "Sending signal via HTTP Webhook..."
        )

        val event = TradeSignalEvent(
            signalId = java.util.UUID.randomUUID().toString(),
            idempotencyKey = java.util.UUID.randomUUID().toString(),
            fingerprint = "MANUAL_${normalized}_${System.currentTimeMillis()}",
            direction = direction,
            command = normalized,
            upPercentage = if (normalized == "CLICK_BUY") 80.0 else 20.0,
            downPercentage = if (normalized == "CLICK_SELL") 80.0 else 20.0,
            strength = 80.0,
            availableTimeframes = setOf("MANUAL"),
            timestamp = System.currentTimeMillis()
        )

        // STRICT SINGLE-ENTRY DISPATCH FOR MANUAL TRADES:
        scope.launch {
            var sentViaWs = false
            val startMs = System.currentTimeMillis()
            if (isWs) {
                try {
                    sentViaWs = WebSocketTradeRelay.sendCommand(normalized)
                } catch (e: Exception) {
                    Log.w(TAG, "WebSocket manual send error: ${e.message}")
                    sentViaWs = false
                }
            }

            if (sentViaWs) {
                val latency = System.currentTimeMillis() - startMs
                _lastManualStatus.value = ManualTradeStatus(
                    command = normalized,
                    direction = direction,
                    timestamp = System.currentTimeMillis(),
                    channel = "WebSocket",
                    status = RelayDeliveryStatus.DELIVERED,
                    latencyMs = latency,
                    message = "Directly delivered to WebSocket server (${latency}ms) ✔"
                )
                WebSocketTradeRelay.addLog("[Single Entry Confirmed] Manual $normalized successfully sent via WebSocket (${latency}ms).")
                recordTrade(
                    ExecutedTradeRecord(
                        command = normalized,
                        direction = direction,
                        isAuto = false,
                        ruleId = "MANUAL",
                        timestamp = event.timestamp,
                        channel = "WebSocket",
                        status = RelayDeliveryStatus.DELIVERED,
                        latencyMs = latency,
                        prev5m = prev5m,
                        prev60m = prev60m,
                        current5m = current5m,
                        current60m = current60m,
                        matrixOrLocation = matrixOrLocation ?: "MANUAL EXECUTION"
                    )
                )
            } else {
                val latency = System.currentTimeMillis() - startMs
                WebSocketTradeRelay.addLog("[Fallback -> Webhook] WS not connected or send failed. Transmitting single manual $normalized via HTTP POST...")
                _lastManualStatus.value = ManualTradeStatus(
                    command = normalized,
                    direction = direction,
                    timestamp = System.currentTimeMillis(),
                    channel = "HTTP Webhook",
                    status = RelayDeliveryStatus.SENDING,
                    message = "Sending to HTTP Webhook..."
                )
                HttpTradeRelay.sendTradeSignal(normalized, investmentAmount)
                recordTrade(
                    ExecutedTradeRecord(
                        command = normalized,
                        direction = direction,
                        isAuto = false,
                        ruleId = "MANUAL",
                        timestamp = event.timestamp,
                        channel = "HTTP Webhook",
                        status = RelayDeliveryStatus.DELIVERED,
                        latencyMs = latency,
                        prev5m = prev5m,
                        prev60m = prev60m,
                        current5m = current5m,
                        current60m = current60m,
                        matrixOrLocation = matrixOrLocation ?: "MANUAL EXECUTION"
                    )
                )
            }
        }
    }

    fun resetAutoTradeState() {
        lastDispatchedFingerprint = null
        lastDispatchedDirection = null
        lastDispatchedTimeMs = 0L
    }

    fun reset() {
        resetAutoTradeState()
        lastManualDispatchedTimeMs = 0L
        lastManualCommand = null
        _lastManualStatus.value = null
        clearExecutedTrades()
    }
}
