package me.study.notey.models

import io.realm.kotlin.types.RealmInstant

data class UiState(
    val selectedNoteId: String? = null,
    val selectedNote: Note? = null,
    val title: String = "",
    val description: String = "",
    val mood: Mood = Mood.Neutral,
    val updatedDateTime: RealmInstant? = null
)