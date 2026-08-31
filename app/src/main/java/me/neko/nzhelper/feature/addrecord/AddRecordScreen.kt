package me.neko.nzhelper.feature.addrecord

import android.content.Context
import android.content.Intent
import android.widget.Toast
import androidx.activity.compose.BackHandler
import androidx.compose.animation.core.FastOutSlowInEasing
import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.animation.core.tween
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.imePadding
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.pager.HorizontalPager
import androidx.compose.foundation.pager.rememberPagerState
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.outlined.ArrowBack
import androidx.compose.material.icons.outlined.Celebration
import androidx.compose.material.icons.outlined.Edit
import androidx.compose.material.icons.outlined.FavoriteBorder
import androidx.compose.material.icons.outlined.Info
import androidx.compose.material.icons.outlined.LocationOn
import androidx.compose.material.icons.outlined.Spa
import androidx.compose.material.icons.outlined.Star
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.IconButtonDefaults
import androidx.compose.material3.LinearProgressIndicator
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.unit.dp
import kotlinx.coroutines.launch
import me.neko.nzhelper.core.auto.AutoTagRules
import me.neko.nzhelper.core.database.SessionRepository
import me.neko.nzhelper.core.datastore.RecordModeSettings
import me.neko.nzhelper.core.datastore.TagSettings
import me.neko.nzhelper.core.model.Session
import me.neko.nzhelper.core.model.SessionFormState
import me.neko.nzhelper.core.model.SessionMode
import me.neko.nzhelper.core.model.toSession
import me.neko.nzhelper.core.service.TimerService
import me.neko.nzhelper.feature.addrecord.components.BasicInfoPage
import me.neko.nzhelper.feature.addrecord.components.PartnerLocationPage
import me.neko.nzhelper.feature.addrecord.components.PartnerPage
import me.neko.nzhelper.feature.addrecord.components.RatingPage
import me.neko.nzhelper.feature.addrecord.components.RemarkPage
import me.neko.nzhelper.feature.addrecord.components.SoloContextPage
import me.neko.nzhelper.feature.addrecord.components.SoloDetailPage
import me.neko.nzhelper.feature.addrecord.components.SummaryPage
import me.neko.nzhelper.ui.component.wizard.PageHeader
import java.time.LocalDateTime

enum class AddRecordFlow(val key: String) {
    TIMER("timer"),
    MANUAL("manual"),
    EDIT("edit");

    companion object {
        fun fromKey(key: String?): AddRecordFlow =
            entries.firstOrNull { it.key == key } ?: MANUAL
    }
}

private enum class AddStep(
    val icon: ImageVector,
    val title: String,
    val subtitle: String
) {
    BASIC(
        Icons.Outlined.Info,
        "基本信息",
        "选择记录模式，确认日期与时长"
    ),
    PARTNER_LOCATION(
        Icons.Outlined.LocationOn,
        "伴侣与地点",
        "选择伴侣、发起者和地点"
    ),
    SOLO_CONTEXT(
        Icons.Outlined.LocationOn,
        "地点与时间",
        "选择发生的地点和时间段"
    ),
    RATING(
        Icons.Outlined.Star,
        "体验与高潮",
        "为本次体验打分，记录高潮次数"
    ),
    PARTNER(
        Icons.Outlined.FavoriteBorder,
        "亲密细节",
        "记录对方性别、体位、玩具与避孕细节"
    ),
    SOLO_DETAIL(
        Icons.Outlined.Spa,
        "状态与道具",
        "记录身体状态、行为与使用的道具"
    ),
    REMARK(
        Icons.Outlined.Edit,
        "备注",
        "写点什么，留个纪念"
    ),
    SUMMARY(
        Icons.Outlined.Celebration,
        "确认信息",
        "核对无误后保存记录"
    )
}

private fun buildSteps(formState: SessionFormState): List<AddStep> =
    buildList {
        add(AddStep.BASIC)
        if (SessionMode.fromKey(formState.mode).isPair) {
            add(AddStep.PARTNER_LOCATION)
        } else {
            add(AddStep.SOLO_CONTEXT)
        }
        add(AddStep.RATING)
        if (SessionMode.fromKey(formState.mode).isPair) {
            add(AddStep.PARTNER)
        } else {
            add(AddStep.SOLO_DETAIL)
        }
        add(AddStep.REMARK)
        add(AddStep.SUMMARY)
    }

private fun initialFormState(
    context: Context,
    flow: AddRecordFlow,
    editSession: Session?
): SessionFormState {
    if (flow == AddRecordFlow.EDIT && editSession != null) {
        val ts = editSession.timestamp
        return SessionFormState(
            remark = editSession.remark,
            categoryId = editSession.categoryId,
            tagIds = editSession.tagIds.toSet(),
            mode = editSession.mode,
            climaxCount = editSession.climaxCount,
            partnerClimaxCount = editSession.partnerClimaxCount,
            partnerGender = editSession.partnerGender,
            partnerName = editSession.partnerName,
            contraception = editSession.contraception,
            partners = editSession.partners.toSet(),
            initiator = editSession.initiator,
            locations = editSession.locations.toSet(),
            moods = editSession.moods.toSet(),
            positions = editSession.positions.toSet(),
            toys = editSession.toys.toSet(),
            ejaculation = editSession.ejaculation,
            rating = editSession.rating,
            durationHour = (editSession.duration / 3600).toString(),
            durationMinute = ((editSession.duration % 3600) / 60).toString(),
            durationSecond = (editSession.duration % 60).takeIf { it != 0 }?.toString() ?: "",
            manualYear = ts.year,
            manualMonth = ts.monthValue,
            manualDay = ts.dayOfMonth,
            manualHour = ts.hour,
            manualMinute = ts.minute
        )
    }
    val now = LocalDateTime.now()
    val defaultMode = RecordModeSettings.getDefaultMode(context)
    val suggested = AutoTagRules.suggest(context, now)
    return SessionFormState(
        mode = defaultMode.key,
        categoryId = TagSettings.defaultCategoryFor(context, defaultMode).id,
        tagIds = suggested,
        autoTagIds = suggested,
        durationMinute = if (flow == AddRecordFlow.MANUAL) "15" else "",
        manualYear = now.year,
        manualMonth = now.monthValue,
        manualDay = now.dayOfMonth,
        manualHour = now.hour,
        manualMinute = now.minute
    )
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun AddRecordScreen(
    flow: AddRecordFlow,
    elapsedSeconds: Int,
    editSession: Session?,
    onClose: () -> Unit
) {
    val context = LocalContext.current
    val scope = rememberCoroutineScope()

    var formState by remember {
        mutableStateOf(initialFormState(context, flow, editSession))
    }
    var finished by remember { mutableStateOf(false) }

    val steps = buildSteps(formState)
    val pagerState = rememberPagerState(pageCount = { steps.size })

    val canGoBack = pagerState.currentPage > 0
    val isLastPage = pagerState.currentPage == steps.size - 1

    val progressTarget = (pagerState.currentPage + 1) / steps.size.toFloat()
    val animatedProgress by animateFloatAsState(
        targetValue = progressTarget,
        animationSpec = tween(durationMillis = 500, easing = FastOutSlowInEasing),
        label = "addRecordProgress"
    )

    fun cancel() {
        if (finished) return
        finished = true
        if (flow == AddRecordFlow.TIMER) {
            context.startService(Intent(context, TimerService::class.java).apply {
                action = TimerService.ACTION_START
            })
        }
        onClose()
    }

    fun skip() {
        scope.launch {
            pagerState.animateScrollToPage(steps.size - 1)
        }
    }

    BackHandler { cancel() }

    fun save() {
        if (finished) return
        val effectiveCategoryId =
            formState.categoryId.ifBlank { TagSettings.defaultCategory(context).id }

        when (flow) {
            AddRecordFlow.TIMER -> {
                finished = true
                val nowTime = LocalDateTime.now()
                val finalTags = run {
                    val suggested = AutoTagRules.suggest(context, nowTime)
                    val (merged, added) = AutoTagRules.merge(formState.tagIds, suggested)
                    merged to (formState.autoTagIds + added)
                }
                val session = formState.toSession(
                    timestamp = nowTime,
                    duration = elapsedSeconds,
                    categoryId = effectiveCategoryId,
                    tagIds = finalTags.first.toList()
                )
                scope.launch {
                    val sessions = SessionRepository.loadSessions(context).toMutableList()
                    sessions.add(0, session)
                    SessionRepository.saveSessions(context, sessions)
                    context.startService(Intent(context, TimerService::class.java).apply {
                        action = TimerService.ACTION_STOP
                    })
                    onClose()
                }
            }

            AddRecordFlow.MANUAL -> {
                val duration = formState.manualDurationSeconds
                if (duration <= 0) {
                    Toast.makeText(context, "请输入时长", Toast.LENGTH_SHORT).show()
                    return
                }
                val timestamp = try {
                    formState.toLocalDateTime()
                } catch (_: Exception) {
                    Toast.makeText(context, "日期时间无效，请重新选择", Toast.LENGTH_SHORT)
                        .show()
                    return
                }
                finished = true
                val session = formState.toSession(
                    timestamp = timestamp,
                    duration = duration,
                    categoryId = effectiveCategoryId,
                    tagIds = formState.tagIds.toList()
                )
                scope.launch {
                    val sessions = SessionRepository.loadSessions(context).toMutableList()
                    sessions.add(0, session)
                    SessionRepository.saveSessions(context, sessions)
                    onClose()
                }
            }

            AddRecordFlow.EDIT -> {
                val original = editSession ?: return
                val duration = formState.manualDurationSeconds
                if (duration <= 0) {
                    Toast.makeText(context, "请输入时长", Toast.LENGTH_SHORT).show()
                    return
                }
                val timestamp = try {
                    formState.toLocalDateTime()
                } catch (_: Exception) {
                    Toast.makeText(context, "日期时间无效，请重新选择", Toast.LENGTH_SHORT)
                        .show()
                    return
                }
                finished = true
                val isPair = SessionMode.fromKey(formState.mode).isPair
                val updated = original.copy(
                    timestamp = timestamp,
                    duration = duration,
                    remark = formState.remark,
                    rating = formState.rating,
                    categoryId = effectiveCategoryId,
                    tagIds = formState.tagIds.toList(),
                    mode = formState.mode,
                    climaxCount = formState.climaxCount,
                    partnerClimaxCount = if (isPair) formState.partnerClimaxCount else 0,
                    partnerGender = if (isPair) formState.partnerGender else "",
                    partnerName = if (isPair) formState.partnerName else "",
                    contraception = if (isPair) formState.contraception else "",
                    partners = if (isPair) formState.partners.toList() else emptyList(),
                    initiator = if (isPair) formState.initiator else "",
                    locations = formState.locations.toList(),
                    moods = formState.moods.toList(),
                    positions = if (isPair) formState.positions.toList() else emptyList(),
                    toys = if (isPair) formState.toys.toList() else emptyList(),
                    ejaculation = if (isPair) formState.ejaculation else ""
                )
                scope.launch {
                    val sessions = SessionRepository.loadSessions(context).toMutableList()
                    val index = sessions.indexOf(original)
                    if (index != -1) sessions[index] = updated else sessions.add(0, updated)
                    SessionRepository.saveSessions(context, sessions)
                    onClose()
                }
            }
        }
    }

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
                    .padding(top = 4.dp),
                verticalAlignment = Alignment.CenterVertically
            ) {
                IconButton(
                    onClick = { cancel() },
                    colors = IconButtonDefaults.iconButtonColors(
                        containerColor = MaterialTheme.colorScheme.surfaceContainerHigh
                    )
                ) {
                    Icon(
                        Icons.AutoMirrored.Outlined.ArrowBack,
                        contentDescription = "返回"
                    )
                }
                LinearProgressIndicator(
                    progress = { animatedProgress },
                    modifier = Modifier
                        .weight(1f)
                        .height(6.dp)
                        .padding(horizontal = 12.dp)
                )
                Spacer(Modifier.width(8.dp))
                if (!isLastPage) {
                    TextButton(
                        onClick = { skip() },
                        colors = ButtonDefaults.textButtonColors(
                            containerColor = MaterialTheme.colorScheme.surfaceContainerHigh
                        ),
                        contentPadding = PaddingValues(horizontal = 8.dp, vertical = 0.dp)
                    ) {
                        Text("跳过")
                    }
                }
            }

            HorizontalPager(
                state = pagerState,
                modifier = Modifier.weight(1f),
                userScrollEnabled = false
            ) { page ->
                val step = steps[page]
                Column(
                    modifier = Modifier
                        .fillMaxSize()
                        .verticalScroll(rememberScrollState())
                        .padding(vertical = 24.dp),
                    horizontalAlignment = Alignment.CenterHorizontally
                ) {
                    PageHeader(
                        icon = step.icon,
                        title = step.title,
                        subtitle = step.subtitle
                    )
                    Spacer(Modifier.height(24.dp))
                    when (step) {
                        AddStep.BASIC -> BasicInfoPage(
                            flow = flow,
                            formState = formState,
                            elapsedSeconds = elapsedSeconds,
                            editSession = editSession
                        ) { formState = it }

                        AddStep.PARTNER_LOCATION ->
                            PartnerLocationPage(formState) { formState = it }

                        AddStep.SOLO_CONTEXT ->
                            SoloContextPage(formState) { formState = it }

                        AddStep.RATING -> RatingPage(formState) { formState = it }
                        AddStep.PARTNER -> PartnerPage(formState) { formState = it }

                        AddStep.SOLO_DETAIL ->
                            SoloDetailPage(formState) { formState = it }

                        AddStep.REMARK -> RemarkPage(formState) { formState = it }
                        AddStep.SUMMARY -> SummaryPage(
                            flow = flow,
                            formState = formState,
                            elapsedSeconds = elapsedSeconds,
                            editSession = editSession
                        )
                    }
                }
            }

            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .imePadding()
                    .padding(vertical = 16.dp),
                horizontalArrangement = Arrangement.spacedBy(12.dp)
            ) {
                if (canGoBack) {
                    OutlinedButton(
                        onClick = {
                            scope.launch {
                                pagerState.animateScrollToPage(pagerState.currentPage - 1)
                            }
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
                            save()
                        } else {
                            scope.launch {
                                pagerState.animateScrollToPage(pagerState.currentPage + 1)
                            }
                        }
                    },
                    modifier = Modifier.weight(1f),
                    shape = MaterialTheme.shapes.large
                ) {
                    Text(
                        if (isLastPage) {
                            if (flow == AddRecordFlow.EDIT) "保存修改" else "保存记录"
                        } else {
                            "下一步"
                        }
                    )
                }
            }
        }
    }
}

