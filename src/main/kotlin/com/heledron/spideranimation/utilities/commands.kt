package com.heledron.spideranimation.utilities

import net.minecraft.server.level.ServerLevel
import net.minecraft.world.entity.Entity
import net.minecraft.world.phys.Vec3

/** Execute a command without producing chat output. */
fun runCommandSilently(level: ServerLevel, command: String) {
    val server = level.server
    val stack = server.createCommandSourceStack()
        .withLevel(level)
        .withPosition(Vec3(0.0, 0.0, 0.0))
        .withSuppressedOutput()
    server.commands.performPrefixedCommand(stack, command.removePrefix("/"))
}

/** Convenience overload that derives the [ServerLevel] from an [Entity]. */
fun runCommandSilently(entity: Entity, command: String) {
    val level = entity.level() as? ServerLevel ?: return
    runCommandSilently(level, command)
}

