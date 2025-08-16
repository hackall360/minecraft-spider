package com.heledron.spideranimation.utilities

import net.minecraft.world.phys.Vec3
import kotlin.test.Test
import kotlin.test.assertEquals

class RGBTest {
    @Test
    fun `withBrightness scales components`() {
        val color = RGB(100, 50, 0)
        val dimmed = color.withBrightness(7)
        val expectedFactor = 7.0 / 15.0
        assertEquals((100 * expectedFactor).toInt(), dimmed.r)
        assertEquals((50 * expectedFactor).toInt(), dimmed.g)
        assertEquals((0 * expectedFactor).toInt(), dimmed.b)
    }

    @Test
    fun `conversion to and from Vec3 preserves values`() {
        val color = RGB(10, 20, 30)
        val vec = color.toVec3()
        assertEquals(Vec3(10.0, 20.0, 30.0), vec)
        assertEquals(color, vec.toRGB())
    }
}
