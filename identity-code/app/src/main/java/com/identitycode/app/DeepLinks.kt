package com.identitycode.app

import android.content.ActivityNotFoundException
import android.content.ComponentName
import android.content.Context
import android.content.Intent
import android.net.Uri
import android.util.Log
import android.widget.Toast

/**
 * 平台深链跳转。
 *
 * 身份码由各平台 App 自己生成并展示（绑定账号、实时刷新），本应用只负责"一键直达"，
 * 不读取、不缓存、不转发任何身份码内容。
 */
object DeepLinks {

    private const val TAG = "DeepLinks"

    const val PLATFORM_TAOBAO = "taobao"
    const val PLATFORM_CAINIAO = "cainiao"
    const val PLATFORM_PDD = "pdd"
    const val PLATFORM_TUXI = "tuxi"

    const val TAOBAO_PKG = "com.taobao.taobao"
    const val CAINIAO_PKG = "com.cainiao.wireless"
    const val PDD_PKG = "com.xunmeng.pinduoduo"
    const val WECHAT_PKG = "com.tencent.mm"

    /**
     * 兔喜生活身份码（微信小程序）。
     *
     * 这个身份码只存在于微信小程序里，没有独立 App。微信小程序的对外入口只有官方
     * 「URL Scheme / URL Link」（weixin://dl/business/?t=…），ticket 必须由小程序方
     * 用 appid+access_token 调接口签发，第三方拿不到；`#小程序://` 这种分享链接是
     * 微信内部格式，外部 App 无法解析。所以这里只能"复制链接 + 打开微信"，
     * 由用户在微信聊天里粘贴发送后点开。
     */
    const val TUXI_MINI_LINK = "#小程序://兔喜生活/OcNmpwU98COjZ1C"
    const val TUXI_MINI_NAME = "兔喜生活"

    /** 淘宝内置浏览器 Activity（manifest 中 exported=true，第三方可显式启动） */
    private const val TAOBAO_BROWSER_ACTIVITY = "com.taobao.browser.BrowserActivity"

    /**
     * 菜鸟驿站身份码 H5 页（last-mile 前端）。
     *
     * 真机实测（2026-10，Redmi K70 Pro）：用淘宝内置浏览器加载这个地址，会直接落进
     * 菜鸟驿站"身份码"页（标题「身份码」+ 条形码 + 号码管理/问题提报），
     * 不需要用户自己在淘宝里翻二级菜单。forceThemis=true 让淘宝用端内 themis 容器渲染。
     */
    const val TAOBAO_IDENTITY_URL =
        "https://pages-fast.m.taobao.com/wow/z/uniapp/1100410/last-mile-fe/m-end-identity-code/home" +
            "?x-ssr=true&skeleton=true&forceThemis=true"

    fun displayName(platform: String): String = when (platform) {
        PLATFORM_TAOBAO -> "淘宝"
        PLATFORM_CAINIAO -> "菜鸟"
        PLATFORM_PDD -> "拼多多"
        PLATFORM_TUXI -> TUXI_MINI_NAME
        else -> platform
    }

    fun packageOf(platform: String): String? = when (platform) {
        PLATFORM_TAOBAO -> TAOBAO_PKG
        PLATFORM_CAINIAO -> CAINIAO_PKG
        PLATFORM_PDD -> PDD_PKG
        // 兔喜生活没有独立 App，承载它的是微信
        PLATFORM_TUXI -> WECHAT_PKG
        else -> null
    }

    fun isInstalled(context: Context, platform: String): Boolean {
        val pkg = packageOf(platform) ?: return false
        return try {
            if (android.os.Build.VERSION.SDK_INT >= android.os.Build.VERSION_CODES.TIRAMISU) {
                context.packageManager.getPackageInfo(pkg, android.content.pm.PackageManager.PackageInfoFlags.of(0))
            } else {
                @Suppress("DEPRECATION")
                context.packageManager.getPackageInfo(pkg, 0)
            }
            true
        } catch (_: Exception) {
            false
        }
    }

    /** 入口：点击卡片或桌面快捷方式 */
    fun open(context: Context, platform: String) {
        val pkg = packageOf(platform) ?: return
        if (!isInstalled(context, platform)) {
            Toast.makeText(context, "未安装${displayName(platform)}，正在跳转应用商店", Toast.LENGTH_SHORT).show()
            openStore(context, pkg)
            return
        }
        when (platform) {
            PLATFORM_TAOBAO -> openTaobao(context)
            PLATFORM_CAINIAO -> openCainiao(context)
            PLATFORM_PDD -> openPdd(context)
            PLATFORM_TUXI -> openTuxi(context)
        }
    }

    /**
     * 兔喜生活身份码：复制小程序链接 → 打开微信。
     *
     * 微信小程序无法被外部 App 直接唤起（没有可用的 scheme，ticket 需要小程序方签发），
     * 唯一稳定的"半自动"路径就是把 `#小程序://兔喜生活/…` 复制到剪贴板，
     * 用户切到微信任意聊天窗口粘贴发送，点这条消息就进小程序了。
     */
    fun openTuxi(context: Context) {
        copyText(context, TUXI_MINI_LINK)
        if (launchApp(context, WECHAT_PKG)) {
            Toast.makeText(
                context,
                "小程序链接已复制，在微信聊天里粘贴发送后点开即可",
                Toast.LENGTH_LONG,
            ).show()
            return
        }
        Toast.makeText(context, "链接已复制，打开微信粘贴发送即可", Toast.LENGTH_LONG).show()
    }

    /** 复制到剪贴板（Android 13+ 系统会自己给一个"已复制"提示，所以只复制不 toast） */
    fun copyText(context: Context, text: String) {
        try {
            val cm = context.getSystemService(Context.CLIPBOARD_SERVICE) as? android.content.ClipboardManager
            cm?.setPrimaryClip(android.content.ClipData.newPlainText("tuxi_mini_program", text))
        } catch (e: Exception) {
            Log.w(TAG, "Clipboard failed", e)
        }
    }

    /**
     * 淘宝 → 菜鸟驿站 → 身份码（推荐路径，真机已跑通）。
     *
     * 淘宝没有给外部开放小程序/themis 入口（TMSActivity 未导出），但内置浏览器
     * com.taobao.browser.BrowserActivity 是导出的，把身份码 H5 地址丢给它就能直达。
     * 失败时依次降级：普通 https 深链 → taobao:// scheme → 淘宝首页。
     */
    private fun openTaobao(context: Context) {
        val explicit = Intent(Intent.ACTION_VIEW, Uri.parse(TAOBAO_IDENTITY_URL)).apply {
            component = ComponentName(TAOBAO_PKG, TAOBAO_BROWSER_ACTIVITY)
            addFlags(Intent.FLAG_ACTIVITY_CLEAR_TOP or Intent.FLAG_ACTIVITY_SINGLE_TOP)
            if (context !is android.app.Activity) addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
        }
        if (tryStart(context, explicit)) {
            Log.i(TAG, "Opened Taobao identity code via BrowserActivity")
            return
        }
        if (tryOpen(context, TAOBAO_IDENTITY_URL, TAOBAO_PKG)) return
        if (tryOpen(
                context,
                "taobao://pages-fast.m.taobao.com/wow/z/uniapp/1100410/last-mile-fe/m-end-identity-code/home",
                TAOBAO_PKG,
            )
        ) return
        if (launchApp(context, TAOBAO_PKG)) {
            Toast.makeText(context, "已打开淘宝，请从「我的淘宝 → 菜鸟」进入身份码", Toast.LENGTH_LONG).show()
            return
        }
        Toast.makeText(context, "打开淘宝失败", Toast.LENGTH_SHORT).show()
    }

    /**
     * 菜鸟身份码（真机实测结论，2026-10）：
     *
     * 身份码页面是 com.cainiao.wireless/.identity_code.IdentityCodeActivity，manifest 里
     * android:exported="false"；而 guoguo://go/station_code 的 intent-filter 恰好挂在这个
     * 非导出 Activity 上，只服务于菜鸟自己的内部路由和桌面快捷方式。
     *
     * 结果：任何第三方 App（连 adb shell 也一样）调 startActivity 都会被系统拒绝
     * （SecurityException: not exported from uid xxx）。旧版本流传的
     * cainiao://desktop/station_code 在现版本直接解析不到。
     *
     * 所以只能打开菜鸟主界面，由用户点首页右上角「出库码」进入身份码页。
     */
    private fun openCainiao(context: Context) {
        if (launchApp(context, CAINIAO_PKG)) {
            Toast.makeText(context, "已打开菜鸟，点首页右上角「出库码」出示身份码", Toast.LENGTH_LONG).show()
            return
        }
        Toast.makeText(context, "打开菜鸟失败", Toast.LENGTH_SHORT).show()
    }

    /** 拼多多身份码：mdkd/package 页面的 ID_CODE tab */
    private fun openPdd(context: Context) {
        val h5 = "https://m.pinduoduo.net/mdkd/package?entry_source=18&extra_params=from_wx_jump%3D1&p_channel=0"
        val uris = listOf(
            "pinduoduo://com.xunmeng.pinduoduo/mdkd/package?tab=ID_CODE&entry_source=11&refer_page_name=login&refer_page_sn=10169",
            "pinduoduo://com.xunmeng.pinduoduo/mdkd/package?tab=ID_CODE&entry_source=11",
            "pinduoduo://com.xunmeng.pinduoduo/mdkd/package",
        )
        for (uri in uris) {
            if (tryOpen(context, uri, PDD_PKG)) return
        }
        if (tryOpen(context, h5, PDD_PKG)) return
        if (launchApp(context, PDD_PKG)) {
            Toast.makeText(context, "已打开拼多多，请手动进入「身份码」页面", Toast.LENGTH_LONG).show()
            return
        }
        Toast.makeText(context, "打开拼多多失败", Toast.LENGTH_SHORT).show()
    }

    /** 用带 package 约束的隐式 Intent 打开深链；解析不到就返回 false 交给下一级兜底 */
    private fun tryOpen(context: Context, uriStr: String, pkg: String?): Boolean {
        return try {
            val intent = Intent(Intent.ACTION_VIEW, Uri.parse(uriStr)).apply {
                if (pkg != null) setPackage(pkg)
                addFlags(Intent.FLAG_ACTIVITY_CLEAR_TOP or Intent.FLAG_ACTIVITY_SINGLE_TOP)
                if (context !is android.app.Activity) addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
            }
            val resolved: ComponentName? = intent.resolveActivity(context.packageManager)
            if (resolved == null) {
                Log.i(TAG, "No activity for $uriStr (pkg=$pkg)")
                false
            } else {
                context.startActivity(intent)
                Log.i(TAG, "Opened $uriStr -> ${resolved.flattenToShortString()}")
                true
            }
        } catch (e: ActivityNotFoundException) {
            Log.w(TAG, "Activity not found: $uriStr", e)
            false
        } catch (e: SecurityException) {
            Log.w(TAG, "Security exception: $uriStr", e)
            false
        } catch (e: Exception) {
            Log.w(TAG, "Open failed: $uriStr", e)
            false
        }
    }

    /** 兜底：直接拉起 App 主界面 */
    private fun launchApp(context: Context, pkg: String): Boolean {
        val launchIntent = context.packageManager.getLaunchIntentForPackage(pkg) ?: return false
        launchIntent.addFlags(Intent.FLAG_ACTIVITY_CLEAR_TOP or Intent.FLAG_ACTIVITY_SINGLE_TOP)
        if (context !is android.app.Activity) launchIntent.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
        return try {
            context.startActivity(launchIntent)
            true
        } catch (e: Exception) {
            Log.w(TAG, "Could not launch $pkg", e)
            false
        }
    }

    /** 未安装时跳应用商店（market:// 优先，失败再用网页） */
    fun openStore(context: Context, pkg: String) {
        val market = Intent(Intent.ACTION_VIEW, Uri.parse("market://details?id=$pkg")).apply {
            addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
        }
        if (tryStart(context, market)) return
        val web = Intent(Intent.ACTION_VIEW, Uri.parse("https://www.coolapk.com/apk/$pkg")).apply {
            addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
        }
        if (!tryStart(context, web)) {
            Toast.makeText(context, "请到应用商店搜索安装", Toast.LENGTH_LONG).show()
        }
    }

    private fun tryStart(context: Context, intent: Intent): Boolean = try {
        context.startActivity(intent)
        true
    } catch (e: Exception) {
        Log.w(TAG, "startActivity failed: $intent", e)
        false
    }
}