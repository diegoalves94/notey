package me.study.notey.data.repository

import kotlinx.coroutines.flow.Flow
import me.study.notey.models.Note
import me.study.notey.models.RequestState
import org.mongodb.kbson.ObjectId
import java.time.LocalDate

typealias Notes = RequestState<Map<LocalDate, List<Note>>>

interface MongoRepository {
    fun configureTheRealm()
    fun getAllNotes(): Flow<Notes>
    fun getSelectedNote(noteId: ObjectId): Flow<RequestState<Note>>
    suspend fun insertNote(note: Note): RequestState<Note>
    suspend fun updateNote(note: Note): RequestState<Note>
    suspend fun deleteNote(noteId: ObjectId): RequestState<Note>
}