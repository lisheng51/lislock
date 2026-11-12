import SwiftUI

/// 答题页：答对一题进入下一题；答错一题整组换新题；全部答对调用 onPassed。
/// 设置了家长密码时，底部有"家长解锁"按钮可直接解锁。
struct QuizView: View {
    let onPassed: () -> Void

    private let level = SharedStore.difficultyLevel
    @State private var questions: [Question] = QuizView.newRound()
    @State private var index = 0
    @State private var message: String?
    @State private var askPin = false

    private static func newRound() -> [Question] {
        let level = SharedStore.difficultyLevel
        return QuizGenerator.generate(
            count: QuizGenerator.questionCount(base: SharedStore.questionCount, level: level),
            level: level
        )
    }

    var body: some View {
        VStack(spacing: 20) {
            Text("⏰ 已使用超过 \(SharedStore.limitMinutes) 分钟").font(.title2.bold())
            Text("答对 \(questions.count) 道题才能继续使用").foregroundStyle(.secondary)
            if level > 0 {
                Text("今天已多次超时，难度提升到 Lv.\(level)").font(.footnote).foregroundStyle(.red)
            }

            ProgressView(value: Double(index), total: Double(questions.count))
            Text("第 \(index + 1) / \(questions.count) 题").font(.footnote)

            Text(questions[index].text)
                .font(.system(size: 30, weight: .semibold))
                .multilineTextAlignment(.center)
                .padding(.vertical)

            ForEach(Array(questions[index].options.enumerated()), id: \.offset) { i, option in
                Button { answer(i) } label: {
                    Text(option).font(.title3).frame(maxWidth: .infinity).padding(.vertical, 8)
                }
                .buttonStyle(.bordered)
            }

            if let message { Text(message).foregroundStyle(.red) }

            if ParentPin.isSet {
                Spacer()
                Button("家长解锁") { askPin = true }
            }
        }
        .padding(24)
        .interactiveDismissDisabled()
        .sheet(isPresented: $askPin) {
            VerifyPinSheet(title: "家长密码直接解锁", onSuccess: onPassed)
        }
    }

    private func answer(_ i: Int) {
        if i == questions[index].answerIndex {
            message = nil
            if index + 1 == questions.count { onPassed() } else { index += 1 }
        } else {
            message = "答错了，换一组题重新开始"
            questions = QuizView.newRound()
            index = 0
        }
    }
}
