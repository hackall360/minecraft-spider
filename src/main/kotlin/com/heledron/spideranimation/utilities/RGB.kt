package com.heledron.spideranimation.utilities

import net.minecraft.world.phys.Vec3
import kotlin.math.pow
import kotlin.math.sqrt

/**
 * Simple RGB container used instead of the limited [net.minecraft.world.item.DyeColor] palette.
 */
data class RGB(val r: Int, val g: Int, val b: Int) {
    /** Scale the colour to the provided Minecraft brightness level (0-15). */
    fun withBrightness(brightness: Int): RGB {
        val factor = brightness.toDouble() / 15.0
        return RGB((r * factor).toInt(), (g * factor).toInt(), (b * factor).toInt())
    }

    /** Euclidean distance to another colour. */
    fun distanceTo(other: RGB): Double {
        return sqrt(
            (r - other.r).toDouble().pow(2) +
            (g - other.g).toDouble().pow(2) +
            (b - other.b).toDouble().pow(2)
        )
    }
}

fun RGB.toVec3(): Vec3 = Vec3(r.toDouble(), g.toDouble(), b.toDouble())

fun Vec3.toRGB(): RGB = RGB(x.toInt(), y.toInt(), z.toInt())

