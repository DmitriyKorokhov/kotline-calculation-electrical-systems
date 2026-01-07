package ui.screens.shieldeditor.dialogs

// Состояния для диалоговых окон выбора оборудования

data class AtsDialogState(
    val manufacturer: String? = null,
    val series: String? = null,
    val selectedPoles: String? = null
)

data class BreakerDialogState(
    val manufacturer: String? = null,
    val series: String? = null,
    val selectedAdditions: List<String> = emptyList(),
    val selectedPoles: String? = null,
    val selectedCurve: String? = null
)

data class RcboDialogState(
    val manufacturer: String? = null,
    val series: String? = null,
    val selectedAdditions: List<String> = emptyList(),
    val selectedPoles: String? = null,
    val selectedCurve: String? = null,
    val selectedResidualCurrent: String? = null
)

data class RcdDialogState(
    val manufacturer: String? = null,
    val series: String? = null,
    val selectedPoles: String? = null,
    val selectedResidualCurrent: String? = null
)


