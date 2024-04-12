package me.study.notey.data.repository

import io.realm.kotlin.Realm
import io.realm.kotlin.ext.query
import io.realm.kotlin.log.LogLevel
import io.realm.kotlin.mongodb.App
import io.realm.kotlin.mongodb.sync.SyncConfiguration
import io.realm.kotlin.query.Sort
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.flow
import kotlinx.coroutines.flow.map
import me.study.notey.models.Note
import me.study.notey.models.RequestState
import me.study.notey.util.Constants.APP_ID
import me.study.notey.util.toInstant
import org.mongodb.kbson.ObjectId
import java.time.ZoneId

object MongoDb : MongoRepository {

    private val app = App.create(APP_ID)
    private val user = app.currentUser
    private lateinit var realm: Realm

    init {
        configureTheRealm()
    }

    override fun configureTheRealm() {
        if (user != null) {
            val config = SyncConfiguration.Builder(
                user,
                setOf(Note::class)
            ).initialSubscriptions { sub ->
                add(
                    query = sub.query<Note>("ownerId == $0", user.id),
                    name = "User's Notes"
                )
            }
                .log(LogLevel.ALL)
                .build()

            realm = Realm.open(config)
        }
    }

    override fun getAllNotes(): Flow<Notes> {
        return if (user != null) {
            try {
                realm.query<Note>(query = "ownerId == $0", user.id)
                    .sort(property = "date", sortOrder = Sort.DESCENDING)
                    .asFlow()
                    .map { result ->
                        RequestState.Success(
                            data = result.list.groupBy {
                                it.date.toInstant()
                                    .atZone(ZoneId.systemDefault())
                                    .toLocalDate()
                            }
                        )
                    }
            } catch (e: Exception) {
                flow {
                    emit(RequestState.Error(e))
                }
            }
        } else {
            flow {
                emit(RequestState.Error(UserNotAuthenticatedException()))
            }
        }
    }

    override fun getSelectedNote(noteId: ObjectId): Flow<RequestState<Note>> {
        return if (user != null) {
            try {
                realm.query<Note>(
                    query = "_id == $0", noteId
                ).asFlow().map {
                    RequestState.Success(data = it.list.first())
                }
            } catch (e: Exception) {
                flow {
                    emit(RequestState.Error(e))
                }
            }
        } else {
            flow {
                emit(RequestState.Error(UserNotAuthenticatedException()))
            }
        }
    }

    override suspend fun insertNote(note: Note): RequestState<Note> {
        return if (user != null) {
            realm.write {
                try {
                    val addedNote = copyToRealm(
                        note.apply {
                            ownerId = user.id
                        }
                    )
                    RequestState.Success(addedNote)
                } catch (e: Exception) {
                    RequestState.Error(e)
                }
            }
        } else {
            RequestState.Error(UserNotAuthenticatedException())
        }
    }

    override suspend fun updateNote(note: Note): RequestState<Note> {
        return if (user != null) {
            realm.write {
                val queriedNote = query<Note>(
                    query = "_id == $0", note._id
                ).first().find()

                if (queriedNote != null) {
                    queriedNote.title = note.title
                    queriedNote.description = note.description
                    queriedNote.mood = note.mood
                    queriedNote.images = note.images
                    queriedNote.date = note.date

                    RequestState.Success(
                        data = queriedNote
                    )
                } else {
                    RequestState.Error(
                        error = Exception("This Note does not exist!")
                    )
                }
            }
        } else {
            RequestState.Error(UserNotAuthenticatedException())
        }
    }

    override suspend fun deleteNote(noteId: ObjectId): RequestState<Note> {
        return if (user != null) {
            realm.write {
                val note = query<Note>(
                    query = "_id == $0 AND ownerId == $1", noteId, user.id
                ).first().find()
                if (note != null) {
                    try {

                        delete(note)
                        RequestState.Success(data = note)
                    } catch (e: Exception) {
                        RequestState.Error(e)
                    }
                } else {
                    RequestState.Error(Exception("Note does note exist."))
                }
            }
        } else {
            RequestState.Error(UserNotAuthenticatedException())
        }
    }
}

private class UserNotAuthenticatedException : Exception("User is not Logged in!")