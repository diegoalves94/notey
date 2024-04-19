package me.study.mongo.db.database

import androidx.room.Database
import androidx.room.RoomDatabase
import me.study.mongo.db.dao.ImageToDeleteDao
import me.study.mongo.db.dao.ImageToUploadDao
import me.study.mongo.db.entities.ImageToDelete
import me.study.mongo.db.entities.ImageToUpload

@Database(
    entities = [ImageToUpload::class, ImageToDelete::class],
    version = 2,
    exportSchema = false
)
abstract class ImagesDatabase: RoomDatabase() {
    abstract fun imageToUploadDao(): ImageToUploadDao
    abstract fun imageToDeleteDao(): ImageToDeleteDao
}