import Foundation

struct Question: Identifiable {
    let id = UUID()
    let text: String
    let options: [String]
    let answerIndex: Int
}

/// 生成解锁题：算术题 + shared/questions.json 题库题随机混合（与 Android 规则一致）。
enum QuizGenerator {
    private struct Bank: Decodable {
        struct Item: Decodable { let q: String; let options: [String]; let answer: Int }
        let questions: [Item]
    }

    private static let bank: [Question] = {
        guard let url = Bundle.main.url(forResource: "questions", withExtension: "json"),
              let data = try? Data(contentsOf: url),
              let bank = try? JSONDecoder().decode(Bank.self, from: data)
        else { return [] }
        return bank.questions.map { Question(text: $0.q, options: $0.options, answerIndex: $0.answer) }
    }()

    static func generate(count: Int) -> [Question] {
        (0..<count).map { _ in
            if let q = bank.randomElement(), Bool.random() { return shuffled(q) }
            return arithmetic()
        }
    }

    private static func arithmetic() -> Question {
        let text: String
        let answer: Int
        switch Int.random(in: 0..<3) {
        case 0:
            let a = Int.random(in: 10..<100), b = Int.random(in: 10..<100)
            text = "\(a) + \(b) = ?"; answer = a + b
        case 1:
            let a = Int.random(in: 30..<100), b = Int.random(in: 10..<a)
            text = "\(a) − \(b) = ?"; answer = a - b
        default:
            let a = Int.random(in: 2..<10), b = Int.random(in: 2..<10)
            text = "\(a) × \(b) = ?"; answer = a * b
        }
        var options: Set<Int> = [answer]
        while options.count < 4 {
            let wrong = answer + Int.random(in: -10...10)
            if wrong >= 0 { options.insert(wrong) }
        }
        let list = options.shuffled().map(String.init)
        return Question(text: text, options: list, answerIndex: list.firstIndex(of: String(answer))!)
    }

    private static func shuffled(_ q: Question) -> Question {
        let correct = q.options[q.answerIndex]
        let opts = q.options.shuffled()
        return Question(text: q.text, options: opts, answerIndex: opts.firstIndex(of: correct)!)
    }
}
