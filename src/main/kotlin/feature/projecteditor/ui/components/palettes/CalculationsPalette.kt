package feature.projecteditor.ui.components.palettes

import androidx.compose.foundation.layout.*
import androidx.compose.material.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier

@Composable
fun CalculationsPalette() {
    Box(modifier = Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
        Text("Здесь будут расчеты (Токи КЗ, Подбор оборудования)")
    }
}
