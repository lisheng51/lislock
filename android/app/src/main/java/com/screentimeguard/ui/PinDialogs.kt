package com.screentimeguard.ui

import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.text.input.PasswordVisualTransformation
import androidx.compose.ui.unit.dp
import com.screentimeguard.data.ParentPin

@Composable
private fun PinField(value: String, label: String, onChange: (String) -> Unit) {
    OutlinedTextField(
        value = value,
        onValueChange = { v -> onChange(v.filter(Char::isDigit).take(6)) },
        label = { Text(label) },
        singleLine = true,
        visualTransformation = PasswordVisualTransformation(),
        keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.NumberPassword),
    )
}

/** 输入家长密码。验证通过调用 [onSuccess]。 */
@Composable
fun VerifyPinDialog(
    pin: ParentPin,
    title: String = "输入家长密码",
    onSuccess: () -> Unit,
    onDismiss: () -> Unit,
) {
    var input by remember { mutableStateOf("") }
    var error by remember { mutableStateOf<String?>(null) }

    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text(title) },
        text = {
            Column {
                PinField(input, "家长密码") { input = it; error = null }
                error?.let {
                    Spacer(Modifier.height(8.dp))
                    Text(it, color = MaterialTheme.colorScheme.error)
                }
            }
        },
        confirmButton = {
            TextButton(onClick = {
                when (val r = pin.verify(input)) {
                    ParentPin.Result.Ok -> onSuccess()
                    is ParentPin.Result.Wrong -> error = "密码错误，还可尝试 ${r.attemptsLeft} 次"
                    is ParentPin.Result.LockedOut -> error = "错误次数过多，请 ${r.remainingMs / 1000 + 1} 秒后再试"
                }
                input = ""
            }) { Text("确定") }
        },
        dismissButton = { TextButton(onClick = onDismiss) { Text("取消") } },
    )
}

/** 设置新的家长密码（输入两次）。 */
@Composable
fun SetPinDialog(onSet: (String) -> Unit, onDismiss: () -> Unit) {
    var first by remember { mutableStateOf("") }
    var second by remember { mutableStateOf("") }
    var error by remember { mutableStateOf<String?>(null) }

    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text("设置家长密码") },
        text = {
            Column {
                PinField(first, "4–6 位数字") { first = it; error = null }
                Spacer(Modifier.height(8.dp))
                PinField(second, "再输入一次") { second = it; error = null }
                error?.let {
                    Spacer(Modifier.height(8.dp))
                    Text(it, color = MaterialTheme.colorScheme.error)
                }
            }
        },
        confirmButton = {
            TextButton(onClick = {
                error = when {
                    !ParentPin.isValidFormat(first) -> "密码必须是 4–6 位数字"
                    first != second -> "两次输入不一致"
                    else -> null
                }
                if (error == null) onSet(first)
            }) { Text("保存") }
        },
        dismissButton = { TextButton(onClick = onDismiss) { Text("取消") } },
    )
}
