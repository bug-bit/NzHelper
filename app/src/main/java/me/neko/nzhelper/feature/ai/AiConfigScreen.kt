package me.neko.nzhelper.feature.ai

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.outlined.ArrowBack
import androidx.compose.material.icons.outlined.AutoAwesome
import androidx.compose.material.icons.outlined.DataArray
import androidx.compose.material.icons.outlined.Dns
import androidx.compose.material.icons.outlined.History
import androidx.compose.material.icons.outlined.Refresh
import androidx.compose.material.icons.outlined.Token
import androidx.compose.material.icons.outlined.Tune
import androidx.compose.material.icons.rounded.SmartToy
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Button
import androidx.compose.material3.Checkbox
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.LargeFlexibleTopAppBar
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.RadioButton
import androidx.compose.material3.RadioButtonDefaults
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Slider
import androidx.compose.material3.Switch
import androidx.compose.material3.Text
import androidx.compose.material3.TopAppBarDefaults
import androidx.compose.material3.rememberTopAppBarState
import androidx.compose.runtime.Composable
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import kotlinx.coroutines.launch
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.input.nestedscroll.nestedScroll
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import me.neko.nzhelper.core.ai.AiProvider
import me.neko.nzhelper.core.ai.AiSettings
import me.neko.nzhelper.ui.component.setting.SettingsCard
import me.neko.nzhelper.ui.component.setting.SettingsDivider
import me.neko.nzhelper.ui.component.setting.SettingsItem

private val TONES = listOf(
    "warm" to "温暖",
    "caring" to "贴心",
    "encouraging" to "鼓励",
    "professional" to "专业",
    "humorous" to "幽默",
    "concise" to "简洁"
)
private val LENGTHS =
    listOf("short" to "简短", "medium" to "适中", "detailed" to "详细", "unlimited" to "无限制")
private val REFRESH_INTERVALS = listOf(
    0 to "仅手动",
    30 to "30 分钟",
    60 to "1 小时",
    240 to "4 小时",
    720 to "12 小时"
)
private val ANALYSIS_RANGES = listOf(
    7 to "最近 7 天",
    30 to "最近 30 天",
    60 to "最近 60 天",
    365 to "最近 1 年",
    0 to "全部记录"
)

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun AiConfigScreen(
    onBack: () -> Unit,
    onProviders: () -> Unit
) {
    val context = LocalContext.current
    val scope = rememberCoroutineScope()
    val scrollBehavior =
        TopAppBarDefaults.exitUntilCollapsedScrollBehavior(rememberTopAppBarState())

    var enabled by remember { mutableStateOf(false) }
    var tone by remember { mutableStateOf("warm") }
    var length by remember { mutableStateOf("medium") }
    var custom by remember { mutableStateOf("") }
    var maxTokens by remember { mutableIntStateOf(500) }
    var refreshInterval by remember { mutableIntStateOf(0) }
    var dataOpts by remember { mutableStateOf(AiSettings.DataOptions()) }
    var analysisDays by remember { mutableIntStateOf(7) }
    var providers by remember { mutableStateOf<List<AiProvider>>(emptyList()) }
    var showDataDialog by remember { mutableStateOf(false) }
    var showAnalysisDaysDialog by remember { mutableStateOf(false) }
    var showToneDialog by remember { mutableStateOf(false) }
    var showLengthDialog by remember { mutableStateOf(false) }
    var showCustomDialog by remember { mutableStateOf(false) }
    var showMaxTokensDialog by remember { mutableStateOf(false) }
    var showRefreshDialog by remember { mutableStateOf(false) }

    LaunchedEffect(Unit) {
        enabled = AiSettings.isEnabled(context)
        tone = AiSettings.getPromptTone(context)
        length = AiSettings.getPromptLength(context)
        custom = AiSettings.getPromptCustom(context)
        maxTokens = AiSettings.getMaxTokens(context)
        refreshInterval = AiSettings.getRefreshIntervalMin(context)
        dataOpts = AiSettings.getDataOptions(context)
        analysisDays = AiSettings.getAnalysisDays(context)
        providers = AiSettings.getProviders(context)
    }

    val toggleEnabled: (Boolean) -> Unit = { e ->
        enabled = e
        scope.launch { AiSettings.setEnabled(context, e) }
    }
    val active = providers.firstOrNull { it.isActive }
    val hasProvider = providers.isNotEmpty()

    Scaffold(
        containerColor = MaterialTheme.colorScheme.surfaceContainer,
        topBar = {
            LargeFlexibleTopAppBar(
                title = { Text("AI 健康建议") },
                navigationIcon = {
                    IconButton(onClick = onBack) {
                        Icon(Icons.AutoMirrored.Outlined.ArrowBack, "返回")
                    }
                },
                scrollBehavior = scrollBehavior,
                colors = TopAppBarDefaults.topAppBarColors(
                    containerColor = MaterialTheme.colorScheme.surfaceContainer,
                    scrolledContainerColor = MaterialTheme.colorScheme.surfaceContainer
                )
            )
        }
    ) { innerPadding ->
        LazyColumn(
            modifier = Modifier
                .fillMaxSize()
                .nestedScroll(scrollBehavior.nestedScrollConnection),
            contentPadding = PaddingValues(
                start = 16.dp, end = 16.dp,
                top = innerPadding.calculateTopPadding() + 16.dp,
                bottom = innerPadding.calculateBottomPadding() + 16.dp
            ),
            verticalArrangement = Arrangement.spacedBy(16.dp)
        ) {
            item {
                SettingsCard {
                    SettingsItem(
                        icon = Icons.Rounded.SmartToy,
                        title = "启用 AI 分析",
                        subtitle = if (hasProvider) "AI 根据记录生成个性化健康建议"
                        else "请先添加供应商",
                        enabled = hasProvider,
                        onClick = { if (hasProvider) toggleEnabled(!enabled) },
                        trailingContent = {
                            Switch(
                                checked = enabled && hasProvider,
                                onCheckedChange = { if (hasProvider) toggleEnabled(it) },
                                enabled = hasProvider
                            )
                        }
                    )
                    SettingsDivider()
                    SettingsItem(
                        icon = Icons.Outlined.Dns,
                        title = "管理供应商",
                        subtitle = if (active != null) "${active.model} · ${providers.size} 个供应商"
                        else if (providers.isNotEmpty()) "${providers.size} 个供应商 · 未激活"
                        else "尚未添加",
                        onClick = onProviders
                    )
                }
            }

            item {
                SettingsCard {
                    SettingsItem(
                        icon = Icons.Outlined.Refresh,
                        title = "自动刷新 AI 建议",
                        subtitle = "按间隔或新增记录后自动请求 AI\n${
                            REFRESH_INTERVALS.firstOrNull { it.first == refreshInterval }?.second
                                ?: "仅手动"
                        }",
                        enabled = hasProvider,
                        onClick = { showRefreshDialog = true }
                    )
                    SettingsDivider()
                    SettingsItem(
                        icon = Icons.Outlined.Token,
                        title = "Max Tokens",
                        subtitle = "$maxTokens（推理模型建议 1000+）",
                        enabled = hasProvider,
                        onClick = { showMaxTokensDialog = true }
                    )
                }
            }

            item {
                SettingsCard {
                    SettingsItem(
                        icon = Icons.Outlined.DataArray,
                        title = "分析数据",
                        subtitle = "选择发送给 AI 分析的数据\n已选 ${dataOpts.fields.size} 项 · ${
                            ANALYSIS_RANGES.firstOrNull { it.first == analysisDays }?.second ?: "最近7天"
                        }",
                        enabled = hasProvider,
                        onClick = { showDataDialog = true }
                    )
                    SettingsDivider()
                    SettingsItem(
                        icon = Icons.Outlined.History,
                        title = "时间范围",
                        subtitle = ANALYSIS_RANGES.firstOrNull { it.first == analysisDays }?.second
                            ?: "最近 7 天",
                        enabled = hasProvider,
                        onClick = { showAnalysisDaysDialog = true }
                    )
                }
            }

            item {
                SettingsCard {
                    SettingsItem(
                        icon = Icons.Outlined.Tune,
                        title = "回答口吻",
                        subtitle = TONES.firstOrNull { it.first == tone }?.second ?: tone,
                        enabled = hasProvider,
                        onClick = { showToneDialog = true }
                    )
                    SettingsDivider()
                    SettingsItem(
                        icon = Icons.Outlined.Tune,
                        title = "回答长度",
                        subtitle = LENGTHS.firstOrNull { it.first == length }?.second ?: length,
                        enabled = hasProvider,
                        onClick = { showLengthDialog = true }
                    )
                    SettingsDivider()
                    SettingsItem(
                        icon = Icons.Outlined.AutoAwesome,
                        title = "自定义要求",
                        subtitle = custom.ifBlank { "未设置" },
                        enabled = hasProvider,
                        onClick = { showCustomDialog = true }
                    )
                }
            }
        }
    }

    // 口吻选择
    if (showToneDialog) {
        AlertDialog(
            onDismissRequest = { showToneDialog = false },
            containerColor = MaterialTheme.colorScheme.surfaceContainerHigh,
            title = {
                Text("回答口吻", textAlign = TextAlign.Center, modifier = Modifier.fillMaxWidth())
            },
            text = {
                Column(verticalArrangement = Arrangement.spacedBy(4.dp)) {
                    TONES.forEach { (v, label) ->
                        val selected = tone == v
                        Row(
                            modifier = Modifier
                                .fillMaxWidth()
                                .clip(RoundedCornerShape(12.dp))
                                .background(
                                    if (selected) MaterialTheme.colorScheme.primaryContainer.copy(
                                        alpha = 0.4f
                                    )
                                    else Color.Transparent
                                )
                                .clickable { tone = v }
                                .padding(horizontal = 12.dp, vertical = 10.dp),
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            RadioButton(
                                selected = selected,
                                onClick = null,
                                colors = RadioButtonDefaults.colors(
                                    selectedColor = MaterialTheme.colorScheme.primary,
                                    unselectedColor = MaterialTheme.colorScheme.onSurfaceVariant.copy(
                                        alpha = 0.5f
                                    )
                                )
                            )
                            Spacer(Modifier.width(8.dp))
                            Text(
                                label,
                                style = MaterialTheme.typography.bodyLarge,
                                color = if (selected) MaterialTheme.colorScheme.onPrimaryContainer
                                else MaterialTheme.colorScheme.onSurface
                            )
                        }
                    }
                }
            },
            confirmButton = {
                Button(
                    onClick = { showToneDialog = false },
                    modifier = Modifier.fillMaxWidth(),
                    shape = MaterialTheme.shapes.large
                ) { Text("确定") }
            }
        )
    }

    // 长度选择
    if (showLengthDialog) {
        AlertDialog(
            onDismissRequest = { showLengthDialog = false },
            containerColor = MaterialTheme.colorScheme.surfaceContainerHigh,
            title = {
                Text("回答长度", textAlign = TextAlign.Center, modifier = Modifier.fillMaxWidth())
            },
            text = {
                Column(verticalArrangement = Arrangement.spacedBy(4.dp)) {
                    LENGTHS.forEach { (v, label) ->
                        val selected = length == v
                        Row(
                            modifier = Modifier
                                .fillMaxWidth()
                                .clip(RoundedCornerShape(12.dp))
                                .background(
                                    if (selected) MaterialTheme.colorScheme.primaryContainer.copy(
                                        alpha = 0.4f
                                    )
                                    else Color.Transparent
                                )
                                .clickable { length = v }
                                .padding(horizontal = 12.dp, vertical = 10.dp),
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            RadioButton(
                                selected = selected,
                                onClick = null,
                                colors = RadioButtonDefaults.colors(
                                    selectedColor = MaterialTheme.colorScheme.primary,
                                    unselectedColor = MaterialTheme.colorScheme.onSurfaceVariant.copy(
                                        alpha = 0.5f
                                    )
                                )
                            )
                            Spacer(Modifier.width(8.dp))
                            Text(
                                label,
                                style = MaterialTheme.typography.bodyLarge,
                                color = if (selected) MaterialTheme.colorScheme.onPrimaryContainer
                                else MaterialTheme.colorScheme.onSurface
                            )
                        }
                    }
                }
            },
            confirmButton = {
                Button(
                    onClick = { showLengthDialog = false },
                    modifier = Modifier.fillMaxWidth(),
                    shape = MaterialTheme.shapes.large
                ) { Text("确定") }
            }
        )
    }

    // 自定义要求
    if (showCustomDialog) {
        var temp by remember(custom) { mutableStateOf(custom) }
        val presets = listOf(
            "毒舌" to "用毒舌傲娇的语气，可以适当毒舌吐槽，但要有分寸",
            "御姐" to "用成熟知性的御姐口吻，温柔中带着强势",
            "元气" to "用元气满满的语气，充满活力和正能量",
            "文艺" to "用文艺清新的风格，像写散文一样优美",
            "损友" to "用损友互怼的语气，像兄弟一样直白不绕弯",
            "骑士" to "用忠诚守护的骑士口吻，充满责任感和使命感"
        )
        AlertDialog(
            onDismissRequest = { showCustomDialog = false },
            containerColor = MaterialTheme.colorScheme.surfaceContainerHigh,
            title = {
                Text("自定义要求", textAlign = TextAlign.Center, modifier = Modifier.fillMaxWidth())
            },
            text = {
                Column {
                    Text(
                        "人格预设",
                        style = MaterialTheme.typography.labelLarge,
                        color = MaterialTheme.colorScheme.primary
                    )
                    Spacer(Modifier.height(6.dp))
                    Column(verticalArrangement = Arrangement.spacedBy(6.dp)) {
                        presets.chunked(2).forEach { row ->
                            Row(
                                modifier = Modifier.fillMaxWidth(),
                                horizontalArrangement = Arrangement.spacedBy(8.dp)
                            ) {
                                row.forEach { (label, text) ->
                                    OutlinedButton(
                                        onClick = { temp = text },
                                        modifier = Modifier.weight(1f),
                                        shape = MaterialTheme.shapes.small,
                                        contentPadding = PaddingValues(
                                            horizontal = 8.dp,
                                            vertical = 4.dp
                                        )
                                    ) {
                                        Text(
                                            label,
                                            style = MaterialTheme.typography.labelMedium,
                                            maxLines = 1
                                        )
                                    }
                                }
                                if (row.size == 1) Spacer(Modifier.weight(1f))
                            }
                        }
                    }
                    Spacer(Modifier.height(12.dp))
                    OutlinedTextField(
                        value = temp,
                        onValueChange = { temp = it },
                        placeholder = { Text("如：多鼓励、少批评、关注作息...") },
                        singleLine = false,
                        modifier = Modifier.fillMaxWidth()
                    )
                }
            },
            confirmButton = {
                Button(
                    onClick = {
                        custom = temp
                        showCustomDialog = false
                    },
                    modifier = Modifier.fillMaxWidth(),
                    shape = MaterialTheme.shapes.large
                ) { Text("确定") }
            }
        )
    }

    // Max Tokens 滑块
    if (showMaxTokensDialog) {
        AlertDialog(
            onDismissRequest = { showMaxTokensDialog = false },
            containerColor = MaterialTheme.colorScheme.surfaceContainerHigh,
            title = {
                Text("Max Tokens", textAlign = TextAlign.Center, modifier = Modifier.fillMaxWidth())
            },
            text = {
                Column(horizontalAlignment = Alignment.CenterHorizontally) {
                    Text(
                        "$maxTokens",
                        style = MaterialTheme.typography.headlineMedium,
                        color = MaterialTheme.colorScheme.primary
                    )
                    Spacer(Modifier.padding(8.dp))
                    Slider(
                        value = maxTokens.toFloat(),
                        onValueChange = { maxTokens = it.toInt() },
                        valueRange = 40f..4000f,
                        steps = 98,
                        modifier = Modifier.fillMaxWidth()
                    )
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween
                    ) {
                        Text(
                            "40", style = MaterialTheme.typography.labelSmall,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                        Text(
                            "4000", style = MaterialTheme.typography.labelSmall,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                    }
                }
            },
            confirmButton = {
                Button(
                    onClick = { showMaxTokensDialog = false },
                    modifier = Modifier.fillMaxWidth(),
                    shape = MaterialTheme.shapes.large
                ) { Text("确定") }
            }
        )
    }

    // 刷新间隔选择
    if (showRefreshDialog) {
        AlertDialog(
            onDismissRequest = { showRefreshDialog = false },
            containerColor = MaterialTheme.colorScheme.surfaceContainerHigh,
            title = {
                Text("自动刷新", textAlign = TextAlign.Center, modifier = Modifier.fillMaxWidth())
            },
            text = {
                Column(verticalArrangement = Arrangement.spacedBy(4.dp)) {
                    REFRESH_INTERVALS.forEach { (v, label) ->
                        val selected = refreshInterval == v
                        Row(
                            modifier = Modifier
                                .fillMaxWidth()
                                .clip(RoundedCornerShape(12.dp))
                                .background(
                                    if (selected) MaterialTheme.colorScheme.primaryContainer.copy(
                                        alpha = 0.4f
                                    )
                                    else Color.Transparent
                                )
                                .clickable { refreshInterval = v }
                                .padding(horizontal = 12.dp, vertical = 10.dp),
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            RadioButton(
                                selected = selected,
                                onClick = null,
                                colors = RadioButtonDefaults.colors(
                                    selectedColor = MaterialTheme.colorScheme.primary,
                                    unselectedColor = MaterialTheme.colorScheme.onSurfaceVariant.copy(
                                        alpha = 0.5f
                                    )
                                )
                            )
                            Spacer(Modifier.width(8.dp))
                            Text(
                                label, style = MaterialTheme.typography.bodyLarge,
                                color = if (selected) MaterialTheme.colorScheme.onPrimaryContainer
                                else MaterialTheme.colorScheme.onSurface
                            )
                        }
                    }
                }
            },
            confirmButton = {
                Button(
                    onClick = { showRefreshDialog = false },
                    modifier = Modifier.fillMaxWidth(),
                    shape = MaterialTheme.shapes.large
                ) { Text("确定") }
            }
        )
    }

    // 数据字段选择
    if (showDataDialog) {
        val groups = AiSettings.DataField.GROUPS
        AlertDialog(
            onDismissRequest = { showDataDialog = false },
            containerColor = MaterialTheme.colorScheme.surfaceContainerHigh,
            title = {
                Text("发送数据", textAlign = TextAlign.Center, modifier = Modifier.fillMaxWidth())
            },
            text = {
                Column(
                    modifier = Modifier.verticalScroll(rememberScrollState()),
                    verticalArrangement = Arrangement.spacedBy(12.dp)
                ) {
                    groups.forEach { (category: String, fields: List<AiSettings.DataField>) ->
                        Column(
                            modifier = Modifier
                                .fillMaxWidth()
                                .clip(RoundedCornerShape(12.dp))
                                .background(
                                    MaterialTheme.colorScheme.surfaceContainerLowest.copy(
                                        alpha = 0.3f
                                    )
                                )
                                .padding(12.dp)
                        ) {
                            Text(
                                category,
                                style = MaterialTheme.typography.labelLarge,
                                color = MaterialTheme.colorScheme.primary
                            )
                            fields.forEach { field ->
                                Row(
                                    modifier = Modifier
                                        .fillMaxWidth()
                                        .clip(RoundedCornerShape(8.dp))
                                        .clickable {
                                            val newFields = dataOpts.fields.toMutableSet()
                                            if (field.key in newFields) newFields -= field.key
                                            else newFields += field.key
                                            dataOpts = AiSettings.DataOptions(newFields)
                                        }
                                        .padding(start = 8.dp, top = 4.dp, bottom = 4.dp),
                                    verticalAlignment = Alignment.CenterVertically,
                                    horizontalArrangement = Arrangement.SpaceBetween
                                ) {
                                    Text(field.label, style = MaterialTheme.typography.bodyLarge)
                                    Checkbox(
                                        checked = field.key in dataOpts.fields,
                                        onCheckedChange = {
                                            val newFields = dataOpts.fields.toMutableSet()
                                            if (it) newFields += field.key else newFields -= field.key
                                            dataOpts = AiSettings.DataOptions(newFields)
                                        }
                                    )
                                }
                            }
                        }
                    }
                }
            },
            confirmButton = {
                Button(
                    onClick = { showDataDialog = false },
                    modifier = Modifier.fillMaxWidth(),
                    shape = MaterialTheme.shapes.large
                ) { Text("确定") }
            }
        )
    }

    // 时间范围选择
    if (showAnalysisDaysDialog) {
        AlertDialog(
            onDismissRequest = { showAnalysisDaysDialog = false },
            containerColor = MaterialTheme.colorScheme.surfaceContainerHigh,
            title = {
                Text("时间范围", textAlign = TextAlign.Center, modifier = Modifier.fillMaxWidth())
            },
            text = {
                Column(verticalArrangement = Arrangement.spacedBy(4.dp)) {
                    ANALYSIS_RANGES.forEach { (v, label) ->
                        val selected = analysisDays == v
                        Row(
                            modifier = Modifier
                                .fillMaxWidth()
                                .clip(RoundedCornerShape(12.dp))
                                .background(
                                    if (selected) MaterialTheme.colorScheme.primaryContainer.copy(
                                        alpha = 0.4f
                                    )
                                    else Color.Transparent
                                )
                                .clickable { analysisDays = v }
                                .padding(horizontal = 12.dp, vertical = 10.dp),
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            RadioButton(
                                selected = selected, onClick = null,
                                colors = RadioButtonDefaults.colors(
                                    selectedColor = MaterialTheme.colorScheme.primary,
                                    unselectedColor = MaterialTheme.colorScheme.onSurfaceVariant.copy(
                                        alpha = 0.5f
                                    )
                                )
                            )
                            Spacer(Modifier.width(8.dp))
                            Text(
                                label, style = MaterialTheme.typography.bodyLarge,
                                color = if (selected) MaterialTheme.colorScheme.onPrimaryContainer
                                else MaterialTheme.colorScheme.onSurface
                            )
                        }
                    }
                }
            },
            confirmButton = {
                Button(
                    onClick = { showAnalysisDaysDialog = false },
                    modifier = Modifier.fillMaxWidth(),
                    shape = MaterialTheme.shapes.large
                ) { Text("确定") }
            }
        )
    }

    DisposableEffect(Unit) {
        onDispose {
            scope.launch {
                AiSettings.savePrompt(context, tone, length, custom)
                AiSettings.setMaxTokens(context, maxTokens)
                AiSettings.setRefreshIntervalMin(context, refreshInterval)
                AiSettings.setDataOptions(context, dataOpts)
                AiSettings.setAnalysisDays(context, analysisDays)
            }
        }
    }
}
