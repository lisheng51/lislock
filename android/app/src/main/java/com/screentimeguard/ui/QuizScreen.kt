package com.screentimeguard.ui

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.Button
import androidx.compose.material3.LinearProgressIndicator
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.screentimeguard.quiz.Question

/**
 * 答题界面。答对一题进入下一题；答错一题则调用 [onWrong] 换一整组新题。
 * 全部答对后调用 [onPassed]。
 */
@Composable
fun QuizScreen(
    limitMinutes: Int,
    level: Int,
    questions: List<Question>,
    onWrong: () -> Unit,
    onPassed: () -> Unit,
) {
    var index by remember(questions) { mutableIntStateOf(0) }
    var message by remember { mutableStateOf<String?>(null) }
    val q = questions[index]

    Column(
        modifier = Modifier.fillMaxSize().padding(24.dp),
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.Center,
    ) {
        Text("⏰ Je zit al meer dan $limitMinutes minuten op je telefoon", style = MaterialTheme.typography.headlineSmall)
        Spacer(Modifier.height(8.dp))
        Text("Beantwoord ${questions.size} vragen goed om verder te gaan", style = MaterialTheme.typography.bodyLarge)
        if (level > 0) {
            Spacer(Modifier.height(4.dp))
            Text(
                "Vandaag al vaker over de tijd – de vragen zijn moeilijker (niveau $level)",
                color = MaterialTheme.colorScheme.error,
                style = MaterialTheme.typography.bodyMedium,
            )
        }
        Spacer(Modifier.height(24.dp))
        LinearProgressIndicator(
            progress = { index / questions.size.toFloat() },
            modifier = Modifier.fillMaxWidth(),
        )
        Spacer(Modifier.height(8.dp))
        Text("Vraag ${index + 1} van ${questions.size}")
        Spacer(Modifier.height(32.dp))
        Text(q.text, fontSize = 26.sp, textAlign = TextAlign.Center)
        Spacer(Modifier.height(32.dp))

        q.options.forEachIndexed { i, option ->
            OutlinedButton(
                onClick = {
                    if (i == q.answerIndex) {
                        message = null
                        if (index + 1 == questions.size) onPassed() else index++
                    } else {
                        message = "Helaas, fout! Je krijgt nieuwe vragen."
                        onWrong()
                    }
                },
                modifier = Modifier.fillMaxWidth().padding(vertical = 4.dp),
            ) { Text(option, fontSize = 20.sp) }
        }

        message?.let {
            Spacer(Modifier.height(16.dp))
            Text(it, color = MaterialTheme.colorScheme.error)
        }
    }
}

@Composable
fun PassedScreen(onClose: () -> Unit) {
    Column(
        modifier = Modifier.fillMaxSize().padding(24.dp),
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.Center,
    ) {
        Text("🎉 Alles goed!", style = MaterialTheme.typography.headlineMedium)
        Spacer(Modifier.height(8.dp))
        Text("De tijd begint opnieuw. Vergeet niet je ogen af en toe rust te geven!")
        Spacer(Modifier.height(24.dp))
        Button(onClick = onClose) { Text("Verder") }
    }
}
