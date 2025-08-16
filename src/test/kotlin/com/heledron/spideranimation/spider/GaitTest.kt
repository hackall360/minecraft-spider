package com.heledron.spideranimation.spider

import com.heledron.spideranimation.spider.configuration.Gait
import com.heledron.spideranimation.spider.configuration.LerpGait
import com.heledron.spideranimation.utilities.SplitDistance
import kotlin.test.Test
import kotlin.test.assertEquals

class GaitTest {
    @Test
    fun `lerp gait interpolates body height and trigger zone`() {
        val start = LerpGait(1.0, SplitDistance(0.5, 1.0))
        val end = LerpGait(3.0, SplitDistance(1.5, 2.0))
        start.lerp(end, 0.5)
        assertEquals(2.0, start.bodyHeight, 1e-6)
        assertEquals(1.0, start.triggerZone.horizontal, 1e-6)
        assertEquals(1.5, start.triggerZone.vertical, 1e-6)
    }

    @Test
    fun `scaling gait updates related properties`() {
        val gait = Gait.defaultWalk()
        val originalSpeed = gait.maxSpeed
        val originalLift = gait.legLiftHeight
        val originalComfort = gait.comfortZone.vertical
        gait.scale(2.0)
        assertEquals(originalSpeed * 2, gait.maxSpeed, 1e-6)
        assertEquals(originalLift * 2, gait.legLiftHeight, 1e-6)
        assertEquals(originalComfort * 2, gait.comfortZone.vertical, 1e-6)
    }
}
