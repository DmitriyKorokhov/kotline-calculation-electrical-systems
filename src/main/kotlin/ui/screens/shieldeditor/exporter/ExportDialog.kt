package ui.screens.shieldeditor.exporter

import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.gestures.detectDragGestures
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.*
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.ArrowDropDown
import androidx.compose.material.icons.filled.Close
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.shadow
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.input.pointer.PointerIcon
import androidx.compose.ui.input.pointer.pointerHoverIcon
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.IntOffset
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.max
import ui.screens.shieldeditor.ShieldData
import java.awt.Cursor
import kotlin.math.roundToInt

@Composable
fun ExportDialog(
    data: ShieldData,
    onDismissRequest: () -> Unit,
    onExportAction: (type: String, format: String) -> Unit
) {
    val density = LocalDensity.current

    // --- Window Position & Size State ---
    var offsetX by remember { mutableStateOf(200f) }
    var offsetY by remember { mutableStateOf(150f) }
    var widthDp by remember { mutableStateOf(500.dp) }
    // Уменьшили высоту по умолчанию, так как убрали картинку
    var heightDp by remember { mutableStateOf(450.dp) }

    val minWidth = 450.dp
    // Уменьшили минимальную высоту
    val minHeight = 350.dp
    val resizeHandleSize = 8.dp

    // --- Logic State ---
    val exportTypes = listOf("DWG", "PDF")
    var selectedExportType by remember { mutableStateOf(exportTypes[0]) }
    var isTypeDropdownExpanded by remember { mutableStateOf(false) }

    val exportFormats = listOf("A3x3", "A3x4")
    var selectedFormat by remember { mutableStateOf(exportFormats[0]) }
    var isFormatDropdownExpanded by remember { mutableStateOf(false) }

    Box(
        modifier = Modifier
            .offset { IntOffset(offsetX.roundToInt(), offsetY.roundToInt()) }
            .size(widthDp, heightDp)
            .shadow(16.dp, RoundedCornerShape(4.dp))
            .background(MaterialTheme.colors.surface, RoundedCornerShape(4.dp))
            .border(1.dp, Color.Gray.copy(alpha = 0.3f), RoundedCornerShape(4.dp))
    ) {
        Column(modifier = Modifier.fillMaxSize()) {
            // --- Header (Draggable) ---
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .height(40.dp)
                    .background(MaterialTheme.colors.primary.copy(alpha = 0.08f))
                    .pointerInput(Unit) {
                        detectDragGestures { change, dragAmount ->
                            change.consume()
                            offsetX += dragAmount.x
                            offsetY += dragAmount.y
                        }
                    },
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.SpaceBetween
            ) {
                Text(
                    text = "Экспорт схемы",
                    style = MaterialTheme.typography.subtitle1,
                    fontWeight = FontWeight.Bold,
                    modifier = Modifier.padding(start = 16.dp)
                )
                IconButton(onClick = onDismissRequest, modifier = Modifier.size(40.dp)) {
                    Icon(Icons.Default.Close, contentDescription = "Закрыть", tint = MaterialTheme.colors.onSurface.copy(alpha = 0.6f))
                }
            }
            Divider(color = Color.Gray.copy(alpha = 0.2f))

            // --- Content ---
            Column(
                modifier = Modifier
                    .weight(1f)
                    .padding(16.dp)
                    .verticalScroll(rememberScrollState()),
                verticalArrangement = Arrangement.spacedBy(16.dp)
            ) {
                // 1. Имя щита (Фиксированная ширина 300)
                Column {
                    Text("Имя щита", style = MaterialTheme.typography.caption, color = Color.Gray)
                    OutlinedTextField(
                        value = data.shieldName.ifBlank { "Без названия" },
                        onValueChange = {},
                        readOnly = true,
                        enabled = false,
                        modifier = Modifier.width(300.dp),
                        colors = TextFieldDefaults.outlinedTextFieldColors(
                            disabledTextColor = MaterialTheme.colors.onSurface,
                            disabledBorderColor = MaterialTheme.colors.onSurface.copy(alpha = 0.12f)
                        )
                    )
                }

                // Строка с двумя выпадающими списками
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(24.dp)
                ) {
                    // 2. Тип экспорта (Фиксированная ширина 150)
                    Column {
                        Text("Тип экспорта", style = MaterialTheme.typography.caption, color = Color.Gray)
                        Box(modifier = Modifier.width(150.dp)) {
                            OutlinedTextField(
                                value = selectedExportType,
                                onValueChange = {},
                                readOnly = true,
                                modifier = Modifier.fillMaxWidth(),
                                trailingIcon = {
                                    Icon(Icons.Default.ArrowDropDown, "Выбрать", Modifier.clickable { isTypeDropdownExpanded = true })
                                }
                            )
                            Box(
                                modifier = Modifier
                                    .matchParentSize()
                                    .clickable { isTypeDropdownExpanded = true }
                            )
                            DropdownMenu(
                                expanded = isTypeDropdownExpanded,
                                onDismissRequest = { isTypeDropdownExpanded = false },
                                modifier = Modifier.width(150.dp)
                            ) {
                                exportTypes.forEach { type ->
                                    DropdownMenuItem(onClick = {
                                        selectedExportType = type
                                        isTypeDropdownExpanded = false
                                    }) {
                                        Text(type)
                                    }
                                }
                            }
                        }
                    }

                    // 3. Формат листа (Фиксированная ширина 150)
                    Column {
                        Text("Формат листа", style = MaterialTheme.typography.caption, color = Color.Gray)
                        Box(modifier = Modifier.width(150.dp)) {
                            OutlinedTextField(
                                value = selectedFormat,
                                onValueChange = {},
                                readOnly = true,
                                modifier = Modifier.fillMaxWidth(),
                                trailingIcon = {
                                    Icon(Icons.Default.ArrowDropDown, "Выбрать", Modifier.clickable { isFormatDropdownExpanded = true })
                                }
                            )
                            Box(
                                modifier = Modifier
                                    .matchParentSize()
                                    .clickable { isFormatDropdownExpanded = true }
                            )
                            DropdownMenu(
                                expanded = isFormatDropdownExpanded,
                                onDismissRequest = { isFormatDropdownExpanded = false },
                                modifier = Modifier.width(150.dp)
                            ) {
                                exportFormats.forEach { format ->
                                    DropdownMenuItem(onClick = {
                                        selectedFormat = format
                                        isFormatDropdownExpanded = false
                                    }) {
                                        Text(format)
                                    }
                                }
                            }
                        }
                    }
                }

                // 4. Штамп
                Column {
                    Text("Основная надпись (Штамп)", style = MaterialTheme.typography.caption, color = Color.Gray)
                    Row(
                        modifier = Modifier
                            .width(400.dp)
                            .border(1.dp, Color.Gray.copy(alpha = 0.4f), RoundedCornerShape(4.dp))
                            .padding(12.dp),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Text("Стандартный штамп", style = MaterialTheme.typography.body2)
                        Button(
                            onClick = { /* TODO: Добавить логику штампа */ },
                            colors = ButtonDefaults.buttonColors(backgroundColor = MaterialTheme.colors.secondaryVariant)
                        ) {
                            Text("Добавить", color = Color.White)
                        }
                    }
                }
            }

            Divider(color = Color.Gray.copy(alpha = 0.2f))

            // --- Footer Buttons ---
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(16.dp),
                horizontalArrangement = Arrangement.End
            ) {
                OutlinedButton(onClick = onDismissRequest) {
                    Text("Отмена")
                }
                Spacer(modifier = Modifier.width(12.dp))

                Button(onClick = {
                    onExportAction(selectedExportType, selectedFormat)
                    onDismissRequest()
                }) {
                    Text("Экспорт")
                }
            }
        }

        // --- Resizing Handles ---
        Box(
            modifier = Modifier.align(Alignment.CenterEnd).fillMaxHeight().width(resizeHandleSize)
                .pointerHoverIcon(PointerIcon(Cursor(Cursor.E_RESIZE_CURSOR)))
                .pointerInput(Unit) { detectDragGestures { change, drag -> change.consume(); widthDp = max(widthDp + with(density){drag.x.toDp()}, minWidth) } }
        )
        Box(
            modifier = Modifier.align(Alignment.BottomCenter).fillMaxWidth().height(resizeHandleSize)
                .pointerHoverIcon(PointerIcon(Cursor(Cursor.S_RESIZE_CURSOR)))
                .pointerInput(Unit) { detectDragGestures { change, drag -> change.consume(); heightDp = max(heightDp + with(density){drag.y.toDp()}, minHeight) } }
        )
        Box(
            modifier = Modifier.align(Alignment.BottomEnd).size(resizeHandleSize)
                .pointerHoverIcon(PointerIcon(Cursor(Cursor.SE_RESIZE_CURSOR)))
                .pointerInput(Unit) { detectDragGestures { change, drag -> change.consume(); widthDp = max(widthDp + with(density){drag.x.toDp()}, minWidth); heightDp = max(heightDp + with(density){drag.y.toDp()}, minHeight) } }
        )

        Box(
            modifier = Modifier
                .align(Alignment.BottomEnd)
                .size(24.dp)
                .pointerHoverIcon(PointerIcon(Cursor(Cursor.SE_RESIZE_CURSOR)))
                .pointerInput(Unit) {
                    detectDragGestures { change, dragAmount ->
                        change.consume()
                        widthDp = max(widthDp + with(density) { dragAmount.x.toDp() }, minWidth)
                        heightDp = max(heightDp + with(density) { dragAmount.y.toDp() }, minHeight)
                    }
                }
        ) {
            Canvas(modifier = Modifier.fillMaxSize().padding(4.dp)) {
                val color = Color.Gray.copy(alpha = 0.5f)
                val w = size.width
                val h = size.height
                drawLine(color, Offset(w, h - 4), Offset(w - 4, h), strokeWidth = 2f)
                drawLine(color, Offset(w, h - 8), Offset(w - 8, h), strokeWidth = 2f)
                drawLine(color, Offset(w, h - 12), Offset(w - 12, h), strokeWidth = 2f)
            }
        }
    }
}