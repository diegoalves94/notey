package me.study.notey.presentation.screens.write

import android.net.Uri
import android.util.Log
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.setValue
import androidx.lifecycle.SavedStateHandle
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.auth.ktx.auth
import com.google.firebase.ktx.Firebase
import com.google.firebase.storage.FirebaseStorage
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.catch
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import me.study.notey.data.db.dao.ImageToDeleteDao
import me.study.notey.data.db.dao.ImageToUploadDao
import me.study.notey.data.db.entities.ImageToDelete
import me.study.notey.data.db.entities.ImageToUpload
import me.study.notey.data.repository.MongoDb
import me.study.notey.models.GalleryImage
import me.study.notey.models.GalleryState
import me.study.notey.models.Mood
import me.study.notey.models.Note
import me.study.notey.models.RequestState
import me.study.notey.models.UiState
import me.study.notey.util.Constants.WRITE_SCREEN_ARGUMENT_KEY
import me.study.notey.util.fetchImagesFromFirebase
import me.study.notey.util.toRealmInstant
import org.mongodb.kbson.ObjectId
import java.time.ZonedDateTime
import javax.inject.Inject

@HiltViewModel
class WriteViewModel @Inject constructor(
    private val savedStateHandle: SavedStateHandle,
    private val imagesToUploadDao: ImageToUploadDao,
    private val imagesToDeleteDao: ImageToDeleteDao
) : ViewModel() {

    val galleryState = GalleryState()

    var uiState by mutableStateOf(UiState())
        private set

    init {
        getNoteIdArg()
        fetchSelectedNote()
    }

    private fun getNoteIdArg() {
        uiState = uiState.copy(
            selectedNoteId = savedStateHandle.get<String>(
                key = WRITE_SCREEN_ARGUMENT_KEY
            )
        )
    }

    private fun fetchSelectedNote() {
        if (uiState.selectedNoteId != null) {
            viewModelScope.launch(Dispatchers.Main) {
                MongoDb.getSelectedNote(
                    noteId = ObjectId.invoke(uiState.selectedNoteId!!)
                ).catch {
                    emit(RequestState.Error(Exception("Note already deleted")))
                }
                    .collect { note ->
                        if (note is RequestState.Success) {
                            setSelectedNote(note = note.data)
                            setTitle(title = note.data.title)
                            setDescription(description = note.data.description)
                            setMood(mood = Mood.valueOf(note.data.mood))

                            fetchImagesFromFirebase(
                                remoteImagePaths = note.data.images,
                                onImageDownload = { downloadedImage ->
                                    galleryState.addImage(
                                        GalleryImage(
                                            image = downloadedImage,
                                            remoteImagePath = extractImagePath(
                                                remotePath = downloadedImage.toString()
                                            )
                                        )
                                    )
                                }
                            )
                        }
                    }
            }
        }
    }

    private fun setSelectedNote(note: Note) {
        uiState = uiState.copy(
            selectedNote = note
        )
    }

    fun setTitle(title: String) {
        uiState = uiState.copy(
            title = title
        )
    }

    fun setDescription(description: String) {
        uiState = uiState.copy(
            description = description
        )
    }

    private fun setMood(mood: Mood) {
        uiState = uiState.copy(
            mood = mood
        )
    }

    fun updateDateTime(zonedDateTime: ZonedDateTime) {
        uiState = uiState.copy(updatedDateTime = zonedDateTime.toInstant().toRealmInstant())
    }

    fun upsertNote(note: Note, onSuccess: () -> Unit, onError: (String) -> Unit) {
        viewModelScope.launch(Dispatchers.IO) {
            if (uiState.selectedNoteId != null) {
                updateNote(
                    note = note,
                    onSuccess = onSuccess,
                    onError = onError
                )
            } else {
                insertNote(
                    note = note,
                    onSuccess = onSuccess,
                    onError = onError
                )
            }
        }
    }

    private suspend fun insertNote(note: Note, onSuccess: () -> Unit, onError: (String) -> Unit) {
        val result = MongoDb.insertNote(note = note.apply {
            if (uiState.updatedDateTime != null) {
                date = uiState.updatedDateTime!!
            }
        })
        if (result is RequestState.Success) {
            uploadImagesToFirebase()
            withContext(Dispatchers.Main) {
                onSuccess()
            }
        } else if (result is RequestState.Error) {
            withContext(Dispatchers.Main) {
                onError(result.error.message.toString())
            }
        }
    }

    private suspend fun updateNote(note: Note, onSuccess: () -> Unit, onError: (String) -> Unit) {
        val result = MongoDb.updateNote(note.apply {
            _id = ObjectId.invoke(uiState.selectedNoteId!!)
            date = if (uiState.updatedDateTime != null) {
                uiState.updatedDateTime!!
            } else {
                uiState.selectedNote!!.date
            }
        })
        if (result is RequestState.Success) {
            uploadImagesToFirebase()
            deleteImagesToFirebase()
            withContext(Dispatchers.Main) {
                onSuccess()
            }
        } else if (result is RequestState.Error) {
            withContext(Dispatchers.Main) {
                onError(result.error.message.toString())
            }
        }
    }

    fun deleteNote(onSuccess: () -> Unit, onError: (String) -> Unit) {
        viewModelScope.launch(Dispatchers.IO) {
            if (uiState.selectedNoteId != null) {
                val result = MongoDb.deleteNote(
                    noteId = ObjectId.invoke(uiState.selectedNoteId!!)
                )
                if (result is RequestState.Success) {
                    withContext(Dispatchers.Main) {
                        uiState.selectedNote?.let {
                            deleteImagesToFirebase(images = it.images)
                        }
                        onSuccess()
                    }
                } else if (result is RequestState.Error) {
                    withContext(Dispatchers.Main) {
                        onError(result.error.message.toString())
                    }
                }

            }
        }
    }

    fun addImage(image: Uri, imageType: String) {
        val remoteImagePath = "images/${FirebaseAuth.getInstance().currentUser?.uid}/" +
                "${image.lastPathSegment}-${System.currentTimeMillis()}.$imageType"

        Log.d("WriteViewModel", remoteImagePath)

        galleryState.addImage(
            GalleryImage(
                image = image,
                remoteImagePath = remoteImagePath
            )
        )
    }

    private fun uploadImagesToFirebase() {
        val storage = FirebaseStorage.getInstance().reference
        galleryState.images.forEach { galleryImage ->
            val imagePath = storage.child(galleryImage.remoteImagePath)
            imagePath.putFile(galleryImage.image)
                .addOnProgressListener {
                    val sessionUri = it.uploadSessionUri
                    if (sessionUri != null) {
                        viewModelScope.launch(Dispatchers.IO) {
                            imagesToUploadDao.addImageToUpload(
                                ImageToUpload(
                                    remoteImagePath = galleryImage.remoteImagePath,
                                    imageUri = galleryImage.image.toString(),
                                    sessionUri = sessionUri.toString()
                                )
                            )
                        }
                    }
                }
        }
    }

    private fun deleteImagesToFirebase(images: List<String>? = null) {
        val storage = FirebaseStorage.getInstance().reference
        if (images != null) {
            images.forEach { remotePath ->
                storage.child(remotePath).delete()
                    .addOnFailureListener {
                        viewModelScope.launch(Dispatchers.IO) {
                            imagesToDeleteDao.addImageToDelete(
                                ImageToDelete(
                                    remoteImagePath = remotePath
                                )
                            )
                        }
                    }
            }
        } else {
            galleryState.imagesToBeDeleted.map {
                it.remoteImagePath
            }.forEach { remotePath ->
                storage.child(remotePath).delete()
                    .addOnFailureListener {
                        viewModelScope.launch(Dispatchers.IO) {
                            imagesToDeleteDao.addImageToDelete(
                                ImageToDelete(
                                    remoteImagePath = remotePath
                                )
                            )
                        }
                    }
            }
        }
    }

    private fun extractImagePath(remotePath: String): String {
        val chunks = remotePath.split("%2F")
        val imageName = chunks[2].split("?").first()
        return "images/${Firebase.auth.currentUser?.uid}/$imageName"
    }
}