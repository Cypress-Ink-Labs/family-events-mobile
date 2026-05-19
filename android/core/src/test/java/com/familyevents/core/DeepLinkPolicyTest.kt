package com.familyevents.core

import org.junit.Assert.assertEquals
import org.junit.Assert.assertNull
import org.junit.Test

class DeepLinkPolicyTest {
    @Test
    fun routesConsumerLinks() {
        assertEquals(DeepLinkTarget.Event(EventId("evt_1")), DeepLinkPolicy.parse("familyevents://event/evt_1"))
        assertEquals(DeepLinkTarget.City(CityId("chi")), DeepLinkPolicy.parse("familyevents://city/chi"))
        assertEquals(DeepLinkTarget.Share(EventId("evt_2")), DeepLinkPolicy.parse("https://familyevents.app/share/evt_2"))
        assertEquals(DeepLinkTarget.ResetPassword("tok_123"), DeepLinkPolicy.parse("familyevents://reset-password?token=tok_123"))
        assertEquals(DeepLinkTarget.Tab("saved"), DeepLinkPolicy.parse("familyevents://tab/saved"))
        assertNull(DeepLinkPolicy.parse("familyevents://event"))
        assertNull(DeepLinkPolicy.parse("https://familyevents.app/settings/profile"))
        assertNull(DeepLinkPolicy.parse("invalid-url"))
    }

    @Test
    fun adminDeepLinksReturnNull() {
        // Admin links were removed from the manifest and policy in this PR
        assertNull(DeepLinkPolicy.parse("familyevents://admin/events"))
        assertNull(DeepLinkPolicy.parse("familyevents://admin/sources"))
        assertNull(DeepLinkPolicy.parse("familyevents://admin/dashboard"))
        assertNull(DeepLinkPolicy.parse("familyevents://admin"))
    }

    @Test
    fun httpsAdminPathReturnsNull() {
        // HTTPS admin paths were removed from the manifest and policy in this PR
        assertNull(DeepLinkPolicy.parse("https://familyevents.app/admin/sources"))
        assertNull(DeepLinkPolicy.parse("https://familyevents.app/admin"))
        assertNull(DeepLinkPolicy.parse("https://familyevents.app/admin/events"))
    }

    @Test
    fun tabLinksForDifferentTabs() {
        assertEquals(DeepLinkTarget.Tab("plan"), DeepLinkPolicy.parse("familyevents://tab/plan"))
        assertEquals(DeepLinkTarget.Tab("explore"), DeepLinkPolicy.parse("familyevents://tab/explore"))
    }

    @Test
    fun eventLinkRequiresId() {
        // Without an ID segment the link is unrecognised
        assertNull(DeepLinkPolicy.parse("familyevents://event"))
        assertNull(DeepLinkPolicy.parse("familyevents://event/"))
    }

    @Test
    fun blankAndInvalidInputReturnNull() {
        assertNull(DeepLinkPolicy.parse(""))
        assertNull(DeepLinkPolicy.parse("   "))
        assertNull(DeepLinkPolicy.parse("not-a-url-at-all"))
    }

    @Test
    fun resetPasswordWithoutTokenReturnsNull() {
        // Token query param is required for a valid reset-password link
        assertNull(DeepLinkPolicy.parse("familyevents://reset-password"))
    }
}
