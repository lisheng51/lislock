import SwiftUI
import FamilyControls
import DeviceActivity
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
    @State private var running = !DeviceActivityCenter().activities.isEmpty
    @State private var error: String?
    @State private var pinSet = ParentPin.isSet
    @State private var parentVerified = false
    @State private var showVerify = false
    @State private var showSetPin = false
    @Environment(\.scenePhase) private var scenePhase

    /// 没设家长密码，或本次已通过验证，才能修改设置
    private var canEdit: Bool { !pinSet || parentVerified }

    var body: some View {
        NavigationStack {
            Form {
                if !canEdit {
                    Section {
                        HStack {
                            Label("设置已被家长锁定", systemImage: "lock.fill")
                            Spacer()
                            Button("家长解锁") { showVerify = true }
                        }
                    }
                }

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
                        Slider(value: $limit, in: 1...180, step: 1)
                    }
                    VStack(alignment: .leading) {
                        Text("解锁需答对：\(Int(count)) 道题")
                        Slider(value: $count, in: 1...10, step: 1)
                    }
                    Button("选择要限制的 App（\(itemCount) 项）") { showPicker = true }
                        .familyActivityPicker(isPresented: $showPicker, selection: $selection)
                }
                .disabled(!canEdit)

                Section {
                    if !pinSet {
                        Button("设置家长密码") { showSetPin = true }
                    } else if canEdit {
                        Button("修改家长密码") { showSetPin = true }
                        Button("删除家长密码", role: .destructive) { ParentPin.clear(); pinSet = false }
                    } else {
                        Label("已设置", systemImage: "checkmark.seal.fill")
                    }
                } header: {
                    Text("家长密码")
                } footer: {
                    Text("设置后，修改设置、停止守护都需要密码；孩子答不出题时也可以用密码直接解锁。")
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
                .disabled(!canEdit)
            }
            .navigationTitle("屏幕时间守卫")
            .sheet(isPresented: $showVerify) { VerifyPinSheet { parentVerified = true } }
            .sheet(isPresented: $showSetPin) {
                SetPinSheet { ParentPin.set($0); pinSet = true; parentVerified = true }
            }
            // 离开 App 就重新上锁
            .onChange(of: scenePhase) { phase in if phase == .background { parentVerified = false } }
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
