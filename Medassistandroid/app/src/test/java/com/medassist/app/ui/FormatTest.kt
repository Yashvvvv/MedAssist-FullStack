package com.medassist.app.ui

import com.medassist.app.ui.common.km
import org.junit.Assert.assertEquals
import org.junit.Test

class FormatTest {
    @Test
    fun `distances read like a person would say them`() {
        assertEquals("Under 100 m", 0.0.km())
        assertEquals("Under 100 m", 0.08.km())
        assertEquals("350 m", 0.372.km())
        assertEquals("950 m", 0.999.km())
        assertEquals("1.0 km", 1.0.km())
        assertEquals("12.4 km", 12.43.km())
    }
}
