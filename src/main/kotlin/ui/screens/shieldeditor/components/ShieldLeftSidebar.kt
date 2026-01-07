package ui.screens.shieldeditor.components

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.selection.selectableGroup
import androidx.compose.material.Icon
import androidx.compose.material.MaterialTheme
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.compositeOver
import androidx.compose.ui.graphics.painter.Painter
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.unit.dp

enum class SidebarTab {
    PROJECT,
    SHIELD_DATA
}

@Composable
fun ShieldLeftSidebar(
    selectedTab: SidebarTab?,
    onTabSelected: (SidebarTab) -> Unit
) {
    val sidebarBackgroundColor = Color.White.copy(alpha = 0.15f)
        .compositeOver(MaterialTheme.colors.surface)

    val activeColor = MaterialTheme.colors.primary
    val inactiveColor = MaterialTheme.colors.onSurface.copy(alpha = 0.4f)

    Column(
        modifier = Modifier
            .fillMaxHeight()
            .width(50.dp)
            .background(sidebarBackgroundColor)
            .selectableGroup(),
        horizontalAlignment = Alignment.CenterHorizontally
    ) {
        Spacer(modifier = Modifier.height(4.dp))

        // 1. Иконка "Проект" (файл project.png)
        SidebarIconItem(
            painter = painterResource("project.png"),
            description = "Проект",
            isSelected = selectedTab == SidebarTab.PROJECT,
            activeColor = activeColor,
            inactiveColor = inactiveColor,
            onClick = { onTabSelected(SidebarTab.PROJECT) }
        )

        Spacer(modifier = Modifier.height(8.dp))

        // 2. Иконка "Данные щита" (файл datashield.png)
        SidebarIconItem(
            painter = painterResource("datashield.png"),
            description = "Данные щита",
            isSelected = selectedTab == SidebarTab.SHIELD_DATA,
            activeColor = activeColor,
            inactiveColor = inactiveColor,
            onClick = { onTabSelected(SidebarTab.SHIELD_DATA) }
        )
    }
}

@Composable
private fun SidebarIconItem(
    painter: Painter, // Изменили тип с ImageVector на Painter
    description: String,
    isSelected: Boolean,
    activeColor: Color,
    inactiveColor: Color,
    onClick: () -> Unit
) {
    Box(
        modifier = Modifier
            .size(42.dp)
            .clickable(onClick = onClick),
        contentAlignment = Alignment.Center
    ) {
        Icon(
            painter = painter, // Передаем painter сюда
            contentDescription = description,
            tint = if (isSelected) activeColor else inactiveColor,
            modifier = Modifier.size(24.dp)
        )
    }
}