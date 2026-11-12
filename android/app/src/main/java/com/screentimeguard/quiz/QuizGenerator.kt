package com.screentimeguard.quiz

import android.content.Context
import org.json.JSONObject
import kotlin.random.Random

data class Question(val text: String, val options: List<String>, val answerIndex: Int)

/** 生成解锁题目：算术题 + shared/questions.json 中的题库题随机混合。 */
class QuizGenerator(context: Context, private val random: Random = Random.Default) {

    private val bank: List<Question> = loadBank(context)

    fun generate(count: Int): List<Question> = List(count) {
        if (bank.isNotEmpty() && random.nextBoolean()) bank.random(random).shuffled() else arithmetic()
    }

    private fun arithmetic(): Question {
        val (text, answer) = when (random.nextInt(3)) {
            0 -> { val a = random.nextInt(10, 100); val b = random.nextInt(10, 100); "$a + $b = ?" to a + b }
            1 -> { val a = random.nextInt(30, 100); val b = random.nextInt(10, a); "$a − $b = ?" to a - b }
            else -> { val a = random.nextInt(2, 10); val b = random.nextInt(2, 10); "$a × $b = ?" to a * b }
        }
        val options = mutableSetOf(answer)
        while (options.size < 4) {
            val wrong = answer + random.nextInt(-10, 11)
            if (wrong >= 0) options += wrong
        }
        val list = options.shuffled(random).map { it.toString() }
        return Question(text, list, list.indexOf(answer.toString()))
    }

    /** 打乱选项顺序，防止记住位置 */
    private fun Question.shuffled(): Question {
        val correct = options[answerIndex]
        val opts = options.shuffled(random)
        return copy(options = opts, answerIndex = opts.indexOf(correct))
    }

    private fun loadBank(context: Context): List<Question> = runCatching {
        val json = context.assets.open("questions.json").bufferedReader().use { it.readText() }
        val arr = JSONObject(json).getJSONArray("questions")
        (0 until arr.length()).map { i ->
            val o = arr.getJSONObject(i)
            val opts = o.getJSONArray("options")
            Question(o.getString("q"), (0 until opts.length()).map(opts::getString), o.getInt("answer"))
        }
    }.getOrDefault(emptyList())
}
