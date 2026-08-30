package com.yijian.wardrobe.data

import android.content.ContentValues
import android.content.Context
import android.database.Cursor
import android.database.sqlite.SQLiteDatabase
import android.database.sqlite.SQLiteOpenHelper
import org.json.JSONArray
import org.json.JSONObject

class WardrobeDatabase(context: Context, databaseName: String = DB_NAME) : SQLiteOpenHelper(context, databaseName, null, DB_VERSION) {
    override fun onConfigure(db: SQLiteDatabase) {
        db.setForeignKeyConstraintsEnabled(true)
        db.enableWriteAheadLogging()
    }

    override fun onCreate(db: SQLiteDatabase) {
        db.execSQL("""CREATE TABLE garments(
            id TEXT PRIMARY KEY, name TEXT NOT NULL, category TEXT NOT NULL, colors TEXT NOT NULL,
            pattern TEXT NOT NULL, material TEXT NOT NULL, seasons TEXT NOT NULL, occasions TEXT NOT NULL,
            style_tags TEXT NOT NULL, brand TEXT NOT NULL, purchase_date TEXT NOT NULL, price_cents INTEGER NOT NULL,
            size TEXT NOT NULL, comfort_note TEXT NOT NULL, care_note TEXT NOT NULL, status TEXT NOT NULL,
            wear_count INTEGER NOT NULL DEFAULT 0, last_worn_at INTEGER, created_at INTEGER NOT NULL, updated_at INTEGER NOT NULL
        )""")
        db.execSQL("""CREATE TABLE garment_photos(
            id TEXT PRIMARY KEY, garment_id TEXT NOT NULL REFERENCES garments(id) ON DELETE CASCADE,
            relative_path TEXT NOT NULL, order_index INTEGER NOT NULL, is_primary INTEGER NOT NULL,
            sha256 TEXT NOT NULL, tryon_quality TEXT NOT NULL
        )""")
        db.execSQL("""CREATE TABLE garment_states(
            id INTEGER PRIMARY KEY AUTOINCREMENT, garment_id TEXT NOT NULL REFERENCES garments(id) ON DELETE CASCADE,
            old_status TEXT, new_status TEXT NOT NULL, changed_at INTEGER NOT NULL
        )""")
        db.execSQL("""CREATE TABLE outfits(
            id TEXT PRIMARY KEY, name TEXT NOT NULL, scene TEXT NOT NULL, rating INTEGER NOT NULL,
            favorite INTEGER NOT NULL, created_at INTEGER NOT NULL
        )""")
        db.execSQL("""CREATE TABLE outfit_items(
            outfit_id TEXT NOT NULL REFERENCES outfits(id) ON DELETE CASCADE,
            garment_id TEXT NOT NULL REFERENCES garments(id), x REAL NOT NULL, y REAL NOT NULL,
            scale REAL NOT NULL, rotation REAL NOT NULL, z_index INTEGER NOT NULL,
            PRIMARY KEY(outfit_id, garment_id)
        )""")
        db.execSQL("""CREATE TABLE wear_logs(
            id TEXT PRIMARY KEY, worn_at INTEGER NOT NULL, scene TEXT NOT NULL, weather_note TEXT NOT NULL,
            satisfaction INTEGER NOT NULL, comfort INTEGER NOT NULL, photo_path TEXT, outfit_id TEXT,
            created_at INTEGER NOT NULL
        )""")
        db.execSQL("""CREATE TABLE wear_log_items(
            wear_log_id TEXT NOT NULL REFERENCES wear_logs(id) ON DELETE CASCADE,
            garment_id TEXT NOT NULL REFERENCES garments(id), PRIMARY KEY(wear_log_id, garment_id)
        )""")
        db.execSQL("""CREATE TABLE tryon_jobs(
            id TEXT PRIMARY KEY, model TEXT NOT NULL, provider_task_id TEXT, status TEXT NOT NULL,
            person_path TEXT NOT NULL, top_path TEXT, bottom_path TEXT, coarse_path TEXT, shoe_paths TEXT NOT NULL,
            result_path TEXT, error_code TEXT, error_message TEXT, key_fingerprint TEXT NOT NULL,
            created_at INTEGER NOT NULL, updated_at INTEGER NOT NULL
        )""")
        db.execSQL("""CREATE TABLE ai_runs(
            id INTEGER PRIMARY KEY AUTOINCREMENT, provider TEXT NOT NULL, model TEXT NOT NULL,
            purpose TEXT NOT NULL, status TEXT NOT NULL, error_code TEXT, created_at INTEGER NOT NULL
        )""")
        db.execSQL("CREATE INDEX idx_garments_status ON garments(status)")
        db.execSQL("CREATE INDEX idx_photos_garment ON garment_photos(garment_id, order_index)")
        db.execSQL("CREATE INDEX idx_wear_date ON wear_logs(worn_at DESC)")
    }

    override fun onUpgrade(db: SQLiteDatabase, oldVersion: Int, newVersion: Int) = Unit

    fun upsertGarment(garment: Garment) = writableDatabase.transaction {
        val oldStatus = rawQuery("SELECT status FROM garments WHERE id=?", arrayOf(garment.id)).use { c ->
            if (c.moveToFirst()) c.getString(0) else null
        }
        insertWithOnConflict("garments", null, garment.values(), SQLiteDatabase.CONFLICT_REPLACE)
        if (oldStatus != null && oldStatus != garment.status.name) {
            insert("garment_states", null, ContentValues().apply {
                put("garment_id", garment.id); put("old_status", oldStatus)
                put("new_status", garment.status.name); put("changed_at", System.currentTimeMillis())
            })
        }
        if (garment.photos.isNotEmpty()) {
            delete("garment_photos", "garment_id=?", arrayOf(garment.id))
            garment.photos.forEach { insert("garment_photos", null, it.values()) }
        }
    }

    fun listGarments(): List<Garment> = readableDatabase.rawQuery(
        "SELECT * FROM garments ORDER BY created_at DESC", null
    ).use { cursor -> buildList { while (cursor.moveToNext()) add(cursor.garment(listPhotos(cursor.str("id")))) } }

    fun garment(id: String): Garment? = readableDatabase.rawQuery(
        "SELECT * FROM garments WHERE id=?", arrayOf(id)
    ).use { c -> if (c.moveToFirst()) c.garment(listPhotos(id)) else null }

    fun listPhotos(garmentId: String): List<GarmentPhoto> = readableDatabase.rawQuery(
        "SELECT * FROM garment_photos WHERE garment_id=? ORDER BY order_index", arrayOf(garmentId)
    ).use { c -> buildList { while (c.moveToNext()) add(c.photo()) } }

    fun deleteGarment(id: String): Pair<Int, Int> = writableDatabase.transactionResult {
        val outfits = rawQuery("SELECT COUNT(*) FROM outfit_items WHERE garment_id=?", arrayOf(id)).use { it.moveToFirst(); it.getInt(0) }
        val logs = rawQuery("SELECT COUNT(*) FROM wear_log_items WHERE garment_id=?", arrayOf(id)).use { it.moveToFirst(); it.getInt(0) }
        delete("outfit_items", "garment_id=?", arrayOf(id))
        delete("wear_log_items", "garment_id=?", arrayOf(id))
        delete("garments", "id=?", arrayOf(id))
        outfits to logs
    }

    fun garmentAssociations(id: String): Pair<Int, Int> = readableDatabase.run {
        val outfits = rawQuery("SELECT COUNT(*) FROM outfit_items WHERE garment_id=?", arrayOf(id)).use { it.moveToFirst(); it.getInt(0) }
        val logs = rawQuery("SELECT COUNT(*) FROM wear_log_items WHERE garment_id=?", arrayOf(id)).use { it.moveToFirst(); it.getInt(0) }
        outfits to logs
    }

    fun saveOutfit(outfit: Outfit) = writableDatabase.transaction {
        insertWithOnConflict("outfits", null, ContentValues().apply {
            put("id", outfit.id); put("name", outfit.name); put("scene", outfit.scene)
            put("rating", outfit.rating); put("favorite", if (outfit.favorite) 1 else 0); put("created_at", outfit.createdAt)
        }, SQLiteDatabase.CONFLICT_REPLACE)
        delete("outfit_items", "outfit_id=?", arrayOf(outfit.id))
        outfit.items.forEach { item ->
            insert("outfit_items", null, ContentValues().apply {
                put("outfit_id", outfit.id); put("garment_id", item.garmentId); put("x", item.x); put("y", item.y)
                put("scale", item.scale); put("rotation", item.rotation); put("z_index", item.zIndex)
            })
        }
    }

    fun listOutfits(): List<Outfit> = readableDatabase.rawQuery("SELECT * FROM outfits ORDER BY created_at DESC", null).use { c ->
        buildList {
            while (c.moveToNext()) {
                val id = c.str("id")
                val items = readableDatabase.rawQuery("SELECT * FROM outfit_items WHERE outfit_id=? ORDER BY z_index", arrayOf(id)).use { ic ->
                    buildList { while (ic.moveToNext()) add(OutfitItem(ic.str("garment_id"), ic.flt("x"), ic.flt("y"), ic.flt("scale"), ic.flt("rotation"), ic.int("z_index"))) }
                }
                add(Outfit(id, c.str("name"), c.str("scene"), c.int("rating"), c.int("favorite") == 1, c.lng("created_at"), items))
            }
        }
    }

    fun deleteOutfit(id: String) { writableDatabase.delete("outfits", "id=?", arrayOf(id)) }

    fun addWearLog(log: WearLog) = writableDatabase.transaction {
        insert("wear_logs", null, ContentValues().apply {
            put("id", log.id); put("worn_at", log.wornAt); put("scene", log.scene); put("weather_note", log.weatherNote)
            put("satisfaction", log.satisfaction); put("comfort", log.comfort); put("photo_path", log.photoPath)
            put("outfit_id", log.outfitId); put("created_at", log.createdAt)
        })
        log.garmentIds.distinct().forEach { garmentId ->
            insert("wear_log_items", null, ContentValues().apply { put("wear_log_id", log.id); put("garment_id", garmentId) })
            execSQL("UPDATE garments SET wear_count=wear_count+1,last_worn_at=?,updated_at=? WHERE id=?", arrayOf(log.wornAt, System.currentTimeMillis(), garmentId))
        }
    }

    fun listWearLogs(): List<WearLog> = readableDatabase.rawQuery("SELECT * FROM wear_logs ORDER BY worn_at DESC", null).use { c ->
        buildList {
            while (c.moveToNext()) {
                val id = c.str("id")
                val ids = readableDatabase.rawQuery("SELECT garment_id FROM wear_log_items WHERE wear_log_id=?", arrayOf(id)).use { x -> buildList { while (x.moveToNext()) add(x.getString(0)) } }
                add(WearLog(id, c.lng("worn_at"), c.str("scene"), c.str("weather_note"), c.int("satisfaction"), c.int("comfort"), c.nullableString("photo_path"), c.nullableString("outfit_id"), ids, c.lng("created_at")))
            }
        }
    }

    fun saveTryOnJob(job: TryOnJob) {
        writableDatabase.insertWithOnConflict("tryon_jobs", null, ContentValues().apply {
            put("id", job.id); put("model", job.model); put("provider_task_id", job.providerTaskId); put("status", job.status.name)
            put("person_path", job.personPath); put("top_path", job.topPath); put("bottom_path", job.bottomPath); put("coarse_path", job.coarsePath)
            put("shoe_paths", JSONArray(job.shoePaths).toString()); put("result_path", job.resultPath); put("error_code", job.errorCode)
            put("error_message", job.errorMessage?.take(500)); put("key_fingerprint", job.keyFingerprint); put("created_at", job.createdAt); put("updated_at", job.updatedAt)
        }, SQLiteDatabase.CONFLICT_REPLACE)
    }

    fun tryOnJob(id: String): TryOnJob? = readableDatabase.rawQuery("SELECT * FROM tryon_jobs WHERE id=?", arrayOf(id)).use { c -> if (c.moveToFirst()) c.job() else null }
    fun pendingTryOnJobs(): List<TryOnJob> = readableDatabase.rawQuery("SELECT * FROM tryon_jobs WHERE status NOT IN ('SUCCEEDED','FAILED')", null).use { c -> buildList { while (c.moveToNext()) add(c.job()) } }
    fun listTryOnJobs(): List<TryOnJob> = readableDatabase.rawQuery("SELECT * FROM tryon_jobs ORDER BY created_at DESC", null).use { c -> buildList { while (c.moveToNext()) add(c.job()) } }

    fun recordAiRun(provider: AiProvider, model: String, purpose: String, status: String, errorCode: String? = null) {
        writableDatabase.insert("ai_runs", null, ContentValues().apply {
            put("provider", provider.name); put("model", model); put("purpose", purpose); put("status", status)
            put("error_code", errorCode); put("created_at", System.currentTimeMillis())
        })
    }

    fun exportJson(): JSONObject = JSONObject().apply {
        put("schemaVersion", DB_VERSION)
        put("exportedAt", System.currentTimeMillis())
        put("garments", JSONArray().apply { listGarments().forEach { put(it.toJson()) } })
        put("outfits", JSONArray().apply { listOutfits().forEach { o ->
            put(JSONObject().apply {
                put("id", o.id); put("name", o.name); put("scene", o.scene); put("rating", o.rating)
                put("favorite", o.favorite); put("createdAt", o.createdAt)
                put("items", JSONArray().apply { o.items.forEach { i -> put(JSONObject().apply {
                    put("garmentId", i.garmentId); put("x", i.x); put("y", i.y); put("scale", i.scale); put("rotation", i.rotation); put("zIndex", i.zIndex)
                }) } })
            })
        } })
        put("wearLogs", JSONArray().apply { listWearLogs().forEach { l -> put(JSONObject().apply {
            put("id", l.id); put("wornAt", l.wornAt); put("scene", l.scene); put("weatherNote", l.weatherNote)
            put("satisfaction", l.satisfaction); put("comfort", l.comfort); put("photoPath", l.photoPath)
            put("outfitId", l.outfitId); put("garmentIds", JSONArray(l.garmentIds)); put("createdAt", l.createdAt)
        }) } })
    }

    fun replaceFromJson(root: JSONObject) = writableDatabase.transaction {
        execSQL("DELETE FROM wear_log_items"); execSQL("DELETE FROM wear_logs")
        execSQL("DELETE FROM outfit_items"); execSQL("DELETE FROM outfits")
        execSQL("DELETE FROM garment_photos"); execSQL("DELETE FROM garment_states"); execSQL("DELETE FROM garments")
        root.getJSONArray("garments").forEachObject { g ->
            val photos = g.optJSONArray("photos")?.objects()?.map { p -> GarmentPhoto(p.str("id"), p.str("garmentId"), p.str("relativePath"), p.optInt("orderIndex"), p.optBoolean("isPrimary"), p.str("sha256"), p.optString("tryOnQuality", "仅作衣橱展示")) } ?: emptyList()
            upsertGarment(Garment(g.str("id"), g.str("name"), g.str("category"), g.optString("colors"), g.optString("pattern"), g.optString("material"), g.optString("seasons"), g.optString("occasions"), g.optString("styleTags"), g.optString("brand"), g.optString("purchaseDate"), g.optLong("priceCents"), g.optString("size"), g.optString("comfortNote"), g.optString("careNote"), GarmentStatus.valueOf(g.optString("status", "AVAILABLE")), g.optInt("wearCount"), g.optLongOrNull("lastWornAt"), g.optLong("createdAt"), g.optLong("updatedAt"), photos))
        }
        root.optJSONArray("outfits")?.forEachObject { o -> saveOutfit(Outfit(o.str("id"), o.str("name"), o.str("scene"), o.optInt("rating"), o.optBoolean("favorite"), o.optLong("createdAt"), o.optJSONArray("items")?.objects()?.map { i -> OutfitItem(i.str("garmentId"), i.optDouble("x").toFloat(), i.optDouble("y").toFloat(), i.optDouble("scale", 1.0).toFloat(), i.optDouble("rotation").toFloat(), i.optInt("zIndex")) } ?: emptyList())) }
        root.optJSONArray("wearLogs")?.forEachObject { l -> addWearLog(WearLog(l.str("id"), l.optLong("wornAt"), l.str("scene"), l.optString("weatherNote"), l.optInt("satisfaction", 3), l.optInt("comfort", 3), l.optStringOrNull("photoPath"), l.optStringOrNull("outfitId"), l.optJSONArray("garmentIds")?.strings() ?: emptyList(), l.optLong("createdAt"))) }
        // addWearLog updates counters as part of normal app operation. A restore must reproduce
        // the exported snapshot exactly rather than counting the imported logs a second time.
        root.getJSONArray("garments").forEachObject { g ->
            execSQL(
                "UPDATE garments SET wear_count=?,last_worn_at=? WHERE id=?",
                arrayOf(g.optInt("wearCount"), g.optLongOrNull("lastWornAt"), g.str("id"))
            )
        }
    }

    private fun Garment.values() = ContentValues().apply {
        put("id", id); put("name", name); put("category", category); put("colors", colors); put("pattern", pattern)
        put("material", material); put("seasons", seasons); put("occasions", occasions); put("style_tags", styleTags)
        put("brand", brand); put("purchase_date", purchaseDate); put("price_cents", priceCents); put("size", size)
        put("comfort_note", comfortNote); put("care_note", careNote); put("status", status.name); put("wear_count", wearCount)
        put("last_worn_at", lastWornAt); put("created_at", createdAt); put("updated_at", updatedAt)
    }
    private fun GarmentPhoto.values() = ContentValues().apply {
        put("id", id); put("garment_id", garmentId); put("relative_path", relativePath); put("order_index", orderIndex)
        put("is_primary", if (isPrimary) 1 else 0); put("sha256", sha256); put("tryon_quality", tryOnQuality)
    }
    private fun Cursor.garment(photos: List<GarmentPhoto>) = Garment(str("id"), str("name"), str("category"), str("colors"), str("pattern"), str("material"), str("seasons"), str("occasions"), str("style_tags"), str("brand"), str("purchase_date"), lng("price_cents"), str("size"), str("comfort_note"), str("care_note"), GarmentStatus.valueOf(str("status")), int("wear_count"), nullableLong("last_worn_at"), lng("created_at"), lng("updated_at"), photos)
    private fun Cursor.photo() = GarmentPhoto(str("id"), str("garment_id"), str("relative_path"), int("order_index"), int("is_primary") == 1, str("sha256"), str("tryon_quality"))
    private fun Cursor.job() = TryOnJob(str("id"), str("model"), nullableString("provider_task_id"), JobStatus.valueOf(str("status")), str("person_path"), nullableString("top_path"), nullableString("bottom_path"), nullableString("coarse_path"), JSONArray(str("shoe_paths")).strings(), nullableString("result_path"), nullableString("error_code"), nullableString("error_message"), str("key_fingerprint"), lng("created_at"), lng("updated_at"))

    companion object { const val DB_NAME = "wardrobe.db"; const val DB_VERSION = 1 }
}

private fun Cursor.idx(name: String) = getColumnIndexOrThrow(name)
private fun Cursor.str(name: String) = getString(idx(name))
private fun Cursor.int(name: String) = getInt(idx(name))
private fun Cursor.lng(name: String) = getLong(idx(name))
private fun Cursor.flt(name: String) = getFloat(idx(name))
private fun Cursor.nullableString(name: String) = if (isNull(idx(name))) null else getString(idx(name))
private fun Cursor.nullableLong(name: String) = if (isNull(idx(name))) null else getLong(idx(name))
private inline fun SQLiteDatabase.transaction(block: SQLiteDatabase.() -> Unit) { beginTransaction(); try { block(); setTransactionSuccessful() } finally { endTransaction() } }
private inline fun <T> SQLiteDatabase.transactionResult(block: SQLiteDatabase.() -> T): T { beginTransaction(); return try { block().also { setTransactionSuccessful() } } finally { endTransaction() } }
private inline fun JSONArray.forEachObject(block: (JSONObject) -> Unit) { for (i in 0 until length()) block(getJSONObject(i)) }
private fun JSONArray.objects() = buildList { for (i in 0 until length()) add(getJSONObject(i)) }
private fun JSONArray.strings() = buildList { for (i in 0 until length()) add(getString(i)) }
private fun JSONObject.str(name: String) = getString(name)
private fun JSONObject.optLongOrNull(name: String) = if (!has(name) || isNull(name)) null else getLong(name)
private fun JSONObject.optStringOrNull(name: String) = if (!has(name) || isNull(name) || optString(name).isBlank()) null else optString(name)
