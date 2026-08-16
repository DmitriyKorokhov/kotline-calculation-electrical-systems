package feature.projecteditor.ui

import androidx.compose.foundation.Canvas
import androidx.compose.foundation.focusable
import androidx.compose.foundation.layout.*
import androidx.compose.material.*
import androidx.compose.runtime.*
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
import feature.projecteditor.ui.components.EditorTab
import feature.projecteditor.ui.components.EditorTabsPanel
import feature.projecteditor.ui.components.palettes.*
import feature.projecteditor.ui.canvas.InteractiveCanvas
import feature.projecteditor.ui.menus.NodeContextMenu
import feature.projecteditor.ui.components.palettes.NodesPalette
import feature.projecteditor.ui.components.palettes.PaletteNodeType
import feature.projecteditor.ui.menus.RenameNodeDialog
import feature.projecteditor.ui.drawing.*
import androidx.compose.ui.draw.clipToBounds
import androidx.compose.ui.graphics.drawscope.scale
import androidx.compose.ui.graphics.drawscope.withTransform
import androidx.compose.ui.input.pointer.PointerEventPass
import androidx.compose.ui.input.pointer.PointerEventType
import androidx.compose.ui.input.pointer.pointerInput
import feature.projecteditor.ui.menus.ConnectionContextMenu
import feature.projecteditor.ui.menus.ItRackRowSettingsWindow
import feature.projecteditor.ui.menus.MultiSelectContextMenu
import feature.projecteditor.ui.utils.toOffset
import feature.projecteditor.ui.utils.toPoint

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

    var selectedTab by remember { mutableStateOf(EditorTab.EQUIPMENT) }

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
                if (event.type == KeyEventType.KeyDown) {
                    // Обработка ESCAPE (снять выделение)
                    if (event.key == Key.Escape) {
                        state.clearSelection()
                        return@onPreviewKeyEvent true
                    }
                    // Обработка DELETE или BACKSPACE (Удалить выделенное)
                    if (event.key == Key.Delete || event.key == Key.Backspace) {
                        // Проверяем, есть ли что удалять, чтобы зря не засорять историю (Undo/Redo)
                        if (state.selectedNodeIds.isNotEmpty() || state.selectedConnections.isNotEmpty()) {
                            state.deleteSelectedNodes()
                        }
                        return@onPreviewKeyEvent true
                    }
                    // Обработка Ctrl + Z (Отмена/Повтор)
                    if (event.isCtrlPressed && event.key == Key.Z) {
                        if (event.isShiftPressed) state.redo() else state.undo()
                        return@onPreviewKeyEvent true
                    }
                }
                false
            }
    ) {

        // 1. ПАНЕЛЬ ВЫБОРА ВКЛАДОК (Риббон-меню)
        EditorTabsPanel(
            selectedTab = selectedTab,
            onTabSelected = { selectedTab = it }
        )

        Divider(color = MaterialTheme.colors.onSurface.copy(alpha = 0.1f), thickness = 1.dp)

        // 2. ДИНАМИЧЕСКАЯ ПАЛИТРА ИНСТРУМЕНТОВ
        Surface(
            elevation = 8.dp,
            modifier = Modifier.fillMaxWidth().height(PALETTE_HEIGHT_DP),
            color = MaterialTheme.colors.surface
        ) {
            // Переключаем панели в зависимости от выбранной вкладки
            when (selectedTab) {
                EditorTab.EQUIPMENT -> {
                    // Твоя текущая панель с оборудованием
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
                                PaletteNodeType.SYSTEM -> state.addSystemNode(worldPos)
                                PaletteNodeType.IT_RACK_ROW -> state.addItRackRowNode(worldPos)
                                PaletteNodeType.RECTIFIER -> state.addRectifierNode(worldPos)
                                null -> {}
                            }
                            paletteDragType = null; palettePreviewWorldPos = null
                        },
                        onCancel = { paletteDragType = null; palettePreviewWorldPos = null }
                    )
                }
                EditorTab.TOOLS -> ToolsPalette()
                EditorTab.ANNOTATIONS -> AnnotationsPalette()
                EditorTab.CALCULATIONS -> CalculationsPalette()
                EditorTab.PROJECT -> ProjectPalette()
                EditorTab.COLLABORATION -> CollaborationPalette()
            }
        }

        // 3. РАБОЧАЯ ОБЛАСТЬ
        Box(
            modifier = Modifier
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
                Canvas(modifier = Modifier.fillMaxSize()) {
                    val w = NODE_WIDTH * state.scale
                    val h = NODE_HEIGHT * state.scale
                    val centerOffset = Offset(previewScreen.x - w / 2, previewScreen.y - h / 2)
                    when (paletteDragType) {
                        PaletteNodeType.SHIELD -> drawShieldShape(centerOffset, Size(w, h))
                        PaletteNodeType.TRANSFORMER -> drawTransformerShape(previewScreen, 40f * state.scale)
                        PaletteNodeType.GENERATOR -> drawGeneratorShape(previewScreen, 50f * state.scale)
                        PaletteNodeType.UPS -> drawUpsShape(textMeasurer, centerOffset, Size(w, h))
                        PaletteNodeType.BATTERY -> drawBatteryShape(centerOffset, Size(w, h))
                        PaletteNodeType.SOLAR_PANEL -> drawSolarPanelShape(centerOffset, Size(w, h))
                        PaletteNodeType.INVERTER -> drawInverterShape(textMeasurer, centerOffset, Size(w, h))
                        PaletteNodeType.SYSTEM -> drawSystemShape( previewScreen, 50f * state.scale)
                        PaletteNodeType.IT_RACK_ROW -> {
                            val dummyNode = feature.projecteditor.domain.ItRackRowNode(0, position = Point.Zero)
                            withTransform({
                                scale(state.scale, pivot = previewScreen)
                            }) {
                                drawItRackRowShape(previewScreen, dummyNode)
                            }
                        }
                        PaletteNodeType.RECTIFIER -> drawRectifierShape(textMeasurer, centerOffset, Size(w, h))
                        null -> {}
                    }
                }
            }

            NodeContextMenu(state, onOpenShield)
            MultiSelectContextMenu(state)
            ConnectionContextMenu(state)
            RenameNodeDialog(state)
            if (state.showRackSettingsDialog) {
                ItRackRowSettingsWindow(
                    state = state,
                    onDismiss = { state.showRackSettingsDialog = false }
                )
            }
        }
    }
}