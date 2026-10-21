package com.restor.gamereel.feature.settings

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.unit.dp
import com.restor.gamereel.model.normalizeHexColor
import com.restor.gamereel.ui.theme.colorFromHex
import gamereel.app.shared.generated.resources.*
import org.jetbrains.compose.resources.stringResource
import kotlin.math.roundToInt

@Composable
internal fun CustomColorDialog(
    title: String,
    initialHex: String,
    onDismiss: () -> Unit,
    onSave: (String) -> Unit,
) {
    var hex by remember(initialHex) { mutableStateOf(initialHex) }
    val valid = normalizeHexColor(hex)
    val parsed = valid?.removePrefix("#")?.toIntOrNull(16)
    val channels = listOf(
        ((parsed ?: 0) shr 16) and 255,
        ((parsed ?: 0) shr 8) and 255,
        (parsed ?: 0) and 255,
    )
    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text(title) },
        text = {
            Column(verticalArrangement = Arrangement.spacedBy(10.dp)) {
                Box(
                    Modifier.fillMaxWidth().height(64.dp)
                        .background(colorFromHex(hex) ?: Color.Transparent, RoundedCornerShape(12.dp))
                        .border(1.dp, MaterialTheme.colorScheme.outline, RoundedCornerShape(12.dp))
                )
                OutlinedTextField(
                    value = hex,
                    onValueChange = { hex = it.take(7) },
                    label = { Text(stringResource(Res.string.settings_color_hex)) },
                    isError = valid == null,
                    singleLine = true,
                    modifier = Modifier.fillMaxWidth(),
                )
                listOf("R", "G", "B").forEachIndexed { index, channel ->
                    Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween) {
                        Text(channel)
                        Text(channels[index].toString())
                    }
                    Slider(
                        value = channels[index].toFloat(),
                        onValueChange = { value ->
                            val updated = channels.toMutableList()
                            updated[index] = value.roundToInt().coerceIn(0, 255)
                            hex = "#" + updated.joinToString("") { it.toString(16).padStart(2, '0') }.uppercase()
                        },
                        valueRange = 0f..255f,
                    )
                }
            }
        },
        confirmButton = {
            Button(onClick = { valid?.let(onSave) }, enabled = valid != null) {
                Text(stringResource(Res.string.settings_color_save))
            }
        },
        dismissButton = { TextButton(onClick = onDismiss) { Text(stringResource(Res.string.cancel)) } },
    )
}
