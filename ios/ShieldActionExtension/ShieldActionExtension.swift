import ManagedSettings
import UserNotifications

/// 屏蔽界面按钮的处理。iOS 不允许从这里直接打开 App，
/// 所以发一条本地通知，用户点通知即进入主 App 的答题页。
final class ShieldActionExtension: ShieldActionDelegate {

    override func handle(action: ShieldAction, for application: ApplicationToken,
                         completionHandler: @escaping (ShieldActionResponse) -> Void) {
        respond(to: action, completionHandler)
    }

    override func handle(action: ShieldAction, for webDomain: WebDomainToken,
                         completionHandler: @escaping (ShieldActionResponse) -> Void) {
        respond(to: action, completionHandler)
    }

    override func handle(action: ShieldAction, for category: ActivityCategoryToken,
                         completionHandler: @escaping (ShieldActionResponse) -> Void) {
        respond(to: action, completionHandler)
    }

    private func respond(to action: ShieldAction, _ completion: @escaping (ShieldActionResponse) -> Void) {
        switch action {
        case .primaryButtonPressed:
            sendQuizNotification { completion(.close) }
        case .secondaryButtonPressed:
            completion(.close)
        @unknown default:
            completion(.close)
        }
    }

    private func sendQuizNotification(_ done: @escaping () -> Void) {
        let content = UNMutableNotificationContent()
        content.title = "点我开始答题解锁"
        content.body = "答对 \(SharedStore.questionCount) 道题后即可继续使用"
        content.sound = .default
        content.userInfo = ["route": "quiz"]
        let request = UNNotificationRequest(identifier: "quiz", content: content, trigger: nil)
        UNUserNotificationCenter.current().add(request) { _ in done() }
    }
}
