package com.screentimeguard.ui

import android.os.Bundle
import android.text.format.DateFormat
import android.widget.Toast
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.itemsIndexed
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Button
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.RadioButton
import androidx.compose.material3.Surface
import androidx.compose.material3.Switch
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateListOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import com.screentimeguard.data.BankJson
import com.screentimeguard.data.BankQuestion
import com.screentimeguard.data.QuestionBank
import com.screentimeguard.data.RemoteBankSync

/**
 * 题库管理（只能从已通过家长验证的设置页进入）：
 *  - 内置题库开关、只出题库题开关
 *  - 远程题库网址 + 立即更新
 *  - 自定义题目的增删改、批量导入 JSON
 */
class QuestionBankActivity : ComponentActivity() {

    private lateinit var bank: QuestionBank

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        bank = QuestionBank(this)
        setContent { MaterialTheme { Surface { Screen() } } }
    }

    override fun onStop() {
        super.onStop()
        // 离开就关闭，避免孩子回到这里看答案（再次进入需要从设置页、通过家长密码）
        if (!isChangingConfigurations) finish()
    }

    /** 正在编辑的题目：index = -1 表示新增 */
    private data class Editing(val index: Int, val question: BankQuestion?)

    @Composable
    private fun Screen() {
        val custom = remember { mutableStateListOf<BankQuestion>().apply { addAll(bank.custom()) } }
        var builtinOn by remember { mutableStateOf(bank.builtinEnabled) }
        var onlyBank by remember { mutableStateOf(bank.onlyBank) }
        var url by remember { mutableStateOf(bank.remoteUrl) }
        var remoteCount by remember { mutableStateOf(bank.remote().size) }
        var remoteStatus by remember { mutableStateOf(remoteStatusText()) }
        var loading by remember { mutableStateOf(false) }
        var editing by remember { mutableStateOf<Editing?>(null) }
        var importing by remember { mutableStateOf(false) }
        val builtinCount = remember { bank.builtin().size }

        fun saveCustom() = bank.saveCustom(custom.toList())

        LazyColumn(Modifier.fillMaxSize().padding(horizontal = 20.dp)) {
            item {
                Spacer(Modifier.height(20.dp))
                Text("题库管理", style = MaterialTheme.typography.headlineMedium)
                Spacer(Modifier.height(4.dp))
                Text("当前共 ${(if (builtinOn) builtinCount else 0) + custom.size + remoteCount} 道题库题")

                SectionTitle("出题设置")
                SwitchRow("使用内置题库（$builtinCount 题）", builtinOn) {
                    builtinOn = it; bank.builtinEnabled = it
                }
                SwitchRow("只出题库题（不出算术题）", onlyBank) {
                    onlyBank = it; bank.onlyBank = it
                }

                SectionTitle("远程题库")
                Text(
                    "填一个 https 网址，内容是题库 JSON（格式见 README）。开启守护后每 6 小时自动更新一次。",
                    style = MaterialTheme.typography.bodySmall,
                )
                Spacer(Modifier.height(8.dp))
                OutlinedTextField(
                    value = url,
                    onValueChange = { url = it },
                    label = { Text("https://…/questions.json") },
                    singleLine = true,
                    keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Uri),
                    modifier = Modifier.fillMaxWidth(),
                )
                Spacer(Modifier.height(8.dp))
                Row {
                    Button(
                        enabled = !loading && url.isNotBlank(),
                        onClick = {
                            bank.remoteUrl = url
                            loading = true
                            remoteStatus = "正在下载…"
                            RemoteBankSync.refresh(this@QuestionBankActivity) { result ->
                                loading = false
                                remoteCount = bank.remote().size
                                remoteStatus = remoteStatusText()
                                result.onSuccess { toast("已加载 $it 道题") }
                            }
                        },
                    ) { Text(if (loading) "更新中…" else "保存并更新") }
                    Spacer(Modifier.width(8.dp))
                    if (remoteCount > 0 || bank.remoteUrl.isNotBlank()) {
                        OutlinedButton(onClick = {
                            bank.clearRemote()
                            url = ""
                            remoteCount = 0
                            remoteStatus = remoteStatusText()
                        }) { Text("移除") }
                    }
                }
                remoteStatus?.let {
                    Spacer(Modifier.height(4.dp))
                    Text(
                        it,
                        style = MaterialTheme.typography.bodySmall,
                        color = if (bank.remoteError != null) MaterialTheme.colorScheme.error
                        else MaterialTheme.colorScheme.onSurfaceVariant,
                    )
                }

                SectionTitle("自定义题目（${custom.size}）")
                Row {
                    Button(onClick = { editing = Editing(-1, null) }) { Text("添加题目") }
                    Spacer(Modifier.width(8.dp))
                    OutlinedButton(onClick = { importing = true }) { Text("批量导入 JSON") }
                }
                Spacer(Modifier.height(8.dp))
                if (custom.isEmpty()) {
                    Text("还没有自定义题目", color = MaterialTheme.colorScheme.onSurfaceVariant)
                }
            }

            itemsIndexed(custom) { i, q ->
                Row(
                    Modifier.fillMaxWidth().clickable { editing = Editing(i, q) }.padding(vertical = 10.dp),
                    verticalAlignment = Alignment.CenterVertically,
                ) {
                    Column(Modifier.weight(1f)) {
                        Text(q.q, maxLines = 2, overflow = TextOverflow.Ellipsis)
                        Text(
                            "答案：${q.options[q.answer]}  ·  ${q.options.size} 个选项",
                            style = MaterialTheme.typography.bodySmall,
                            color = MaterialTheme.colorScheme.onSurfaceVariant,
                        )
                    }
                    TextButton(onClick = { custom.removeAt(i); saveCustom() }) { Text("删除") }
                }
                HorizontalDivider()
            }

            item { Spacer(Modifier.height(40.dp)) }
        }

        editing?.let { e ->
            EditQuestionDialog(
                initial = e.question,
                onSave = { q ->
                    if (e.index >= 0) custom[e.index] = q else custom.add(q)
                    saveCustom()
                    editing = null
                },
                onDismiss = { editing = null },
            )
        }

        if (importing) {
            ImportDialog(
                onImport = { list ->
                    custom.addAll(list)
                    saveCustom()
                    importing = false
                    toast("已导入 ${list.size} 道题")
                },
                onDismiss = { importing = false },
            )
        }
    }

    private fun remoteStatusText(): String? {
        bank.remoteError?.let { return "更新失败：$it" }
        val n = bank.remote().size
        if (n == 0) return null
        val time = DateFormat.format("yyyy-MM-dd HH:mm", bank.remoteUpdatedAt)
        return "已加载 $n 道题，更新于 $time"
    }

    private fun toast(msg: String) = Toast.makeText(this, msg, Toast.LENGTH_SHORT).show()
}

@Composable
private fun SectionTitle(text: String) {
    Spacer(Modifier.height(24.dp))
    Text(text, style = MaterialTheme.typography.titleMedium)
    Spacer(Modifier.height(8.dp))
}

@Composable
private fun SwitchRow(label: String, checked: Boolean, onChange: (Boolean) -> Unit) {
    Row(
        Modifier.fillMaxWidth().padding(vertical = 4.dp),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        Text(label, Modifier.weight(1f))
        Switch(checked = checked, onCheckedChange = onChange)
    }
}

/** 新增 / 编辑一道选择题：题目 + 2–6 个选项 + 选中正确答案 */
@Composable
private fun EditQuestionDialog(
    initial: BankQuestion?,
    onSave: (BankQuestion) -> Unit,
    onDismiss: () -> Unit,
) {
    var text by remember { mutableStateOf(initial?.q ?: "") }
    val options = remember {
        mutableStateListOf<String>().apply { addAll(initial?.options ?: listOf("", "", "", "")) }
    }
    var answer by remember { mutableStateOf(initial?.answer ?: 0) }
    var error by remember { mutableStateOf<String?>(null) }

    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text(if (initial == null) "添加题目" else "编辑题目") },
        text = {
            Column(Modifier.heightIn(max = 480.dp).verticalScroll(rememberScrollState())) {
                OutlinedTextField(
                    value = text,
                    onValueChange = { text = it; error = null },
                    label = { Text("题目") },
                    modifier = Modifier.fillMaxWidth(),
                )
                Spacer(Modifier.height(8.dp))
                Text("选项（点左边圆圈标记正确答案）", style = MaterialTheme.typography.bodySmall)
                options.forEachIndexed { i, opt ->
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        RadioButton(selected = answer == i, onClick = { answer = i })
                        OutlinedTextField(
                            value = opt,
                            onValueChange = { options[i] = it; error = null },
                            label = { Text("选项 ${i + 1}") },
                            singleLine = true,
                            modifier = Modifier.weight(1f),
                        )
                        if (options.size > 2) {
                            TextButton(onClick = {
                                options.removeAt(i)
                                if (answer >= options.size) answer = options.size - 1
                                else if (answer > i) answer--
                            }) { Text("✕") }
                        }
                    }
                }
                if (options.size < 6) {
                    TextButton(onClick = { options.add("") }) { Text("+ 添加选项") }
                }
                error?.let { Text(it, color = MaterialTheme.colorScheme.error) }
            }
        },
        confirmButton = {
            TextButton(onClick = {
                val q = BankQuestion(text.trim(), options.map { it.trim() }, answer)
                error = BankJson.validate(q)
                if (error == null) onSave(q)
            }) { Text("保存") }
        },
        dismissButton = { TextButton(onClick = onDismiss) { Text("取消") } },
    )
}

/** 粘贴题库 JSON，追加到自定义题目 */
@Composable
private fun ImportDialog(onImport: (List<BankQuestion>) -> Unit, onDismiss: () -> Unit) {
    var json by remember { mutableStateOf("") }
    var error by remember { mutableStateOf<String?>(null) }

    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text("批量导入") },
        text = {
            Column {
                Text("粘贴题库 JSON（和远程题库格式相同），题目会追加到自定义题目里。", style = MaterialTheme.typography.bodySmall)
                Spacer(Modifier.height(8.dp))
                OutlinedTextField(
                    value = json,
                    onValueChange = { json = it; error = null },
                    modifier = Modifier.fillMaxWidth().heightIn(min = 160.dp, max = 320.dp),
                    placeholder = { Text("{\"questions\": [{\"q\": \"…\", \"options\": [\"A\", \"B\"], \"answer\": 0}]}") },
                )
                error?.let { Text(it, color = MaterialTheme.colorScheme.error) }
            }
        },
        confirmButton = {
            TextButton(onClick = {
                runCatching { BankJson.parse(json) }
                    .onSuccess { if (it.isEmpty()) error = "没有题目" else onImport(it) }
                    .onFailure { error = it.message }
            }) { Text("导入") }
        },
        dismissButton = { TextButton(onClick = onDismiss) { Text("取消") } },
    )
}
