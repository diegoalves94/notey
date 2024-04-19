package me.study.ui

import android.net.Uri

/**
 * A class that represents a single Image within a Gallery.
 * @param image The image URI inside a gallery.
 * @param remoteImagePath The path of the [image] where you plan to upload it.
 * */
data class GalleryImage(
    val image: Uri,
    val remoteImagePath: String = ""
)