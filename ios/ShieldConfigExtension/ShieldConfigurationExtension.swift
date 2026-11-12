import ManagedSettings
import ManagedSettingsUI
import UIKit

/// 自定义系统屏蔽界面的文案和按钮。
final class ShieldConfigurationExtension: ShieldConfigurationDataSource {

    override func configuration(shielding application: Application) -> ShieldConfiguration {
        makeConfig()
    }

    override func configuration(shielding application: Application, in category: ActivityCategory) -> ShieldConfiguration {
        makeConfig()
    }

    override func configuration(shielding webDomain: WebDomain) -> ShieldConfiguration {
        makeConfig()
    }

    override func configuration(shielding webDomain: WebDomain, in category: ActivityCategory) -> ShieldConfiguration {
        makeConfig()
    }

    private func makeConfig() -> ShieldConfiguration {
        ShieldConfiguration(
            backgroundBlurStyle: .systemMaterial,
            icon: UIImage(systemName: "hourglass"),
            title: .init(text: "已使用超过 \(SharedStore.limitMinutes) 分钟", color: .label),
            subtitle: .init(text: "回答 \(SharedStore.questionCount) 道题即可解锁", color: .secondaryLabel),
            primaryButtonLabel: .init(text: "去答题解锁", color: .white),
            primaryButtonBackgroundColor: .systemBlue,
            secondaryButtonLabel: .init(text: "休息一下", color: .systemBlue)
        )
    }
}
