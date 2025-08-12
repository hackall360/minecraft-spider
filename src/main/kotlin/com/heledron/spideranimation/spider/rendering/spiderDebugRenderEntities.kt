package com.heledron.spideranimation.spider.rendering

import com.heledron.spideranimation.spider.Spider
import com.heledron.spideranimation.utilities.*
import net.minecraft.world.entity.Display
import net.minecraft.world.level.block.Blocks
import net.minecraft.world.phys.Vec3
import org.joml.Matrix4f
import org.joml.Quaternionf
import org.joml.Vector3f


fun spiderDebugRenderEntities(spider: Spider): RenderEntityGroup {
    val group = RenderEntityGroup()

    val scale = spider.options.bodyPlan.scale.toFloat()

    for ((legIndex, leg) in spider.body.legs.withIndex()) {
        // Render scan bars
        if (spider.options.debug.scanBars) group.add("scanBar" to legIndex, lineRenderEntity(
            level = spider.world,
            position = leg.scanStartPosition,
            vector = leg.scanVector,
            thickness = .05f * scale,
            init = {
                it.setBrightness(Brightness(15, 15))
            },
            update = {
                val block = if (leg.isPrimary) Blocks.GOLD_BLOCK else Blocks.IRON_BLOCK
                it.blockState = block.defaultBlockState()
            }
        ))

        // Render trigger zone
        if (spider.options.debug.triggerZones) group.add("triggerZoneVertical" to legIndex, blockRenderEntity(
            level = spider.world,
            position = leg.triggerZone.center,
            init = {
                it.setTeleportDuration(1)
                it.setInterpolationDuration(1)
                it.setBrightness(Brightness(15, 15))
            },
            update = {
                val block = if (leg.isUncomfortable) Blocks.RED_STAINED_GLASS else Blocks.CYAN_STAINED_GLASS
                it.blockState = block.defaultBlockState()

                val thickness = .07f * scale
                val transform = Matrix4f()
                    .rotate(spider.gait.scanPivotMode.get(spider))
                    .scale(thickness, 2 * leg.triggerZone.vertical.toFloat(), thickness)
                    .translate(-.5f,-.5f,-.5f)

                it.applyTransformationWithInterpolation(transform)
            }
        ))

        // Render trigger zone
        if (spider.options.debug.triggerZones) group.add("triggerZoneHorizontal" to legIndex, blockRenderEntity(
            level = spider.world,
            position = run {
                var pos = leg.triggerZone.center
                pos = pos.setY(leg.target.position.y.coerceIn(pos.y - leg.triggerZone.vertical, pos.y + leg.triggerZone.vertical))
                pos
            },
            init = {
                it.setTeleportDuration(1)
                it.setInterpolationDuration(1)
                it.setBrightness(Brightness(15, 15))
            },
            update = {
                val block = if (leg.isUncomfortable) Blocks.RED_STAINED_GLASS else Blocks.CYAN_STAINED_GLASS
                it.blockState = block.defaultBlockState()

                val size = 2 * leg.triggerZone.horizontal.toFloat()
                val ySize = 0.02f
                val transform = Matrix4f()
                    .rotate(spider.gait.scanPivotMode.get(spider))
                    .scale(size, ySize, size)
                    .translate(-.5f,-.5f,-.5f)

                it.applyTransformationWithInterpolation(transform)
            }
        ))

        // Render end effector
        if (spider.options.debug.endEffectors) group.add("endEffector" to legIndex, blockRenderEntity(
            level = spider.world,
            position = leg.endEffector,
            init = {
                it.setTeleportDuration(1)
                it.setBrightness(Brightness(15, 15))
            },
            update = {
                val size = (if (leg == spider.pointDetector.selectedLeg) .2f else .15f) * scale
                it.transformation = centredTransform(size, size, size)
                it.blockState = when {
                    leg.isDisabled -> Blocks.BLACK_CONCRETE.defaultBlockState()
                    leg.isGrounded() -> Blocks.DIAMOND_BLOCK.defaultBlockState()
                    leg.touchingGround -> Blocks.LAPIS_BLOCK.defaultBlockState()
                    else -> Blocks.REDSTONE_BLOCK.defaultBlockState()
                }
            }
        ))

        // Render target position
        if (spider.options.debug.targetPositions) group.add("targetPosition" to legIndex, blockRenderEntity(
            level = spider.world,
            position = leg.target.position,
            init = {
                it.setTeleportDuration(1)
                it.setBrightness(Brightness(15, 15))

                val size = 0.2f * scale
                it.transformation = centredTransform(size, size, size)
            },
            update = {
                val block = if (leg.target.isGrounded) Blocks.LIME_STAINED_GLASS else Blocks.RED_STAINED_GLASS
                it.blockState = block.defaultBlockState()
            }
        ))
    }

    // Render spider direction
    if (spider.options.debug.orientation) group.add("direction", blockRenderEntity(
        level = spider.world,
        position = spider.position,
        init = {
            it.setTeleportDuration(1)
            it.setInterpolationDuration(1)
            it.setBrightness(Brightness(15, 15))
        },
        update = {
            it.blockState = if (spider.gallop) Blocks.REDSTONE_BLOCK.defaultBlockState() else Blocks.EMERALD_BLOCK.defaultBlockState()

            val size = .1f * scale
            val displacement = 1f * scale
            val transform = Matrix4f()
                .rotate(spider.orientation)
                .translate(FORWARD_VECTOR.toVector3f().mul(displacement))
                .scale(size, size, size)
                .translate(-.5f,-.5f, -.5f)

            it.applyTransformationWithInterpolation(transform)
        }
    ))

    // Render preferred orientation
    if (spider.options.debug.preferredOrientation) {
        fun renderEntity(orientation: Quaternionf, direction: Vec3, thickness: Float, length: Float, block: net.minecraft.world.level.block.Block) = run {
            val mTranslation = Vector3f(-1f, -1f, -1f).add(direction.toVector3f()).mul(.5f)
            val mScale = Vector3f(thickness, thickness, thickness).add(direction.toVector3f().mul(length))

            blockRenderEntity(
                level = spider.world,
                position = spider.position,
                init = {
                    it.blockState = block.defaultBlockState()
                    it.setTeleportDuration(1)
                    it.setInterpolationDuration(1)
                },
                update = {
                    val transform = Matrix4f()
                        .rotate(orientation)
                        .scale(mScale)
                        .translate(mTranslation)

                    it.applyTransformationWithInterpolation(transform)
                }
            )
        }

        val thickness = .025f * scale
        group.add("preferredForwards", renderEntity(spider.preferredOrientation, FORWARD_VECTOR, thickness, 2.0f * scale, Blocks.DIAMOND_BLOCK))
        group.add("preferredRight"   , renderEntity(spider.preferredOrientation, RIGHT_VECTOR  , thickness, 1.0f * scale, Blocks.DIAMOND_BLOCK))
        group.add("preferredUp"      , renderEntity(spider.preferredOrientation, UP_VECTOR     , thickness, 1.0f * scale, Blocks.DIAMOND_BLOCK))
    }


    val normal = spider.body.normal ?: return group
    if (spider.options.debug.legPolygons && normal.contactPolygon != null) {
        val points = normal.contactPolygon//.map { it.toLocation(spider.world)}
        for (i in points.indices) {
            val a = points[i]
            val b = points[(i + 1) % points.size]

            group.add("polygon" to i, lineRenderEntity(
                level = spider.world,
                position = a,
                vector = b.subtract(a),
                thickness = .05f * scale,
                interpolation = 0,
                init = { it.setBrightness(Brightness(15, 15)) },
                update = { it.blockState = Blocks.EMERALD_BLOCK.defaultBlockState() }
            ))
        }
    }

    if (spider.options.debug.centreOfMass && normal.centreOfMass != null) group.add("centreOfMass", blockRenderEntity(
        level = spider.world,
        position = normal.centreOfMass,
        init = {
            it.setTeleportDuration(1)
            it.setBrightness(Brightness(15, 15))

            val size = 0.1f * scale
            it.transformation = centredTransform(size, size, size)
        },
        update = {
            val block = if (normal.normal.horizontalLength() == .0) Blocks.LAPIS_BLOCK else Blocks.REDSTONE_BLOCK
            it.blockState = block.defaultBlockState()
        }
    ))


    if (spider.options.debug.normalForce && normal.centreOfMass != null && normal.origin !== null) group.add("acceleration", lineRenderEntity(
        level = spider.world,
        position = normal.origin,
        vector = normal.centreOfMass.subtract(normal.origin),
        thickness = .02f * scale,
        interpolation = 1,
        init = { it.setBrightness(Brightness(15, 15)) },
        update = {
            val block = if (spider.body.normalAcceleration.isZero) Blocks.BLACK_CONCRETE else Blocks.WHITE_CONCRETE
            it.blockState = block.defaultBlockState()
        }
    ))

    return group
}