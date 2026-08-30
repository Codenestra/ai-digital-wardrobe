package com.yijian.wardrobe.data

import org.json.JSONArray
import org.json.JSONObject
import java.util.UUID

enum class GarmentStatus(val label: String, val wearable: Boolean) {
    AVAILABLE("可穿", true),
    WASHING("清洗中", false),
    DRY_CLEANING("送洗中", false),
    STORED("收纳中", false),
    LENT("已借出", false),
    DONATED("已捐赠", false),
    SOLD("已售出", false)
}

data class Garment(
    val id: String = UUID.randomUUID().toString(),
    val name: String,
    val category: String,
    val colors: String = "",
    val pattern: String = "",
    val material: String = "",
    val seasons: String = "",
    val occasions: String = "",
    val styleTags: String = "",
    val brand: String = "",
    val purchaseDate: String = "",
    val priceCents: Long = 0,
    val size: String = "",
    val comfortNote: String = "",
    val careNote: String = "",
    val status: GarmentStatus = GarmentStatus.AVAILABLE,
    val wearCount: Int = 0,
    val lastWornAt: Long? = null,
    val createdAt: Long = System.currentTimeMillis(),
    val updatedAt: Long = System.currentTimeMillis(),
    val photos: List<GarmentPhoto> = emptyList()
) {
    val costPerWearCents: Long? get() = if (priceCents > 0 && wearCount > 0) priceCents / wearCount else null
    fun toJson() = JSONObject().apply {
        put("id", id); put("name", name); put("category", category); put("colors", colors)
        put("pattern", pattern); put("material", material); put("seasons", seasons)
        put("occasions", occasions); put("styleTags", styleTags); put("brand", brand)
        put("purchaseDate", purchaseDate); put("priceCents", priceCents); put("size", size)
        put("comfortNote", comfortNote); put("careNote", careNote); put("status", status.name)
        put("wearCount", wearCount); put("lastWornAt", lastWornAt ?: JSONObject.NULL)
        put("createdAt", createdAt); put("updatedAt", updatedAt)
        put("photos", JSONArray().apply { photos.forEach { put(it.toJson()) } })
    }
}

data class GarmentPhoto(
    val id: String = UUID.randomUUID().toString(),
    val garmentId: String,
    val relativePath: String,
    val orderIndex: Int,
    val isPrimary: Boolean,
    val sha256: String,
    val tryOnQuality: String = "仅作衣橱展示"
) {
    fun toJson() = JSONObject().apply {
        put("id", id); put("garmentId", garmentId); put("relativePath", relativePath)
        put("orderIndex", orderIndex); put("isPrimary", isPrimary); put("sha256", sha256)
        put("tryOnQuality", tryOnQuality)
    }
}

data class OutfitItem(
    val garmentId: String,
    val x: Float = 0f,
    val y: Float = 0f,
    val scale: Float = 1f,
    val rotation: Float = 0f,
    val zIndex: Int = 0
)

data class Outfit(
    val id: String = UUID.randomUUID().toString(),
    val name: String,
    val scene: String,
    val rating: Int = 0,
    val favorite: Boolean = false,
    val createdAt: Long = System.currentTimeMillis(),
    val items: List<OutfitItem> = emptyList()
)

data class WearLog(
    val id: String = UUID.randomUUID().toString(),
    val wornAt: Long,
    val scene: String,
    val weatherNote: String = "",
    val satisfaction: Int = 3,
    val comfort: Int = 3,
    val photoPath: String? = null,
    val outfitId: String? = null,
    val garmentIds: List<String> = emptyList(),
    val createdAt: Long = System.currentTimeMillis()
)

enum class AiProvider { DEEPSEEK, BAILIAN }

enum class AiCapability(val provider: AiProvider, val label: String, val model: String) {
    RECOGNITION(AiProvider.DEEPSEEK, "衣物自动识别", "deepseek-v4-flash-vision-exp"),
    OUTFIT_ANALYSIS(AiProvider.DEEPSEEK, "AI搭配与理由", "deepseek-v4-flash-vision-exp"),
    MIRROR_ANALYSIS(AiProvider.DEEPSEEK, "镜面照分析", "deepseek-v4-flash-vision-exp"),
    BUY_CHECK(AiProvider.DEEPSEEK, "买前检查", "deepseek-v4-flash-vision-exp"),
    NATURAL_SEARCH(AiProvider.DEEPSEEK, "自然语言搜索", "deepseek-v4-flash"),
    REVIEW(AiProvider.DEEPSEEK, "衣橱复盘", "deepseek-v4-flash"),
    TRYON_BASIC(AiProvider.BAILIAN, "极速试穿", "aitryon"),
    TRYON_PLUS(AiProvider.BAILIAN, "高清试穿", "aitryon-plus"),
    TRYON_REFINER(AiProvider.BAILIAN, "图片精修", "aitryon-refiner"),
    TRYON_PARSING(AiProvider.BAILIAN, "局部替换", "aitryon-parsing-v1"),
    SHOE(AiProvider.BAILIAN, "鞋靴试穿（实验）", "shoemodel-v1")
}

enum class JobStatus { PREPARING, UPLOADING, SUBMITTED, PENDING, RUNNING, REFINING, REPLACING_SHOES, DOWNLOADING, SUCCEEDED, FAILED }

data class TryOnJob(
    val id: String = UUID.randomUUID().toString(),
    val model: String,
    val providerTaskId: String? = null,
    val status: JobStatus = JobStatus.PREPARING,
    val personPath: String,
    val topPath: String? = null,
    val bottomPath: String? = null,
    val coarsePath: String? = null,
    val shoePaths: List<String> = emptyList(),
    val resultPath: String? = null,
    val errorCode: String? = null,
    val errorMessage: String? = null,
    val keyFingerprint: String,
    val createdAt: Long = System.currentTimeMillis(),
    val updatedAt: Long = System.currentTimeMillis()
)

data class AiPreview(
    val capability: AiCapability,
    val imagePaths: List<String>,
    val purpose: String,
    val textSummary: String,
    val costReference: String
)

data class AiResult(val success: Boolean, val displayMessage: String, val rawJson: String? = null)
