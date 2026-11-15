package com.screentimeguard.ui

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material3.Button
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.text.input.PasswordVisualTransformation
import androidx.compose.ui.unit.dp
import com.screentimeguard.data.ParentPin
import com.screentimeguard.data.Prefs
import com.screentimeguard.quiz.QuizGenerator

/**
 * 锁定界面的全部内容：答题 → 全部答对 →「Verder」。
 * 底部可用家长密码直接解锁（内嵌输入框，不用弹窗——悬浮层窗口里不能弹 Dialog）。
 * 同时用于全屏悬浮层（LockOverlay）和兜底的 LockActivity。
 */
@Composable
fun LockContent(onFinished: () -> Unit) {
    val context = LocalContext.current
    val prefs = remember { Prefs(context) }
    val pin = remember { ParentPin(context) }
    val generator = remember { QuizGenerator(context) }
    val level = remember { prefs.difficultyLevel }

    fun newRound() = generator.generate(QuizGenerator.questionCountFor(prefs.questionCount, level), level)

    var questions by remember { mutableStateOf(newRound()) }
    var passed by remember { mutableStateOf(!prefs.locked) }
    var askPin by remember { mutableStateOf(false) }

    fun unlock() {
        prefs.unlock()
        passed = true
    }

    when {
        passed -> PassedScreen(onClose = onFinished)
        askPin -> PinPanel(
            pin = pin,
            onSuccess = { askPin = false; unlock() },
            onCancel = { askPin = false },
        )
        else -> Column(Modifier.fillMaxSize()) {
            Box(Modifier.weight(1f)) {
                QuizScreen(
                    limitMinutes = prefs.limitMinutes,
                    level = level,
                    questions = questions,
                    onWrong = { questions = newRound() },
                    onPassed = { unlock() },
                )
            }
            if (pin.isSet) {
                TextButton(
                    onClick = { askPin = true },
                    modifier = Modifier.align(Alignment.CenterHorizontally).padding(16.dp),
                ) { Text("Ouder · 家长解锁") }
            }
        }
    }
}

/** 内嵌的家长密码输入（家长用，中文） */
@Composable
private fun PinPanel(pin: ParentPin, onSuccess: () -> Unit, onCancel: () -> Unit) {
    var input by remember { mutableStateOf("") }
    var error by remember { mutableStateOf<String?>(null) }

    Column(
        modifier = Modifier.fillMaxSize().padding(24.dp),
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.Center,
    ) {
        Text("家长密码直接解锁", style = MaterialTheme.typography.headlineSmall)
        Spacer(Modifier.height(16.dp))
        OutlinedTextField(
            value = input,
            onValueChange = { v -> input = v.filter(Char::isDigit).take(6); error = null },
            label = { Text("家长密码") },
            singleLine = true,
            visualTransformation = PasswordVisualTransformation(),
            keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.NumberPassword),
        )
        error?.let {
            Spacer(Modifier.height(8.dp))
            Text(it, color = MaterialTheme.colorScheme.error)
        }
        Spacer(Modifier.height(16.dp))
        Row {
            OutlinedButton(onClick = onCancel) { Text("取消") }
            Spacer(Modifier.width(12.dp))
            Button(onClick = {
                when (val r = pin.verify(input)) {
                    ParentPin.Result.Ok -> onSuccess()
                    is ParentPin.Result.Wrong -> error = "密码错误，还可尝试 ${r.attemptsLeft} 次"
                    is ParentPin.Result.LockedOut -> error = "错误次数过多，请 ${r.remainingMs / 1000 + 1} 秒后再试"
                }
                input = ""
            }) { Text("解锁") }
        }
    }
}
