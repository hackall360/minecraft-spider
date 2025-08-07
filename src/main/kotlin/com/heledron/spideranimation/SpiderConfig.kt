package com.heledron.spideranimation

import net.minecraftforge.common.ForgeConfigSpec
import net.minecraftforge.eventbus.api.SubscribeEvent
import net.minecraftforge.fml.common.Mod
import net.minecraftforge.fml.config.ModConfig
import net.minecraftforge.fml.event.config.ModConfigEvent

/**
 * Forge configuration for the Spider Animation mod.  This replaces the
 * previous Bukkit based configuration by persisting options in a standard
 * Forge config file.
 */
object SpiderConfig {
    private val builder = ForgeConfigSpec.Builder()

    val SHOW_LASER: ForgeConfigSpec.BooleanValue =
        builder.comment("Render laser pointer").define("showLaser", true)
    val SHOW_DEBUG: ForgeConfigSpec.BooleanValue =
        builder.comment("Show debug visuals").define("showDebug", false)
    val GALLOP: ForgeConfigSpec.BooleanValue =
        builder.comment("Enable gallop gait").define("gallop", false)

    val SPEC: ForgeConfigSpec = builder.build()
    lateinit var config: ModConfig

    /** Save the config to disk when values are changed at runtime. */
    fun save() {
        if (::config.isInitialized) config.save()
    }
}

@Mod.EventBusSubscriber(modid = SpiderAnimationMod.MOD_ID, bus = Mod.EventBusSubscriber.Bus.MOD)
object ConfigHandler {
    @SubscribeEvent
    fun onLoad(event: ModConfigEvent) {
        if (event.config.spec != SpiderConfig.SPEC) return
        SpiderConfig.config = event.config
        apply()
    }

    @SubscribeEvent
    fun onReload(event: ModConfigEvent.Reloading) {
        if (event.config.spec != SpiderConfig.SPEC) return
        apply()
    }

    private fun apply() {
        AppState.miscOptions.showLaser = SpiderConfig.SHOW_LASER.get()
        AppState.showDebugVisuals = SpiderConfig.SHOW_DEBUG.get()
        AppState.gallop = SpiderConfig.GALLOP.get()
    }
}
