package view

import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.animation.core.tween
import androidx.compose.foundation.focusable
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.text.BasicTextField
import androidx.compose.material.ExperimentalMaterialApi
import androidx.compose.material.Text
import androidx.compose.material.TextFieldDefaults
import androidx.compose.runtime.*
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.SolidColor
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.input.ImeAction
import androidx.compose.ui.text.input.VisualTransformation
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.ui.focus.FocusRequester
import androidx.compose.ui.focus.focusRequester
import androidx.compose.ui.focus.onFocusChanged

@OptIn(ExperimentalMaterialApi::class)
@Composable
fun CompactOutlinedTextField(
    label: String,
    value: String,
    onValueChange: (String) -> Unit,
    modifier: Modifier = Modifier,
    contentPadding: PaddingValues = PaddingValues(horizontal = 6.dp, vertical = 2.dp),
    textColor: Color = Color.White,
    focusedBorderColor: Color = Color.White,
    unfocusedBorderColor: Color = Color.LightGray,
    fontSizeSp: Int = 16,
    singleLine: Boolean = true,
    minLines: Int = 1,
    maxLines: Int = Int.MAX_VALUE,
    imeAction: ImeAction = ImeAction.Done,
    trailingIcon: @Composable (() -> Unit)? = null
) {
    val focusRequester = remember { FocusRequester() }
    val interaction = remember { MutableInteractionSource() }
    var isFocused by remember { mutableStateOf(false) }
    val targetAlpha = if (isFocused || value.isNotBlank()) 1f else 0.25f
    val labelAlpha by animateFloatAsState(targetValue = targetAlpha, animationSpec = tween(200))

    BasicTextField(
        value = value,
        onValueChange = onValueChange,
        singleLine = singleLine,
        minLines = if (singleLine) 1 else minLines,
        maxLines = if (singleLine) 1 else maxLines,
        textStyle = TextStyle(fontSize = fontSizeSp.sp, color = textColor),
        modifier = modifier
            .focusRequester(focusRequester)
            .onFocusChanged { isFocused = it.isFocused }
            .focusable(),
        keyboardOptions = KeyboardOptions.Default.copy(
            imeAction = if (singleLine) imeAction else ImeAction.Default
        ),
        decorationBox = { innerTextField ->
            TextFieldDefaults.OutlinedTextFieldDecorationBox(
                value = value,
                visualTransformation = VisualTransformation.None,
                innerTextField = innerTextField,
                singleLine = singleLine,
                enabled = true,
                isError = false,
                interactionSource = interaction,
                label = {
                    Text(
                        text = label,
                        color = textColor.copy(alpha = labelAlpha),
                        style = TextStyle(fontSize = (fontSizeSp - 2).sp)
                    )
                },
                trailingIcon = trailingIcon,
                colors = TextFieldDefaults.outlinedTextFieldColors(
                    textColor = textColor,
                    focusedBorderColor = focusedBorderColor,
                    unfocusedBorderColor = unfocusedBorderColor,
                    focusedLabelColor = focusedBorderColor,
                    unfocusedLabelColor = unfocusedBorderColor,
                    cursorColor = focusedBorderColor,
                    placeholderColor = Color.LightGray
                ),
                contentPadding = contentPadding
            )
        }
    )
}
