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

    static let maxLevel = 3

    /// 实际题数 = 基础题数 + 难度等级（最多 10 题）
    static func questionCount(base: Int, level: Int) -> Int {
        min(base + min(max(level, 0), maxLevel), 10)
    }

    /// 难度递增（与 Android 一致）：
    ///  Lv.0 两位数加减、一位数乘法；Lv.1 三位数加减、两位数×一位数；
    ///  Lv.2 两位数×两位数、三数连加减；Lv.3 a×b+c，只出算术题
    static func generate(count: Int, level: Int = 0) -> [Question] {
        let lv = min(max(level, 0), maxLevel)
        let bankChance = [0.5, 0.35, 0.2, 0.0][lv]
        return (0..<count).map { _ in
            if let q = bank.randomElement(), Double.random(in: 0..<1) < bankChance { return shuffled(q) }
            return arithmetic(level: lv)
        }
    }

    private static func r(_ range: Range<Int>) -> Int { Int.random(in: range) }

    private static func arithmetic(level: Int) -> Question {
        let text: String
        let answer: Int
        switch (level, r(0..<3)) {
        case (0, 0):
            let a = r(10..<100), b = r(10..<100); text = "\(a) + \(b) = ?"; answer = a + b
        case (0, 1):
            let a = r(30..<100), b = r(10..<a); text = "\(a) − \(b) = ?"; answer = a - b
        case (0, _):
            let a = r(2..<10), b = r(2..<10); text = "\(a) × \(b) = ?"; answer = a * b
        case (1, 0):
            let a = r(100..<1000), b = r(100..<1000); text = "\(a) + \(b) = ?"; answer = a + b
        case (1, 1):
            let a = r(300..<1000), b = r(100..<a); text = "\(a) − \(b) = ?"; answer = a - b
        case (1, _):
            let a = r(11..<100), b = r(2..<10); text = "\(a) × \(b) = ?"; answer = a * b
        case (2, 0):
            let a = r(11..<50), b = r(11..<50); text = "\(a) × \(b) = ?"; answer = a * b
        case (2, _):
            let a = r(100..<1000), b = r(10..<100), c = r(10..<100)
            text = "\(a) + \(b) − \(c) = ?"; answer = a + b - c
        default:
            let a = r(11..<100), b = r(3..<10), c = r(10..<100)
            text = "\(a) × \(b) + \(c) = ?"; answer = a * b + c
        }
        let spread = level >= 2 ? 20 : 10
        var options: Set<Int> = [answer]
        while options.count < 4 {
            let wrong = answer + Int.random(in: -spread...spread)
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
