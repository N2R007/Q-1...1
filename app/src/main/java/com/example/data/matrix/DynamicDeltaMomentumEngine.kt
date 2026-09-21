package com.example.data.matrix

import com.example.data.models.MetricSnapshot
import com.example.data.models.TradeDirection
import kotlin.math.abs

/**
 * Dynamic Delta Momentum Engine (স্বয়ংক্রিয় গাণিতিক ডেল্টা মোমেন্টাম ইঞ্জিন).
 *
 * When 5m and 60m percentages do not match any of the pre-defined 206/312 matrix rules,
 * this engine evaluates the mathematical trajectory, rate of change (Delta Δ),
 * multi-timeframe confluence ratio, and vector directional pressure to generate a valid,
 * authoritative UP or DOWN quantitative signal.
 */
object DynamicDeltaMomentumEngine {

    data class DynamicMatch(
        val id: String,
        val direction: TradeDirection,
        val outputCode: String,
        val title: String,
        val conditionDescription: String
    )

    fun calculate(
        val5m: Double,
        val60m: Double,
        val1d: Double? = null,
        history: List<MetricSnapshot> = emptyList()
    ): DynamicMatch? {
        // 1. Noise Filter: If both 5m and 60m are virtually zero dead-zone, do not trade
        if (abs(val5m) < 0.005 && abs(val60m) < 0.005) {
            return null
        }

        // 2. Extract previous readings from history to compute Delta (Δ)
        val prevSnapshot = history.lastOrNull { it.val5m != null && it.val60m != null }
            ?: history.firstOrNull { it.val5m != null && it.val60m != null }

        val prev5m = prevSnapshot?.val5m
        val prev60m = prevSnapshot?.val60m

        val delta5m = if (prev5m != null) val5m - prev5m else 0.0
        val delta60m = if (prev60m != null) val60m - prev60m else 0.0

        val netSum = val5m + val60m
        val deltaNet = delta5m + delta60m

        // 3. Mathematical Vector Analysis:
        // Weightings: 5m has immediate reactive velocity (65%), 60m has macro bias (35%)
        val momentumVector = (val5m * 0.65) + (val60m * 0.35)
        val deltaVelocity = (delta5m * 0.70) + (delta60m * 0.30)

        // Case A: High Velocity Delta Movement (Rate of change is significant)
        if (prev5m != null && abs(delta5m) >= 0.01) {
            // Strong UP Delta expansion or reversal
            if (delta5m > 0 && (val5m > 0 || deltaNet > 0 || deltaVelocity > 0)) {
                return createMatch(TradeDirection.UP, val5m, val60m, delta5m, delta60m, "DELTA_EXPANSION_UP")
            }
            // Strong DOWN Delta expansion or breakdown
            if (delta5m < 0 && (val5m < 0 || deltaNet < 0 || deltaVelocity < 0)) {
                return createMatch(TradeDirection.DOWN, val5m, val60m, delta5m, delta60m, "DELTA_EXPANSION_DOWN")
            }
        }

        // Case B: Alignment Confluence (Both timeframes point in same direction)
        if (val5m > 0 && val60m > 0) {
            return createMatch(TradeDirection.UP, val5m, val60m, delta5m, delta60m, "DUAL_TIMEFRAME_BULLISH")
        }
        if (val5m < 0 && val60m < 0) {
            return createMatch(TradeDirection.DOWN, val5m, val60m, delta5m, delta60m, "DUAL_TIMEFRAME_BEARISH")
        }

        // Case C: Divergence / Reversal based on dominant momentum vector
        if (momentumVector > 0.02) {
            return createMatch(TradeDirection.UP, val5m, val60m, delta5m, delta60m, "VECTOR_MOMENTUM_UP")
        }
        if (momentumVector < -0.02) {
            return createMatch(TradeDirection.DOWN, val5m, val60m, delta5m, delta60m, "VECTOR_MOMENTUM_DOWN")
        }

        // Case D: Net Sum dominance if vector is marginally flat
        if (netSum > 0.03) {
            return createMatch(TradeDirection.UP, val5m, val60m, delta5m, delta60m, "NET_SURPLUS_UP")
        }
        if (netSum < -0.03) {
            return createMatch(TradeDirection.DOWN, val5m, val60m, delta5m, delta60m, "NET_DEFICIT_DOWN")
        }

        // Dead-zone / Ambiguous chop: Safe wait
        return null
    }

    private fun createMatch(
        dir: TradeDirection,
        val5m: Double,
        val60m: Double,
        delta5m: Double,
        delta60m: Double,
        code: String
    ): DynamicMatch {
        val id = if (dir == TradeDirection.UP) "DYN-UP" else "DYN-DN"
        val effectiveDir = UserRuleRegistry.getRuleOverride(id) ?: dir
        val formatted5m = if (val5m >= 0) "+%.2f%%".format(val5m) else "%.2f%%".format(val5m)
        val formatted60m = if (val60m >= 0) "+%.2f%%".format(val60m) else "%.2f%%".format(val60m)
        val formattedD5 = if (delta5m >= 0) "+%.2f%%".format(delta5m) else "%.2f%%".format(delta5m)

        return DynamicMatch(
            id = id,
            direction = effectiveDir,
            outputCode = code,
            title = if (effectiveDir == TradeDirection.UP) "DYNAMIC MATH (UP)" else "DYNAMIC MATH (DOWN)",
            conditionDescription = "Auto Dynamic: 5m=$formatted5m (Δ$formattedD5), 60m=$formatted60m"
        )
    }
}
