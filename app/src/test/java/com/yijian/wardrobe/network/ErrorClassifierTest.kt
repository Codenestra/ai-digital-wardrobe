package com.yijian.wardrobe.network

import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test

class ErrorClassifierTest {
    @Test fun exactFreeTierCodeIsPreserved() {
        val error = parsedError(403, "{\"code\":\"AllocationQuota.FreeTierOnly\",\"message\":\"Free allocated quota exceeded\"}")
        assertEquals("AllocationQuota.FreeTierOnly", error.code)
        assertTrue(error.message!!.contains("免费额度"))
    }

    @Test fun rateLimitDoesNotBecomeQuotaLock() {
        val error = parsedError(429, "{\"code\":\"Throttling.RateQuota\",\"message\":\"too many requests\"}")
        assertFalse(error.code == "AllocationQuota.FreeTierOnly")
        assertTrue(error.message!!.contains("频繁"))
    }

    @Test fun workspaceErrorIsActionableAndSecretFree() {
        val secret = "sk-test-secret-never-echo"
        val error = parsedError(403, "{\"code\":\"Endpoint.AccessDenied\",\"message\":\"workspace mismatch $secret\"}")
        assertTrue(error.message!!.contains("Workspace"))
        assertFalse(error.message!!.contains(secret))
    }

    @Test fun unsupportedImageIsTranslatedWithoutProviderLeak() {
        val error = parsedError(400, "{\"message\":\"You have uploaded an unsupported image\"}")
        assertTrue(error.message!!.contains("无法解析测试图片"))
        assertFalse(error.message!!.contains("You have uploaded"))
    }

    @Test fun unpurchasedModelIsTranslatedToActionableChinese() {
        val error = parsedError(403, "{\"code\":\"AccessDenied.Unpurchased\",\"message\":\"Access to model denied\"}")
        assertEquals("AccessDenied.Unpurchased", error.code)
        assertEquals("当前业务空间尚未开通该模型，请先在百炼控制台申请或开通对应模型权限", error.message)
    }
}
