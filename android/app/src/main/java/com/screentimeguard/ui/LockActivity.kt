package com.screentimeguard.ui

import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.OnBackPressedCallback
import androidx.activity.compose.setContent
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.setValue
import com.screentimeguard.data.Prefs
import com.screentimeguard.quiz.QuizGenerator

/** 超时后的全屏锁定界面。返回键无效；被切走时 GuardService 会重新拉起它。 */
class LockActivity : ComponentActivity() {

    private lateinit var prefs: Prefs
    private lateinit var generator: QuizGenerator
    private var questions by mutableStateOf(emptyList<com.screentimeguard.quiz.Question>())
    private var passed by mutableStateOf(false)

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        prefs = Prefs(this)
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
                        QuizScreen(
                            limitMinutes = prefs.limitMinutes,
                            questions = questions,
                            onWrong = { newRound() },
                            onPassed = {
                                prefs.unlock()
                                passed = true
                            },
                        )
                    }
                }
            }
        }
    }

    private fun newRound() {
        questions = generator.generate(prefs.questionCount)
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
