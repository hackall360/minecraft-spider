package com.heledron.spideranimation.spider

import com.heledron.spideranimation.spider.body.Leg
import com.heledron.spideranimation.spider.body.SpiderBody
import com.heledron.spideranimation.spider.configuration.SpiderOptions
import com.heledron.spideranimation.spider.misc.*
import com.heledron.spideranimation.spider.rendering.SpiderRenderer
import com.heledron.spideranimation.utilities.*
import net.minecraft.world.level.Level
import net.minecraft.world.phys.Vec3
import org.joml.Quaternionf
import org.joml.Vector3f
import java.io.Closeable

interface SpiderComponent : Closeable {
    fun update() {}
    fun render() {}
    override fun close() {}
}

class Spider(
    val world: Level,
    var position: Vec3,
    var orientation: Quaternionf,
    val options: SpiderOptions
): Closeable {
    companion object {
        fun fromPosition(level: Level, position: Vec3, orientation: Quaternionf, options: SpiderOptions): Spider {
            return Spider(level, position, orientation, options)
        }
    }

    fun forwardDirection(): Vec3 = FORWARD_VECTOR.rotate(orientation)

    val gait get() = if (gallop) options.gallopGait else options.walkGait

    // memo
    var lerpedGait = options.walkGait.stationary.clone()
    var preferredPitch = orientation.getEulerAnglesYXZ(Vector3f()).x
    var preferredRoll = orientation.getEulerAnglesYXZ(Vector3f()).z
    var preferredOrientation = Quaternionf(orientation)

    // params
    var gallop = false
    var showDebugVisuals = false

    // state
    var isWalking = false
    var isRotatingYaw = false
//    var isRotatingPitch = false

    var velocity = Vec3(0.0, 0.0, 0.0)
    val rotationalVelocity = Vector3f(0f,0f,0f)

    fun accelerateRotation(axis: Vec3, angle: Float) {
        val acceleration = Quaternionf().rotateAxis(angle, axis.toVector3f())
        val oldVelocity = Quaternionf().rotationYXZ(rotationalVelocity.y, rotationalVelocity.x, rotationalVelocity.z)

        val rotVelocity = acceleration.mul(oldVelocity)

        val rotEuler = rotVelocity.getEulerAnglesYXZ(Vector3f())
        rotationalVelocity.set(rotEuler)
    }

    // components
    val body = SpiderBody(this)
    val tridentDetector = TridentHitDetector(this)
    val cloak = Cloak(this)
    val sound = SoundsAndParticles(this)
    val mount = Mountable(this)
    val pointDetector = PointDetector(this)

    var behaviour: SpiderComponent = StayStillBehaviour(this)
    set (value) {
        field.close()
        field = value
    }

    var renderer: SpiderComponent = SpiderRenderer(this)
    set (value) {
        field.close()
        field = value
    }

    override fun close() {
        getComponents().forEach { it.close() }
    }

    fun teleport(newPosition: Vec3) {
        val displacement = newPosition.subtract(position)
        position = newPosition

        for (leg in body.legs) {
            leg.groundPosition = leg.groundPosition?.add(displacement)
            leg.restPosition = leg.restPosition.add(displacement)
            leg.lookAheadPosition = leg.lookAheadPosition.add(displacement)
            leg.scanStartPosition = leg.scanStartPosition.add(displacement)
            leg.attachmentPosition = leg.attachmentPosition.add(displacement)
            leg.triggerZone = SplitDistanceZone(leg.triggerZone.center.add(displacement), leg.triggerZone.size)
            leg.comfortZone = SplitDistanceZone(leg.comfortZone.center.add(displacement), leg.comfortZone.size)

            leg.target.position = leg.target.position.add(displacement)
            leg.endEffector = leg.endEffector.add(displacement)
            leg.previousEndEffector = leg.previousEndEffector.add(displacement)

            leg.chain.root = leg.chain.root.add(displacement)
            leg.chain.segments.forEach { it.position = it.position.add(displacement) }
        }

        renderer.render()
    }

    fun getComponents() = iterator<SpiderComponent> {
        yield(behaviour)
        yield(cloak)
        yield(body)
        yield(tridentDetector)
        yield(sound)
        yield(mount)
        yield(pointDetector)
        yield(renderer)
    }

    fun update() {
        updatePreferredAngles()
        updateLerpedGait()
        val components = getComponents().asSequence().toList()
        for (component in components) component.update()
        for (component in components) component.render()
    }

    private fun updateLerpedGait() {
        if (isRotatingYaw) {
            lerpedGait = gait.moving.clone()
            return
        }

//        if (!isWalking && !velocity.isZero) {
//            gaitLerped = moveGait.movingButNotWalking.clone()
//            return
//        }

        val speedFraction = velocity.length() / gait.maxSpeed
        lerpedGait = gait.stationary.clone().lerp(gait.moving, speedFraction)
    }

    private fun updatePreferredAngles() {
        val currentEuler = orientation.getEulerAnglesYXZ(Vector3f())

        if (gait.disableAdvancedRotation) {
            preferredPitch = .0f
            preferredRoll = .0f
            preferredOrientation = Quaternionf().rotationYXZ(currentEuler.y, .0f, .0f)
            return
        }

        fun getPos(leg: Leg): Vec3 {
            return leg.groundPosition ?: leg.restPosition
        }

        val frontLeft  = getPos(body.legs.getOrNull(0) ?: return)
        val frontRight = getPos(body.legs.getOrNull(1) ?: return)
        val backLeft  = getPos(body.legs.getOrNull(body.legs.size - 2) ?: return)
        val backRight = getPos(body.legs.getOrNull(body.legs.size - 1) ?: return)

        val forwardLeftVec = frontLeft.subtract(backLeft)
        val forwardRightVec = frontRight.subtract(backRight)
        val forward = listOf(forwardLeftVec, forwardRightVec).average()

        var sideways = Vec3(0.0,0.0,0.0)
        for (i in 0 until body.legs.size step 2) {
            val left = body.legs.getOrNull(i) ?: continue
            val right = body.legs.getOrNull(i + 1) ?: continue

            sideways = sideways.add(getPos(right).subtract(getPos(left)))
        }

        preferredPitch = forward.getPitch().lerp(preferredPitch, gait.preferredRotationLerpFraction)
        preferredRoll = sideways.getPitch().lerp(preferredRoll, gait.preferredRotationLerpFraction)

        if (preferredPitch < gait.preferLevelBreakpoint) preferredPitch *= 1 - gait.preferLevelBias
        if (preferredRoll < gait.preferLevelBreakpoint) preferredRoll *= 1 - gait.preferLevelBias


        preferredOrientation = Quaternionf().rotationYXZ(currentEuler.y, preferredPitch, preferredRoll)
    }
}

