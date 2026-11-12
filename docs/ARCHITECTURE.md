# 架构设计

## 核心流程

```
开始守护 ──► 计时 ──► 累计 ≥ 上限？──否──► 继续计时
                          │
                          是
                          ▼
                       锁定手机 ──► 答题 ──► 全部答对？──否──► 换一组题重答
                                                 │
                                                 是
                                                 ▼
                                           解锁 + 计时清零
```

## 题目规则（两端一致）

- 每次解锁需答对 `questionCount` 道题（默认 3）
- 题目随机从两类中抽取：
  - 算术题：两位数加减、一位数乘法，四个选项
  - 题库题：来自 `shared/questions.json`
- 答错一道 → 整组重新抽题（防止反复猜）

## Android

| 组件 | 作用 |
|---|---|
| `GuardService` | 前台服务，每 5 秒 tick：屏幕亮且已解锁时累加使用时长；超时则拉起 `LockActivity` |
| `LockActivity` | 全屏答题界面；屏蔽返回键；被切走时服务会在下一个 tick 重新拉起 |
| `BootReceiver` | 开机 / App 更新后自动重启服务 |
| `Prefs` | SharedPreferences：上限、题数、已用时长、锁定状态 |
| `QuizGenerator` | 生成题目（读取 assets 中的 questions.json） |

需要的权限：
- `FOREGROUND_SERVICE` / `FOREGROUND_SERVICE_SPECIAL_USE`：常驻计时
- `SYSTEM_ALERT_WINDOW`：Android 10+ 后台启动 Activity 的豁免条件
- `POST_NOTIFICATIONS`：前台服务通知
- `RECEIVE_BOOT_COMPLETED`：开机自启
- `REQUEST_IGNORE_BATTERY_OPTIMIZATIONS`：防止被系统杀掉

> 用户仍可在系统设置中强制停止 App 或撤销权限。若要防止，可进一步做成
> 设备管理员（Device Admin）或配合"家长密码"，在路线图中。

## iOS

iOS 无法后台常驻计时，也无法在其他 App 上弹窗，因此完全依赖 Screen Time API：

1. **主 App** 请求 `AuthorizationCenter.requestAuthorization(for: .individual)`，
   用户用 `FamilyActivityPicker` 选择要限制的 App / 类别（可全选）。
2. 主 App 调用 `DeviceActivityCenter.startMonitoring`，设置一个从"现在"到当天 23:59 的
   区间，以及一个阈值为上限分钟数的 `DeviceActivityEvent`。
3. 系统统计到所选 App 使用时长达到阈值 → 唤醒 **MonitorExtension** 的
   `eventDidReachThreshold`，它通过 `ManagedSettingsStore` 给所选 App 加 Shield。
4. 用户打开被屏蔽的 App 时看到 **ShieldConfigExtension** 定制的界面，点击"去答题解锁"。
5. **ShieldActionExtension** 发出本地通知；用户点通知打开主 App 的答题页。
6. 答对后主 App 清空 Shield，并**重新 startMonitoring**（以当前时间为新区间起点，计时清零）。

主 App 与扩展通过 **App Group**（`UserDefaults(suiteName:)`）共享设置和选中的 App。
