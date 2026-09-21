package com.example.data.matrix

import android.content.Context
import android.content.SharedPreferences
import com.example.data.models.TradeDirection
import org.json.JSONArray
import org.json.JSONObject
import java.util.concurrent.ConcurrentHashMap

/**
 * Data class representing a user-created custom rule.
 * Triggered when 5m and 60m percentages fall within the specified ranges.
 */
data class CustomRule(
    val id: String, // e.g. "C001", "C002"
    val title: String, // e.g. "Scalp Momentum"
    val min5m: Double,
    val max5m: Double,
    val min60m: Double,
    val max60m: Double,
    val direction: TradeDirection,
    val isActive: Boolean = true,
    val createdAt: Long = System.currentTimeMillis()
)

/**
 * Data class representing a rule item for the verification matrix interface.
 */
data class MatrixRuleVerificationItem(
    val serial: Int,
    val id: String,
    val defaultDirection: TradeDirection,
    val title: String,
    val category: String,
    val isCustom: Boolean = false,
    val globalSerial: Int = serial
)

/**
 * Registry for:
 * 1. User direction overrides on existing 206 directional rules (e.g. U001 -> DOWN).
 * 2. User-created custom rules captured from live screen or created manually.
 *
 * Persisted in SharedPreferences so settings survive app restarts.
 * Zero-risk: Can be completely reset back to pristine defaults at any time.
 */
object UserRuleRegistry {
    private const val PREFS_NAME = "quant_user_rule_registry_v1"
    private const val KEY_OVERRIDES = "rule_direction_overrides"
    private const val KEY_CUSTOM_RULES = "user_custom_rules"
    private const val KEY_VERIFIED_RULES = "user_verified_rules"
    private const val KEY_UNVERIFIED_RULES = "user_unverified_rules"
    private const val KEY_INIT_VERIFIED_POWER_V1 = "init_verified_power_v1"
    private const val KEY_INIT_VERIFIED_HIGH_V2 = "init_verified_high_v2"
    private const val KEY_INIT_VERIFIED_ALL_V3 = "init_verified_all_v3"
    private const val KEY_HAS_SAVED_VERIFICATION = "user_has_saved_verification_v1"

    // Complete System Rule Catalog (D001-D165, U001-U103, M001-M165)
    val ALL_SYSTEM_RULE_IDS: Set<String> by lazy {
        val set = LinkedHashSet<String>()
        for (i in 1..165) {
            set.add("D%03d".format(i))
        }
        for (i in 1..103) {
            set.add("U%03d".format(i))
        }
        for (i in 1..165) {
            set.add("M%03d".format(i))
        }
        set
    }

    // All High Category rules pre-verified with tick mark (✓) for UP & DOWN per user mandate
    val DEFAULT_VERIFIED_HIGH_RULES = setOf(
        // Category 1: Mega Super Climax (UP & DOWN)
        "U035", "U036", "D035", "D036",
        // Category 2: Ultra Climax & Hyper Blowout (UP & DOWN - Includes U032 & D032)
        "U027", "U028", "U029", "U030", "U031", "U032", "U033", "U034",
        "D027", "D028", "D029", "D030", "D031", "D032", "D033", "D034",
        // Category 3: High Momentum & Dual Expansion (UP & DOWN)
        "U019", "U020", "U021", "U022", "U023", "U024", "U025", "U026",
        "D019", "D020", "D021", "D022", "D023", "D024", "D025", "D026",
        // Category 4: High Velocity & Fast Momentum Breakout / Strong Anchor (UP & DOWN)
        "U011", "U012", "U013", "U014", "U015", "U016", "U017", "U018",
        "D011", "D012", "D013", "D014", "D015", "D016", "D017", "D018",
        // Category 5: Deep Pullback Heavy Support & Macro Recovery (UP & DOWN)
        "U042", "U043", "U044",
        "D042", "D043", "D044",
        // Category 6: Macro Bottom Exhaustion & Extreme Reversal (UP & DOWN)
        "U053", "U054",
        "D053", "D054",
        // Category 7: Harmonic High Alignment (UP & DOWN)
        "U069", "U070",
        "D069", "D070",
        // Category 8: Asymmetric High Dominance (UP & DOWN)
        "U073", "U074", "U077", "U078",
        "D073", "D074", "D077", "D078",
        // Category 9: Range High Volatility Escape & Drop (UP & DOWN)
        "U057", "U058",
        "D057", "D058",
        // Category 10: Aggressive Net Sum & Squeeze Breakout (UP & DOWN)
        "U099", "U100", "U102", "U103",
        "D099", "D100", "D102", "D103"
    )

    val DEFAULT_VERIFIED_POWER_RULES = DEFAULT_VERIFIED_HIGH_RULES

    val GOOD_STRONG_RULES = setOf(
        "U011", "U012", "U013", "U014", "U015", "U016", "U017", "U018",
        "D011", "D012", "D013", "D014", "D015", "D016", "D017", "D018",
        "U042", "U043", "U044", "D042", "D043", "D044",
        "U053", "U054", "D053", "D054"
    )

    // Medium rules (without default tick mark)
    val MEDIUM_RULES = setOf(
        "U005", "U006", "U007", "U008", "U009", "U010",
        "D005", "D006", "D007", "D008", "D009", "D010",
        "U037", "U038", "U039", "U040", "U041",
        "D037", "D038", "D039", "D040", "D041",
        "U047", "U048", "U049", "U050", "U051", "U052",
        "D047", "D048", "D049", "D050", "D051", "D052",
        "U055", "U056", "U059", "U060",
        "D055", "D056", "D059", "D060",
        "U071", "U072", "U075", "U076",
        "D071", "D072", "D075", "D076"
    )

    /**
     * Converts any user or system rule ID representation (e.g. "u1", "d61", "m42", "c1", "[D061]")
     * into standard 3-digit canonical format ("U001", "D061", "M042", "C001").
     */
    fun canonicalizeRuleId(raw: String?): String {
        if (raw.isNullOrBlank()) return ""
        val clean = raw.replace("[", "").replace("]", "").trim().uppercase()
        val match = Regex("^([UDMC])(\\d+)$").find(clean)
        if (match != null) {
            val prefix = match.groupValues[1]
            val num = match.groupValues[2].toIntOrNull()
            if (num != null) {
                return "%s%03d".format(prefix, num)
            }
        }
        val pureDigitsMatch = Regex("^(\\d+)$").find(clean)
        if (pureDigitsMatch != null) {
            val num = pureDigitsMatch.groupValues[1].toIntOrNull()
            if (num != null) {
                return "M%03d".format(num)
            }
        }
        return clean
    }

    fun getRuleCategoryLabel(ruleId: String): String {
        val clean = canonicalizeRuleId(ruleId)
        return when {
            clean.startsWith("DYN") -> "DYNAMIC MOMENTUM (স্বয়ংক্রিয় ডেল্টা ভেক্টর - HIGH)"
            clean.startsWith("C") -> "CUSTOM STRATEGY (কাস্টম রুল - HIGH)"
            clean in setOf("U035", "U036", "D035", "D036") -> "MEGA SUPER CLIMAX (HIGH)"
            clean in setOf("U027", "U028", "U029", "U030", "U031", "U032", "U033", "U034", "D027", "D028", "D029", "D030", "D031", "D032", "D033", "D034") -> "ULTRA CLIMAX (HIGH)"
            clean in setOf("U019", "U020", "U021", "U022", "U023", "U024", "U025", "U026", "D019", "D020", "D021", "D022", "D023", "D024", "D025", "D026") -> "HIGH MOMENTUM (HIGH)"
            clean in setOf("U011", "U012", "U013", "U014", "U015", "U016", "U017", "U018", "D011", "D012", "D013", "D014", "D015", "D016", "D017", "D018") -> "HIGH VELOCITY & ANCHOR (HIGH)"
            clean in setOf("U042", "U043", "U044", "D042", "D043", "D044") -> "HEAVY SUPPORT RECOVERY (HIGH)"
            clean in setOf("U053", "U054", "D053", "D054") -> "MACRO REVERSAL EXTREME (HIGH)"
            clean in setOf("U069", "U070", "D069", "D070") -> "HARMONIC ALIGNMENT (HIGH)"
            clean in setOf("U073", "U074", "U077", "U078", "D073", "D074", "D077", "D078") -> "ASYMMETRIC DOMINANCE (HIGH)"
            clean in setOf("U057", "U058", "D057", "D058") -> "RANGE HIGH ESCAPE (HIGH)"
            clean in setOf("U099", "U100", "U102", "U103", "D099", "D100", "D102", "D103") -> "AGGRESSIVE EXPANSION (HIGH)"
            clean in MEDIUM_RULES -> "MEDIUM / মিডিয়াম"
            else -> "NORMAL / লো"
        }
    }

    /**
     * Returns the simplified English tier ("HIGH", "MEDIUM", "LOW") for dashboard UI display.
     */
    fun getRuleTierSimple(ruleId: String): String {
        val clean = canonicalizeRuleId(ruleId)
        if (clean.isBlank()) return ""
        return when {
            clean.startsWith("DYN") || clean in DEFAULT_VERIFIED_HIGH_RULES || clean.startsWith("C") -> "HIGH"
            clean in MEDIUM_RULES -> "MEDIUM"
            else -> "LOW"
        }
    }

    private var preferences: SharedPreferences? = null

    // Cache of 206 rule direction overrides: Rule ID -> TradeDirection
    private val directionOverrides = ConcurrentHashMap<String, TradeDirection>()

    // Cache of Custom Rules
    private val customRules = ArrayList<CustomRule>()
    private val lock = Any()

    // Cache of User-Verified Rules (Rule ID -> Boolean)
    private val verifiedRuleIds = java.util.concurrent.ConcurrentHashMap.newKeySet<String>()
    private val unverifiedRuleIds = java.util.concurrent.ConcurrentHashMap.newKeySet<String>()

    fun init(context: Context) {
        try {
            val appCtx = context.applicationContext ?: context
            preferences = appCtx.getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE)
            loadFromPrefs()
        } catch (_: Exception) {}
    }

    private fun loadFromPrefs() {
        val prefs = preferences ?: return
        try {
            // 1. Load direction overrides
            directionOverrides.clear()
            val overridesJsonStr = prefs.getString(KEY_OVERRIDES, null)
            if (!overridesJsonStr.isNullOrBlank()) {
                val json = JSONObject(overridesJsonStr)
                json.keys().forEach { key ->
                    val dirStr = json.optString(key)
                    val dir = try { TradeDirection.valueOf(dirStr) } catch (_: Exception) { null }
                    if (dir != null) {
                        directionOverrides[key] = dir
                    }
                }
            }

            // 2. Load custom rules
            synchronized(lock) {
                customRules.clear()
                val customJsonStr = prefs.getString(KEY_CUSTOM_RULES, null)
                if (!customJsonStr.isNullOrBlank()) {
                    val arr = JSONArray(customJsonStr)
                    for (i in 0 until arr.length()) {
                        val obj = arr.getJSONObject(i)
                        val id = obj.getString("id")
                        val title = obj.optString("title", "Custom Rule $id")
                        val min5m = obj.getDouble("min5m")
                        val max5m = obj.getDouble("max5m")
                        val min60m = obj.getDouble("min60m")
                        val max60m = obj.getDouble("max60m")
                        val dirStr = obj.getString("direction")
                        val direction = TradeDirection.valueOf(dirStr)
                        val isActive = obj.optBoolean("isActive", true)
                        val createdAt = obj.optLong("createdAt", System.currentTimeMillis())

                        customRules.add(
                            CustomRule(
                                id = id,
                                title = title,
                                min5m = min5m,
                                max5m = max5m,
                                min60m = min60m,
                                max60m = max60m,
                                direction = direction,
                                isActive = isActive,
                                createdAt = createdAt
                            )
                        )
                    }
                }
            }

            // 3. Load verified & unverified rules
            verifiedRuleIds.clear()
            unverifiedRuleIds.clear()
            val unverifiedSet = prefs.getStringSet(KEY_UNVERIFIED_RULES, null)
            if (unverifiedSet != null) {
                unverifiedRuleIds.addAll(unverifiedSet)
            }

            val hasSaved = prefs.getBoolean(KEY_HAS_SAVED_VERIFICATION, false)
            val hasInitializedV3 = prefs.getBoolean(KEY_INIT_VERIFIED_ALL_V3, false)
            if (!hasInitializedV3 && !hasSaved) {
                // First-run only: Pre-verify ALL system rules (U001-U103, D001-D103, M001-M165) for 100% auto-trade readiness
                verifiedRuleIds.addAll(ALL_SYSTEM_RULE_IDS)
                val existing = prefs.getStringSet(KEY_VERIFIED_RULES, null)
                if (existing != null) {
                    verifiedRuleIds.addAll(existing)
                }
                // Subtract any explicitly unverified rules
                verifiedRuleIds.removeAll(unverifiedRuleIds)
                prefs.edit()
                    .putStringSet(KEY_VERIFIED_RULES, HashSet(verifiedRuleIds))
                    .putBoolean(KEY_INIT_VERIFIED_POWER_V1, true)
                    .putBoolean(KEY_INIT_VERIFIED_HIGH_V2, true)
                    .putBoolean(KEY_INIT_VERIFIED_ALL_V3, true)
                    .putBoolean(KEY_HAS_SAVED_VERIFICATION, true)
                    .apply()
            } else {
                val verifiedSet = prefs.getStringSet(KEY_VERIFIED_RULES, null)
                if (verifiedSet != null) {
                    verifiedRuleIds.addAll(verifiedSet)
                }
                verifiedRuleIds.removeAll(unverifiedRuleIds)
            }
        } catch (_: Exception) {}
    }

    private fun saveVerifiedRulesToPrefs() {
        try {
            val prefs = preferences ?: return
            prefs.edit()
                .putStringSet(KEY_VERIFIED_RULES, HashSet(verifiedRuleIds))
                .putStringSet(KEY_UNVERIFIED_RULES, HashSet(unverifiedRuleIds))
                .putBoolean(KEY_HAS_SAVED_VERIFICATION, true)
                .apply()
        } catch (_: Exception) {}
    }

    private fun saveOverridesToPrefs() {
        try {
            val prefs = preferences ?: return
            val json = JSONObject()
            directionOverrides.forEach { (id, dir) ->
                json.put(id, dir.name)
            }
            prefs.edit().putString(KEY_OVERRIDES, json.toString()).apply()
        } catch (_: Exception) {}
    }

    private fun saveCustomRulesToPrefs() {
        try {
            val prefs = preferences ?: return
            val arr = JSONArray()
            synchronized(lock) {
                for (rule in customRules) {
                    val obj = JSONObject().apply {
                        put("id", rule.id)
                        put("title", rule.title)
                        put("min5m", rule.min5m)
                        put("max5m", rule.max5m)
                        put("min60m", rule.min60m)
                        put("max60m", rule.max60m)
                        put("direction", rule.direction.name)
                        put("isActive", rule.isActive)
                        put("createdAt", rule.createdAt)
                    }
                    arr.put(obj)
                }
            }
            prefs.edit().putString(KEY_CUSTOM_RULES, arr.toString()).apply()
        } catch (_: Exception) {}
    }

    // --- OVERRIDE METHODS ---

    fun getRuleOverride(ruleId: String): TradeDirection? {
        val cleanId = canonicalizeRuleId(ruleId)
        return directionOverrides[cleanId] ?: directionOverrides[ruleId.replace("[", "").replace("]", "").uppercase().trim()]
    }

    fun setRuleOverride(ruleId: String, direction: TradeDirection) {
        val cleanId = canonicalizeRuleId(ruleId)
        if (cleanId.isNotBlank()) {
            directionOverrides[cleanId] = direction
            // User-edited rules are guaranteed verified for immediate auto-trade execution
            setRuleVerified(cleanId, true)
            saveOverridesToPrefs()
        }
    }

    fun removeRuleOverride(ruleId: String) {
        val cleanId = canonicalizeRuleId(ruleId)
        directionOverrides.remove(cleanId)
        directionOverrides.remove(ruleId.replace("[", "").replace("]", "").uppercase().trim())
        saveOverridesToPrefs()
    }

    fun getAllOverrides(): Map<String, TradeDirection> {
        return directionOverrides.toMap()
    }

    fun clearAllOverrides() {
        directionOverrides.clear()
        saveOverridesToPrefs()
    }

    // --- CUSTOM RULES METHODS ---

    fun getCustomRules(): List<CustomRule> {
        synchronized(lock) {
            return ArrayList(customRules)
        }
    }

    fun getNextCustomRuleId(): String {
        synchronized(lock) {
            var index = 1
            while (customRules.any { it.id.equals("C%03d".format(index), ignoreCase = true) }) {
                index++
            }
            return "C%03d".format(index)
        }
    }

    fun addOrUpdateCustomRule(rule: CustomRule) {
        val canonicalRule = rule.copy(
            id = canonicalizeRuleId(rule.id).ifBlank { getNextCustomRuleId() },
            min5m = minOf(rule.min5m, rule.max5m),
            max5m = maxOf(rule.min5m, rule.max5m),
            min60m = minOf(rule.min60m, rule.max60m),
            max60m = maxOf(rule.min60m, rule.max60m)
        )
        synchronized(lock) {
            val existingIdx = customRules.indexOfFirst { it.id.equals(canonicalRule.id, ignoreCase = true) }
            if (existingIdx >= 0) {
                customRules[existingIdx] = canonicalRule
            } else {
                customRules.add(canonicalRule)
            }
        }
        setRuleVerified(canonicalRule.id, true)
        saveCustomRulesToPrefs()
    }

    fun deleteCustomRule(ruleId: String) {
        val cleanId = canonicalizeRuleId(ruleId)
        synchronized(lock) {
            customRules.removeAll { it.id.equals(cleanId, ignoreCase = true) || it.id.equals(ruleId, ignoreCase = true) }
        }
        saveCustomRulesToPrefs()
    }

    fun toggleCustomRule(ruleId: String, isActive: Boolean) {
        val cleanId = canonicalizeRuleId(ruleId)
        synchronized(lock) {
            val idx = customRules.indexOfFirst { it.id.equals(cleanId, ignoreCase = true) || it.id.equals(ruleId, ignoreCase = true) }
            if (idx >= 0) {
                customRules[idx] = customRules[idx].copy(isActive = isActive)
            }
        }
        saveCustomRulesToPrefs()
    }

    fun clearAllCustomRules() {
        synchronized(lock) {
            customRules.clear()
        }
        saveCustomRulesToPrefs()
    }

    /**
     * Evaluates custom rules before any built-in 206 rules.
     * Evaluates in strict order. First matching active rule returns.
     */
    fun evaluateCustomRules(val5m: Double, val60m: Double): Directional206MatrixEngine.DirectionalMatrixMatch? {
        synchronized(lock) {
            for (rule in customRules) {
                if (!rule.isActive) continue
                val low5 = minOf(rule.min5m, rule.max5m)
                val high5 = maxOf(rule.min5m, rule.max5m)
                val low60 = minOf(rule.min60m, rule.max60m)
                val high60 = maxOf(rule.min60m, rule.max60m)

                // Epsilon tolerance (0.0001) to prevent double precision boundary issues
                val match5 = val5m >= (low5 - 0.0001) && val5m <= (high5 + 0.0001)
                val match60 = val60m >= (low60 - 0.0001) && val60m <= (high60 + 0.0001)

                if (match5 && match60) {
                    val effDir = getRuleOverride(rule.id) ?: rule.direction
                    return Directional206MatrixEngine.DirectionalMatrixMatch(
                        id = rule.id,
                        direction = effDir,
                        outputCode = "CUSTOM_${rule.id}",
                        title = rule.title.ifBlank { "Custom Rule ${rule.id}" },
                        conditionDescription = "Custom [$effDir] 5m:[$low5..$high5] 60m:[$low60..$high60]",
                        priority = 1000 // Highest priority
                    )
                }
            }
        }
        return null
    }

    /**
     * Check if a rule is verified by the user.
     * Controlled strictly by the verification set in UserRuleRegistry:
     * - Returns true ONLY if the rule is explicitly in verifiedRuleIds and not in unverifiedRuleIds.
     * - If user deselects or clears all rules, returns false for all rules (auto-trade safely halted).
     * - When user checks specific rules and saves, returns true exclusively for those checked rules.
     */
    fun isRuleVerified(ruleId: String): Boolean {
        if (ruleId.isBlank()) return false
        val cleanId = canonicalizeRuleId(ruleId)
        if (unverifiedRuleIds.contains(cleanId)) return false
        return verifiedRuleIds.contains(cleanId)
    }

    /**
     * Toggle the verified status of a rule. Returns true if now verified, false if unverified.
     */
    fun toggleVerifiedRule(ruleId: String): Boolean {
        if (ruleId.isBlank()) return false
        val cleanId = canonicalizeRuleId(ruleId)
        val currentlyVerified = isRuleVerified(cleanId)
        val isNowVerified = if (currentlyVerified) {
            verifiedRuleIds.remove(cleanId)
            unverifiedRuleIds.add(cleanId)
            false
        } else {
            unverifiedRuleIds.remove(cleanId)
            verifiedRuleIds.add(cleanId)
            true
        }
        saveVerifiedRulesToPrefs()
        return isNowVerified
    }

    fun setRuleVerified(ruleId: String, verified: Boolean) {
        if (ruleId.isBlank()) return
        val cleanId = canonicalizeRuleId(ruleId)
        if (verified) {
            unverifiedRuleIds.remove(cleanId)
            verifiedRuleIds.add(cleanId)
        } else {
            verifiedRuleIds.remove(cleanId)
            unverifiedRuleIds.add(cleanId)
        }
        saveVerifiedRulesToPrefs()
    }

    fun getVerifiedRuleIds(): Set<String> {
        val all = LinkedHashSet<String>(verifiedRuleIds)
        all.removeAll(unverifiedRuleIds)
        return all
    }

    fun clearAllVerifiedRules() {
        verifiedRuleIds.clear()
        unverifiedRuleIds.addAll(ALL_SYSTEM_RULE_IDS)
        synchronized(lock) {
            customRules.forEach { unverifiedRuleIds.add(canonicalizeRuleId(it.id)) }
        }
        saveVerifiedRulesToPrefs()
    }

    /**
     * Batch update rule verification states and persist to SharedPreferences in a single pass.
     */
    fun setRulesBatchVerified(updates: Map<String, Boolean>) {
        updates.forEach { (ruleId, verified) ->
            val cleanId = canonicalizeRuleId(ruleId)
            if (cleanId.isNotBlank()) {
                if (verified) {
                    unverifiedRuleIds.remove(cleanId)
                    verifiedRuleIds.add(cleanId)
                } else {
                    verifiedRuleIds.remove(cleanId)
                    unverifiedRuleIds.add(cleanId)
                }
            }
        }
        saveVerifiedRulesToPrefs()
    }

    /**
     * Complete directional rule output code mapping for U001-U103 and D001-D103.
     */
    val SYSTEM_DIRECTIONAL_TITLES: Map<String, String> by lazy {
        mapOf(
            "U001" to "ULTRA_LOW_ALIGNED_UP",
            "U002" to "LOW_VOLATILITY_5M_LEAD",
            "U003" to "LOW_VOLATILITY_60M_LEAD",
            "U004" to "LOW_VOLATILITY_ALIGNED_UP",
            "U005" to "EARLY_IMPULSE_5M_LEAD_A",
            "U006" to "EARLY_IMPULSE_5M_LEAD_B",
            "U007" to "STEADY_ACCUMULATION_60M_LEAD_A",
            "U008" to "STEADY_ACCUMULATION_60M_LEAD_B",
            "U009" to "BALANCED_BULLISH_EXPANSION_LOW",
            "U010" to "BALANCED_BULLISH_EXPANSION_MID",
            "U011" to "FAST_MOMENTUM_SURGE_5M_A",
            "U012" to "FAST_MOMENTUM_SURGE_5M_B",
            "U013" to "STRONG_ANCHOR_TREND_UP_A",
            "U014" to "STRONG_ANCHOR_TREND_UP_B",
            "U015" to "HIGH_VELOCITY_BREAKOUT_A",
            "U016" to "HIGH_VELOCITY_BREAKOUT_B",
            "U017" to "MID_RANGE_CONFLUENCE_UP_A",
            "U018" to "MID_RANGE_CONFLUENCE_UP_B",
            "U019" to "AGGRESSIVE_PARABOLIC_IMPULSE_A",
            "U020" to "AGGRESSIVE_PARABOLIC_IMPULSE_B",
            "U021" to "INSTITUTIONAL_HEAVY_ACCUMULATION_A",
            "U022" to "INSTITUTIONAL_HEAVY_ACCUMULATION_B",
            "U023" to "POWER_SURGE_DUAL_EXPANSION_A",
            "U024" to "POWER_SURGE_DUAL_EXPANSION_B",
            "U025" to "STEADY_HEAVY_ACCUMULATION_A",
            "U026" to "STEADY_HEAVY_ACCUMULATION_B",
            "U027" to "EXTREME_MICRO_SQUEEZE_UP_LOW",
            "U028" to "EXTREME_MICRO_SQUEEZE_UP_HIGH",
            "U029" to "HYPER_MICRO_SQUEEZE_BLOWOUT",
            "U030" to "EXTREME_MACRO_EXPANSION_DOWN_LOW",
            "U031" to "EXTREME_MACRO_EXPANSION_UP_HIGH",
            "U032" to "HYPER_MACRO_EXPANSION_BLOWOUT",
            "U033" to "HYPER_VOLATILITY_MICRO_BREAKOUT",
            "U034" to "HYPER_VOLATILITY_MACRO_BREAKOUT",
            "U035" to "TOTAL_PARABOLIC_CLIMAX_UP",
            "U036" to "MEGA_PARABOLIC_SUPER_CLIMAX",
            "U037" to "SHALLOW_PULLBACK_DIP_BUY",
            "U038" to "LIGHT_PULLBACK_DIP_BUY",
            "U039" to "DEEP_ANCHOR_SHALLOW_PULLBACK",
            "U040" to "DEEP_ANCHOR_LIGHT_PULLBACK",
            "U041" to "MEDIUM_PULLBACK_SUPPORT_REJOIN",
            "U042" to "DEEP_PULLBACK_HEAVY_SUPPORT",
            "U043" to "EXTREME_DIP_STRONG_BULL_TREND",
            "U044" to "MAXIMUM_DIP_MACRO_BULL_RECOVERY",
            "U045" to "WEAK_REBOUND_EARLY_ATTEMPT_A",
            "U046" to "WEAK_REBOUND_EARLY_ATTEMPT_B",
            "U047" to "BULLISH_DIVERGENCE_CONFIRMED_A",
            "U048" to "BULLISH_DIVERGENCE_CONFIRMED_B",
            "U049" to "STRONG_COUNTER_TREND_PUMP_A",
            "U050" to "STRONG_COUNTER_TREND_PUMP_B",
            "U051" to "HEAVY_BEAR_DIVERGENCE_REVERSAL_A",
            "U052" to "HEAVY_BEAR_DIVERGENCE_REVERSAL_B",
            "U053" to "MACRO_BOTTOM_EXHAUSTION_REBOUND",
            "U054" to "EXTREME_V_BOTTOM_REVERSAL",
            "U055" to "CONSOLIDATION_MICRO_BREAKOUT_A",
            "U056" to "CONSOLIDATION_MICRO_BREAKOUT_B",
            "U057" to "RANGE_HIGH_VOLATILITY_ESCAPE_A",
            "U058" to "RANGE_HIGH_VOLATILITY_ESCAPE_B",
            "U059" to "EXPLOSIVE_RANGE_BREAKOUT_A",
            "U060" to "EXPLOSIVE_RANGE_BREAKOUT_B",
            "U061" to "HOURLY_BULLISH_DRIFT_A",
            "U062" to "HOURLY_BULLISH_DRIFT_B",
            "U063" to "HOURLY_STRONG_ACCUMULATION_A",
            "U064" to "HOURLY_STRONG_ACCUMULATION_B",
            "U065" to "HARMONIC_LOW_ALIGNMENT_A",
            "U066" to "HARMONIC_LOW_ALIGNMENT_B",
            "U067" to "HARMONIC_MID_ALIGNMENT_A",
            "U068" to "HARMONIC_MID_ALIGNMENT_B",
            "U069" to "HARMONIC_HIGH_ALIGNMENT_A",
            "U070" to "HARMONIC_HIGH_ALIGNMENT_B",
            "U071" to "ASYMMETRIC_5M_DOMINANCE_LOW_A",
            "U072" to "ASYMMETRIC_5M_DOMINANCE_LOW_B",
            "U073" to "ASYMMETRIC_5M_DOMINANCE_HIGH_A",
            "U074" to "ASYMMETRIC_5M_DOMINANCE_HIGH_B",
            "U075" to "ASYMMETRIC_60M_DOMINANCE_LOW_A",
            "U076" to "ASYMMETRIC_60M_DOMINANCE_LOW_B",
            "U077" to "ASYMMETRIC_60M_DOMINANCE_HIGH_A",
            "U078" to "ASYMMETRIC_60M_DOMINANCE_HIGH_B",
            "U079" to "EARLY_V_SHAPE_RECOVERY_A",
            "U080" to "EARLY_V_SHAPE_RECOVERY_B",
            "U081" to "CONFIRMED_V_SHAPE_RECOVERY_A",
            "U082" to "CONFIRMED_V_SHAPE_RECOVERY_B",
            "U083" to "EXPLOSIVE_V_SHAPE_RECOVERY_A",
            "U084" to "EXPLOSIVE_V_SHAPE_RECOVERY_B",
            "U085" to "MICRO_LEAD_SLIGHT_MACRO_POSITIVE_A",
            "U086" to "MICRO_LEAD_SLIGHT_MACRO_POSITIVE_B",
            "U087" to "STRONG_MICRO_LEAD_SLIGHT_MACRO_POSITIVE_A",
            "U088" to "STRONG_MICRO_LEAD_SLIGHT_MACRO_POSITIVE_B",
            "U089" to "SLIGHT_MICRO_POSITIVE_MACRO_LEAD_A",
            "U090" to "SLIGHT_MICRO_POSITIVE_MACRO_LEAD_B",
            "U091" to "SLIGHT_MICRO_POSITIVE_STRONG_MACRO_LEAD_A",
            "U092" to "SLIGHT_MICRO_POSITIVE_STRONG_MACRO_LEAD_B",
            "U093" to "MICRO_PULSE_NEUTRAL_MACRO_A",
            "U094" to "MICRO_PULSE_NEUTRAL_MACRO_B",
            "U095" to "STRONG_MICRO_PULSE_NEUTRAL_MACRO_A",
            "U096" to "STRONG_MICRO_PULSE_NEUTRAL_MACRO_B",
            "U097" to "INTENSE_SQUEEZE_AGAINST_BEAR_A",
            "U098" to "INTENSE_SQUEEZE_AGAINST_BEAR_B",
            "U099" to "MONSTER_BREAKOUT_FROM_SQUEEZE_A",
            "U100" to "MONSTER_BREAKOUT_FROM_SQUEEZE_B",
            "U101" to "MICRO_CONSOLIDATION_DRIFT_UP",
            "U102" to "ANCHORED_BULLISH_CONTINUATION",
            "U103" to "AGGRESSIVE_NET_SUM_EXPANSION_UP",
            "D001" to "ULTRA_LOW_ALIGNED_DOWN",
            "D002" to "LOW_VOLATILITY_5M_DROP_LEAD",
            "D003" to "LOW_VOLATILITY_60M_DUMP_LEAD",
            "D004" to "LOW_VOLATILITY_ALIGNED_DOWN",
            "D005" to "EARLY_IMPULSE_5M_DUMP_A",
            "D006" to "EARLY_IMPULSE_5M_DUMP_B",
            "D007" to "STEADY_DISTRIBUTION_60M_LEAD_A",
            "D008" to "STEADY_DISTRIBUTION_60M_LEAD_B",
            "D009" to "BALANCED_BEARISH_EXPANSION_LOW",
            "D010" to "BALANCED_BEARISH_EXPANSION_MID",
            "D011" to "FAST_SELLING_SURGE_5M_A",
            "D012" to "FAST_SELLING_SURGE_5M_B",
            "D013" to "STRONG_ANCHOR_TREND_DOWN_A",
            "D014" to "STRONG_ANCHOR_TREND_DOWN_B",
            "D015" to "HIGH_VELOCITY_BREAKDOWN_A",
            "D016" to "HIGH_VELOCITY_BREAKDOWN_B",
            "D017" to "MID_RANGE_CONFLUENCE_DOWN_A",
            "D018" to "MID_RANGE_CONFLUENCE_DOWN_B",
            "D019" to "AGGRESSIVE_WATERFALL_IMPULSE_A",
            "D020" to "AGGRESSIVE_WATERFALL_IMPULSE_B",
            "D021" to "INSTITUTIONAL_HEAVY_SELLING_A",
            "D022" to "INSTITUTIONAL_HEAVY_SELLING_B",
            "D023" to "CASCADING_DUAL_DUMP_A",
            "D024" to "CASCADING_DUAL_DUMP_B",
            "D025" to "STEADY_HEAVY_DISTRIBUTION_A",
            "D026" to "STEADY_HEAVY_DISTRIBUTION_B",
            "D027" to "EXTREME_MICRO_FLASH_DUMP_LOW",
            "D028" to "EXTREME_MICRO_FLASH_DUMP_HIGH",
            "D029" to "HYPER_MICRO_FLASH_CRASH",
            "D030" to "EXTREME_MACRO_CAPITULATION_LOW",
            "D031" to "EXTREME_MACRO_CAPITULATION_HIGH",
            "D032" to "HYPER_MACRO_CAPITULATION_CRASH",
            "D033" to "HYPER_VOLATILITY_MICRO_DUMP",
            "D034" to "HYPER_VOLATILITY_MACRO_DUMP",
            "D035" to "TOTAL_PARABOLIC_WATERFALL_DOWN",
            "D036" to "MEGA_PARABOLIC_SUPER_CRASH",
            "D037" to "SHALLOW_RALLY_PULLBACK_SELL",
            "D038" to "LIGHT_RALLY_PULLBACK_SELL",
            "D039" to "DEEP_ANCHOR_SHALLOW_RALLY",
            "D040" to "DEEP_ANCHOR_LIGHT_RALLY",
            "D041" to "MEDIUM_RALLY_RESISTANCE_REJECT",
            "D042" to "DEEP_RALLY_HEAVY_RESISTANCE",
            "D043" to "EXTREME_RALLY_STRONG_BEAR_TREND",
            "D044" to "MAXIMUM_RALLY_MACRO_BEAR_REJECTION",
            "D045" to "WEAK_DROP_EARLY_ATTEMPT_A",
            "D046" to "WEAK_DROP_EARLY_ATTEMPT_B",
            "D047" to "BEARISH_DIVERGENCE_CONFIRMED_A",
            "D048" to "BEARISH_DIVERGENCE_CONFIRMED_B",
            "D049" to "STRONG_COUNTER_TREND_DUMP_A",
            "D050" to "STRONG_COUNTER_TREND_DUMP_B",
            "D051" to "HEAVY_BULL_DIVERGENCE_REVERSAL_A",
            "D052" to "HEAVY_BULL_DIVERGENCE_REVERSAL_B",
            "D053" to "MACRO_TOP_EXHAUSTION_REJECTION",
            "D054" to "EXTREME_V_TOP_REJECTION",
            "D055" to "CONSOLIDATION_MICRO_BREAKDOWN_A",
            "D056" to "CONSOLIDATION_MICRO_BREAKDOWN_B",
            "D057" to "RANGE_HIGH_VOLATILITY_DROP_A",
            "D058" to "RANGE_HIGH_VOLATILITY_DROP_B",
            "D059" to "EXPLOSIVE_RANGE_BREAKDOWN_A",
            "D060" to "EXPLOSIVE_RANGE_BREAKDOWN_B",
            "D061" to "HOURLY_BEARISH_DRIFT_A",
            "D062" to "HOURLY_BEARISH_DRIFT_B",
            "D063" to "HOURLY_STRONG_DISTRIBUTION_A",
            "D064" to "HOURLY_STRONG_DISTRIBUTION_B",
            "D065" to "HARMONIC_LOW_BEAR_ALIGNMENT_A",
            "D066" to "HARMONIC_LOW_BEAR_ALIGNMENT_B",
            "D067" to "HARMONIC_MID_BEAR_ALIGNMENT_A",
            "D068" to "HARMONIC_MID_BEAR_ALIGNMENT_B",
            "D069" to "HARMONIC_HIGH_BEAR_ALIGNMENT_A",
            "D070" to "HARMONIC_HIGH_BEAR_ALIGNMENT_B",
            "D071" to "ASYMMETRIC_5M_BEAR_LOW_A",
            "D072" to "ASYMMETRIC_5M_BEAR_LOW_B",
            "D073" to "ASYMMETRIC_5M_BEAR_HIGH_A",
            "D074" to "ASYMMETRIC_5M_BEAR_HIGH_B",
            "D075" to "ASYMMETRIC_60M_BEAR_LOW_A",
            "D076" to "ASYMMETRIC_60M_BEAR_LOW_B",
            "D077" to "ASYMMETRIC_60M_BEAR_HIGH_A",
            "D078" to "ASYMMETRIC_60M_BEAR_HIGH_B",
            "D079" to "EARLY_INVERTED_V_REVERSAL_A",
            "D080" to "EARLY_INVERTED_V_REVERSAL_B",
            "D081" to "CONFIRMED_INVERTED_V_REVERSAL_A",
            "D082" to "CONFIRMED_INVERTED_V_REVERSAL_B",
            "D083" to "EXPLOSIVE_INVERTED_V_REVERSAL_A",
            "D084" to "EXPLOSIVE_INVERTED_V_REVERSAL_B",
            "D085" to "MICRO_DROP_SLIGHT_MACRO_NEGATIVE_A",
            "D086" to "MICRO_DROP_SLIGHT_MACRO_NEGATIVE_B",
            "D087" to "STRONG_MICRO_DROP_SLIGHT_MACRO_NEGATIVE_A",
            "D088" to "STRONG_MICRO_DROP_SLIGHT_MACRO_NEGATIVE_B",
            "D089" to "SLIGHT_MICRO_NEGATIVE_MACRO_LEAD_A",
            "D090" to "SLIGHT_MICRO_NEGATIVE_MACRO_LEAD_B",
            "D091" to "SLIGHT_MICRO_NEGATIVE_STRONG_MACRO_LEAD_A",
            "D092" to "SLIGHT_MICRO_NEGATIVE_STRONG_MACRO_LEAD_B",
            "D093" to "MICRO_DROP_NEUTRAL_MACRO_A",
            "D094" to "MICRO_DROP_NEUTRAL_MACRO_B",
            "D095" to "STRONG_MICRO_DROP_NEUTRAL_MACRO_A",
            "D096" to "STRONG_MICRO_DROP_NEUTRAL_MACRO_B",
            "D097" to "INTENSE_DUMP_AGAINST_BULL_A",
            "D098" to "INTENSE_DUMP_AGAINST_BULL_B",
            "D099" to "MONSTER_BREAKDOWN_FROM_SQUEEZE_A",
            "D100" to "MONSTER_BREAKDOWN_FROM_SQUEEZE_B",
            "D101" to "MICRO_CONSOLIDATION_DRIFT_DOWN",
            "D102" to "ANCHORED_BEARISH_CONTINUATION",
            "D103" to "AGGRESSIVE_NET_SUM_EXPANSION_DOWN"
        )
    }

    /**
     * Retrieves all available system and custom rules formatted for the verification matrix view.
     */
    fun getAllMatrixRulesForVerification(): List<MatrixRuleVerificationItem> {
        val result = ArrayList<MatrixRuleVerificationItem>()
        var globalCounter = 1

        // 1. D001 to D165 (Down Directional Matrix Rules)
        for (i in 1..165) {
            val id = "D%03d".format(i)
            val title = SYSTEM_DIRECTIONAL_TITLES[id] ?: "EXTENDED_DOWN_VECTOR_$id"
            result.add(
                MatrixRuleVerificationItem(
                    serial = i,
                    id = id,
                    defaultDirection = TradeDirection.DOWN,
                    title = title,
                    category = getRuleCategoryLabel(id),
                    isCustom = false,
                    globalSerial = globalCounter++
                )
            )
        }

        // 2. U001 to U103 (Up Directional Matrix Rules)
        for (i in 1..103) {
            val id = "U%03d".format(i)
            val title = SYSTEM_DIRECTIONAL_TITLES[id] ?: "EXTENDED_UP_VECTOR_$id"
            result.add(
                MatrixRuleVerificationItem(
                    serial = i,
                    id = id,
                    defaultDirection = TradeDirection.UP,
                    title = title,
                    category = getRuleCategoryLabel(id),
                    isCustom = false,
                    globalSerial = globalCounter++
                )
            )
        }

        // 3. M001 to M165 (Quantitative Confluence Matrices)
        MatrixCatalog.allMatrices.forEachIndexed { idx, m ->
            result.add(
                MatrixRuleVerificationItem(
                    serial = idx + 1,
                    id = m.id,
                    defaultDirection = m.direction,
                    title = m.outputCode.ifBlank { m.title },
                    category = m.category,
                    isCustom = false,
                    globalSerial = globalCounter++
                )
            )
        }

        // 4. Custom Rules (Added by User in Tab 2 New Rule) - Always placed at the bottom with latest serial number!
        val customList = getCustomRules()
        customList.forEachIndexed { idx, cr ->
            val mMatch = Regex("^M(\\d+)$").find(cr.id.uppercase())
            val customMatrixSerial = if (mMatch != null) {
                mMatch.groupValues[1].toIntOrNull() ?: (165 + idx + 1)
            } else {
                idx + 1
            }
            result.add(
                MatrixRuleVerificationItem(
                    serial = customMatrixSerial,
                    id = cr.id,
                    defaultDirection = cr.direction,
                    title = cr.title.ifBlank { "Custom Rule ${cr.id}" },
                    category = "User Custom Strategy",
                    isCustom = true,
                    globalSerial = globalCounter++
                )
            )
        }

        return result
    }

    /**
     * Reset everything back to original state with all system rules verified.
     */
    fun resetAll() {
        clearAllOverrides()
        clearAllCustomRules()
        unverifiedRuleIds.clear()
        verifiedRuleIds.clear()
        verifiedRuleIds.addAll(ALL_SYSTEM_RULE_IDS)
        saveVerifiedRulesToPrefs()
    }

    /**
     * Export all custom rules and overrides as a clean JSON backup string.
     */
    fun exportBackupJson(): String {
        val root = JSONObject()
        val overridesObj = JSONObject()
        directionOverrides.forEach { (id, dir) -> overridesObj.put(id, dir.name) }
        root.put("overrides", overridesObj)

        val rulesArr = JSONArray()
        synchronized(lock) {
            for (rule in customRules) {
                val obj = JSONObject().apply {
                    put("id", rule.id)
                    put("title", rule.title)
                    put("min5m", rule.min5m)
                    put("max5m", rule.max5m)
                    put("min60m", rule.min60m)
                    put("max60m", rule.max60m)
                    put("direction", rule.direction.name)
                    put("isActive", rule.isActive)
                    put("createdAt", rule.createdAt)
                }
                rulesArr.put(obj)
            }
        }
        root.put("customRules", rulesArr)

        val verifiedArr = JSONArray()
        verifiedRuleIds.forEach { verifiedArr.put(it) }
        root.put("verifiedRules", verifiedArr)

        return root.toString(2)
    }

data class BackupImportResult(
    val success: Boolean,
    val overridesCount: Int = 0,
    val customRulesCount: Int = 0,
    val verifiedCount: Int = 0,
    val message: String = ""
)

    /**
     * Import custom rules and overrides from a JSON backup string with detailed feedback.
     */
    fun importBackupDetailed(jsonString: String): BackupImportResult {
        if (jsonString.isBlank()) {
            return BackupImportResult(false, message = "Empty backup data.")
        }
        return try {
            var clean = jsonString.trim()
            if (clean.startsWith("```json")) {
                clean = clean.removePrefix("```json")
            } else if (clean.startsWith("```")) {
                clean = clean.removePrefix("```")
            }
            if (clean.endsWith("```")) {
                clean = clean.removeSuffix("```")
            }
            clean = clean.trim()

            var overridesCount = 0
            var customRulesCount = 0
            var verifiedCount = 0

            // Support direct array of custom rules: [{"id":"C001",...}]
            if (clean.startsWith("[")) {
                val rulesArr = JSONArray(clean)
                for (i in 0 until rulesArr.length()) {
                    val obj = rulesArr.optJSONObject(i) ?: continue
                    val dirStr = obj.optString("direction", "UP")
                    val dir = try { TradeDirection.valueOf(dirStr) } catch (_: Exception) { TradeDirection.UP }
                    val rawId = obj.optString("id", "")
                    val id = canonicalizeRuleId(rawId)
                    if (id.isNotBlank()) {
                        val min5 = obj.optDouble("min5m", 0.0)
                        val max5 = obj.optDouble("max5m", 0.0)
                        val min60 = obj.optDouble("min60m", 0.0)
                        val max60 = obj.optDouble("max60m", 0.0)
                        val rule = CustomRule(
                            id = id,
                            title = obj.optString("title", "Custom Rule $id"),
                            min5m = minOf(min5, max5),
                            max5m = maxOf(min5, max5),
                            min60m = minOf(min60, max60),
                            max60m = maxOf(min60, max60),
                            direction = dir,
                            isActive = obj.optBoolean("isActive", true),
                            createdAt = obj.optLong("createdAt", System.currentTimeMillis())
                        )
                        synchronized(lock) {
                            customRules.removeAll { it.id.equals(rule.id, ignoreCase = true) }
                            customRules.add(rule)
                        }
                        setRuleVerified(rule.id, true)
                        customRulesCount++
                    }
                }
                saveCustomRulesToPrefs()
            } else {
                val root = JSONObject(clean)

                val overridesObj = root.optJSONObject("overrides")
                if (overridesObj != null) {
                    val keys = overridesObj.keys()
                    while (keys.hasNext()) {
                        val rawK = keys.next()
                        val k = canonicalizeRuleId(rawK)
                        val v = overridesObj.optString(rawK)
                        val dir = try { TradeDirection.valueOf(v) } catch (_: Exception) { null }
                        if (dir != null && k.isNotBlank()) {
                            directionOverrides[k] = dir
                            setRuleVerified(k, true)
                            overridesCount++
                        }
                    }
                    saveOverridesToPrefs()
                }

                val rulesArr = root.optJSONArray("customRules")
                if (rulesArr != null) {
                    for (i in 0 until rulesArr.length()) {
                        val obj = rulesArr.optJSONObject(i) ?: continue
                        val dirStr = obj.optString("direction", "UP")
                        val dir = try { TradeDirection.valueOf(dirStr) } catch (_: Exception) { TradeDirection.UP }
                        val rawId = obj.optString("id", "")
                        val id = canonicalizeRuleId(rawId)
                        if (id.isNotBlank()) {
                            val min5 = obj.optDouble("min5m", 0.0)
                            val max5 = obj.optDouble("max5m", 0.0)
                            val min60 = obj.optDouble("min60m", 0.0)
                            val max60 = obj.optDouble("max60m", 0.0)
                            val rule = CustomRule(
                                id = id,
                                title = obj.optString("title", "Custom Rule $id"),
                                min5m = minOf(min5, max5),
                                max5m = maxOf(min5, max5),
                                min60m = minOf(min60, max60),
                                max60m = maxOf(min60, max60),
                                direction = dir,
                                isActive = obj.optBoolean("isActive", true),
                                createdAt = obj.optLong("createdAt", System.currentTimeMillis())
                            )
                            synchronized(lock) {
                                customRules.removeAll { it.id.equals(rule.id, ignoreCase = true) }
                                customRules.add(rule)
                            }
                            setRuleVerified(rule.id, true)
                            customRulesCount++
                        }
                    }
                    saveCustomRulesToPrefs()
                }

                val verifiedArr = root.optJSONArray("verifiedRules")
                if (verifiedArr != null) {
                    for (i in 0 until verifiedArr.length()) {
                        val id = canonicalizeRuleId(verifiedArr.optString(i, ""))
                        if (id.isNotBlank()) {
                            verifiedRuleIds.add(id)
                            unverifiedRuleIds.remove(id)
                            verifiedCount++
                        }
                    }
                    saveVerifiedRulesToPrefs()
                }
            }

            if (overridesCount == 0 && customRulesCount == 0 && verifiedCount == 0) {
                BackupImportResult(false, message = "No valid rules found in the backup.")
            } else {
                BackupImportResult(
                    success = true,
                    overridesCount = overridesCount,
                    customRulesCount = customRulesCount,
                    verifiedCount = verifiedCount,
                    message = "Restored: $customRulesCount custom rules, $overridesCount overrides, $verifiedCount verified rules."
                )
            }
        } catch (e: Exception) {
            BackupImportResult(false, message = "JSON Error: ${e.localizedMessage ?: "Invalid structure"}")
        }
    }

    /**
     * Import custom rules and overrides from a JSON backup string.
     * Returns true if successful, false otherwise.
     */
    fun importBackupJson(jsonString: String): Boolean {
        return importBackupDetailed(jsonString).success
    }
}
