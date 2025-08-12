package com.heledron.spideranimation.spider.misc

import com.heledron.spideranimation.spider.Spider
import com.heledron.spideranimation.spider.SpiderComponent
import com.heledron.spideranimation.spider.body.Leg
import com.heledron.spideranimation.utilities.lookingAtPoint
import net.minecraft.server.level.ServerPlayer

class PointDetector(val spider: Spider) : SpiderComponent {
    var selectedLeg: Leg? = null
    var player: ServerPlayer? = null

    override fun update() {
        val player = player
        selectedLeg = if (player != null) getLeg(player) else null
    }

    private fun getLeg(player: ServerPlayer): Leg? {
        if (spider.world != player.level()) return null

        val eye = player.eyePosition
        val direction = player.lookAngle
        for (leg in spider.body.legs) {
            val lookingAt = lookingAtPoint(eye, direction, leg.endEffector, spider.lerpedGait.bodyHeight * .15)
            if (lookingAt) return leg
        }
        return null
    }
}