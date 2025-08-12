package com.heledron.spideranimation.spider.body

import com.heledron.spideranimation.utilities.ChainSegment
import com.heledron.spideranimation.utilities.KinematicChain
import com.heledron.spideranimation.spider.Spider
import com.heledron.spideranimation.spider.configuration.LegPlan
import com.heledron.spideranimation.utilities.*
import net.minecraft.core.BlockPos
import net.minecraft.world.phys.Vec3
import org.joml.Quaternionf
import kotlin.math.ceil
import kotlin.math.floor

class Leg(
    val spider: Spider,
    var legPlan: LegPlan
) {
    // memo
    lateinit var triggerZone: SplitDistanceZone; private set
    lateinit var comfortZone: SplitDistanceZone; private set

    var groundPosition: Vec3? = null; private set
    lateinit var restPosition: Vec3; private set
    lateinit var lookAheadPosition: Vec3; private set
    lateinit var scanStartPosition: Vec3; private set
    lateinit var scanVector: Vec3; private set

    lateinit var attachmentPosition: Vec3; private set

    init {
        updateMemo()
    }

    // state
    var target = locateGround() ?: strandedTarget()
    var endEffector = target.position
    var previousEndEffector = endEffector
    var chain = KinematicChain(Vec3(0.0, 0.0, 0.0), listOf())

    var touchingGround = true; private set
    var isMoving = false; private set
    var timeSinceBeginMove = 0; private set
    var timeSinceStopMove = 0; private set

    var isDisabled = false
    var isPrimary = false
    var canMove = false

    // utils
    val isOutsideTriggerZone: Boolean; get () { return !triggerZone.contains(endEffector) }
    val isUncomfortable: Boolean; get () { return !comfortZone.contains(endEffector) }

    // events
    val onStep = EventEmitter()

    init {
        onStep.listen { timeSinceStopMove = 0 }
    }

    fun isGrounded(): Boolean {
        return touchingGround && !isMoving && !isDisabled
    }

    fun updateMemo() {
        val orientation = spider.gait.scanPivotMode.get(spider)

        val upVector = UP_VECTOR.rotate(orientation)
        val scanStartAxis = upVector.scale(spider.lerpedGait.bodyHeight * 1.6)
        val scanAxis = upVector.scale(-spider.lerpedGait.bodyHeight * 3.5)

        // rest position
        restPosition = legPlan.restPosition
            .add(upVector.scale(-spider.lerpedGait.bodyHeight))
            .rotate(orientation)
            .add(spider.position)

        // trigger zone
        triggerZone = SplitDistanceZone(restPosition, spider.lerpedGait.triggerZone)

        // comfort zone
        // we want the comfort zone to extend above the spider's body
        // and below the rest position
        var comfortZoneCenter = restPosition
        comfortZoneCenter = comfortZoneCenter.setY(restPosition.y.lerp(spider.position.y, .5))
        val comfortZoneSize = SplitDistance(
            horizontal = spider.gait.comfortZone.horizontal,
            vertical = spider.gait.comfortZone.vertical + (spider.position.y - restPosition.y).coerceAtLeast(.0)
        )
        comfortZone = SplitDistanceZone(comfortZoneCenter, comfortZoneSize)

        // lookahead
        lookAheadPosition = lookAheadPosition(restPosition, triggerZone.size.horizontal)

        // scan
        scanStartPosition = lookAheadPosition.add(scanStartAxis)
        scanVector = scanAxis

        // attachment position
        attachmentPosition = legPlan.attachmentPosition.rotate(spider.orientation).add(spider.position)
    }

    fun update() {
        legPlan = spider.options.bodyPlan.legs.getOrNull(spider.body.legs.indexOf(this)) ?: legPlan
        updateMovement()
        chain = chain()
    }

    private fun updateMovement() {
        previousEndEffector = endEffector

        val gait = spider.gait
        var didStep = false

        timeSinceBeginMove += 1
        timeSinceStopMove += 1

        // update target
        val ground = locateGround()
        groundPosition = locateGround()?.position

        if (isDisabled) {
            target = disabledTarget()
        } else {
            if (ground != null) target = ground

            if (!target.isGrounded || !comfortZone.contains(target.position)) {
                target = strandedTarget()
            }
        }

        // inherit parent velocity
        if (!isGrounded()) {
            endEffector = endEffector.add(spider.velocity)
            endEffector = endEffector.rotateAroundY(spider.rotationalVelocity.y.toDouble(), spider.position)
        }

        // resolve ground collision
        if (!touchingGround) {
            val collision = spider.world.resolveCollision(endEffector, DOWN_VECTOR)
            if (collision != null) {
                didStep = true
                touchingGround = true
                endEffector = endEffector.setY(collision.position.y)
            }
        }

        if (isMoving) {
            val legMoveSpeed = gait.legMoveSpeed

            endEffector = endEffector.moveTowards(target.position, legMoveSpeed)

            val targetY = target.position.y + gait.legLiftHeight
                val hDistance = endEffector.horizontalDistance(target.position)
                if (hDistance > gait.legDropDistance) {
                    endEffector = endEffector.setY(endEffector.y.moveTowards(targetY, legMoveSpeed))
                }

            if (endEffector.distanceTo(target.position) < 0.0001) {
                isMoving = false

                touchingGround = touchingGround()
                didStep = touchingGround
            }

        } else {
            canMove = spider.gait.type.canMoveLeg(this)

            if (canMove) {
                isMoving = true
                timeSinceBeginMove = 0
            }
        }

        if (didStep) this.onStep.emit()
    }

    private fun chain(): KinematicChain {
        if (chain.segments.size != legPlan.segments.size) {
            var stride = 0.0
            chain = KinematicChain(attachmentPosition, legPlan.segments.map {
                stride += it.length
                val position = spider.position.add(legPlan.restPosition.normalize().scale(stride))
                ChainSegment(position, it.length, it.initDirection)
            })
        }

        chain.root = attachmentPosition

        if (spider.gait.straightenLegs) {
            val pivot = Quaternionf(spider.gait.legChainPivotMode.get(spider))

            val direction = endEffector.subtract(attachmentPosition)
            val rotation = direction.getRotationAroundAxis(pivot)

            rotation.x += spider.gait.legStraightenRotation
            val orientation = pivot.rotateYXZ(rotation.y, rotation.x, .0f)

            chain.straightenDirection(orientation)
        }

        if (!spider.options.debug.disableFabrik) {
            chain.fabrik(endEffector)

            // the spider might be falling while the leg is still grounded
//            if (endEffector.distance(chain.getEndEffector()) > .3) {
//                endEffector.copy(chain.getEndEffector())
//
//                if (!isMoving) {
//                    println("Updated end effector")
//                    isMoving = true
////                    timeSinceBeginMove = 0
//                }
//
//            }
        }

        return chain
    }

    private fun touchingGround(): Boolean {
        return spider.world.isOnGround(endEffector, DOWN_VECTOR.rotate(spider.orientation))
    }

    private fun lookAheadPosition(restPosition: Vec3, triggerZoneRadius: Double): Vec3 {
        if (!spider.isWalking) return restPosition

        val direction = if (spider.velocity.isZero) spider.forwardDirection() else spider.velocity.normalize()

        var lookAhead = direction.scale(triggerZoneRadius * spider.gait.legLookAheadFraction).add(restPosition)
        lookAhead = lookAhead.rotateAroundY(spider.rotationalVelocity.y.toDouble(), spider.position)
        return lookAhead
    }

    private fun locateGround(): LegTarget? {
        val lookAhead = lookAheadPosition
        val scanLength = scanVector.length()

        fun candidateAllowed(id: Int): Boolean {
            return true
        }

        var id = 0
        val world = spider.world
        fun rayCast(x: Double, z: Double): LegTarget? {
            id += 1

            if (!candidateAllowed(id)) return null

            val start = Vec3(x, scanStartPosition.y, z)
            val hit = world.raycastGround(start, scanVector, scanLength) ?: return null

            return LegTarget(position = hit.location, isGrounded = true, id = id)
        }

        val x = scanStartPosition.x
        val z = scanStartPosition.z

        val mainCandidate = rayCast(x, z)

        if (!spider.gait.legScanAlternativeGround) return mainCandidate

        if (mainCandidate != null) {
            if (mainCandidate.position.y in lookAhead.y - .24 .. lookAhead.y + 1.5) {
                return mainCandidate
            }
        }

        val margin = 2 / 16.0
        val nx = floor(x) - margin
        val nz = floor(z) - margin
        val pz = ceil(z) + margin
        val px = ceil(x) + margin

        val candidates = listOf(
            rayCast(nx, nz), rayCast(nx, z), rayCast(nx, pz),
            rayCast(x, nz),  mainCandidate,  rayCast(x, pz),
            rayCast(px, nz), rayCast(px, z), rayCast(px, pz),
        )

        var preferredPosition = lookAhead

        val frontBlock = lookAhead.add(spider.forwardDirection().scale(1.0))
        val frontBlockPos = BlockPos(frontBlock.x.toInt(), frontBlock.y.toInt(), frontBlock.z.toInt())
        if (!spider.world.getBlockState(frontBlockPos).isAir) preferredPosition = preferredPosition.setY(preferredPosition.y + spider.gait.legScanHeightBias)

        val best = candidates
            .filterNotNull()
            .minByOrNull { it.position.distanceToSqr(preferredPosition) }

        if (best != null && !comfortZone.contains(best.position)) {
            return null
        }

        return best
    }

    private fun strandedTarget(): LegTarget {
        return LegTarget(position = lookAheadPosition, isGrounded = false, id = -1)
    }

    private fun disabledTarget(): LegTarget {
        val upVector = UP_VECTOR.rotate(spider.orientation)

        val target = strandedTarget()
        target.position = target.position.add(upVector.scale(spider.lerpedGait.bodyHeight * .5))

        val minY = (groundPosition?.y ?: -Double.MAX_VALUE) + spider.lerpedGait.bodyHeight * .1
        target.position = target.position.setY(target.position.y.coerceAtLeast(minY))

        return target
    }
}

class LegTarget(
    var position: Vec3,
    val isGrounded: Boolean,
    val id: Int,
)