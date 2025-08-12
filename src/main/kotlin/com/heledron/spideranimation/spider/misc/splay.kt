package com.heledron.spideranimation.spider.misc

import com.heledron.spideranimation.AppState
import com.heledron.spideranimation.utilities.*
import net.minecraft.world.entity.Display.BlockDisplay
import net.minecraft.world.phys.Vec3
import org.joml.Transformation
import org.joml.Quaternionf
import org.joml.Vector3f
import com.mojang.math.Transformation as MojangTransformation
import java.io.Closeable
import kotlin.random.Random

private fun Transformation.lerp(newTransform: Transformation, lerpAmount: Float): Transformation {

    this.translation.lerp(newTransform.translation, lerpAmount)
    this.scale.lerp(newTransform.scale, lerpAmount)
    this.leftRotation.slerp(newTransform.leftRotation, lerpAmount)
    this.rightRotation.slerp(newTransform.rightRotation, lerpAmount)

    return this
}

fun Transformation.clone() = Transformation(
    Vector3f(translation),
    Quaternionf(leftRotation),
    Vector3f(scale),
    Quaternionf(rightRotation)
)


fun splay() {
    val spider = AppState.spider ?: return

    // detach and get entities
    val entities = mutableListOf<BlockDisplay>()
    for ((id, entity) in AppState.renderer.rendered.toList()) {
        if (entity !is BlockDisplay) continue
        entities += entity
        AppState.renderer.detach(id)
        AppState.closeables += Closeable { entity.discard() }
    }

    AppState.spider = null


    val pieces = mutableListOf<BlockDisplayModelPiece>()
    for (piece in spider.options.bodyPlan.bodyModel.pieces) {
        pieces += piece
    }

    for ((legIndex, leg) in spider.body.legs.withIndex()) {
        for ((segmentIndex, segment) in leg.chain.segments.withIndex()) {
            val model = spider.options.bodyPlan.legs[legIndex].segments[segmentIndex].model
            for (piece in model.pieces) pieces += piece
        }
    }

    for ((i, entity) in entities.withIndex().shuffled()) {
        val offset = entity.position().subtract(spider.position)

        // normalize position
        entity.setTeleportDuration(0)
        entity.setInterpolationDuration(0)
        entity.setInterpolationDelay(100)

        val transformation = Transformation(entity.transformation.matrix)
        runLater(2) {
            entity.absMoveTo(spider.position.x, spider.position.y, spider.position.z)

            transformation.translation.add(offset.toVector3f())
            entity.transformation = MojangTransformation(matrixFromTransform(transformation))
            entity.interpolationStartDelta = 0
        }

        runLater(3L + i / 4) {
            splay(entity)
        }
    }
}

private fun splay(entity: BlockDisplay) {
    val targetTransformation = Transformation(entity.transformation.matrix)
    targetTransformation.translation.apply {
        normalize().mul(Random.nextDouble(1.0, 3.0).toFloat())
    }
    targetTransformation.scale.set(.35f)
    targetTransformation.leftRotation.identity()
    targetTransformation.rightRotation.identity()

    entity.setInterpolationDuration(1)
    entity.setInterpolationDelay(0)

    var lerpAmount = .0f
    interval(0, 1) {
        lerpAmount = lerpAmount.moveTowards(1f, .1f)

        val eased = lerpAmount * lerpAmount * (3 - 2 * lerpAmount)
        val current = Transformation(entity.transformation.matrix)
        val result = current.lerp(targetTransformation, eased)
        entity.transformation = MojangTransformation(matrixFromTransform(result))
        entity.setInterpolationDelay(0)
        entity.interpolationStartDelta = 0

        if (lerpAmount >= 1) it.close()
    }
}
