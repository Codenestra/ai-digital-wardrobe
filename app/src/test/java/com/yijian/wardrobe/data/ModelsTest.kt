package com.yijian.wardrobe.data

import org.junit.Assert.assertEquals
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Test

class ModelsTest {
    @Test fun costPerWearRequiresPriceAndUsage() {
        assertNull(Garment(name = "A", category = "上装", priceCents = 10000).costPerWearCents)
        assertEquals(2500L, Garment(name = "A", category = "上装", priceCents = 10000, wearCount = 4).costPerWearCents)
    }

    @Test fun unavailableStatusesAreExcludedFromWearablePool() {
        assertTrue(GarmentStatus.entries.filterNot { it == GarmentStatus.AVAILABLE }.none { it.wearable })
    }
}
