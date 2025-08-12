package com.heledron.spideranimation.spider.presets

import com.heledron.spideranimation.spider.configuration.BodyPlan
import com.heledron.spideranimation.spider.configuration.LegPlan
import com.heledron.spideranimation.spider.configuration.SegmentPlan
import com.heledron.spideranimation.spider.configuration.SpiderOptions
import com.heledron.spideranimation.utilities.FORWARD_VECTOR
import net.minecraft.world.level.block.Blocks
import net.minecraft.world.phys.Vec3


private fun equalLength(segmentCount: Int, length: Double): List<SegmentPlan> {
    return List(segmentCount) { SegmentPlan(length, FORWARD_VECTOR) }
}

private fun BodyPlan.addLegPair(root: Vec3, rest: Vec3, segments: List<SegmentPlan>) {
    legs += LegPlan(Vec3(root.x, root.y, root.z), Vec3(rest.x, rest.y, rest.z), segments)
    legs += LegPlan(Vec3(-root.x, root.y, root.z), Vec3(-rest.x, rest.y, rest.z), segments.map { it.clone() })
}

fun biped(segmentCount: Int, segmentLength: Double): SpiderOptions {
    val options = SpiderOptions()
    options.bodyPlan.addLegPair(Vec3(.0, .0, .0), Vec3(1.0, .0, .0), equalLength(segmentCount, 1.0 * segmentLength))
      applyLineLegModel(options.bodyPlan, Blocks.NETHERITE_BLOCK.defaultBlockState())
    return options
}

fun quadruped(segmentCount: Int, segmentLength: Double): SpiderOptions {
    val options = SpiderOptions()
    options.bodyPlan.addLegPair(Vec3(.0, .0, .0), Vec3(0.9,.0, 0.9), equalLength(segmentCount, 0.9 * segmentLength))
    options.bodyPlan.addLegPair(Vec3(.0, .0, .0), Vec3(1.0, .0, -1.1), equalLength(segmentCount, 1.2 * segmentLength))
      applyLineLegModel(options.bodyPlan, Blocks.NETHERITE_BLOCK.defaultBlockState())
    return options
}

fun hexapod(segmentCount: Int, segmentLength: Double): SpiderOptions {
    val options = SpiderOptions()
    options.bodyPlan.addLegPair(Vec3(.0,.0,0.1), Vec3(1.0,.0, 1.1), equalLength(segmentCount, 1.1 * segmentLength))
    options.bodyPlan.addLegPair(Vec3(.0,.0,0.0), Vec3(1.3,.0,-0.3), equalLength(segmentCount, 1.1 * segmentLength))
    options.bodyPlan.addLegPair(Vec3(.0,.0,-.1), Vec3(1.2,.0,-2.0), equalLength(segmentCount, 1.6 * segmentLength))
      applyLineLegModel(options.bodyPlan, Blocks.NETHERITE_BLOCK.defaultBlockState())
    return options
}

fun octopod(segmentCount: Int, segmentLength: Double): SpiderOptions {
    val options = SpiderOptions()
    options.bodyPlan.addLegPair(Vec3(.0,.0,  .1), Vec3(1.0, .0,  1.6), equalLength(segmentCount, 1.1 * segmentLength))
    options.bodyPlan.addLegPair(Vec3(.0,.0,  .0), Vec3(1.3, .0,  0.4), equalLength(segmentCount, 1.0 * segmentLength))
    options.bodyPlan.addLegPair(Vec3(.0,.0, -.1), Vec3(1.3, .0, -0.9), equalLength(segmentCount, 1.1 * segmentLength))
    options.bodyPlan.addLegPair(Vec3(.0,.0, -.2), Vec3(1.1, .0, -2.5), equalLength(segmentCount, 1.6 * segmentLength))
      applyLineLegModel(options.bodyPlan, Blocks.NETHERITE_BLOCK.defaultBlockState())
    return options
}


private fun createRobotSegments(segmentCount: Int, lengthScale: Double) = List(segmentCount) { index ->
    var length = lengthScale.toFloat()
    var initDirection = FORWARD_VECTOR

    if (index == 0) {
        length *= .5f
        initDirection = initDirection.rotateAroundX(Math.PI / 3)
    }

    if (index == 1) length *= .8f

    SegmentPlan(length.toDouble(), initDirection)
}


fun quadBot(segmentCount: Int, segmentLength: Double): SpiderOptions {
    val options = SpiderOptions()
    options.bodyPlan.bodyModel = SpiderTorsoModels.FLAT.model.clone()
    options.bodyPlan.addLegPair(root = Vec3(.2,-.2 - .15, .2), rest = Vec3(1.3 * 1.0,.0, 1.0), createRobotSegments(segmentCount, .9 * .7 * segmentLength))
    options.bodyPlan.addLegPair(root = Vec3(.2,-.2 - .15,-.2), rest = Vec3(1.3 * 1.1,.0,-1.2), createRobotSegments(segmentCount, 1.2 * .7 * segmentLength))
    applyMechanicalLegModel(options.bodyPlan)
    return options
}

fun hexBot(segmentCount: Int, segmentLength: Double): SpiderOptions {
    val options = SpiderOptions()
    options.bodyPlan.bodyModel = SpiderTorsoModels.FLAT.model.clone()
    options.bodyPlan.addLegPair(root = Vec3(.2,-.2 - .15, .2), rest = Vec3(1.3 * 1.0,.0, 1.3), createRobotSegments(segmentCount, 1.1 * .7 * segmentLength))
    options.bodyPlan.addLegPair(root = Vec3(.2,-.2 - .15, .0), rest = Vec3(1.3 * 1.2,.0,-0.1), createRobotSegments(segmentCount, 1.1 * .7 * segmentLength))
    options.bodyPlan.addLegPair(root = Vec3(.2,-.2 - .15,-.2), rest = Vec3(1.3 * 1.1,.0,-1.6), createRobotSegments(segmentCount, 1.3 * .7 * segmentLength))
    applyMechanicalLegModel(options.bodyPlan)
    return options
}

fun octoBot(segmentCount: Int, segmentLength: Double): SpiderOptions {
    val options = SpiderOptions()
    options.bodyPlan.bodyModel = SpiderTorsoModels.FLAT.model.clone()
    options.bodyPlan.addLegPair(root = Vec3(.2,-.2 - .15, .3), rest = Vec3(1.3 * 1.0,.0, 1.3), createRobotSegments(segmentCount, 1.1 * .7 * segmentLength))
    options.bodyPlan.addLegPair(root = Vec3(.2,-.2 - .15, .1), rest = Vec3(1.3 * 1.2,.0, 0.5), createRobotSegments(segmentCount, 1.0 * .7 * segmentLength))
    options.bodyPlan.addLegPair(root = Vec3(.2,-.2 - .15, .1), rest = Vec3(1.3 * 1.2,.0,-0.7), createRobotSegments(segmentCount, 1.1 * .7 * segmentLength))
    options.bodyPlan.addLegPair(root = Vec3(.2,-.2 - .15,-.3), rest = Vec3(1.3 * 1.1,.0,-1.6), createRobotSegments(segmentCount, 1.3 * .7 * segmentLength))
    applyMechanicalLegModel(options.bodyPlan)
    return options
}