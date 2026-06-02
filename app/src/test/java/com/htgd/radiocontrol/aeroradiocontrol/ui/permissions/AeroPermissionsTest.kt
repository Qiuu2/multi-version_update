package com.htgd.radiocontrol.aeroradiocontrol.ui.permissions

import android.Manifest
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test

/**
 * Unit tests for [AeroPermissions] — the cold-start permission policy (TASK-AR-010).
 *
 * Runs on a plain JVM, so `Build.VERSION.SDK_INT` reads as 0 and the version
 * gates in [AeroPermissions.optional] take their pre-API-33 / pre-API-31
 * branches. These tests pin the legacy-branch contents and the criticality
 * split; the version-gated additions (granular media, bluetooth runtime,
 * notifications) are exercised on-device and intentionally not asserted here.
 */
class AeroPermissionsTest {

    @Test
    fun `critical set is exactly RECORD_AUDIO`() {
        assertEquals(listOf(Manifest.permission.RECORD_AUDIO), AeroPermissions.critical)
    }

    @Test
    fun `optional set contains location and phone state but never RECORD_AUDIO`() {
        val optional = AeroPermissions.optional
        assertTrue(optional.contains(Manifest.permission.ACCESS_FINE_LOCATION))
        assertTrue(optional.contains(Manifest.permission.READ_PHONE_STATE))
        assertFalse(
            "RECORD_AUDIO must live in critical, not optional",
            optional.contains(Manifest.permission.RECORD_AUDIO),
        )
    }

    @Test
    fun `pre-API-33 optional set uses legacy storage permissions`() {
        val optional = AeroPermissions.optional
        // On a plain JVM (SDK_INT = 0) we take the else branch.
        assertTrue(optional.contains(Manifest.permission.READ_EXTERNAL_STORAGE))
        assertTrue(optional.contains(Manifest.permission.WRITE_EXTERNAL_STORAGE))
        assertFalse(optional.contains(Manifest.permission.READ_MEDIA_AUDIO))
    }

    @Test
    fun `all set is critical first then optional with no duplicates`() {
        val all = AeroPermissions.all
        assertEquals(AeroPermissions.critical.first(), all.first())
        assertEquals(
            "no permission should appear twice",
            all.size,
            all.toSet().size,
        )
        assertTrue(all.containsAll(AeroPermissions.critical))
        assertTrue(all.containsAll(AeroPermissions.optional))
    }

    @Test
    fun `criticalGranted is true only when every critical permission is granted`() {
        assertTrue(AeroPermissions.criticalGranted(mapOf(Manifest.permission.RECORD_AUDIO to true)))
        assertFalse(AeroPermissions.criticalGranted(mapOf(Manifest.permission.RECORD_AUDIO to false)))
        // Missing entry (system did not prompt it) is not a grant.
        assertFalse(AeroPermissions.criticalGranted(emptyMap()))
    }

    @Test
    fun `criticalGranted ignores unrelated optional grants`() {
        val result = mapOf(
            Manifest.permission.ACCESS_FINE_LOCATION to true,
            Manifest.permission.RECORD_AUDIO to false,
        )
        assertFalse(AeroPermissions.criticalGranted(result))
    }
}
