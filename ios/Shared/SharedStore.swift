import Foundation
import FamilyControls
import ManagedSettings

/// App 与各扩展通过 App Group 共享的数据。
/// ⚠️ 修改 project.yml 中的 APP_GROUP 时，这里也要同步修改。
enum SharedStore {
    static let appGroup = "group.com.example.screentimeguard"
    static let defaults = UserDefaults(suiteName: appGroup)!

    private enum Key {
        static let limitMinutes = "limitMinutes"
        static let questionCount = "questionCount"
        static let selection = "selection"
        static let locked = "locked"
        static let lockDate = "lockDate"
        static let lockCount = "lockCount"
    }

    private static var today: String {
        let f = DateFormatter(); f.dateFormat = "yyyy-MM-dd"; return f.string(from: Date())
    }

    /// 今天第几次超时，跨天自动清零
    static var lockCountToday: Int {
        defaults.string(forKey: Key.lockDate) == today ? defaults.integer(forKey: Key.lockCount) : 0
    }

    /// 当前难度：今天第 1 次超时为 Lv.0，之后每次 +1，最高 Lv.3
    static var difficultyLevel: Int { min(max(lockCountToday - 1, 0), 3) }

    /// 记录一次超时
    static func recordLock() {
        defaults.set(lockCountToday + 1, forKey: Key.lockCount)
        defaults.set(today, forKey: Key.lockDate)
    }

    /// 使用上限（分钟），默认 60
    static var limitMinutes: Int {
        get { defaults.object(forKey: Key.limitMinutes) as? Int ?? 60 }
        set { defaults.set(newValue, forKey: Key.limitMinutes) }
    }

    /// 解锁需答对的题数，默认 3
    static var questionCount: Int {
        get { defaults.object(forKey: Key.questionCount) as? Int ?? 3 }
        set { defaults.set(newValue, forKey: Key.questionCount) }
    }

    /// 是否已被锁定（等待答题）
    static var locked: Bool {
        get { defaults.bool(forKey: Key.locked) }
        set { defaults.set(newValue, forKey: Key.locked) }
    }

    /// 用户在 FamilyActivityPicker 中选择的要限制的 App / 类别
    static var selection: FamilyActivitySelection {
        get {
            guard let data = defaults.data(forKey: Key.selection),
                  let value = try? JSONDecoder().decode(FamilyActivitySelection.self, from: data)
            else { return FamilyActivitySelection() }
            return value
        }
        set { defaults.set(try? JSONEncoder().encode(newValue), forKey: Key.selection) }
    }
}

/// 屏蔽 / 解除屏蔽
enum Shield {
    static let store = ManagedSettingsStore()

    static func apply() {
        let s = SharedStore.selection
        store.shield.applications = s.applicationTokens.isEmpty ? nil : s.applicationTokens
        store.shield.applicationCategories = s.categoryTokens.isEmpty ? nil : .specific(s.categoryTokens)
        store.shield.webDomains = s.webDomainTokens.isEmpty ? nil : s.webDomainTokens
        if !SharedStore.locked { SharedStore.recordLock() }
        SharedStore.locked = true
    }

    static func clear() {
        store.clearAllSettings()
        SharedStore.locked = false
    }
}
