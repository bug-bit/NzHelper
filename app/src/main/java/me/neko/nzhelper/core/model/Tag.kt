package me.neko.nzhelper.core.model

import androidx.compose.runtime.Immutable

@Immutable
/// 顶层分类
data class CategoryDef(
    val id: String,
    val name: String,
    val icon: String = "tag",
    val color: String = "rose",
    val sortOrder: Int = 0
)

@Immutable
data class TagGroupDef(
    val id: String,
    val name: String,
    val icon: String = "folder",
    val color: String = "slate",
    val sortOrder: Int = 0,
    val modeKeys: List<String> = emptyList()
) {
    /** 该分组是否适用于指定记录模式。 */
    fun appliesTo(mode: SessionMode): Boolean =
        modeKeys.isEmpty() || mode.key in modeKeys
}

@Immutable
/// 单个标签 | 归属于某个分组 | name 全局唯一
/// icon / color 为展示属性（参见 ui/theme/TagColors、TagIcons）
data class TagDef(
    val id: String,
    val name: String,
    val icon: String = "hash",
    val color: String = "slate",
    val groupId: String,
    val sortOrder: Int = 0,
    val modeKeys: List<String> = emptyList()
) {
    /** 该标签是否适用于指定记录模式。 */
    fun appliesTo(mode: SessionMode): Boolean =
        modeKeys.isEmpty() || mode.key in modeKeys
}
