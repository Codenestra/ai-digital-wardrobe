package com.yijian.wardrobe

import androidx.test.core.app.ApplicationProvider
import androidx.test.ext.junit.runners.AndroidJUnit4
import com.yijian.wardrobe.data.Garment
import com.yijian.wardrobe.data.GarmentStatus
import com.yijian.wardrobe.data.Outfit
import com.yijian.wardrobe.data.OutfitItem
import com.yijian.wardrobe.data.WardrobeDatabase
import com.yijian.wardrobe.data.WearLog
import org.junit.After
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test
import org.junit.runner.RunWith

@RunWith(AndroidJUnit4::class)
class DatabaseFlowTest {
    private val context = ApplicationProvider.getApplicationContext<android.content.Context>()
    private val testDbName = "wardrobe-instrumented-test.db"
    private val db = WardrobeDatabase(context, testDbName)

    @After fun clean() { db.close(); context.deleteDatabase(testDbName) }

    @Test fun garmentOutfitWearAndStatusPersistAcrossReopen() {
        val top = Garment(name = "奶油针织", category = "上装", colors = "奶油白", priceCents = 12000)
        val bottom = Garment(name = "深色长裤", category = "下装", colors = "深灰")
        db.upsertGarment(top); db.upsertGarment(bottom)
        db.saveOutfit(Outfit(name = "通勤组合", scene = "通勤", items = listOf(OutfitItem(top.id), OutfitItem(bottom.id))))
        db.addWearLog(WearLog(wornAt = System.currentTimeMillis(), scene = "通勤", garmentIds = listOf(top.id, bottom.id)))
        db.upsertGarment(db.garment(top.id)!!.copy(status = GarmentStatus.WASHING))

        val reopened = WardrobeDatabase(context, testDbName)
        assertEquals(2, reopened.listGarments().size)
        assertEquals(1, reopened.listOutfits().size)
        assertEquals(1, reopened.listWearLogs().size)
        assertEquals(1, reopened.garment(top.id)!!.wearCount)
        assertFalse(reopened.garment(top.id)!!.status.wearable)
        assertTrue(reopened.garment(bottom.id)!!.status.wearable)
        val exported = reopened.exportJson()
        reopened.replaceFromJson(exported)
        assertEquals(1, reopened.garment(top.id)!!.wearCount)
        assertEquals(1, reopened.listWearLogs().size)
        reopened.close()
    }

    @Test fun deletingGarmentRemovesLinksAndKeepsHistoricalContainers() {
        val garment = Garment(id = "delete-me", name = "待删上衣", category = "上装")
        db.upsertGarment(garment)
        db.saveOutfit(Outfit(id = "linked-outfit", name = "关联造型", scene = "通勤", items = listOf(OutfitItem(garment.id))))
        db.addWearLog(WearLog(id = "linked-log", wornAt = System.currentTimeMillis(), scene = "通勤", garmentIds = listOf(garment.id)))

        assertEquals(1 to 1, db.deleteGarment(garment.id))
        assertEquals(null, db.garment(garment.id))
        assertEquals(1, db.listWearLogs().size)
        assertTrue(db.listWearLogs().single().garmentIds.isEmpty())
        assertTrue(db.listOutfits().single().items.isEmpty())
    }
}
