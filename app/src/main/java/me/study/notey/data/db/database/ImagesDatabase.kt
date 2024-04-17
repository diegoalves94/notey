package me.study.notey.data.db.database

import androidx.room.Database
import androidx.room.RoomDatabase
import me.study.notey.data.db.dao.ImageToDeleteDao
import me.study.notey.data.db.dao.ImageToUploadDao
import me.study.notey.data.db.entities.ImageToDelete
import me.study.notey.data.db.entities.ImageToUpload

@Database(
    entities = [ImageToUpload::class, ImageToDelete::class],
    version = 2,
    exportSchema = false
)
abstract class ImagesDatabase: RoomDatabase() {
    abstract fun imageToUploadDao(): ImageToUploadDao
    abstract fun imageToDeleteDao(): ImageToDeleteDao
}