package me.neko.nzhelper.feature.onboarding

import android.Manifest
import android.content.Context
import android.content.Intent
import android.os.Build
import android.provider.Settings
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.core.FastOutSlowInEasing
import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.animation.core.tween
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.ExperimentalLayoutApi
import androidx.compose.foundation.layout.FlowRow
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.pager.HorizontalPager
import androidx.compose.foundation.pager.rememberPagerState
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.outlined.BrightnessAuto
import androidx.compose.material.icons.outlined.Cake
import androidx.compose.material.icons.outlined.Celebration
import androidx.compose.material.icons.outlined.CheckCircle
import androidx.compose.material.icons.outlined.DarkMode
import androidx.compose.material.icons.outlined.Favorite
import androidx.compose.material.icons.outlined.FavoriteBorder
import androidx.compose.material.icons.outlined.Female
import androidx.compose.material.icons.outlined.LightMode
import androidx.compose.material.icons.outlined.Male
import androidx.compose.material.icons.outlined.Notifications
import androidx.compose.material.icons.outlined.Palette
import androidx.compose.material.icons.outlined.ViewInAr
import androidx.compose.material3.Button
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.DropdownMenuItem
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.ExposedDropdownMenu
import androidx.compose.material3.ExposedDropdownMenuAnchorType
import androidx.compose.material3.ExposedDropdownMenuBox
import androidx.compose.material3.ExposedDropdownMenuDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.LinearProgressIndicator
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Switch
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.alpha
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.core.app.NotificationManagerCompat
import kotlinx.coroutines.launch
import me.neko.nzhelper.core.datastore.AgeGroupSettings
import me.neko.nzhelper.core.datastore.RecordModeSettings
import me.neko.nzhelper.core.datastore.ThemeSettings
import me.neko.nzhelper.core.model.SessionMode
import me.neko.nzhelper.ui.component.wizard.OptionCard
import me.neko.nzhelper.ui.component.wizard.PageHeader
import me.neko.nzhelper.ui.component.wizard.SummaryRow
import me.neko.nzhelper.ui.theme.LocalThemeState
import me.neko.nzhelper.ui.theme.ThemeColorOptions
import java.time.LocalDate
import java.time.YearMonth
import java.time.format.DateTimeFormatter
import java.time.temporal.ChronoUnit

private const val PAGE_COUNT = 6

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun OnboardingScreen(
    onFinish: () -> Unit
) {
    val context = LocalContext.current
    val scope = rememberCoroutineScope()
    val pagerState = rememberPagerState(pageCount = { PAGE_COUNT })

    val canGoBack = pagerState.currentPage > 0
    val isLastPage = pagerState.currentPage == PAGE_COUNT - 1

    val progressTarget = (pagerState.currentPage + 1) / PAGE_COUNT.toFloat()
    val animatedProgress by animateFloatAsState(
        targetValue = progressTarget,
        animationSpec = tween(durationMillis = 500, easing = FastOutSlowInEasing),
        label = "onboardingProgress"
    )

    Scaffold(
        containerColor = MaterialTheme.colorScheme.surfaceContainer
    ) { innerPadding ->
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(innerPadding)
                .padding(horizontal = 24.dp)
        ) {
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(top = 12.dp),
                verticalAlignment = Alignment.CenterVertically
            ) {
                LinearProgressIndicator(
                    progress = { animatedProgress },
                    modifier = Modifier
                        .weight(1f)
                        .height(6.dp)
                )
                Spacer(Modifier.width(12.dp))
                TextButton(
                    onClick = onFinish,
                    contentPadding = androidx.compose.foundation.layout.PaddingValues(
                        horizontal = 8.dp, vertical = 0.dp
                    )
                ) {
                    Text("跳过")
                }
            }

            HorizontalPager(
                state = pagerState,
                modifier = Modifier.weight(1f),
                userScrollEnabled = false
            ) { page ->
                Column(
                    modifier = Modifier
                        .fillMaxSize()
                        .verticalScroll(rememberScrollState())
                        .padding(vertical = 24.dp),
                    horizontalAlignment = Alignment.CenterHorizontally
                ) {
                    when (page) {
                        0 -> WelcomePage()
                        1 -> ModePage(context)
                        2 -> BirthDatePage(context)
                        3 -> ThemePage(context)
                        4 -> NotificationPage(context)
                        else -> DonePage(context)
                    }
                }
            }

            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(vertical = 16.dp),
                horizontalArrangement = Arrangement.spacedBy(12.dp)
            ) {
                if (canGoBack) {
                    OutlinedButton(
                        onClick = {
                            scope.launch { pagerState.animateScrollToPage(pagerState.currentPage - 1) }
                        },
                        modifier = Modifier.weight(1f),
                        shape = MaterialTheme.shapes.large
                    ) {
                        Text("上一步")
                    }
                }
                Button(
                    onClick = {
                        if (isLastPage) {
                            onFinish()
                        } else {
                            scope.launch {
                                pagerState.animateScrollToPage(pagerState.currentPage + 1)
                            }
                        }
                    },
                    modifier = Modifier.weight(1f),
                    shape = MaterialTheme.shapes.large
                ) {
                    Text(if (isLastPage) "开始使用" else "下一步")
                }
            }
        }
    }
}

@Composable
private fun WelcomePage() {
    PageHeader(
        icon = Icons.Outlined.Favorite,
        title = "欢迎使用 NzHelper",
        subtitle = "一个简单、高效、易用的性生活记录工具。\n先用几步设置你的常用偏好，之后随时可以在设置中修改。"
    )
    Spacer(Modifier.height(24.dp))
    Text(
        "所有代码均在 GitHub 开源，欢迎审查与反馈",
        style = MaterialTheme.typography.bodyMedium,
        color = MaterialTheme.colorScheme.onSurfaceVariant
    )
    Text(
        "数据加密保护且仅保存在本地，不上传任何服务器",
        style = MaterialTheme.typography.bodyMedium,
        color = MaterialTheme.colorScheme.onSurfaceVariant
    )
}

// 记录模式
@Composable
private fun ModePage(context: Context) {
    var selected by remember { mutableStateOf(RecordModeSettings.getDefaultMode(context)) }
    PageHeader(
        icon = Icons.Outlined.ViewInAr,
        title = "常用记录模式",
        subtitle = "新建记录时默认选中的模式，可在记录表单里随时切换。"
    )
    Spacer(Modifier.height(20.dp))
    Column(verticalArrangement = Arrangement.spacedBy(12.dp)) {
        SessionMode.entries.forEach { mode ->
            OptionCard(
                icon = when (mode) {
                    SessionMode.SOLO_MALE -> Icons.Outlined.Male
                    SessionMode.SOLO_FEMALE -> Icons.Outlined.Female
                    SessionMode.PAIR -> Icons.Outlined.FavoriteBorder
                },
                title = mode.label,
                subtitle = when (mode) {
                    SessionMode.SOLO_MALE -> "个人记录"
                    SessionMode.SOLO_FEMALE -> "个人记录"
                    SessionMode.PAIR -> "双人记录，与伴侣"
                },
                selected = selected == mode,
                onClick = {
                    selected = mode
                    RecordModeSettings.setDefaultMode(context, mode)
                }
            )
        }
    }
}

// 出生日期
@OptIn(ExperimentalMaterial3Api::class)
@Composable
private fun BirthDatePage(context: Context) {
    val today = LocalDate.now()
    val stored = if (AgeGroupSettings.isBirthDateSet(context)) {
        AgeGroupSettings.getBirthDate(context)
    } else {
        today.minusYears(AgeGroupSettings.DEFAULT_AGE.toLong())
    }

    var selectedYear by remember { mutableIntStateOf(stored.year) }
    var selectedMonth by remember { mutableIntStateOf(stored.monthValue) }
    var selectedDay by remember { mutableIntStateOf(stored.dayOfMonth) }

    val daysInMonth = YearMonth.of(selectedYear, selectedMonth).lengthOfMonth()
    val effectiveDay = selectedDay.coerceAtMost(daysInMonth)
    val selectedDate = LocalDate.of(selectedYear, selectedMonth, effectiveDay)
    val age = ChronoUnit.YEARS.between(selectedDate, today).toInt()
        .coerceIn(AgeGroupSettings.MIN_AGE, AgeGroupSettings.MAX_AGE)

    LaunchedEffect(selectedDate) {
        AgeGroupSettings.setBirthDate(context, selectedDate)
    }

    val minYear = today.year - AgeGroupSettings.MAX_AGE
    val maxYear = today.year - AgeGroupSettings.MIN_AGE
    val yearOptions = (maxYear downTo minYear).map { "${it}年" }
    val monthOptions = (1..12).map { "${it}月" }
    val dayOptions = (1..daysInMonth).map { "${it}日" }

    PageHeader(
        icon = Icons.Outlined.Cake,
        title = "你的出生日期",
        subtitle = "用于年龄相关统计，仅保存在本地。"
    )
    Spacer(Modifier.height(24.dp))
    Row(
        modifier = Modifier.fillMaxWidth(),
        horizontalArrangement = Arrangement.spacedBy(8.dp)
    ) {
        DropdownSelector(
            label = "年",
            value = "${selectedYear}年",
            options = yearOptions,
            onSelect = { index -> selectedYear = maxYear - index },
            modifier = Modifier.weight(1.3f)
        )
        DropdownSelector(
            label = "月",
            value = "${selectedMonth}月",
            options = monthOptions,
            onSelect = { index -> selectedMonth = index + 1 },
            modifier = Modifier.weight(1f)
        )
        DropdownSelector(
            label = "日",
            value = "${effectiveDay}日",
            options = dayOptions,
            onSelect = { index -> selectedDay = index + 1 },
            modifier = Modifier.weight(1f)
        )
    }
    Spacer(Modifier.height(16.dp))
    Text(
        text = "${selectedDate.format(DateTimeFormatter.ofPattern("yyyy-MM-dd"))}（$age 岁）",
        style = MaterialTheme.typography.titleMedium,
        color = MaterialTheme.colorScheme.primary
    )
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
private fun DropdownSelector(
    label: String,
    value: String,
    options: List<String>,
    onSelect: (Int) -> Unit,
    modifier: Modifier = Modifier
) {
    var expanded by remember { mutableStateOf(false) }
    ExposedDropdownMenuBox(
        expanded = expanded,
        onExpandedChange = { expanded = it },
        modifier = modifier
    ) {
        OutlinedTextField(
            value = value,
            onValueChange = {},
            readOnly = true,
            singleLine = true,
            label = { Text(label) },
            trailingIcon = { ExposedDropdownMenuDefaults.TrailingIcon(expanded = expanded) },
            modifier = Modifier
                .fillMaxWidth()
                .menuAnchor(ExposedDropdownMenuAnchorType.PrimaryNotEditable)
        )
        ExposedDropdownMenu(
            expanded = expanded,
            onDismissRequest = { expanded = false }
        ) {
            options.forEachIndexed { index, option ->
                DropdownMenuItem(
                    text = { Text(option) },
                    onClick = {
                        onSelect(index)
                        expanded = false
                    }
                )
            }
        }
    }
}

// 主题模式
@OptIn(ExperimentalLayoutApi::class)
@Composable
private fun ThemePage(context: Context) {
    val themeState = LocalThemeState.current
    val darkSelected = themeState.themeMode == ThemeSettings.ThemeMode.DARK

    PageHeader(
        icon = Icons.Outlined.Palette,
        title = "喜欢的外观",
        subtitle = "选择你喜欢的主题色及主题模式，切换立即生效，之后可随时调整。"
    )
    Spacer(Modifier.height(20.dp))
    Column(verticalArrangement = Arrangement.spacedBy(12.dp)) {
        ThemeSettings.ThemeMode.entries.forEach { mode ->
            OptionCard(
                icon = when (mode) {
                    ThemeSettings.ThemeMode.SYSTEM -> Icons.Outlined.BrightnessAuto
                    ThemeSettings.ThemeMode.LIGHT -> Icons.Outlined.LightMode
                    ThemeSettings.ThemeMode.DARK -> Icons.Outlined.DarkMode
                },
                title = mode.label,
                subtitle = when (mode) {
                    ThemeSettings.ThemeMode.SYSTEM -> "随系统自动切换明暗"
                    ThemeSettings.ThemeMode.LIGHT -> "始终使用浅色外观"
                    ThemeSettings.ThemeMode.DARK -> "始终使用深色外观"
                },
                selected = themeState.themeMode == mode,
                onClick = {
                    themeState.themeMode = mode
                    ThemeSettings.setThemeMode(context, mode)
                }
            )
        }
    }
    Spacer(Modifier.height(16.dp))
    Card(
        modifier = Modifier.fillMaxWidth(),
        shape = MaterialTheme.shapes.large,
        colors = CardDefaults.cardColors(
            containerColor = MaterialTheme.colorScheme.surfaceBright
        )
    ) {
        Column(modifier = Modifier.padding(vertical = 4.dp)) {
            ToggleRow(
                title = "AMOLED 纯黑",
                subtitle = if (darkSelected) "深色模式下使用纯黑背景，更省电" else "仅深色模式可用",
                checked = themeState.amoledDark,
                enabled = darkSelected,
                onCheckedChange = { enabled ->
                    themeState.amoledDark = enabled
                    ThemeSettings.setAmoledDark(context, enabled)
                }
            )
            ToggleRow(
                title = "动态取色",
                subtitle = "跟随壁纸取色（Android 12+）",
                checked = themeState.dynamicColor,
                onCheckedChange = { enabled ->
                    themeState.dynamicColor = enabled
                    ThemeSettings.setDynamicColor(context, enabled)
                }
            )
            AnimatedVisibility(visible = !themeState.dynamicColor) {
                Column(
                    modifier = Modifier
                        .padding(horizontal = 16.dp)
                        .padding(bottom = 12.dp)
                ) {
                    Text(
                        text = "主题色",
                        style = MaterialTheme.typography.labelMedium,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                    Spacer(Modifier.height(8.dp))
                    FlowRow(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.spacedBy(10.dp),
                        verticalArrangement = Arrangement.spacedBy(10.dp)
                    ) {
                        ThemeColorOptions.forEachIndexed { index, option ->
                            ColorDot(
                                color = option.seed,
                                selected = index == themeState.themeColorIndex,
                                onClick = {
                                    themeState.themeColorIndex = index
                                    ThemeSettings.setThemeColorIndex(context, index)
                                }
                            )
                        }
                    }
                }
            }
        }
    }
}

@Composable
private fun ColorDot(
    color: Color,
    selected: Boolean,
    onClick: () -> Unit
) {
    Box(
        modifier = Modifier
            .size(40.dp)
            .clip(CircleShape)
            .clickable(onClick = onClick)
            .border(
                width = if (selected) 3.dp else 1.dp,
                color = if (selected) MaterialTheme.colorScheme.primary
                else MaterialTheme.colorScheme.outlineVariant,
                shape = CircleShape
            )
            .padding(6.dp),
        contentAlignment = Alignment.Center
    ) {
        Box(
            modifier = Modifier
                .fillMaxSize()
                .clip(CircleShape)
                .background(color)
        )
    }
}

@Composable
private fun ToggleRow(
    title: String,
    subtitle: String,
    checked: Boolean,
    onCheckedChange: (Boolean) -> Unit,
    enabled: Boolean = true
) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .alpha(if (enabled) 1f else 0.38f)
            .clickable(enabled = enabled) { onCheckedChange(!checked) }
            .padding(horizontal = 16.dp, vertical = 12.dp),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.SpaceBetween
    ) {
        Column(modifier = Modifier.weight(1f)) {
            Text(
                text = title,
                style = MaterialTheme.typography.titleMedium,
                color = MaterialTheme.colorScheme.onSurface
            )
            Spacer(Modifier.height(2.dp))
            Text(
                text = subtitle,
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )
        }
        Spacer(Modifier.width(12.dp))
        Switch(
            checked = checked,
            onCheckedChange = onCheckedChange,
            enabled = enabled
        )
    }
}

// 通知权限
@Composable
private fun NotificationPage(context: Context) {
    var granted by remember {
        mutableStateOf(NotificationManagerCompat.from(context).areNotificationsEnabled())
    }
    val permissionLauncher = rememberLauncherForActivityResult(
        ActivityResultContracts.RequestPermission()
    ) {
        granted = NotificationManagerCompat.from(context).areNotificationsEnabled()
    }

    fun openSettings() {
        val intent = Intent().apply {
            action = Settings.ACTION_APP_NOTIFICATION_SETTINGS
            putExtra(Settings.EXTRA_APP_PACKAGE, context.packageName)
            putExtra("app_uid", context.applicationInfo.uid)
            addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
        }
        context.startActivity(intent)
    }

    PageHeader(
        icon = Icons.Outlined.Notifications,
        title = "开启通知",
        subtitle = "计时期间需要在通知栏显示状态，后台计时才能稳定运行。\n建议开启，之后也可在系统设置中关闭。"
    )
    Spacer(Modifier.height(20.dp))
    Card(
        modifier = Modifier.fillMaxWidth(),
        shape = MaterialTheme.shapes.large,
        colors = CardDefaults.cardColors(
            containerColor = if (granted) {
                MaterialTheme.colorScheme.primaryContainer
            } else {
                MaterialTheme.colorScheme.surfaceBright
            }
        )
    ) {
        Row(
            modifier = Modifier.padding(16.dp),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(12.dp)
        ) {
            Icon(
                imageVector = if (granted) Icons.Outlined.CheckCircle else Icons.Outlined.Notifications,
                contentDescription = null,
                tint = if (granted) MaterialTheme.colorScheme.primary
                else MaterialTheme.colorScheme.onSurfaceVariant
            )
            Text(
                text = if (granted) "通知权限已开启" else "通知权限未开启",
                style = MaterialTheme.typography.titleMedium,
                color = MaterialTheme.colorScheme.onSurface
            )
        }
    }
    Spacer(Modifier.height(16.dp))
    if (!granted) {
        Button(
            onClick = {
                if (Build.VERSION.SDK_INT >= 33) {
                    permissionLauncher.launch(Manifest.permission.POST_NOTIFICATIONS)
                } else {
                    openSettings()
                }
            },
            modifier = Modifier.fillMaxWidth(),
            shape = MaterialTheme.shapes.large
        ) {
            Text("开启通知")
        }
    }
    Spacer(Modifier.height(8.dp))
    Text(
        text = "也可以先跳过，之后随时在系统设置中开启。",
        style = MaterialTheme.typography.bodySmall,
        color = MaterialTheme.colorScheme.onSurfaceVariant,
        textAlign = TextAlign.Center
    )
}

// 完成
@Composable
private fun DonePage(context: Context) {
    PageHeader(
        icon = Icons.Outlined.Celebration,
        title = "设置完成",
        subtitle = "一切就绪，开始记录吧！以下偏好都可以在设置中修改。"
    )
    Spacer(Modifier.height(20.dp))
    Card(
        modifier = Modifier.fillMaxWidth(),
        shape = MaterialTheme.shapes.large,
        colors = CardDefaults.cardColors(
            containerColor = MaterialTheme.colorScheme.surfaceBright
        )
    ) {
        Column(
            modifier = Modifier.padding(16.dp),
            verticalArrangement = Arrangement.spacedBy(12.dp)
        ) {
            SummaryRow("记录模式", RecordModeSettings.getDefaultMode(context).label)
            SummaryRow(
                "年龄",
                if (AgeGroupSettings.isBirthDateSet(context)) {
                    "${AgeGroupSettings.getAge(context)} 岁"
                } else "未设置"
            )
            SummaryRow("主题", ThemeSettings.getThemeMode(context).label)
            SummaryRow(
                "通知",
                if (NotificationManagerCompat.from(context)
                        .areNotificationsEnabled()
                ) "已开启" else "未开启"
            )
        }
    }
}
