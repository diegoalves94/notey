package me.study.notey.presentation.screens.home

import androidx.compose.runtime.MutableState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.setValue
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.storage.FirebaseStorage
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.Job
import kotlinx.coroutines.cancelAndJoin
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import me.study.notey.connectivity.ConnectivityObserver
import me.study.notey.connectivity.NetworkConnectivityObserver
import me.study.notey.data.db.dao.ImageToDeleteDao
import me.study.notey.data.db.entities.ImageToDelete
import me.study.notey.data.repository.MongoDb
import me.study.notey.data.repository.Notes
import me.study.notey.models.RequestState
import java.time.ZonedDateTime
import javax.inject.Inject

@HiltViewModel
class HomeViewModel @Inject constructor(
    private val connectivity: NetworkConnectivityObserver,
    private val imageToDeleteDao: ImageToDeleteDao
) : ViewModel() {

    private lateinit var allNotesJob: Job
    private lateinit var filteredNotesJob: Job

    private var network by mutableStateOf(ConnectivityObserver.Status.Unavailable)
    var notes: MutableState<Notes> = mutableStateOf(RequestState.Idle)
    var dateIsSelected by mutableStateOf(false)
        private set

    init {
        getNotes()

        viewModelScope.launch {
            connectivity.observe().collect {
                network = it
            }
        }
    }

    fun getNotes(zonedDateTime: ZonedDateTime? = null) {
        dateIsSelected = zonedDateTime != null
        notes.value = RequestState.Loading

        if (dateIsSelected && zonedDateTime != null) {
            observeFilteredNotes(zonedDateTime)
        } else {
            observeAllNotes()
        }
    }

    private fun observeAllNotes() {
        allNotesJob = viewModelScope.launch {
            if (::filteredNotesJob.isInitialized) {
                filteredNotesJob.cancelAndJoin()
            }
            MongoDb.getAllNotes().collect { result ->
                notes.value = result
            }
        }
    }

    private fun observeFilteredNotes(zonedDateTime: ZonedDateTime?) {
        filteredNotesJob = viewModelScope.launch {
            if (::allNotesJob.isInitialized) {
                allNotesJob.cancelAndJoin()
            }
            MongoDb.getFilteredNotes(zonedDateTime!!).collect { result ->
                notes.value = result
            }
        }
    }

    fun deleteAllNotes(
        onSuccess: () -> Unit,
        onError: (Throwable) -> Unit
    ) {
        if (network == ConnectivityObserver.Status.Available) {
            val userId = FirebaseAuth.getInstance().currentUser?.uid
            val imagesDirectory = "images/${userId}"
            val storage = FirebaseStorage.getInstance().reference
            storage.child(imagesDirectory)
                .listAll()
                .addOnSuccessListener {
                    it.items.forEach { ref ->
                        val imagePath = "images/${userId}/${ref.name}"
                        storage.child(imagePath).delete()
                            .addOnFailureListener {
                                viewModelScope.launch(Dispatchers.IO) {
                                    imageToDeleteDao.addImageToDelete(
                                        ImageToDelete(
                                            remoteImagePath = imagePath
                                        )
                                    )
                                }
                            }
                    }
                    viewModelScope.launch(Dispatchers.IO) {
                        val result = MongoDb.deleteAllNotes()
                        if (result is RequestState.Success) {
                            withContext(Dispatchers.Main) {
                                onSuccess()
                            }
                        } else if (result is RequestState.Error) {
                            withContext(Dispatchers.Main) {
                                onError(result.error)
                            }
                        }
                    }
                }
                .addOnFailureListener {
                    onError(it)
                }
        } else {
            onError(Exception("No Internet Connection"))
        }
    }

}