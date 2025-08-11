package com.heledron.spideranimation.utilities

import net.minecraft.server.level.ServerLevel
import net.minecraft.world.entity.Display.BlockDisplay
import net.minecraft.world.phys.AABB
import net.minecraft.world.phys.Vec3
import net.minecraftforge.server.ServerLifecycleHooks
import org.joml.Matrix4f

/**
 * Execute a summon command and translate the resulting [BlockDisplay] entities
 * into a [DisplayModel].  Any block display spawned by the command within a
 * unit cube around the origin is sampled and immediately discarded.
 */
fun parseModelFromCommand(level: ServerLevel, command: String): DisplayModel {
    val server = level.server
    val source = server.createCommandSourceStack()
        .withLevel(level)
        .withPosition(Vec3(0.0, 0.0, 0.0))
        .withSuppressedOutput()
    val clean = command.removePrefix("/")
    server.commands.performPrefixedCommand(source, clean)

    val box = AABB(-1.0, -1.0, -1.0, 1.0, 1.0, 1.0)
    val displays = level.getEntities(null, box) { it is BlockDisplay }
        .map { it as BlockDisplay }

    val pieces = displays
        .filter { !it.blockState.isAir }
        .map { display ->
            BlockDisplayModelPiece(
                block = display.blockState,
                transform = Matrix4f(display.transformation.matrix),
                tags = display.tags.toList()
            )
        }

    // Clean up the temporary entities
    displays.forEach { it.discard() }

    return DisplayModel(pieces)
}

/** Convenience overload that uses the overworld from the running server. */
fun parseModelFromCommand(command: String): DisplayModel {
    val server = ServerLifecycleHooks.getCurrentServer()
        ?: throw IllegalStateException("Server not running")
    return parseModelFromCommand(server.overworld(), command)
}

