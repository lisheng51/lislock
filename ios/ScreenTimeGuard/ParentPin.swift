import Foundation
import CryptoKit
import Security

/// 家长密码（4–6 位数字）。只保存加盐的哈希；连续输错 5 次锁定 60 秒。
enum ParentPin {
    static let maxAttempts = 5
    static let lockoutSeconds: TimeInterval = 60

    private static let d = UserDefaults.standard
    private enum Key {
        static let hash = "pin.hash", salt = "pin.salt", fails = "pin.fails", lockoutUntil = "pin.lockoutUntil"
    }

    enum Result: Equatable { case ok, wrong(attemptsLeft: Int), lockedOut(seconds: Int) }

    static var isSet: Bool { d.data(forKey: Key.hash) != nil }

    static func isValidFormat(_ pin: String) -> Bool {
        (4...6).contains(pin.count) && pin.allSatisfy(\.isNumber)
    }

    static func set(_ pin: String) {
        var salt = Data(count: 16)
        _ = salt.withUnsafeMutableBytes { SecRandomCopyBytes(kSecRandomDefault, 16, $0.baseAddress!) }
        d.set(salt, forKey: Key.salt)
        d.set(hash(pin, salt: salt), forKey: Key.hash)
        d.set(0, forKey: Key.fails)
        d.removeObject(forKey: Key.lockoutUntil)
    }

    static func clear() {
        [Key.hash, Key.salt, Key.fails, Key.lockoutUntil].forEach(d.removeObject(forKey:))
    }

    static func verify(_ pin: String) -> Result {
        guard isSet else { return .ok }
        let remaining = d.double(forKey: Key.lockoutUntil) - Date().timeIntervalSince1970
        if remaining > 0 { return .lockedOut(seconds: Int(remaining) + 1) }

        let salt = d.data(forKey: Key.salt) ?? Data()
        if hash(pin, salt: salt) == d.data(forKey: Key.hash) {
            d.set(0, forKey: Key.fails)
            return .ok
        }
        let fails = d.integer(forKey: Key.fails) + 1
        if fails >= maxAttempts {
            d.set(0, forKey: Key.fails)
            d.set(Date().timeIntervalSince1970 + lockoutSeconds, forKey: Key.lockoutUntil)
            return .lockedOut(seconds: Int(lockoutSeconds))
        }
        d.set(fails, forKey: Key.fails)
        return .wrong(attemptsLeft: maxAttempts - fails)
    }

    /// 加盐 SHA-256，迭代 20000 次
    private static func hash(_ pin: String, salt: Data) -> Data {
        var digest = Data(SHA256.hash(data: salt + Data(pin.utf8)))
        for _ in 0..<20_000 { digest = Data(SHA256.hash(data: digest + salt)) }
        return digest
    }
}
