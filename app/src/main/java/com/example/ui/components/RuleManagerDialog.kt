package com.example.ui.components

import android.content.Intent
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.border
import androidx.compose.foundation.horizontalScroll
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.lazy.itemsIndexed
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.ArrowDownward
import androidx.compose.material.icons.filled.ArrowUpward
import androidx.compose.material.icons.filled.Check
import androidx.compose.material.icons.filled.Close
import androidx.compose.material.icons.filled.ContentCopy
import androidx.compose.material.icons.filled.Delete
import androidx.compose.material.icons.filled.Download
import androidx.compose.material.icons.filled.Edit
import androidx.compose.material.icons.filled.Refresh
import androidx.compose.material.icons.filled.Search
import androidx.compose.material.icons.filled.Share
import androidx.compose.material.icons.filled.Tune
import androidx.compose.material.icons.filled.Upload
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.OutlinedTextFieldDefaults
import androidx.compose.material3.Surface
import androidx.compose.material3.Switch
import androidx.compose.material3.SwitchDefaults
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.hapticfeedback.HapticFeedbackType
import androidx.compose.ui.platform.LocalClipboardManager
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalHapticFeedback
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.AnnotatedString
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.window.DialogProperties
import com.example.data.matrix.CustomRule
import com.example.data.matrix.UserRuleRegistry
import com.example.data.models.TradeDirection
import com.example.ui.theme.AccentAmber
import com.example.ui.theme.AccentCyan
import com.example.ui.theme.BorderStrokeLight
import com.example.ui.theme.DarkBackground
import com.example.ui.theme.DarkCard
import com.example.ui.theme.DarkSurface
import com.example.ui.theme.DarkSurfaceVariant
import com.example.ui.theme.NeonGreen
import com.example.ui.theme.NeonGreenDim
import com.example.ui.theme.NeonGreenLight
import com.example.ui.theme.NeonRed
import com.example.ui.theme.NeonRedDim
import com.example.ui.theme.NeonRedLight
import com.example.ui.theme.TextMuted
import com.example.ui.theme.TextPrimary
import com.example.ui.theme.TextSecondary
import java.util.Locale

/**
 * Robust percentage parser handling +, -, unicode minus, comma decimal separators, and % symbols.
 */
private fun parsePercentageInput(raw: String): Double? {
    val clean = raw.trim()
        .replace("%", "")
        .replace(",", ".")
        .replace("−", "-")
        .replace("+", "")
        .trim()
    return clean.toDoubleOrNull()
}

@Composable
fun RuleManagerDialog(
    initialMatrixId: String? = null,
    live5m: Double? = null,
    live60m: Double? = null,
    onDismiss: () -> Unit
) {
    // 0 = Rule Direction Override, 1 = Create New Custom Rule
    var selectedTab by remember { mutableIntStateOf(0) }

    // Version counter to trigger recomposition when rules change
    var refreshTick by remember { mutableIntStateOf(0) }

    // Overrides state
    val cleanInitialId = UserRuleRegistry.canonicalizeRuleId(initialMatrixId)
    var editRuleId by remember { mutableStateOf(cleanInitialId.ifEmpty { "U001" }) }
    var selectedOverrideDirection by remember { mutableStateOf(TradeDirection.DOWN) }
    var overrideStatusMessage by remember { mutableStateOf<String?>(null) }

    LaunchedEffect(initialMatrixId) {
        val id = UserRuleRegistry.canonicalizeRuleId(initialMatrixId)
        if (id.isNotBlank()) {
            editRuleId = id
        }
    }

    // Custom Rule Form State
    var customRuleId by remember { mutableStateOf(UserRuleRegistry.getNextCustomRuleId()) }
    var customRuleTitle by remember { mutableStateOf("") }
    var customMin5m by remember { mutableStateOf(String.format(Locale.US, "%.2f", (live5m ?: 0.10) - 0.05)) }
    var customMax5m by remember { mutableStateOf(String.format(Locale.US, "%.2f", (live5m ?: 0.10) + 0.05)) }
    var customMin60m by remember { mutableStateOf(String.format(Locale.US, "%.2f", (live60m ?: -0.20) - 0.05)) }
    var customMax60m by remember { mutableStateOf(String.format(Locale.US, "%.2f", (live60m ?: -0.20) + 0.05)) }
    var customDirection by remember { mutableStateOf(TradeDirection.UP) }
    var customStatusMessage by remember { mutableStateOf<String?>(null) }

    var showResetConfirm by remember { mutableStateOf(false) }

    val clipboardManager = LocalClipboardManager.current
    val context = LocalContext.current
    var backupStatusMessage by remember { mutableStateOf<String?>(null) }
    var importBackupText by remember { mutableStateOf("") }

    val currentOverrides = remember(refreshTick) { UserRuleRegistry.getAllOverrides() }
    val currentCustomRules = remember(refreshTick) { UserRuleRegistry.getCustomRules() }
    val currentVerifiedRules = remember(refreshTick) { UserRuleRegistry.getVerifiedRuleIds() }
    val currentBackupJson = remember(refreshTick) { UserRuleRegistry.exportBackupJson() }

    val hapticFeedback = LocalHapticFeedback.current
    var verificationFilterCategory by remember { mutableStateOf("All") }
    var verificationSearchQuery by remember { mutableStateOf("") }
    var verificationStatusMessage by remember { mutableStateOf<String?>(null) }

    val allVerificationRules = remember(refreshTick) {
        UserRuleRegistry.getAllMatrixRulesForVerification()
    }

    var draftVerifiedMap by remember(refreshTick) {
        val initial = mutableMapOf<String, Boolean>()
        allVerificationRules.forEach { rule ->
            initial[rule.id] = UserRuleRegistry.isRuleVerified(rule.id)
        }
        mutableStateOf(initial)
    }

    AlertDialog(
        onDismissRequest = onDismiss,
        properties = DialogProperties(usePlatformDefaultWidth = false),
        modifier = Modifier
            .fillMaxWidth(0.96f)
            .padding(vertical = 16.dp),
        confirmButton = {},
        dismissButton = {},
        text = {
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .background(DarkBackground, RoundedCornerShape(16.dp))
                    .padding(14.dp),
                verticalArrangement = Arrangement.spacedBy(12.dp)
            ) {
                // Header
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Row(
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.spacedBy(6.dp)
                    ) {
                        Icon(
                            imageVector = Icons.Default.Tune,
                            contentDescription = "Rule Manager",
                            tint = AccentCyan,
                            modifier = Modifier.size(20.dp)
                        )
                        Text(
                            text = "Rule Editor & Custom Rules",
                            fontSize = 15.sp,
                            fontWeight = FontWeight.Bold,
                            color = Color.White
                        )
                    }
                    IconButton(
                        onClick = onDismiss,
                        modifier = Modifier.size(28.dp)
                    ) {
                        Icon(
                            imageVector = Icons.Default.Close,
                            contentDescription = "Close",
                            tint = TextMuted
                        )
                    }
                }

                // Tab Switcher
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(4.dp)
                ) {
                    Surface(
                        onClick = { selectedTab = 0 },
                        modifier = Modifier.weight(1f),
                        shape = RoundedCornerShape(8.dp),
                        color = if (selectedTab == 0) AccentCyan else DarkSurfaceVariant,
                        border = BorderStroke(1.dp, if (selectedTab == 0) AccentCyan else BorderStrokeLight)
                    ) {
                        Text(
                            text = "1. Edit (${currentOverrides.size})",
                            fontSize = 8.5.sp,
                            fontWeight = FontWeight.Bold,
                            color = if (selectedTab == 0) DarkBackground else TextPrimary,
                            textAlign = TextAlign.Center,
                            modifier = Modifier.padding(vertical = 7.dp)
                        )
                    }

                    Surface(
                        onClick = { selectedTab = 1 },
                        modifier = Modifier.weight(1f),
                        shape = RoundedCornerShape(8.dp),
                        color = if (selectedTab == 1) NeonGreen else DarkSurfaceVariant,
                        border = BorderStroke(1.dp, if (selectedTab == 1) NeonGreen else BorderStrokeLight)
                    ) {
                        Text(
                            text = "2. New (${currentCustomRules.size})",
                            fontSize = 8.5.sp,
                            fontWeight = FontWeight.Bold,
                            color = if (selectedTab == 1) DarkBackground else TextPrimary,
                            textAlign = TextAlign.Center,
                            modifier = Modifier.padding(vertical = 7.dp)
                        )
                    }

                    Surface(
                        onClick = { selectedTab = 2 },
                        modifier = Modifier.weight(1f),
                        shape = RoundedCornerShape(8.dp),
                        color = if (selectedTab == 2) AccentAmber else DarkSurfaceVariant,
                        border = BorderStroke(1.dp, if (selectedTab == 2) AccentAmber else BorderStrokeLight)
                    ) {
                        Text(
                            text = "3. Backup 💾",
                            fontSize = 8.5.sp,
                            fontWeight = FontWeight.Bold,
                            color = if (selectedTab == 2) DarkBackground else TextPrimary,
                            textAlign = TextAlign.Center,
                            modifier = Modifier.padding(vertical = 7.dp)
                        )
                    }

                    Surface(
                        onClick = { selectedTab = 3 },
                        modifier = Modifier.weight(1.15f),
                        shape = RoundedCornerShape(8.dp),
                        color = if (selectedTab == 3) NeonGreen else DarkSurfaceVariant,
                        border = BorderStroke(1.dp, if (selectedTab == 3) NeonGreen else BorderStrokeLight)
                    ) {
                        Text(
                            text = "4. Verify Matrix ✓",
                            fontSize = 8.5.sp,
                            fontWeight = FontWeight.Bold,
                            color = if (selectedTab == 3) DarkBackground else NeonGreen,
                            textAlign = TextAlign.Center,
                            modifier = Modifier.padding(vertical = 7.dp)
                        )
                    }
                }

                // TAB 1: DIRECTION OVERRIDE
                if (selectedTab == 0) {
                    Column(
                        modifier = Modifier.fillMaxWidth(),
                        verticalArrangement = Arrangement.spacedBy(10.dp)
                    ) {
                        Text(
                            text = "Override signal direction for any rule (e.g. U001, D061):",
                            fontSize = 11.sp,
                            color = TextSecondary
                        )

                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.spacedBy(8.dp),
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            OutlinedTextField(
                                value = editRuleId,
                                onValueChange = { editRuleId = it.uppercase() },
                                label = { Text("Rule ID (e.g. U001, D061)", fontSize = 11.sp) },
                                modifier = Modifier.weight(1.2f),
                                singleLine = true,
                                shape = RoundedCornerShape(8.dp),
                                colors = OutlinedTextFieldDefaults.colors(
                                    focusedTextColor = TextPrimary,
                                    unfocusedTextColor = TextPrimary,
                                    focusedBorderColor = AccentCyan,
                                    unfocusedBorderColor = BorderStrokeLight
                                )
                            )

                            val canonicalInitial = UserRuleRegistry.canonicalizeRuleId(initialMatrixId)
                            if (canonicalInitial.isNotBlank()) {
                                Surface(
                                    onClick = {
                                        editRuleId = canonicalInitial
                                    },
                                    shape = RoundedCornerShape(6.dp),
                                    color = DarkCard,
                                    border = BorderStroke(1.dp, BorderStrokeLight)
                                ) {
                                    Text(
                                        text = "Insert Current Rule",
                                        fontSize = 10.sp,
                                        color = AccentCyan,
                                        modifier = Modifier.padding(horizontal = 8.dp, vertical = 6.dp)
                                    )
                                }
                            }
                        }

                        val cleanId = UserRuleRegistry.canonicalizeRuleId(editRuleId)
                        LaunchedEffect(cleanId) {
                            if (cleanId.isNotBlank()) {
                                val currentOv = UserRuleRegistry.getRuleOverride(cleanId)
                                if (currentOv != null) {
                                    selectedOverrideDirection = currentOv
                                } else if (cleanId.startsWith("U")) {
                                    selectedOverrideDirection = TradeDirection.UP
                                } else if (cleanId.startsWith("D")) {
                                    selectedOverrideDirection = TradeDirection.DOWN
                                }
                            }
                        }
                        val isCurrentRuleVerified = if (cleanId.isNotBlank()) {
                            refreshTick.let { }
                            UserRuleRegistry.isRuleVerified(cleanId)
                        } else false

                        // Auto-Trade Active Status Box
                        Surface(
                            shape = RoundedCornerShape(6.dp),
                            color = DarkSurfaceVariant,
                            border = BorderStroke(1.dp, if (isCurrentRuleVerified) NeonGreen.copy(alpha = 0.6f) else BorderStrokeLight),
                            modifier = Modifier.fillMaxWidth()
                        ) {
                            Row(
                                modifier = Modifier.padding(horizontal = 8.dp, vertical = 6.dp),
                                horizontalArrangement = Arrangement.Center,
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                Text(
                                    text = "✓ অটো ট্রেড এক্টিভ",
                                    fontSize = 11.sp,
                                    fontWeight = FontWeight.Bold,
                                    color = NeonGreenLight
                                )
                            }
                        }

                        // Target Direction Buttons
                        Text(
                            text = "Trigger signal when this rule matches:",
                            fontSize = 11.sp,
                            fontWeight = FontWeight.SemiBold,
                            color = TextPrimary
                        )

                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.spacedBy(8.dp)
                        ) {
                            Surface(
                                onClick = { selectedOverrideDirection = TradeDirection.UP },
                                modifier = Modifier.weight(1f),
                                shape = RoundedCornerShape(8.dp),
                                color = if (selectedOverrideDirection == TradeDirection.UP) NeonGreen else NeonGreenDim,
                                border = BorderStroke(1.dp, if (selectedOverrideDirection == TradeDirection.UP) NeonGreenLight else BorderStrokeLight)
                            ) {
                                Row(
                                    modifier = Modifier.padding(vertical = 8.dp),
                                    horizontalArrangement = Arrangement.Center,
                                    verticalAlignment = Alignment.CenterVertically
                                ) {
                                    Icon(
                                        imageVector = Icons.Default.ArrowUpward,
                                        contentDescription = "UP",
                                        tint = if (selectedOverrideDirection == TradeDirection.UP) Color.White else NeonGreenLight,
                                        modifier = Modifier.size(16.dp)
                                    )
                                    Spacer(modifier = Modifier.width(4.dp))
                                    Text(
                                        text = "🟢 UP",
                                        fontSize = 12.sp,
                                        fontWeight = FontWeight.Bold,
                                        color = if (selectedOverrideDirection == TradeDirection.UP) Color.White else NeonGreenLight
                                    )
                                }
                            }

                            Surface(
                                onClick = { selectedOverrideDirection = TradeDirection.DOWN },
                                modifier = Modifier.weight(1f),
                                shape = RoundedCornerShape(8.dp),
                                color = if (selectedOverrideDirection == TradeDirection.DOWN) NeonRed else NeonRedDim,
                                border = BorderStroke(1.dp, if (selectedOverrideDirection == TradeDirection.DOWN) NeonRedLight else BorderStrokeLight)
                            ) {
                                Row(
                                    modifier = Modifier.padding(vertical = 8.dp),
                                    horizontalArrangement = Arrangement.Center,
                                    verticalAlignment = Alignment.CenterVertically
                                ) {
                                    Icon(
                                        imageVector = Icons.Default.ArrowDownward,
                                        contentDescription = "DOWN",
                                        tint = if (selectedOverrideDirection == TradeDirection.DOWN) Color.White else NeonRedLight,
                                        modifier = Modifier.size(16.dp)
                                    )
                                    Spacer(modifier = Modifier.width(4.dp))
                                    Text(
                                        text = "🔴 DOWN",
                                        fontSize = 12.sp,
                                        fontWeight = FontWeight.Bold,
                                        color = if (selectedOverrideDirection == TradeDirection.DOWN) Color.White else NeonRedLight
                                    )
                                }
                            }
                        }

                        // Save Direction and Verify Buttons
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.spacedBy(8.dp)
                        ) {
                            Surface(
                                onClick = {
                                    if (cleanId.isNotBlank()) {
                                        UserRuleRegistry.setRuleOverride(cleanId, selectedOverrideDirection)
                                        UserRuleRegistry.setRuleVerified(cleanId, true)
                                        refreshTick++
                                        overrideStatusMessage = "✅ Rule $cleanId saved as ${selectedOverrideDirection.name} & Verified (✓)!"
                                    }
                                },
                                shape = RoundedCornerShape(8.dp),
                                color = AccentCyan,
                                modifier = Modifier.weight(1.2f)
                            ) {
                                Text(
                                    text = "💾 Save Direction",
                                    fontSize = 12.sp,
                                    fontWeight = FontWeight.Bold,
                                    color = DarkBackground,
                                    textAlign = TextAlign.Center,
                                    modifier = Modifier.padding(vertical = 9.dp)
                                )
                            }

                            Surface(
                                onClick = {
                                    if (cleanId.isNotBlank()) {
                                        val nowVerified = UserRuleRegistry.toggleVerifiedRule(cleanId)
                                        refreshTick++
                                        overrideStatusMessage = if (nowVerified) "✅ Rule $cleanId marked as Verified (✓)!" else "ℹ️ Rule $cleanId unverified."
                                    }
                                },
                                shape = RoundedCornerShape(8.dp),
                                color = if (isCurrentRuleVerified) NeonGreen else DarkSurfaceVariant,
                                border = BorderStroke(1.dp, if (isCurrentRuleVerified) NeonGreenLight else BorderStrokeLight),
                                modifier = Modifier.weight(0.9f)
                            ) {
                                Row(
                                    modifier = Modifier.padding(vertical = 9.dp),
                                    horizontalArrangement = Arrangement.Center,
                                    verticalAlignment = Alignment.CenterVertically
                                ) {
                                    Icon(
                                        imageVector = Icons.Default.Check,
                                        contentDescription = "Verify",
                                        tint = if (isCurrentRuleVerified) DarkBackground else TextSecondary,
                                        modifier = Modifier.size(13.dp)
                                    )
                                    Spacer(modifier = Modifier.width(3.dp))
                                    Text(
                                        text = if (isCurrentRuleVerified) "Verified ✓" else "Verify",
                                        fontSize = 11.sp,
                                        fontWeight = FontWeight.Bold,
                                        color = if (isCurrentRuleVerified) DarkBackground else TextPrimary
                                    )
                                }
                            }
                        }

                        if (overrideStatusMessage != null) {
                            Text(
                                text = overrideStatusMessage ?: "",
                                fontSize = 11.sp,
                                color = NeonGreenLight,
                                fontWeight = FontWeight.Medium
                            )
                        }

                        // Existing Overrides List
                        if (currentOverrides.isNotEmpty()) {
                            Spacer(modifier = Modifier.height(4.dp))
                            Text(
                                text = "Edited Rules List (${currentOverrides.size}):",
                                fontSize = 11.sp,
                                fontWeight = FontWeight.Bold,
                                color = TextMuted
                            )

                            LazyColumn(
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .height(130.dp),
                                verticalArrangement = Arrangement.spacedBy(4.dp)
                            ) {
                                items(currentOverrides.toList()) { (ruleId, dir) ->
                                    Card(
                                        shape = RoundedCornerShape(6.dp),
                                        colors = CardDefaults.cardColors(containerColor = DarkCard),
                                        border = BorderStroke(0.5.dp, BorderStrokeLight),
                                        modifier = Modifier
                                            .fillMaxWidth()
                                            .clickable {
                                                editRuleId = ruleId
                                                selectedOverrideDirection = dir
                                            }
                                    ) {
                                        Row(
                                            modifier = Modifier
                                                .fillMaxWidth()
                                                .padding(horizontal = 8.dp, vertical = 4.dp),
                                            horizontalArrangement = Arrangement.SpaceBetween,
                                            verticalAlignment = Alignment.CenterVertically
                                        ) {
                                            Row(
                                                verticalAlignment = Alignment.CenterVertically,
                                                horizontalArrangement = Arrangement.spacedBy(6.dp)
                                            ) {
                                                Text(
                                                    text = "[$ruleId]",
                                                    fontSize = 11.sp,
                                                    fontWeight = FontWeight.Bold,
                                                    fontFamily = FontFamily.Monospace,
                                                    color = AccentCyan
                                                )
                                                Text(
                                                    text = "➔ ${dir.name}",
                                                    fontSize = 11.sp,
                                                    fontWeight = FontWeight.Black,
                                                    color = if (dir == TradeDirection.UP) NeonGreenLight else NeonRedLight
                                                )
                                                if (UserRuleRegistry.isRuleVerified(ruleId)) {
                                                    Text(
                                                        text = "✓",
                                                        fontSize = 11.sp,
                                                        fontWeight = FontWeight.Black,
                                                        color = NeonGreenLight
                                                    )
                                                }
                                            }

                                            IconButton(
                                                onClick = {
                                                    UserRuleRegistry.removeRuleOverride(ruleId)
                                                    refreshTick++
                                                },
                                                modifier = Modifier.size(24.dp)
                                            ) {
                                                Icon(
                                                    imageVector = Icons.Default.Close,
                                                    contentDescription = "Revert",
                                                    tint = TextMuted,
                                                    modifier = Modifier.size(14.dp)
                                                )
                                            }
                                        }
                                    }
                                }
                            }
                        }
                    }
                }

                // TAB 2: CREATE CUSTOM RULE
                if (selectedTab == 1) {
                    Column(
                        modifier = Modifier.fillMaxWidth(),
                        verticalArrangement = Arrangement.spacedBy(8.dp)
                    ) {
                        // Live Screen Reference
                        Surface(
                            shape = RoundedCornerShape(8.dp),
                            color = DarkCard,
                            border = BorderStroke(1.dp, BorderStrokeLight),
                            modifier = Modifier.fillMaxWidth()
                        ) {
                            Row(
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .padding(8.dp),
                                horizontalArrangement = Arrangement.SpaceBetween,
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                Column {
                                    Text(
                                        text = "Current Screen Values:",
                                        fontSize = 10.sp,
                                        color = TextMuted
                                    )
                                    Text(
                                        text = "5m: ${if (live5m != null) String.format(Locale.US, "%+.2f%%", live5m) else "--"} | 60m: ${if (live60m != null) String.format(Locale.US, "%+.2f%%", live60m) else "--"}",
                                        fontSize = 11.sp,
                                        fontWeight = FontWeight.Bold,
                                        color = AccentCyan,
                                        fontFamily = FontFamily.Monospace
                                    )
                                }

                                Surface(
                                    onClick = {
                                        val v5 = live5m ?: 0.0
                                        val v60 = live60m ?: 0.0
                                        customMin5m = String.format(Locale.US, "%.2f", v5 - 0.05)
                                        customMax5m = String.format(Locale.US, "%.2f", v5 + 0.05)
                                        customMin60m = String.format(Locale.US, "%.2f", v60 - 0.05)
                                        customMax60m = String.format(Locale.US, "%.2f", v60 + 0.05)
                                        customDirection = if (v5 >= 0) TradeDirection.UP else TradeDirection.DOWN
                                        if (customRuleId.isBlank() || customRuleId.startsWith("C001")) {
                                            val nextMatrixNum = 165 + currentCustomRules.count { it.id.startsWith("M") } + 1
                                            customRuleId = "M%03d".format(nextMatrixNum)
                                        }
                                        if (customRuleTitle.isBlank()) {
                                            customRuleTitle = "Matrix Range ${customRuleId.ifBlank { "M" }}"
                                        }
                                    },
                                    shape = RoundedCornerShape(6.dp),
                                    color = DarkSurfaceVariant,
                                    border = BorderStroke(1.dp, AccentCyan.copy(alpha = 0.5f))
                                ) {
                                    Text(
                                        text = "⚡ Auto-Fill Range",
                                        fontSize = 10.sp,
                                        fontWeight = FontWeight.Bold,
                                        color = AccentCyan,
                                        modifier = Modifier.padding(horizontal = 6.dp, vertical = 4.dp)
                                    )
                                }
                            }
                        }

                        // Form Inputs
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.spacedBy(6.dp)
                        ) {
                            OutlinedTextField(
                                value = customRuleId,
                                onValueChange = { customRuleId = it.uppercase().trim() },
                                label = { Text("Rule ID", fontSize = 10.sp) },
                                modifier = Modifier.weight(1f),
                                singleLine = true,
                                shape = RoundedCornerShape(8.dp),
                                colors = OutlinedTextFieldDefaults.colors(
                                    focusedTextColor = TextPrimary,
                                    unfocusedTextColor = TextPrimary,
                                    focusedBorderColor = NeonGreenLight,
                                    unfocusedBorderColor = BorderStrokeLight
                                )
                            )

                            OutlinedTextField(
                                value = customRuleTitle,
                                onValueChange = { customRuleTitle = it },
                                label = { Text("Name / Strategy", fontSize = 10.sp) },
                                modifier = Modifier.weight(1.6f),
                                singleLine = true,
                                shape = RoundedCornerShape(8.dp),
                                colors = OutlinedTextFieldDefaults.colors(
                                    focusedTextColor = TextPrimary,
                                    unfocusedTextColor = TextPrimary,
                                    focusedBorderColor = NeonGreenLight,
                                    unfocusedBorderColor = BorderStrokeLight
                                )
                            )
                        }

                        // 5m Range Inputs
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.spacedBy(6.dp),
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Text("5m Range:", fontSize = 10.sp, color = TextMuted, modifier = Modifier.width(55.dp))
                            OutlinedTextField(
                                value = customMin5m,
                                onValueChange = { customMin5m = it },
                                label = { Text("Min %", fontSize = 9.sp) },
                                modifier = Modifier.weight(1f),
                                singleLine = true,
                                shape = RoundedCornerShape(8.dp),
                                colors = OutlinedTextFieldDefaults.colors(
                                    focusedTextColor = TextPrimary,
                                    unfocusedTextColor = TextPrimary
                                )
                            )
                            Text("to", fontSize = 10.sp, color = TextMuted)
                            OutlinedTextField(
                                value = customMax5m,
                                onValueChange = { customMax5m = it },
                                label = { Text("Max %", fontSize = 9.sp) },
                                modifier = Modifier.weight(1f),
                                singleLine = true,
                                shape = RoundedCornerShape(8.dp),
                                colors = OutlinedTextFieldDefaults.colors(
                                    focusedTextColor = TextPrimary,
                                    unfocusedTextColor = TextPrimary
                                )
                            )
                        }

                        // 60m Range Inputs
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.spacedBy(6.dp),
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Text("60m Range:", fontSize = 10.sp, color = TextMuted, modifier = Modifier.width(55.dp))
                            OutlinedTextField(
                                value = customMin60m,
                                onValueChange = { customMin60m = it },
                                label = { Text("Min %", fontSize = 9.sp) },
                                modifier = Modifier.weight(1f),
                                singleLine = true,
                                shape = RoundedCornerShape(8.dp),
                                colors = OutlinedTextFieldDefaults.colors(
                                    focusedTextColor = TextPrimary,
                                    unfocusedTextColor = TextPrimary
                                )
                            )
                            Text("to", fontSize = 10.sp, color = TextMuted)
                            OutlinedTextField(
                                value = customMax60m,
                                onValueChange = { customMax60m = it },
                                label = { Text("Max %", fontSize = 9.sp) },
                                modifier = Modifier.weight(1f),
                                singleLine = true,
                                shape = RoundedCornerShape(8.dp),
                                colors = OutlinedTextFieldDefaults.colors(
                                    focusedTextColor = TextPrimary,
                                    unfocusedTextColor = TextPrimary
                                )
                            )
                        }

                        // Direction Selection
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.spacedBy(6.dp)
                        ) {
                            Surface(
                                onClick = { customDirection = TradeDirection.UP },
                                modifier = Modifier.weight(1f),
                                shape = RoundedCornerShape(8.dp),
                                color = if (customDirection == TradeDirection.UP) NeonGreen else NeonGreenDim,
                                border = BorderStroke(1.dp, if (customDirection == TradeDirection.UP) NeonGreenLight else BorderStrokeLight)
                            ) {
                                Text(
                                    text = "🟢 UP Signal",
                                    fontSize = 11.sp,
                                    fontWeight = FontWeight.Bold,
                                    color = if (customDirection == TradeDirection.UP) Color.White else NeonGreenLight,
                                    textAlign = TextAlign.Center,
                                    modifier = Modifier.padding(vertical = 7.dp)
                                )
                            }

                            Surface(
                                onClick = { customDirection = TradeDirection.DOWN },
                                modifier = Modifier.weight(1f),
                                shape = RoundedCornerShape(8.dp),
                                color = if (customDirection == TradeDirection.DOWN) NeonRed else NeonRedDim,
                                border = BorderStroke(1.dp, if (customDirection == TradeDirection.DOWN) NeonRedLight else BorderStrokeLight)
                            ) {
                                Text(
                                    text = "🔴 DOWN Signal",
                                    fontSize = 11.sp,
                                    fontWeight = FontWeight.Bold,
                                    color = if (customDirection == TradeDirection.DOWN) Color.White else NeonRedLight,
                                    textAlign = TextAlign.Center,
                                    modifier = Modifier.padding(vertical = 7.dp)
                                )
                            }
                        }

                        // Save Custom Rule & Verify Button Row
                        val isExistingRule = currentCustomRules.any { it.id.equals(customRuleId.trim(), ignoreCase = true) }
                        val cleanCustomId = customRuleId.uppercase().trim()
                        val isCustomRuleVerified = cleanCustomId.isNotBlank() && UserRuleRegistry.isRuleVerified(cleanCustomId)

                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.spacedBy(8.dp),
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Surface(
                                onClick = {
                                    val min5 = parsePercentageInput(customMin5m)
                                    val max5 = parsePercentageInput(customMax5m)
                                    val min60 = parsePercentageInput(customMin60m)
                                    val max60 = parsePercentageInput(customMax60m)

                                    if (min5 != null && max5 != null && min60 != null && max60 != null) {
                                        val low5 = minOf(min5, max5)
                                        val high5 = maxOf(min5, max5)
                                        val low60 = minOf(min60, max60)
                                        val high60 = maxOf(min60, max60)
                                        val targetId = UserRuleRegistry.canonicalizeRuleId(
                                            if (customRuleId.isBlank()) {
                                                val nextMatrixNum = 165 + currentCustomRules.count { it.id.startsWith("M") } + 1
                                                "M%03d".format(nextMatrixNum)
                                            } else customRuleId
                                        ).let {
                                            if (it.isBlank()) {
                                                val nextMatrixNum = 165 + currentCustomRules.count { r -> r.id.startsWith("M") } + 1
                                                "M%03d".format(nextMatrixNum)
                                            } else it
                                        }

                                        val newRule = CustomRule(
                                            id = targetId,
                                            title = customRuleTitle.ifBlank { "Custom Matrix $targetId" },
                                            min5m = low5,
                                            max5m = high5,
                                            min60m = low60,
                                            max60m = high60,
                                            direction = customDirection,
                                            isActive = true
                                        )
                                        UserRuleRegistry.addOrUpdateCustomRule(newRule)
                                        UserRuleRegistry.setRuleVerified(newRule.id, true)
                                        val nextMatrixNum = 165 + currentCustomRules.count { it.id.startsWith("M") } + 2
                                        customRuleId = "M%03d".format(nextMatrixNum)
                                        customRuleTitle = ""
                                        refreshTick++
                                        customStatusMessage = "✅ ম্যাট্রিক্স ${newRule.id} সফলভাবে সেভ ও অ্যাক্টিভ করা হয়েছে! Verify Matrix সেকশনের নিচে সর্বশেষ সিরিয়াল নাম্বার হিসেবে যুক্ত হয়েছে।"
                                    } else {
                                        customStatusMessage = "⚠️ Please enter valid percentage numbers (e.g. 0.10, -0.20)."
                                    }
                                },
                                shape = RoundedCornerShape(8.dp),
                                color = NeonGreen,
                                modifier = Modifier.weight(1.3f)
                            ) {
                                Text(
                                    text = if (isExistingRule) "💾 Update $customRuleId" else "💾 Save & Activate Rule",
                                    fontSize = 11.5.sp,
                                    fontWeight = FontWeight.Bold,
                                    color = DarkBackground,
                                    textAlign = TextAlign.Center,
                                    modifier = Modifier.padding(vertical = 8.dp)
                                )
                            }

                            if (isExistingRule) {
                                Surface(
                                    onClick = {
                                        customRuleId = UserRuleRegistry.getNextCustomRuleId()
                                        customRuleTitle = ""
                                        val v5 = live5m ?: 0.10
                                        val v60 = live60m ?: -0.20
                                        customMin5m = String.format(Locale.US, "%.2f", v5 - 0.05)
                                        customMax5m = String.format(Locale.US, "%.2f", v5 + 0.05)
                                        customMin60m = String.format(Locale.US, "%.2f", v60 - 0.05)
                                        customMax60m = String.format(Locale.US, "%.2f", v60 + 0.05)
                                        customDirection = if (v5 >= 0) TradeDirection.UP else TradeDirection.DOWN
                                        customStatusMessage = "Switched to creating a fresh new rule."
                                    },
                                    shape = RoundedCornerShape(8.dp),
                                    color = DarkCard,
                                    border = BorderStroke(1.dp, BorderStrokeLight)
                                ) {
                                    Text(
                                        text = "➕ New",
                                        fontSize = 11.sp,
                                        fontWeight = FontWeight.Bold,
                                        color = AccentCyan,
                                        modifier = Modifier.padding(horizontal = 8.dp, vertical = 8.dp)
                                    )
                                }
                            }

                            // Dedicated Verify Button with Checkmark
                            Surface(
                                onClick = {
                                    if (cleanCustomId.isNotBlank()) {
                                        val nowVerified = UserRuleRegistry.toggleVerifiedRule(cleanCustomId)
                                        refreshTick++
                                        customStatusMessage = if (nowVerified) "✅ Rule $cleanCustomId marked as Verified (✓)!" else "ℹ️ Rule $cleanCustomId unverified."
                                    }
                                },
                                shape = RoundedCornerShape(8.dp),
                                color = if (isCustomRuleVerified) NeonGreen else DarkSurfaceVariant,
                                border = BorderStroke(1.dp, if (isCustomRuleVerified) NeonGreenLight else BorderStrokeLight),
                                modifier = Modifier.weight(0.9f)
                            ) {
                                Row(
                                    modifier = Modifier.padding(vertical = 8.dp),
                                    horizontalArrangement = Arrangement.Center,
                                    verticalAlignment = Alignment.CenterVertically
                                ) {
                                    Icon(
                                        imageVector = Icons.Default.Check,
                                        contentDescription = "Verify Custom Rule",
                                        tint = if (isCustomRuleVerified) DarkBackground else TextSecondary,
                                        modifier = Modifier.size(13.dp)
                                    )
                                    Spacer(modifier = Modifier.width(3.dp))
                                    Text(
                                        text = if (isCustomRuleVerified) "Verified ✓" else "Verify",
                                        fontSize = 11.sp,
                                        fontWeight = FontWeight.Bold,
                                        color = if (isCustomRuleVerified) DarkBackground else TextPrimary
                                    )
                                }
                            }
                        }

                        if (customStatusMessage != null) {
                            Column(
                                modifier = Modifier.fillMaxWidth(),
                                verticalArrangement = Arrangement.spacedBy(4.dp)
                            ) {
                                Text(
                                    text = customStatusMessage ?: "",
                                    fontSize = 11.sp,
                                    color = NeonGreenLight,
                                    fontWeight = FontWeight.Medium
                                )
                                Surface(
                                    onClick = {
                                        selectedTab = 3 // Switch directly to Tab 4 (Verify Matrix)
                                    },
                                    shape = RoundedCornerShape(6.dp),
                                    color = DarkSurfaceVariant,
                                    border = BorderStroke(0.8.dp, NeonGreen)
                                ) {
                                    Row(
                                        modifier = Modifier.padding(horizontal = 8.dp, vertical = 5.dp),
                                        verticalAlignment = Alignment.CenterVertically,
                                        horizontalArrangement = Arrangement.spacedBy(4.dp)
                                    ) {
                                        Icon(
                                            imageVector = Icons.Default.Check,
                                            contentDescription = "Go to Verify Matrix",
                                            tint = NeonGreen,
                                            modifier = Modifier.size(13.dp)
                                        )
                                        Text(
                                            text = "👉 Verify Matrix তালিকায় নিচে দেখুন (Go to Verify Matrix)",
                                            fontSize = 10.5.sp,
                                            fontWeight = FontWeight.Bold,
                                            color = NeonGreen
                                        )
                                    }
                                }
                            }
                        }

                        // Saved Custom Rules List
                        if (currentCustomRules.isNotEmpty()) {
                            Text(
                                text = "Your Custom Rules (${currentCustomRules.size}):",
                                fontSize = 11.sp,
                                fontWeight = FontWeight.Bold,
                                color = TextMuted
                            )

                            LazyColumn(
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .height(130.dp),
                                verticalArrangement = Arrangement.spacedBy(4.dp)
                            ) {
                                items(currentCustomRules) { rule ->
                                    Card(
                                        shape = RoundedCornerShape(6.dp),
                                        colors = CardDefaults.cardColors(containerColor = DarkCard),
                                        border = BorderStroke(0.5.dp, if (rule.isActive) NeonGreen.copy(alpha = 0.5f) else BorderStrokeLight),
                                        modifier = Modifier
                                            .fillMaxWidth()
                                            .clickable {
                                                customRuleId = rule.id
                                                customRuleTitle = rule.title
                                                customMin5m = rule.min5m.toString()
                                                customMax5m = rule.max5m.toString()
                                                customMin60m = rule.min60m.toString()
                                                customMax60m = rule.max60m.toString()
                                                customDirection = rule.direction
                                                customStatusMessage = "Editing rule ${rule.id}..."
                                            }
                                    ) {
                                        Row(
                                            modifier = Modifier
                                                .fillMaxWidth()
                                                .padding(horizontal = 8.dp, vertical = 5.dp),
                                            horizontalArrangement = Arrangement.SpaceBetween,
                                            verticalAlignment = Alignment.CenterVertically
                                        ) {
                                             Column(modifier = Modifier.weight(1f)) {
                                                Row(
                                                    verticalAlignment = Alignment.CenterVertically,
                                                    horizontalArrangement = Arrangement.spacedBy(6.dp)
                                                ) {
                                                    Text(
                                                        text = "[${rule.id}]",
                                                        fontSize = 11.sp,
                                                        fontWeight = FontWeight.Bold,
                                                        fontFamily = FontFamily.Monospace,
                                                        color = if (rule.direction == TradeDirection.UP) NeonGreenLight else NeonRedLight
                                                    )
                                                    Text(
                                                        text = rule.direction.name,
                                                        fontSize = 11.sp,
                                                        fontWeight = FontWeight.Black,
                                                        color = if (rule.direction == TradeDirection.UP) NeonGreenLight else NeonRedLight
                                                    )
                                                    Text(
                                                        text = rule.title,
                                                        fontSize = 10.sp,
                                                        color = TextPrimary,
                                                        maxLines = 1
                                                    )
                                                    if (UserRuleRegistry.isRuleVerified(rule.id)) {
                                                        Text(
                                                            text = "✓",
                                                            fontSize = 12.sp,
                                                            fontWeight = FontWeight.Black,
                                                            color = NeonGreenLight
                                                        )
                                                    }
                                                }
                                                Text(
                                                    text = "5m: [${rule.min5m}..${rule.max5m}] | 60m: [${rule.min60m}..${rule.max60m}]",
                                                    fontSize = 9.sp,
                                                    color = TextMuted,
                                                    fontFamily = FontFamily.Monospace
                                                )
                                            }

                                            Row(
                                                verticalAlignment = Alignment.CenterVertically,
                                                horizontalArrangement = Arrangement.spacedBy(2.dp)
                                            ) {
                                                val isVerified = UserRuleRegistry.isRuleVerified(rule.id)
                                                IconButton(
                                                    onClick = {
                                                        UserRuleRegistry.toggleVerifiedRule(rule.id)
                                                        refreshTick++
                                                    },
                                                    modifier = Modifier.size(26.dp)
                                                ) {
                                                    Icon(
                                                        imageVector = Icons.Default.Check,
                                                        contentDescription = "Toggle Verification",
                                                        tint = if (isVerified) NeonGreenLight else TextMuted.copy(alpha = 0.5f),
                                                        modifier = Modifier.size(16.dp)
                                                    )
                                                }
                                                Switch(
                                                    checked = rule.isActive,
                                                    onCheckedChange = { active ->
                                                        UserRuleRegistry.toggleCustomRule(rule.id, active)
                                                        refreshTick++
                                                    },
                                                    modifier = Modifier.size(36.dp),
                                                    colors = SwitchDefaults.colors(
                                                        checkedThumbColor = NeonGreen,
                                                        checkedTrackColor = NeonGreenDim
                                                    )
                                                )
                                                IconButton(
                                                    onClick = {
                                                        customRuleId = rule.id
                                                        customRuleTitle = rule.title
                                                        customMin5m = rule.min5m.toString()
                                                        customMax5m = rule.max5m.toString()
                                                        customMin60m = rule.min60m.toString()
                                                        customMax60m = rule.max60m.toString()
                                                        customDirection = rule.direction
                                                        customStatusMessage = "Editing rule ${rule.id}..."
                                                    },
                                                    modifier = Modifier.size(24.dp)
                                                ) {
                                                    Icon(
                                                        imageVector = Icons.Default.Edit,
                                                        contentDescription = "Edit Rule",
                                                        tint = AccentCyan,
                                                        modifier = Modifier.size(15.dp)
                                                    )
                                                }
                                                IconButton(
                                                    onClick = {
                                                        UserRuleRegistry.deleteCustomRule(rule.id)
                                                        refreshTick++
                                                    },
                                                    modifier = Modifier.size(24.dp)
                                                ) {
                                                    Icon(
                                                        imageVector = Icons.Default.Delete,
                                                        contentDescription = "Delete",
                                                        tint = NeonRedLight,
                                                        modifier = Modifier.size(15.dp)
                                                    )
                                                }
                                            }
                                        }
                                    }
                                }
                            }
                        }
                    }
                }

                // TAB 3: BACKUP & RESTORE
                if (selectedTab == 2) {
                    Column(
                        modifier = Modifier.fillMaxWidth(),
                        verticalArrangement = Arrangement.spacedBy(10.dp)
                    ) {
                        Card(
                            shape = RoundedCornerShape(8.dp),
                            colors = CardDefaults.cardColors(containerColor = DarkSurface),
                            border = BorderStroke(1.dp, AccentAmber.copy(alpha = 0.5f)),
                            modifier = Modifier.fillMaxWidth()
                        ) {
                            Column(
                                modifier = Modifier.padding(10.dp),
                                verticalArrangement = Arrangement.spacedBy(6.dp)
                            ) {
                                Row(
                                    verticalAlignment = Alignment.CenterVertically,
                                    horizontalArrangement = Arrangement.spacedBy(6.dp)
                                ) {
                                    Icon(
                                        imageVector = Icons.Default.Download,
                                        contentDescription = "Backup",
                                        tint = AccentAmber,
                                        modifier = Modifier.size(18.dp)
                                    )
                                    Text(
                                        text = "Rules Backup & Restore Center",
                                        fontSize = 13.sp,
                                        fontWeight = FontWeight.Bold,
                                        color = AccentAmber
                                    )
                                }
                                Text(
                                    text = "আপনার সেভ করা কাস্টম রুল ও এডিট অন্য ডিভাইসে নিতে বা সুরক্ষিত রাখতে ব্যাকআপ কোডটি সেভ করে রাখুন।",
                                    fontSize = 11.sp,
                                    color = TextSecondary,
                                    lineHeight = 15.sp
                                )

                                // Real-time Statistics Badges
                                Row(
                                    modifier = Modifier.fillMaxWidth(),
                                    horizontalArrangement = Arrangement.spacedBy(6.dp)
                                ) {
                                    Surface(
                                        shape = RoundedCornerShape(4.dp),
                                        color = DarkBackground,
                                        border = BorderStroke(0.5.dp, BorderStrokeLight),
                                        modifier = Modifier.weight(1f)
                                    ) {
                                        Text(
                                            text = "✏️ Edits: ${currentOverrides.size}",
                                            fontSize = 10.sp,
                                            fontWeight = FontWeight.SemiBold,
                                            color = AccentCyan,
                                            textAlign = TextAlign.Center,
                                            modifier = Modifier.padding(vertical = 4.dp)
                                        )
                                    }
                                    Surface(
                                        shape = RoundedCornerShape(4.dp),
                                        color = DarkBackground,
                                        border = BorderStroke(0.5.dp, BorderStrokeLight),
                                        modifier = Modifier.weight(1f)
                                    ) {
                                        Text(
                                            text = "⚡ Custom: ${currentCustomRules.size}",
                                            fontSize = 10.sp,
                                            fontWeight = FontWeight.SemiBold,
                                            color = NeonGreenLight,
                                            textAlign = TextAlign.Center,
                                            modifier = Modifier.padding(vertical = 4.dp)
                                        )
                                    }
                                    Surface(
                                        shape = RoundedCornerShape(4.dp),
                                        color = DarkBackground,
                                        border = BorderStroke(0.5.dp, BorderStrokeLight),
                                        modifier = Modifier.weight(1f)
                                    ) {
                                        Text(
                                            text = "✓ Verified: ${currentVerifiedRules.size}",
                                            fontSize = 10.sp,
                                            fontWeight = FontWeight.SemiBold,
                                            color = AccentAmber,
                                            textAlign = TextAlign.Center,
                                            modifier = Modifier.padding(vertical = 4.dp)
                                        )
                                    }
                                }
                            }
                        }

                        // 1. Export Buttons Row (Copy & Share)
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.spacedBy(8.dp)
                        ) {
                            Surface(
                                onClick = {
                                    clipboardManager.setText(AnnotatedString(currentBackupJson))
                                    backupStatusMessage = "✅ ব্যাকআপ JSON কোড ক্লিপবোর্ডে কপি করা হয়েছে!"
                                },
                                shape = RoundedCornerShape(8.dp),
                                color = AccentAmber,
                                modifier = Modifier.weight(1f)
                            ) {
                                Row(
                                    modifier = Modifier.padding(vertical = 9.dp, horizontal = 6.dp),
                                    verticalAlignment = Alignment.CenterVertically,
                                    horizontalArrangement = Arrangement.Center
                                ) {
                                    Icon(
                                        imageVector = Icons.Default.ContentCopy,
                                        contentDescription = "Copy",
                                        tint = DarkBackground,
                                        modifier = Modifier.size(15.dp)
                                    )
                                    Spacer(modifier = Modifier.width(4.dp))
                                    Text(
                                        text = "Copy Backup",
                                        fontSize = 11.sp,
                                        fontWeight = FontWeight.Bold,
                                        color = DarkBackground
                                    )
                                }
                            }

                            Surface(
                                onClick = {
                                    try {
                                        val sendIntent = Intent().apply {
                                            action = Intent.ACTION_SEND
                                            putExtra(Intent.EXTRA_TEXT, currentBackupJson)
                                            putExtra(Intent.EXTRA_SUBJECT, "Quant Vision AI - Rules Backup")
                                            type = "text/plain"
                                        }
                                        val shareIntent = Intent.createChooser(sendIntent, "Share Rules Backup")
                                        context.startActivity(shareIntent)
                                        backupStatusMessage = "📤 শেয়ার উইন্ডো ওপেন হয়েছে!"
                                    } catch (_: Exception) {
                                        clipboardManager.setText(AnnotatedString(currentBackupJson))
                                        backupStatusMessage = "✅ ব্যাকআপ কোড ক্লিপবোর্ডে কপি হয়েছে!"
                                    }
                                },
                                shape = RoundedCornerShape(8.dp),
                                color = AccentCyan,
                                modifier = Modifier.weight(1f)
                            ) {
                                Row(
                                    modifier = Modifier.padding(vertical = 9.dp, horizontal = 6.dp),
                                    verticalAlignment = Alignment.CenterVertically,
                                    horizontalArrangement = Arrangement.Center
                                ) {
                                    Icon(
                                        imageVector = Icons.Default.Share,
                                        contentDescription = "Share",
                                        tint = DarkBackground,
                                        modifier = Modifier.size(15.dp)
                                    )
                                    Spacer(modifier = Modifier.width(4.dp))
                                    Text(
                                        text = "Share / Save",
                                        fontSize = 11.sp,
                                        fontWeight = FontWeight.Bold,
                                        color = DarkBackground
                                    )
                                }
                            }
                        }

                        // 2. Visible Live Backup Code Box
                        Column(
                            modifier = Modifier.fillMaxWidth(),
                            verticalArrangement = Arrangement.spacedBy(4.dp)
                        ) {
                            Row(
                                modifier = Modifier.fillMaxWidth(),
                                horizontalArrangement = Arrangement.SpaceBetween,
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                Text(
                                    text = "CURRENT BACKUP CODE (লাইভ ব্যাকআপ ডাটা):",
                                    fontSize = 10.sp,
                                    fontWeight = FontWeight.Bold,
                                    color = TextSecondary
                                )
                                Text(
                                    text = "${currentBackupJson.length} chars",
                                    fontSize = 9.sp,
                                    color = TextMuted
                                )
                            }

                            Surface(
                                shape = RoundedCornerShape(6.dp),
                                color = DarkBackground,
                                border = BorderStroke(0.5.dp, BorderStrokeLight),
                                modifier = Modifier.fillMaxWidth()
                            ) {
                                Box(
                                    modifier = Modifier
                                        .fillMaxWidth()
                                        .height(72.dp)
                                        .padding(8.dp)
                                ) {
                                    Text(
                                        text = currentBackupJson,
                                        fontSize = 9.5.sp,
                                        fontFamily = FontFamily.Monospace,
                                        color = TextPrimary.copy(alpha = 0.85f),
                                        lineHeight = 13.sp,
                                        maxLines = 4
                                    )
                                }
                            }
                        }

                        // 3. Restore Section
                        Column(
                            modifier = Modifier.fillMaxWidth(),
                            verticalArrangement = Arrangement.spacedBy(6.dp)
                        ) {
                            Text(
                                text = "RESTORE / IMPORT (ব্যাকআপ থেকে ফেরত আনা):",
                                fontSize = 10.sp,
                                fontWeight = FontWeight.Bold,
                                color = TextSecondary
                            )

                            Surface(
                                onClick = {
                                    val clipboardText = clipboardManager.getText()?.text
                                    if (!clipboardText.isNullOrBlank()) {
                                        val result = UserRuleRegistry.importBackupDetailed(clipboardText)
                                        if (result.success) {
                                            refreshTick++
                                            backupStatusMessage = "✅ সফলভাবে রিস্টোর হয়েছে: ${result.customRulesCount} কাস্টম রুল, ${result.overridesCount} ওভাররাইড, ${result.verifiedCount} ভেরিফাইড রুল!"
                                        } else {
                                            backupStatusMessage = "❌ ক্লিপবোর্ডের ডাটা সঠিক নয়: ${result.message}"
                                        }
                                    } else {
                                        backupStatusMessage = "❌ ক্লিপবোর্ডে কোনো ডাটা নেই! কোড কপি করে আবার চেষ্টা করুন।"
                                    }
                                },
                                shape = RoundedCornerShape(8.dp),
                                color = NeonGreen,
                                modifier = Modifier.fillMaxWidth()
                            ) {
                                Row(
                                    modifier = Modifier.padding(vertical = 9.dp, horizontal = 8.dp),
                                    verticalAlignment = Alignment.CenterVertically,
                                    horizontalArrangement = Arrangement.Center
                                ) {
                                    Icon(
                                        imageVector = Icons.Default.Upload,
                                        contentDescription = "Restore",
                                        tint = DarkBackground,
                                        modifier = Modifier.size(16.dp)
                                    )
                                    Spacer(modifier = Modifier.width(6.dp))
                                    Text(
                                        text = "Restore from Clipboard (ক্লিপবোর্ড থেকে রিস্টোর)",
                                        fontSize = 11.sp,
                                        fontWeight = FontWeight.Bold,
                                        color = DarkBackground
                                    )
                                }
                            }

                            // Manual Paste & Restore Section
                            OutlinedTextField(
                                value = importBackupText,
                                onValueChange = { importBackupText = it },
                                label = { Text("অথবা এখানে ব্যাকআপ JSON কোড পেস্ট করুন", fontSize = 10.sp) },
                                placeholder = { Text("Paste JSON code here...", fontSize = 10.sp, color = TextMuted) },
                                modifier = Modifier.fillMaxWidth().height(80.dp),
                                shape = RoundedCornerShape(8.dp),
                                textStyle = androidx.compose.ui.text.TextStyle(fontSize = 10.sp, fontFamily = FontFamily.Monospace, color = Color.White),
                                colors = OutlinedTextFieldDefaults.colors(
                                    focusedBorderColor = AccentCyan,
                                    unfocusedBorderColor = BorderStrokeLight,
                                    focusedLabelColor = AccentCyan,
                                    unfocusedLabelColor = TextSecondary,
                                    cursorColor = AccentCyan
                                )
                            )

                            if (importBackupText.isNotBlank()) {
                                Surface(
                                    onClick = {
                                        val result = UserRuleRegistry.importBackupDetailed(importBackupText)
                                        if (result.success) {
                                            refreshTick++
                                            importBackupText = ""
                                            backupStatusMessage = "✅ সফলভাবে রিস্টোর হয়েছে: ${result.customRulesCount} কাস্টম রুল, ${result.overridesCount} ওভাররাইড, ${result.verifiedCount} ভেরিফাইড রুল!"
                                        } else {
                                            backupStatusMessage = "❌ ভুল ফরম্যাট: ${result.message}"
                                        }
                                    },
                                    shape = RoundedCornerShape(8.dp),
                                    color = AccentCyan,
                                    modifier = Modifier.fillMaxWidth()
                                ) {
                                    Text(
                                        text = "Apply Restore from Pasted Code",
                                        fontSize = 11.sp,
                                        fontWeight = FontWeight.Bold,
                                        color = DarkBackground,
                                        textAlign = TextAlign.Center,
                                        modifier = Modifier.padding(vertical = 8.dp)
                                    )
                                }
                            }
                        }

                        // Status message
                        if (backupStatusMessage != null) {
                            Text(
                                text = backupStatusMessage ?: "",
                                fontSize = 11.sp,
                                color = if (backupStatusMessage?.startsWith("✅") == true) NeonGreenLight else NeonRedLight,
                                fontWeight = FontWeight.SemiBold,
                                textAlign = TextAlign.Center,
                                modifier = Modifier.fillMaxWidth().padding(top = 4.dp)
                            )
                        }
                    }
                }

                // Global Reset Button
                Spacer(modifier = Modifier.height(2.dp))
                if (!showResetConfirm) {
                    TextButton(
                        onClick = { showResetConfirm = true },
                        modifier = Modifier.fillMaxWidth()
                    ) {
                        Icon(
                            imageVector = Icons.Default.Refresh,
                            contentDescription = "Reset All",
                            tint = NeonRedLight,
                            modifier = Modifier.size(14.dp)
                        )
                        Spacer(modifier = Modifier.width(4.dp))
                        Text(
                            text = "Reset all custom modifications to defaults",
                            fontSize = 10.sp,
                            color = NeonRedLight,
                            fontWeight = FontWeight.Medium
                        )
                    }
                } else {
                    Card(
                        shape = RoundedCornerShape(8.dp),
                        colors = CardDefaults.cardColors(containerColor = NeonRedDim),
                        border = BorderStroke(1.dp, NeonRed.copy(alpha = 0.6f)),
                        modifier = Modifier.fillMaxWidth()
                    ) {
                        Column(
                            modifier = Modifier.padding(8.dp),
                            horizontalAlignment = Alignment.CenterHorizontally,
                            verticalArrangement = Arrangement.spacedBy(6.dp)
                        ) {
                            Text(
                                text = "Are you sure you want to reset all rule edits and restore factory defaults?",
                                fontSize = 11.sp,
                                color = TextPrimary,
                                textAlign = TextAlign.Center
                            )
                            Row(
                                horizontalArrangement = Arrangement.spacedBy(10.dp)
                            ) {
                                Surface(
                                    onClick = {
                                        UserRuleRegistry.resetAll()
                                        refreshTick++
                                        showResetConfirm = false
                                        overrideStatusMessage = "Reset to factory defaults successfully!"
                                        customStatusMessage = null
                                    },
                                    shape = RoundedCornerShape(6.dp),
                                    color = NeonRed
                                ) {
                                    Text(
                                        text = "Yes, Reset All",
                                        fontSize = 11.sp,
                                        fontWeight = FontWeight.Bold,
                                        color = Color.White,
                                        modifier = Modifier.padding(horizontal = 10.dp, vertical = 5.dp)
                                    )
                                }
                                Surface(
                                    onClick = { showResetConfirm = false },
                                    shape = RoundedCornerShape(6.dp),
                                    color = DarkCard
                                ) {
                                    Text(
                                        text = "Cancel",
                                        fontSize = 11.sp,
                                        color = TextSecondary,
                                        modifier = Modifier.padding(horizontal = 10.dp, vertical = 5.dp)
                                    )
                                }
                            }
                        }
                    }
                }

                // TAB 4: RULE VERIFICATION MATRIX LIST
                if (selectedTab == 3) {
                    val filteredRules = remember(allVerificationRules, verificationFilterCategory, verificationSearchQuery) {
                        allVerificationRules.filter { item ->
                            val matchesCategory = when (verificationFilterCategory) {
                                "D-Rules" -> item.id.startsWith("D")
                                "U-Rules" -> item.id.startsWith("U")
                                "M-Matrix" -> item.id.startsWith("M")
                                "Custom" -> item.isCustom || item.id.startsWith("C")
                                else -> true // "All"
                            }
                            val query = verificationSearchQuery.trim().uppercase()
                            val matchesQuery = query.isEmpty() ||
                                item.id.uppercase().contains(query) ||
                                item.serial.toString() == query ||
                                item.title.uppercase().contains(query) ||
                                item.category.uppercase().contains(query)
                            matchesCategory && matchesQuery
                        }
                    }

                    val totalVerifiedCount = allVerificationRules.count { draftVerifiedMap[it.id] == true }

                    Column(
                        modifier = Modifier.fillMaxWidth(),
                        verticalArrangement = Arrangement.spacedBy(8.dp)
                    ) {
                        // Header & Description Card
                        Surface(
                            shape = RoundedCornerShape(8.dp),
                            color = DarkCard,
                            border = BorderStroke(0.5.dp, BorderStrokeLight),
                            modifier = Modifier.fillMaxWidth()
                        ) {
                            Column(modifier = Modifier.padding(8.dp), verticalArrangement = Arrangement.spacedBy(4.dp)) {
                                Row(
                                    verticalAlignment = Alignment.CenterVertically,
                                    horizontalArrangement = Arrangement.spacedBy(6.dp)
                                ) {
                                    Icon(
                                        imageVector = Icons.Default.Check,
                                        contentDescription = "Verify",
                                        tint = NeonGreen,
                                        modifier = Modifier.size(14.dp)
                                    )
                                    Text(
                                        text = "ম্যাট্রিক্স রুল ভেরিফিকেশন প্যানেল (Rule Verification Matrix)",
                                        fontSize = 11.sp,
                                        fontWeight = FontWeight.Bold,
                                        color = Color.White
                                    )
                                }
                                Text(
                                    text = "১ থেকে শুরু করে সব ম্যাট্রিক্স সাজানো রয়েছে। টিকচিহ্ন [✓] যুক্ত রুলগুলো ১০০% ভেরিফাইড এবং ক্যামেরা ডিটেকশনে সাথে সাথে অটো-ট্রেড ফায়ার করবে।",
                                    fontSize = 9.sp,
                                    color = TextSecondary,
                                    lineHeight = 12.sp
                                )
                            }
                        }

                        // Category Filter Chips
                        val categories = listOf(
                            "All" to "All (${allVerificationRules.size})",
                            "M-Matrix" to "M-Matrix (${allVerificationRules.count { it.id.startsWith("M") }})",
                            "D-Rules" to "D001-D165 (ডাউন)",
                            "U-Rules" to "U001-U103 (আপ)",
                            "Custom" to "Custom (${currentCustomRules.size})"
                        )
                        val chipScrollState = rememberScrollState()
                        Row(
                            modifier = Modifier
                                .fillMaxWidth()
                                .horizontalScroll(chipScrollState),
                            horizontalArrangement = Arrangement.spacedBy(6.dp)
                        ) {
                            categories.forEach { (key, label) ->
                                val isSelected = verificationFilterCategory == key
                                Surface(
                                    onClick = { verificationFilterCategory = key },
                                    shape = RoundedCornerShape(16.dp),
                                    color = if (isSelected) NeonGreen.copy(alpha = 0.2f) else DarkSurfaceVariant,
                                    border = BorderStroke(
                                        width = if (isSelected) 1.dp else 0.5.dp,
                                        color = if (isSelected) NeonGreen else BorderStrokeLight
                                    )
                                ) {
                                    Text(
                                        text = label,
                                        fontSize = 9.sp,
                                        fontWeight = if (isSelected) FontWeight.Bold else FontWeight.Normal,
                                        color = if (isSelected) NeonGreen else TextSecondary,
                                        modifier = Modifier.padding(horizontal = 8.dp, vertical = 4.dp)
                                    )
                                }
                            }
                        }

                        // Search Bar
                        OutlinedTextField(
                            value = verificationSearchQuery,
                            onValueChange = { verificationSearchQuery = it },
                            placeholder = { Text("রুল নং বা নাম সার্চ করুন (যেমন: D001, M042, 15)...", fontSize = 10.sp, color = TextMuted) },
                            leadingIcon = {
                                Icon(
                                    imageVector = Icons.Default.Search,
                                    contentDescription = "Search",
                                    tint = TextMuted,
                                    modifier = Modifier.size(16.dp)
                                )
                            },
                            trailingIcon = {
                                if (verificationSearchQuery.isNotEmpty()) {
                                    IconButton(
                                        onClick = { verificationSearchQuery = "" },
                                        modifier = Modifier.size(24.dp)
                                    ) {
                                        Icon(
                                            imageVector = Icons.Default.Close,
                                            contentDescription = "Clear search",
                                            tint = TextMuted,
                                            modifier = Modifier.size(14.dp)
                                        )
                                    }
                                }
                            },
                            singleLine = true,
                            colors = OutlinedTextFieldDefaults.colors(
                                focusedBorderColor = AccentCyan,
                                unfocusedBorderColor = BorderStrokeLight,
                                focusedTextColor = TextPrimary,
                                unfocusedTextColor = TextPrimary,
                                focusedContainerColor = DarkSurfaceVariant,
                                unfocusedContainerColor = DarkSurfaceVariant
                            ),
                            shape = RoundedCornerShape(8.dp),
                            modifier = Modifier
                                .fillMaxWidth()
                                .height(46.dp)
                        )

                        // Action Controls Bar: Select All, Deselect All, and Live Counter
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.SpaceBetween,
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Row(horizontalArrangement = Arrangement.spacedBy(6.dp)) {
                                // Select All Button
                                Surface(
                                    onClick = {
                                        val updated = draftVerifiedMap.toMutableMap()
                                        filteredRules.forEach { updated[it.id] = true }
                                        draftVerifiedMap = updated
                                        verificationStatusMessage = "বর্তমান ফিল্টারের সব রুল সিলেক্ট করা হয়েছে [✓]"
                                    },
                                    shape = RoundedCornerShape(6.dp),
                                    color = DarkSurfaceVariant,
                                    border = BorderStroke(0.5.dp, NeonGreen.copy(alpha = 0.5f))
                                ) {
                                    Row(
                                        modifier = Modifier.padding(horizontal = 7.dp, vertical = 4.dp),
                                        verticalAlignment = Alignment.CenterVertically,
                                        horizontalArrangement = Arrangement.spacedBy(4.dp)
                                    ) {
                                        Icon(
                                            imageVector = Icons.Default.Check,
                                            contentDescription = "Select All",
                                            tint = NeonGreen,
                                            modifier = Modifier.size(12.dp)
                                        )
                                        Text(
                                            text = "Select All (সব সিলেক্ট)",
                                            fontSize = 9.sp,
                                            fontWeight = FontWeight.Bold,
                                            color = NeonGreen
                                        )
                                    }
                                }

                                // Deselect All Button
                                Surface(
                                    onClick = {
                                        val updated = draftVerifiedMap.toMutableMap()
                                        filteredRules.forEach { updated[it.id] = false }
                                        draftVerifiedMap = updated
                                        verificationStatusMessage = "বর্তমান ফিল্টারের সব রুল থেকে টিক সরানো হয়েছে"
                                    },
                                    shape = RoundedCornerShape(6.dp),
                                    color = DarkSurfaceVariant,
                                    border = BorderStroke(0.5.dp, NeonRed.copy(alpha = 0.5f))
                                ) {
                                    Row(
                                        modifier = Modifier.padding(horizontal = 7.dp, vertical = 4.dp),
                                        verticalAlignment = Alignment.CenterVertically,
                                        horizontalArrangement = Arrangement.spacedBy(4.dp)
                                    ) {
                                        Icon(
                                            imageVector = Icons.Default.Close,
                                            contentDescription = "Deselect All",
                                            tint = NeonRed,
                                            modifier = Modifier.size(12.dp)
                                        )
                                        Text(
                                            text = "Deselect All (সব মুছুন)",
                                            fontSize = 9.sp,
                                            fontWeight = FontWeight.Bold,
                                            color = NeonRed
                                        )
                                    }
                                }
                            }

                            // Live Counter Badge
                            Surface(
                                shape = RoundedCornerShape(12.dp),
                                color = NeonGreen.copy(alpha = 0.15f),
                                border = BorderStroke(0.5.dp, NeonGreen.copy(alpha = 0.4f))
                            ) {
                                Text(
                                    text = "Verified: $totalVerifiedCount",
                                    fontSize = 9.5.sp,
                                    fontWeight = FontWeight.Bold,
                                    color = NeonGreen,
                                    modifier = Modifier.padding(horizontal = 8.dp, vertical = 3.5.dp)
                                )
                            }
                        }

                        // Scrollable Rules List
                        LazyColumn(
                            modifier = Modifier
                                .fillMaxWidth()
                                .heightIn(max = 340.dp),
                            verticalArrangement = Arrangement.spacedBy(4.dp)
                        ) {
                            itemsIndexed(
                                items = filteredRules,
                                key = { _, item -> item.id }
                            ) { _, rule ->
                                val effDir = currentOverrides[rule.id] ?: rule.defaultDirection
                                val isOverridden = currentOverrides.containsKey(rule.id)
                                val isChecked = draftVerifiedMap[rule.id] == true

                                Surface(
                                    onClick = {
                                        val updated = draftVerifiedMap.toMutableMap()
                                        updated[rule.id] = !isChecked
                                        draftVerifiedMap = updated
                                        verificationStatusMessage = null
                                    },
                                    shape = RoundedCornerShape(6.dp),
                                    color = if (isChecked) DarkSurfaceVariant else DarkCard,
                                    border = BorderStroke(
                                        width = if (isChecked) 0.8.dp else 0.5.dp,
                                        color = if (isChecked) NeonGreen.copy(alpha = 0.6f) else BorderStrokeLight
                                    ),
                                    modifier = Modifier.fillMaxWidth()
                                ) {
                                    Row(
                                        modifier = Modifier
                                            .fillMaxWidth()
                                            .padding(horizontal = 8.dp, vertical = 6.dp),
                                        verticalAlignment = Alignment.CenterVertically,
                                        horizontalArrangement = Arrangement.SpaceBetween
                                    ) {
                                        // Left Side: Serial Number, Rule ID, Direction, and Title
                                        Row(
                                            verticalAlignment = Alignment.CenterVertically,
                                            horizontalArrangement = Arrangement.spacedBy(8.dp),
                                            modifier = Modifier.weight(1f)
                                        ) {
                                            // Serial Number pill [1], [2], ...
                                            Surface(
                                                shape = RoundedCornerShape(4.dp),
                                                color = DarkBackground,
                                                border = BorderStroke(0.5.dp, BorderStrokeLight)
                                            ) {
                                                Text(
                                                    text = if (verificationFilterCategory == "All") "[#${rule.globalSerial}]" else "[${rule.serial}]",
                                                    fontSize = 9.sp,
                                                    fontWeight = FontWeight.Bold,
                                                    fontFamily = FontFamily.Monospace,
                                                    color = AccentAmber,
                                                    modifier = Modifier.padding(horizontal = 4.dp, vertical = 2.dp)
                                                )
                                            }

                                            // Rule ID
                                            Text(
                                                text = rule.id,
                                                fontSize = 11.sp,
                                                fontWeight = FontWeight.Bold,
                                                fontFamily = FontFamily.Monospace,
                                                color = Color.White
                                            )

                                            // Direction Badge (Shows effective direction with override applied!)
                                            val badgeColor = when (effDir) {
                                                TradeDirection.UP -> NeonGreen
                                                TradeDirection.DOWN -> NeonRed
                                                else -> TextMuted
                                            }
                                            Surface(
                                                shape = RoundedCornerShape(3.dp),
                                                color = badgeColor.copy(alpha = 0.2f),
                                                border = BorderStroke(0.5.dp, badgeColor.copy(alpha = 0.6f))
                                            ) {
                                                Row(
                                                    verticalAlignment = Alignment.CenterVertically,
                                                    modifier = Modifier.padding(horizontal = 4.dp, vertical = 1.5.dp)
                                                ) {
                                                    Text(
                                                        text = effDir.name,
                                                        fontSize = 8.5.sp,
                                                        fontWeight = FontWeight.ExtraBold,
                                                        color = badgeColor
                                                    )
                                                    if (isOverridden) {
                                                        Text(
                                                            text = "*",
                                                            fontSize = 8.5.sp,
                                                            fontWeight = FontWeight.Bold,
                                                            color = AccentCyan
                                                        )
                                                    }
                                                }
                                            }

                                            // Rule Output Title
                                            Text(
                                                text = rule.title.replace("_", " "),
                                                fontSize = 9.sp,
                                                color = TextSecondary,
                                                maxLines = 1,
                                                modifier = Modifier.weight(1f, fill = false)
                                            )
                                        }

                                        // Right Side: Checkbox / Checkmark Toggle
                                        Surface(
                                            shape = RoundedCornerShape(4.dp),
                                            color = if (isChecked) NeonGreen else DarkBackground,
                                            border = BorderStroke(
                                                width = 1.dp,
                                                color = if (isChecked) NeonGreen else BorderStrokeLight
                                            ),
                                            modifier = Modifier.size(24.dp)
                                        ) {
                                            Box(contentAlignment = Alignment.Center) {
                                                if (isChecked) {
                                                    Icon(
                                                        imageVector = Icons.Default.Check,
                                                        contentDescription = "Verified",
                                                        tint = DarkBackground,
                                                        modifier = Modifier.size(16.dp)
                                                    )
                                                }
                                            }
                                        }
                                    }
                                }
                            }
                        }

                        // Status feedback message
                        if (verificationStatusMessage != null) {
                            Surface(
                                shape = RoundedCornerShape(6.dp),
                                color = DarkCard,
                                border = BorderStroke(0.5.dp, NeonGreen.copy(alpha = 0.5f)),
                                modifier = Modifier.fillMaxWidth()
                            ) {
                                Text(
                                    text = verificationStatusMessage ?: "",
                                    fontSize = 9.5.sp,
                                    fontWeight = FontWeight.Bold,
                                    color = NeonGreen,
                                    textAlign = TextAlign.Center,
                                    modifier = Modifier.padding(6.dp)
                                )
                            }
                        }

                        // Bottom Action Controls: Save Verification & Cancel
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.spacedBy(8.dp)
                        ) {
                            // Cancel Button
                            Surface(
                                onClick = {
                                    val reset = mutableMapOf<String, Boolean>()
                                    allVerificationRules.forEach { r ->
                                        reset[r.id] = UserRuleRegistry.isRuleVerified(r.id)
                                    }
                                    draftVerifiedMap = reset
                                    verificationStatusMessage = "সকল পরিবর্তন বাতিল করা হয়েছে।"
                                },
                                modifier = Modifier.weight(1f),
                                shape = RoundedCornerShape(8.dp),
                                color = DarkSurfaceVariant,
                                border = BorderStroke(1.dp, BorderStrokeLight)
                            ) {
                                Row(
                                    modifier = Modifier.padding(vertical = 9.dp),
                                    horizontalArrangement = Arrangement.Center,
                                    verticalAlignment = Alignment.CenterVertically
                                ) {
                                    Icon(
                                        imageVector = Icons.Default.Close,
                                        contentDescription = "Cancel",
                                        tint = TextSecondary,
                                        modifier = Modifier.size(14.dp)
                                    )
                                    Spacer(modifier = Modifier.width(4.dp))
                                    Text(
                                        text = "Cancel (বাতিল)",
                                        fontSize = 10.5.sp,
                                        fontWeight = FontWeight.Bold,
                                        color = TextSecondary
                                    )
                                }
                            }

                            // Save Verification Button
                            Surface(
                                onClick = {
                                    UserRuleRegistry.setRulesBatchVerified(draftVerifiedMap)
                                    hapticFeedback.performHapticFeedback(HapticFeedbackType.LongPress)
                                    refreshTick++
                                    val savedCount = allVerificationRules.count { draftVerifiedMap[it.id] == true }
                                    verificationStatusMessage = "✅ সমস্ত ভেরিফিকেশন সফলভাবে সেভ হয়েছে! ($savedCount টি রুলস ভেরিফাইড ✓)"
                                },
                                modifier = Modifier.weight(1.4f),
                                shape = RoundedCornerShape(8.dp),
                                color = NeonGreen
                            ) {
                                Row(
                                    modifier = Modifier.padding(vertical = 9.dp),
                                    horizontalArrangement = Arrangement.Center,
                                    verticalAlignment = Alignment.CenterVertically
                                ) {
                                    Icon(
                                        imageVector = Icons.Default.Check,
                                        contentDescription = "Save",
                                        tint = DarkBackground,
                                        modifier = Modifier.size(15.dp)
                                    )
                                    Spacer(modifier = Modifier.width(4.dp))
                                    Text(
                                        text = "Save Verification (সংরক্ষণ)",
                                        fontSize = 10.5.sp,
                                        fontWeight = FontWeight.ExtraBold,
                                        color = DarkBackground
                                    )
                                }
                            }
                        }
                    }
                }
            }
        }
    )
}
