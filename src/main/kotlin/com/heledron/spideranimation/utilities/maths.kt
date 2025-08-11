package com.heledron.spideranimation.utilities

import net.minecraft.world.phys.Vec3
import org.joml.*
import org.bukkit.util.Vector
import java.lang.Math
import kotlin.math.abs
import kotlin.math.atan2
import kotlin.math.sign
import kotlin.math.sqrt

fun Double.lerp(target: Double, factor: Double): Double {
    return this + (target - this) * factor
}

fun Float.lerp(target: Float, factor: Float): Float {
    return this + (target - this) * factor
}

fun Double.moveTowards(target: Double, speed: Double): Double {
    val distance = target - this
    return if (abs(distance) < speed) target else this + speed * distance.sign
}

fun Float.moveTowards(target: Float, speed: Float): Float {
    val distance = target - this
    return if (abs(distance) < speed) target else this + speed * distance.sign
}

fun Vec3.moveTowards(target: Vec3, constant: Double): Vec3 {
    val diff = target.subtract(this)
    val distance = diff.length()
    return if (distance <= constant) {
        target
    } else {
        this.add(diff.scale(constant / distance))
    }
}

fun Vector3f.moveTowards(target: Vector3f, constant: Float): Vector3f {
    val diff = Vector3f(target).sub(this)
    val distance = diff.length()
    if (distance <= constant) {
        this.set(target)
    } else {
        this.add(diff.mul(constant / distance))
    }
    return this
}

fun Vec3.lerp(target: Vec3, factor: Double): Vec3 {
    return this.add(target.subtract(this).scale(factor))
}

fun Vec3.toVector3d(): Vector3d = Vector3d(this.x, this.y, this.z)

fun Vec3.toVector3f(): Vector3f = Vector3f(this.x.toFloat(), this.y.toFloat(), this.z.toFloat())

fun Vector3f.toVec3(): Vec3 = Vec3(this.x.toDouble(), this.y.toDouble(), this.z.toDouble())

fun Vector3d.toVec3(): Vec3 = Vec3(this.x, this.y, this.z)

fun Vector.toVec3(): Vec3 = Vec3(this.x, this.y, this.z)

fun Vec3.rotateAroundY(angle: Double, origin: Vec3): Vec3 {
    val translated = this.subtract(origin)
    val sin = kotlin.math.sin(angle)
    val cos = kotlin.math.cos(angle)
    val x = translated.x * cos - translated.z * sin
    val z = translated.x * sin + translated.z * cos
    return Vec3(x, translated.y, z).add(origin)
}

fun Vec3.rotate(rotation: Quaterniond): Vec3 {
    val vec = toVector3d().rotate(rotation)
    return Vec3(vec.x, vec.y, vec.z)
}

fun Vec3.rotate(rotation: Quaternionf): Vec3 {
    val vec = toVector3f().rotate(rotation)
    return Vec3(vec.x.toDouble(), vec.y.toDouble(), vec.z.toDouble())
}

fun Vec3.getPitch(): Float {
    val x = this.x
    val y = this.y
    val z = this.z
    val xz = sqrt(x * x + z * z)
    val pitch = atan2(-y, xz)
    return pitch.toFloat()
}

fun Vec3.getYaw(): Float {
    val x = this.x
    val z = this.z
    val yaw = atan2(-x, z)
    return yaw.toFloat()
}


fun Float.yawRadians(): Float {
    return -toRadians(this)
}

fun Float.pitchRadians(): Float {
    return toRadians(this)
}

//fun Quaterniond.rotationToYX(fromDir: Vector3d, toDir: Vector3d): Quaterniond {
//    this.rotationTo(fromDir, toDir)
//    val euler = this.getEulerAnglesYXZ(Vector3d())
//    return this.rotationYXZ(euler.y, euler.x, .0)
//}

fun Quaternionf.getYXZRelative(pivot: Quaternionf): Vector3f {
    val relative = Quaternionf(pivot).difference(this)
    return relative.getEulerAnglesYXZ(Vector3f())
}

fun Vec3.getRotationAroundAxis(pivot: Quaternionf): Vector3f {
    val orientation = Quaternionf().rotationTo(FORWARD_VECTOR.toVector3f(), this.toVector3f())
    return orientation.getYXZRelative(pivot)
}

fun toDegrees(radians: Double): Double {
    return Math.toDegrees(radians)
}

fun toDegrees(radians: Float): Float {
    return Math.toDegrees(radians.toDouble()).toFloat()
}

fun toRadians(degrees: Double): Double {
    return Math.toRadians(degrees)
}

fun toRadians(degrees: Float): Float {
    return Math.toRadians(degrees.toDouble()).toFloat()
}

fun Vec3.verticalDistance(other: Vec3): Double {
    return abs(this.y - other.y)
}

fun Vec3.horizontalDistance(other: Vec3): Double {
    val x = this.x - other.x
    val z = this.z - other.z
    return sqrt(x * x + z * z)
}

fun Vec3.horizontalLength(): Double {
    return sqrt(x * x + z * z)
}

fun List<Vec3>.average(): Vec3 {
    var out = Vec3(0.0, 0.0, 0.0)
    for (vector in this) out = out.add(vector)
    return out.scale(1.0 / this.size)
}

class SplitDistance(
    val horizontal: Double,
    val vertical: Double
) {
    fun clone(): SplitDistance {
        return SplitDistance(horizontal, vertical)
    }

    fun scale(factor: Double): SplitDistance {
        return SplitDistance(horizontal * factor, vertical * factor)
    }

    fun lerp(target: SplitDistance, factor: Double): SplitDistance {
        return SplitDistance(horizontal.lerp(target.horizontal, factor), vertical.lerp(target.vertical, factor))
    }
}

class SplitDistanceZone(
    val center: Vec3,
    val size: SplitDistance
) {
    fun contains(point: Vec3): Boolean {
//        return point.distance(center) <= size.horizontal
    return center.horizontalDistance(point) <= size.horizontal && center.verticalDistance(point) <= size.vertical
    }

    val horizontal: Double; get() = size.horizontal
    val vertical: Double; get() = size.vertical
}


val DOWN_VECTOR; get() = Vec3(0.0, -1.0, 0.0)
val UP_VECTOR; get() = Vec3(0.0, 1.0, 0.0)
val FORWARD_VECTOR; get() = Vec3(0.0, 0.0, 1.0)
val LEFT_VECTOR; get() = Vec3(-1.0, 0.0, 0.0)
val RIGHT_VECTOR; get() = Vec3(1.0, 0.0, 0.0)


