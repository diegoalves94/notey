package me.study.notey.data.db.entities

import androidx.room.Entity
import androidx.room.PrimaryKey
import me.study.notey.util.Constants.IMAGE_TO_UPLOAD_TABLE

@Entity(
    tableName = IMAGE_TO_UPLOAD_TABLE
)
data class ImageToUpload(
    @PrimaryKey(autoGenerate = true)
    val id: Int = 0,
    val remoteImagePath: String,
    val imageUri: String,
    val sessionUri: String
)