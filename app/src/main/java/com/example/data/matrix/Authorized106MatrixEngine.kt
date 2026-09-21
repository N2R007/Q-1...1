package com.example.data.matrix

import com.example.data.models.MetricSnapshot
import com.example.data.models.TradeDirection

/**
 * Directional Matrix Engine Adapter.
 * Discards legacy matrix rules and delegates strictly to Directional206MatrixEngine
 * containing EXACTLY 206 RULES (103 UP + 103 DOWN).
 */
object Authorized106MatrixEngine {

    data class Matrix106Match(
        val id: String,
        val direction: TradeDirection,
        val outputCode: String,
        val title: String,
        val conditionDescription: String,
        val priority: Int
    )

    /**
     * Evaluates val5m and val60m against the 206 Directional Matrix Rules.
     * Evaluates in deterministic order with Single-Match Guarantee.
     */
    fun evaluate(
        val5m: Double?,
        val60m: Double?,
        val1d: Double? = null,
        history: List<MetricSnapshot> = emptyList()
    ): Matrix106Match? {
        if (val5m == null || val60m == null) return null

        // 1. First priority: Strict 206 Directional Matrix Rules & User Custom Rules
        val res = Directional206MatrixEngine.evaluate(
            val5m = val5m,
            val60m = val60m,
            val1d = val1d,
            history = history
        )

        if (res != null) {
            val effDir = UserRuleRegistry.getRuleOverride(res.id) ?: res.direction
            return Matrix106Match(
                id = res.id,
                direction = effDir,
                outputCode = res.outputCode,
                title = res.title,
                conditionDescription = if (effDir != res.direction) {
                    "User Override ${res.id}: [$effDir] (Default: ${res.direction}) ${res.outputCode}"
                } else res.conditionDescription,
                priority = res.priority
            )
        }

        // 2. Second priority: Dynamic Delta Momentum Engine (স্বয়ংক্রিয় গাণিতিক ডেল্টা মোমেন্টাম ভেক্টর)
        // Dynamically calculates direction using live 5m/60m percentages, rate of change (Delta), and trend vectors.
        val dyn = DynamicDeltaMomentumEngine.calculate(
            val5m = val5m,
            val60m = val60m,
            val1d = val1d,
            history = history
        ) ?: return null

        val effDir = UserRuleRegistry.getRuleOverride(dyn.id) ?: dyn.direction
        return Matrix106Match(
            id = dyn.id,
            direction = effDir,
            outputCode = dyn.outputCode,
            title = dyn.title,
            conditionDescription = if (effDir != dyn.direction) {
                "User Override ${dyn.id}: [$effDir] (Default: ${dyn.direction})"
            } else dyn.conditionDescription,
            priority = 50
        )
    }
}


