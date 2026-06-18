package com.familyevents.app

import com.familyevents.core.UserId
import com.familyevents.data.PersistedSession
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNull
import org.junit.Test

class SessionCodecTest {
    private fun store(vararg pairs: Pair<String, String?>): (String) -> String? {
        val map = pairs.toMap()
        return { key -> map[key] }
    }

    @Test fun decode_returnsNull_whenUserIdMissing() {
        assertNull(SessionCodec.decode(store()))
    }

    @Test fun decode_returnsNull_whenUserIdBlank() {
        assertNull(SessionCodec.decode(store(SessionCodec.UserIdKey to "  ")))
    }

    @Test fun decode_mapsTokens_whenPresent() {
        val s = SessionCodec.decode(store(
            SessionCodec.UserIdKey to "u1",
            SessionCodec.AccessTokenKey to "a",
            SessionCodec.RefreshTokenKey to "r",
        ))
        assertEquals(UserId("u1"), s?.userId)
        assertEquals("a", s?.accessToken)
        assertEquals("r", s?.refreshToken)
    }

    @Test fun decode_treatsBlankTokensAsNull() {
        val s = SessionCodec.decode(store(
            SessionCodec.UserIdKey to "u1",
            SessionCodec.AccessTokenKey to "",
            SessionCodec.RefreshTokenKey to "   ",
        ))
        assertNull(s?.accessToken)
        assertNull(s?.refreshToken)
    }

    @Test fun encode_null_removesAllKeys() {
        val w = SessionCodec.encode(null)
        assertNull(w[SessionCodec.UserIdKey])
        assertNull(w[SessionCodec.AccessTokenKey])
        assertNull(w[SessionCodec.RefreshTokenKey])
    }

    @Test fun encode_session_writesUserIdAndPassesThroughNullTokens() {
        val w = SessionCodec.encode(PersistedSession(UserId("u1"), accessToken = null, refreshToken = "r"))
        assertEquals("u1", w[SessionCodec.UserIdKey])
        assertNull(w[SessionCodec.AccessTokenKey])
        assertEquals("r", w[SessionCodec.RefreshTokenKey])
    }
}
