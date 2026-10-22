package com.restor.gamereel.feature.settings

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.text.KeyboardActions
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.selection.toggleable
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.selected
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.text.input.ImeAction
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.unit.dp
import com.restor.gamereel.generator.WorkspaceEditor
import com.restor.gamereel.data.storage.WorkspaceBackup
import com.restor.gamereel.data.storage.makeWorkspaceBackup
import com.restor.gamereel.feature.generator.SpinTimeline
import com.restor.gamereel.model.*
import com.restor.gamereel.ui.components.StudioCard
import com.restor.gamereel.ui.presentation.title
import com.restor.gamereel.ui.theme.colorFromHex
import com.restor.gamereel.ui.theme.swatch
import gamereel.app.shared.generated.resources.*
import io.github.vinceglb.filekit.dialogs.FileKitDialogSettings
import io.github.vinceglb.filekit.dialogs.FileKitType
import io.github.vinceglb.filekit.dialogs.compose.rememberFilePickerLauncher
import com.restor.gamereel.platform.rememberExportLauncher
import io.github.vinceglb.filekit.readBytes
import kotlinx.coroutines.launch
import kotlinx.serialization.encodeToString
import kotlinx.serialization.json.Json
import kotlin.math.roundToInt
import org.jetbrains.compose.resources.stringResource

@Composable
internal fun PreferencesContent(
    workspace: Workspace,
    libraries: List<LibraryPack>,
    reelCount: Int,
    onSpeed: (AnimationSpeed) -> Unit,
    onView: (GeneratorView) -> Unit,
    onLanguage: (AppLanguage) -> Unit,
    onSound: (Boolean) -> Unit,
    onAccent: (UiAccent) -> Unit,
    onBackground: (UiBackground) -> Unit,
    onText: (UiText) -> Unit,
    onSaveColor: (UiColorRole, String) -> Unit,
    onRemoveColor: (UiColorRole, String) -> Unit,
    onTiming: (Int, Int) -> Unit,
    onRestoreBackup: (WorkspaceBackup) -> Unit,
    onShowWelcome: () -> Unit,
) {
    val preferences = workspace.preferences
    var privacyOpen by remember { mutableStateOf(false) }
    var editingColor by remember { mutableStateOf<UiColorRole?>(null) }
    var pendingBackup by remember { mutableStateOf<WorkspaceBackup?>(null) }
    var backupError by remember { mutableStateOf<String?>(null) }
    val scope = rememberCoroutineScope()
    val backupJson = remember { Json { ignoreUnknownKeys = true; encodeDefaults = true; prettyPrint = true } }
    val invalidBackup = stringResource(Res.string.backup_invalid)
    val backupSaver = rememberExportLauncher(
        dialogSettings = FileKitDialogSettings.createDefault(), onResult = { file ->
            if (file != null) scope.launch {
                runCatching {
                    file.write(backupJson.encodeToString(makeWorkspaceBackup(workspace, libraries))
                        .encodeToByteArray())
                }.onFailure { backupError = it.message ?: invalidBackup }
            }
        },
    )
    val backupPicker = rememberFilePickerLauncher(
        type = FileKitType.File(extensions = listOf("json")), onResult = { file ->
            if (file != null) scope.launch {
                runCatching {
                    val bytes = file.readBytes()
                    require(bytes.size <= 30_000_000)
                    backupJson.decodeFromString<WorkspaceBackup>(bytes.decodeToString())
                        .also(WorkspaceBackup::validate)
                }.onSuccess { pendingBackup = it }
                    .onFailure { backupError = invalidBackup }
            }
        },
    )
    LazyColumn(
        contentPadding = PaddingValues(16.dp),
        verticalArrangement = Arrangement.spacedBy(12.dp),
    ) {
        item {
            SettingsGroup(
                title = stringResource(Res.string.settings_language),
                hint = stringResource(Res.string.settings_language_hint),
            ) {
                FlowRow(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                    AppLanguage.entries.forEach { language ->
                        FilterChip(
                            selected = preferences.language == language,
                            onClick = { onLanguage(language) },
                            label = {
                                Text(
                                    stringResource(
                                        when (language) {
                                            AppLanguage.SYSTEM -> Res.string.language_system
                                            AppLanguage.RUSSIAN -> Res.string.language_russian
                                            AppLanguage.ENGLISH -> Res.string.language_english
                                            AppLanguage.SPANISH -> Res.string.language_spanish
                                            AppLanguage.FRENCH -> Res.string.language_french
                                            AppLanguage.GERMAN -> Res.string.language_german
                                            AppLanguage.PORTUGUESE -> Res.string.language_portuguese
                                            AppLanguage.ITALIAN -> Res.string.language_italian
                                            AppLanguage.POLISH -> Res.string.language_polish
                                            AppLanguage.TURKISH -> Res.string.language_turkish
                                            AppLanguage.UKRAINIAN -> Res.string.language_ukrainian
                                            AppLanguage.INDONESIAN -> Res.string.language_indonesian
                                            AppLanguage.JAPANESE -> Res.string.language_japanese
                                            AppLanguage.KOREAN -> Res.string.language_korean
                                            AppLanguage.CHINESE -> Res.string.language_chinese
                                        }
                                    )
                                )
                            },
                        )
                    }
                }
            }
        }
        item {
            ColorSettingsGroup(
                title = stringResource(Res.string.settings_accent_color),
                builtins = UiAccent.entries.map { it.swatch() },
                selectedBuiltin = UiAccent.entries.indexOf(preferences.uiAccent)
                    .takeIf { preferences.customColors.selectedAccent == null },
                customColors = preferences.customColors.accents,
                selectedCustom = preferences.customColors.selectedAccent,
                onBuiltin = { onAccent(UiAccent.entries[it]) },
                onCustom = { onSaveColor(UiColorRole.ACCENT, it) },
                onRemove = { onRemoveColor(UiColorRole.ACCENT, it) },
                onAdd = { editingColor = UiColorRole.ACCENT },
            )
        }
        item {
            ColorSettingsGroup(
                title = stringResource(Res.string.settings_background_color),
                builtins = UiBackground.entries.map { it.swatch() },
                selectedBuiltin = UiBackground.entries.indexOf(preferences.uiBackground)
                    .takeIf { preferences.customColors.selectedBackground == null },
                customColors = preferences.customColors.backgrounds,
                selectedCustom = preferences.customColors.selectedBackground,
                onBuiltin = { onBackground(UiBackground.entries[it]) },
                onCustom = { onSaveColor(UiColorRole.BACKGROUND, it) },
                onRemove = { onRemoveColor(UiColorRole.BACKGROUND, it) },
                onAdd = { editingColor = UiColorRole.BACKGROUND },
            )
        }
        item {
            ColorSettingsGroup(
                title = stringResource(Res.string.settings_text_color),
                builtins = UiText.entries.map { it.swatch() },
                selectedBuiltin = UiText.entries.indexOf(preferences.uiText)
                    .takeIf { preferences.customColors.selectedText == null },
                customColors = preferences.customColors.texts,
                selectedCustom = preferences.customColors.selectedText,
                onBuiltin = { onText(UiText.entries[it]) },
                onCustom = { onSaveColor(UiColorRole.TEXT, it) },
                onRemove = { onRemoveColor(UiColorRole.TEXT, it) },
                onAdd = { editingColor = UiColorRole.TEXT },
            )
        }
        item {
            SettingsGroup(
                title = stringResource(Res.string.settings_animation),
                hint = stringResource(Res.string.settings_animation_hint),
            ) {
                val enabled = preferences.animationSpeed != AnimationSpeed.OFF
                val requestedDuration = preferences.requestedSpinDurationMs(reelCount)
                val decimalSeparator = stringResource(Res.string.settings_decimal_separator)
                var totalText by remember(requestedDuration, decimalSeparator) {
                    mutableStateOf(formatSeconds(requestedDuration, decimalSeparator))
                }
                var gapText by remember(preferences.reelStopGapMs, decimalSeparator) {
                    mutableStateOf(formatSeconds(preferences.reelStopGapMs, decimalSeparator))
                }
                val totalMs = parseSeconds(totalText, 500, 30000)
                val gapMs = parseSeconds(gapText, 0, 3000)
                val canApply = totalMs != null && gapMs != null &&
                    (totalMs != requestedDuration || gapMs != preferences.reelStopGapMs)
                val applyTiming = {
                    if (totalMs != null && gapMs != null &&
                        (totalMs != requestedDuration || gapMs != preferences.reelStopGapMs)
                    ) onTiming(totalMs, gapMs)
                }
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Text(
                        stringResource(Res.string.settings_animation_enabled),
                        Modifier.weight(1f),
                        style = MaterialTheme.typography.bodyMedium,
                    )
                    Switch(
                        checked = enabled,
                        onCheckedChange = { onSpeed(if (it) AnimationSpeed.NORMAL else AnimationSpeed.OFF) },
                    )
                }
                OutlinedTextField(
                    value = totalText,
                    onValueChange = { totalText = it.take(7) },
                    label = { Text(stringResource(Res.string.settings_total_duration_label)) },
                    singleLine = true,
                    isError = totalMs == null,
                    keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Decimal, imeAction = ImeAction.Done),
                    keyboardActions = KeyboardActions(onDone = { applyTiming() }),
                    modifier = Modifier.fillMaxWidth(),
                )
                OutlinedTextField(
                    value = gapText,
                    onValueChange = { gapText = it.take(7) },
                    label = { Text(stringResource(Res.string.settings_stop_gap_label)) },
                    singleLine = true,
                    isError = gapMs == null,
                    keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Decimal, imeAction = ImeAction.Done),
                    keyboardActions = KeyboardActions(onDone = { applyTiming() }),
                    modifier = Modifier.fillMaxWidth(),
                )
                if (totalMs == null || gapMs == null)
                    Text(
                        stringResource(Res.string.settings_timing_error),
                        color = MaterialTheme.colorScheme.error,
                        style = MaterialTheme.typography.bodySmall,
                    )
                Button(onClick = applyTiming, enabled = canApply) {
                    Text(stringResource(Res.string.settings_apply_timing))
                }
                val requestedMs = totalMs ?: requestedDuration
                val actualMs = SpinTimeline.actualDurationMs(
                    requestedMs,
                    gapMs ?: preferences.reelStopGapMs,
                    reelCount,
                )
                if (actualMs > requestedMs)
                    Text(
                        stringResource(
                            Res.string.settings_actual_duration,
                            formatSeconds(actualMs, decimalSeparator),
                            reelCount,
                        ),
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                    )
            }
        }
        item {
            StudioCard(Modifier.fillMaxWidth()) {
                Row(
                    Modifier.fillMaxWidth()
                        .toggleable(
                            value = preferences.soundEnabled,
                            role = Role.Switch,
                            onValueChange = onSound,
                        )
                        .padding(16.dp),
                    verticalAlignment = Alignment.CenterVertically,
                ) {
                    Text(
                        stringResource(Res.string.settings_sound),
                        Modifier.weight(1f),
                        style = MaterialTheme.typography.titleMedium,
                    )
                    Switch(checked = preferences.soundEnabled, onCheckedChange = null)
                }
            }
        }
        item {
            SettingsGroup(title = stringResource(Res.string.settings_view)) {
                FlowRow(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                    GeneratorView.entries.forEach { view ->
                        FilterChip(
                            selected = preferences.generatorView == view,
                            onClick = { onView(view) },
                            label = { Text(stringResource(view.title)) },
                        )
                    }
                }
            }
        }
        item {
            SettingsGroup(title = stringResource(Res.string.backup_title),
                hint = stringResource(Res.string.backup_hint)) {
                Button(onClick = {
                    backupSaver.launch(suggestedName = "gamereel-backup", defaultExtension = "json")
                }) { Text(stringResource(Res.string.backup_export)) }
                OutlinedButton(onClick = { backupPicker.launch() }) {
                    Text(stringResource(Res.string.backup_import))
                }
                backupError?.let { Text(it, color = MaterialTheme.colorScheme.error) }
            }
        }
        item {
            SettingsGroup(title = stringResource(Res.string.welcome_title)) {
                TextButton(onClick = onShowWelcome) {
                    Text(stringResource(Res.string.welcome_show_again))
                }
            }
        }
        item {
            SettingsGroup(title = stringResource(Res.string.privacy_title)) {
                Text(
                    stringResource(Res.string.settings_storage_hint, WorkspaceEditor.HISTORY_LIMIT),
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                )
                TextButton(onClick = { privacyOpen = true }) {
                    Text(stringResource(Res.string.privacy_title))
                }
            }
        }
    }
    if (privacyOpen) PrivacyDialog { privacyOpen = false }
    pendingBackup?.let { backup ->
        AlertDialog(
            onDismissRequest = { pendingBackup = null },
            title = { Text(stringResource(Res.string.backup_confirm_title)) },
            text = { Text(stringResource(Res.string.backup_confirm_text)) },
            confirmButton = { TextButton(onClick = {
                onRestoreBackup(backup)
                pendingBackup = null
            }) { Text(stringResource(Res.string.backup_restore)) } },
            dismissButton = { TextButton(onClick = { pendingBackup = null }) {
                Text(stringResource(Res.string.cancel))
            } },
        )
    }
    editingColor?.let { role ->
        val title = stringResource(when (role) {
            UiColorRole.ACCENT -> Res.string.settings_accent_color
            UiColorRole.BACKGROUND -> Res.string.settings_background_color
            UiColorRole.TEXT -> Res.string.settings_text_color
        })
        val initial = when (role) {
            UiColorRole.ACCENT -> preferences.customColors.selectedAccent ?: "#C5AEFF"
            UiColorRole.BACKGROUND -> preferences.customColors.selectedBackground ?: "#202838"
            UiColorRole.TEXT -> preferences.customColors.selectedText ?: "#E8DFFF"
        }
        CustomColorDialog(title, initial, onDismiss = { editingColor = null }) { hex ->
            onSaveColor(role, hex)
            editingColor = null
        }
    }
}

private fun formatSeconds(milliseconds: Int, decimalSeparator: String): String {
    val seconds = milliseconds / 1000
    val fraction = (milliseconds % 1000).toString().padStart(3, '0').trimEnd('0')
    return if (fraction.isEmpty()) seconds.toString() else "$seconds$decimalSeparator$fraction"
}

private fun parseSeconds(value: String, minMs: Int, maxMs: Int): Int? =
    value.trim().replace(',', '.').toFloatOrNull()
        ?.takeIf { it.isFinite() }
        ?.let { (it * 1000).roundToInt() }
        ?.takeIf { it in minMs..maxMs }

@Composable
private fun ColorSettingsGroup(
    title: String,
    builtins: List<Color>,
    selectedBuiltin: Int?,
    customColors: List<String>,
    selectedCustom: String?,
    onBuiltin: (Int) -> Unit,
    onCustom: (String) -> Unit,
    onRemove: (String) -> Unit,
    onAdd: () -> Unit,
) {
    SettingsGroup(title = title) {
        FlowRow(horizontalArrangement = Arrangement.spacedBy(10.dp), verticalArrangement = Arrangement.spacedBy(10.dp)) {
            builtins.forEachIndexed { index, color ->
                ColorSquare(color, selectedBuiltin == index, "$title ${index + 1}") { onBuiltin(index) }
            }
            customColors.forEach { hex ->
                colorFromHex(hex)?.let { color ->
                    ColorSquare(color, selectedCustom == hex, "$title $hex") { onCustom(hex) }
                }
            }
            OutlinedButton(onClick = onAdd, modifier = Modifier.size(52.dp), contentPadding = PaddingValues(0.dp)) {
                Text("+", style = MaterialTheme.typography.headlineSmall)
            }
        }
        Text(stringResource(Res.string.settings_add_custom_color), style = MaterialTheme.typography.bodySmall,
            color = MaterialTheme.colorScheme.onSurfaceVariant)
        if (selectedCustom != null) {
            TextButton(onClick = { onRemove(selectedCustom) }) {
                Text(stringResource(Res.string.settings_remove_custom_color))
            }
        }
    }
}

@Composable
private fun ColorSquare(color: Color, isSelected: Boolean, label: String, onClick: () -> Unit) {
    val shape = RoundedCornerShape(12.dp)
    Box(
        Modifier.size(52.dp)
            .background(color, shape)
            .border(if (isSelected) 3.dp else 1.dp,
                if (isSelected) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.outline,
                shape)
            .clickable(onClick = onClick)
            .semantics { contentDescription = label; selected = isSelected },
        contentAlignment = Alignment.Center,
    ) {
        if (isSelected) Text("✓", color =
            if (color.red * 0.299f + color.green * 0.587f + color.blue * 0.114f > 0.55f) Color.Black else Color.White)
    }
}

@Composable
private fun SettingsGroup(
    title: String,
    hint: String? = null,
    content: @Composable ColumnScope.() -> Unit,
) {
    StudioCard(Modifier.fillMaxWidth()) {
        Column(Modifier.padding(16.dp), verticalArrangement = Arrangement.spacedBy(8.dp)) {
            Text(title, style = MaterialTheme.typography.titleMedium)
            content()
            if (hint != null)
                Text(
                    hint,
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                )
        }
    }
}
