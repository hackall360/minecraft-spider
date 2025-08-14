package com.heledron.spideranimation

import com.heledron.spideranimation.kinematic_chain_visualizer.KinematicChainVisualizer
import com.heledron.spideranimation.spider.misc.StayStillBehaviour
import com.heledron.spideranimation.utilities.AppState
import com.heledron.spideranimation.utilities.RenderEntityGroup
import com.heledron.spideranimation.utilities.vec3MarkerRenderEntity
import com.heledron.spideranimation.registerCommands
import net.minecraftforge.common.MinecraftForge
import net.minecraftforge.event.RegisterCommandsEvent
import net.minecraftforge.event.TickEvent
import net.minecraftforge.event.entity.EntityJoinLevelEvent
import net.minecraftforge.event.server.ServerStartingEvent
import net.minecraftforge.event.server.ServerStoppingEvent
import net.minecraftforge.eventbus.api.SubscribeEvent
import net.minecraftforge.fml.ModLoadingContext
import net.minecraftforge.fml.common.Mod
import net.minecraftforge.fml.config.ModConfig
import net.minecraftforge.fml.event.lifecycle.FMLCommonSetupEvent
import net.minecraftforge.fml.javafmlmod.FMLJavaModLoadingContext
import org.apache.logging.log4j.LogManager
import net.minecraft.server.level.ServerLevel
import net.minecraft.world.phys.Vec3
import java.io.Closeable

@Mod(SpiderAnimationMod.MOD_ID)
class SpiderAnimationMod {
    init {
        val bus = FMLJavaModLoadingContext.get().modEventBus
        bus.addListener(this::commonSetup)
        ModLoadingContext.get().registerConfig(ModConfig.Type.COMMON, SpiderConfig.SPEC)
        MinecraftForge.EVENT_BUS.register(this)
        MinecraftForge.EVENT_BUS.register(ControlItemEvents)
    }

    private val closeables = mutableListOf<Closeable>()

    private fun commonSetup(event: FMLCommonSetupEvent) { }

    @SubscribeEvent
    fun onServerStarting(event: ServerStartingEvent) {
        LOGGER.info("Enabling Spider Animation mod")
        closeables += Closeable {
            AppState.spider?.close()
            AppState.chainVisualizer?.close()
            AppState.renderer.close()
        }
    }

    @SubscribeEvent
    fun onServerStopping(event: ServerStoppingEvent) {
        LOGGER.info("Disabling Spider Animation mod")
        closeables.forEach { it.close() }
        AppState.closeables.forEach { it.close() }
    }

    @SubscribeEvent
    fun onRegisterCommands(event: RegisterCommandsEvent) {
        registerCommands(event)
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

        // Render target marker if one is set
        val target = if (AppState.miscOptions.showLaser) AppState.target else null
        if (target != null) {
            val level = event.server.overworld()
            val group = RenderEntityGroup().apply {
                add(0, vec3MarkerRenderEntity(level, target))
            }
            AppState.renderer.render("target", group)
        }

        AppState.chainVisualizer?.render()

        AppState.renderer.flush()
        AppState.target = null
    }

    @SubscribeEvent
    fun onEntityJoin(event: EntityJoinLevelEvent) {
        if (event.level.isClientSide) return
        if (!event.entity.tags.contains("spider.chain_visualizer")) return
        val segmentPlans = AppState.options.bodyPlan.legs.lastOrNull()?.segments ?: return
        val level = event.level as? ServerLevel ?: return
        val root = Vec3(event.entity.x, event.entity.y, event.entity.z)
        AppState.chainVisualizer = if (AppState.chainVisualizer != null) null else KinematicChainVisualizer.create(
            segmentPlans = segmentPlans,
            root = root,
            world = level,
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
