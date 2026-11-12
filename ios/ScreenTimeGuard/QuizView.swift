import SwiftUI

/// 答题页：答对一题进入下一题；答错一题整组换新题；全部答对调用 onPassed。
struct QuizView: View {
    let onPassed: () -> Void

    @State private var questions = QuizGenerator.generate(count: SharedStore.questionCount)
    @State private var index = 0
    @State private var message: String?

    var body: some View {
        VStack(spacing: 20) {
            Text("⏰ 已使用超过 \(SharedStore.limitMinutes) 分钟").font(.title2.bold())
            Text("答对 \(questions.count) 道题才能继续使用").foregroundStyle(.secondary)

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
        }
        .padding(24)
        .interactiveDismissDisabled()
    }

    private func answer(_ i: Int) {
        if i == questions[index].answerIndex {
            message = nil
            if index + 1 == questions.count { onPassed() } else { index += 1 }
        } else {
            message = "答错了，换一组题重新开始"
            questions = QuizGenerator.generate(count: SharedStore.questionCount)
            index = 0
        }
    }
}
