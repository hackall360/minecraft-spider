package com.heledron.spideranimation.utilities

import net.minecraftforge.common.MinecraftForge
import net.minecraftforge.event.TickEvent
import net.minecraftforge.eventbus.api.SubscribeEvent
import net.minecraftforge.fml.common.Mod
import net.minecraft.world.entity.Entity
import net.minecraft.world.entity.EntityType
import net.minecraft.world.level.ClipContext
import net.minecraft.world.level.Level
import net.minecraft.world.phys.BlockHitResult
import net.minecraft.world.phys.HitResult
import net.minecraft.world.phys.Vec3
import net.minecraft.sounds.SoundEvent
import net.minecraft.sounds.SoundSource
import net.minecraft.core.particles.ParticleOptions
import org.joml.Matrix4f
import org.joml.Quaternionf
import org.joml.Vector3f
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

fun raycastGround(level: Level, origin: Vec3, direction: Vec3, maxDistance: Double): BlockHitResult? {
    val end = origin.add(direction.normalize().scale(maxDistance))
    val context = ClipContext(origin, end, ClipContext.Block.OUTLINE, ClipContext.Fluid.NONE, null)
    val result = level.clip(context)
    return if (result.type == HitResult.Type.MISS) null else result
}

fun Level.raycastGround(position: Vec3, direction: Vec3, maxDistance: Double): BlockHitResult? =
    raycastGround(this, position, direction, maxDistance)

fun Level.isOnGround(position: Vec3, downVector: Vec3 = Vec3(0.0, -1.0, 0.0)): Boolean =
    raycastGround(position, downVector, 0.001) != null

data class CollisionResult(val position: Vec3, val offset: Vec3)

fun resolveCollision(level: Level, position: Vec3, direction: Vec3): CollisionResult? {
    val start = position.subtract(direction)
    val end = position
    val context = ClipContext(start, end, ClipContext.Block.OUTLINE, ClipContext.Fluid.NONE, null)
    val result = level.clip(context)
    return if (result.type == HitResult.Type.MISS) null else CollisionResult(result.location, result.location.subtract(position))
}

fun Level.resolveCollision(position: Vec3, direction: Vec3): CollisionResult? =
    resolveCollision(this, position, direction)

fun playSound(level: Level, position: Vec3, sound: SoundEvent, volume: Float, pitch: Float) {
    level.playSound(null, position.x, position.y, position.z, sound, SoundSource.BLOCKS, volume, pitch)
}

fun <T : Entity> spawnEntity(level: Level, position: Vec3, type: EntityType<T>, initializer: (T) -> Unit): T {
    val entity = type.create(level) ?: throw IllegalArgumentException("Cannot create entity")
    entity.moveTo(position.x, position.y, position.z, entity.yRot, entity.xRot)
    initializer(entity)
    level.addFreshEntity(entity)
    return entity
}

fun spawnParticle(level: Level, particle: ParticleOptions, position: Vec3, count: Int,
                   offsetX: Double, offsetY: Double, offsetZ: Double, speed: Double) {
    for (i in 0 until count) {
        level.addParticle(particle, position.x, position.y, position.z, offsetX, offsetY, offsetZ)
    }
}

fun lookingAtPoint(eye: Vec3, direction: Vec3, point: Vec3, tolerance: Double): Boolean {
    val pointDistance = eye.distanceTo(point)
    val lookingAtPoint = eye.add(direction.normalize().scale(pointDistance))
    return lookingAtPoint.distanceTo(point) < tolerance
}

fun centredTransform(xSize: Float, ySize: Float, zSize: Float): org.joml.Transformation {
    return org.joml.Transformation(
        Vector3f(-xSize / 2, -ySize / 2, -zSize / 2),
        Quaternionf(0f, 0f, 0f, 1f),
        Vector3f(xSize, ySize, zSize),
        Quaternionf(0f, 0f, 0f, 1f)
    )
}

fun centeredMatrix(xSize: Float, ySize: Float, zSize: Float): Matrix4f {
    return Matrix4f()
        .scale(xSize, ySize, zSize)
        .translate(-.5f, -.5f, -.5f)
}

fun matrixFromTransform(transformation: org.joml.Transformation): Matrix4f {
    val matrix = Matrix4f()
    matrix.translate(transformation.translation)
    matrix.rotate(transformation.leftRotation)
    matrix.scale(transformation.scale)
    matrix.rotate(transformation.rightRotation)
    return matrix
}

fun net.minecraft.world.entity.Display.applyTransformationWithInterpolation(transformation: org.joml.Transformation) {
    if (this.transformation == transformation) return
    this.transformation = transformation
    this.interpolationDelay = 0
}

fun net.minecraft.world.entity.Display.applyTransformationWithInterpolation(matrix: Matrix4f) {
    val oldTransform = this.transformation
    this.transformation = net.minecraft.util.Mth.quatFromXYZ(0f,0f,0f) // placeholder
    if (oldTransform == this.transformation) return
    this.interpolationDelay = 0
}
