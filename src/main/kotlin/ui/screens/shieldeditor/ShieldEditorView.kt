package ui.screens.shieldeditor

import androidx.compose.foundation.layout.*
import androidx.compose.material.Button
import androidx.compose.material.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp

/**
 * Экран 3: Окно проектирования конкретного щита (пока заглушка).
 * @param shieldId ID открытого щита.
 * @param onBack Лямбда-функция для возврата на экран проекта.
 */
@Composable
fun ShieldEditorView(shieldId: Int?, onBack: () -> Unit) {
    Box(
        modifier = Modifier.fillMaxSize(),
        contentAlignment = Alignment.Center
    ) {
        Column(horizontalAlignment = Alignment.CenterHorizontally) {
            Text("Окно проектирования щита", fontSize = 20.sp)
            Spacer(Modifier.height(8.dp))
            Text("ID открытого щита: $shieldId")
            Spacer(Modifier.height(24.dp))
            Button(onClick = onBack) {
                Text("Назад к проекту")
            }
        }
    }
}