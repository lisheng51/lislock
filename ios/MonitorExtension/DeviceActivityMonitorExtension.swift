import DeviceActivity
import Foundation

/// 系统在后台唤醒此扩展：使用时长达到阈值时加屏蔽。
final class DeviceActivityMonitorExtension: DeviceActivityMonitor {

    override func eventDidReachThreshold(_ event: DeviceActivityEvent.Name, activity: DeviceActivityName) {
        super.eventDidReachThreshold(event, activity: activity)
        guard event == .limitReached else { return }
        Shield.apply()
    }

    override func intervalDidEnd(for activity: DeviceActivityName) {
        super.intervalDidEnd(for: activity)
        // 一天结束：如果没在锁定中，就以新一天的全天区间重新开始监控
        guard activity == .usage, !SharedStore.locked else { return }
        try? Monitoring.restart(fromNow: false)
    }
}
