package com.shidan.app

import android.content.Context
import android.graphics.Bitmap
import android.graphics.BitmapFactory
import android.graphics.Matrix
import android.net.Uri
import androidx.core.content.FileProvider
import androidx.exifinterface.media.ExifInterface
import java.io.File
import java.io.FileOutputStream
import java.io.InputStream
import java.util.UUID
import kotlin.math.max
import kotlin.math.roundToInt

/**
 * 照片都放在 app 私有目录 filesDir/photos/，不进系统相册。
 * 好处是不用申请任何存储权限，卸载 app 时一起干净带走。
 *
 * 每张进来都压到长边 1600、JPEG 80%，一张一般 150–300KB。
 * 原图 4MB 拍进来变几百 K，肉眼看不出差别，但手机上能存几千张。
 */
object Photos {

    private const val MAX_SIDE = 1600
    private const val QUALITY = 80

    fun dir(context: Context): File =
        File(context.filesDir, "photos").apply { if (!exists()) mkdirs() }

    fun file(context: Context, name: String): File = File(dir(context), name)

    /** 相机要写进去的目标文件，返回 (文件名, 给相机的 Uri) */
    fun newCaptureTarget(context: Context): Pair<String, Uri> {
        val name = "${UUID.randomUUID()}.jpg"
        val f = file(context, name)
        val uri = FileProvider.getUriForFile(
            context, "${context.packageName}.fileprovider", f
        )
        return name to uri
    }

    /**
     * 相机拍完是原图，就地压一遍。
     * 失败就把文件删掉并返回 false，免得列表里挂一张打不开的图。
     */
    fun finishCapture(context: Context, name: String): Boolean {
        val f = file(context, name)
        if (!f.exists() || f.length() == 0L) {
            f.delete()
            return false
        }
        return try {
            val rotation = readRotation { f.inputStream() }
            val bmp = decodeScaled({ f.inputStream() }, MAX_SIDE) ?: run {
                f.delete(); return false
            }
            val fixed = applyRotation(bmp, rotation)
            writeJpeg(fixed, f)
            if (fixed !== bmp) bmp.recycle()
            fixed.recycle()
            true
        } catch (e: Exception) {
            f.delete()
            false
        }
    }

    /** 从相册选的图，复制一份压缩版进来，返回文件名 */
    fun importFrom(context: Context, uri: Uri): String? {
        return try {
            val opener = { context.contentResolver.openInputStream(uri) }
            val rotation = readRotation(opener)
            val bmp = decodeScaled(opener, MAX_SIDE) ?: return null
            val fixed = applyRotation(bmp, rotation)
            val name = "${UUID.randomUUID()}.jpg"
            writeJpeg(fixed, file(context, name))
            if (fixed !== bmp) bmp.recycle()
            fixed.recycle()
            name
        } catch (e: Exception) {
            null
        }
    }

    /** 显示用：按需要的尺寸解码，列表里的小图不会把整张大图读进内存 */
    fun load(context: Context, name: String, maxSide: Int): Bitmap? {
        val f = file(context, name)
        if (!f.exists()) return null
        return try {
            decodeScaled({ f.inputStream() }, maxSide)
        } catch (e: Exception) {
            null
        }
    }

    fun delete(context: Context, name: String) {
        try {
            file(context, name).delete()
        } catch (ignored: Exception) {
        }
    }

    // ---------- 内部 ----------

    private fun writeJpeg(bmp: Bitmap, out: File) {
        FileOutputStream(out).use { bmp.compress(Bitmap.CompressFormat.JPEG, QUALITY, it) }
    }

    private fun readRotation(open: () -> InputStream?): Int {
        return try {
            open()?.use { stream ->
                when (ExifInterface(stream).getAttributeInt(
                    ExifInterface.TAG_ORIENTATION, ExifInterface.ORIENTATION_NORMAL
                )) {
                    ExifInterface.ORIENTATION_ROTATE_90 -> 90
                    ExifInterface.ORIENTATION_ROTATE_180 -> 180
                    ExifInterface.ORIENTATION_ROTATE_270 -> 270
                    else -> 0
                }
            } ?: 0
        } catch (e: Exception) {
            0
        }
    }

    private fun applyRotation(bmp: Bitmap, degrees: Int): Bitmap {
        if (degrees == 0) return bmp
        return try {
            val m = Matrix().apply { postRotate(degrees.toFloat()) }
            Bitmap.createBitmap(bmp, 0, 0, bmp.width, bmp.height, m, true)
        } catch (e: OutOfMemoryError) {
            bmp
        }
    }

    /** 先读尺寸算采样率，再解码，最后精确缩放到目标边长 */
    private fun decodeScaled(open: () -> InputStream?, maxSide: Int): Bitmap? {
        val bounds = BitmapFactory.Options().apply { inJustDecodeBounds = true }
        open()?.use { BitmapFactory.decodeStream(it, null, bounds) }
        if (bounds.outWidth <= 0 || bounds.outHeight <= 0) return null

        var sample = 1
        while (max(bounds.outWidth, bounds.outHeight) / (sample * 2) >= maxSide) sample *= 2

        val opts = BitmapFactory.Options().apply { inSampleSize = sample }
        val raw = open()?.use { BitmapFactory.decodeStream(it, null, opts) } ?: return null

        val longest = max(raw.width, raw.height)
        if (longest <= maxSide) return raw

        val scale = maxSide.toFloat() / longest
        val w = (raw.width * scale).roundToInt().coerceAtLeast(1)
        val h = (raw.height * scale).roundToInt().coerceAtLeast(1)
        return try {
            val scaled = Bitmap.createScaledBitmap(raw, w, h, true)
            if (scaled !== raw) raw.recycle()
            scaled
        } catch (e: OutOfMemoryError) {
            raw
        }
    }
}
