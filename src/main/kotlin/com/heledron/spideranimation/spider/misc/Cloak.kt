package com.heledron.spideranimation.spider.misc

import com.heledron.spideranimation.spider.Spider
import com.heledron.spideranimation.spider.SpiderComponent
import com.heledron.spideranimation.utilities.*
import net.minecraft.server.level.ServerPlayer
import net.minecraft.world.entity.Display
import net.minecraft.world.level.block.Blocks
import net.minecraft.world.level.block.state.BlockState
import net.minecraft.world.phys.BlockHitResult
import net.minecraft.world.phys.Vec3
import java.util.WeakHashMap

class Cloak(var  spider: Spider) : SpiderComponent {
    var active = false
    val onCloakDamage = EventEmitter()
    val onToggle = EventEmitter()

    // Store colours as Vec3 for high-precision transitions.
    private var cloakColor = WeakHashMap<Any, Vec3>()
    private var cloakOverride = WeakHashMap<Any, BlockState>()
    private var cloakGlitching = false

    init {
        spider.tridentDetector.onHit.listen {
            if (active) onCloakDamage.emit()
            active = false
        }

        onCloakDamage.listen {
            breakCloak()
        }
    }

    override fun update() {
        // no nothing
    }

    fun toggleCloak() {
        active = !active
        onToggle.emit()
    }

    fun getPiece(id: Any, position: Vec3, originalBlock: BlockState, originalBrightness: Display.Brightness?): Pair<BlockState, Display.Brightness?> {
        applyCloak(id, position, originalBlock, originalBrightness?.skyLight ?: 15)

        val override = cloakOverride[id]
        if (override != null) return override to Display.Brightness(0, 15)

        val cloakColor = cloakColor[id] ?: return originalBlock to originalBrightness
        val match = getBestMatchFromColor(cloakColor.toRGB(), spider.options.cloak.allowCustomBrightness)
        return match.state to Display.Brightness(0, match.brightness)
    }

    private fun applyCloak(id: Any, position: Vec3, originalBlock: BlockState, originalBrightness: Int) {
        val location = position

        if (cloakGlitching) return

        fun groundCast(): BlockHitResult? {
            return raycastGround(spider.world, location, DOWN_VECTOR, 5.0)
        }

        fun cast(): BlockHitResult? {
            val targetPlayer = spider.world.players.firstOrNull() as? ServerPlayer ?: return groundCast()

            val eye = targetPlayer.eyePosition
            val direction = location.subtract(eye)
            return raycastGround(spider.world, location, direction, 30.0)
        }

        val originalColor = getColorFromBlock(originalBlock, originalBrightness)?.toVec3() ?: return
        val currentColor = cloakColor[id] ?: originalColor

        val targetColor = run getTargetColor@{
            if (!active) return@getTargetColor originalColor

            val rayTrace = cast() ?: return@getTargetColor currentColor
            val block = spider.world.getBlockState(rayTrace.blockPos)
            val lightLevel = 15
            getColorFromBlock(block, lightLevel)?.toVec3() ?: currentColor
        }


        val newColor = currentColor
            .lerp(targetColor, spider.options.cloak.lerpSpeed)
            .moveTowards(targetColor, spider.options.cloak.moveSpeed)

        if (newColor == originalColor) cloakColor.remove(id)
        else cloakColor[id] = newColor
    }


    private fun breakCloak() {
        cloakGlitching = true

        val originalColors = cloakColor.values.toList()

        val glitch = listOf(
            { id: Any -> cloakOverride[id] = Blocks.LIGHT_BLUE_GLAZED_TERRACOTTA.defaultBlockState() },
            { id: Any -> cloakOverride[id] = Blocks.CYAN_GLAZED_TERRACOTTA.defaultBlockState() },
            { id: Any -> cloakOverride[id] = Blocks.WHITE_GLAZED_TERRACOTTA.defaultBlockState() },
            { id: Any -> cloakOverride[id] = Blocks.GRAY_GLAZED_TERRACOTTA.defaultBlockState() },

            { id: Any -> cloakOverride.remove(id) },
            { id: Any -> cloakColor[id] = originalColors.random() },
        )

        var maxTime = 0

        for ((id) in cloakColor) {
            val scheduler = SeriesScheduler()

            fun randomSleep(min: Int, max: Int) {
                scheduler.sleep((min + Math.random() * (max - min)).toLong())
            }

            randomSleep(0, 3)
            for (i in 0 until (Math.random() * 4).toInt()) {
                scheduler.run { glitch.random()(id) }
                scheduler.sleep(2L)
            }

            scheduler.run {
                cloakColor.remove(id)
                cloakOverride.remove(id)
            }

            if (Math.random() < 1.0 / 6) continue

            randomSleep(0, 3)

            for (i in 0 until  (Math.random() * 3).toInt()) {
                scheduler.run {
                    cloakOverride[id] = getBestMatchFromColor(originalColors.random().toRGB(), spider.options.cloak.allowCustomBrightness).state
                }

                randomSleep(5, 15)

                scheduler.run {
                    cloakOverride.remove(id)
                }
                scheduler.sleep(2L)
            }

            if (scheduler.time > maxTime) maxTime = scheduler.time.toInt()
        }

        runLater(maxTime.toLong()) {
            cloakGlitching = false
        }
    }


//    val transitioning = ArrayList<Any>()
//    fun transitionSegmentBlock(id: Any, waitTime: Int, glitchTime: Int, newBlock: Material?) {
//        if (transitioning.contains(id)) return
//        transitioning.add(id)
//
//        val scheduler = SeriesScheduler()
//        scheduler.sleep(waitTime.toLong())
//        scheduler.run {
//            cloakOverride[id] = Material.GRAY_GLAZED_TERRACOTTA.createBlockData()
//        }
//
//        scheduler.sleep(glitchTime.toLong())
//        scheduler.run {
//            cloakColor[id] = newBlock
//            transitioning.remove(id)
//        }
//    }
}
