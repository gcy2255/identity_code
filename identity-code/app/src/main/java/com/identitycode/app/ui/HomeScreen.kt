package com.identitycode.app.ui

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.safeDrawingPadding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.selection.SelectionContainer
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.alpha
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.lifecycle.Lifecycle
import androidx.lifecycle.LifecycleEventObserver
import androidx.lifecycle.compose.LocalLifecycleOwner
import com.identitycode.app.DeepLinks
import com.identitycode.app.theme.Theme

/** 淘宝品牌橙 */
private val TaobaoOrange = Color(0xFFFF5000)

/** 菜鸟品牌蓝 */
private val CainiaoBlue = Color(0xFF1266F1)

/** 拼多多品牌红 */
private val PddRed = Color(0xFFE02E24)

/** 兔喜生活品牌橙 */
private val TuxiOrange = Color(0xFFFF7A00)

@Composable
fun HomeScreen() {
    val context = LocalContext.current
    var taobaoInstalled by remember { mutableStateOf(DeepLinks.isInstalled(context, DeepLinks.PLATFORM_TAOBAO)) }
    var cainiaoInstalled by remember { mutableStateOf(DeepLinks.isInstalled(context, DeepLinks.PLATFORM_CAINIAO)) }
    var pddInstalled by remember { mutableStateOf(DeepLinks.isInstalled(context, DeepLinks.PLATFORM_PDD)) }
    var wechatInstalled by remember { mutableStateOf(DeepLinks.isInstalled(context, DeepLinks.PLATFORM_TUXI)) }
    var showTuxiDialog by remember { mutableStateOf(false) }

    // 用户可能跳去应用商店装完再回来，回到前台时刷新安装状态
    val lifecycleOwner = LocalLifecycleOwner.current
    DisposableEffect(lifecycleOwner) {
        val observer = LifecycleEventObserver { _, event ->
            if (event == Lifecycle.Event.ON_RESUME) {
                taobaoInstalled = DeepLinks.isInstalled(context, DeepLinks.PLATFORM_TAOBAO)
                cainiaoInstalled = DeepLinks.isInstalled(context, DeepLinks.PLATFORM_CAINIAO)
                pddInstalled = DeepLinks.isInstalled(context, DeepLinks.PLATFORM_PDD)
                wechatInstalled = DeepLinks.isInstalled(context, DeepLinks.PLATFORM_TUXI)
            }
        }
        lifecycleOwner.lifecycle.addObserver(observer)
        onDispose { lifecycleOwner.lifecycle.removeObserver(observer) }
    }

    HomeContent(
        taobaoInstalled = taobaoInstalled,
        cainiaoInstalled = cainiaoInstalled,
        pddInstalled = pddInstalled,
        wechatInstalled = wechatInstalled,
        onTaobaoClick = {
            if (taobaoInstalled) DeepLinks.open(context, DeepLinks.PLATFORM_TAOBAO)
            else DeepLinks.openStore(context, DeepLinks.TAOBAO_PKG)
        },
        onCainiaoClick = {
            if (cainiaoInstalled) DeepLinks.open(context, DeepLinks.PLATFORM_CAINIAO)
            else DeepLinks.openStore(context, DeepLinks.CAINIAO_PKG)
        },
        onPddClick = {
            if (pddInstalled) DeepLinks.open(context, DeepLinks.PLATFORM_PDD)
            else DeepLinks.openStore(context, DeepLinks.PDD_PKG)
        },
        onTuxiClick = {
            if (wechatInstalled) showTuxiDialog = true
            else DeepLinks.openStore(context, DeepLinks.WECHAT_PKG)
        },
    )

    if (showTuxiDialog) {
        TuxiMiniProgramDialog(
            onDismiss = { showTuxiDialog = false },
            onConfirm = {
                showTuxiDialog = false
                DeepLinks.openTuxi(context)
            },
        )
    }
}

/**
 * 兔喜生活身份码的说明弹窗。
 *
 * 微信小程序没有对外的唤起入口，这里把限制讲清楚，并把"复制链接 → 打开微信"做成一步。
 */
@Composable
private fun TuxiMiniProgramDialog(
    onDismiss: () -> Unit,
    onConfirm: () -> Unit,
) {
    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text(text = "兔喜生活 · 微信小程序") },
        text = {
            Column {
                Text(
                    text = "兔喜生活的身份码在微信小程序里，微信不允许其他 App 直接打开小程序。" +
                        "点下面的按钮会复制小程序链接并打开微信，把链接粘贴到任意聊天窗口发送，" +
                        "再点开这条消息就进小程序了。",
                    style = MaterialTheme.typography.bodyMedium,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                )
                Spacer(Modifier.height(12.dp))
                SelectionContainer {
                    Text(
                        text = DeepLinks.TUXI_MINI_LINK,
                        style = MaterialTheme.typography.bodyMedium,
                        fontWeight = FontWeight.Medium,
                        color = MaterialTheme.colorScheme.onSurface,
                    )
                }
            }
        },
        confirmButton = {
            TextButton(onClick = onConfirm) { Text("复制链接并打开微信") }
        },
        dismissButton = {
            TextButton(onClick = onDismiss) { Text("取消") }
        },
    )
}

@Composable
private fun HomeContent(
    taobaoInstalled: Boolean,
    cainiaoInstalled: Boolean,
    pddInstalled: Boolean,
    wechatInstalled: Boolean,
    onTaobaoClick: () -> Unit,
    onCainiaoClick: () -> Unit,
    onPddClick: () -> Unit,
    onTuxiClick: () -> Unit,
) {
    Column(
        modifier = Modifier
            .fillMaxSize()
            .background(MaterialTheme.colorScheme.background)
            .safeDrawingPadding()
            .padding(horizontal = 24.dp),
    ) {
        Spacer(Modifier.height(40.dp))
        Text(
            text = "身份码",
            style = MaterialTheme.typography.displaySmall,
            fontWeight = FontWeight.Bold,
            color = MaterialTheme.colorScheme.onBackground,
        )
        Spacer(Modifier.height(8.dp))
        Text(
            text = "取件时，一键唤醒身份码",
            style = MaterialTheme.typography.bodyLarge,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
        )

        Spacer(Modifier.height(32.dp))

        PlatformCard(
            badge = "淘",
            name = "淘宝 · 菜鸟驿站",
            subtitle = if (taobaoInstalled) "直达身份码页（推荐）" else "未安装 · 点击前往安装",
            brandColor = TaobaoOrange,
            installed = taobaoInstalled,
            onClick = onTaobaoClick,
        )

        Spacer(Modifier.height(16.dp))

        PlatformCard(
            badge = "菜",
            name = "菜鸟",
            subtitle = if (cainiaoInstalled) "打开菜鸟 · 右上角「出库码」" else "未安装 · 点击前往安装",
            brandColor = CainiaoBlue,
            installed = cainiaoInstalled,
            onClick = onCainiaoClick,
        )

        Spacer(Modifier.height(16.dp))

        PlatformCard(
            badge = "拼",
            name = "拼多多",
            subtitle = if (pddInstalled) "打开拼多多身份码" else "未安装 · 点击前往安装",
            brandColor = PddRed,
            installed = pddInstalled,
            onClick = onPddClick,
        )

        Spacer(Modifier.height(16.dp))

        PlatformCard(
            badge = "兔",
            name = "兔喜生活",
            subtitle = if (wechatInstalled) "微信小程序 · 复制链接后粘贴打开" else "未安装微信 · 点击前往安装",
            brandColor = TuxiOrange,
            installed = wechatInstalled,
            onClick = onTuxiClick,
        )

        Spacer(Modifier.weight(1f))

        Text(
            text = "身份码由淘宝 / 菜鸟 / 拼多多 / 兔喜生活生成并展示\n本应用仅负责快速跳转，不读取、不上传任何信息",
            style = MaterialTheme.typography.bodySmall,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
            textAlign = TextAlign.Center,
            modifier = Modifier.fillMaxWidth(),
        )
        Text(
            text = "提示：长按桌面图标可直接选择平台",
            style = MaterialTheme.typography.bodySmall,
            color = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.7f),
            textAlign = TextAlign.Center,
            modifier = Modifier
                .fillMaxWidth()
                .padding(top = 8.dp, bottom = 24.dp),
        )
    }
}

@Composable
private fun PlatformCard(
    badge: String,
    name: String,
    subtitle: String,
    brandColor: Color,
    installed: Boolean,
    onClick: () -> Unit,
) {
    Card(
        onClick = onClick,
        modifier = Modifier
            .fillMaxWidth()
            .alpha(if (installed) 1f else 0.72f),
        shape = RoundedCornerShape(28.dp),
        colors = CardDefaults.cardColors(
            containerColor = MaterialTheme.colorScheme.surfaceContainerHigh,
        ),
        elevation = CardDefaults.cardElevation(defaultElevation = 0.dp),
    ) {
        Row(
            modifier = Modifier.padding(horizontal = 20.dp, vertical = 22.dp),
            verticalAlignment = Alignment.CenterVertically,
        ) {
            Box(
                modifier = Modifier
                    .size(56.dp)
                    .clip(CircleShape)
                    .background(brandColor),
                contentAlignment = Alignment.Center,
            ) {
                Text(
                    text = badge,
                    color = Color.White,
                    fontSize = 24.sp,
                    fontWeight = FontWeight.Bold,
                )
            }

            Spacer(Modifier.width(16.dp))

            Column(
                modifier = Modifier.weight(1f),
                verticalArrangement = Arrangement.Center,
            ) {
                Text(
                    text = name,
                    style = MaterialTheme.typography.titleLarge,
                    fontWeight = FontWeight.SemiBold,
                    color = MaterialTheme.colorScheme.onSurface,
                )
                Spacer(Modifier.height(4.dp))
                Text(
                    text = subtitle,
                    style = MaterialTheme.typography.bodyMedium,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                )
            }

            Text(
                text = "›",
                fontSize = 30.sp,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
            )
        }
    }
}

@Preview(showBackground = true)
@Composable
private fun HomeContentPreview() {
    Theme(dynamicColor = false) {
        HomeContent(
            taobaoInstalled = true,
            cainiaoInstalled = true,
            pddInstalled = true,
            wechatInstalled = true,
            onTaobaoClick = {},
            onCainiaoClick = {},
            onPddClick = {},
            onTuxiClick = {},
        )
    }
}