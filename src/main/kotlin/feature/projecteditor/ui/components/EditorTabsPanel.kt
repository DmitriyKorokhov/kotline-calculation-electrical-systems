package feature.projecteditor.ui.components

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.material.MaterialTheme
import androidx.compose.material.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp

enum class EditorTab(val title: String) {
    EQUIPMENT("Оборудование"),
    TOOLS("Инструменты"),
    ANNOTATIONS("Аннотации"),
    CALCULATIONS("Расчеты"),
    PROJECT("Проект"),
    COLLABORATION("Совместная работа")
}

@Composable
fun EditorTabsPanel(
    selectedTab: EditorTab,
    onTabSelected: (EditorTab) -> Unit
) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .height(32.dp)
            .background(MaterialTheme.colors.surface),
        verticalAlignment = Alignment.CenterVertically
    ) {
        EditorTab.values().forEach { tab ->
            val isSelected = selectedTab == tab

            Box(
                modifier = Modifier
                    .fillMaxHeight()
                    .clickable { onTabSelected(tab) }
                    // Легкая подсветка активной вкладки
                    .background(if (isSelected) MaterialTheme.colors.onSurface.copy(alpha = 0.08f) else Color.Transparent)
                    .padding(horizontal = 16.dp),
                contentAlignment = Alignment.Center
            ) {
                Text(
                    text = tab.title,
                    color = if (isSelected) MaterialTheme.colors.primary else MaterialTheme.colors.onSurface.copy(alpha = 0.7f),
                    fontSize = 13.sp,
                    fontWeight = if (isSelected) FontWeight.SemiBold else FontWeight.Normal
                )
            }
        }
    }
}
