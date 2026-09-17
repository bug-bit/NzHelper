package me.neko.nzhelper.feature.achievement

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.WindowInsets
import androidx.compose.foundation.layout.WindowInsetsSides
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.only
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.safeDrawing
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.ExperimentalMaterial3ExpressiveApi
import androidx.compose.material3.FilterChip
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.LargeFlexibleTopAppBar
import androidx.compose.material3.LinearProgressIndicator
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.material3.TopAppBarDefaults
import androidx.compose.material3.rememberTopAppBarState
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.input.nestedscroll.nestedScroll
import androidx.compose.ui.draw.clip
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import me.neko.nzhelper.core.achievement.AchievementRepository
import me.neko.nzhelper.core.model.AchievementCategory
import me.neko.nzhelper.core.model.AchievementProgress
import me.neko.nzhelper.feature.achievement.components.AchievementCard
import me.neko.nzhelper.feature.achievement.components.AchievementDetailSheet
import me.neko.nzhelper.feature.achievement.components.icon
import kotlin.math.roundToInt

@OptIn(
    ExperimentalMaterial3Api::class,
    ExperimentalMaterial3ExpressiveApi::class
)
@Composable
fun AchievementScreen(onBack: () -> Unit) {
    val context = LocalContext.current
    val scrollBehavior =
        TopAppBarDefaults.exitUntilCollapsedScrollBehavior(rememberTopAppBarState())

    var progressList by remember { mutableStateOf<List<AchievementProgress>>(emptyList()) }
    var isLoading by remember { mutableStateOf(true) }
    var selectedCategory by remember { mutableStateOf<AchievementCategory?>(null) }
    var selectedProgress by remember { mutableStateOf<AchievementProgress?>(null) }

    LaunchedEffect(Unit) {
        progressList = AchievementRepository.sync(context)
        AchievementRepository.markAllSeen(context)
        isLoading = false
    }

    val revealedProgress = remember(progressList) {
        progressList.filter { it.isRevealed }
    }
    val unlockedCount = revealedProgress.count { it.isUnlocked }
    val totalCount = revealedProgress.size
    val overallFraction =
        if (totalCount == 0) 0f else unlockedCount.toFloat() / totalCount

    val visibleProgress = remember(revealedProgress, selectedCategory) {
        revealedProgress
            .filter { selectedCategory == null || it.achievement.category == selectedCategory }
            .sortedWith(
                compareByDescending<AchievementProgress> { it.isUnlocked }
                    .thenByDescending { it.fraction }
            )
    }

    Scaffold(
        containerColor = MaterialTheme.colorScheme.surfaceContainer,
        topBar = {
            LargeFlexibleTopAppBar(
                title = { Text("成就") },
                navigationIcon = {
                    IconButton(onClick = onBack) {
                        Icon(
                            imageVector = Icons.AutoMirrored.Filled.ArrowBack,
                            contentDescription = "返回"
                        )
                    }
                },
                scrollBehavior = scrollBehavior,
                colors = TopAppBarDefaults.topAppBarColors(
                    containerColor = MaterialTheme.colorScheme.surfaceContainer,
                    scrolledContainerColor = MaterialTheme.colorScheme.surfaceContainer
                )
            )
        },
        contentWindowInsets = WindowInsets.safeDrawing.only(
            WindowInsetsSides.Top + WindowInsetsSides.Horizontal
        )
    ) { innerPadding ->
        Box(
            modifier = Modifier
                .fillMaxSize()
                .padding(innerPadding)
                .nestedScroll(scrollBehavior.nestedScrollConnection)
        ) {
            if (isLoading) {
                Box(
                    modifier = Modifier.fillMaxSize(),
                    contentAlignment = Alignment.Center
                ) {
                    CircularProgressIndicator()
                }
            } else {
                LazyColumn(
                    modifier = Modifier.fillMaxSize(),
                    contentPadding = PaddingValues(
                        start = 16.dp,
                        end = 16.dp,
                        top = 8.dp,
                        bottom = 32.dp
                    ),
                    verticalArrangement = Arrangement.spacedBy(12.dp)
                ) {
                    item(key = "summary") {
                        AchievementSummaryCard(
                            unlockedCount = unlockedCount,
                            totalCount = totalCount,
                            fraction = overallFraction,
                            // 用 revealedProgress，避免提前暴露隐藏成就
                            nextUp = revealedProgress
                                .filterNot { it.isUnlocked }
                                .maxByOrNull { it.fraction }
                        )
                    }

                    item(key = "filters") {
                        CategoryFilterRow(
                            selected = selectedCategory,
                            onSelect = { selectedCategory = it }
                        )
                    }

                    if (selectedCategory == null) {
                        AchievementCategory.entries.forEach { category ->
                            val categoryItems =
                                visibleProgress.filter { it.achievement.category == category }
                            if (categoryItems.isEmpty()) return@forEach
                            item(key = "header_${category.name}") {
                                CategoryHeader(
                                    category = category,
                                    unlocked = categoryItems.count { it.isUnlocked },
                                    total = categoryItems.size
                                )
                            }
                            items(categoryItems, key = { it.achievement.key }) { entry ->
                                AchievementCard(
                                    progress = entry,
                                    onClick = { selectedProgress = entry }
                                )
                            }
                        }
                    } else {
                        items(visibleProgress, key = { it.achievement.key }) { entry ->
                            AchievementCard(
                                progress = entry,
                                onClick = { selectedProgress = entry }
                            )
                        }
                    }
                }
            }
        }
    }

    selectedProgress?.let { progress ->
        AchievementDetailSheet(
            progress = progress,
            onDismiss = { selectedProgress = null }
        )
    }
}

@Composable
private fun AchievementSummaryCard(
    unlockedCount: Int,
    totalCount: Int,
    fraction: Float,
    nextUp: AchievementProgress?
) {
    Card(
        modifier = Modifier.fillMaxWidth(),
        shape = MaterialTheme.shapes.large,
        colors = CardDefaults.cardColors(
            containerColor = MaterialTheme.colorScheme.primaryContainer.copy(alpha = 0.4f)
        )
    ) {
        Column(modifier = Modifier.padding(16.dp)) {
            Row(verticalAlignment = Alignment.Bottom) {
                Text(
                    text = unlockedCount.toString(),
                    style = MaterialTheme.typography.headlineMedium,
                    color = MaterialTheme.colorScheme.primary,
                    fontWeight = FontWeight.SemiBold
                )
                Spacer(Modifier.width(6.dp))
                Text(
                    text = "/ $totalCount 已解锁",
                    style = MaterialTheme.typography.bodyMedium,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                    modifier = Modifier.padding(bottom = 4.dp)
                )
                Spacer(Modifier.weight(1f))
                Text(
                    text = "${(fraction * 100).roundToInt()}%",
                    style = MaterialTheme.typography.titleMedium,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                    modifier = Modifier.padding(bottom = 2.dp)
                )
            }

            Spacer(Modifier.height(10.dp))

            LinearProgressIndicator(
                progress = { fraction },
                modifier = Modifier
                    .fillMaxWidth()
                    .height(8.dp),
                trackColor = MaterialTheme.colorScheme.surfaceContainerHighest
            )

            Spacer(Modifier.height(10.dp))

            Text(
                text = nextUp?.let {
                    "最接近解锁：${it.achievement.title}（${it.progressText}）"
                } ?: "全部成就已解锁，了不起！",
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )
        }
    }
}

@Composable
private fun CategoryFilterRow(
    selected: AchievementCategory?,
    onSelect: (AchievementCategory?) -> Unit
) {
    // null 代表「全部」
    val options: List<AchievementCategory?> = listOf(null) + AchievementCategory.entries

    LazyRow(
        modifier = Modifier.fillMaxWidth(),
        horizontalArrangement = Arrangement.spacedBy(8.dp)
    ) {
        items(options, key = { it?.name ?: "all" }) { category ->
            FilterChip(
                selected = selected == category,
                onClick = { onSelect(category) },
                label = { Text(category?.label ?: "全部") }
            )
        }
    }
}

@Composable
private fun CategoryHeader(
    category: AchievementCategory,
    unlocked: Int,
    total: Int
) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .padding(top = 4.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        Box(
            modifier = Modifier
                .size(24.dp)
                .clip(CircleShape)
                .background(MaterialTheme.colorScheme.surfaceContainerHighest),
            contentAlignment = Alignment.Center
        ) {
            Icon(
                imageVector = category.icon,
                contentDescription = null,
                tint = MaterialTheme.colorScheme.onSurfaceVariant,
                modifier = Modifier.size(14.dp)
            )
        }
        Spacer(Modifier.width(8.dp))
        Text(
            text = category.label,
            style = MaterialTheme.typography.titleSmall,
            color = MaterialTheme.colorScheme.primary,
            modifier = Modifier.weight(1f)
        )
        Text(
            text = "$unlocked / $total",
            style = MaterialTheme.typography.labelMedium,
            color = MaterialTheme.colorScheme.onSurfaceVariant
        )
    }
}

@Preview(showBackground = true)
@Composable
private fun AchievementScreenPreview() {
    AchievementScreen(onBack = {})
}
