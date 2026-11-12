package com.screentimeguard.quiz

import android.content.Context
import com.screentimeguard.data.QuestionBank
import kotlin.random.Random

data class Question(val text: String, val options: List<String>, val answerIndex: Int)

/**
 * 生成解锁题：算术题 + 题库题（内置 / 自定义 / 远程）随机混合。
 * 题库设置里打开"只出题库题"时，不出算术题。
 *
 * 难度递增：同一天里每多超时一次，难度 +1（最高 [MAX_LEVEL]）。
 *  - Lv.0  两位数加减、一位数乘法
 *  - Lv.1  三位数加减、两位数 × 一位数，题数 +1
 *  - Lv.2  两位数 × 两位数、三个数连加减，题数 +2，题库题减少
 *  - Lv.3  a × b + c 混合运算，题数 +3，题库题最少
 */
class QuizGenerator(context: Context, private val random: Random = Random.Default) {

    private val settings = QuestionBank(context)
    private val bank: List<Question> = settings.all().map { Question(it.q, it.options, it.answer) }

    fun generate(count: Int, level: Int = 0): List<Question> {
        val lv = level.coerceIn(0, MAX_LEVEL)
        // 难度越高，题库题占比越低（算术题越多）：50% → 40% → 30% → 20%
        val bankChance = if (settings.onlyBank) 1.0 else listOf(0.5, 0.4, 0.3, 0.2)[lv]
        // 只出题库题时尽量不重复
        val pool = bank.shuffled(random).toMutableList()
        return List(count) {
            if (bank.isNotEmpty() && random.nextDouble() < bankChance) {
                if (pool.isEmpty()) pool += bank.shuffled(random)
                pool.removeAt(0).shuffled()
            } else arithmetic(lv)
        }
    }

    private fun r(from: Int, until: Int) = random.nextInt(from, until)

    private fun arithmetic(level: Int): Question {
        val (text, answer) = when (level) {
            0 -> when (r(0, 3)) {
                0 -> { val a = r(10, 100); val b = r(10, 100); "$a + $b = ?" to a + b }
                1 -> { val a = r(30, 100); val b = r(10, a); "$a − $b = ?" to a - b }
                else -> { val a = r(2, 10); val b = r(2, 10); "$a × $b = ?" to a * b }
            }
            1 -> when (r(0, 3)) {
                0 -> { val a = r(100, 1000); val b = r(100, 1000); "$a + $b = ?" to a + b }
                1 -> { val a = r(300, 1000); val b = r(100, a); "$a − $b = ?" to a - b }
                else -> { val a = r(11, 100); val b = r(2, 10); "$a × $b = ?" to a * b }
            }
            2 -> when (r(0, 2)) {
                0 -> { val a = r(11, 50); val b = r(11, 50); "$a × $b = ?" to a * b }
                else -> {
                    val a = r(100, 1000); val b = r(10, 100); val c = r(10, 100)
                    "$a + $b − $c = ?" to a + b - c
                }
            }
            else -> {
                val a = r(11, 100); val b = r(3, 10); val c = r(10, 100)
                "$a × $b + $c = ?" to a * b + c
            }
        }
        // 干扰项与正确答案的差距：难度越高越接近，越难猜
        val spread = if (level >= 2) 20 else 10
        val options = mutableSetOf(answer)
        while (options.size < 4) {
            val wrong = answer + r(-spread, spread + 1)
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

    companion object {
        const val MAX_LEVEL = 3

        /** 实际题数 = 基础题数 + 难度等级（最多 10 题） */
        fun questionCountFor(base: Int, level: Int) = (base + level.coerceIn(0, MAX_LEVEL)).coerceAtMost(10)
    }
}
