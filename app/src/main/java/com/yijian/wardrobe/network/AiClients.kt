package com.yijian.wardrobe.network

import android.content.Context
import android.graphics.Bitmap
import android.graphics.BitmapFactory
import android.net.ConnectivityManager
import android.net.NetworkCapabilities
import android.util.Base64
import com.yijian.wardrobe.R
import com.yijian.wardrobe.data.AiCapability
import com.yijian.wardrobe.data.AiProvider
import com.yijian.wardrobe.data.AiResult
import com.yijian.wardrobe.data.AiSettings
import com.yijian.wardrobe.data.LocalFiles
import com.yijian.wardrobe.data.SecureSecrets
import okhttp3.MediaType.Companion.toMediaType
import okhttp3.MultipartBody
import okhttp3.OkHttpClient
import okhttp3.Request
import okhttp3.RequestBody.Companion.asRequestBody
import okhttp3.RequestBody.Companion.toRequestBody
import org.json.JSONArray
import org.json.JSONObject
import java.io.ByteArrayOutputStream
import java.io.File
import java.util.concurrent.TimeUnit

class SafeApiException(val httpStatus: Int?, val code: String, safeMessage: String) : Exception(safeMessage.take(400))

class AiPreflight(private val context: Context, private val secrets: SecureSecrets, private val settings: AiSettings) {
    fun check(capability: AiCapability): String? {
        val master = if (capability.provider == AiProvider.DEEPSEEK) settings.deepSeekEnabled else settings.bailianEnabled
        if (!master) return "${if (capability.provider == AiProvider.DEEPSEEK) "DeepSeek" else "百炼"}总开关已关闭"
        if (!settings.capabilityEnabled(capability)) return "${capability.label}子开关已关闭"
        val keyName = if (capability.provider == AiProvider.DEEPSEEK) SecureSecrets.DEEPSEEK_KEY else SecureSecrets.BAILIAN_KEY
        if (secrets.get(keyName).isNullOrBlank()) return "尚未配置对应服务的 API Key"
        if (capability.provider == AiProvider.BAILIAN && !settings.workspaceId.matches(Regex("^ws[-_][A-Za-z0-9]{6,64}$"))) return "Workspace ID 格式不正确"
        if (!networkAvailable()) return "当前网络不可用；本地衣橱仍可正常使用"
        if (capability == AiCapability.SHOE) {
            val fp = secrets.fingerprint(SecureSecrets.BAILIAN_KEY)
            if (fp != null && settings.shoeLockedFor(fp)) return "当前 Key 的鞋靴免费额度已明确耗尽，需更换并验证新 Key"
        }
        return null
    }

    fun networkAvailable(): Boolean {
        val cm = context.getSystemService(ConnectivityManager::class.java)
        val network = cm.activeNetwork ?: return false
        val caps = cm.getNetworkCapabilities(network) ?: return false
        return caps.hasCapability(NetworkCapabilities.NET_CAPABILITY_INTERNET) && caps.hasCapability(NetworkCapabilities.NET_CAPABILITY_VALIDATED)
    }
}

class DeepSeekClient(
    private val context: Context,
    private val settings: AiSettings,
    private val secrets: SecureSecrets,
    private val files: LocalFiles,
    private val client: OkHttpClient = defaultClient()
) {
    fun testConnection(): AiResult {
        // Use the exact, production launcher image shown in the confirmation dialog. The old
        // 2x2 PNG was technically valid but too small to be a reliable provider capability test.
        val launcherBitmap = requireNotNull(BitmapFactory.decodeResource(context.resources, R.drawable.ic_launcher_art)) {
            "Unable to decode launcher image"
        }
        val imageBytes = ByteArrayOutputStream().use { output ->
            launcherBitmap.compress(Bitmap.CompressFormat.PNG, 100, output)
            output.toByteArray()
        }
        val prompt = "只返回 JSON：{\"subject\":\"...\",\"ok\":true}。简短识别这张衣见应用图标。"
        return runCatching {
            val raw = chat(settings.deepSeekVisionModel, prompt, listOf(imageBytes to "image/png"))
            if (!raw.contains("ok", ignoreCase = true)) throw SafeApiException(null, "UNEXPECTED_RESPONSE", "视觉模型已响应，但返回内容不符合测试预期")
            AiResult(true, "视觉模型鉴权、模型 ID 与 PNG 图片输入均可用")
        }
            .getOrElse { AiResult(false, safeError(it)) }
    }

    fun recognize(imagePath: String): String {
        val file = files.absolute(imagePath)
        val prompt = """识别这件衣物。只返回一个合法 JSON 对象，不要 Markdown：
            {"category":"","colors":[],"pattern":"","material_guess":[],"seasons":[],"occasions":[],"style_tags":[],"warmth":1,"formality":1,"uncertain_fields":[]}
            材质只能作为视觉推测；禁止推断人物身份、年龄或身体评价。""".trimIndent()
        return chat(settings.deepSeekVisionModel, prompt, listOf(file.readBytes() to "image/jpeg"))
    }

    fun analyzeText(purpose: String, content: String, imagePaths: List<String> = emptyList()): String {
        val images = imagePaths.map { files.absolute(it).readBytes() to "image/jpeg" }
        val model = if (images.isEmpty()) settings.deepSeekTextModel else settings.deepSeekVisionModel
        return chat(model, "$purpose\n$content\n回答必须中性、可解释，不做身材或颜值评分。", images)
    }

    private fun chat(model: String, text: String, images: List<Pair<ByteArray, String>>): String {
        val key = secrets.get(SecureSecrets.DEEPSEEK_KEY) ?: throw SafeApiException(null, "NOT_CONFIGURED", "DeepSeek Key 未配置")
        val content = JSONArray().put(JSONObject().put("type", "text").put("text", text))
        images.forEach { (bytes, mime) -> content.put(JSONObject().put("type", "image_url").put("image_url", JSONObject().put("url", "data:$mime;base64,${Base64.encodeToString(bytes, Base64.NO_WRAP)}").put("detail", "auto"))) }
        val body = JSONObject().put("model", model).put("messages", JSONArray().put(JSONObject().put("role", "user").put("content", content))).put("temperature", 0.2).put("max_tokens", 1200)
        val request = Request.Builder().url("${settings.deepSeekBaseUrl}/chat/completions")
            .header("Authorization", "Bearer $key").header("Content-Type", "application/json")
            .post(body.toString().toRequestBody(JSON)).build()
        client.newCall(request).execute().use { response ->
            val raw = response.body?.string().orEmpty()
            if (!response.isSuccessful) throw parsedError(response.code, raw)
            val json = JSONObject(raw)
            val message = json.getJSONArray("choices").getJSONObject(0).getJSONObject("message")
            val content = message.optString("content").trim()
            if (content.isBlank()) throw SafeApiException(null, "EMPTY_RESPONSE", "视觉模型返回了空内容，请重试；本次不会写入本地结果")
            return content
        }
    }

}

data class UploadPolicy(
    val policy: String, val signature: String, val uploadDir: String, val uploadHost: String,
    val accessKeyId: String, val objectAcl: String, val forbidOverwrite: String
)
data class TaskState(val taskId: String, val status: String, val resultUrl: String?, val code: String?, val message: String?)

class BailianClient(
    private val settings: AiSettings,
    private val secrets: SecureSecrets,
    private val client: OkHttpClient = defaultClient()
) {
    private fun key() = secrets.get(SecureSecrets.BAILIAN_KEY) ?: throw SafeApiException(null, "NOT_CONFIGURED", "百炼 Key 未配置")
    private fun base(): String {
        val workspace = settings.workspaceId.trim()
        require(workspace.matches(Regex("^ws[-_][A-Za-z0-9]{6,64}$"))) { "Workspace ID 格式不正确" }
        return "https://$workspace.cn-beijing.maas.aliyuncs.com/api/v1"
    }

    fun testConnection(): AiResult = runCatching {
        val modelsReq = Request.Builder().url("${base()}/models").header("Authorization", "Bearer ${key()}").get().build()
        client.newCall(modelsReq).execute().use { r -> val raw = r.body?.string().orEmpty(); if (!r.isSuccessful) throw parsedError(r.code, raw) }
        getUploadPolicy("aitryon")
        AiResult(true, "北京 Workspace、Key、模型列表和临时上传权限可用")
    }.getOrElse { AiResult(false, safeError(it)) }

    fun getUploadPolicy(model: String): UploadPolicy {
        val url = "https://dashscope.aliyuncs.com/api/v1/uploads?action=getPolicy&model=$model"
        val req = Request.Builder().url(url).header("Authorization", "Bearer ${key()}").header("Content-Type", "application/json").get().build()
        client.newCall(req).execute().use { r ->
            val raw = r.body?.string().orEmpty(); if (!r.isSuccessful) throw parsedError(r.code, raw)
            val d = JSONObject(raw).getJSONObject("data")
            return UploadPolicy(d.getString("policy"), d.getString("signature"), d.getString("upload_dir"), d.getString("upload_host"), d.getString("oss_access_key_id"), d.optString("x_oss_object_acl", "private"), d.optString("x_oss_forbid_overwrite", "true"))
        }
    }

    fun uploadTemporary(file: File, model: String): String {
        val p = getUploadPolicy(model)
        val safeName = "${System.currentTimeMillis()}_${file.name.replace(Regex("[^A-Za-z0-9._-]"), "_")}"
        val objectKey = "${p.uploadDir}/$safeName"
        val multipart = MultipartBody.Builder().setType(MultipartBody.FORM)
            .addFormDataPart("OSSAccessKeyId", p.accessKeyId)
            .addFormDataPart("Signature", p.signature)
            .addFormDataPart("policy", p.policy)
            .addFormDataPart("x-oss-object-acl", p.objectAcl)
            .addFormDataPart("x-oss-forbid-overwrite", p.forbidOverwrite)
            .addFormDataPart("key", objectKey)
            .addFormDataPart("success_action_status", "200")
            .addFormDataPart("file", safeName, file.asRequestBody("image/jpeg".toMediaType()))
            .build()
        val request = Request.Builder().url(p.uploadHost).post(multipart).build()
        client.newCall(request).execute().use { r -> if (!r.isSuccessful) throw SafeApiException(r.code, "UPLOAD_FAILED", "临时上传失败（HTTP ${r.code}）") }
        return "oss://$objectKey"
    }

    fun submitTryOn(model: String, personUrl: String, topUrl: String?, bottomUrl: String?, coarseUrl: String? = null, shoeUrls: List<String> = emptyList()): String {
        val input = JSONObject()
        val parameters = JSONObject()
        when (model) {
            "shoemodel-v1" -> {
                input.put("template_image_url", personUrl).put("shoe_image_url", JSONArray(shoeUrls))
                parameters.put("n", 1).put("scale", 5.0)
            }
            "aitryon-refiner" -> {
                input.put("person_image_url", personUrl).put("top_garment_url", requireNotNull(topUrl)).put("coarse_image_url", requireNotNull(coarseUrl))
                if (bottomUrl != null) input.put("bottom_garment_url", bottomUrl)
                parameters.put("gender", "woman")
            }
            "aitryon-parsing-v1" -> {
                input.put("image_url", personUrl)
                parameters.put("clothes_type", JSONArray().put("upper"))
            }
            else -> {
                input.put("person_image_url", personUrl)
                if (topUrl != null) input.put("top_garment_url", topUrl)
                if (bottomUrl != null) input.put("bottom_garment_url", bottomUrl)
                parameters.put("resolution", -1).put("restore_face", true)
            }
        }
        val endpoint = when (model) {
            "shoemodel-v1" -> "${base()}/services/aigc/virtualmodel/generation"
            "aitryon-parsing-v1" -> "${base()}/services/vision/image-process/process"
            else -> "${base()}/services/aigc/image2image/image-synthesis"
        }
        val body = JSONObject().put("model", model).put("input", input).put("parameters", parameters)
        val builder = Request.Builder().url(endpoint).header("Authorization", "Bearer ${key()}")
            .header("Content-Type", "application/json")
            .header("X-DashScope-OssResourceResolve", "enable")
        if (model != "aitryon-parsing-v1") builder.header("X-DashScope-Async", "enable")
        val req = builder.post(body.toString().toRequestBody(JSON)).build()
        client.newCall(req).execute().use { r ->
            val raw = r.body?.string().orEmpty(); if (!r.isSuccessful) throw parsedError(r.code, raw)
            val output = JSONObject(raw).getJSONObject("output")
            if (model == "aitryon-parsing-v1") {
                val resultUrl = output.optJSONArray("parsing_img_url")?.optString(0)?.takeIf { it.isNotBlank() }
                    ?: output.optJSONArray("crop_img_url")?.optString(0)?.takeIf { it.isNotBlank() }
                    ?: throw SafeApiException(null, "MISSING_RESULT", "分割完成但没有可下载结果")
                return "sync:$resultUrl"
            }
            return output.getString("task_id")
        }
    }

    fun queryTask(taskId: String): TaskState {
        val req = Request.Builder().url("${base()}/tasks/$taskId").header("Authorization", "Bearer ${key()}").get().build()
        client.newCall(req).execute().use { r ->
            val raw = r.body?.string().orEmpty(); if (!r.isSuccessful) throw parsedError(r.code, raw)
            val out = JSONObject(raw).getJSONObject("output")
            val url = out.optString("image_url").takeIf { it.isNotBlank() }
                ?: out.optJSONArray("results")?.optJSONObject(0)?.optString("url")?.takeIf { it.isNotBlank() }
            return TaskState(out.optString("task_id", taskId), out.optString("task_status", "UNKNOWN"), url, out.optString("code").takeIf { it.isNotBlank() }, out.optString("message").takeIf { it.isNotBlank() })
        }
    }

    fun download(url: String, target: File) {
        val req = Request.Builder().url(url).get().build()
        client.newCall(req).execute().use { r ->
            if (!r.isSuccessful) throw SafeApiException(r.code, "DOWNLOAD_FAILED", "结果下载失败（HTTP ${r.code}）")
            target.parentFile?.mkdirs(); target.outputStream().use { out -> requireNotNull(r.body).byteStream().copyTo(out) }
            if (target.length() < 1024) throw SafeApiException(null, "RESULT_INVALID", "下载结果无效")
        }
    }
}

private val JSON = "application/json; charset=utf-8".toMediaType()
fun defaultClient() = OkHttpClient.Builder().connectTimeout(20, TimeUnit.SECONDS).readTimeout(90, TimeUnit.SECONDS).writeTimeout(90, TimeUnit.SECONDS).callTimeout(120, TimeUnit.SECONDS).retryOnConnectionFailure(false).build()

fun parsedError(httpStatus: Int, raw: String): SafeApiException {
    val json = runCatching { JSONObject(raw) }.getOrNull()
    val code = json?.optString("code")?.takeIf { it.isNotBlank() }
        ?: json?.optJSONObject("error")?.optString("code")?.takeIf { it.isNotBlank() }
        ?: "HTTP_$httpStatus"
    val providerMessage = json?.optString("message")?.takeIf { it.isNotBlank() }
        ?: json?.optJSONObject("error")?.optString("message")?.takeIf { it.isNotBlank() }
    val safe = when {
        httpStatus == 401 || code.contains("InvalidApiKey", true) -> "API Key 无效或已失效"
        httpStatus == 429 || code.contains("Throttl", true) || code.contains("Rate", true) -> "请求过于频繁，请稍后重试"
        code.contains("AccessDenied.Unpurchased", true) || code.contains("Unpurchased", true) -> "当前业务空间尚未开通该模型，请先在百炼控制台申请或开通对应模型权限"
        code.contains("Workspace", true) || code.contains("Endpoint.AccessDenied", true) -> "Workspace、地域或模型权限不匹配"
        code == "AllocationQuota.FreeTierOnly" -> "官方返回免费额度终止状态"
        code.contains("Quota", true) -> "模型额度不足或计费状态异常"
        providerMessage?.contains("unsupported image", true) == true -> "服务商无法解析测试图片；请确认使用视觉模型，并重试新版内置 PNG 测试图"
        providerMessage?.contains("does not support image", true) == true -> "当前模型不支持图片，请使用 deepseek-v4-flash-vision-exp"
        code.contains("Image", true) || code.contains("InvalidParameter", true) -> "图片或请求参数不符合模型要求"
        else -> providerMessage?.take(180) ?: "服务请求失败（HTTP $httpStatus）"
    }
    return SafeApiException(httpStatus, code, safe)
}

fun safeError(error: Throwable): String = when (error) {
    is SafeApiException -> "${error.message} [${error.code}]"
    is java.net.UnknownHostException -> "网络不可用或域名解析失败"
    is java.net.SocketTimeoutException -> "请求超时，请稍后重试"
    is IllegalArgumentException -> error.message ?: "配置或输入不正确"
    else -> "请求未完成：${error::class.simpleName ?: "未知错误"}"
}
