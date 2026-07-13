package me.neko.nzhelper.core.datastore

import android.content.Context
import androidx.core.content.edit
import com.google.gson.Gson
import com.google.gson.reflect.TypeToken
import me.neko.nzhelper.NzApplication
import me.neko.nzhelper.core.model.CategoryDef
import me.neko.nzhelper.core.model.Session
import me.neko.nzhelper.core.model.TagDef
import me.neko.nzhelper.core.model.TagGroupDef
import java.util.UUID

object TagSettings {

    const val DEFAULT_CATEGORY_ID: String = Session.DEFAULT_CATEGORY_ID

    private const val PREFS_NAME = "tag_taxonomy_prefs"
    private const val KEY_CATEGORIES = "categories"
    private const val KEY_GROUPS = "groups"
    private const val KEY_TAGS = "tags"
    private const val KEY_DEFAULTS_SEEDED = "defaults_seeded_v1"

    private val gson: Gson get() = NzApplication.gson

    private val DEFAULT_CATEGORIES = listOf(
        CategoryDef("cat_self", "手冲", "hand", "rose", 0)
    )

    private val DEFAULT_GROUPS = listOf(
        TagGroupDef("grp_env", "环境", "map-pin", "emerald", 0),
        TagGroupDef("grp_time", "时间", "clock", "amber", 1),
        TagGroupDef("grp_state", "状态", "heart-pulse", "rose", 2),
        TagGroupDef("grp_act", "行为", "sparkles", "violet", 3),
        TagGroupDef("grp_tool", "道具", "wrench", "teal", 4)
    )

    private val DEFAULT_TAGS = listOf(
        // 环境
        TagDef("tag_env_bedroom", "卧室", "bed", "emerald", "grp_env", 0),
        TagDef("tag_env_bathroom", "浴室", "shower-head", "teal", "grp_env", 1),
        TagDef("tag_env_toilet", "厕所", "door-closed", "emerald", "grp_env", 2),
        TagDef("tag_env_sofa", "沙发", "sofa", "emerald", "grp_env", 3),
        TagDef("tag_env_office", "办公室", "briefcase", "emerald", "grp_env", 4),
        TagDef("tag_env_hotel", "酒店出差", "building-2", "teal", "grp_env", 5),
        // 时间
        TagDef("tag_time_morning", "上午", "sunrise", "amber", "grp_time", 0),
        TagDef("tag_time_afternoon", "下午", "sun", "orange", "grp_time", 1),
        TagDef("tag_time_evening", "晚上", "sunset", "amber", "grp_time", 2),
        TagDef("tag_time_latenight", "深夜", "moon", "amber", "grp_time", 3),
        TagDef("tag_time_dawn", "凌晨", "moon-star", "orange", "grp_time", 4),
        TagDef("tag_time_weekend", "周末", "calendar-days", "amber", "grp_time", 5),
        TagDef("tag_time_weekday", "工作日", "calendar", "orange", "grp_time", 6),
        // 状态
        TagDef("tag_state_calm", "平静", "leaf", "slate", "grp_state", 0),
        TagDef("tag_state_stress", "压力大", "brain", "rose", "grp_state", 1),
        TagDef("tag_state_happy", "开心", "smile", "pink", "grp_state", 2),
        TagDef("tag_state_excited", "兴奋", "flame", "rose", "grp_state", 3),
        TagDef("tag_state_joy", "愉悦", "party-popper", "pink", "grp_state", 4),
        TagDef("tag_state_exhausted", "疲惫", "battery-alert", "rose", "grp_state", 5),
        TagDef("tag_state_bored", "无聊", "meh", "slate", "grp_state", 6),
        TagDef("tag_state_insomnia", "失眠", "eye-off", "rose", "grp_state", 7),
        TagDef("tag_state_empty", "空虚", "cloud-fog", "slate", "grp_state", 8),
        TagDef("tag_state_sick", "生病", "thermometer", "rose", "grp_state", 9),
        // 行为
        TagDef("tag_act_porn", "看小电影", "monitor-play", "violet", "grp_act", 0),
        TagDef("tag_act_aftershower", "洗澡后", "droplets", "violet", "grp_act", 1),
        TagDef("tag_act_aftergym", "运动后", "dumbbell", "violet", "grp_act", 2),
        TagDef("tag_act_drunk", "喝酒", "wine", "violet", "grp_act", 3),
        TagDef("tag_act_bed", "赖床", "bed-double", "violet", "grp_act", 4),
        TagDef("tag_act_beforesleep", "睡前", "moon", "violet", "grp_act", 5),
        // 道具
        TagDef("tag_tool_hand", "手", "hand", "teal", "grp_tool", 0),
        TagDef("tag_tool_cup", "飞机杯", "cup-soda", "teal", "grp_tool", 1),
        TagDef("tag_tool_doll", "小胶妻", "baby", "teal", "grp_tool", 2)
    )

    /** 分组 id → 默认色（迁移旧字段时按分组归位）。 */
    val LEGACY_GROUP_ENV: String = "grp_env"
    val LEGACY_GROUP_STATE: String = "grp_state"
    val LEGACY_GROUP_TOOL: String = "grp_tool"
    val LEGACY_GROUP_ACT: String = "grp_act"

    // ===================== 初始化 =====================
    fun ensureDefaults(context: Context) {
        val prefs = context.getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE)
        if (prefs.getBoolean(KEY_DEFAULTS_SEEDED, false)) return
        prefs.edit {
            putString(KEY_CATEGORIES, gson.toJson(DEFAULT_CATEGORIES))
            putString(KEY_GROUPS, gson.toJson(DEFAULT_GROUPS))
            putString(KEY_TAGS, gson.toJson(DEFAULT_TAGS))
            putBoolean(KEY_DEFAULTS_SEEDED, true)
        }
    }

    fun getCategories(context: Context) =
        readList<CategoryDef>(context, KEY_CATEGORIES)
            .sortedBy { it.sortOrder }

    fun getGroups(context: Context) =
        readList<TagGroupDef>(context, KEY_GROUPS)
            .sortedBy { it.sortOrder }

    fun getTags(context: Context) =
        readList<TagDef>(context, KEY_TAGS)
            .sortedWith(compareBy({ it.sortOrder }, { it.name }))

    /** 分组及其下标签，按 sortOrder 排序。 */
    fun groupedTags(context: Context): List<Pair<TagGroupDef, List<TagDef>>> {
        val tags = getTags(context)
        return getGroups(context).map { g -> g to tags.filter { it.groupId == g.id } }
    }

    fun getCategory(context: Context, id: String): CategoryDef? =
        getCategories(context).firstOrNull { it.id == id }

    fun getGroup(context: Context, id: String): TagGroupDef? =
        getGroups(context).firstOrNull { it.id == id }

    fun getTag(context: Context, id: String): TagDef? =
        getTags(context).firstOrNull { it.id == id }

    /** 按 name 全局查找（name 唯一）。 */
    fun findTagByName(context: Context, name: String): TagDef? =
        getTags(context).firstOrNull { it.name == name }

    /** 默认分类（第一条 / cat_self）。 */
    fun defaultCategory(context: Context): CategoryDef {
        ensureDefaults(context)
        val list = getCategories(context)
        return list.firstOrNull { it.id == DEFAULT_CATEGORY_ID }
            ?: list.firstOrNull()
            ?: DEFAULT_CATEGORIES.first()
    }

    fun addCategory(
        context: Context,
        name: String,
        icon: String = "tag",
        color: String = "rose"
    ): CategoryDef {
        val list = getCategories(context).toMutableList()
        val item = CategoryDef("cat_" + uuid(), name.trim(), icon, color, list.size)
        list += item
        writeList(context, KEY_CATEGORIES, list)
        return item
    }

    fun updateCategory(
        context: Context,
        id: String,
        name: String? = null,
        icon: String? = null,
        color: String? = null
    ) {
        val list = getCategories(context).map {
            if (it.id == id) it.copy(
                name = name?.trim() ?: it.name,
                icon = icon ?: it.icon,
                color = color ?: it.color
            ) else it
        }
        writeList(context, KEY_CATEGORIES, list)
    }

    /** 删除分类；若仍被 Session 引用则返回 false。 */
    fun deleteCategory(context: Context, id: String): Boolean {
        val list = getCategories(context)
        if (list.size <= 1) return false // 至少保留 1 个
        writeList(context, KEY_CATEGORIES, list.filterNot { it.id == id })
        return true
    }

    fun addGroup(
        context: Context,
        name: String,
        icon: String = "folder",
        color: String = "slate"
    ): TagGroupDef {
        val list = getGroups(context).toMutableList()
        val item = TagGroupDef("grp_" + uuid(), name.trim(), icon, color, list.size)
        list += item
        writeList(context, KEY_GROUPS, list)
        return item
    }

    fun updateGroup(
        context: Context,
        id: String,
        name: String? = null,
        icon: String? = null,
        color: String? = null
    ) {
        val list = getGroups(context).map {
            if (it.id == id) it.copy(
                name = name?.trim() ?: it.name,
                icon = icon ?: it.icon,
                color = color ?: it.color
            ) else it
        }
        writeList(context, KEY_GROUPS, list)
    }

    /** 删除分组（连同其下标签一起删除）。若其下标签仍被 Session 引用则返回 false。 */
    fun deleteGroup(context: Context, id: String): Boolean {
        val groups = getGroups(context).filterNot { it.id == id }
        val tags = getTags(context).filterNot { it.groupId == id }
        writeList(context, KEY_GROUPS, groups)
        writeList(context, KEY_TAGS, tags)
        return true
    }

    fun addTag(
        context: Context,
        name: String,
        groupId: String,
        icon: String = "hash",
        color: String = "slate"
    ): TagDef? {
        val trimmed = name.trim()
        if (trimmed.isEmpty()) return null
        val list = getTags(context).toMutableList()
        if (list.any { it.name == trimmed }) return null // name 全局唯一
        val inGroup = list.count { it.groupId == groupId }
        val item = TagDef("tag_" + uuid(), trimmed, icon, color, groupId, inGroup)
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
        groupId: String? = null
    ) {
        val list = getTags(context).map {
            if (it.id == id) it.copy(
                name = name?.trim() ?: it.name,
                icon = icon ?: it.icon,
                color = color ?: it.color,
                groupId = groupId ?: it.groupId
            ) else it
        }
        writeList(context, KEY_TAGS, list)
    }

    fun deleteTag(context: Context, id: String) {
        writeList(context, KEY_TAGS, getTags(context).filterNot { it.id == id })
    }

    /**
     * 按 name 查找标签；找不到则在指定分组下创建（用于旧数据迁移）。
     * 返回标签 id。
     */
    fun getOrCreateTag(context: Context, groupId: String, name: String): String {
        val trimmed = name.trim()
        findTagByName(context, trimmed)?.let { return it.id }
        return (addTag(context, trimmed, groupId) ?: error("tag create failed")).id
    }

    /** 合并外部分类法（备份恢复用）：按 id upsert，不破坏既有项。 */
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

    // ===================== 旧数据迁移 =====================
    /**
     * 把旧 Session 的 location/mood/props/watchedMovie 字段迁移为 tagIds
     * - 已有 tagIds 的直接返回
     * - 缺失的标签会自动在对应分组下创建
     */
    fun migrateLegacySession(context: Context, s: Session): Session {
        val existingTagIds: List<String> = s.tagIds.orEmpty()
        val catRaw: String = s.categoryId.orEmpty()
        val existingCategoryId: String? = catRaw.takeIf { it.isNotBlank() }
        val loc: String = s.location.orEmpty()
        val mood: String = s.mood.orEmpty()
        val props: String = s.props.orEmpty()
        val remark: String = s.remark.orEmpty()
        val watched: Boolean = s.watchedMovie

        if (existingTagIds.isNotEmpty()) {
            return s.copy(
                categoryId = existingCategoryId ?: DEFAULT_CATEGORY_ID,
                tagIds = existingTagIds,
                location = loc,
                mood = mood,
                props = props,
                remark = remark
            )
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
        )
    }

    private inline fun <reified T> readList(
        context: Context,
        key: String
    ): List<T> {
        val json = context.getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE)
            .getString(key, null)

        if (json.isNullOrBlank()) return emptyList()
        val type = object : TypeToken<List<T>>() {}.type
        return gson.fromJson(json, type) ?: emptyList()
    }

    private fun <T> writeList(context: Context, key: String, list: List<T>) {
        context.getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE)
            .edit { putString(key, gson.toJson(list)) }
    }

    private fun uuid(): String = UUID.randomUUID().toString().take(8)
}
