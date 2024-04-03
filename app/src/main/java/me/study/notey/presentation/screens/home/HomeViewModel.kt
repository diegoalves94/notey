package me.study.notey.presentation.screens.home

import androidx.compose.runtime.MutableState
import androidx.compose.runtime.mutableStateOf
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import kotlinx.coroutines.launch
import me.study.notey.data.repository.MongoDb
import me.study.notey.data.repository.Notes
import me.study.notey.util.RequestState

class HomeViewModel : ViewModel() {

    var notes: MutableState<Notes> = mutableStateOf(RequestState.Idle)

    init {
        observeAllNotes()
    }

    private fun observeAllNotes() {
        viewModelScope.launch {
            MongoDb.getAllNotes().collect { result ->
                notes.value = result
            }
        }
    }
}