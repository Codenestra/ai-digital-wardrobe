package com.yijian.wardrobe.data

import android.content.Context
import android.graphics.Bitmap
import android.graphics.BitmapFactory
import android.net.Uri
import org.json.JSONArray
import org.json.JSONObject
import java.io.File
import java.io.FileInputStream
import java.io.FileOutputStream
import java.security.MessageDigest
import java.util.UUID
import java.util.zip.ZipEntry
import java.util.zip.ZipInputStream
import java.util.zip.ZipOutputStream

class LocalFiles(private val context: Context) {
    private val dataRoot = File(context.filesDir, "wardrobe_media").apply { mkdirs() }

    fun importImage(uri: Uri, folder: String): Pair<String, String> {
        val targetDir = File(dataRoot, folder).apply { mkdirs() }
        val target = File(targetDir, "${UUID.randomUUID()}.jpg")
        val bitmap = context.contentResolver.openInputStream(uri).use { input ->
            requireNotNull(input) { "无法读取所选图片" }
            BitmapFactory.decodeStream(input) ?: error("不支持的图片格式")
        }
        val maxSide = maxOf(bitmap.width, bitmap.height)
        val scaled = if (maxSide > 2048) {
            val ratio = 2048f / maxSide
            Bitmap.createScaledBitmap(bitmap, (bitmap.width * ratio).toInt(), (bitmap.height * ratio).toInt(), true)
        } else bitmap
        FileOutputStream(target).use { out -> check(scaled.compress(Bitmap.CompressFormat.JPEG, 92, out)) }
        if (scaled !== bitmap) scaled.recycle()
        bitmap.recycle()
        return relative(target) to sha256(target)
    }

    fun absolute(relativePath: String): File = File(context.filesDir, relativePath).canonicalFile.also {
        require(it.path.startsWith(context.filesDir.canonicalPath))
    }

    fun relative(file: File) = file.canonicalFile.relativeTo(context.filesDir.canonicalFile).invariantSeparatorsPath
    fun sha256(file: File): String = MessageDigest.getInstance("SHA-256").run {
        FileInputStream(file).use { input -> val buffer = ByteArray(8192); while (true) { val n = input.read(buffer); if (n < 0) break; update(buffer, 0, n) } }
        digest().joinToString("") { "%02x".format(it) }
    }
}

data class BackupValidation(val valid: Boolean, val message: String, val garments: Int = 0, val outfits: Int = 0, val wearLogs: Int = 0)

class BackupManager(private val context: Context, private val db: WardrobeDatabase) {
    private val files = LocalFiles(context)

    fun exportTo(uri: Uri, includeTryOn: Boolean): BackupValidation {
        val root = db.exportJson()
        val mediaPaths = linkedSetOf<String>()
        root.getJSONArray("garments").let { arr -> for (i in 0 until arr.length()) {
            val photos = arr.getJSONObject(i).optJSONArray("photos") ?: continue
            for (j in 0 until photos.length()) mediaPaths += photos.getJSONObject(j).getString("relativePath")
        } }
        root.optJSONArray("wearLogs")?.let { arr -> for (i in 0 until arr.length()) arr.getJSONObject(i).optString("photoPath").takeIf { it.isNotBlank() }?.let(mediaPaths::add) }
        if (includeTryOn) db.listTryOnJobs().mapNotNullTo(mediaPaths) { it.resultPath }
        val dataBytes = root.toString().toByteArray(Charsets.UTF_8)
        val manifestFiles = JSONArray()
        context.contentResolver.openOutputStream(uri, "w").use { raw ->
            requireNotNull(raw) { "无法创建备份文件" }
            ZipOutputStream(raw).use { zip ->
                zip.putNextEntry(ZipEntry("data.json")); zip.write(dataBytes); zip.closeEntry()
                manifestFiles.put(JSONObject().put("path", "data.json").put("sha256", digest(dataBytes)))
                mediaPaths.forEach { relative ->
                    val file = files.absolute(relative)
                    if (file.isFile) {
                        val entryName = "media/${relative.replace('\\', '/')}"
                        zip.putNextEntry(ZipEntry(entryName)); file.inputStream().use { it.copyTo(zip) }; zip.closeEntry()
                        manifestFiles.put(JSONObject().put("path", entryName).put("sha256", files.sha256(file)))
                    }
                }
                val manifest = JSONObject().put("format", "yijian-backup").put("version", 1)
                    .put("createdAt", System.currentTimeMillis()).put("includesTryOn", includeTryOn).put("files", manifestFiles)
                val bytes = manifest.toString().toByteArray(Charsets.UTF_8)
                zip.putNextEntry(ZipEntry("manifest.json")); zip.write(bytes); zip.closeEntry()
            }
        }
        return BackupValidation(true, "备份已创建，API Key 和临时上传内容未写入", root.getJSONArray("garments").length(), root.optJSONArray("outfits")?.length() ?: 0, root.optJSONArray("wearLogs")?.length() ?: 0)
    }

    fun validate(uri: Uri): BackupValidation {
        val extracted = readZip(uri, extractMedia = false)
        return extracted.validation
    }

    fun restoreReplace(uri: Uri): BackupValidation {
        val extracted = readZip(uri, extractMedia = true)
        if (!extracted.validation.valid || extracted.data == null) return extracted.validation
        db.replaceFromJson(extracted.data)
        return extracted.validation.copy(message = "恢复完成；AI Key 未恢复，需要重新配置")
    }

    private data class ReadResult(val validation: BackupValidation, val data: JSONObject?)
    private fun readZip(uri: Uri, extractMedia: Boolean): ReadResult {
        val entries = mutableMapOf<String, ByteArray>()
        context.contentResolver.openInputStream(uri).use { raw ->
            if (raw == null) return ReadResult(BackupValidation(false, "无法读取备份"), null)
            ZipInputStream(raw).use { zip ->
                while (true) {
                    val entry = zip.nextEntry ?: break
                    if (entry.isDirectory || entry.name.contains("..") || entry.name.startsWith('/')) continue
                    entries[entry.name] = zip.readBytesLimited(64 * 1024 * 1024)
                    zip.closeEntry()
                }
            }
        }
        val manifest = runCatching { JSONObject(entries["manifest.json"]!!.toString(Charsets.UTF_8)) }.getOrElse { return ReadResult(BackupValidation(false, "备份清单无效"), null) }
        if (manifest.optString("format") != "yijian-backup" || manifest.optInt("version") != 1) return ReadResult(BackupValidation(false, "不支持的备份版本"), null)
        val listed = manifest.getJSONArray("files")
        for (i in 0 until listed.length()) {
            val item = listed.getJSONObject(i); val bytes = entries[item.getString("path")]
                ?: return ReadResult(BackupValidation(false, "备份缺少 ${item.getString("path")}"), null)
            if (!digest(bytes).equals(item.getString("sha256"), true)) return ReadResult(BackupValidation(false, "校验失败：${item.getString("path")}"), null)
        }
        val data = runCatching { JSONObject(entries["data.json"]!!.toString(Charsets.UTF_8)) }.getOrElse { return ReadResult(BackupValidation(false, "数据文件无效"), null) }
        val validation = BackupValidation(true, "备份校验通过", data.getJSONArray("garments").length(), data.optJSONArray("outfits")?.length() ?: 0, data.optJSONArray("wearLogs")?.length() ?: 0)
        if (extractMedia) entries.filterKeys { it.startsWith("media/") }.forEach { (name, bytes) ->
            val relative = name.removePrefix("media/")
            val target = File(context.filesDir, relative).canonicalFile
            require(target.path.startsWith(context.filesDir.canonicalPath))
            target.parentFile?.mkdirs(); target.writeBytes(bytes)
        }
        return ReadResult(validation, data)
    }

    private fun digest(bytes: ByteArray) = MessageDigest.getInstance("SHA-256").digest(bytes).joinToString("") { "%02x".format(it) }
    private fun ZipInputStream.readBytesLimited(limit: Int): ByteArray {
        val out = java.io.ByteArrayOutputStream(); val buffer = ByteArray(8192); var total = 0
        while (true) { val n = read(buffer); if (n < 0) break; total += n; require(total <= limit) { "备份条目过大" }; out.write(buffer, 0, n) }
        return out.toByteArray()
    }
}
