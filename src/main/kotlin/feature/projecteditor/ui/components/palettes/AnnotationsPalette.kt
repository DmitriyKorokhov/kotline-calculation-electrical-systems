package feature.projecteditor.ui.components.palettes

import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.horizontalScroll
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.BasicTextField
import androidx.compose.material.*
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.ArrowDropDown
import androidx.compose.material.icons.filled.KeyboardArrowDown
import androidx.compose.material.icons.filled.KeyboardArrowUp
import androidx.compose.material.icons.filled.Search
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.font.FontStyle
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextDecoration
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import core.view.CompactOutlinedTextField
import feature.projecteditor.domain.CalloutNode
import feature.projecteditor.domain.TextNode
import feature.projecteditor.state.CanvasToolMode
import feature.projecteditor.state.ProjectCanvasState

@Composable
fun AnnotationsPalette(state: ProjectCanvasState) {
    // 1. СМОТРИМ ТОЛЬКО НА ТОТ УЗЕЛ, КОТОРЫЙ СЕЙЧАС РЕДАКТИРУЕТСЯ
    val editingNode = state.nodes.find { it.id == state.inlineEditingNodeId }
    val editingTextNode = if (editingNode is TextNode || editingNode is CalloutNode) editingNode else null

    // Читаем из редактируемой модели ИЛИ из настроек по умолчанию
    val displayFontSize = (editingTextNode as? TextNode)?.fontSize ?: (editingTextNode as? CalloutNode)?.fontSize ?: state.defaultFontSize
    val displayIsBold = (editingTextNode as? TextNode)?.isBold ?: (editingTextNode as? CalloutNode)?.isBold ?: state.defaultIsBold
    val displayIsItalic = (editingTextNode as? TextNode)?.isItalic ?: (editingTextNode as? CalloutNode)?.isItalic ?: state.defaultIsItalic
    val displayIsUnderline = (editingTextNode as? TextNode)?.isUnderline ?: (editingTextNode as? CalloutNode)?.isUnderline ?: state.defaultIsUnderline
    val displayIsStrikethrough = (editingTextNode as? TextNode)?.isStrikethrough ?: (editingTextNode as? CalloutNode)?.isStrikethrough ?: state.defaultIsStrikethrough
    val displayAlign = (editingTextNode as? TextNode)?.align ?: state.defaultAlign
    val displayColor = (editingTextNode as? TextNode)?.colorArgb ?: (editingTextNode as? CalloutNode)?.colorArgb ?: state.defaultColorArgb
    val displayHasBg = (editingTextNode as? TextNode)?.hasBackground ?: (editingTextNode as? CalloutNode)?.hasBackground ?: state.defaultHasBackground
    val displayBgColor = (editingTextNode as? TextNode)?.backgroundColorArgb ?: (editingTextNode as? CalloutNode)?.backgroundColorArgb ?: state.defaultBackgroundColorArgb

    var fontDropdownExpanded by remember { mutableStateOf(false) }
    var selectedFont by remember { mutableStateOf("ISOCPEUR") }

    var sizeInputValue by remember(displayFontSize) { mutableStateOf(displayFontSize.toInt().toString()) }

    Row(
        modifier = Modifier
            .fillMaxWidth()
            .padding(horizontal = 16.dp, vertical = 4.dp)
            .horizontalScroll(rememberScrollState()),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.spacedBy(24.dp)
    ) {
        // --- А: ТЕКСТ ---
        RectangularToolButton(
            label = "Текст",
            isActive = state.currentToolMode == CanvasToolMode.ADD_TEXT,
            onClick = { state.currentToolMode = if (state.currentToolMode == CanvasToolMode.ADD_TEXT) CanvasToolMode.SELECT else CanvasToolMode.ADD_TEXT }
        ) {
            Text(
                text = "АБС",
                fontSize = 22.sp,
                fontWeight = FontWeight.Bold,
                color = Color.Black,
                modifier = Modifier.offset(x = 0.dp, y = (-6).dp)
            )
        }

        // --- З: ШРИФТ И ПОИСК ---
        Column(
            verticalArrangement = Arrangement.spacedBy(8.dp),
            modifier = Modifier.width(160.dp)
        ) {
            // Выпадающий список шрифта
            Box(modifier = Modifier.fillMaxWidth()) {
                CompactOutlinedTextField(
                    label = "",
                    value = selectedFont,
                    onValueChange = {},
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(42.dp)
                        .background(Color.White, RoundedCornerShape(4.dp)),
                    textColor = Color.Black,
                    focusedBorderColor = Color.LightGray,
                    unfocusedBorderColor = Color.LightGray,
                    fontSizeSp = 13,
                    contentPadding = PaddingValues(horizontal = 8.dp, vertical = 0.dp),
                    trailingIcon = { Icon(Icons.Default.ArrowDropDown, contentDescription = null, tint = Color.Black) }
                )
                Box(
                    modifier = Modifier
                        .matchParentSize()
                        .clickable { fontDropdownExpanded = true }
                )
                DropdownMenu(
                    expanded = fontDropdownExpanded,
                    onDismissRequest = { fontDropdownExpanded = false },
                    modifier = Modifier.width(160.dp)
                ) {
                    DropdownMenuItem(onClick = { selectedFont = "ISOCPEUR"; fontDropdownExpanded = false }) { Text("ISOCPEUR") }
                    DropdownMenuItem(onClick = { selectedFont = "Standard"; fontDropdownExpanded = false }) { Text("Standard") }
                }
            }

            // Поле поиска текста
            BasicTextField(
                value = state.searchQuery, // Теперь берем из state
                onValueChange = { state.updateSearch(it) }, // Обновляем через state
                singleLine = true,
                textStyle = TextStyle(fontSize = 13.sp, color = Color.Black),
                modifier = Modifier.fillMaxWidth().height(42.dp),
                decorationBox = { innerTextField ->
                    Row(
                        verticalAlignment = Alignment.CenterVertically,
                        modifier = Modifier
                            .fillMaxSize()
                            .border(1.dp, Color.LightGray, RoundedCornerShape(4.dp))
                            .background(Color.White, RoundedCornerShape(4.dp))
                            .padding(horizontal = 8.dp)
                    ) {
                        Icon(Icons.Default.Search, contentDescription = "Поиск", tint = Color.Black, modifier = Modifier.size(22.dp))
                        Spacer(modifier = Modifier.width(6.dp))
                        Box(
                            modifier = Modifier.weight(1f).fillMaxHeight(),
                            contentAlignment = Alignment.CenterStart
                        ) {
                            if (state.searchQuery.isEmpty()) {
                                Text("Поиск", color = Color.DarkGray, fontSize = 13.sp)
                            }
                            innerTextField()
                        }

                        if (state.searchResults.isNotEmpty()) {
                            Text(
                                text = "${state.currentSearchIndex + 1} / ${state.searchResults.size}",
                                fontSize = 12.sp,
                                color = Color.Black,
                                modifier = Modifier.padding(horizontal = 4.dp)
                            )
                            Icon(
                                imageVector = Icons.Default.KeyboardArrowUp,
                                contentDescription = "Prev",
                                tint = Color.Black,
                                modifier = Modifier.clickable { state.prevSearchResult() }
                            )
                            Icon(
                                imageVector = Icons.Default.KeyboardArrowDown,
                                contentDescription = "Next",
                                tint = Color.Black,
                                modifier = Modifier.clickable { state.nextSearchResult() }
                            )
                        }
                    }
                }
            )
        }

        // --- В: РАЗМЕР ТЕКСТА ---
        Column(horizontalAlignment = Alignment.CenterHorizontally, verticalArrangement = Arrangement.spacedBy(6.dp)) {
            Text("Размер текста", fontSize = 13.sp, color = Color.White, fontWeight = FontWeight.Medium)
            Row(
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                // Кнопка "-"
                Box(
                    modifier = Modifier
                        .size(28.dp)
                        .clip(RoundedCornerShape(4.dp))
                        .background(MaterialTheme.colors.primary)
                        .clickable {
                            val newSize = maxOf(2f, displayFontSize - 2f)
                            state.defaultFontSize = newSize
                            state.updateInlineEditingTextProperties(fontSize = newSize) // Меняем целевой узел
                        },
                    contentAlignment = Alignment.Center
                ) {
                    Text("-", fontWeight = FontWeight.Bold, fontSize = 18.sp, color = Color.White)
                }

                // Значение
                BasicTextField(
                    value = sizeInputValue,
                    onValueChange = { newValue ->
                        sizeInputValue = newValue
                        val newSize = newValue.toFloatOrNull()
                        if (newSize != null && newSize > 0f) {
                            state.defaultFontSize = newSize
                            state.updateInlineEditingTextProperties(fontSize = newSize)
                        }
                    },
                    singleLine = true,
                    textStyle = TextStyle(
                        fontSize = 14.sp,
                        fontWeight = FontWeight.Bold,
                        color = Color.Black,
                        textAlign = TextAlign.Center
                    ),
                    modifier = Modifier
                        .height(28.dp)
                        .width(40.dp)
                        .border(1.dp, Color.LightGray, RoundedCornerShape(4.dp))
                        .background(Color.White, RoundedCornerShape(4.dp)),
                    decorationBox = { innerTextField ->
                        Box(contentAlignment = Alignment.Center, modifier = Modifier.fillMaxSize()) { innerTextField() }
                    }
                )

                // Кнопка "+"
                Box(
                    modifier = Modifier
                        .size(28.dp)
                        .clip(RoundedCornerShape(4.dp))
                        .background(MaterialTheme.colors.primary)
                        .clickable {
                            val newSize = displayFontSize + 2f
                            state.defaultFontSize = newSize
                            state.updateInlineEditingTextProperties(fontSize = newSize)
                        },
                    contentAlignment = Alignment.Center
                ) {
                    Text("+", fontWeight = FontWeight.Bold, fontSize = 18.sp, color = Color.White)
                }
            }
        }

        // --- Г: СТИЛИ ---
        Column(verticalArrangement = Arrangement.spacedBy(4.dp)) {
            Row(horizontalArrangement = Arrangement.spacedBy(4.dp)) {
                FormatToggleButton(text = "B", isBold = true, isActive = displayIsBold) {
                    state.defaultIsBold = !displayIsBold
                    state.updateInlineEditingTextProperties(isBold = state.defaultIsBold)
                }
                FormatToggleButton(text = "I", isItalic = true, isActive = displayIsItalic) {
                    state.defaultIsItalic = !displayIsItalic
                    state.updateInlineEditingTextProperties(isItalic = state.defaultIsItalic)
                }
            }
            Row(horizontalArrangement = Arrangement.spacedBy(4.dp)) {
                FormatToggleButton(text = "U", isUnderline = true, isActive = displayIsUnderline) {
                    state.defaultIsUnderline = !displayIsUnderline
                    state.updateInlineEditingTextProperties(isUnderline = state.defaultIsUnderline)
                }
                FormatToggleButton(text = "S", isStrikethrough = true, isActive = displayIsStrikethrough) {
                    state.defaultIsStrikethrough = !displayIsStrikethrough
                    state.updateInlineEditingTextProperties(isStrikethrough = state.defaultIsStrikethrough)
                }
            }
        }

        // --- Д: ВЫРАВНИВАНИЕ ---
        Column(verticalArrangement = Arrangement.spacedBy(4.dp)) {
            AlignToggleButton(type = 0, isActive = displayAlign == 0) {
                state.defaultAlign = 0
                state.updateInlineEditingTextProperties(align = 0)
            }
            AlignToggleButton(type = 1, isActive = displayAlign == 1) {
                state.defaultAlign = 1
                state.updateInlineEditingTextProperties(align = 1)
            }
            AlignToggleButton(type = 2, isActive = displayAlign == 2) {
                state.defaultAlign = 2
                state.updateInlineEditingTextProperties(align = 2)
            }
        }

        // --- Е: ЦВЕТ ТЕКСТА ---
        Column(horizontalAlignment = Alignment.CenterHorizontally, verticalArrangement = Arrangement.spacedBy(6.dp)) {
            Text("Цвет текста", fontSize = 13.sp, color = Color.White, fontWeight = FontWeight.Medium)
            Column(verticalArrangement = Arrangement.spacedBy(6.dp)) {
                Row(horizontalArrangement = Arrangement.spacedBy(6.dp)) {
                    val colors1 = listOf(0xFFFFFFFF, 0xFF000000, 0xFFD32F2F, 0xFF1976D2, 0xFF388E3C, 0xFF757575, 0xFFF57C00)
                    colors1.forEach { colorVal ->
                        ColorButton(colorVal, displayColor) {
                            state.defaultColorArgb = colorVal
                            state.updateInlineEditingTextProperties(colorArgb = colorVal)
                        }
                    }
                }
                Row(horizontalArrangement = Arrangement.spacedBy(6.dp)) {
                    val colors2 = listOf(0xFF7B1FA2, 0xFFFBC02D, 0xFF00BCD4, 0xFF8D6E63, 0xFFE91E63, 0xFFCDDC39, 0xFF607D8B)
                    colors2.forEach { colorVal ->
                        ColorButton(colorVal, displayColor) {
                            state.defaultColorArgb = colorVal
                            state.updateInlineEditingTextProperties(colorArgb = colorVal)
                        }
                    }
                }
            }
        }

        // --- Ж: ФОН ДЛЯ ТЕКСТА ---
        Column(horizontalAlignment = Alignment.CenterHorizontally, verticalArrangement = Arrangement.spacedBy(8.dp)) {
            Text("Фон для текста", fontSize = 13.sp, color = Color.White, fontWeight = FontWeight.Medium)
            Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                BgColorButton(isTransparent = true, color = 0xFFFFFFFF, isActive = !displayHasBg) {
                    state.defaultHasBackground = false
                    state.updateInlineEditingTextProperties(hasBackground = false)
                }
                BgColorButton(isTransparent = false, color = 0xFFFFFFFF, isActive = displayHasBg && displayBgColor == 0xFFFFFFFF) {
                    state.defaultHasBackground = true
                    state.defaultBackgroundColorArgb = 0xFFFFFFFF
                    state.updateInlineEditingTextProperties(hasBackground = true, backgroundColorArgb = 0xFFFFFFFF)
                }
                BgColorButton(isTransparent = false, color = 0xFF424242, isActive = displayHasBg && displayBgColor == 0xFF424242) {
                    state.defaultHasBackground = true
                    state.defaultBackgroundColorArgb = 0xFF424242
                    state.updateInlineEditingTextProperties(hasBackground = true, backgroundColorArgb = 0xFF424242)
                }
            }
        }

        // --- Б, З: ВЫНОСКА ---
        val displayStartStyle = (editingTextNode as? CalloutNode)?.startStyle ?: state.defaultCalloutStartStyle

        Row(
            horizontalArrangement = Arrangement.spacedBy(4.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            SquareToolButton(
                label = "Выноска",
                isActive = state.currentToolMode == CanvasToolMode.ADD_CALLOUT,
                onClick = { state.currentToolMode = if (state.currentToolMode == CanvasToolMode.ADD_CALLOUT) CanvasToolMode.SELECT else CanvasToolMode.ADD_CALLOUT }
            ) {
                val iconColor = Color.Black // Иконка черная
                Canvas(modifier = Modifier.size(32.dp)) {
                    // Максимально растягиваем рисунок по холсту
                    val start = Offset(4.dp.toPx(), size.height - 4.dp.toPx())
                    val mid = Offset(size.width * 0.35f, 6.dp.toPx())
                    val end = Offset(size.width, 6.dp.toPx()) // Длинная полка уходит в самый край

                    // Рисуем наконечник
                    when (displayStartStyle) {
                        0 -> { // Стрелка
                            val arrowLen = 8.dp.toPx() // Сделали стрелку больше
                            val angle = kotlin.math.atan2(mid.y - start.y, mid.x - start.x)
                            val p1 = Offset(start.x + arrowLen * kotlin.math.cos(angle - Math.PI/6).toFloat(), start.y + arrowLen * kotlin.math.sin(angle - Math.PI/6).toFloat())
                            val p2 = Offset(start.x + arrowLen * kotlin.math.cos(angle + Math.PI/6).toFloat(), start.y + arrowLen * kotlin.math.sin(angle + Math.PI/6).toFloat())
                            drawLine(iconColor, start, p1, strokeWidth = 2.5f)
                            drawLine(iconColor, start, p2, strokeWidth = 2.5f)
                        }
                        1 -> drawCircle(color = iconColor, radius = 5.dp.toPx(), center = start, style = Stroke(2.5f))
                        2 -> drawCircle(color = iconColor, radius = 3.5f.dp.toPx(), center = start)
                    }

                    // Сама ломаная линия
                    drawLine(color = iconColor, start = start, end = mid, strokeWidth = 2.5f)
                    drawLine(color = iconColor, start = mid, end = end, strokeWidth = 2.5f)
                }
            }

            // 3 вертикальные кнопки выбора стиля
            Column(verticalArrangement = Arrangement.spacedBy(4.dp)) {
                CalloutStyleButton(type = 0, isActive = displayStartStyle == 0) {
                    state.defaultCalloutStartStyle = 0
                    state.updateInlineEditingTextProperties(calloutStartStyle = 0)
                }
                CalloutStyleButton(type = 1, isActive = displayStartStyle == 1) {
                    state.defaultCalloutStartStyle = 1
                    state.updateInlineEditingTextProperties(calloutStartStyle = 1)
                }
                CalloutStyleButton(type = 2, isActive = displayStartStyle == 2) {
                    state.defaultCalloutStartStyle = 2
                    state.updateInlineEditingTextProperties(calloutStartStyle = 2)
                }
            }
        }
    }
}

// ==========================================
// ВСПОМОГАТЕЛЬНЫЕ КОМПОНЕНТЫ
// ==========================================

@Composable
private fun CalloutStyleButton(type: Int, isActive: Boolean, onClick: () -> Unit) {
    val bgColor = if (isActive) Color(0xFF81D4FA) else Color(0xFFE3F2FD)
    Box(
        modifier = Modifier
            .size(28.dp) // Три кнопки по 28dp + 2 отступа по 4dp = ровно 92dp (высота большой кнопки)
            .clip(RoundedCornerShape(4.dp))
            .background(bgColor)
            .border(1.dp, if (isActive) MaterialTheme.colors.primary else Color.LightGray, RoundedCornerShape(4.dp))
            .clickable(onClick = onClick),
        contentAlignment = Alignment.Center
    ) {
        Canvas(modifier = Modifier.fillMaxSize()) {
            val color = Color.Black
            val centerPt = Offset(size.width / 2, size.height / 2)
            when (type) {
                0 -> {
                    val arrowLen = 5.dp.toPx()
                    val path = androidx.compose.ui.graphics.Path().apply {
                        moveTo(centerPt.x, centerPt.y - arrowLen/2)
                        lineTo(centerPt.x - arrowLen/1.5f, centerPt.y + arrowLen/1.5f)
                        moveTo(centerPt.x, centerPt.y - arrowLen/2)
                        lineTo(centerPt.x + arrowLen/1.5f, centerPt.y + arrowLen/1.5f)
                        moveTo(centerPt.x, centerPt.y - arrowLen/2)
                        lineTo(centerPt.x, centerPt.y + arrowLen)
                    }
                    drawPath(path, color, style = Stroke(1.5f))
                }
                1 -> drawCircle(color, 4.dp.toPx(), centerPt, style = Stroke(1.5f))
                2 -> drawCircle(color, 2.5f.dp.toPx(), centerPt)
            }
        }
    }
}

@Composable
private fun RectangularToolButton(label: String, isActive: Boolean, onClick: () -> Unit, iconContent: @Composable () -> Unit) {
    val bgColor = if (isActive) Color(0xFF81D4FA) else Color(0xFFE3F2FD) // Голубой фон
    Column(
        modifier = Modifier
            .size(width = 110.dp, height = 92.dp)
            .clip(RoundedCornerShape(8.dp))
            .background(bgColor)
            .border(
                width = if (isActive) 2.dp else 1.dp,
                color = if (isActive) MaterialTheme.colors.primary else Color.LightGray,
                shape = RoundedCornerShape(8.dp)
            )
            .clickable(onClick = onClick)
            .padding(4.dp),
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.Center
    ) {
        Box(modifier = Modifier.weight(1f), contentAlignment = Alignment.Center) { iconContent() }
        Text(label, fontSize = 12.sp, color = Color.Black)
    }
}

@Composable
private fun SquareToolButton(label: String, isActive: Boolean, onClick: () -> Unit, iconContent: @Composable () -> Unit) {
    val bgColor = if (isActive) Color(0xFF81D4FA) else Color(0xFFE3F2FD)
    Column(
        modifier = Modifier
            .size(width = 80.dp, height = 92.dp)
            .clip(RoundedCornerShape(8.dp))
            .background(bgColor)
            .border(
                width = if (isActive) 2.dp else 1.dp,
                color = if (isActive) MaterialTheme.colors.primary else Color.LightGray,
                shape = RoundedCornerShape(8.dp)
            )
            .clickable(onClick = onClick)
            .padding(4.dp),
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.Center
    ) {
        Box(modifier = Modifier.weight(1f), contentAlignment = Alignment.Center) { iconContent() }
        Text(label, fontSize = 12.sp, color = Color.Black)
    }
}

@Composable
private fun FormatToggleButton(
    text: String,
    isBold: Boolean = false,
    isItalic: Boolean = false,
    isUnderline: Boolean = false,
    isStrikethrough: Boolean = false,
    isActive: Boolean,
    onClick: () -> Unit
) {
    Box(
        modifier = Modifier
            .size(26.dp)
            .clip(RoundedCornerShape(4.dp))
            .background(if (isActive) MaterialTheme.colors.primary.copy(alpha = 0.2f) else Color.Transparent)
            .clickable(onClick = onClick),
        contentAlignment = Alignment.Center
    ) {
        Text(
            text = text,
            fontWeight = if (isBold) FontWeight.Bold else FontWeight.Normal,
            fontStyle = if (isItalic) FontStyle.Italic else FontStyle.Normal,
            textDecoration = if (isUnderline) TextDecoration.Underline else if (isStrikethrough) TextDecoration.LineThrough else TextDecoration.None,
            color = MaterialTheme.colors.onSurface,
            fontSize = 15.sp
        )
    }
}

@Composable
private fun AlignToggleButton(type: Int, isActive: Boolean, onClick: () -> Unit) {
    Box(
        modifier = Modifier
            .size(24.dp)
            .clip(RoundedCornerShape(4.dp))
            .background(if (isActive) MaterialTheme.colors.primary.copy(alpha = 0.2f) else Color.Transparent)
            .clickable(onClick = onClick),
        contentAlignment = Alignment.Center
    ) {
        Canvas(modifier = Modifier.size(14.dp)) {
            val color = Color.Black
            val stroke = 1.5f
            val y1 = size.height * 0.2f
            val y2 = size.height * 0.5f
            val y3 = size.height * 0.8f

            when (type) {
                0 -> {
                    drawLine(color, Offset(0f, y1), Offset(size.width, y1), strokeWidth = stroke)
                    drawLine(color, Offset(0f, y2), Offset(size.width * 0.6f, y2), strokeWidth = stroke)
                    drawLine(color, Offset(0f, y3), Offset(size.width * 0.8f, y3), strokeWidth = stroke)
                }
                1 -> {
                    drawLine(color, Offset(0f, y1), Offset(size.width, y1), strokeWidth = stroke)
                    drawLine(color, Offset(size.width * 0.2f, y2), Offset(size.width * 0.8f, y2), strokeWidth = stroke)
                    drawLine(color, Offset(size.width * 0.1f, y3), Offset(size.width * 0.9f, y3), strokeWidth = stroke)
                }
                2 -> {
                    drawLine(color, Offset(0f, y1), Offset(size.width, y1), strokeWidth = stroke)
                    drawLine(color, Offset(size.width * 0.4f, y2), Offset(size.width, y2), strokeWidth = stroke)
                    drawLine(color, Offset(size.width * 0.2f, y3), Offset(size.width, y3), strokeWidth = stroke)
                }
            }
        }
    }
}

@Composable
private fun ColorButton(colorArgb: Long, currentColor: Long, onClick: () -> Unit) {
    val isActive = colorArgb == currentColor
    val color = Color(colorArgb)
    Box(
        modifier = Modifier
            .size(24.dp)
            .clip(CircleShape)
            .background(color)
            .border(
                width = if (isActive) 2.dp else 1.dp,
                color = if (isActive) MaterialTheme.colors.primary else Color.LightGray,
                shape = CircleShape
            )
            .clickable(onClick = onClick)
    )
}

@Composable
private fun BgColorButton(isTransparent: Boolean, color: Long, isActive: Boolean, onClick: () -> Unit) {
    val bgColor = Color(color)
    Box(
        modifier = Modifier
            .size(36.dp)
            .clip(RoundedCornerShape(4.dp))
            .background(if (isTransparent) Color.White else bgColor)
            .border(
                width = if (isActive) 2.dp else 1.dp,
                color = if (isActive) MaterialTheme.colors.primary else Color.LightGray,
                shape = RoundedCornerShape(4.dp)
            )
            .clickable(onClick = onClick),
        contentAlignment = Alignment.Center
    ) {
        if (isTransparent) {
            Canvas(modifier = Modifier.fillMaxSize()) {
                drawLine(
                    color = Color.Red.copy(alpha = 0.6f),
                    start = Offset(0f, size.height),
                    end = Offset(size.width, 0f),
                    strokeWidth = 2f
                )
            }
        }
    }
}