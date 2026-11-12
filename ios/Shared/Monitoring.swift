import Foundation
import DeviceActivity

extension DeviceActivityName {
    static let usage = Self("usage")
}

extension DeviceActivityEvent.Name {
    static let limitReached = Self("limitReached")
}

/// 启动 / 重启系统的使用时长监控。
/// DeviceActivity 统计的是"区间内"的使用时间，所以每次解锁后用"从现在开始"的新区间重启监控，
/// 相当于计时清零；到了当天 23:59 区间结束时，扩展会再以 00:00 开始的全天区间重启。
enum Monitoring {
    static let center = DeviceActivityCenter()

    static func restart(fromNow: Bool = true) throws {
        center.stopMonitoring([.usage])

        let cal = Calendar.current
        let end = DateComponents(hour: 23, minute: 59, second: 59)
        var start = DateComponents(hour: 0, minute: 0, second: 0)
        if fromNow {
            // 系统要求区间至少 15 分钟；太晚时把起点提前，保证合法
            let latestStart = cal.date(bySettingHour: 23, minute: 44, second: 0, of: Date())!
            let startDate = min(Date(), latestStart)
            start = cal.dateComponents([.hour, .minute, .second], from: startDate)
        }

        let schedule = DeviceActivitySchedule(intervalStart: start, intervalEnd: end, repeats: false)

        let s = SharedStore.selection
        let threshold = DateComponents(minute: SharedStore.limitMinutes)
        let event: DeviceActivityEvent
        if #available(iOS 17.4, *) {
            event = DeviceActivityEvent(
                applications: s.applicationTokens,
                categories: s.categoryTokens,
                webDomains: s.webDomainTokens,
                threshold: threshold,
                includesPastActivity: false
            )
        } else {
            event = DeviceActivityEvent(
                applications: s.applicationTokens,
                categories: s.categoryTokens,
                webDomains: s.webDomainTokens,
                threshold: threshold
            )
        }

        try center.startMonitoring(.usage, during: schedule, events: [.limitReached: event])
    }

    static func stop() {
        center.stopMonitoring()
    }
}
