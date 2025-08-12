package com.heledron.spideranimation.spider.configuration

import com.heledron.spideranimation.spider.presets.AnimatedPalettes
import com.heledron.spideranimation.spider.presets.SpiderTorsoModels
import com.heledron.spideranimation.utilities.DisplayModel
import net.minecraft.world.phys.Vec3

class SegmentPlan(
    var length: Double,
    var initDirection: Vec3,
    var model: DisplayModel = DisplayModel(listOf())
) {
    fun clone() = SegmentPlan(length, initDirection.clone(), model.clone())
}

class LegPlan(
    var attachmentPosition: Vec3,
    var restPosition: Vec3,
    var segments: List<SegmentPlan>,
)

class BodyPlan {
    var scale = 1.0
    var legs = emptyList<LegPlan>()

    var bodyModel = SpiderTorsoModels.EMPTY.model.clone()

    var eyePalette = AnimatedPalettes.CYAN_EYES.palette
    var blinkingPalette = AnimatedPalettes.CYAN_BLINKING_LIGHTS.palette

    fun scale(scale: Double) {
        this.scale *= scale
        bodyModel.scale(scale.toFloat())
        legs.forEach {
            it.attachmentPosition = it.attachmentPosition.scale(scale)
            it.restPosition = it.restPosition.scale(scale)
            it.segments.forEach { segment ->
                segment.length *= scale
                segment.model.scale(scale.toFloat())
            }
        }
    }
}