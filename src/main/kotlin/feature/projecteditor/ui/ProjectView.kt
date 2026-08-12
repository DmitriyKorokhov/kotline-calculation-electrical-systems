package feature.projecteditor.ui

import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.focusable
import androidx.compose.foundation.layout.*
import androidx.compose.material.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.focus.FocusRequester
import androidx.compose.ui.focus.focusRequester
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.input.key.*
import androidx.compose.ui.layout.onGloballyPositioned
import androidx.compose.ui.layout.positionInRoot
import androidx.compose.ui.text.ExperimentalTextApi
import androidx.compose.ui.text.rememberTextMeasurer
import androidx.compose.ui.unit.dp
import feature.projecteditor.domain.Point
import feature.projecteditor.state.ProjectCanvasState
import core.storage.ProjectStorage
import feature.projecteditor.ui.canvas.InteractiveCanvas
import feature.projecteditor.ui.components.CanvasContextMenu
import feature.projecteditor.ui.components.NodeContextMenu
import feature.projecteditor.ui.components.NodesPalette
import feature.projecteditor.ui.components.PaletteNodeType
import feature.projecteditor.ui.components.RenameNodeDialog
import feature.projecteditor.ui.drawing.*
import androidx.compose.ui.draw.clipToBounds
import androidx.compose.ui.input.pointer.PointerEventPass
import androidx.compose.ui.input.pointer.PointerEventType
import androidx.compose.ui.input.pointer.pointerInput

private const val NODE_WIDTH = 120f
private const val NODE_HEIGHT = 80f
private val PALETTE_HEIGHT_DP = 128.dp

@OptIn(ExperimentalTextApi::class)
@Composable
fun ProjectView(
    state: ProjectCanvasState,
    onOpenShield: (Int) -> Unit
) {
    val textMeasurer = rememberTextMeasurer()
    var paletteDragType by remember { mutableStateOf<PaletteNodeType?>(null) }
    var palettePreviewWorldPos by remember { mutableStateOf<Point?>(null) }
    var canvasTopLeft by remember { mutableStateOf(Offset.Zero) }

    val focusRequester = remember { FocusRequester() }
    LaunchedEffect(Unit) { focusRequester.requestFocus() }

    Column(
        modifier = Modifier
            .fillMaxSize()
            .focusRequester(focusRequester)
            .focusable()
            .pointerInput(Unit) {
                awaitPointerEventScope {
                    while (true) {
                        val event = awaitPointerEvent(pass = PointerEventPass.Initial)
                        if (event.type == PointerEventType.Press) {
                            focusRequester.requestFocus()
                        }
                    }
                }
            }
            .onPreviewKeyEvent { event ->
                if (event.type == KeyEventType.KeyDown && event.isCtrlPressed && event.key == Key.Z) {
                    if (event.isShiftPressed) state.redo() else state.undo()
                    return@onPreviewKeyEvent true
                }
                false
            }
    ) {
        // 2. ПАЛИТРА МОДЕЛЕЙ (Теперь это отдельный блок в Column, она больше не накладывается на Canvas)
        Surface(elevation = 8.dp, modifier = Modifier.fillMaxWidth().height(PALETTE_HEIGHT_DP), color = MaterialTheme.colors.surface) {
            NodesPalette(
                textMeasurer = textMeasurer,
                cellHeight = 110.dp,
                onStartDrag = { type, pos -> paletteDragType = type; palettePreviewWorldPos = state.screenToWorld((pos - canvasTopLeft).toPoint()) },
                onDrag = { pos -> palettePreviewWorldPos = state.screenToWorld((pos - canvasTopLeft).toPoint()) },
                onEndDrag = { pos ->
                    val worldPos = state.screenToWorld((pos - canvasTopLeft).toPoint())
                    when (paletteDragType) {
                        PaletteNodeType.SHIELD -> state.addShieldNode(worldPos)
                        PaletteNodeType.TRANSFORMER -> state.addTransformerNode(worldPos)
                        PaletteNodeType.GENERATOR -> state.addGeneratorNode(worldPos)
                        PaletteNodeType.UPS -> state.addUpsNode(worldPos)
                        PaletteNodeType.BATTERY -> state.addBatteryNode(worldPos)
                        PaletteNodeType.SOLAR_PANEL -> state.addSolarPanelNode(worldPos)
                        PaletteNodeType.INVERTER -> state.addInverterNode(worldPos)
                        null -> {}
                    }
                    paletteDragType = null; palettePreviewWorldPos = null
                },
                onCancel = { paletteDragType = null; palettePreviewWorldPos = null }
            )
        }

        // 3. РАБОЧАЯ ОБЛАСТЬ (Занимает весь оставшийся экран)
        Box(modifier = Modifier
            .weight(1f)
            .fillMaxWidth()
            .clipToBounds()
        ) {
            InteractiveCanvas(
                state = state,
                textMeasurer = textMeasurer,
                onOpenShield = onOpenShield,
                modifier = Modifier
                    .fillMaxSize()
                    .onGloballyPositioned { canvasTopLeft = it.positionInRoot() }
            )

            // ПРЕДПРОСМОТР ПРИ ПЕРЕТАСКИВАНИИ ИЗ ПАЛИТРЫ
            if (paletteDragType != null && palettePreviewWorldPos != null) {
                val previewScreen = state.worldToScreen(palettePreviewWorldPos!!).toOffset()
                Canvas(modifier = Modifier.fillMaxSize()) { // Убран отступ padding
                    val w = NODE_WIDTH * state.scale
                    val h = NODE_HEIGHT * state.scale
                    val centerOffset = Offset(previewScreen.x - w / 2, previewScreen.y - h / 2)
                    when (paletteDragType) {
                        PaletteNodeType.SHIELD -> drawShieldShape(centerOffset, Size(w, h))
                        PaletteNodeType.TRANSFORMER -> drawTransformerShape(previewScreen, 40f * state.scale)
                        PaletteNodeType.GENERATOR -> drawGeneratorShape(textMeasurer, previewScreen, 50f * state.scale)
                        PaletteNodeType.UPS -> drawUpsShape(textMeasurer, centerOffset, Size(w, h))
                        PaletteNodeType.BATTERY -> drawBatteryShape(centerOffset, Size(w, h))
                        PaletteNodeType.SOLAR_PANEL -> drawSolarPanelShape(centerOffset, Size(w, h))
                        PaletteNodeType.INVERTER -> drawInverterShape(textMeasurer, centerOffset, Size(w, h))
                        null -> {}
                    }
                }
            }

            NodeContextMenu(state, onOpenShield)
            CanvasContextMenu(state)
            RenameNodeDialog(state)
        }
    }
}