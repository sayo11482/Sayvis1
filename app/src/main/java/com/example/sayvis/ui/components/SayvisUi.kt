package com.example.sayvis.ui.components

import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.horizontalScroll
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.FilterChip
import androidx.compose.material3.FilterChipDefaults
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.LocalContentColor
import androidx.compose.material3.LocalTextStyle
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.OutlinedTextFieldDefaults
import androidx.compose.material3.Switch
import androidx.compose.material3.SwitchDefaults
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.compositionLocalOf
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.text.input.PasswordVisualTransformation
import androidx.compose.ui.text.input.VisualTransformation
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.TextUnit
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.sayvis.ai.TranslationResult
import com.example.sayvis.ai.TranslationSource
import com.example.sayvis.i18n.LocalStrings
import com.example.sayvis.ui.theme.SayvisAmberWarning
import com.example.sayvis.ui.theme.SayvisBorder
import com.example.sayvis.ui.theme.SayvisCyan
import com.example.sayvis.ui.theme.SayvisGold
import com.example.sayvis.ui.theme.SayvisGreenSuccess
import com.example.sayvis.ui.theme.SayvisRedAlert
import com.example.sayvis.ui.theme.SayvisSilver
import com.example.sayvis.ui.theme.SayvisSilverMuted
import com.example.sayvis.ui.theme.SayvisSurfaceVariant

/**
 * Bridge between the Compose tree and the translation service.
 *
 * Screens never touch the AI layer directly; they read [LocalTranslation] and let
 * [SayvisText] decide whether a string needs a dictionary lookup, a cached value or a
 * live machine translation.
 */
interface TranslationBridge {
    /** True when the active UI language is Persian. */
    val isPersian: Boolean

    /** True when the owner allowed online translation of free-form text. */
    val autoTranslate: Boolean

    /** True when numbers should render with Persian digits. */
    val persianDigits: Boolean

    /** Offline resolution: dictionary, templates and cache. Never suspends. */
    fun offline(text: String): TranslationResult = resolve(text, persianDigits)

    /** Offline resolution with an explicit digit preference. */
    fun resolve(text: String, persianDigits: Boolean): TranslationResult

    /** Live machine translation. Returns null when unavailable. */
    suspend fun online(text: String): String?
}

val LocalTranslation = compositionLocalOf<TranslationBridge?> { null }

/** Current render state of one localised string. */
data class LocalizedText(
    val text: String,
    val source: TranslationSource,
    val pending: Boolean
) {
    val isMachine: Boolean get() = source == TranslationSource.MACHINE || source == TranslationSource.CACHE
}

/**
 * Resolves [source] through the hybrid pipeline and keeps upgrading it:
 * dictionary → cache → live machine translation.
 */
@Composable
fun rememberLocalizedText(source: String): LocalizedText {
    val strings = LocalStrings.current
    val bridge = LocalTranslation.current

    val initial = remember(source, strings.fa) {
        when {
            !strings.fa -> LocalizedText(source, TranslationSource.ORIGINAL, false)
            bridge == null -> LocalizedText(source, TranslationSource.ORIGINAL, false)
            else -> {
                val resolved = bridge.offline(source)
                if (resolved.source == TranslationSource.FAILED) {
                    LocalizedText(source, TranslationSource.FAILED, true)
                } else {
                    LocalizedText(resolved.text, resolved.source, false)
                }
            }
        }
    }

    var state by remember(source, strings.fa) { mutableStateOf(initial) }

    LaunchedEffect(source, strings.fa, bridge) {
        val active = bridge ?: return@LaunchedEffect
        if (!initial.pending || !active.autoTranslate || !active.isPersian) {
            if (state.pending) state = initial.copy(pending = false)
            return@LaunchedEffect
        }
        val translated = active.online(source)
        state = if (translated != null) {
            LocalizedText(translated, TranslationSource.MACHINE, false)
        } else {
            LocalizedText(source, TranslationSource.FAILED, false)
        }
    }

    return state
}

/**
 * Drop-in replacement for [Text] that is always fully localised.
 *
 * When [markTranslated] is on and the text came from the machine translator, a small
 * marker is appended so the owner is never misled about provenance.
 */
@Composable
fun SayvisText(
    source: String,
    modifier: Modifier = Modifier,
    style: TextStyle = LocalTextStyle.current,
    color: Color = LocalContentColor.current,
    fontWeight: FontWeight? = null,
    fontSize: TextUnit = TextUnit.Unspecified,
    lineHeight: TextUnit = TextUnit.Unspecified,
    maxLines: Int = Int.MAX_VALUE,
    textAlign: TextAlign? = null,
    markTranslated: Boolean = false
) {
    val strings = LocalStrings.current
    val localized = rememberLocalizedText(source)
    val suffix = if (markTranslated && localized.isMachine) " · ${strings.machineTranslated}" else ""
    Text(
        text = localized.text + suffix,
        modifier = modifier,
        style = style,
        color = color,
        fontWeight = fontWeight,
        fontSize = fontSize,
        lineHeight = lineHeight,
        maxLines = maxLines,
        textAlign = textAlign
    )
}

// --------------------------------------------------------------------- layout

/** Section header used across Settings, Tools and the gateway screens. */
@Composable
fun SayvisSectionHeader(
    title: String,
    subtitle: String? = null,
    icon: ImageVector? = null,
    modifier: Modifier = Modifier
) {
    Row(
        modifier = modifier
            .fillMaxWidth()
            .padding(top = 18.dp, bottom = 8.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        if (icon != null) {
            Icon(
                imageVector = icon,
                contentDescription = null,
                tint = SayvisGold,
                modifier = Modifier.size(18.dp)
            )
            Spacer(modifier = Modifier.width(8.dp))
        }
        Column {
            Text(
                text = title,
                fontSize = 14.sp,
                fontWeight = FontWeight.Bold,
                color = SayvisSilver
            )
            if (subtitle != null) {
                Text(text = subtitle, fontSize = 11.sp, color = SayvisSilverMuted)
            }
        }
    }
}

@Composable
fun SayvisCard(
    modifier: Modifier = Modifier,
    borderColor: Color = SayvisBorder,
    containerColor: Color = SayvisSurfaceVariant,
    contentPadding: PaddingValues = PaddingValues(14.dp),
    content: @Composable () -> Unit
) {
    Card(
        modifier = modifier.fillMaxWidth(),
        shape = RoundedCornerShape(12.dp),
        colors = CardDefaults.cardColors(containerColor = containerColor),
        border = BorderStroke(1.dp, borderColor)
    ) {
        Column(modifier = Modifier.padding(contentPadding)) { content() }
    }
}

@Composable
fun SayvisToggleRow(
    label: String,
    hint: String?,
    checked: Boolean,
    onCheckedChange: (Boolean) -> Unit,
    modifier: Modifier = Modifier,
    enabled: Boolean = true
) {
    Row(
        modifier = modifier
            .fillMaxWidth()
            .padding(vertical = 8.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        Column(modifier = Modifier.weight(1f)) {
            Text(text = label, fontSize = 13.sp, fontWeight = FontWeight.Medium, color = SayvisSilver)
            if (hint != null) {
                Text(text = hint, fontSize = 10.5.sp, color = SayvisSilverMuted)
            }
        }
        Spacer(modifier = Modifier.width(12.dp))
        Switch(
            checked = checked,
            onCheckedChange = onCheckedChange,
            enabled = enabled,
            colors = SwitchDefaults.colors(
                checkedThumbColor = SayvisCyan,
                checkedTrackColor = SayvisCyan.copy(alpha = 0.25f),
                uncheckedThumbColor = SayvisSilverMuted,
                uncheckedTrackColor = SayvisSurfaceVariant,
                uncheckedBorderColor = SayvisBorder
            )
        )
    }
}

/** Horizontally scrollable chip group used for every enumerated choice. */
@Composable
fun SayvisOptionRow(
    label: String,
    options: List<String>,
    selectedIndex: Int,
    onSelect: (Int) -> Unit,
    modifier: Modifier = Modifier,
    hint: String? = null
) {
    Column(modifier = modifier.fillMaxWidth().padding(vertical = 6.dp)) {
        Text(text = label, fontSize = 13.sp, fontWeight = FontWeight.Medium, color = SayvisSilver)
        if (hint != null) {
            Text(text = hint, fontSize = 10.5.sp, color = SayvisSilverMuted)
        }
        Spacer(modifier = Modifier.height(6.dp))
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .horizontalScroll(rememberScrollState()),
            horizontalArrangement = Arrangement.spacedBy(6.dp)
        ) {
            options.forEachIndexed { index, option ->
                FilterChip(
                    selected = index == selectedIndex,
                    onClick = { onSelect(index) },
                    label = { Text(text = option, fontSize = 11.sp, fontWeight = FontWeight.Medium) },
                    colors = FilterChipDefaults.filterChipColors(
                        selectedContainerColor = SayvisCyan.copy(alpha = 0.22f),
                        selectedLabelColor = SayvisCyan,
                        containerColor = SayvisSurfaceVariant,
                        labelColor = SayvisSilverMuted
                    ),
                    border = FilterChipDefaults.filterChipBorder(
                        enabled = true,
                        selected = index == selectedIndex,
                        borderColor = SayvisBorder,
                        selectedBorderColor = SayvisCyan
                    )
                )
            }
        }
    }
}

@Composable
fun SayvisField(
    label: String,
    value: String,
    onValueChange: (String) -> Unit,
    modifier: Modifier = Modifier,
    hint: String? = null,
    singleLine: Boolean = true,
    minLines: Int = 1,
    isSecret: Boolean = false,
    revealSecret: Boolean = false,
    keyboardType: KeyboardType = KeyboardType.Text,
    monospace: Boolean = false,
    trailing: (@Composable () -> Unit)? = null,
    enabled: Boolean = true
) {
    Column(modifier = modifier.fillMaxWidth().padding(vertical = 5.dp)) {
        Text(text = label, fontSize = 12.sp, fontWeight = FontWeight.Medium, color = SayvisSilver)
        Spacer(modifier = Modifier.height(4.dp))
        OutlinedTextField(
            value = value,
            onValueChange = onValueChange,
            modifier = Modifier.fillMaxWidth(),
            enabled = enabled,
            singleLine = singleLine,
            minLines = minLines,
            placeholder = { Text(text = hint ?: "", fontSize = 11.sp, color = SayvisSilverMuted) },
            visualTransformation = if (isSecret && !revealSecret) PasswordVisualTransformation() else VisualTransformation.None,
            keyboardOptions = KeyboardOptions(keyboardType = keyboardType),
            textStyle = TextStyle(
                fontSize = if (monospace) 11.5.sp else 13.sp,
                fontFamily = if (monospace) FontFamily.Monospace else FontFamily.Default,
                color = SayvisSilver
            ),
            trailingIcon = trailing,
            shape = RoundedCornerShape(10.dp),
            colors = OutlinedTextFieldDefaults.colors(
                focusedBorderColor = SayvisCyan,
                unfocusedBorderColor = SayvisBorder,
                focusedTextColor = SayvisSilver,
                unfocusedTextColor = SayvisSilver,
                cursorColor = SayvisCyan,
                focusedContainerColor = SayvisSurfaceVariant,
                unfocusedContainerColor = SayvisSurfaceVariant
            )
        )
    }
}

@Composable
fun SayvisButton(
    label: String,
    onClick: () -> Unit,
    modifier: Modifier = Modifier,
    enabled: Boolean = true,
    icon: ImageVector? = null,
    tone: ButtonTone = ButtonTone.PRIMARY,
    busy: Boolean = false
) {
    val container = when (tone) {
        ButtonTone.PRIMARY -> SayvisCyan
        ButtonTone.GOLD -> SayvisGold
        ButtonTone.SUCCESS -> SayvisGreenSuccess
        ButtonTone.DANGER -> SayvisRedAlert
        ButtonTone.NEUTRAL -> SayvisSurfaceVariant
    }
    val content = when (tone) {
        ButtonTone.NEUTRAL -> SayvisSilver
        else -> Color.Black
    }
    Button(
        onClick = onClick,
        modifier = modifier.height(40.dp),
        enabled = enabled && !busy,
        shape = RoundedCornerShape(10.dp),
        contentPadding = PaddingValues(horizontal = 16.dp),
        colors = ButtonDefaults.buttonColors(
            containerColor = container,
            contentColor = content,
            disabledContainerColor = container.copy(alpha = 0.3f),
            disabledContentColor = content.copy(alpha = 0.5f)
        )
    ) {
        if (busy) {
            CircularProgressIndicator(
                modifier = Modifier.size(15.dp),
                color = content,
                strokeWidth = 2.dp
            )
            Spacer(modifier = Modifier.width(8.dp))
        } else if (icon != null) {
            Icon(imageVector = icon, contentDescription = null, modifier = Modifier.size(16.dp))
            Spacer(modifier = Modifier.width(8.dp))
        }
        Text(text = label, fontSize = 12.5.sp, fontWeight = FontWeight.Bold)
    }
}

enum class ButtonTone { PRIMARY, GOLD, SUCCESS, DANGER, NEUTRAL }

/** Small coloured status label. */
@Composable
fun SayvisStatusPill(text: String, color: Color, modifier: Modifier = Modifier) {
    Box(
        modifier = modifier
            .background(color.copy(alpha = 0.18f), RoundedCornerShape(6.dp))
            .padding(horizontal = 8.dp, vertical = 3.dp)
    ) {
        Text(text = text, fontSize = 10.sp, fontWeight = FontWeight.Bold, color = color)
    }
}

/** Label + value row used for read-only account statistics. */
@Composable
fun SayvisStatRow(label: String, value: String, valueColor: Color = SayvisSilver, modifier: Modifier = Modifier) {
    Row(
        modifier = modifier
            .fillMaxWidth()
            .padding(vertical = 3.dp),
        horizontalArrangement = Arrangement.SpaceBetween,
        verticalAlignment = Alignment.CenterVertically
    ) {
        Text(text = label, fontSize = 11.5.sp, color = SayvisSilverMuted)
        Text(text = value, fontSize = 12.sp, fontWeight = FontWeight.Bold, color = valueColor)
    }
}

@Composable
fun SayvisDivider(modifier: Modifier = Modifier) {
    HorizontalDivider(
        modifier = modifier.padding(vertical = 6.dp),
        thickness = 1.dp,
        color = SayvisBorder
    )
}

/** Consistent colour for a profit/loss figure. */
fun pnlColor(value: Double): Color = when {
    value > 0 -> SayvisGreenSuccess
    value < 0 -> SayvisRedAlert
    else -> SayvisSilverMuted
}

/** Consistent colour for a warning banner tone. */
fun toneColor(warning: Boolean = false, danger: Boolean = false, success: Boolean = false): Color = when {
    danger -> SayvisRedAlert
    warning -> SayvisAmberWarning
    success -> SayvisGreenSuccess
    else -> SayvisCyan
}

@Composable
fun SayvisLinkButton(label: String, onClick: () -> Unit, modifier: Modifier = Modifier) {
    TextButton(onClick = onClick, modifier = modifier) {
        Text(text = label, fontSize = 11.5.sp, fontWeight = FontWeight.Bold, color = SayvisGold)
    }
}

/**
 * Consistent confirmation dialog. Used for every destructive or irreversible action so
 * the owner always sees the same shape of warning.
 */
@Composable
fun SayvisConfirmDialog(
    title: String,
    body: String,
    confirmLabel: String,
    dismissLabel: String,
    onConfirm: () -> Unit,
    onDismiss: () -> Unit,
    danger: Boolean = false,
    extra: (@Composable () -> Unit)? = null
) {
    androidx.compose.material3.AlertDialog(
        onDismissRequest = onDismiss,
        containerColor = SayvisSurfaceVariant,
        titleContentColor = SayvisSilver,
        textContentColor = SayvisSilverMuted,
        shape = RoundedCornerShape(16.dp),
        title = { Text(text = title, fontSize = 15.sp, fontWeight = FontWeight.Bold) },
        text = {
            Column {
                Text(text = body, fontSize = 12.sp)
                if (extra != null) {
                    Spacer(modifier = Modifier.height(10.dp))
                    extra()
                }
            }
        },
        confirmButton = {
            TextButton(onClick = onConfirm) {
                Text(
                    text = confirmLabel,
                    fontSize = 12.5.sp,
                    fontWeight = FontWeight.Bold,
                    color = if (danger) SayvisRedAlert else SayvisCyan
                )
            }
        },
        dismissButton = {
            TextButton(onClick = onDismiss) {
                Text(text = dismissLabel, fontSize = 12.5.sp, color = SayvisSilverMuted)
            }
        }
    )
}

/**
 * Synchronous, offline-only translation for short tokens that appear inside a larger
 * composed string (capability tags, audit actor names, enum codes). Returns the source
 * unchanged when there is no dictionary entry, so it is always safe to call.
 */
@Composable
fun offlineTranslate(source: String): String {
    val strings = LocalStrings.current
    val bridge = LocalTranslation.current
    return remember(source, strings.fa) {
        if (!strings.fa || bridge == null || source.isBlank()) {
            source
        } else {
            bridge.offline(source).text
        }
    }
}
