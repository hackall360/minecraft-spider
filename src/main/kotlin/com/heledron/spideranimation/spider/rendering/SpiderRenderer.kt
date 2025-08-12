package com.heledron.spideranimation.spider.rendering

import com.heledron.spideranimation.AppState
import com.heledron.spideranimation.spider.Spider
import com.heledron.spideranimation.spider.SpiderComponent
import com.heledron.spideranimation.utilities.interval
import com.heledron.spideranimation.utilities.spawnParticle
import org.bukkit.Color
import org.bukkit.Location
import org.bukkit.Particle
import net.minecraft.world.phys.Vec3
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

class SpiderParticleRenderer(val spider: Spider): SpiderComponent {
    override fun render() {
        renderSpider(spider)
    }

    companion object {
        fun renderTarget(location: Location) {
            spawnParticle(Particle.DUST, location, 1, 0.0, 0.0, 0.0, 0.0, Particle.DustOptions(Color.RED, 1f))
        }

        fun renderSpider(spider: Spider) {
            for (leg in spider.body.legs) {
                val world = leg.spider.world
                val chain = leg.chain
                var current = Location(world, chain.root.x, chain.root.y, chain.root.z)

                for ((i, segment) in chain.segments.withIndex()) {
                    val thickness = (chain.segments.size - i - 1) * 0.025
                    renderLine(current, segment.position, thickness)
                    current = Location(world, segment.position.x, segment.position.y, segment.position.z)
                }
            }
        }

        fun renderLine(point1: Location, point2: Vec3, thickness: Double) {
            val gap = .05

            val startVec = Vec3(point1.x, point1.y, point1.z)
            val amount = startVec.distanceTo(point2) / gap
            val step = point2.subtract(startVec).scale(1 / amount)

            val current = point1.clone()

            for (i in 0..amount.toInt()) {
                spawnParticle(Particle.BUBBLE, current, 1, thickness, thickness, thickness, 0.0)
                current.add(step.x, step.y, step.z)
            }
        }
    }
}



