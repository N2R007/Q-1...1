package com.example.data.analyzer

import com.example.data.models.MicroMovementPrediction
import com.example.data.models.MicroTriggerCondition
import com.example.data.models.TradeDirection
import kotlin.math.abs

/**
 * Micro-Pullback Detection Engine
 * 
 * Detects short-term pullback patterns that occur before the main direction resumes.
 * This helps identify potential wick traps and provides better entry timing signals.
 * 
 * IMPORTANT: This is a probabilistic indicator, NOT a guaranteed prediction.
 * Use it as a supplementary signal alongside the main analysis.
 */
object MicroPullbackDetector {
    
    private const val IMPROVEMENT_THRESHOLD = 0.05  // 0.05% change considered meaningful
    private const val CONFIDENCE_BASE = 70.0        // Base confidence for predictions
    private const val CONFIDENCE_MAX = 85.0         // Maximum confidence
    
    /**
     * Analyzes current vs previous metrics to detect micro-pullback patterns
     * 
     * @param prev5m Previous 5-minute percentage
     * @param prev60m Previous 60-minute percentage  
     * @param curr5m Current 5-minute percentage
     * @param curr60m Current 60-minute percentage
     * @param primaryDirection Main signal direction from core analysis
     * @return MicroMovementPrediction with detected pattern details
     */
    fun detectMicroPullback(
        prev5m: Double?,
        prev60m: Double?,
        curr5m: Double?,
        curr60m: Double?,
        primaryDirection: TradeDirection = TradeDirection.NEUTRAL
    ): MicroMovementPrediction {
        
        // Return no prediction if data is insufficient
        if (prev5m == null || prev60m == null || curr5m == null || curr60m == null) {
            return MicroMovementPrediction.NO_PREDICTION
        }
        
        // Check for invalid values
        if (prev5m.isNaN() || prev60m.isNaN() || curr5m.isNaN() || curr60m.isNaN() ||
            prev5m.isInfinite() || prev60m.isInfinite() || curr5m.isInfinite() || curr60m.isInfinite()) {
            return MicroMovementPrediction.NO_PREDICTION
        }
        
        // Calculate 60-minute improvement/deterioration
        val sixtyMinChange = curr60m - prev60m
        val isImproving60m = sixtyMinChange > IMPROVEMENT_THRESHOLD
        val isDeteriorating60m = sixtyMinChange < -IMPROVEMENT_THRESHOLD
        
        // Check 5-minute changes
        val fiveMinChange = curr5m - prev5m
        val isFiveMinUp = fiveMinChange > IMPROVEMENT_THRESHOLD
        val isFiveMinDown = fiveMinChange < -IMPROVEMENT_THRESHOLD
        
        // Pattern 1: 60min improving but still bearish (potential UP pullback before DOWN continues)
        if (isImproving60m && curr60m < 0 && primaryDirection == TradeDirection.DOWN) {
            val confidence = calculateConfidence(abs(sixtyMinChange), abs(fiveMinChange))
            return MicroMovementPrediction(
                hasPrediction = true,
                primaryDirection = TradeDirection.DOWN,
                microPullback = NextMovementBias.UP,
                confidence = confidence,
                reason = "60min improving (${"%.2f".format(sixtyMinChange)}%) but still negative (${"%.2f".format(curr60m)}%)",
                triggerCondition = MicroTriggerCondition.IMPROVING_60M_STILL_BEARISH
            )
        }
        
        // Pattern 2: 60min improving but still bullish (potential DOWN pullback before UP continues)
        if (isImproving60m && curr60m > 0 && primaryDirection == TradeDirection.UP) {
            val confidence = calculateConfidence(abs(sixtyMinChange), abs(fiveMinChange))
            return MicroMovementPrediction(
                hasPrediction = true,
                primaryDirection = TradeDirection.UP,
                microPullback = NextMovementBias.DOWN,
                confidence = confidence,
                reason = "60min improving (${"%.2f".format(sixtyMinChange)}%) but still positive (${"%.2f".format(curr60m)}%)",
                triggerCondition = MicroTriggerCondition.IMPROVING_60M_STILL_BULLISH
            )
        }
        
        // Pattern 3: 60min deteriorating but still bearish (accelerating DOWN)
        if (isDeteriorating60m && curr60m < 0 && primaryDirection == TradeDirection.DOWN) {
            val confidence = calculateConfidence(abs(sixtyMinChange), abs(fiveMinChange))
            return MicroMovementPrediction(
                hasPrediction = true,
                primaryDirection = TradeDirection.DOWN,
                microPullback = NextMovementBias.DOWN,
                confidence = confidence,
                reason = "60min deteriorating (${"%.2f".format(sixtyMinChange)}%) while still negative (${"%.2f".format(curr60m)}%)",
                triggerCondition = MicroTriggerCondition.DETERIORATING_60M_STILL_BEARISH
            )
        }
        
        // Pattern 4: 60min deteriorating but still bullish (accelerating UP)
        if (isDeteriorating60m && curr60m > 0 && primaryDirection == TradeDirection.UP) {
            val confidence = calculateConfidence(abs(sixtyMinChange), abs(fiveMinChange))
            return MicroMovementPrediction(
                hasPrediction = true,
                primaryDirection = TradeDirection.UP,
                microPullback = NextMovementBias.UP,
                confidence = confidence,
                reason = "60min deteriorating (${"%.2f".format(sixtyMinChange)}%) while still positive (${"%.2f".format(curr60m)}%)",
                triggerCondition = MicroTriggerCondition.DETERIORATING_60M_STILL_BULLISH
            )
        }
        
        // Pattern 5: 5min spike against 60min trend
        if (isDivergencePattern(curr5m, curr60m, primaryDirection)) {
            val confidence = CONFIDENCE_BASE + 5.0
            return MicroMovementPrediction(
                hasPrediction = true,
                primaryDirection = primaryDirection,
                microPullback = if (primaryDirection == TradeDirection.DOWN) NextMovementBias.UP else NextMovementBias.DOWN,
                confidence = confidence.coerceAtMost(CONFIDENCE_MAX),
                reason = "5min (${"%.2f".format(curr5m)}%) spiking against 60min (${"%.2f".format(curr60m)}%) trend",
                triggerCondition = MicroTriggerCondition.FIVE_MIN_SPIKE_AGAINST_TREND
            )
        }
        
        // Pattern 6: General divergence pattern
        if (isGeneralDivergence(curr5m, curr60m)) {
            val confidence = CONFIDENCE_BASE
            return MicroMovementPrediction(
                hasPrediction = true,
                primaryDirection = if (abs(curr60m) > abs(curr5m)) {
                    if (curr60m > 0) TradeDirection.UP else TradeDirection.DOWN
                } else {
                    if (curr5m > 0) TradeDirection.UP else TradeDirection.DOWN
                },
                microPullback = NextMovementBias.NEUTRAL,
                confidence = confidence,
                reason = "Divergence between 5min (${"%.2f".format(curr5m)}%) and 60min (${"%.2f".format(curr60m)}%)",
                triggerCondition = MicroTriggerCondition.DIVERGENCE_PATTERN
            )
        }
        
        // No clear pattern detected
        return MicroMovementPrediction.NO_PREDICTION
    }
    
    /**
     * Calculates confidence based on magnitude of changes
     */
    private fun calculateConfidence(sixtyMinChange: Double, fiveMinChange: Double): Double {
        val magnitudeScore = (sixtyMinChange * 10.0 + fiveMinChange * 5.0).coerceIn(0.0, 15.0)
        return (CONFIDENCE_BASE + magnitudeScore).coerceAtMost(CONFIDENCE_MAX)
    }
    
    /**
     * Checks if 5min is spiking against the 60min trend
     */
    private fun isDivergencePattern(curr5m: Double, curr60m: Double, primaryDirection: TradeDirection): Boolean {
        val is60mBearish = curr60m < -0.10
        val is60mBullish = curr60m > 0.10
        val is5mStrongUp = curr5m > 0.15
        val is5mStrongDown = curr5m < -0.15
        
        return when (primaryDirection) {
            TradeDirection.DOWN -> is60mBearish && is5mStrongUp
            TradeDirection.UP -> is60mBullish && is5mStrongDown
            else -> false
        }
    }
    
    /**
     * Checks for general divergence between timeframes
     */
    private fun isGeneralDivergence(curr5m: Double, curr60m: Double): Boolean {
        val is5mPositive = curr5m > 0.05
        val is5mNegative = curr5m < -0.05
        val is60mPositive = curr60m > 0.05
        val is60mNegative = curr60m < -0.05
        
        return (is5mPositive && is60mNegative) || (is5mNegative && is60mPositive)
    }
}