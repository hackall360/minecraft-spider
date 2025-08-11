package com.heledron.spideranimation.kinematic_chain_visualizer

import com.heledron.spideranimation.AppState
import com.heledron.spideranimation.spider.configuration.SegmentPlan
import com.heledron.spideranimation.utilities.*
import net.minecraft.server.level.ServerLevel
import net.minecraft.world.entity.Display
import net.minecraft.world.level.block.Block
import net.minecraft.world.level.block.Blocks
import net.minecraft.world.phys.Vec3
import org.joml.Matrix4f
import org.joml.Quaternionf
import java.io.Closeable

class KinematicChainVisualizer(
    val world: ServerLevel,
    val root: Vec3,
    val segments: List<ChainSegment>,
    val segmentPlans: List<SegmentPlan>,
    val straightenRotation: Float,
) : Closeable {
    enum class Stage { Backwards, Forwards }

    val interruptions = mutableListOf<() -> Unit>()
    var iterator = 0
    var previous: Triple<Stage, Int, List<ChainSegment>>? = null
    var stage = Stage.Forwards
    var target: Vec3? = null

    var detailed = false
        set(value) {
            field = value
            interruptions.clear()
            render()
        }

    init {
        reset()
        render()
    }

    override fun close() {
        AppState.renderer.detach(this)
    }

    companion object {
        fun create(
            segmentPlans: List<SegmentPlan>,
            root: Vec3,
            world: ServerLevel,
            straightenRotation: Float
        ): KinematicChainVisualizer {
            val segments = segmentPlans.map { ChainSegment(root, it.length, it.initDirection) }
            return KinematicChainVisualizer(world, root, segments, segmentPlans, straightenRotation)
        }
    }

    fun resetIterator() {
        interruptions.clear()
        iterator = segments.size - 1
        previous = null
        stage = Stage.Forwards
    }

    fun reset() {
        resetIterator()

        target = null

        var direction = Vec3(0.0, 1.0, 0.0)
        val rotAxis = Vec3(1.0, 0.0, -1.0)
        val rotation = -0.2
        var pos = root
        for (segment in segments) {
            direction = direction.rotateAroundAxis(rotAxis, rotation)
            pos = pos.add(direction.scale(segment.length))
            segment.position = pos
        }

        render()
    }

    fun step() {
        if (interruptions.isNotEmpty()) {
            interruptions.removeAt(0)()
            return
        }

        val target = target ?: return

        previous = Triple(stage, iterator, segments.map { it.clone() })

        if (stage == Stage.Forwards) {
            val segment = segments[iterator]
            val nextSegment = segments.getOrNull(iterator + 1)

            if (nextSegment == null) {
                segment.position = target
            } else {
                segment.position = fabrik_moveSegment(segment.position, nextSegment.position, nextSegment.length)
            }

            if (iterator == 0) stage = Stage.Backwards else iterator--
        } else {
            val segment = segments[iterator]
            val prevPosition = segments.getOrNull(iterator - 1)?.position ?: root

            segment.position = fabrik_moveSegment(segment.position, prevPosition, segment.length)

            if (iterator == segments.size - 1) stage = Stage.Forwards else iterator++
        }

        render()
    }

    fun straighten(target: Vec3) {
        resetIterator()

        val pivot = Quaternionf()

        val direction = target.subtract(root).normalize()
        val rotation = direction.getRotationAroundAxis(pivot)

        rotation.x += straightenRotation
        val orientation = pivot.rotateYXZ(rotation.y, rotation.x, .0f)

        KinematicChain(root, segments).straightenDirection(orientation)

        render()
    }

    private fun fabrik_moveSegment(point: Vec3, pullTowards: Vec3, segment: Double): Vec3 {
        val direction = pullTowards.subtract(point).normalize()
        return pullTowards.subtract(direction.scale(segment))
    }

    fun render() {
        val group = if (detailed) renderDetailed() else renderNormal()
        AppState.renderer.render(this, group)
    }

    private fun renderNormal(): RenderEntityGroup {
        val group = RenderEntityGroup()

        val pivot = Quaternionf()

        val previous = previous
        for (i in segments.indices) {
            val segmentPlan = segmentPlans[i]
            val segment = segments[i]

            val list = if (previous == null || i == previous.second) segments else previous.third

            val prev = list.getOrNull(i - 1)?.position ?: root
            var vector = segment.position.subtract(prev)
            if (vector != Vec3.ZERO) vector = vector.normalize().scale(segment.length)
            val position = segment.position.subtract(vector)

            val rotation = KinematicChain(root, list).getRotations(pivot)[i]
            val transform = Matrix4f().rotate(rotation)

            for (piece in segmentPlan.model.pieces) {
                group.add(i to piece, blockRenderEntity(
                    level = world,
                    position = position,
                    init = {
                        it.setTeleportDuration(3)
                        it.setInterpolationDuration(3)
                    },
                    update = {
                        val pieceTransform = Matrix4f(transform).mul(piece.transform)
                        it.applyTransformationWithInterpolation(pieceTransform)
                        it.blockState = piece.block
                        it.setBrightness(piece.brightness)
                    }
                ))
            }
        }

        return group
    }

    private fun renderDetailed(subStage: Int = 0): RenderEntityGroup {
        val group = RenderEntityGroup()

        val previous = previous

        var renderedSegments = segments

        if (previous != null) run {
            val (stage, iterator, segmentsPrev) = previous

            val arrowStart = if (stage == Stage.Forwards)
                segmentsPrev.getOrNull(iterator + 1)?.position
            else
                segmentsPrev.getOrNull(iterator - 1)?.position ?: root

            if (arrowStart == null) return@run
            renderedSegments = segmentsPrev

            if (subStage == 0) {
                interruptions += { AppState.renderer.render(this@KinematicChainVisualizer, renderDetailed(1)) }
                interruptions += { AppState.renderer.render(this@KinematicChainVisualizer, renderDetailed(2)) }
                interruptions += { AppState.renderer.render(this@KinematicChainVisualizer, renderDetailed(3)) }
                interruptions += { AppState.renderer.render(this@KinematicChainVisualizer, renderDetailed(4)) }
            }

            var arrow = segmentsPrev[iterator].position.subtract(arrowStart)

            if (subStage >= 1) arrow = arrow.normalize()
            if (subStage >= 2) arrow = arrow.scale(segmentsPrev[iterator].length)
            if (subStage >= 3) renderedSegments = this.segments
            if (subStage >= 4) return@run

            val crossProduct = if (arrow == UP_VECTOR) FORWARD_VECTOR else
                arrow.cross(UP_VECTOR).normalize()

            val arrowCenter = arrowStart
                .add(arrow.scale(0.5))
                .add(crossProduct.rotateAroundAxis(arrow, Math.toRadians(-90.0)).scale(.5))

            group.add("arrow_length", textRenderEntity(
                level = world,
                position = arrowCenter,
                text = String.format("%.2f", arrow.length()),
                interpolation = 3,
            ))

            group.add("arrow", arrowRenderEntity(
                level = world,
                position = arrowStart,
                vector = arrow,
                thickness = .101f,
                interpolation = 3,
            ))
        }

        group.add("root", pointRenderEntity(world, root, Blocks.DIAMOND_BLOCK))

        for (i in renderedSegments.indices) {
            val segment = renderedSegments[i]
            group.add("p$i", pointRenderEntity(world, segment.position, Blocks.EMERALD_BLOCK))

            val prev = renderedSegments.getOrNull(i - 1)?.position ?: root

            val a = prev
            val b = segment.position

            group.add(i, lineRenderEntity(
                level = world,
                position = a,
                vector = b.subtract(a),
                thickness = .1f,
                interpolation = 3,
                update = {
                    it.setBrightness(Display.Brightness(0, 15))
                    it.blockState = Blocks.BLACK_STAINED_GLASS.defaultBlockState()
                }
            ))
        }

        return group
    }
}

private fun pointRenderEntity(level: ServerLevel, position: Vec3, block: Block) = blockRenderEntity(
    level = level,
    position = position,
    init = {
        it.blockState = block.defaultBlockState()
        it.setTeleportDuration(3)
        it.setBrightness(Display.Brightness(15, 15))
        it.transformation = centredTransform(.26f, .26f, .26f)
    }
)

private fun arrowRenderEntity(
    level: ServerLevel,
    position: Vec3,
    vector: Vec3,
    thickness: Float,
    interpolation: Int
): RenderEntityGroup {
    val line = lineRenderEntity(
        level = level,
        position = position,
        vector = vector,
        thickness = thickness,
        interpolation = interpolation,
        init = {
            it.blockState = Blocks.GOLD_BLOCK.defaultBlockState()
            it.setBrightness(Display.Brightness(0, 15))
        },
    )

    val tipLength = 0.5
    val tip = position.add(vector)
    val crossProduct = if (vector == UP_VECTOR) FORWARD_VECTOR else
        vector.cross(UP_VECTOR).normalize().scale(tipLength)

    val tipDirection = vector.normalize().scale(-tipLength)
    val tipRotation = 30.0

    val top = lineRenderEntity(
        level = level,
        position = tip,
        vector = tipDirection.rotateAroundAxis(crossProduct, Math.toRadians(tipRotation)),
        thickness = thickness,
        interpolation = interpolation,
        init = {
            it.blockState = Blocks.GOLD_BLOCK.defaultBlockState()
            it.setBrightness(Display.Brightness(0, 15))
        },
    )

    val bottom = lineRenderEntity(
        level = level,
        position = tip,
        vector = tipDirection.rotateAroundAxis(crossProduct, Math.toRadians(-tipRotation)),
        thickness = thickness,
        interpolation = interpolation,
        init = {
            it.blockState = Blocks.GOLD_BLOCK.defaultBlockState()
            it.setBrightness(Display.Brightness(0, 15))
        },
    )

    return RenderEntityGroup().apply {
        add("line", line)
        add("top", top)
        add("bottom", bottom)
    }
}

private fun Vec3.rotateAroundAxis(axis: Vec3, angle: Double): Vec3 {
    val q = Quaternionf().rotateAxis(angle.toFloat(), axis.toVector3f())
    val vec = this.toVector3f()
    q.transform(vec)
    return vec.toVec3()
}

