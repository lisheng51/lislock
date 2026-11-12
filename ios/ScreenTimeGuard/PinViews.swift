import SwiftUI

/// 输入家长密码的弹窗
struct VerifyPinSheet: View {
    var title = "输入家长密码"
    let onSuccess: () -> Void
    @Environment(\.dismiss) private var dismiss
    @State private var input = ""
    @State private var error: String?

    var body: some View {
        NavigationStack {
            Form {
                SecureField("家长密码", text: $input)
                    .keyboardType(.numberPad)
                if let error { Text(error).foregroundStyle(.red) }
            }
            .navigationTitle(title)
            .navigationBarTitleDisplayMode(.inline)
            .toolbar {
                ToolbarItem(placement: .cancellationAction) { Button("取消") { dismiss() } }
                ToolbarItem(placement: .confirmationAction) {
                    Button("确定") {
                        switch ParentPin.verify(input) {
                        case .ok: dismiss(); onSuccess()
                        case .wrong(let left): error = "密码错误，还可尝试 \(left) 次"
                        case .lockedOut(let s): error = "错误次数过多，请 \(s) 秒后再试"
                        }
                        input = ""
                    }
                }
            }
        }
        .presentationDetents([.medium])
    }
}

/// 设置新的家长密码（输入两次）
struct SetPinSheet: View {
    let onSet: (String) -> Void
    @Environment(\.dismiss) private var dismiss
    @State private var first = ""
    @State private var second = ""
    @State private var error: String?

    var body: some View {
        NavigationStack {
            Form {
                SecureField("4–6 位数字", text: $first).keyboardType(.numberPad)
                SecureField("再输入一次", text: $second).keyboardType(.numberPad)
                if let error { Text(error).foregroundStyle(.red) }
            }
            .navigationTitle("设置家长密码")
            .navigationBarTitleDisplayMode(.inline)
            .toolbar {
                ToolbarItem(placement: .cancellationAction) { Button("取消") { dismiss() } }
                ToolbarItem(placement: .confirmationAction) {
                    Button("保存") {
                        if !ParentPin.isValidFormat(first) { error = "密码必须是 4–6 位数字" }
                        else if first != second { error = "两次输入不一致" }
                        else { onSet(first); dismiss() }
                    }
                }
            }
        }
        .presentationDetents([.medium])
    }
}
