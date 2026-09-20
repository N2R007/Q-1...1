package com.example.data.analyzer

import com.example.data.models.MicroMovementPrediction
import com.example.data.models.MicroTriggerCondition
import com.example.data.models.TradeDirection
import kotlin.math.abs

/**
 * Ultra-Sensitive Velocity-Momentum Divergence Engine (v2.0)
 * 
 * Detects short-term pullback patterns that occur before the main direction resumes.
 * Operates with ultra-high sensitivity (0.001% threshold / 50x sensitive),
 * analyzing 5m and 60m velocity, 2.5x momentum amplification, and divergence intensity.
 * 
 * IMPORTANT: This is a probabilistic indicator, NOT a guaranteed prediction.
 * It provides auxiliary confirmation and warning against wick traps without
 * modifying primary execution rules.
 */
object MicroPullbackDetector {
    
    // Ultra-sensitive configuration thresholds
    private const val IMPROVEMENT_THRESHOLD = 0.001 // 0.001% threshold (50x more sensitive than 0.05%)
    private const val VELOCITY_THRESHOLD = 0.0005   // 0.0005% velocity threshold
    private const val MOMENTUM_FACTOR = 2.5         // 2.5x momentum amplification factor
    private const val CONFIDENCE_BASE = 70.0        // Base confidence (70.0%)
    private const val CONFIDENCE_MAX = 85.0         // Maximum confidence (85.0%)
    
    /**
     * Analyzes current vs previous metrics to detect micro-pullback patterns
     * using the Ultra-Sensitive Velocity-Momentum Divergence Formula.
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
        
        // Step 2: Velocity Calculation (Rate of change)
        val sixtyMinChange = curr60m - prev60m
        val fiveMinChange = curr5m - prev5m
        
        // Step 3: Momentum Calculation (2.5x amplification)
        val fiveMinMomentum = fiveMinChange * MOMENTUM_FACTOR
        val sixtyMinMomentum = sixtyMinChange * MOMENTUM_FACTOR
        
        // Step 4: Divergence Intensity Calculation
        val divergenceIntensity = abs(fiveMinChange - sixtyMinChange)
        val isStrongDivergence = divergenceIntensity > VELOCITY_THRESHOLD
        
        // Step 5: Threshold Check (0.001% ultra-sensitive)
        val isImproving60m = sixtyMinChange > IMPROVEMENT_THRESHOLD
        val isDeteriorating60m = sixtyMinChange < -IMPROVEMENT_THRESHOLD
        val isFiveMinUp = fiveMinChange > IMPROVEMENT_THRESHOLD
        val isFiveMinDown = fiveMinChange < -IMPROVEMENT_THRESHOLD
        
        // Step 6: Pattern Matching (7 Patterns)
        
        // Pattern 1: 60min improving but still bearish (potential UP pullback before DOWN continues)
        if (isImproving60m && curr60m < 0 && primaryDirection == TradeDirection.DOWN) {
            val confidence = calculateConfidence(abs(sixtyMinChange), abs(fiveMinChange))
            return MicroMovementPrediction(
                hasPrediction = true,
                primaryDirection = TradeDirection.DOWN,
                microPullback = NextMovementBias.UP,
                confidence = confidence,
                reason = "60min improving (${"%.4f".format(sixtyMinChange)}%) but still negative (${"%.4f".format(curr60m)}%)",
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
                reason = "60min improving (${"%.4f".format(sixtyMinChange)}%) but still positive (${"%.4f".format(curr60m)}%)",
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
                reason = "60min deteriorating (${"%.4f".format(sixtyMinChange)}%) while still negative (${"%.4f".format(curr60m)}%)",
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
                reason = "60min deteriorating (${"%.4f".format(sixtyMinChange)}%) while still positive (${"%.4f".format(curr60m)}%)",
                triggerCondition = MicroTriggerCondition.DETERIORATING_60M_STILL_BULLISH
            )
        }
        
        // Pattern 5: 5min spike against 60min trend
        if (isDivergencePattern(curr5m, curr60m, primaryDirection)) {
            val confidence = (CONFIDENCE_BASE + 5.0).coerceAtMost(CONFIDENCE_MAX)
            return MicroMovementPrediction(
                hasPrediction = true,
                primaryDirection = primaryDirection,
                microPullback = if (primaryDirection == TradeDirection.DOWN) NextMovementBias.UP else NextMovementBias.DOWN,
                confidence = confidence,
                reason = "5min (${"%.4f".format(curr5m)}%) spiking against 60min (${"%.4f".format(curr60m)}%) trend",
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
                reason = "Divergence between 5min (${"%.4f".format(curr5m)}%) and 60min (${"%.4f".format(curr60m)}%)",
                triggerCondition = MicroTriggerCondition.DIVERGENCE_PATTERN
            )
        }

        // Pattern 7: Velocity Divergence (Ultra-Sensitive New Feature)
        if (isStrongDivergence) {
            val bias = if (fiveMinChange > sixtyMinChange) NextMovementBias.UP else NextMovementBias.DOWN
            val confidence = calculateConfidence(abs(sixtyMinChange), abs(fiveMinChange))
            return MicroMovementPrediction(
                hasPrediction = true,
                primaryDirection = primaryDirection,
                microPullback = bias,
                confidence = confidence,
                reason = "Velocity divergence: 5min (${"%.4f".format(fiveMinChange)}%) vs 60min (${"%.4f".format(sixtyMinChange)}%)",
                triggerCondition = MicroTriggerCondition.VELOCITY_DIVERGENCE
            )
        }
        
        // No clear pattern detected
        return MicroMovementPrediction.NO_PREDICTION
    }
    
    /**
     * Step 7: Confidence Calculation
     * magnitudeScore = (sixtyMinChange * 10.0 + fiveMinChange * 5.0).coerceIn(0.0, 15.0)
     * confidence = (70.0 + magnitudeScore).coerceAtMost(85.0)
     */
    private fun calculateConfidence(sixtyMinChange: Double, fiveMinChange: Double): Double {
        val magnitudeScore = (abs(sixtyMinChange) * 10.0 + abs(fiveMinChange) * 5.0).coerceIn(0.0, 15.0)
        return (CONFIDENCE_BASE + magnitudeScore).coerceAtMost(CONFIDENCE_MAX)
    }
    
    /**
     * Checks if 5min is spiking against the 60min trend (Ultra-sensitive thresholds)
     */
    private fun isDivergencePattern(curr5m: Double, curr60m: Double, primaryDirection: TradeDirection): Boolean {
        val is60mBearish = curr60m < -0.01 // Ultra-sensitive: -0.01%
        val is60mBullish = curr60m > 0.01  // Ultra-sensitive: +0.01%
        val is5mStrongUp = curr5m > 0.02   // Ultra-sensitive: +0.02%
        val is5mStrongDown = curr5m < -0.02 // Ultra-sensitive: -0.02%
        
        return when (primaryDirection) {
            TradeDirection.DOWN -> is60mBearish && is5mStrongUp
            TradeDirection.UP -> is60mBullish && is5mStrongDown
            else -> false
        }
    }
    
    /**
     * Checks for general divergence between timeframes (Ultra-sensitive thresholds: 0.001%)
     */
    private fun isGeneralDivergence(curr5m: Double, curr60m: Double): Boolean {
        val is5mPositive = curr5m > 0.001
        val is5mNegative = curr5m < -0.001
        val is60mPositive = curr60m > 0.001
        val is60mNegative = curr60m < -0.001
        
        return (is5mPositive && is60mNegative) || (is5mNegative && is60mPositive)
    }
}
