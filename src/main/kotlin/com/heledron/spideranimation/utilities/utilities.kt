package com.heledron.spideranimation.utilities

import net.md_5.bungee.api.ChatMessageType
import net.minecraftforge.common.MinecraftForge
import net.minecraftforge.event.TickEvent
import net.minecraftforge.event.entity.EntityJoinLevelEvent
import net.minecraftforge.eventbus.api.SubscribeEvent
import net.minecraftforge.fml.common.Mod
import org.bukkit.Bukkit
import org.bukkit.ChatColor
import org.bukkit.FluidCollisionMode
import org.bukkit.Location
import org.bukkit.World
import org.bukkit.entity.Display
import org.bukkit.entity.Entity
import org.bukkit.entity.Player
import org.bukkit.entity.minecart.CommandMinecart
import org.bukkit.event.Listener
import org.bukkit.event.block.Action
import org.bukkit.inventory.EquipmentSlot
import org.bukkit.inventory.ItemStack
import org.bukkit.util.RayTraceResult
import org.bukkit.util.Transformation
import org.bukkit.util.Vector
import org.joml.*
import java.io.Closeable
import java.util.concurrent.CopyOnWriteArrayList

@Mod.EventBusSubscriber(modid = com.heledron.spideranimation.SpiderAnimationMod.MOD_ID)
object Scheduler {
    private data class Task(var runAt: Long, val period: Long, val action: (Closeable) -> Unit) {
        var cancelled = false
        val handle: Closeable = Closeable { cancelled = true }
    }

    private val tasks = CopyOnWriteArrayList<Task>()
    private var tick = 0L

    fun runLater(delay: Long, task: () -> Unit): Closeable {
        val t = Task(tick + delay, 0) { task() }
        tasks += t
        return t.handle
    }

    fun interval(delay: Long, period: Long, task: (Closeable) -> Unit): Closeable {
        val t = Task(tick + delay, period, task)
        tasks += t
        return t.handle
    }

    @SubscribeEvent
    fun onServerTick(event: TickEvent.ServerTickEvent) {
        if (event.phase != TickEvent.Phase.END) return
        tick++
        val iterator = tasks.iterator()
        while (iterator.hasNext()) {
            val t = iterator.next()
            if (t.cancelled) {
                iterator.remove()
                continue
            }
            if (tick < t.runAt) continue
            t.action(t.handle)
            if (t.cancelled || t.period <= 0) {
                iterator.remove()
            } else {
                t.runAt = tick + t.period
            }
        }
    }
}

fun runLater(delay: Long, task: () -> Unit): Closeable = Scheduler.runLater(delay, task)

fun interval(delay: Long, period: Long, task: (Closeable) -> Unit): Closeable = Scheduler.interval(delay, period, task)

fun onServerTick(task: (Closeable) -> Unit): Closeable {
    lateinit var handle: Closeable
    val listener = object {
        @SubscribeEvent
        fun tick(event: TickEvent.ServerTickEvent) {
            if (event.phase != TickEvent.Phase.END) return
            task(handle)
        }
    }
    handle = Closeable { MinecraftForge.EVENT_BUS.unregister(listener) }
    MinecraftForge.EVENT_BUS.register(listener)
    return handle
}

fun addEventListener(@Suppress("UNUSED_PARAMETER") listener: Listener): Closeable {
    return Closeable { }
}

fun onInteractEntity(@Suppress("UNUSED_PARAMETER") listener: (Player, Entity, EquipmentSlot) -> Unit): Closeable {
    return Closeable { }
}

fun onSpawnEntity(listener: (Entity, World) -> Unit): Closeable {
    val handler = object {
        @SubscribeEvent
        fun handle(event: EntityJoinLevelEvent) {
            val worldName = event.level.dimension().location().path
            val bukkitWorld = Bukkit.getWorld(worldName) ?: return
            val bukkitEntity = event.entity.bukkitEntity
            listener(bukkitEntity, bukkitWorld)
        }
    }
    MinecraftForge.EVENT_BUS.register(handler)
    return Closeable { MinecraftForge.EVENT_BUS.unregister(handler) }
}


private var commandBlockMinecart: CommandMinecart? = null
fun runCommandSilently(command: String, location: Location = Bukkit.getWorlds().first().spawnLocation) {
    val server = Bukkit.getServer()

    val commandBlockMinecart = commandBlockMinecart ?: spawnEntity(location, CommandMinecart::class.java) {
        commandBlockMinecart = it
        it.remove()
    }

    server.dispatchCommand(commandBlockMinecart, command)
}

fun onGestureUseItem(@Suppress("UNUSED_PARAMETER") listener: (Player, ItemStack) -> Unit): Closeable {
    return Closeable { }
}


class SeriesScheduler {
    var time = 0L

    fun sleep(time: Long) {
        this.time += time
    }

    fun run(task: () -> Unit) {
        runLater(time, task)
    }
}

class EventEmitter {
    private val listeners = mutableListOf<() -> Unit>()
    fun listen(listener: () -> Unit): Closeable {
        listeners.add(listener)
        return Closeable { listeners.remove(listener) }
    }

    fun emit() {
        for (listener in listeners) listener()
    }
}

fun firstPlayer(): Player? {
    return Bukkit.getOnlinePlayers().firstOrNull()
}

fun sendDebugMessage(message: String) {
    sendActionBar(firstPlayer() ?: return, message)
}

fun sendActionBar(player: Player, message: String) {
//    player.sendActionBar(message)
    player.spigot().sendMessage(ChatMessageType.ACTION_BAR, net.md_5.bungee.api.chat.TextComponent(message))
}

fun raycastGround(location: Location, direction: Vector, maxDistance: Double): RayTraceResult? {
    return location.world!!.rayTraceBlocks(location, direction, maxDistance, FluidCollisionMode.NEVER, true)
}

fun World.raycastGround(position: Vector, direction: Vector, maxDistance: Double): RayTraceResult? {
    return raycastGround(position.toLocation(this), direction, maxDistance)
}

fun World.isOnGround(position: Vector, downVector: Vector = DOWN_VECTOR): Boolean {
    return raycastGround(position.toLocation(this), downVector, 0.001) != null
}

data class CollisionResult(val position: Vector, val offset: Vector)

fun resolveCollision(location: Location, direction: Vector): CollisionResult? {
    val ray = location.world!!.rayTraceBlocks(location.clone().subtract(direction), direction, direction.length(), FluidCollisionMode.NEVER, true)
    if (ray != null) {
        val newLocation = ray.hitPosition.toLocation(location.world!!)
        return CollisionResult(newLocation.toVector(), ray.hitPosition.subtract(location.toVector()))
    }

    return null
}

fun World.resolveCollision(position: Vector, direction: Vector): CollisionResult? {
    return resolveCollision(position.toLocation(this), direction)
}

fun playSound(location: Location, sound: org.bukkit.Sound, volume: Float, pitch: Float) {
    location.world!!.playSound(location, sound, volume, pitch)
}

fun World.playSound(position: Vector, sound: org.bukkit.Sound, volume: Float, pitch: Float) {
    playSound(position.toLocation(this), sound, volume, pitch)
}

fun <T : Entity> spawnEntity(location: Location, clazz: Class<T>, initializer: (T) -> Unit): T {
    return location.world!!.spawn(location, clazz, initializer)
}

fun spawnParticle(particle: org.bukkit.Particle, location: Location, count: Int, offsetX: Double, offsetY: Double, offsetZ: Double, extra: Double) {
    location.world!!.spawnParticle(particle, location, count, offsetX, offsetY, offsetZ, extra)
}

fun <T> spawnParticle(particle: org.bukkit.Particle, location: Location, count: Int, offsetX: Double, offsetY: Double, offsetZ: Double, extra: Double, data: T) {
    location.world!!.spawnParticle(particle, location, count, offsetX, offsetY, offsetZ, extra, data)
}


fun lookingAtPoint(eye: Vector, direction: Vector, point: Vector, tolerance: Double): Boolean {
    val pointDistance = eye.distance(point)
    val lookingAtPoint = eye.clone().add(direction.clone().multiply(pointDistance))
    return lookingAtPoint.distance(point) < tolerance
}

fun centredTransform(xSize: Float, ySize: Float, zSize: Float): Transformation {
    return Transformation(
        Vector3f(-xSize / 2, -ySize / 2, -zSize / 2),
        AxisAngle4f(0f, 0f, 0f, 1f),
        Vector3f(xSize, ySize, zSize),
        AxisAngle4f(0f, 0f, 0f, 1f)
    )
}

fun centeredMatrix(xSize: Float, ySize: Float, zSize: Float): Matrix4f {
    return Matrix4f()
        .scale(xSize, ySize, zSize)
        .translate(-.5f, -.5f, -.5f)
}

fun matrixFromTransform(transformation: Transformation): Matrix4f {
    val matrix = Matrix4f()
    matrix.translate(transformation.translation)
    matrix.rotate(transformation.leftRotation)
    matrix.scale(transformation.scale)
    matrix.rotate(transformation.rightRotation)
    return matrix
}



fun Display.applyTransformationWithInterpolation(transformation: Transformation) {
    if (this.transformation == transformation) return
    this.transformation = transformation
    this.interpolationDelay = 0
}

fun Display.applyTransformationWithInterpolation(matrix: Matrix4f) {
    val oldTransform = this.transformation
    setTransformationMatrix(matrix)

    if (oldTransform == this.transformation) return
    this.interpolationDelay = 0
}