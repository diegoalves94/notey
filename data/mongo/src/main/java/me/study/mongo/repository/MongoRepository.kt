package me.study.mongo.repository

import kotlinx.coroutines.flow.Flow
import me.study.util.models.Note
import me.study.util.models.RequestState
import org.mongodb.kbson.ObjectId
import java.time.LocalDate
import java.time.ZonedDateTime

typealias Notes = RequestState<Map<LocalDate, List<Note>>>

internal interface MongoRepository {
    fun configureTheRealm()
    fun getAllNotes(): Flow<Notes>
    fun getSelectedNote(noteId: ObjectId): Flow<RequestState<Note>>
    fun getFilteredNotes(zonedDateTime: ZonedDateTime): Flow<Notes>
    suspend fun insertNote(note: Note): RequestState<Note>
    suspend fun updateNote(note: Note): RequestState<Note>
    suspend fun deleteNote(noteId: ObjectId): RequestState<Note>
    suspend fun deleteAllNotes(): RequestState<Boolean>
}