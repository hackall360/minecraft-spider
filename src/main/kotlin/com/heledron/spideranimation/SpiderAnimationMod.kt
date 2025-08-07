package com.heledron.spideranimation

import com.heledron.spideranimation.kinematic_chain_visualizer.KinematicChainVisualizer
import com.heledron.spideranimation.spider.misc.StayStillBehaviour
import com.heledron.spideranimation.spider.rendering.targetRenderEntity
import com.heledron.spideranimation.utilities.AppState
import com.heledron.spideranimation.registerItems
import com.heledron.spideranimation.registerCommands
import net.minecraftforge.common.MinecraftForge
import net.minecraftforge.event.TickEvent
import net.minecraftforge.event.entity.EntityJoinLevelEvent
import net.minecraftforge.event.server.ServerStartingEvent
import net.minecraftforge.event.server.ServerStoppingEvent
import net.minecraftforge.eventbus.api.SubscribeEvent
import net.minecraftforge.fml.common.Mod
import net.minecraftforge.fml.event.lifecycle.FMLCommonSetupEvent
import net.minecraftforge.fml.javafmlmod.FMLJavaModLoadingContext
import org.apache.logging.log4j.LogManager
import org.bukkit.Bukkit
import org.bukkit.util.Vector
import java.io.Closeable

@Mod(SpiderAnimationMod.MOD_ID)
class SpiderAnimationMod {
    init {
        FMLJavaModLoadingContext.get().modEventBus.addListener(this::commonSetup)
        MinecraftForge.EVENT_BUS.register(this)
    }

    private val closeables = mutableListOf<Closeable>()

    private fun commonSetup(event: FMLCommonSetupEvent) {
        registerItems()
    }

    @SubscribeEvent
    fun onServerStarting(event: ServerStartingEvent) {
        LOGGER.info("Enabling Spider Animation mod")
        closeables += Closeable {
            AppState.spider?.close()
            AppState.chainVisualizer?.close()
            AppState.renderer.close()
        }
        registerCommands()
    }

    @SubscribeEvent
    fun onServerStopping(event: ServerStoppingEvent) {
        LOGGER.info("Disabling Spider Animation mod")
        closeables.forEach { it.close() }
        AppState.closeables.forEach { it.close() }
    }

    @SubscribeEvent
    fun onServerTick(event: TickEvent.ServerTickEvent) {
        if (event.phase != TickEvent.Phase.END) return

        val spider = AppState.spider
        if (spider != null) {
            spider.showDebugVisuals = AppState.showDebugVisuals
            spider.gallop = AppState.gallop

            spider.update()
            if (spider.mount.getRider() == null) spider.behaviour = StayStillBehaviour(spider)
        }

        val target = (if (AppState.miscOptions.showLaser) AppState.target else null)
            ?: AppState.chainVisualizer?.target
        if (target != null) AppState.renderer.render("target", targetRenderEntity(target))

        AppState.renderer.flush()
        AppState.target = null
    }

    @SubscribeEvent
    fun onEntityJoin(event: EntityJoinLevelEvent) {
        if (!event.entity.tags.contains("spider.chain_visualizer")) return
        val segmentPlans = AppState.options.bodyPlan.legs.lastOrNull()?.segments ?: return
        val worldName = event.level.dimension().location().path
        val bukkitWorld = Bukkit.getWorld(worldName) ?: return
        val root = Vector(event.entity.x, event.entity.y, event.entity.z)
        AppState.chainVisualizer = if (AppState.chainVisualizer != null) null else KinematicChainVisualizer.create(
            segmentPlans = segmentPlans,
            root = root,
            world = bukkitWorld,
            straightenRotation = AppState.options.walkGait.legStraightenRotation
        )
        AppState.chainVisualizer?.detailed = AppState.showDebugVisuals
        event.entity.discard()
    }

    companion object {
        const val MOD_ID = "spideranimation"
        private val LOGGER = LogManager.getLogger()
    }
}
