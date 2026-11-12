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
        return QuizGenerator.generate(count: SharedStore.unlockQuestionCount, level: level)
    }

    var body: some View {
        VStack(spacing: 20) {
            Text("⏰ Je zit al meer dan \(SharedStore.limitMinutes) minuten op je telefoon").font(.title2.bold())
            Text("Beantwoord \(questions.count) vragen goed om verder te gaan").foregroundStyle(.secondary)
            if level > 0 {
                Text("Vandaag al vaker over de tijd – de vragen zijn moeilijker (niveau \(level))").font(.footnote).foregroundStyle(.red)
            }

            ProgressView(value: Double(index), total: Double(questions.count))
            Text("Vraag \(index + 1) van \(questions.count)").font(.footnote)

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
                Button("Ouder · 家长解锁") { askPin = true }
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
            message = "Helaas, fout! Je krijgt nieuwe vragen."
            questions = QuizView.newRound()
            index = 0
        }
    }
}
