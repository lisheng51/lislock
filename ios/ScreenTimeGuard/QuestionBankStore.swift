import Foundation

/// 题库中的一道选择题，格式与 shared/questions.json 一致。
struct BankQuestion: Codable, Equatable, Identifiable {
    var id = UUID()
    var q: String
    var options: [String]
    var answer: Int

    enum CodingKeys: String, CodingKey { case q, options, answer }

    init(q: String, options: [String], answer: Int) {
        self.q = q; self.options = options; self.answer = answer
    }

    /// 返回错误说明；合法时返回 nil
    var validationError: String? {
        if q.trimmingCharacters(in: .whitespaces).isEmpty { return "题目不能为空" }
        if !(2...6).contains(options.count) { return "选项数量必须是 2–6 个" }
        if options.contains(where: { $0.trimmingCharacters(in: .whitespaces).isEmpty }) { return "选项不能为空" }
        if Set(options).count != options.count { return "选项不能重复" }
        if !options.indices.contains(answer) { return "answer 必须是正确选项的序号（从 0 开始）" }
        return nil
    }
}

enum BankJSON {
    static let maxQuestions = 2000

    private struct File: Codable { var version = 1; var questions: [BankQuestion] }

    /// 解析并校验。支持 `{"questions": [...]}` 或直接是数组。
    static func parse(_ data: Data) throws -> [BankQuestion] {
        let list: [BankQuestion]
        if let file = try? JSONDecoder().decode(File.self, from: data) {
            list = file.questions
        } else if let arr = try? JSONDecoder().decode([BankQuestion].self, from: data) {
            list = arr
        } else {
            throw BankError("不是有效的题库 JSON")
        }
        if list.count > maxQuestions { throw BankError("题目太多（最多 \(maxQuestions) 道）") }
        for (i, q) in list.enumerated() {
            if let e = q.validationError { throw BankError("第 \(i + 1) 题格式错误：\(e)") }
        }
        return list
    }

    static func serialize(_ list: [BankQuestion]) -> Data {
        let enc = JSONEncoder()
        enc.outputFormatting = [.prettyPrinted, .withoutEscapingSlashes]
        return (try? enc.encode(File(questions: list))) ?? Data()
    }
}

struct BankError: LocalizedError {
    let message: String
    init(_ message: String) { self.message = message }
    var errorDescription: String? { message }
}

/// 题库：内置（App 包里的 questions.json）+ 自定义 + 远程（从网址下载的缓存）。
final class QuestionBankStore: ObservableObject {
    static let shared = QuestionBankStore()

    private static let refreshInterval: TimeInterval = 6 * 3600
    private static let maxBytes = 1_000_000

    private let defaults = UserDefaults.standard
    private let dir = FileManager.default.urls(for: .applicationSupportDirectory, in: .userDomainMask)[0]
    private var customURL: URL { dir.appendingPathComponent("custom_questions.json") }
    private var remoteURL: URL { dir.appendingPathComponent("remote_questions.json") }

    let builtin: [BankQuestion]
    @Published var custom: [BankQuestion] = [] { didSet { save(custom, to: customURL) } }
    @Published private(set) var remote: [BankQuestion] = []
    @Published private(set) var isUpdating = false

    @Published var builtinEnabled: Bool { didSet { defaults.set(builtinEnabled, forKey: "bank.builtin") } }
    @Published var onlyBank: Bool { didSet { defaults.set(onlyBank, forKey: "bank.onlyBank") } }
    @Published var remoteAddress: String { didSet { defaults.set(remoteAddress, forKey: "bank.remoteURL") } }
    @Published private(set) var remoteUpdatedAt: Date? {
        didSet { defaults.set(remoteUpdatedAt, forKey: "bank.remoteUpdatedAt") }
    }
    @Published private(set) var remoteError: String? { didSet { defaults.set(remoteError, forKey: "bank.remoteError") } }

    private init() {
        try? FileManager.default.createDirectory(at: dir, withIntermediateDirectories: true)
        builtin = Bundle.main.url(forResource: "questions", withExtension: "json")
            .flatMap { try? Data(contentsOf: $0) }
            .flatMap { try? BankJSON.parse($0) } ?? []
        builtinEnabled = defaults.object(forKey: "bank.builtin") as? Bool ?? true
        onlyBank = defaults.bool(forKey: "bank.onlyBank")
        remoteAddress = defaults.string(forKey: "bank.remoteURL") ?? ""
        remoteUpdatedAt = defaults.object(forKey: "bank.remoteUpdatedAt") as? Date
        remoteError = defaults.string(forKey: "bank.remoteError")
        custom = load(customURL)
        remote = load(remoteURL)
    }

    /// 出题时使用的全部题目
    var allQuestions: [BankQuestion] { (builtinEnabled ? builtin : []) + custom + remote }

    /// 立即下载远程题库。下载或校验失败时保留旧缓存。
    @MainActor
    func refreshRemote() async {
        let address = remoteAddress.trimmingCharacters(in: .whitespaces)
        guard !isUpdating else { return }
        isUpdating = true
        defer { isUpdating = false }
        do {
            guard let url = URL(string: address), url.scheme?.lowercased() == "https" else {
                throw BankError("网址必须以 https:// 开头")
            }
            var request = URLRequest(url: url, timeoutInterval: 15)
            request.setValue("application/json", forHTTPHeaderField: "Accept")
            let (data, response) = try await URLSession.shared.data(for: request)
            if let http = response as? HTTPURLResponse, http.statusCode != 200 {
                throw BankError("服务器返回 HTTP \(http.statusCode)")
            }
            if data.count > Self.maxBytes { throw BankError("文件太大（超过 1 MB）") }
            let list = try BankJSON.parse(data)
            if list.isEmpty { throw BankError("题库是空的") }
            remote = list
            save(list, to: remoteURL)
            remoteUpdatedAt = Date()
            remoteError = nil
        } catch {
            remoteError = error.localizedDescription
        }
    }

    /// App 回到前台时调用：距上次成功更新超过 6 小时才下载
    @MainActor
    func refreshIfStale() async {
        guard !remoteAddress.isEmpty else { return }
        if let last = remoteUpdatedAt, Date().timeIntervalSince(last) < Self.refreshInterval { return }
        await refreshRemote()
    }

    func clearRemote() {
        remote = []
        try? FileManager.default.removeItem(at: remoteURL)
        remoteAddress = ""
        remoteUpdatedAt = nil
        remoteError = nil
    }

    private func load(_ url: URL) -> [BankQuestion] {
        (try? Data(contentsOf: url)).flatMap { try? BankJSON.parse($0) } ?? []
    }

    private func save(_ list: [BankQuestion], to url: URL) {
        try? BankJSON.serialize(list).write(to: url, options: .atomic)
    }
}
