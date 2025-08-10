package com.heledron.spideranimation

import com.heledron.spideranimation.kinematic_chain_visualizer.KinematicChainVisualizer
import com.heledron.spideranimation.spider.*
import com.heledron.spideranimation.spider.presets.hexBot
import com.heledron.spideranimation.utilities.MultiEntityRenderer
import net.minecraft.server.level.ServerLevel
import net.minecraft.world.phys.Vec3
import org.joml.Quaternionf
import java.io.Closeable

object AppState {
    var closeables = mutableListOf<Closeable>()

    val renderer = MultiEntityRenderer()

    var options = hexBot(4, 1.0)
    var miscOptions = MiscellaneousOptions()

    var showDebugVisuals = false
    var gallop = false

    var spider: Spider? = null
    set (value) {
        if (field != value) field?.close()
        field = value
    }

    var target: Vec3? = null

    var chainVisualizer: KinematicChainVisualizer? = null
    set (value) {
        if (field != value) field?.close()
        field = value
    }

    /**
     * Creates a new [Spider] in the given [level] at the specified [position].
     *
     * Using [ServerLevel] and [Vec3] keeps this API in terms of native Minecraft
     * types instead of Bukkit wrappers. The supplied [position] should represent
     * the ground location for the spider; this method adds the configured body
     * height so the spider's body rests above the terrain.
     *
     * [orientation] is a quaternion describing the spider's rotation in world
     * space. An identity quaternion leaves the spider upright and facing its
     * default forward direction.
     */
    fun createSpider(level: ServerLevel, position: Vec3, orientation: Quaternionf = Quaternionf()): Spider {
        val adjusted = position.add(0.0, options.walkGait.stationary.bodyHeight, 0.0)
        val spider = Spider.fromPosition(level, adjusted, orientation, options)
        this.spider = spider
        return spider
    }

    /**
     * Rebuilds the current [spider] using its existing world, position and
     * orientation. The same body height offset logic from [createSpider] is
     * applied so the new instance appears at the correct elevation.
     */
    fun recreateSpider() {
        val spider = this.spider ?: return
        val level = spider.world as? ServerLevel ?: return
        createSpider(level, spider.position, spider.orientation)
    }
}

class MiscellaneousOptions {
    var showLaser = true
}