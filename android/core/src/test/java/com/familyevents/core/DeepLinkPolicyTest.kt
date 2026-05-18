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
        assertNull(DeepLinkPolicy.parse("familyevents://saved"))
        assertNull(DeepLinkPolicy.parse("invalid-url"))
    }
}
