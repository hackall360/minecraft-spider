package com.heledron.spideranimation

import com.heledron.spideranimation.spider.misc.splay
import com.heledron.spideranimation.spider.presets.*
import com.heledron.spideranimation.utilities.runLater
import com.mojang.brigadier.CommandDispatcher
import com.mojang.brigadier.arguments.DoubleArgumentType
import com.mojang.brigadier.arguments.IntegerArgumentType
import com.mojang.brigadier.arguments.StringArgumentType
import net.minecraft.commands.CommandSourceStack
import net.minecraft.commands.Commands
import net.minecraft.commands.SharedSuggestionProvider
import net.minecraft.network.chat.Component
import net.minecraftforge.event.RegisterCommandsEvent

/**
 * Registers Brigadier command trees for the mod using Forge's
 * [RegisterCommandsEvent]. Commands operate using [CommandSourceStack].
 */
fun registerCommands(event: RegisterCommandsEvent) {
    val dispatcher: CommandDispatcher<CommandSourceStack> = event.dispatcher

    registerScale(dispatcher)
    registerPreset(dispatcher)
    registerFall(dispatcher)
    registerSplay(dispatcher)
}

private fun registerScale(dispatcher: CommandDispatcher<CommandSourceStack>) {
    dispatcher.register(
        Commands.literal("scale")
            .requires { it.hasPermission(2) }
            .then(
                Commands.argument("scale", DoubleArgumentType.doubleArg())
                    .executes { ctx ->
                        val scale = DoubleArgumentType.getDouble(ctx, "scale")
                        val oldScale = AppState.options.bodyPlan.scale
                        AppState.options.scale(scale / oldScale)
                        AppState.recreateSpider()
                        ctx.source.sendSuccess({ Component.literal("Set scale to $scale") }, false)
                        1
                    }
            )
    )
}

private fun registerPreset(dispatcher: CommandDispatcher<CommandSourceStack>) {
    val presets = mapOf(
        "biped" to ::biped,
        "quadruped" to ::quadruped,
        "hexapod" to ::hexapod,
        "octopod" to ::octopod,
        "quadbot" to ::quadBot,
        "hexbot" to ::hexBot,
        "octobot" to ::octoBot,
    )

    dispatcher.register(
        Commands.literal("preset")
            .requires { it.hasPermission(2) }
            .then(
                Commands.argument("name", StringArgumentType.word())
                    .suggests { _, builder ->
                        SharedSuggestionProvider.suggest(presets.keys, builder)
                    }
                    .then(
                        Commands.argument("segments", IntegerArgumentType.integer(1))
                            .then(
                                Commands.argument("length", DoubleArgumentType.doubleArg(0.0))
                                    .executes { ctx ->
                                        val name = StringArgumentType.getString(ctx, "name")
                                        val segments = IntegerArgumentType.getInteger(ctx, "segments")
                                        val length = DoubleArgumentType.getDouble(ctx, "length")
                                        applyPreset(ctx.source, presets, name, segments, length)
                                    }
                            )
                            .executes { ctx ->
                                val name = StringArgumentType.getString(ctx, "name")
                                val segments = IntegerArgumentType.getInteger(ctx, "segments")
                                val length = if (name.contains("bot")) 1.0 else 1.0
                                applyPreset(ctx.source, presets, name, segments, length)
                            }
                    )
                    .executes { ctx ->
                        val name = StringArgumentType.getString(ctx, "name")
                        val segments = if (name.contains("bot")) 4 else 3
                        val length = 1.0
                        applyPreset(ctx.source, presets, name, segments, length)
                    }
            )
    )
}

private fun applyPreset(
    source: CommandSourceStack,
    presets: Map<String, (Int, Double) -> com.heledron.spideranimation.spider.configuration.SpiderOptions>,
    name: String,
    segments: Int,
    length: Double,
): Int {
    val preset = presets[name]
    if (preset == null) {
        source.sendFailure(Component.literal("Invalid preset: $name"))
        return 0
    }
    AppState.options = preset(segments, length)
    AppState.recreateSpider()
    source.sendSuccess({ Component.literal("Applied preset: $name") }, false)
    return 1
}

private fun registerFall(dispatcher: CommandDispatcher<CommandSourceStack>) {
    dispatcher.register(
        Commands.literal("fall")
            .requires { it.hasPermission(2) }
            .then(
                Commands.argument("height", DoubleArgumentType.doubleArg())
                    .executes { ctx ->
                        val spider = AppState.spider ?: return@executes 0
                        val height = DoubleArgumentType.getDouble(ctx, "height")
                        spider.teleport(spider.location().add(0.0, height, 0.0))
                        1
                    }
            )
    )
}

private fun registerSplay(dispatcher: CommandDispatcher<CommandSourceStack>) {
    dispatcher.register(
        Commands.literal("splay")
            .requires { it.hasPermission(2) }
            .then(
                Commands.argument("delay", IntegerArgumentType.integer(0))
                    .executes { ctx ->
                        val delay = IntegerArgumentType.getInteger(ctx, "delay").toLong()
                        runLater(delay) { splay() }
                        1
                    }
            )
            .executes { _ ->
                runLater(0) { splay() }
                1
            }
    )
}
