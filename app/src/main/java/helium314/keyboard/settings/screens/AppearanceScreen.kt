package helium314.keyboard.settings.screens

import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.geometry.CornerRadius
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.nativeCanvas
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.unit.dp
import dev.headunit.keyboard.KeycapShape
import dev.headunit.keyboard.RetroDriveKeySurface
import dev.headunit.keyboard.DashboardStyle
import helium314.keyboard.keyboard.KeyboardSwitcher
import helium314.keyboard.settings.SearchSettingsScreen

@Composable
fun AppearanceScreen(onClickBack: () -> Unit) {
    val ctx = LocalContext.current
    val night = androidx.compose.foundation.isSystemInDarkTheme()
    val prefs = DashboardStyle.prefs(ctx)
    var selected by remember { mutableIntStateOf(prefs.getInt("dashboard_theme", -1)) }
    var accent by remember { mutableIntStateOf(DashboardStyle.accent(ctx)) }
    var picker by remember { mutableStateOf(false) }
    fun select(index: Int) {
        selected = index
        prefs.edit().putInt("dashboard_theme", index).apply()
        accent = DashboardStyle.accent(ctx)
        KeyboardSwitcher.getInstance().setThemeNeedsReload()
    }
    SearchSettingsScreen(onClickBack = onClickBack, title = "Theme store", settings = emptyList()) {
        Box(Modifier.fillMaxWidth(), contentAlignment = Alignment.TopCenter) {
        Column(Modifier.widthIn(max = 840.dp).fillMaxWidth().verticalScroll(rememberScrollState()).padding(20.dp),
            verticalArrangement = Arrangement.spacedBy(14.dp)) {
            var sample by remember { mutableStateOf("") }
            OutlinedTextField(value = sample, onValueChange = { sample = it },
                modifier = Modifier.fillMaxWidth(), singleLine = true, label = { Text("Try the keyboard") },
                keyboardOptions = androidx.compose.foundation.text.KeyboardOptions(
                    imeAction = androidx.compose.ui.text.input.ImeAction.Search))
            Row(verticalAlignment = Alignment.CenterVertically) {
                Text("Follow system day and night", Modifier.weight(1f))
                Switch(checked = selected == -1, onCheckedChange = { select(if (it) -1 else 0) })
            }
            BoxWithConstraints(Modifier.fillMaxWidth()) {
                val columns = if (maxWidth >= 600.dp) 3 else 1
                Column(verticalArrangement = Arrangement.spacedBy(12.dp)) {
                    (0 until DashboardStyle.THEME_COUNT).chunked(columns).forEach { group ->
                        Row(horizontalArrangement = Arrangement.spacedBy(10.dp)) {
                            group.forEach { index ->
                                val style = remember(index, night) { DashboardStyle.at(index, night) }
                                val active = if (selected == -1) DashboardStyle.currentIndex(ctx) == index else selected == index
                                val shape = RoundedCornerShape(16.dp)
                                Column(Modifier.weight(1f).clip(shape)
                                    .background(MaterialTheme.colorScheme.surfaceVariant)
                                    .border(if (active) 2.dp else 0.dp, if (active) Color(accent) else Color.Transparent, shape)
                                    .clickable { select(index) }.padding(12.dp)) {
                                    Text(style.name + if (active) "   ✓" else "", style = MaterialTheme.typography.titleMedium)
                                    Spacer(Modifier.height(8.dp))
                                    PreviewCaps(style, if (active) accent else DashboardStyle.accent(ctx, index), index == DashboardStyle.RETRO_DRIVE)
                                }
                            }
                            repeat(columns - group.size) { Spacer(Modifier.weight(1f)) }
                        }
                    }
                }
            }
            Row(Modifier.fillMaxWidth().clickable { picker = true }.padding(vertical = 12.dp),
                verticalAlignment = Alignment.CenterVertically) {
                Text("Action key accent", Modifier.weight(1f), style = MaterialTheme.typography.titleMedium)
                Box(Modifier.size(36.dp).background(Color(accent), CircleShape))
            }
            var clearance by remember { mutableStateOf(prefs.getBoolean("dashboard_nav_clearance", true)) }
            Row(verticalAlignment = Alignment.CenterVertically) {
                Text("Leave room for the head-unit navigation bar", Modifier.weight(1f))
                Switch(checked = clearance, onCheckedChange = {
                    clearance = it
                    prefs.edit().putBoolean("dashboard_nav_clearance", it).apply()
                    KeyboardSwitcher.getInstance().setThemeNeedsReload()
                })
            }
        }
    }
    }
    if (picker) AccentPicker(accent, onCancel = { picker = false }) {
        accent = it
        prefs.edit().putInt(DashboardStyle.accentKey(selected), it).apply()
        KeyboardSwitcher.getInstance().setThemeNeedsReload()
        picker = false
    }
}

@Composable
private fun PreviewCaps(style: DashboardStyle, accent: Int, hardware: Boolean) {
    val ctx = LocalContext.current
    val lettering = remember(hardware) { android.graphics.Paint(android.graphics.Paint.ANTI_ALIAS_FLAG).apply {
        typeface = if (hardware) android.graphics.Typeface.MONOSPACE else DashboardStyle.font(ctx)
        textAlign = android.graphics.Paint.Align.CENTER
    } }
    Canvas(Modifier.fillMaxWidth().height(88.dp).background(Color(style.tray), RoundedCornerShape(9.dp))
        .semantics { contentDescription = style.name + " keyboard preview" }) {
        val surface = if (hardware) RetroDriveKeySurface(accent, DashboardStyle.isNight(ctx)) else null
        val u = minOf(size.width / 14.77f, size.height / 4.32f)
        val top = (size.height - 4.04f * u) / 2
        val gap = u * .11f
        val left = (size.width - u * 14.48f) / 2
        fun cap(row: Int, x: Float, width: Float, label: String, color: Int, round: Boolean = false) {
            val shape = when {
                round && label == "0" -> KeycapShape.PILL
                round -> KeycapShape.CIRCLE
                width >= 1.3f -> KeycapShape.WIDE
                else -> KeycapShape.SQUARE
            }
            val cellWidth = width * u
            val faceHeight = (u - gap).coerceAtLeast(1f)
            val faceWidth = if (shape == KeycapShape.WIDE || shape == KeycapShape.PILL) cellWidth - gap else faceHeight
            val center = Offset(left + x*u + cellWidth/2, top + u*1.01f*(row+.5f))
            val radius = shape.radius(faceHeight)
            if (surface != null) surface.draw(drawContext.canvas.nativeCanvas, center.x, center.y,
                faceWidth, faceHeight, round, label == "Done", 0f, label == "⇧", false)
            else if (shape == KeycapShape.CIRCLE) drawCircle(Color(color), radius, center)
            else drawRoundRect(Color(color), center - Offset(faceWidth/2, faceHeight/2),
                Size(faceWidth, faceHeight), CornerRadius(radius))
            lettering.color = if (color == accent) DashboardStyle.contrast(accent) else style.ink
            lettering.textSize = faceHeight*.41f
            drawContext.canvas.nativeCanvas.drawText(label, center.x,
                center.y - (lettering.ascent() + lettering.descent())/2, lettering)
        }
        listOf("qwertyuiop","asdfghjkl","zxcvbnm,.").forEachIndexed { r, chars ->
            chars.forEachIndexed { i, ch -> cap(r,i+r*.5f,1f,ch.toString(),style.face) }
        }
        cap(0,10f,1.4f,"⌫",style.face); cap(1,9.5f,1.9f,"Done",accent)
        cap(2,0f,1f,"\u21e7",style.face)
        cap(2,10f,1.4f,"⇧",style.face); cap(3,0f,1f,"⌄",style.face)
        cap(3,1f,1f,"?123",style.face)
        cap(3,2f,5.4f,"",style.face)
        "-;!?".forEachIndexed { i,ch -> cap(3,7.4f+i,1f,ch.toString(),style.face) }
        "789456123".forEachIndexed { i,ch -> cap(i/3,11.48f+i%3,1f,ch.toString(),style.number,true) }
        cap(3,11.48f,2f,"0",style.number,true); cap(3,13.48f,1f,".",style.number,true)
    }
}

@Composable
private fun AccentPicker(initial: Int, onCancel: () -> Unit, onApply: (Int) -> Unit) {
    val hsv = remember { FloatArray(3).also { android.graphics.Color.colorToHSV(initial,it) } }
    var hue by remember { mutableFloatStateOf(hsv[0]) }
    var saturation by remember { mutableFloatStateOf(hsv[1]) }
    var brightness by remember { mutableFloatStateOf(hsv[2]) }
    val color = android.graphics.Color.HSVToColor(floatArrayOf(hue,saturation,brightness))
    AlertDialog(onDismissRequest = onCancel, title = { Text("Action key accent") }, text = {
        Column(Modifier.verticalScroll(rememberScrollState())) {
            Box(Modifier.fillMaxWidth().height(44.dp).background(Color(color), RoundedCornerShape(9.dp)),
                contentAlignment = Alignment.Center) { Text("Search", color = Color(DashboardStyle.contrast(color))) }
            Spacer(Modifier.height(16.dp))
            Row(Modifier.fillMaxWidth(),horizontalArrangement = Arrangement.SpaceBetween) {
                val presets = listOf("Amber" to DashboardStyle.AMBER, "Coral" to 0xffff775d.toInt(),
                    "Ice" to 0xff8ad7ed.toInt(), "Mint" to 0xff80e1ad.toInt(), "Violet" to 0xffbaa4fc.toInt())
                presets.forEach { (name, value) ->
                    Box(Modifier.size(44.dp).clip(CircleShape).background(Color(value)).clickable {
                        android.graphics.Color.colorToHSV(value,hsv)
                        hue = hsv[0]; saturation = hsv[1]; brightness = hsv[2]
                    }.semantics { contentDescription = name })
                }
            }
            Text("Hue"); Slider(hue,{ hue = it },valueRange = 0f..359f)
            Text("Saturation"); Slider(saturation,{ saturation = it })
            Text("Brightness"); Slider(brightness,{ brightness = it })
        }
    }, confirmButton = { TextButton(onClick = { onApply(color) }) { Text("Apply") } },
        dismissButton = { TextButton(onClick = onCancel) { Text("Cancel") } })
}
