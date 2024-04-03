package me.study.notey.data.repository

import kotlinx.coroutines.flow.Flow
import me.study.notey.models.Note
import me.study.notey.util.RequestState
import java.time.LocalDate

typealias Notes = RequestState<Map<LocalDate, List<Note>>>

interface MongoRepository {
    fun configureTheRealm()
    fun getAllNotes(): Flow<Notes>
}