package com.medassist.app.data

import android.content.Context
import android.graphics.Bitmap
import android.graphics.BitmapFactory
import android.net.Uri
import com.medassist.app.core.repository.UploadFile
import java.io.ByteArrayOutputStream
import java.io.File
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext

/**
 * Turns a picked or captured photo into an upload: scaled to at most 1600 px and re-encoded as
 * JPEG. A 12 MP camera photo is ~5 MB; this is ~300 KB, which matters on Indian mobile data and
 * stays far under the server's 8 MB limit, while text on a medicine strip stays readable for the AI.
 */
class PhotoReader(private val context: Context) {

    suspend fun read(uri: Uri): UploadFile? = withContext(Dispatchers.IO) {
        runCatching {
            val bounds = BitmapFactory.Options().apply { inJustDecodeBounds = true }
            context.contentResolver.openInputStream(uri)?.use { BitmapFactory.decodeStream(it, null, bounds) }
            var sample = 1
            while (maxOf(bounds.outWidth, bounds.outHeight) / (sample * 2) >= MAX_SIDE) sample *= 2
            val bitmap = context.contentResolver.openInputStream(uri)?.use {
                BitmapFactory.decodeStream(it, null, BitmapFactory.Options().apply { inSampleSize = sample })
            } ?: return@runCatching null
            val scale = MAX_SIDE.toFloat() / maxOf(bitmap.width, bitmap.height)
            val scaled = if (scale < 1f) {
                Bitmap.createScaledBitmap(bitmap, (bitmap.width * scale).toInt(), (bitmap.height * scale).toInt(), true)
            } else bitmap
            val out = ByteArrayOutputStream()
            scaled.compress(Bitmap.CompressFormat.JPEG, 85, out)
            UploadFile(out.toByteArray(), "image/jpeg", "photo.jpg")
        }.getOrNull()
    }

    /** A fresh cache file the camera app can write into (shared through the FileProvider). */
    fun newCaptureFile(): File = File(context.cacheDir, "photos").apply { mkdirs() }
        .let { File(it, "capture_${System.currentTimeMillis()}.jpg") }

    private companion object {
        const val MAX_SIDE = 1600
    }
}
