package com.example.data.models

import com.example.data.analyzer.NextMovementBias

/**
 * Micro-Movement Prediction Model
 * 
 * Detects short-term pullback patterns before the main direction resumes.
 * For example: DOWN signal with slight UP pullback due to 60min improvement.
 * 
 * IMPORTANT: This is a probabilistic indicator, NOT a guaranteed prediction.
 * It helps identify potential wick traps and better entry timing.
 */
data class MicroMovementPrediction(
    val hasPrediction: Boolean = false,
    val primaryDirection: TradeDirection = TradeDirection.NEUTRAL,
    val microPullback: NextMovementBias = NextMovementBias.UNKNOWN,
    val confidence: Double = 0.0,
    val reason: String = "",
    val triggerCondition: MicroTriggerCondition = MicroTriggerCondition.NONE
) {
    companion object {
        val NO_PREDICTION = MicroMovementPrediction(
            hasPrediction = false,
            primaryDirection = TradeDirection.NEUTRAL,
            microPullback = NextMovementBias.UNKNOWN,
            confidence = 0.0,
            reason = "No micro-movement pattern detected",
            triggerCondition = MicroTriggerCondition.NONE
        )
    }
}

/**
 * Micro-movement trigger conditions
 */
enum class MicroTriggerCondition {
    NONE,
    IMPROVING_60M_STILL_BEARISH,    // 60min improving but still negative
    IMPROVING_60M_STILL_BULLISH,    // 60min improving but still positive
    DETERIORATING_60M_STILL_BEARISH, // 60min deteriorating but still negative
    DETERIORATING_60M_STILL_BULLISH, // 60min deteriorating but still positive
    FIVE_MIN_SPIKE_AGAINST_TREND,   // 5min spike against 60min trend
    DIVERGENCE_PATTERN               // 5min and 60min moving in opposite directions
}