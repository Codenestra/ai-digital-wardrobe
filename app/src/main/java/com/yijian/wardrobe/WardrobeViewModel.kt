package com.yijian.wardrobe

import android.app.Application
import android.app.NotificationChannel
import android.app.NotificationManager
import android.net.Uri
import androidx.lifecycle.AndroidViewModel
import androidx.core.app.NotificationCompat
import androidx.lifecycle.viewModelScope
import com.yijian.wardrobe.data.AiCapability
import com.yijian.wardrobe.data.AiProvider
import com.yijian.wardrobe.data.AiResult
import com.yijian.wardrobe.data.BackupValidation
import com.yijian.wardrobe.data.Garment
import com.yijian.wardrobe.data.GarmentPhoto
import com.yijian.wardrobe.data.GarmentStatus
import com.yijian.wardrobe.data.Outfit
import com.yijian.wardrobe.data.OutfitItem
import com.yijian.wardrobe.data.SecureSecrets
import com.yijian.wardrobe.data.TryOnJob
import com.yijian.wardrobe.data.WearLog
import com.yijian.wardrobe.network.AiPreflight
import com.yijian.wardrobe.network.safeError
import com.yijian.wardrobe.worker.TryOnWorker
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import org.json.JSONObject

data class UiSnapshot(
    val garments: List<Garment> = emptyList(),
    val outfits: List<Outfit> = emptyList(),
    val wearLogs: List<WearLog> = emptyList(),
    val tryOnJobs: List<TryOnJob> = emptyList(),
    val busy: Boolean = false,
    val message: String? = null,
    val revision: Long = 0L
)

class WardrobeViewModel(application: Application) : AndroidViewModel(application) {
    private val app = application as YiJianApplication
    private val preflight = AiPreflight(app, app.secrets, app.aiSettings)
    private val _state = MutableStateFlow(UiSnapshot())
    val state: StateFlow<UiSnapshot> = _state.asStateFlow()

    init {
        refresh()
        app.database.pendingTryOnJobs().forEach { TryOnWorker.enqueue(app, it.id) }
    }

    fun refresh() = viewModelScope.launch(Dispatchers.IO) {
        val snapshot = UiSnapshot(
            app.database.listGarments(), app.database.listOutfits(), app.database.listWearLogs(),
            app.database.listTryOnJobs(), _state.value.busy, _state.value.message,
            _state.value.revision + 1L
        )
        _state.value = snapshot
    }

    fun consumeMessage() { _state.value = _state.value.copy(message = null) }
    private fun message(text: String) { _state.value = _state.value.copy(message = text, busy = false) }
    private fun busy() { _state.value = _state.value.copy(busy = true, message = null) }

    fun saveGarment(
        existing: Garment?, name: String, category: String, colors: String, material: String,
        seasons: String, occasions: String, style: String, brand: String, priceYuan: String,
        status: GarmentStatus, photoUris: List<Uri>
    ) = viewModelScope.launch(Dispatchers.IO) {
        busy()
        runCatching {
            require(name.isNotBlank()) { "请填写衣物名称" }
            val id = existing?.id ?: java.util.UUID.randomUUID().toString()
            val newPhotos = photoUris.mapIndexed { index, uri ->
                val (path, sha) = app.files.importImage(uri, "garments/$id")
                GarmentPhoto(garmentId = id, relativePath = path, orderIndex = (existing?.photos?.size ?: 0) + index, isPrimary = existing?.photos.isNullOrEmpty() && index == 0, sha256 = sha, tryOnQuality = "AI 抠图后待确认")
            }
            val mergedPhotos = (existing?.photos.orEmpty() + newPhotos).mapIndexed { index, p -> p.copy(orderIndex = index, isPrimary = index == 0) }.take(5)
            val cents = ((priceYuan.toDoubleOrNull() ?: 0.0) * 100).toLong()
            app.database.upsertGarment(Garment(id, name.trim(), category, colors.trim(), material = material.trim(), seasons = seasons.trim(), occasions = occasions.trim(), styleTags = style.trim(), brand = brand.trim(), priceCents = cents, status = status, wearCount = existing?.wearCount ?: 0, lastWornAt = existing?.lastWornAt, createdAt = existing?.createdAt ?: System.currentTimeMillis(), updatedAt = System.currentTimeMillis(), photos = mergedPhotos))
        }.onSuccess { message("衣物已保存到本机") }.onFailure { message(it.message ?: "保存失败") }
        refresh()
    }

    fun updateStatus(garment: Garment, status: GarmentStatus) = viewModelScope.launch(Dispatchers.IO) {
        app.database.upsertGarment(garment.copy(status = status, updatedAt = System.currentTimeMillis()))
        message("状态已更新，造型候选已同步")
        refresh()
    }

    fun deleteGarment(garment: Garment) = viewModelScope.launch(Dispatchers.IO) {
        app.database.deleteGarment(garment.id)
        garment.photos.forEach { runCatching { app.files.absolute(it.relativePath).delete() } }
        message("单品已删除；历史记录保留事实但关联已移除")
        refresh()
    }

    fun deletePhoto(garment: Garment, photoId: String) = viewModelScope.launch(Dispatchers.IO) {
        val photo = garment.photos.find { it.id == photoId } ?: return@launch
        val remaining = garment.photos.filterNot { it.id == photoId }.mapIndexed { index, p -> p.copy(orderIndex = index, isPrimary = index == 0) }
        app.database.upsertGarment(garment.copy(photos = remaining, updatedAt = System.currentTimeMillis()))
        runCatching { app.files.absolute(photo.relativePath).delete() }
        message("照片已从本机删除")
        refresh()
    }

    fun importAiPreview(uri: Uri, onReady: (String) -> Unit) = viewModelScope.launch(Dispatchers.IO) {
        runCatching { app.files.importImage(uri, "ai_previews").first }
            .onSuccess { withContext(Dispatchers.Main) { onReady(it) } }
            .onFailure { message(it.message ?: "无法准备图片") }
    }

    fun sendTestMaintenanceNotification() {
        val manager = app.getSystemService(NotificationManager::class.java)
        manager.createNotificationChannel(NotificationChannel("wardrobe_care", "衣物维护提醒", NotificationManager.IMPORTANCE_DEFAULT))
        val notification = NotificationCompat.Builder(app, "wardrobe_care")
            .setSmallIcon(com.yijian.wardrobe.R.drawable.ic_launcher)
            .setContentTitle("衣见 · 衣物维护提醒")
            .setContentText("这是本机通知测试。关闭通知不会影响衣橱、造型与记录。")
            .setAutoCancel(true).build()
        runCatching { manager.notify(1001, notification) }.onSuccess { message("测试通知已发送") }.onFailure { message("通知未发送，请检查系统权限") }
    }

    fun saveOutfit(name: String, scene: String, items: List<OutfitItem>, existingId: String? = null) = viewModelScope.launch(Dispatchers.IO) {
        if (items.isEmpty()) { message("请先把至少一件可穿单品放入画布"); return@launch }
        app.database.saveOutfit(Outfit(id = existingId ?: java.util.UUID.randomUUID().toString(), name = name.ifBlank { "未命名造型" }, scene = scene.ifBlank { "日常" }, items = items))
        message("造型已保存")
        refresh()
    }

    fun addWearLog(garmentIds: List<String>, outfitId: String?, scene: String, satisfaction: Int, comfort: Int) = viewModelScope.launch(Dispatchers.IO) {
        if (garmentIds.isEmpty()) { message("请选择衣物或造型"); return@launch }
        app.database.addWearLog(WearLog(wornAt = System.currentTimeMillis(), scene = scene.ifBlank { "日常" }, satisfaction = satisfaction, comfort = comfort, outfitId = outfitId, garmentIds = garmentIds))
        message("今天的穿着已记录，次数与单次成本已更新")
        refresh()
    }

    fun associations(id: String) = app.database.garmentAssociations(id)
    fun isEnabled(capability: AiCapability) = app.aiSettings.capabilityEnabled(capability)
    private fun settingsChanged() {
        _state.value = _state.value.copy(revision = _state.value.revision + 1L)
    }
    fun setEnabled(capability: AiCapability, enabled: Boolean) { app.aiSettings.setCapability(capability, enabled); settingsChanged() }
    fun deepSeekEnabled() = app.aiSettings.deepSeekEnabled
    fun bailianEnabled() = app.aiSettings.bailianEnabled
    fun setDeepSeekEnabled(enabled: Boolean) { app.aiSettings.deepSeekEnabled = enabled; settingsChanged() }
    fun setBailianEnabled(enabled: Boolean) { app.aiSettings.bailianEnabled = enabled; settingsChanged() }
    fun maskedDeepSeek() = app.secrets.masked(SecureSecrets.DEEPSEEK_KEY)
    fun maskedBailian() = app.secrets.masked(SecureSecrets.BAILIAN_KEY)
    fun workspaceId() = app.aiSettings.workspaceId
    fun deepSeekBaseUrl() = app.aiSettings.deepSeekBaseUrl
    fun deepSeekVisionModel() = app.aiSettings.deepSeekVisionModel
    fun deepSeekTextModel() = app.aiSettings.deepSeekTextModel
    fun lastDeepSeekTest() = app.aiSettings.lastDeepSeekTest
    fun lastBailianTest() = app.aiSettings.lastBailianTest

    fun saveDeepSeekConfig(key: String, baseUrl: String, visionModel: String, textModel: String) {
        if (key.isNotBlank()) app.secrets.put(SecureSecrets.DEEPSEEK_KEY, key.trim())
        app.aiSettings.deepSeekBaseUrl = baseUrl; app.aiSettings.deepSeekVisionModel = visionModel; app.aiSettings.deepSeekTextModel = textModel
        app.aiSettings.deepSeekEnabled = true
        settingsChanged()
        message("DeepSeek 配置已使用 Android Keystore 加密保存")
    }

    fun saveBailianConfig(key: String, workspaceId: String) {
        if (key.isNotBlank()) app.secrets.put(SecureSecrets.BAILIAN_KEY, key.trim())
        app.aiSettings.workspaceId = workspaceId; app.aiSettings.bailianEnabled = true
        settingsChanged()
        message("百炼配置已加密保存；地域固定为华北2（北京）")
    }

    fun clearProvider(provider: AiProvider) {
        if (provider == AiProvider.DEEPSEEK) app.secrets.remove(SecureSecrets.DEEPSEEK_KEY) else app.secrets.remove(SecureSecrets.BAILIAN_KEY)
        app.aiSettings.clearProvider(provider)
        settingsChanged()
        message("配置已清除；衣橱和已下载结果未删除")
    }

    fun testProvider(provider: AiProvider) = viewModelScope.launch(Dispatchers.IO) {
        busy()
        val result = if (provider == AiProvider.DEEPSEEK) app.deepSeek.testConnection() else app.bailian.testConnection()
        if (provider == AiProvider.BAILIAN && result.success) app.secrets.fingerprint(SecureSecrets.BAILIAN_KEY)?.let(app.aiSettings::unlockShoeAfterVerifiedNewKey)
        val status = "${if (result.success) "通过" else "失败"} · ${result.displayMessage}"
        if (provider == AiProvider.DEEPSEEK) app.aiSettings.lastDeepSeekTest = status else app.aiSettings.lastBailianTest = status
        refresh()
        message(result.displayMessage)
    }

    fun recognizeGarment(garment: Garment, onResult: (String) -> Unit) = viewModelScope.launch(Dispatchers.IO) {
        val capability = AiCapability.RECOGNITION
        val blocked = preflight.check(capability)
        if (blocked != null) { message(blocked); return@launch }
        val path = garment.photos.firstOrNull()?.relativePath ?: run { message("该单品没有可发送图片"); return@launch }
        busy()
        runCatching { app.deepSeek.recognize(path) }
            .onSuccess { raw ->
                runCatching { JSONObject(raw.removePrefix("```json").removeSuffix("```").trim()) }.getOrElse { throw IllegalStateException("AI 返回格式异常，未写入正式衣橱") }
                app.database.recordAiRun(AiProvider.DEEPSEEK, app.aiSettings.deepSeekVisionModel, "衣物识别", "SUCCEEDED")
                withContext(Dispatchers.Main) { onResult(raw) }
                message("识别完成，请核对后再保存")
            }.onFailure { error -> app.database.recordAiRun(AiProvider.DEEPSEEK, app.aiSettings.deepSeekVisionModel, "衣物识别", "FAILED"); message(safeError(error)) }
    }

    fun deepSeekText(capability: AiCapability, prompt: String, imagePaths: List<String> = emptyList(), onResult: (String) -> Unit) = viewModelScope.launch(Dispatchers.IO) {
        val blocked = preflight.check(capability)
        if (blocked != null) { message(blocked); return@launch }
        busy()
        runCatching { app.deepSeek.analyzeText(capability.label, prompt, imagePaths) }
            .onSuccess { withContext(Dispatchers.Main) { onResult(it) }; message("${capability.label}完成") }
            .onFailure { message(safeError(it)) }
    }

    fun startTryOn(modelCapability: AiCapability, personUri: Uri, top: Garment?, bottom: Garment?, coarsePath: String? = null, shoe: Garment? = null) = viewModelScope.launch(Dispatchers.IO) {
        val blocked = preflight.check(modelCapability)
        if (blocked != null) { message(blocked); return@launch }
        busy()
        runCatching {
            val (personPath, _) = app.files.importImage(personUri, "tryon_profiles")
            val topPath = top?.photos?.firstOrNull()?.relativePath
            val bottomPath = bottom?.photos?.firstOrNull()?.relativePath
            val shoePaths = shoe?.photos?.take(2)?.map { it.relativePath }.orEmpty()
            when (modelCapability) {
                AiCapability.SHOE -> require(shoePaths.isNotEmpty()) { "请选择有鞋靴图片的单品" }
                AiCapability.TRYON_REFINER -> require(topPath != null && coarsePath != null) { "精修需要原人物、原服装和粗试衣结果" }
                AiCapability.TRYON_PARSING -> Unit
                else -> require(topPath != null || bottomPath != null) { "请选择至少一件有照片的衣物" }
            }
            val fp = requireNotNull(app.secrets.fingerprint(SecureSecrets.BAILIAN_KEY))
            val job = TryOnJob(model = modelCapability.model, personPath = personPath, topPath = topPath, bottomPath = bottomPath, coarsePath = coarsePath, shoePaths = shoePaths, keyFingerprint = fp)
            app.database.saveTryOnJob(job)
            TryOnWorker.enqueue(app, job.id)
        }.onSuccess { message("任务已创建，可离开页面；后台会恢复同一 task_id") }.onFailure { message(it.message ?: "无法创建任务") }
        refresh()
    }

    fun exportBackup(uri: Uri, includeTryOn: Boolean) = viewModelScope.launch(Dispatchers.IO) { busy(); message(runCatching { app.backups.exportTo(uri, includeTryOn).message }.getOrElse { it.message ?: "备份失败" }) }
    fun validateBackup(uri: Uri, onResult: (BackupValidation) -> Unit) = viewModelScope.launch(Dispatchers.IO) { val r = runCatching { app.backups.validate(uri) }.getOrElse { BackupValidation(false, it.message ?: "校验失败") }; withContext(Dispatchers.Main) { onResult(r) }; message(r.message) }
    fun restoreBackup(uri: Uri) = viewModelScope.launch(Dispatchers.IO) { busy(); val r = runCatching { app.backups.restoreReplace(uri) }.getOrElse { BackupValidation(false, it.message ?: "恢复失败") }; message(r.message); refresh() }
}
