package me.neko.nzhelper.core.datastore

import android.content.Context
import com.google.gson.Gson
import com.google.gson.reflect.TypeToken
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.runBlocking
import me.neko.nzhelper.NzApplication
import me.neko.nzhelper.core.database.AppDatabase
import me.neko.nzhelper.core.database.entity.TaxonomyEntity
import me.neko.nzhelper.core.model.CategoryDef
import me.neko.nzhelper.core.model.Session
import me.neko.nzhelper.core.model.SessionMode
import me.neko.nzhelper.core.model.TagDef
import me.neko.nzhelper.core.model.TagGroupDef
import java.util.UUID

object TagSettings {

    const val DEFAULT_CATEGORY_ID: String = Session.DEFAULT_CATEGORY_ID
    const val CAT_F_SELF_ID: String = "cat_f_self"
    const val CAT_PAIR_ID: String = "cat_pair"

    private const val KEY_CATEGORIES = "categories"
    private const val KEY_GROUPS = "groups"
    private const val KEY_TAGS = "tags"
    private const val KEY_DEFAULTS_SEEDED = "defaults_seeded_v1"
    private const val KEY_DEFAULTS_SEEDED_V3 = "defaults_seeded_v3"

    private val gson: Gson get() = NzApplication.gson

    private val DEFAULT_CATEGORIES = listOf(
        CategoryDef("cat_self", "手冲", "hand", "rose", 0),
        CategoryDef(CAT_F_SELF_ID, "自慰", "smile", "pink", 1),
        CategoryDef(CAT_PAIR_ID, "双人", "heart-pulse", "rose", 2)
    )

    private val DEFAULT_GROUPS = listOf(
        TagGroupDef("grp_env", "地点", "map-pin", "emerald", 0),
        TagGroupDef("grp_time", "时间", "clock", "amber", 1),
        TagGroupDef("grp_state", "情绪", "mood", "rose", 2),
        TagGroupDef("grp_body", "身体", "heart-pulse", "teal", 3),
        TagGroupDef("grp_act", "行为", "sparkles", "violet", 4),
        TagGroupDef("grp_tool", "道具", "wrench", "slate", 5),
        TagGroupDef(
            "grp_partner", "伴侣", "smile", "pink", 6,
            listOf(SessionMode.PAIR.key)
        ),
        TagGroupDef(
            "grp_position", "体位", "bed-double", "orange", 7,
            listOf(SessionMode.PAIR.key)
        ),
        TagGroupDef(
            "grp_ejaculate", "射精方式", "droplets", "sky", 8,
            listOf(SessionMode.PAIR.key)
        )
    )

    private val DEFAULT_TAGS = listOf(
        // 地点
        TagDef("tag_env_bedroom", "卧室", "bed", "emerald", "grp_env", 0),
        TagDef("tag_env_livingroom", "客厅", "sofa", "emerald", "grp_env", 1),
        TagDef("tag_env_kitchen", "厨房", "kitchen", "emerald", "grp_env", 2),
        TagDef("tag_env_bathroom", "浴室", "shower-head", "teal", "grp_env", 3),
        TagDef("tag_env_toilet", "厕所", "door-closed", "emerald", "grp_env", 4),
        TagDef("tag_env_balcony", "阳台", "landscape", "teal", "grp_env", 5),
        TagDef("tag_env_stairwell", "楼梯间", "map", "emerald", "grp_env", 6),
        TagDef("tag_env_sofa", "沙发", "sofa", "emerald", "grp_env", 7),
        TagDef("tag_env_office", "办公室", "briefcase", "emerald", "grp_env", 8),
        TagDef("tag_env_hotel", "酒店出差", "building-2", "teal", "grp_env", 9),
        TagDef("tag_env_cinema", "电影院", "tv", "violet", "grp_env", 10),
        TagDef("tag_env_wild", "野外", "forest", "emerald", "grp_env", 11),
        TagDef("tag_env_car", "车里", "map-pin", "emerald", "grp_env", 12),
        TagDef("tag_env_outdoor", "户外", "leaf", "emerald", "grp_env", 13),
        // 时间
        TagDef("tag_time_morning", "上午", "sunrise", "amber", "grp_time", 0),
        TagDef("tag_time_afternoon", "下午", "sun", "orange", "grp_time", 1),
        TagDef("tag_time_evening", "晚上", "sunset", "amber", "grp_time", 2),
        TagDef("tag_time_latenight", "深夜", "moon", "amber", "grp_time", 3),
        TagDef("tag_time_dawn", "凌晨", "moon-star", "orange", "grp_time", 4),
        TagDef("tag_time_earlymorning", "清晨", "sunrise", "amber", "grp_time", 5),
        TagDef("tag_time_noonbreak", "午休", "bed", "amber", "grp_time", 6),
        TagDef("tag_time_overnight", "通宵", "moon-star", "orange", "grp_time", 7),
        TagDef("tag_time_weekend", "周末", "calendar-days", "amber", "grp_time", 8),
        TagDef("tag_time_holiday", "节假日", "party-popper", "amber", "grp_time", 9),
        TagDef("tag_time_weekday", "工作日", "calendar", "orange", "grp_time", 10),
        // 情绪
        TagDef("tag_state_calm", "平静", "leaf", "slate", "grp_state", 0),
        TagDef("tag_state_stress", "压力大", "brain", "rose", "grp_state", 1),
        TagDef("tag_state_happy", "开心", "smile", "pink", "grp_state", 2),
        TagDef("tag_state_excited", "兴奋", "flame", "rose", "grp_state", 3),
        TagDef("tag_state_joy", "愉悦", "party-popper", "pink", "grp_state", 4),
        TagDef("tag_state_bored", "无聊", "meh", "slate", "grp_state", 5),
        TagDef("tag_state_empty", "空虚", "cloud-fog", "slate", "grp_state", 6),
        TagDef("tag_state_romantic", "浪漫", "heart-pulse", "rose", "grp_state", 7),
        TagDef("tag_state_gentle", "温柔", "smile", "pink", "grp_state", 8),
        TagDef("tag_state_passion", "激情", "flame", "rose", "grp_state", 9),
        TagDef("tag_state_relaxed", "放松", "leaf", "slate", "grp_state", 10),
        TagDef("tag_state_nervous", "紧张", "brain", "amber", "grp_state", 11),
        TagDef("tag_state_expectant", "期待", "sparkles", "pink", "grp_state", 12),
        TagDef("tag_state_satisfied", "满足", "thumb-up", "pink", "grp_state", 13),
        TagDef("tag_state_shy", "害羞", "smile-2", "pink", "grp_state", 14),
        TagDef("tag_state_curious", "好奇", "brain", "slate", "grp_state", 15),
        TagDef("tag_state_anxious", "焦虑", "battery-alert", "amber", "grp_state", 16),
        // 身体
        TagDef("tag_body_exhausted", "疲惫", "battery-alert", "rose", "grp_body", 0),
        TagDef("tag_body_sore", "酸痛", "battery-low", "rose", "grp_body", 1),
        TagDef("tag_body_headache", "头痛", "brain", "rose", "grp_body", 2),
        TagDef("tag_body_insomnia", "失眠", "eye-off", "rose", "grp_body", 3),
        TagDef("tag_body_sick", "生病", "thermometer", "rose", "grp_body", 4),
        TagDef("tag_body_hungry", "空腹", "restaurant", "amber", "grp_body", 5),
        TagDef("tag_body_drunk", "酒后", "wine", "violet", "grp_body", 6),
        TagDef(
            "tag_body_period", "生理期", "calendar-days", "rose", "grp_body", 7,
            listOf(SessionMode.SOLO_FEMALE.key)
        ),
        TagDef("tag_body_fever", "发烧", "thermometer", "rose", "grp_body", 8),
        TagDef("tag_body_cough", "咳嗽", "cloud-fog", "rose", "grp_body", 9),
        TagDef("tag_body_allergy", "过敏", "sparkles", "amber", "grp_body", 10),
        TagDef("tag_body_backache", "腰酸", "battery-low", "rose", "grp_body", 11),
        TagDef("tag_body_heat", "上火", "flame", "amber", "grp_body", 12),
        TagDef("tag_body_latenight", "熬夜", "moon-star", "rose", "grp_body", 13),
        // 行为
        TagDef(
            "tag_act_porn", "看小电影", "monitor-play", "violet", "grp_act", 0,
            listOf(SessionMode.SOLO_MALE.key, SessionMode.SOLO_FEMALE.key)
        ),
        TagDef("tag_act_aftershower", "洗澡后", "droplets", "violet", "grp_act", 1),
        TagDef("tag_act_aftergym", "运动后", "dumbbell", "violet", "grp_act", 2),
        TagDef("tag_act_drunk", "喝酒", "wine", "violet", "grp_act", 3),
        TagDef("tag_act_bed", "赖床", "bed-double", "violet", "grp_act", 4),
        TagDef("tag_act_beforesleep", "睡前", "moon", "violet", "grp_act", 5),
        TagDef("tag_act_naked", "裸睡", "bed", "violet", "grp_act", 6),
        TagDef(
            "tag_act_foreplay", "前戏", "heart-pulse", "violet", "grp_act", 7,
            listOf(SessionMode.PAIR.key)
        ),
        TagDef(
            "tag_act_aftercare", "事后温存", "smile", "violet", "grp_act", 8,
            listOf(SessionMode.PAIR.key)
        ),
        TagDef(
            "tag_act_safeperiod", "安全期", "calendar", "violet", "grp_act", 9,
            listOf(SessionMode.PAIR.key)
        ),
        TagDef(
            "tag_act_kiss", "接吻", "favorite", "violet", "grp_act", 10,
            listOf(SessionMode.PAIR.key)
        ),
        TagDef(
            "tag_act_caress", "爱抚", "hand", "violet", "grp_act", 11,
            listOf(SessionMode.PAIR.key)
        ),
        TagDef(
            "tag_act_hug", "拥抱", "heart-pulse", "violet", "grp_act", 12,
            listOf(SessionMode.PAIR.key)
        ),
        TagDef(
            "tag_act_shower2", "一起洗澡", "bathtub", "violet", "grp_act", 13,
            listOf(SessionMode.PAIR.key)
        ),
        TagDef(
            "tag_act_massage", "按摩", "healing", "violet", "grp_act", 14,
            listOf(SessionMode.PAIR.key)
        ),
        TagDef("tag_act_earlyrise", "早起", "sunrise", "violet", "grp_act", 15),
        TagDef("tag_act_videos", "刷视频", "videocam", "violet", "grp_act", 16),
        TagDef("tag_act_alone", "独处", "person", "violet", "grp_act", 17),
        TagDef("tag_act_biztrip", "出差", "briefcase", "violet", "grp_act", 18),
        TagDef(
            "tag_act_comic", "看漫画", "tv", "violet", "grp_act", 19,
            listOf(SessionMode.SOLO_MALE.key, SessionMode.SOLO_FEMALE.key)
        ),
        TagDef(
            "tag_act_audio", "听语音", "headphones", "violet", "grp_act", 20,
            listOf(SessionMode.SOLO_MALE.key, SessionMode.SOLO_FEMALE.key)
        ),
        TagDef(
            "tag_act_chat", "聊天", "smile", "violet", "grp_act", 21,
            listOf(SessionMode.SOLO_MALE.key, SessionMode.SOLO_FEMALE.key)
        ),
        TagDef(
            "tag_act_fantasy", "性幻想", "brain", "violet", "grp_act", 22,
            listOf(SessionMode.SOLO_MALE.key, SessionMode.SOLO_FEMALE.key)
        ),
        // 道具
        TagDef("tag_tool_hand", "手", "hand", "teal", "grp_tool", 0),
        TagDef(
            "tag_tool_cup", "飞机杯", "cup-soda", "teal", "grp_tool", 1,
            listOf(SessionMode.SOLO_MALE.key)
        ),
        TagDef(
            "tag_tool_dildo", "假阳具", "toys", "pink", "grp_tool", 5,
            listOf(SessionMode.SOLO_FEMALE.key)
        ),
        TagDef(
            "tag_tool_eggtoy", "跳蛋", "sparkles", "pink", "grp_tool", 6,
            listOf(SessionMode.PAIR.key, SessionMode.SOLO_FEMALE.key)
        ),
        TagDef(
            "tag_tool_vibrator", "震动棒", "sparkles", "rose", "grp_tool", 7,
            listOf(SessionMode.PAIR.key)
        ),
        TagDef(
            "tag_tool_analplug", "肛塞", "droplets", "teal", "grp_tool", 8,
            listOf(SessionMode.PAIR.key)
        ),
        TagDef(
            "tag_tool_bondage", "束缚", "wrench", "slate", "grp_tool", 9,
            listOf(SessionMode.PAIR.key)
        ),
        TagDef(
            "tag_tool_eyemask", "眼罩", "eye-off", "slate", "grp_tool", 10,
            listOf(SessionMode.PAIR.key)
        ),
        TagDef(
            "tag_tool_nippleclamp", "乳夹", "favorite", "pink", "grp_tool", 11,
            listOf(SessionMode.PAIR.key)
        ),
        TagDef(
            "tag_tool_collar", "项圈", "pets", "slate", "grp_tool", 12,
            listOf(SessionMode.PAIR.key)
        ),
        TagDef(
            "tag_tool_gag", "口球", "meh", "slate", "grp_tool", 13,
            listOf(SessionMode.PAIR.key)
        ),
        TagDef(
            "tag_tool_whip", "鞭子", "bolt", "amber", "grp_tool", 14,
            listOf(SessionMode.PAIR.key)
        ),
        TagDef(
            "tag_tool_candle", "蜡烛", "flame", "amber", "grp_tool", 15,
            listOf(SessionMode.PAIR.key)
        ),
        TagDef(
            "tag_tool_massager", "按摩器", "healing", "pink", "grp_tool", 16,
            listOf(SessionMode.PAIR.key)
        ),
        TagDef(
            "tag_tool_meiki", "名器", "toys", "teal", "grp_tool", 17,
            listOf(SessionMode.SOLO_MALE.key)
        ),
        TagDef(
            "tag_tool_mold", "倒模", "sparkles", "teal", "grp_tool", 18,
            listOf(SessionMode.SOLO_MALE.key)
        ),
        TagDef(
            "tag_tool_inflatable", "充气娃娃", "baby", "teal", "grp_tool", 19,
            listOf(SessionMode.SOLO_MALE.key)
        ),
        TagDef(
            "tag_tool_massagewand", "按摩棒", "healing", "pink", "grp_tool", 20,
            listOf(SessionMode.SOLO_FEMALE.key)
        ),
        TagDef(
            "tag_tool_suction", "吸吮器", "droplets", "pink", "grp_tool", 21,
            listOf(SessionMode.SOLO_FEMALE.key)
        ),
        TagDef(
            "tag_tool_wearable", "穿戴玩具", "smart-toy", "pink", "grp_tool", 22,
            listOf(SessionMode.SOLO_FEMALE.key)
        ),
        // 体位（仅双人）
        TagDef(
            "tag_pos_missionary", "传教士", "bed-double", "orange", "grp_position", 0,
            listOf(SessionMode.PAIR.key)
        ),
        TagDef(
            "tag_pos_doggy", "后入", "bed", "orange", "grp_position", 1,
            listOf(SessionMode.PAIR.key)
        ),
        TagDef(
            "tag_pos_cowgirl", "女上", "smile", "orange", "grp_position", 2,
            listOf(SessionMode.PAIR.key)
        ),
        TagDef(
            "tag_pos_riding", "骑乘", "pets", "orange", "grp_position", 3,
            listOf(SessionMode.PAIR.key)
        ),
        TagDef(
            "tag_pos_sideways", "侧卧", "sofa", "orange", "grp_position", 4,
            listOf(SessionMode.PAIR.key)
        ),
        TagDef(
            "tag_pos_sitting", "坐姿", "mood", "orange", "grp_position", 5,
            listOf(SessionMode.PAIR.key)
        ),
        TagDef(
            "tag_pos_standing", "站姿", "mood", "orange", "grp_position", 6,
            listOf(SessionMode.PAIR.key)
        ),
        TagDef(
            "tag_pos_oral", "口交", "sparkles", "orange", "grp_position", 7,
            listOf(SessionMode.PAIR.key)
        ),
        TagDef(
            "tag_pos_deepthroat", "深喉", "sparkles", "orange", "grp_position", 8,
            listOf(SessionMode.PAIR.key)
        ),
        TagDef(
            "tag_pos_handjob", "手交", "hand", "orange", "grp_position", 9,
            listOf(SessionMode.PAIR.key)
        ),
        TagDef(
            "tag_pos_titjob", "乳交", "favorite", "orange", "grp_position", 10,
            listOf(SessionMode.PAIR.key)
        ),
        TagDef(
            "tag_pos_69", "69", "moon", "orange", "grp_position", 11,
            listOf(SessionMode.PAIR.key)
        ),
        // 射精方式（仅双人）
        TagDef(
            "tag_ejac_inside", "体内", "droplets", "sky", "grp_ejaculate", 0,
            listOf(SessionMode.PAIR.key)
        ),
        TagDef(
            "tag_ejac_outside", "体外", "sun", "sky", "grp_ejaculate", 1,
            listOf(SessionMode.PAIR.key)
        ),
        TagDef(
            "tag_ejac_oral", "口内", "smile", "sky", "grp_ejaculate", 2,
            listOf(SessionMode.PAIR.key)
        ),
        TagDef(
            "tag_ejac_facial", "颜射", "sparkles", "sky", "grp_ejaculate", 3,
            listOf(SessionMode.PAIR.key)
        ),
        TagDef(
            "tag_ejac_chest", "胸部", "favorite", "sky", "grp_ejaculate", 4,
            listOf(SessionMode.PAIR.key)
        ),
        TagDef(
            "tag_ejac_belly", "腹部", "waves", "sky", "grp_ejaculate", 5,
            listOf(SessionMode.PAIR.key)
        ),
        TagDef(
            "tag_ejac_back", "后背", "person", "sky", "grp_ejaculate", 6,
            listOf(SessionMode.PAIR.key)
        )
    )

    val LEGACY_GROUP_ENV: String = "grp_env"
    val LEGACY_GROUP_STATE: String = "grp_state"
    val LEGACY_GROUP_TOOL: String = "grp_tool"
    val LEGACY_GROUP_ACT: String = "grp_act"
    val GROUP_PARTNER: String = "grp_partner"
    val GROUP_POSITION: String = "grp_position"
    val GROUP_EJACULATE: String = "grp_ejaculate"
    val GROUP_TIME: String = "grp_time"
    val GROUP_BODY: String = "grp_body"

    private fun dao(context: Context) = AppDatabase.get(context).taxonomyDao()

    private val cache = HashMap<String, String?>()

    fun preload(context: Context) {
        runBlocking(Dispatchers.IO) {
            val dao = dao(context)
            val keys = listOf(
                KEY_CATEGORIES, KEY_GROUPS, KEY_TAGS,
                KEY_DEFAULTS_SEEDED, KEY_DEFAULTS_SEEDED_V3
            )
            for (key in keys) {
                cache[key] = dao.get(key)
            }
            if (cache[KEY_DEFAULTS_SEEDED] != "true") {
                val d = dao
                if (d.get(KEY_CATEGORIES) == null) {
                    val j = gson.toJson(DEFAULT_CATEGORIES)
                    d.upsert(TaxonomyEntity(KEY_CATEGORIES, j))
                    cache[KEY_CATEGORIES] = j
                }
                if (d.get(KEY_GROUPS) == null) {
                    val j = gson.toJson(DEFAULT_GROUPS)
                    d.upsert(TaxonomyEntity(KEY_GROUPS, j))
                    cache[KEY_GROUPS] = j
                }
                if (d.get(KEY_TAGS) == null) {
                    val j = gson.toJson(DEFAULT_TAGS)
                    d.upsert(TaxonomyEntity(KEY_TAGS, j))
                    cache[KEY_TAGS] = j
                }
                d.upsert(TaxonomyEntity(KEY_DEFAULTS_SEEDED, "true"))
                cache[KEY_DEFAULTS_SEEDED] = "true"
                d.upsert(TaxonomyEntity(KEY_DEFAULTS_SEEDED_V3, "true"))
                cache[KEY_DEFAULTS_SEEDED_V3] = "true"
            }
        }
    }

    private fun readRaw(context: Context, key: String): String? {
        return cache[key]
    }

    private fun writeRaw(context: Context, key: String, value: String) {
        val old = cache[key]
        cache[key] = value
        NzApplication.appScope.launch {
            try {
                dao(context).upsert(TaxonomyEntity(key, value))
            } catch (e: Exception) {
                e.printStackTrace()
                cache[key] = old
            }
        }
    }

    fun ensureDefaults(context: Context) {
        if (readRaw(context, KEY_DEFAULTS_SEEDED) != "true") {
            val defaultsJson = mapOf(
                KEY_CATEGORIES to gson.toJson(DEFAULT_CATEGORIES),
                KEY_GROUPS to gson.toJson(DEFAULT_GROUPS),
                KEY_TAGS to gson.toJson(DEFAULT_TAGS)
            )
            for ((key, json) in defaultsJson) {
                if (cache[key] == null) {
                    writeRaw(context, key, json)
                }
            }
            writeRaw(context, KEY_DEFAULTS_SEEDED, "true")
            writeRaw(context, KEY_DEFAULTS_SEEDED_V3, "true")
        }
        ensureModeDefaults(context)
        ensureExtraTagDefaults(context)
    }

    private fun ensureExtraTagDefaults(context: Context) {
        val legacyPartnerIds = setOf(
            "tag_partner_gf", "tag_partner_wife", "tag_partner_regular", "tag_partner_temp"
        )
        val removedToolNames = setOf("安全套", "润滑液")
        val removedToolIds = setOf("tag_tool_doll", "tag_tool_delayring", "tag_tool_toy")
        val eggToyModeKeys = listOf(SessionMode.PAIR.key, SessionMode.SOLO_FEMALE.key)
        val currentTags = getTags(context)
        for (id in legacyPartnerIds) {
            if (currentTags.any { it.id == id }) {
                deleteTag(context, id)
            }
        }
        for (tag in currentTags) {
            if (tag.groupId == LEGACY_GROUP_TOOL && tag.name in removedToolNames) {
                deleteTag(context, tag.id)
            }
            if (tag.id in removedToolIds) {
                deleteTag(context, tag.id)
            }
        }
        val eggToy = currentTags.firstOrNull { it.id == "tag_tool_eggtoy" }
        if (eggToy != null && eggToy.modeKeys != eggToyModeKeys) {
            updateTag(context, eggToy.id, modeKeys = eggToyModeKeys)
        }
        val pairKeys = listOf(SessionMode.PAIR.key)
        val maleKeys = listOf(SessionMode.SOLO_MALE.key)
        val femaleKeys = listOf(SessionMode.SOLO_FEMALE.key)
        val soloKeys = listOf(SessionMode.SOLO_MALE.key, SessionMode.SOLO_FEMALE.key)
        val seeds = listOf(
            // 地点
            TagSeed("客厅", LEGACY_GROUP_ENV, "sofa", "emerald", emptyList()),
            TagSeed("厨房", LEGACY_GROUP_ENV, "kitchen", "emerald", emptyList()),
            TagSeed("阳台", LEGACY_GROUP_ENV, "landscape", "teal", emptyList()),
            TagSeed("楼梯间", LEGACY_GROUP_ENV, "map", "emerald", emptyList()),
            TagSeed("电影院", LEGACY_GROUP_ENV, "tv", "violet", emptyList()),
            TagSeed("野外", LEGACY_GROUP_ENV, "forest", "emerald", emptyList()),
            TagSeed("车里", LEGACY_GROUP_ENV, "map-pin", "emerald", emptyList()),
            TagSeed("户外", LEGACY_GROUP_ENV, "leaf", "emerald", emptyList()),
            // 时间
            TagSeed("清晨", GROUP_TIME, "sunrise", "amber", emptyList()),
            TagSeed("午休", GROUP_TIME, "bed", "amber", emptyList()),
            TagSeed("通宵", GROUP_TIME, "moon-star", "orange", emptyList()),
            TagSeed("节假日", GROUP_TIME, "party-popper", "amber", emptyList()),
            // 情绪
            TagSeed("浪漫", LEGACY_GROUP_STATE, "heart-pulse", "rose", emptyList()),
            TagSeed("温柔", LEGACY_GROUP_STATE, "smile", "pink", emptyList()),
            TagSeed("激情", LEGACY_GROUP_STATE, "flame", "rose", emptyList()),
            TagSeed("放松", LEGACY_GROUP_STATE, "leaf", "slate", emptyList()),
            TagSeed("紧张", LEGACY_GROUP_STATE, "brain", "amber", emptyList()),
            TagSeed("期待", LEGACY_GROUP_STATE, "sparkles", "pink", emptyList()),
            TagSeed("满足", LEGACY_GROUP_STATE, "thumb-up", "pink", emptyList()),
            TagSeed("害羞", LEGACY_GROUP_STATE, "smile-2", "pink", emptyList()),
            TagSeed("好奇", LEGACY_GROUP_STATE, "brain", "slate", emptyList()),
            TagSeed("焦虑", LEGACY_GROUP_STATE, "battery-alert", "amber", emptyList()),
            // 身体
            TagSeed("酸痛", GROUP_BODY, "battery-low", "rose", emptyList()),
            TagSeed("头痛", GROUP_BODY, "brain", "rose", emptyList()),
            TagSeed("空腹", GROUP_BODY, "restaurant", "amber", emptyList()),
            TagSeed("酒后", GROUP_BODY, "wine", "violet", emptyList()),
            TagSeed("发烧", GROUP_BODY, "thermometer", "rose", emptyList()),
            TagSeed("咳嗽", GROUP_BODY, "cloud-fog", "rose", emptyList()),
            TagSeed("过敏", GROUP_BODY, "sparkles", "amber", emptyList()),
            TagSeed("腰酸", GROUP_BODY, "battery-low", "rose", emptyList()),
            TagSeed("上火", GROUP_BODY, "flame", "amber", emptyList()),
            TagSeed("熬夜", GROUP_BODY, "moon-star", "rose", emptyList()),
            // 行为
            TagSeed("裸睡", LEGACY_GROUP_ACT, "bed", "violet", emptyList()),
            TagSeed("早起", LEGACY_GROUP_ACT, "sunrise", "violet", emptyList()),
            TagSeed("刷视频", LEGACY_GROUP_ACT, "videocam", "violet", emptyList()),
            TagSeed("独处", LEGACY_GROUP_ACT, "person", "violet", emptyList()),
            TagSeed("出差", LEGACY_GROUP_ACT, "briefcase", "violet", emptyList()),
            TagSeed("看漫画", LEGACY_GROUP_ACT, "tv", "violet", soloKeys),
            TagSeed("听语音", LEGACY_GROUP_ACT, "headphones", "violet", soloKeys),
            TagSeed("聊天", LEGACY_GROUP_ACT, "smile", "violet", soloKeys),
            TagSeed("性幻想", LEGACY_GROUP_ACT, "brain", "violet", soloKeys),
            TagSeed("接吻", LEGACY_GROUP_ACT, "favorite", "violet", pairKeys),
            TagSeed("爱抚", LEGACY_GROUP_ACT, "hand", "violet", pairKeys),
            TagSeed("拥抱", LEGACY_GROUP_ACT, "heart-pulse", "violet", pairKeys),
            TagSeed("一起洗澡", LEGACY_GROUP_ACT, "bathtub", "violet", pairKeys),
            TagSeed("按摩", LEGACY_GROUP_ACT, "healing", "violet", pairKeys),
            // 道具
            TagSeed("名器", LEGACY_GROUP_TOOL, "toys", "teal", maleKeys),
            TagSeed("倒模", LEGACY_GROUP_TOOL, "sparkles", "teal", maleKeys),
            TagSeed("充气娃娃", LEGACY_GROUP_TOOL, "baby", "teal", maleKeys),
            TagSeed("假阳具", LEGACY_GROUP_TOOL, "toys", "pink", femaleKeys),
            TagSeed("按摩棒", LEGACY_GROUP_TOOL, "healing", "pink", femaleKeys),
            TagSeed("吸吮器", LEGACY_GROUP_TOOL, "droplets", "pink", femaleKeys),
            TagSeed("穿戴玩具", LEGACY_GROUP_TOOL, "smart-toy", "pink", femaleKeys),
            TagSeed("跳蛋", LEGACY_GROUP_TOOL, "sparkles", "pink", pairKeys),
            TagSeed("震动棒", LEGACY_GROUP_TOOL, "sparkles", "rose", pairKeys),
            TagSeed("肛塞", LEGACY_GROUP_TOOL, "droplets", "teal", pairKeys),
            TagSeed("束缚", LEGACY_GROUP_TOOL, "wrench", "slate", pairKeys),
            TagSeed("眼罩", LEGACY_GROUP_TOOL, "eye-off", "slate", pairKeys),
            TagSeed("乳夹", LEGACY_GROUP_TOOL, "favorite", "pink", pairKeys),
            TagSeed("项圈", LEGACY_GROUP_TOOL, "pets", "slate", pairKeys),
            TagSeed("口球", LEGACY_GROUP_TOOL, "meh", "slate", pairKeys),
            TagSeed("鞭子", LEGACY_GROUP_TOOL, "bolt", "amber", pairKeys),
            TagSeed("蜡烛", LEGACY_GROUP_TOOL, "flame", "amber", pairKeys),
            TagSeed("按摩器", LEGACY_GROUP_TOOL, "healing", "pink", pairKeys),
            // 体位
            TagSeed("站姿", GROUP_POSITION, "mood", "orange", pairKeys),
            TagSeed("骑乘", GROUP_POSITION, "pets", "orange", pairKeys),
            TagSeed("深喉", GROUP_POSITION, "sparkles", "orange", pairKeys),
            TagSeed("手交", GROUP_POSITION, "hand", "orange", pairKeys),
            TagSeed("乳交", GROUP_POSITION, "favorite", "orange", pairKeys),
            // 射精方式
            TagSeed("胸部", GROUP_EJACULATE, "favorite", "sky", pairKeys),
            TagSeed("腹部", GROUP_EJACULATE, "waves", "sky", pairKeys),
            TagSeed("后背", GROUP_EJACULATE, "person", "sky", pairKeys)
        )
        val existing = getTags(context)
        for (seed in seeds) {
            if (existing.none { it.name == seed.name }) {
                addTag(context, seed.name, seed.groupId, seed.icon, seed.color, seed.modeKeys)
            }
        }
    }

    private data class TagSeed(
        val name: String,
        val groupId: String,
        val icon: String,
        val color: String,
        val modeKeys: List<String>
    )

    fun isTaxonomyUpdated(context: Context): Boolean =
        readRaw(context, KEY_DEFAULTS_SEEDED_V3) == "true"

    fun shouldPromptTaxonomyUpdate(context: Context): Boolean =
        readRaw(context, KEY_DEFAULTS_SEEDED) == "true" &&
                !isTaxonomyUpdated(context)

    fun applyTaxonomyUpdate(context: Context) {
        val groupsJson = gson.toJson(DEFAULT_GROUPS)
        val tagsJson = gson.toJson(DEFAULT_TAGS)
        cache.remove(KEY_GROUPS)
        cache.remove(KEY_TAGS)
        cache[KEY_GROUPS] = groupsJson
        cache[KEY_TAGS] = tagsJson
        cache[KEY_DEFAULTS_SEEDED_V3] = "true"
        NzApplication.appScope.launch {
            try {
                val d = dao(context)
                d.delete(KEY_GROUPS)
                d.delete(KEY_TAGS)
                d.upsert(TaxonomyEntity(KEY_GROUPS, groupsJson))
                d.upsert(TaxonomyEntity(KEY_TAGS, tagsJson))
                d.upsert(TaxonomyEntity(KEY_DEFAULTS_SEEDED_V3, "true"))
            } catch (e: Exception) {
                e.printStackTrace()
            }
        }
    }

    fun restoreDefaultTaxonomy(context: Context) {
        val categoriesJson = gson.toJson(DEFAULT_CATEGORIES)
        val groupsJson = gson.toJson(DEFAULT_GROUPS)
        val tagsJson = gson.toJson(DEFAULT_TAGS)
        cache[KEY_CATEGORIES] = categoriesJson
        cache[KEY_GROUPS] = groupsJson
        cache[KEY_TAGS] = tagsJson
        cache[KEY_DEFAULTS_SEEDED_V3] = "true"
        NzApplication.appScope.launch {
            try {
                val d = dao(context)
                d.delete(KEY_CATEGORIES)
                d.delete(KEY_GROUPS)
                d.delete(KEY_TAGS)
                d.upsert(TaxonomyEntity(KEY_CATEGORIES, categoriesJson))
                d.upsert(TaxonomyEntity(KEY_GROUPS, groupsJson))
                d.upsert(TaxonomyEntity(KEY_TAGS, tagsJson))
                d.upsert(TaxonomyEntity(KEY_DEFAULTS_SEEDED_V3, "true"))
            } catch (e: Exception) {
                e.printStackTrace()
            }
        }
    }

    private fun ensureModeDefaults(context: Context) {
        val list = getCategories(context).toMutableList()
        var changed = false
        if (list.none { it.id == CAT_F_SELF_ID }) {
            list += CategoryDef(CAT_F_SELF_ID, "自慰", "sparkles", "pink", list.size)
            changed = true
        }
        if (list.none { it.id == CAT_PAIR_ID }) {
            list += CategoryDef(CAT_PAIR_ID, "双人", "heart-pulse", "rose", list.size)
            changed = true
        }
        if (changed) {
            writeList(context, KEY_CATEGORIES, list.sortedBy { it.sortOrder })
        }
    }

    fun getCategories(context: Context) =
        readList<CategoryDef>(context, KEY_CATEGORIES)
            .sortedBy { it.sortOrder }

    fun getGroups(context: Context) =
        readList<TagGroupDef>(context, KEY_GROUPS)
            .map { it.copy(modeKeys = it.modeKeys.orEmpty()) }
            .sortedBy { it.sortOrder }

    fun getTags(context: Context) =
        readList<TagDef>(context, KEY_TAGS)
            .map { it.copy(modeKeys = it.modeKeys.orEmpty()) }
            .sortedWith(compareBy({ it.sortOrder }, { it.name }))

    fun getCategory(context: Context, id: String): CategoryDef? =
        getCategories(context).firstOrNull { it.id == id }

    fun getGroup(context: Context, id: String): TagGroupDef? =
        getGroups(context).firstOrNull { it.id == id }

    fun getTag(context: Context, id: String): TagDef? =
        getTags(context).firstOrNull { it.id == id }

    fun findTagByName(context: Context, name: String): TagDef? =
        getTags(context).firstOrNull { it.name == name }

    fun defaultCategory(context: Context): CategoryDef =
        defaultCategoryFor(context, SessionMode.SOLO_MALE)

    /** 按记录模式返回推荐默认分类（找不到时回退到第一个分类）。 */
    fun defaultCategoryFor(context: Context, mode: SessionMode): CategoryDef {
        ensureDefaults(context)
        val list = getCategories(context)
        val preferred = when (mode) {
            SessionMode.SOLO_MALE -> DEFAULT_CATEGORY_ID
            SessionMode.SOLO_FEMALE -> CAT_F_SELF_ID
            SessionMode.PAIR -> CAT_PAIR_ID
        }
        return list.firstOrNull { it.id == preferred }
            ?: list.firstOrNull()
            ?: DEFAULT_CATEGORIES.first()
    }

    fun addGroup(
        context: Context,
        name: String,
        icon: String = "folder",
        color: String = "slate",
        modeKeys: List<String> = emptyList()
    ): TagGroupDef {
        val list = getGroups(context).toMutableList()
        val item = TagGroupDef("grp_" + uuid(), name.trim(), icon, color, list.size, modeKeys)
        list += item
        writeList(context, KEY_GROUPS, list)
        return item
    }

    fun updateGroup(
        context: Context,
        id: String,
        name: String? = null,
        icon: String? = null,
        color: String? = null,
        modeKeys: List<String>? = null
    ) {
        val list = getGroups(context).map {
            if (it.id == id) it.copy(
                name = name?.trim() ?: it.name,
                icon = icon ?: it.icon,
                color = color ?: it.color,
                modeKeys = modeKeys ?: it.modeKeys
            ) else it
        }
        writeList(context, KEY_GROUPS, list)
    }

    fun deleteGroup(context: Context, id: String): Boolean {
        val groups = getGroups(context).filterNot { it.id == id }
        val tags = getTags(context).filterNot { it.groupId == id }
        writeList(context, KEY_GROUPS, groups)
        writeList(context, KEY_TAGS, tags)
        return true
    }

    fun reorderGroups(context: Context, orderedIds: List<String>) {
        val orderMap = orderedIds.withIndex().associate { it.value to it.index }
        val list = getGroups(context).map {
            if (it.id in orderMap) it.copy(sortOrder = orderMap.getValue(it.id)) else it
        }
        writeList(context, KEY_GROUPS, list)
    }

    fun addTag(
        context: Context,
        name: String,
        groupId: String,
        icon: String = "hash",
        color: String = "slate",
        modeKeys: List<String> = emptyList()
    ): TagDef? {
        val trimmed = name.trim()
        if (trimmed.isEmpty()) return null
        val list = getTags(context).toMutableList()
        if (list.any { it.name == trimmed }) return null
        val inGroup = list.count { it.groupId == groupId }
        val item = TagDef("tag_" + uuid(), trimmed, icon, color, groupId, inGroup, modeKeys)
        list += item
        writeList(context, KEY_TAGS, list)
        return item
    }

    fun updateTag(
        context: Context,
        id: String,
        name: String? = null,
        icon: String? = null,
        color: String? = null,
        groupId: String? = null,
        modeKeys: List<String>? = null
    ) {
        val list = getTags(context).map {
            if (it.id == id) it.copy(
                name = name?.trim() ?: it.name,
                icon = icon ?: it.icon,
                color = color ?: it.color,
                groupId = groupId ?: it.groupId,
                modeKeys = modeKeys ?: it.modeKeys
            ) else it
        }
        writeList(context, KEY_TAGS, list)
    }

    fun deleteTag(context: Context, id: String) {
        writeList(context, KEY_TAGS, getTags(context).filterNot { it.id == id })
    }

    fun reorderTags(context: Context, orderedIds: List<String>) {
        val orderMap = orderedIds.withIndex().associate { it.value to it.index }
        val list = getTags(context).map {
            if (it.id in orderMap) it.copy(sortOrder = orderMap.getValue(it.id)) else it
        }
        writeList(context, KEY_TAGS, list)
    }

    fun getOrCreateTag(context: Context, groupId: String, name: String): String {
        val trimmed = name.trim()
        findTagByName(context, trimmed)?.let { return it.id }
        return (addTag(context, trimmed, groupId) ?: error("tag create failed")).id
    }

    fun mergeTaxonomy(
        context: Context,
        categories: List<CategoryDef>?,
        groups: List<TagGroupDef>?,
        tags: List<TagDef>?
    ) {
        ensureDefaults(context)
        val cats = categories.orEmpty()
        val grps = groups.orEmpty()
        val tgs = tags.orEmpty()
        if (cats.isNotEmpty()) {
            val cur = getCategories(context).toMutableList()
            for (c in cats) {
                val i = cur.indexOfFirst { it.id == c.id }
                if (i >= 0) cur[i] = c else cur += c
            }
            writeList(context, KEY_CATEGORIES, cur)
        }
        if (grps.isNotEmpty()) {
            val cur = getGroups(context).toMutableList()
            for (g in grps) {
                val i = cur.indexOfFirst { it.id == g.id }
                if (i >= 0) cur[i] = g else cur += g
            }
            writeList(context, KEY_GROUPS, cur)
        }
        if (tgs.isNotEmpty()) {
            val cur = getTags(context).toMutableList()
            for (t in tgs) {
                val i = cur.indexOfFirst { it.id == t.id }
                if (i >= 0) cur[i] = t else cur += t
            }
            writeList(context, KEY_TAGS, cur)
        }
    }

    fun migrateLegacySession(context: Context, original: Session): Session {
        val s = Session(
            timestamp = original.timestamp,
            duration = original.duration,
            remark = original.remark.orEmpty(),
            rating = original.rating,
            climax = original.climax,
            categoryId = original.categoryId.orEmpty(),
            tagIds = original.tagIds.orEmpty(),
            mode = original.mode.orEmpty().ifBlank { SessionMode.SOLO_MALE.key },
            climaxCount = original.climaxCount,
            partnerClimaxCount = original.partnerClimaxCount,
            partnerGender = original.partnerGender.orEmpty(),
            partnerName = original.partnerName.orEmpty(),
            contraception = original.contraception.orEmpty(),
            partners = original.partners.orEmpty(),
            initiator = original.initiator.orEmpty(),
            locations = original.locations.orEmpty(),
            moods = original.moods.orEmpty(),
            positions = original.positions.orEmpty(),
            toys = original.toys.orEmpty(),
            ejaculation = original.ejaculation.orEmpty(),
            location = original.location.orEmpty(),
            watchedMovie = original.watchedMovie,
            mood = original.mood.orEmpty(),
            props = original.props.orEmpty()
        )
        val existingTagIds: List<String> = s.tagIds
        val catRaw: String = s.categoryId
        val existingCategoryId: String? = catRaw.takeIf { it.isNotBlank() }
        val loc: String = s.location
        val mood: String = s.mood
        val props: String = s.props
        val remark: String = s.remark
        val watched: Boolean = s.watchedMovie

        if (existingTagIds.isNotEmpty()) {
            return s.copy(
                categoryId = existingCategoryId ?: DEFAULT_CATEGORY_ID,
                tagIds = existingTagIds,
                location = loc,
                mood = mood,
                props = props,
                remark = remark
            ).normalized()
        }
        ensureDefaults(context)
        val ids = mutableListOf<String>()
        if (loc.isNotBlank()) {
            ids += getOrCreateTag(context, LEGACY_GROUP_ENV, loc)
        }
        if (mood.isNotBlank()) {
            ids += getOrCreateTag(context, LEGACY_GROUP_STATE, mood)
        }
        if (props.isNotBlank()) {
            ids += getOrCreateTag(context, LEGACY_GROUP_TOOL, props)
        }
        if (watched) {
            ids += getOrCreateTag(context, LEGACY_GROUP_ACT, "看小电影")
        }
        return s.copy(
            categoryId = existingCategoryId ?: DEFAULT_CATEGORY_ID,
            tagIds = ids.distinct(),
            location = loc,
            mood = mood,
            props = props,
            remark = remark
        ).normalized()
    }

    private inline fun <reified T> readList(
        context: Context,
        key: String
    ): List<T> {
        val json = readRaw(context, key)
        if (json.isNullOrBlank()) return emptyList()
        val type = object : TypeToken<List<T>>() {}.type
        return gson.fromJson(json, type) ?: emptyList()
    }

    private fun <T> writeList(context: Context, key: String, list: List<T>) {
        writeRaw(context, key, gson.toJson(list))
    }

    private fun uuid(): String = UUID.randomUUID().toString().take(8)
}
