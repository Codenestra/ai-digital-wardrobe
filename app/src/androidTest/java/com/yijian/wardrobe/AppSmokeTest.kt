package com.yijian.wardrobe

import androidx.compose.ui.test.assertIsDisplayed
import androidx.compose.ui.test.assertCountEquals
import androidx.compose.ui.test.junit4.createAndroidComposeRule
import androidx.compose.ui.test.onNodeWithText
import androidx.compose.ui.test.onAllNodesWithText
import androidx.compose.ui.test.onAllNodesWithTag
import androidx.compose.ui.test.performClick
import androidx.compose.ui.test.performScrollTo
import android.Manifest
import androidx.test.platform.app.InstrumentationRegistry
import androidx.test.ext.junit.runners.AndroidJUnit4
import org.junit.Assert.assertTrue
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith

@RunWith(AndroidJUnit4::class)
class AppSmokeTest {
    @get:Rule val compose = createAndroidComposeRule<MainActivity>()

    @Test fun threeEntryNavigationIsVisible() {
        compose.onAllNodesWithText("衣橱")[0].assertIsDisplayed()
        compose.onNodeWithText("造型间").assertIsDisplayed()
        compose.onNodeWithText("衣迹").assertIsDisplayed()
    }

    @Test fun closetHasOnlyOneAddGarmentAction() {
        compose.onAllNodesWithText("衣橱")[1].performClick()
        compose.waitForIdle()
        compose.onAllNodesWithTag("add-garment-fab").assertCountEquals(1)
    }

    @Test fun studioOffersAlbumAndCameraAnalysis() {
        compose.onNodeWithText("造型间").performClick()
        compose.waitForIdle()
        compose.onNodeWithText("相册分析").performScrollTo().assertIsDisplayed()
        compose.onNodeWithText("拍照分析").performScrollTo().assertIsDisplayed()
    }

    @Test fun cameraPermissionIsDeclared() {
        val context = InstrumentationRegistry.getInstrumentation().targetContext
        val info = context.packageManager.getPackageInfo(context.packageName, android.content.pm.PackageManager.GET_PERMISSIONS)
        assertTrue(info.requestedPermissions.orEmpty().contains(Manifest.permission.CAMERA))
    }
}
