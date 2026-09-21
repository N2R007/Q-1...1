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

    // Complete System Rule Catalog (U001-U103, D001-D103, M001-M106)
    val ALL_SYSTEM_RULE_IDS: Set<String> by lazy {
        val set = LinkedHashSet<String>()
        for (i in 1..103) {
            set.add("U%03d".format(i))
            set.add("D%03d".format(i))
        }
        for (i in 1..106) {
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

            val hasInitializedV3 = prefs.getBoolean(KEY_INIT_VERIFIED_ALL_V3, false)
            if (!hasInitializedV3) {
                // Pre-verify ALL system rules (U001-U103, D001-D103, M001-M106) for 100% auto-trade readiness
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
                    .apply()
            } else {
                val verifiedSet = prefs.getStringSet(KEY_VERIFIED_RULES, null)
                if (verifiedSet != null) {
                    verifiedRuleIds.addAll(verifiedSet)
                } else {
                    verifiedRuleIds.addAll(ALL_SYSTEM_RULE_IDS)
                    verifiedRuleIds.removeAll(unverifiedRuleIds)
                }
            }
        } catch (_: Exception) {}
    }

    private fun saveVerifiedRulesToPrefs() {
        try {
            val prefs = preferences ?: return
            prefs.edit()
                .putStringSet(KEY_VERIFIED_RULES, HashSet(verifiedRuleIds))
                .putStringSet(KEY_UNVERIFIED_RULES, HashSet(unverifiedRuleIds))
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
     * All system rules (U/D/M) and active custom rules (C) are verified by default
     * for 100% Auto-Trade execution, unless explicitly unverified by the user.
     */
    fun isRuleVerified(ruleId: String): Boolean {
        if (ruleId.isBlank()) return false
        val cleanId = canonicalizeRuleId(ruleId)
        if (unverifiedRuleIds.contains(cleanId)) return false
        if (verifiedRuleIds.contains(cleanId)) return true
        // Default: Any valid directional/matrix rule (U, D, M), custom rule (C), or dynamic momentum (DYN) is eligible for 100% Auto-Trade
        return cleanId.startsWith("U") || cleanId.startsWith("D") || cleanId.startsWith("M") || cleanId.startsWith("C") || cleanId.startsWith("DYN")
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
        all.addAll(ALL_SYSTEM_RULE_IDS)
        all.removeAll(unverifiedRuleIds)
        return all
    }

    fun clearAllVerifiedRules() {
        verifiedRuleIds.clear()
        unverifiedRuleIds.addAll(ALL_SYSTEM_RULE_IDS)
        saveVerifiedRulesToPrefs()
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
