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

    private val stringListType = object : TypeToken<List<String>>() {}.type

    fun sessionToEntity(s: Session, gson: Gson): SessionEntity = SessionEntity(
        timestampIso = s.timestamp.format(DateTimeFormatter.ISO_LOCAL_DATE_TIME),
        duration = s.duration,
        remark = s.remark,
        rating = s.rating,
        climax = false,
        categoryId = s.categoryId,
        tagIdsJson = gson.toJson(s.tagIds, stringListType),
        mode = s.mode,
        climaxCount = s.climaxCount,
        partnerClimaxCount = s.partnerClimaxCount,
        partnerGender = s.partnerGender,
        partnerName = s.partnerName,
        contraception = s.contraception,
        partnersJson = gson.toJson(s.partners, stringListType),
        initiator = s.initiator,
        moodsJson = gson.toJson(s.moods, stringListType),
        positionsJson = gson.toJson(s.positions, stringListType),
        toysJson = gson.toJson(s.toys, stringListType),
        ejaculation = s.ejaculation,
        locationsJson = gson.toJson(s.locations, stringListType),
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
        tagIds = gson.fromJson<List<String>>(e.tagIdsJson, stringListType) ?: emptyList(),
        mode = e.mode,
        climaxCount = e.climaxCount,
        partnerClimaxCount = e.partnerClimaxCount,
        partnerGender = e.partnerGender,
        partnerName = e.partnerName,
        contraception = e.contraception,
        partners = gson.fromJson<List<String>>(e.partnersJson, stringListType) ?: emptyList(),
        initiator = e.initiator,
        moods = gson.fromJson<List<String>>(e.moodsJson, stringListType) ?: emptyList(),
        positions = gson.fromJson<List<String>>(e.positionsJson, stringListType) ?: emptyList(),
        toys = gson.fromJson<List<String>>(e.toysJson, stringListType) ?: emptyList(),
        ejaculation = e.ejaculation,
        locations = gson.fromJson<List<String>>(e.locationsJson, stringListType) ?: emptyList(),
        location = e.location,
        watchedMovie = e.watchedMovie,
        mood = e.mood,
        props = e.props
    ).normalized()

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
