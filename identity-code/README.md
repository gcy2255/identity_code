# 身份码

一个极简的 Android 工具 App：把「菜鸟身份码」和「拼多多身份码」变成一键直达。

## 它做什么

- 打开 App → 两个大按钮 → 点击直接跳到对应 App 的身份码页面
- 长按桌面图标 → 直接选择「菜鸟身份码」/「拼多多身份码」（静态快捷方式）
- 目标 App 未安装时：自动跳应用商店
- 深链失效时：自动退回"打开 App 主界面"并给出提示

## 它不做什么

身份码由菜鸟 / 拼多多 App 根据你的账号实时生成并展示，本应用**不读取、不缓存、不上传**任何身份码内容，只负责快速跳转。

## 跳转协议

| 平台 | 包名 | 深链 | 真机结果 |
| --- | --- | --- | --- |
| 菜鸟 | `com.cainiao.wireless` | 无可用深链（见下） | 只能打开主界面 |
| 拼多多 | `com.xunmeng.pinduoduo` | `pinduoduo://com.xunmeng.pinduoduo/mdkd/package?tab=ID_CODE` | ✅ 直达身份码页 |

拼多多有多级兜底：完整参数深链 → 精简参数深链 → H5 链接 → 主界面。

### 菜鸟为什么不能直达（Redmi K70 Pro / Android 15 实测）

菜鸟的身份码页是 `com.cainiao.wireless/.identity_code.IdentityCodeActivity`，
在 manifest 中 `android:exported="false"`；`guoguo://go/station_code` 的 intent-filter
恰好挂在这个非导出 Activity 上，只对菜鸟自己的内部路由和桌面快捷方式生效。

因此任何第三方 App 调 `startActivity` 都会被系统拒绝
（`SecurityException: not exported from uid ...`），流传的
`cainiao://desktop/station_code` 在现版本更是直接解析不到；对外导出的
`cainiao://router?url=` 打开后立即自行结束（需要登录态）。

结论：菜鸟只能"打开主界面 + 用户点右上角『出库码』"。
如需真正一键直达，唯一的技术路径是加无障碍服务自动点击（需用户在系统设置里授权，尚未实现）。

深链解析失败不会崩溃，逐级回退。

## 技术栈

- Kotlin + Jetpack Compose（Material 3）
- AGP 9 / Gradle 9.1 / compileSdk 36 / minSdk 26
- 单 Activity，无第三方运行时依赖

## 目录

```
app/src/main/java/com/identitycode/app/
├── MainActivity.kt        # 单 Activity，处理快捷方式 extra
├── DeepLinks.kt           # 深链 / 兜底 / 商店跳转
└── ui/HomeScreen.kt       # 主页 UI
app/src/main/res/xml/shortcuts.xml   # 桌面长按快捷方式
app/src/main/AndroidManifest.xml     # <queries> 包可见性声明
```

Android 11+ 必须声明 `<queries>`，否则 `resolveActivity` 恒为 null、`getPackageInfo` 抛异常——本项目已在 Manifest 中声明两个包和两个 scheme。

## 构建

```bash
# Windows（git-bash）：gradlew 脚本会把 POSIX 路径传给 Java，直接调 wrapper 类更稳
java -cp "C:\\身份码\\identity-code\\gradle\\wrapper\\gradle-wrapper.jar" \
     org.gradle.wrapper.GradleWrapperMain assembleDebug
```

产物：`app/build/outputs/apk/debug/app-debug.apk`

安装：

```bash
adb install -r app/build/outputs/apk/debug/app-debug.apk
```