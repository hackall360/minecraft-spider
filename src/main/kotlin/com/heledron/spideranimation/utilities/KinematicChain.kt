package com.heledron.spideranimation.utilities

import net.minecraft.world.phys.Vec3
import org.joml.Quaternionf

class KinematicChain(
    var root: Vec3,
    val segments: List<ChainSegment>,
) {
    var maxIterations = 20
    var tolerance = 0.01

    fun fabrik(target: Vec3) {
        for (i in 0 until maxIterations) {
            fabrikForward(target)
            fabrikBackward()
            if (getEndEffector().distanceToSqr(target) < tolerance) {
                break
            }
        }
    }

    fun straightenDirection(rotation: Quaternionf) {
        var position = root
        for (segment in segments) {
            val initDirection = segment.initDirection.rotate(rotation)
            position = position.add(initDirection.scale(segment.length))
            segment.position = position
        }
    }

    fun fabrikForward(newPosition: Vec3) {
        val lastSegment = segments.last()
        lastSegment.position = newPosition
        for (i in segments.size - 1 downTo 1) {
            val previousSegment = segments[i]
            val segment = segments[i - 1]
            segment.position = moveSegment(segment.position, previousSegment.position, previousSegment.length)
        }
    }

    fun fabrikBackward() {
        segments[0].position = moveSegment(segments.first().position, root, segments.first().length)
        for (i in 1 until segments.size) {
            val previousSegment = segments[i - 1]
            val segment = segments[i]
            segment.position = moveSegment(segment.position, previousSegment.position, segment.length)
        }
    }

    fun moveSegment(point: Vec3, pullTowards: Vec3, segment: Double): Vec3 {
        val direction = pullTowards.subtract(point).normalize()
        return pullTowards.subtract(direction.scale(segment))
    }

    fun getEndEffector(): Vec3 {
        return segments.last().position
    }

    fun getVectors(): List<Vec3> {
        return segments.mapIndexed { i, segment ->
            val previous = segments.getOrNull(i - 1)?.position ?: root
            segment.position.subtract(previous)
        }
    }

    fun getRelativeRotations(pivot: Quaternionf): List<Quaternionf> {
        val vectors = getVectors()
        val firstEuler = vectors.first().getRotationAroundAxis(pivot)
        val firstRotation = Quaternionf(pivot).rotateYXZ(firstEuler.y, firstEuler.x, .0f)
        val rotations = vectors.mapIndexed { i, current ->
            val previous = vectors.getOrNull(i - 1) ?: return@mapIndexed firstRotation
            Quaternionf().rotationTo(previous.toVector3f(), current.toVector3f())
        }
        return rotations
    }

    fun getRotations(pivot: Quaternionf): List<Quaternionf> {
        return getRelativeRotations(pivot).apply { cumulateRotations(this) }
    }

    private fun cumulateRotations(rotations: List<Quaternionf>) {
        for (i in 1 until rotations.size) {
            rotations[i].mul(rotations[i - 1])
        }
    }
}

class ChainSegment(
    var position: Vec3,
    var length: Double,
    var initDirection: Vec3,
) {
    fun clone(): ChainSegment {
        return ChainSegment(
            Vec3(position.x, position.y, position.z),
            length,
            Vec3(initDirection.x, initDirection.y, initDirection.z),
        )
    }
}
