package com.identitycode.app

import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import com.identitycode.app.theme.Theme
import com.identitycode.app.ui.HomeScreen

class MainActivity : ComponentActivity() {

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()

        // 从桌面快捷方式进入（长按图标 → 淘宝/菜鸟/拼多多/兔喜生活身份码）：直接跳转，不在本地界面停留
        val platform = if (savedInstanceState == null) intent?.getStringExtra(EXTRA_PLATFORM) else null
        if (platform != null) {
            DeepLinks.open(this, platform)
            finish()
            return
        }

        setContent {
            Theme { HomeScreen() }
        }
    }

    companion object {
        /** 桌面快捷方式传入的平台标识：taobao / cainiao / pdd / tuxi */
        const val EXTRA_PLATFORM = "extra_platform"
    }
}