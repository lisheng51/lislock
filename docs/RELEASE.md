# 发布正式签名版（Android）

## 一次性设置：把签名密钥存进 GitHub Secrets

仓库 → **Settings → Secrets and variables → Actions → New repository secret**，添加 3 个：

| Name | Secret（值） |
|---|---|
| `ANDROID_KEYSTORE_BASE64` | `lislock-release.jks.base64.txt` 文件的全部内容（一整行） |
| `ANDROID_KEYSTORE_PASSWORD` | `password.txt` 里的密码 |
| `ANDROID_KEY_ALIAS` | `lislock` |

> ⚠️ **签名密钥（lislock-release.jks）和密码务必备份到安全的地方（密码管理器 / U 盘）。**
> 以后的每个版本都必须用同一把密钥签名，丢了就无法让已安装的 App 覆盖升级，只能卸载重装（数据会丢）。
> 密钥绝对不要提交到仓库里（.gitignore 已经忽略 *.jks）。

## 每次发布

1. 改好代码，推送到 `main`
2. 发布新版本（两种方式任选）：

   **方式 A（网页，推荐）**：仓库 → **Actions → Release → Run workflow**，输入版本号（如 `v0.2.0`）→ Run

   **方式 B（命令行）**：打一个版本标签并推送

   ```bash
   git tag v0.2.0
   git push origin v0.2.0
   ```

3. 等几分钟，GitHub Actions 的 **Release** 流程会自动：打包 → 签名 → 校验签名 → 在
   [Releases](https://github.com/lisheng51/lislock/releases) 页面发布 `lislock-v0.2.0.apk`

版本号取自标签（`v0.2.0` → versionName `0.2.0`），versionCode 自动递增。

## 安装说明

- 手机打开 Releases 页面，下载 APK 点开安装即可；以后新版本直接覆盖安装，设置和题库都会保留
- 如果手机上装过**调试版**（app-debug.apk），因为签名不同，需要先卸载调试版再装正式版
