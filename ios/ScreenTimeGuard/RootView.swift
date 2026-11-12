import SwiftUI
import FamilyControls
import UserNotifications

struct RootView: View {
    @Environment(\.scenePhase) private var scenePhase
    @State private var locked = SharedStore.locked

    var body: some View {
        Group {
            if locked {
                QuizView(onPassed: {
                    Shield.clear()
                    try? Monitoring.restart(fromNow: true)
                    locked = false
                })
            } else {
                SettingsView()
            }
        }
        .onChange(of: scenePhase) { _ in locked = SharedStore.locked }
        .onReceive(NotificationCenter.default.publisher(for: .openQuiz)) { _ in locked = SharedStore.locked }
    }
}

struct SettingsView: View {
    @ObservedObject private var auth = AuthorizationCenter.shared
    @State private var limit = Double(SharedStore.limitMinutes)
    @State private var count = Double(SharedStore.questionCount)
    @State private var selection = SharedStore.selection
    @State private var showPicker = false
    @State private var running = false
    @State private var error: String?

    var body: some View {
        NavigationStack {
            Form {
                Section("权限") {
                    if auth.authorizationStatus == .approved {
                        Label("已获得屏幕使用时间权限", systemImage: "checkmark.seal.fill")
                    } else {
                        Button("授权屏幕使用时间") { Task { await requestAuth() } }
                    }
                }

                Section("规则") {
                    VStack(alignment: .leading) {
                        Text("使用上限：\(Int(limit)) 分钟")
                        Slider(value: $limit, in: 5...180, step: 5)
                    }
                    VStack(alignment: .leading) {
                        Text("解锁需答对：\(Int(count)) 道题")
                        Slider(value: $count, in: 1...10, step: 1)
                    }
                    Button("选择要限制的 App（\(itemCount) 项）") { showPicker = true }
                        .familyActivityPicker(isPresented: $showPicker, selection: $selection)
                }

                Section {
                    Button(running ? "重新开始守护" : "开始守护", action: start)
                        .disabled(auth.authorizationStatus != .approved || itemCount == 0)
                    if running {
                        Button("停止守护", role: .destructive) {
                            Monitoring.stop(); Shield.clear(); running = false
                        }
                    }
                } footer: {
                    if let error { Text(error).foregroundStyle(.red) }
                    else { Text("建议在选择器中勾选\"所有 App 和类别\"，以统计整机使用时间。") }
                }
            }
            .navigationTitle("屏幕时间守卫")
        }
    }

    private var itemCount: Int {
        selection.applicationTokens.count + selection.categoryTokens.count + selection.webDomainTokens.count
    }

    private func requestAuth() async {
        do {
            try await AuthorizationCenter.shared.requestAuthorization(for: .individual)
            _ = try? await UNUserNotificationCenter.current().requestAuthorization(options: [.alert, .sound])
        } catch {
            self.error = "授权失败：\(error.localizedDescription)"
        }
    }

    private func start() {
        SharedStore.limitMinutes = Int(limit)
        SharedStore.questionCount = Int(count)
        SharedStore.selection = selection
        do {
            try Monitoring.restart(fromNow: true)
            running = true
            error = nil
        } catch {
            self.error = "启动监控失败：\(error.localizedDescription)"
        }
    }
}
