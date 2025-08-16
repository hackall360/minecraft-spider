package com.heledron.spideranimation.utilities

import net.minecraft.network.chat.Component
import net.minecraft.world.entity.Display
import net.minecraft.world.entity.Display.BlockDisplay
import net.minecraft.world.entity.Display.TextDisplay
import net.minecraft.world.entity.Entity
import net.minecraft.world.entity.EntityType
import net.minecraft.world.level.Level
import net.minecraft.world.level.block.Blocks
import net.minecraft.world.phys.Vec3
import org.joml.Matrix4f
import java.io.Closeable

/**
 * A template describing how to spawn and update an entity instance when
 * rendering.  The vanilla display entities are spawned directly in the world
 * and updated every tick by the renderer.
 */
class RenderEntity<T : Entity>(
    val type: EntityType<T>,
    val level: Level,
    val position: Vec3,
    val init: (T) -> Unit = {},
    val update: (T) -> Unit = {},
)

/**
 * A collection of renderable entities keyed by an arbitrary id.  Nested groups
 * are flattened using a composite key so that callers can build complex
 * hierarchies of entities to render.
 */
class RenderEntityGroup {
    val items = mutableMapOf<Any, RenderEntity<out Entity>>()

    fun add(id: Any, item: RenderEntity<out Entity>) {
        items[id] = item
    }

    fun add(id: Any, group: RenderEntityGroup) {
        for ((subId, part) in group.items) {
            items[id to subId] = part
        }
    }
}

/** Create a [BlockDisplay] render entity. */
fun blockRenderEntity(
    level: Level,
    position: Vec3,
    init: (BlockDisplay) -> Unit = {},
    update: (BlockDisplay) -> Unit = {},
) = RenderEntity(
    type = EntityType.BLOCK_DISPLAY,
    level = level,
    position = position,
    init = init,
    update = update,
)

/**
 * Create a line rendered as a scaled [BlockDisplay].  The line is represented
 * by a unit cube scaled and rotated so that the Z axis follows the provided
 * vector.
 */
fun lineRenderEntity(
    level: Level,
    position: Vec3,
    vector: Vec3,
    upVector: Vec3 = if (vector.x + vector.z != 0.0) UP_VECTOR else FORWARD_VECTOR,
    thickness: Float = .1f,
    interpolation: Int = 1,
    init: (BlockDisplay) -> Unit = {},
    update: (BlockDisplay) -> Unit = {},
) = blockRenderEntity(
    level = level,
    position = position,
    init = {
        it.setTeleportDuration(interpolation)
        it.setInterpolationDuration(interpolation)
        init(it)
    },
    update = {
        val matrix = Matrix4f().rotateTowards(vector.toVector3f(), upVector.toVector3f())
            .translate(-thickness / 2, -thickness / 2, 0f)
            .scale(thickness, thickness, vector.length().toFloat())

        it.applyTransformationWithInterpolation(matrix)
        update(it)
    },
)

/** Create a [TextDisplay] render entity. */
fun textRenderEntity(
    level: Level,
    position: Vec3,
    text: String,
    interpolation: Int,
    init: (TextDisplay) -> Unit = {},
    update: (TextDisplay) -> Unit = {},
) = RenderEntity(
    type = EntityType.TEXT_DISPLAY,
    level = level,
    position = position,
    init = {
        it.setTeleportDuration(interpolation)
        it.setInterpolationDuration(interpolation)
        it.setBillboard(Display.Billboard.CENTER)
        init(it)
    },
    update = {
        it.setText(Component.literal(text))
        update(it)
    },
)

/** Create a simple marker [BlockDisplay] at the given [position]. */
fun vec3MarkerRenderEntity(
    level: Level,
    position: Vec3,
) = blockRenderEntity(
    level = level,
    position = position,
    init = {
        it.blockState = Blocks.REDSTONE_BLOCK.defaultBlockState()
        it.setTeleportDuration(1)
        it.setInterpolationDuration(1)
        it.setBrightness(Brightness(15, 15))
        it.transformation = centredTransform(.25f, .25f, .25f)
    },
)

/** Renderer for a single entity instance. */
class SingleEntityRenderer<T : Entity> : Closeable {
    var entity: T? = null

    fun render(part: RenderEntity<T>) {
        entity = (entity ?: spawnEntity(part.level, part.position, part.type) {
            part.init(it)
        }).apply {
            this.moveTo(part.position.x, part.position.y, part.position.z, this.yRot, this.xRot)
            part.update(this)
        }
    }

    fun renderIf(predicate: Boolean, entity: RenderEntity<T>) {
        if (predicate) render(entity) else close()
    }

    override fun close() {
        entity?.discard()
        entity = null
    }
}

/** Renderer that maintains a group of entities keyed by id. */
class GroupEntityRenderer : Closeable {
    val rendered = mutableMapOf<Any, Entity>()
    private val used = mutableSetOf<Any>()

    fun detachEntity(id: Any) {
        rendered.remove(id)
    }

    override fun close() {
        for (entity in rendered.values) {
            entity.discard()
        }
        rendered.clear()
        used.clear()
    }

    fun render(group: RenderEntityGroup) {
        for ((id, template) in group.items) {
            renderPart(id, template)
        }

        val toRemove = rendered.keys - used
        for (key in toRemove) {
            rendered[key]?.discard()
            rendered.remove(key)
        }
        used.clear()
    }

    fun <T : Entity> render(part: RenderEntity<T>) {
        val group = RenderEntityGroup().apply { add(0, part) }
        render(group)
    }

    private fun <T : Entity> renderPart(id: Any, template: RenderEntity<T>) {
        used.add(id)

        val oldEntity = rendered[id]
        if (oldEntity != null) {
            if (oldEntity.type == template.type) {
                oldEntity.moveTo(
                    template.position.x,
                    template.position.y,
                    template.position.z,
                    oldEntity.yRot,
                    oldEntity.xRot
                )
                @Suppress("UNCHECKED_CAST")
                template.update(oldEntity as T)
                return
            }
            oldEntity.discard()
            rendered.remove(id)
        }

        val entity = spawnEntity(template.level, template.position, template.type) {
            template.init(it)
            template.update(it)
        }
        rendered[id] = entity
    }
}

/** Renderer that manages multiple [RenderEntityGroup] instances keyed by id. */
class MultiEntityRenderer : Closeable {
    private val groups = mutableMapOf<Any, GroupEntityRenderer>()
    private val used = mutableSetOf<Any>()

    val rendered: Map<Any, Entity>
        get() = groups.flatMap { (id, grp) ->
            grp.rendered.map { id to it.value }
        }.toMap()

    /** Render the given [group] associated with [id]. */
    fun render(id: Any, group: RenderEntityGroup) {
        val renderer = groups.getOrPut(id) { GroupEntityRenderer() }
        renderer.render(group)
        used += id
    }

    /** Detach the renderer for the supplied [id], discarding any entities. */
    fun detach(id: Any) {
        groups.remove(id)?.close()
    }

    /** Remove any groups that were not rendered since the last [flush] call. */
    fun flush() {
        val toRemove = groups.keys - used
        for (key in toRemove) {
            groups.remove(key)?.close()
        }
        used.clear()
    }

    override fun close() {
        for (renderer in groups.values) renderer.close()
        groups.clear()
        used.clear()
    }
}

