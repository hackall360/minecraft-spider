package com.heledron.spideranimation.utilities

import net.minecraft.world.phys.Vec3
import org.bukkit.util.Vector

fun Vector.toVec3(): Vec3 = Vec3(this.x, this.y, this.z)

fun Vec3.toVector(): Vector = Vector(this.x, this.y, this.z)
