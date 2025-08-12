package com.heledron.spideranimation.spider.rendering

import com.heledron.spideranimation.AppState
import com.heledron.spideranimation.spider.Spider
import com.heledron.spideranimation.spider.SpiderComponent
import com.heledron.spideranimation.utilities.interval
import com.heledron.spideranimation.utilities.spawnParticle
import com.heledron.spideranimation.utilities.RGB
import net.minecraft.core.particles.DustParticleOptions
import net.minecraft.core.particles.ParticleTypes
import net.minecraft.world.level.Level
import net.minecraft.world.phys.Vec3
import org.joml.Vector3f
import kotlin.random.Random

class SpiderRenderer(val spider: Spider): SpiderComponent {
    // apply eye blinking effect
    val eyeInterval = interval(0,10) {
        val pieces = spider.options.bodyPlan.bodyModel.pieces.filter { it.tags.contains("eye") }

        if (Random.nextBoolean()) return@interval
        for (piece in pieces) {
            val block = spider.options.bodyPlan.eyePalette.random()
            piece.block = block.first
            piece.brightness = block.second
        }
    }

    // apply blinking lights effect
    val blinkingInterval = interval(0,5) {
        val pieces = spider.options.bodyPlan.bodyModel.pieces.filter { it.tags.contains("blinking_lights") }

        if (Random.nextBoolean()) return@interval
        for (piece in pieces) {
            val block = spider.options.bodyPlan.blinkingPalette.random()
            piece.block = block.first
            piece.brightness = block.second
        }
    }
    /*interval(0,20 * 4) {
        val pieces = spider.options.bodyPlan.bodyModel.pieces.filter { it.tags.contains("blinking_lights") }

//        val blinkBlock = spider.options.bodyPlan.blinkingPalette.random()
        for (piece in pieces) {
            val currentBlock = piece.block
            val currentBrightness = piece.brightness

            val scheduler = SeriesScheduler()
            for (i in 0 until 2) {
                scheduler.run {
                    piece.block = spider.options.bodyPlan.blinkingPalette.random()
                    piece.brightness = Display.Brightness(15, 15)
                }
                scheduler.sleep(2)
                scheduler.run {
                    piece.block = currentBlock
                    piece.brightness = currentBrightness
                }
                scheduler.sleep(2)
            }

        }
    }*/

    override fun render() {
        AppState.renderer.render(this, spiderRenderEntities(spider))
        if (spider.showDebugVisuals) AppState.renderer.render(this to "debug", spiderDebugRenderEntities(spider))
    }

    override fun close() {
        eyeInterval.close()
        blinkingInterval.close()
    }
}

class SpiderParticleRenderer(val spider: Spider) : SpiderComponent {
    override fun render() {
        renderSpider(spider)
    }

    companion object {
        fun renderTarget(level: Level, position: Vec3) {
            val colour = RGB(255, 0, 0)
            val particle = DustParticleOptions(Vector3f(colour.r / 255f, colour.g / 255f, colour.b / 255f), 1f)
            spawnParticle(level, particle, position, 1, 0.0, 0.0, 0.0, 0.0)
        }

        fun renderSpider(spider: Spider) {
            for (leg in spider.body.legs) {
                val level = leg.spider.world
                val chain = leg.chain
                var current = chain.root

                for ((i, segment) in chain.segments.withIndex()) {
                    val thickness = (chain.segments.size - i - 1) * 0.025
                    renderLine(level, current, segment.position, thickness)
                    current = segment.position
                }
            }
        }

        fun renderLine(level: Level, point1: Vec3, point2: Vec3, thickness: Double) {
            val gap = .05

            val amount = point1.distanceTo(point2) / gap
            val step = point2.subtract(point1).scale(1 / amount)

            var current = point1

            for (i in 0..amount.toInt()) {
                spawnParticle(level, ParticleTypes.BUBBLE, current, 1, thickness, thickness, thickness, 0.0)
                current = current.add(step)
            }
        }
    }
}



