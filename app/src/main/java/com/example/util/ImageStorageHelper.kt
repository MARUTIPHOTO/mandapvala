package com.example.util

import android.content.Context
import android.net.Uri
import java.io.File
import java.io.FileOutputStream
import java.util.UUID

object ImageStorageHelper {

    fun saveUriToInternalStorage(context: Context, uri: Uri): String {
        return try {
            val photosDir = File(context.filesDir, "gallery_images")
            if (!photosDir.exists()) {
                photosDir.mkdirs()
            }
            val fileName = "mandap_img_${System.currentTimeMillis()}_${UUID.randomUUID().toString().take(6)}.jpg"
            val destFile = File(photosDir, fileName)

            context.contentResolver.openInputStream(uri)?.use { input ->
                FileOutputStream(destFile).use { output ->
                    input.copyTo(output)
                }
            }
            destFile.absolutePath
        } catch (e: Exception) {
            e.printStackTrace()
            uri.toString()
        }
    }

    fun saveMultipleUris(context: Context, uris: List<Uri>): List<String> {
        return uris.map { uri ->
            saveUriToInternalStorage(context, uri)
        }
    }
}
