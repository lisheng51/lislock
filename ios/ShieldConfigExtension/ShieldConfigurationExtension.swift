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
            title: .init(text: "Je zit al meer dan \(SharedStore.limitMinutes) minuten op je telefoon", color: .label),
            subtitle: .init(text: "Beantwoord \(SharedStore.unlockQuestionCount) vragen goed om verder te gaan", color: .secondaryLabel),
            primaryButtonLabel: .init(text: "Vragen beantwoorden", color: .white),
            primaryButtonBackgroundColor: .systemBlue,
            secondaryButtonLabel: .init(text: "Even pauze", color: .systemBlue)
        )
    }
}
