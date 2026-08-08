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
/// 标签分组 环境 / 时间 / 状态 / 行为 / 道具等
data class TagGroupDef(
    val id: String,
    val name: String,
    val icon: String = "folder",
    val color: String = "slate",
    val sortOrder: Int = 0
)

@Immutable
/// 单个标签 | 归属于某个分组 | name 全局唯一
/// icon / color 为展示属性（参见 ui/theme/TagColors、TagIcons）
data class TagDef(
    val id: String,
    val name: String,
    val icon: String = "hash",
    val color: String = "slate",
    val groupId: String,
    val sortOrder: Int = 0
)
