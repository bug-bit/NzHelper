package me.neko.nzhelper.core.database

import com.google.gson.Gson
import com.google.gson.reflect.TypeToken
import me.neko.nzhelper.core.database.entity.RecycleBinEntity
import me.neko.nzhelper.core.database.entity.SessionEntity
import me.neko.nzhelper.core.model.RecycleBinItem
import me.neko.nzhelper.core.model.Session
import java.time.LocalDateTime
import java.time.format.DateTimeFormatter

internal object Mappers {

    private val tagIdsType = object : TypeToken<List<String>>() {}.type

    fun sessionToEntity(s: Session, gson: Gson): SessionEntity = SessionEntity(
        timestampIso = s.timestamp.format(DateTimeFormatter.ISO_LOCAL_DATE_TIME),
        duration = s.duration,
        remark = s.remark,
        rating = s.rating,
        climax = s.climax,
        categoryId = s.categoryId,
        tagIdsJson = gson.toJson(s.tagIds, tagIdsType),
        location = s.location,
        watchedMovie = s.watchedMovie,
        mood = s.mood,
        props = s.props
    )

    fun entityToSession(e: SessionEntity, gson: Gson): Session = Session(
        timestamp = LocalDateTime.parse(e.timestampIso, DateTimeFormatter.ISO_LOCAL_DATE_TIME),
        duration = e.duration,
        remark = e.remark,
        rating = e.rating,
        climax = e.climax,
        categoryId = e.categoryId,
        tagIds = gson.fromJson<List<String>>(e.tagIdsJson, tagIdsType) ?: emptyList(),
        location = e.location,
        watchedMovie = e.watchedMovie,
        mood = e.mood,
        props = e.props
    )

    fun itemToEntity(item: RecycleBinItem, gson: Gson): RecycleBinEntity = RecycleBinEntity(
        deletedTimestamp = item.deletedTimestamp,
        sessionTimestampIso = item.session.timestamp.format(DateTimeFormatter.ISO_LOCAL_DATE_TIME),
        sessionJson = gson.toJson(item.session)
    )

    fun entityToItem(e: RecycleBinEntity, gson: Gson): RecycleBinItem = RecycleBinItem(
        session = gson.fromJson(e.sessionJson, Session::class.java),
        deletedTimestamp = e.deletedTimestamp
    )

    fun sessionKey(s: Session): String =
        s.timestamp.format(DateTimeFormatter.ISO_LOCAL_DATE_TIME)
}
