package view

import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.animation.core.tween
import androidx.compose.foundation.clickable
import androidx.compose.foundation.focusable
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.text.BasicTextField
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material.OutlinedTextField
import androidx.compose.material.Text
import androidx.compose.material.TextFieldDefaults
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.input.ImeAction
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.material.ExperimentalMaterialApi
import androidx.compose.material.TextFieldDefaults.OutlinedTextFieldDecorationBox
import androidx.compose.runtime.*
import androidx.compose.ui.focus.FocusRequester
import androidx.compose.ui.focus.focusRequester
import androidx.compose.ui.focus.onFocusChanged
import androidx.compose.ui.text.input.VisualTransformation

@Composable
fun CompactOutlinedTextField(
    label: String,
    value: String,
    onValueChange: (String) -> Unit,
    modifier: Modifier = Modifier,
    contentPadding: PaddingValues = PaddingValues(horizontal = 12.dp, vertical = 6.dp), // уменьшены вертикальные паддинги
    textColor: Color = Color.White,
    focusedBorderColor: Color = Color.White,
    unfocusedBorderColor: Color = Color.LightGray,
    fontSizeSp: Int = 16, // чуть меньше по умолчанию
    singleLine: Boolean = true,
    imeAction: ImeAction = ImeAction.Done,
) {
    val focusRequester = remember { FocusRequester() }
    var isFocused by remember { mutableStateOf(false) }

    val targetAlpha = if (isFocused || value.isNotBlank()) 1f else 0.25f
    val labelAlpha by animateFloatAsState(targetValue = targetAlpha, animationSpec = tween(durationMillis = 200))

    OutlinedTextField(
        value = value,
        onValueChange = onValueChange,
        label = { Text(text = label, color = textColor.copy(alpha = labelAlpha)) },
        singleLine = singleLine,
        textStyle = TextStyle(fontSize = fontSizeSp.sp, color = textColor),
        modifier = modifier
            .focusRequester(focusRequester)
            .onFocusChanged { isFocused = it.isFocused }
            .focusable(),
        keyboardOptions = KeyboardOptions.Default.copy(imeAction = imeAction),
        colors = TextFieldDefaults.outlinedTextFieldColors(
            textColor = textColor,
            focusedBorderColor = focusedBorderColor,
            unfocusedBorderColor = unfocusedBorderColor,
            focusedLabelColor = focusedBorderColor,
            unfocusedLabelColor = unfocusedBorderColor,
            cursorColor = focusedBorderColor,
            placeholderColor = Color.LightGray
        )
    )
}