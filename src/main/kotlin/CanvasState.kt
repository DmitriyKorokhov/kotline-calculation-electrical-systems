import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.setValue
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.GraphicsLayerScope
import androidx.compose.ui.input.pointer.PointerEvent

/**
 * Manages the state of the canvas camera, including zoom and pan (offset).
 * @param initialZoom The starting zoom level.
 * @param minZoom The minimum allowed zoom level.
 * @param maxZoom The maximum allowed zoom level.
 */
class CameraState(
    initialZoom: Float = 1f,
    private val minZoom: Float = 0.2f,
    private val maxZoom: Float = 3f
) {
    // Current zoom level of the camera.
    var zoom by mutableStateOf(initialZoom)
        private set

    // Current offset (pan) of the camera.
    var offset by mutableStateOf(Offset.Zero)
        private set

    /**
     * Applies the current camera transformations (scale and translation) to a graphics layer.
     * This is used to transform the entire canvas view.
     */
    fun applyTo(layer: GraphicsLayerScope) {
        layer.scaleX = zoom
        layer.scaleY = zoom
        layer.translationX = offset.x
        layer.translationY = offset.y
    }

    /**
     * Pans the camera by a given amount.
     * @param amount The vector to pan by.
     */
    fun pan(amount: Offset) {
        offset += amount
    }

    /**
     * Zooms the camera, adjusting the offset to keep the point under the mouse stationary.
     * @param event The pointer event, used to get the mouse position.
     * @param delta The amount to zoom by (positive for zoom in, negative for zoom out).
     */
    fun zoom(event: PointerEvent, delta: Float) {
        val oldZoom = zoom
        zoom = (zoom + delta * 0.1f * zoom).coerceIn(minZoom, maxZoom)

        // Adjust offset to zoom towards the mouse cursor
        val mousePos = event.changes.first().position
        val zoomRatio = zoom / oldZoom
        offset = (offset - mousePos) * zoomRatio + mousePos
    }

    /**
     * Converts screen coordinates to world coordinates.
     * This is essential for correctly interacting with objects on the canvas after panning or zooming.
     * @param screenPos The position on the screen.
     * @return The corresponding position in the "world" (on the canvas).
     */
    fun screenToWorld(screenPos: Offset): Offset {
        return (screenPos - offset) / zoom
    }
}
