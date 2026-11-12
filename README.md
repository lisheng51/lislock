# LisLock 屏幕时间守卫

一个跨平台（iOS / Android）的手机使用时间控制 App：
**连续使用手机超过设定时间（默认 1 小时）后，弹出锁定界面，必须答对几道题才能继续使用。**

## 功能

- 设置使用上限（默认 60 分钟）
- 超时后全屏锁定，必须答对 N 道题（默认 3 道）才能解锁
- 题目类型：算术题 + 题库选择题（内置 / 自定义 / 远程）
- 解锁后重新开始计时
- **家长密码**：设置后，修改设置 / 停止守护都要输密码；锁定页可用密码直接解锁；连续输错 5 次锁 60 秒
- **自定义题库**：在 App 里增删改题目、粘贴 JSON 批量导入；支持从网址**远程加载题库**并自动更新
- **难度递增**：同一天每多超时一次难度 +1（最高 Lv.3）——题数变多、数字变大、常识题变少
- 开机自动恢复监控（Android）

## 为什么是两套原生代码？

"监控使用时长 + 盖住其他 App" 这两件事在两个系统上都**只能用原生 API** 实现，
跨平台框架（Flutter / React Native）最终也要写同样的原生代码，所以本项目直接用原生：

| | Android（Kotlin） | iOS（Swift / SwiftUI） |
|---|---|---|
| 统计使用时间 | 前台服务 + 屏幕亮屏/解锁状态计时 | `DeviceActivity` 框架（系统统计） |
| 超时触发 | 服务里判断累计时间 ≥ 上限 | `DeviceActivityMonitor` 扩展的 `eventDidReachThreshold` |
| 锁定方式 | 全屏 `LockActivity`（悬浮窗权限允许后台拉起，离开会被重新拉起） | `ManagedSettings` 给 App 加"屏蔽罩"（Shield） |
| 答题 | 直接在锁定界面答题 | 屏蔽罩按钮 → 推送通知 → 打开主 App 答题 → 移除屏蔽 |

> **iOS 限制说明**：Apple 不允许第三方 App 在其他 App 上弹自定义窗口，只能用系统 Shield。
> 而且 Shield 上的按钮不能直接打开我们的 App，所以通过一条本地通知跳转到主 App 答题。
> 使用 `FamilyControls` 需要 Apple 的 **Family Controls 权限（entitlement）**：开发调试可直接用，
> 上架 App Store 前需要在 [Apple Developer](https://developer.apple.com/contact/request/family-controls-distribution) 申请。

详细设计见 [docs/ARCHITECTURE.md](docs/ARCHITECTURE.md)。

## 目录结构

```
shared/questions.json      两端共用的自定义题库
android/                   Android 工程（Gradle，Kotlin + Jetpack Compose）
ios/                       iOS 工程（XcodeGen 生成 Xcode 工程，SwiftUI）
  ScreenTimeGuard/         主 App：设置、选择要监控的 App、答题解锁
  MonitorExtension/        DeviceActivityMonitor：到时间后加屏蔽
  ShieldConfigExtension/   自定义屏蔽界面文案
  ShieldActionExtension/   屏蔽界面按钮 → 发通知引导去答题
  Shared/                  App Group 共享的设置 / 题目逻辑
```

## 在 Windows 电脑上测试

> ⚠️ **iOS 版无法在 Windows 上编译或运行**（必须 Mac + Xcode + iPhone 真机）。
> Windows 上可以完整测试 **Android 版**。仓库的 GitHub Actions 会在 macOS 上帮你检查 iOS 代码能否编译通过。

### 方式 A：直接下载 APK 装到安卓手机（最快）

1. 打开仓库的 **Actions** 页 → 最新一次 *Build* → 底部 **Artifacts** 下载 `lislock-debug-apk`
2. 解压得到 `app-debug.apk`，发到安卓手机上安装（需允许"安装未知来源应用"）

### 方式 B：Android Studio + 模拟器

1. 安装 [Android Studio](https://developer.android.com/studio)（自带 JDK 和 Android SDK）
2. 安装 [Git for Windows](https://git-scm.com/download/win)，然后 `git clone https://github.com/lisheng51/lislock.git`
3. Android Studio → **Open** → 选择 `lislock\android` 文件夹，等待 Gradle 同步完成
4. **Device Manager** → 新建一个模拟器（如 Pixel 8，系统镜像 API 34）
5. 点绿色 ▶ **Run**

命令行也可以（PowerShell，在 `lislock\android` 目录下）：

```powershell
.\gradlew.bat assembleDebug
adb install -r app\build\outputs\apk\debug\app-debug.apk
```

### 测试步骤

1. 授予三项权限 → 把**使用上限调到 1 分钟** → 点"开始守护"
2. 随便用 1 分钟 → 应弹出答题锁定页；按 Home 键离开，几秒后会被拉回来
3. 故意答错 → 整组换题；全部答对 → 解锁，计时清零
4. 再等 1 分钟 → 第二次锁定，显示"难度提升到 Lv.1"，题目变难变多
5. 设置家长密码 → 返回桌面再进 App，设置页应被锁定；锁定页出现"家长解锁"按钮
6. 连续输错 5 次密码 → 应提示等待 60 秒
7. 重启模拟器 → 守护自动恢复

## 运行

### Android

需要 Android Studio（或 Android SDK 34+）：

```bash
cd android
./gradlew installDebug
```

首次打开 App 依次授予：**悬浮窗（显示在其他应用上层）**、**通知**、**忽略电池优化**，然后点"开始守护"。

### iOS

需要 macOS + Xcode 15+、真机（模拟器不支持 Screen Time API）：

```bash
brew install xcodegen
cd ios
xcodegen generate
open ScreenTimeGuard.xcodeproj
```

在 Xcode 中把 4 个 target 的 Team 设为你的开发者账号，并把 `project.yml` 里的
`com.example.screentimeguard` / `group.com.example.screentimeguard` 改成你自己的 Bundle ID 和 App Group。

## 题库

出题时从**算术题**和**题库题**里随机抽。题库有三个来源，可以同时使用：

| 来源 | 说明 |
|---|---|
| 内置题库 | `shared/questions.json`，打包进 App，可在题库管理中关闭 |
| 自定义题目 | 家长在 App 里「设置 → 管理题库」中添加 / 编辑 / 删除，或粘贴 JSON 批量导入 |
| 远程题库 | 填一个 https 网址，App 下载后缓存；Android 守护运行时每 6 小时更新，iOS 打开 App 时每 6 小时更新 |

打开「只出题库题」后不再出算术题。否则题库题的占比随难度降低：Lv.0 50% → Lv.3 20%。

### 题库 JSON 格式

```json
{
  "version": 1,
  "questions": [
    { "q": "apple 是什么意思？", "options": ["香蕉", "苹果", "橙子", "葡萄"], "answer": 1 },
    { "q": "3 × 7 = ?", "options": ["21", "24"], "answer": 0 }
  ]
}
```

- `q`：题目；`options`：2–6 个选项，不能重复；`answer`：正确选项的序号，**从 0 开始**
- 也可以直接写成数组 `[ {...}, {...} ]`
- 最多 2000 题，文件不超过 1 MB；格式错误时 App 会提示是第几题出错，并保留旧题库
- 出题时选项顺序会被打乱，不用担心孩子记位置

### 远程题库放在哪里？

任何能通过 **https 直接返回 JSON 原文**的地址都可以，例如：

- **GitHub Gist**（推荐，免费）：新建 gist → 粘贴 JSON → 创建后点 **Raw**，复制网址。
  要更新题目时编辑 gist，网址里去掉 commit 哈希（形如 `https://gist.githubusercontent.com/<用户>/<id>/raw/questions.json`），App 就总能拿到最新版
- 公开 GitHub 仓库里的文件（点 Raw 得到 `raw.githubusercontent.com/...` 网址）
- 自己的服务器、对象存储（OSS / COS / S3）等

> 本仓库是私有的，所以 `raw.githubusercontent.com/lisheng51/lislock/...` 这类网址**手机上访问不了**，请用 Gist 或其他公开地址。

## 路线图

- [ ] 按 App 分别设置时长（Android 用 UsageStatsManager）
- [x] 家长密码
- [ ] 远程管理 / 防卸载（Android 设备管理员）
- [ ] 每日总时长统计图表
- [x] 题目难度随超时次数递增
