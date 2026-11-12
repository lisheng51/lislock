import SwiftUI

/// 题库管理：内置题库开关、只出题库题、远程题库网址、自定义题目增删改、批量导入。
struct QuestionBankView: View {
    @ObservedObject private var store = QuestionBankStore.shared
    @State private var editing: EditTarget?
    @State private var showImport = false

    /// index == nil 表示新增
    struct EditTarget: Identifiable {
        let id = UUID()
        let index: Int?
        let question: BankQuestion?
    }

    var body: some View {
        Form {
            Section {
                Toggle("使用内置题库（\(store.builtin.count) 题）", isOn: $store.builtinEnabled)
                Toggle("只出题库题（不出算术题）", isOn: $store.onlyBank)
            } header: {
                Text("出题设置")
            } footer: {
                Text("当前共 \(store.allQuestions.count) 道题库题")
            }

            Section {
                TextField("https://…/questions.json", text: $store.remoteAddress)
                    .keyboardType(.URL)
                    .textInputAutocapitalization(.never)
                    .autocorrectionDisabled()
                Button(store.isUpdating ? "更新中…" : "保存并更新") {
                    Task { await store.refreshRemote() }
                }
                .disabled(store.isUpdating || store.remoteAddress.isEmpty)
                if !store.remote.isEmpty || !store.remoteAddress.isEmpty {
                    Button("移除远程题库", role: .destructive) { store.clearRemote() }
                }
            } header: {
                Text("远程题库")
            } footer: {
                if let error = store.remoteError {
                    Text("更新失败：\(error)").foregroundStyle(.red)
                } else if let date = store.remoteUpdatedAt, !store.remote.isEmpty {
                    Text("已加载 \(store.remote.count) 道题，更新于 \(date.formatted(date: .numeric, time: .shortened))")
                } else {
                    Text("填一个 https 网址，内容是题库 JSON（格式见 README）。打开 App 时每 6 小时自动更新一次。")
                }
            }

            Section("自定义题目（\(store.custom.count)）") {
                Button("添加题目") { editing = EditTarget(index: nil, question: nil) }
                Button("批量导入 JSON") { showImport = true }
                ForEach(Array(store.custom.enumerated()), id: \.element.id) { i, q in
                    Button { editing = EditTarget(index: i, question: q) } label: {
                        VStack(alignment: .leading, spacing: 2) {
                            Text(q.q).lineLimit(2).foregroundStyle(.primary)
                            Text("答案：\(q.options[q.answer])  ·  \(q.options.count) 个选项")
                                .font(.caption).foregroundStyle(.secondary)
                        }
                    }
                }
                .onDelete { store.custom.remove(atOffsets: $0) }
            }
        }
        .navigationTitle("题库管理")
        .sheet(item: $editing) { target in
            EditQuestionView(initial: target.question) { q in
                if let i = target.index { store.custom[i] = q } else { store.custom.append(q) }
            }
        }
        .sheet(isPresented: $showImport) {
            ImportBankView { store.custom.append(contentsOf: $0) }
        }
    }
}

/// 新增 / 编辑一道选择题
struct EditQuestionView: View {
    let initial: BankQuestion?
    let onSave: (BankQuestion) -> Void
    @Environment(\.dismiss) private var dismiss
    @State private var text = ""
    @State private var options = ["", "", "", ""]
    @State private var answer = 0
    @State private var error: String?

    var body: some View {
        NavigationStack {
            Form {
                Section("题目") {
                    TextField("题目", text: $text, axis: .vertical)
                }
                Section {
                    ForEach(options.indices, id: \.self) { i in
                        HStack {
                            Button { answer = i } label: {
                                Image(systemName: answer == i ? "checkmark.circle.fill" : "circle")
                            }
                            .buttonStyle(.plain)
                            TextField("选项 \(i + 1)", text: $options[i])
                        }
                    }
                    .onDelete { offsets in
                        guard options.count - offsets.count >= 2 else { return }
                        options.remove(atOffsets: offsets)
                        answer = min(answer, options.count - 1)
                    }
                    if options.count < 6 {
                        Button("+ 添加选项") { options.append("") }
                    }
                } header: {
                    Text("选项（点圆圈标记正确答案，左滑删除）")
                } footer: {
                    if let error { Text(error).foregroundStyle(.red) }
                }
            }
            .navigationTitle(initial == nil ? "添加题目" : "编辑题目")
            .navigationBarTitleDisplayMode(.inline)
            .toolbar {
                ToolbarItem(placement: .cancellationAction) { Button("取消") { dismiss() } }
                ToolbarItem(placement: .confirmationAction) {
                    Button("保存") {
                        let q = BankQuestion(
                            q: text.trimmingCharacters(in: .whitespaces),
                            options: options.map { $0.trimmingCharacters(in: .whitespaces) },
                            answer: answer
                        )
                        if let e = q.validationError { error = e } else { onSave(q); dismiss() }
                    }
                }
            }
            .onAppear {
                if let initial {
                    text = initial.q; options = initial.options; answer = initial.answer
                }
            }
        }
    }
}

/// 粘贴题库 JSON 批量导入到自定义题目
struct ImportBankView: View {
    let onImport: ([BankQuestion]) -> Void
    @Environment(\.dismiss) private var dismiss
    @State private var json = ""
    @State private var error: String?

    var body: some View {
        NavigationStack {
            Form {
                Section {
                    TextEditor(text: $json)
                        .font(.system(.footnote, design: .monospaced))
                        .frame(minHeight: 200)
                        .textInputAutocapitalization(.never)
                        .autocorrectionDisabled()
                } footer: {
                    if let error { Text(error).foregroundStyle(.red) }
                    else { Text("粘贴题库 JSON（和远程题库格式相同），题目会追加到自定义题目里。") }
                }
            }
            .navigationTitle("批量导入")
            .navigationBarTitleDisplayMode(.inline)
            .toolbar {
                ToolbarItem(placement: .cancellationAction) { Button("取消") { dismiss() } }
                ToolbarItem(placement: .confirmationAction) {
                    Button("导入") {
                        do {
                            let list = try BankJSON.parse(Data(json.utf8))
                            if list.isEmpty { error = "没有题目"; return }
                            onImport(list)
                            dismiss()
                        } catch {
                            self.error = error.localizedDescription
                        }
                    }
                }
            }
        }
    }
}
