package com.screentimeguard.ui

import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.OnBackPressedCallback
import androidx.activity.compose.setContent
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import com.screentimeguard.data.ParentPin
import com.screentimeguard.data.Prefs
import com.screentimeguard.quiz.Question
import com.screentimeguard.quiz.QuizGenerator

/**
 * 超时后的全屏锁定界面。返回键无效；被切走时 GuardService 会重新拉起它。
 * 解锁方式：答对全部题目，或输入家长密码。
 */
class LockActivity : ComponentActivity() {

    private lateinit var prefs: Prefs
    private lateinit var pin: ParentPin
    private lateinit var generator: QuizGenerator
    private var questions by mutableStateOf(emptyList<Question>())
    private var passed by mutableStateOf(false)

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        prefs = Prefs(this)
        pin = ParentPin(this)
        generator = QuizGenerator(this)
        newRound()

        onBackPressedDispatcher.addCallback(this, object : OnBackPressedCallback(true) {
            override fun handleOnBackPressed() { /* 锁定期间禁止返回 */ }
        })

        setContent {
            MaterialTheme {
                Surface {
                    if (passed || !prefs.locked) {
                        PassedScreen(onClose = { finish() })
                    } else {
                        LockedContent()
                    }
                }
            }
        }
    }

    @androidx.compose.runtime.Composable
    private fun LockedContent() {
        var askPin by remember { mutableStateOf(false) }
        Column(Modifier.fillMaxSize()) {
            Box(Modifier.weight(1f)) {
                QuizScreen(
                    limitMinutes = prefs.limitMinutes,
                    level = prefs.difficultyLevel,
                    questions = questions,
                    onWrong = { newRound() },
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
        if (askPin) {
            VerifyPinDialog(
                pin = pin,
                title = "家长密码直接解锁",
                onSuccess = { askPin = false; unlock() },
                onDismiss = { askPin = false },
            )
        }
    }

    private fun unlock() {
        prefs.unlock()
        passed = true
    }

    private fun newRound() {
        val level = prefs.difficultyLevel
        questions = generator.generate(QuizGenerator.questionCountFor(prefs.questionCount, level), level)
    }

    override fun onResume() {
        super.onResume()
        isVisible = true
    }

    override fun onPause() {
        isVisible = false
        super.onPause()
    }

    companion object {
        @Volatile
        var isVisible = false
            private set
    }
}
