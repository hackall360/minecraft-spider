package com.heledron.spideranimation.kinematic_chain_visualizer

import com.heledron.spideranimation.spider.configuration.SegmentPlan
import com.heledron.spideranimation.utilities.*
import org.bukkit.Location
import org.bukkit.Material
import org.bukkit.World
import org.bukkit.entity.Display
import org.bukkit.util.Vector
import org.joml.Matrix4f
import org.joml.Quaternionf
import java.io.Closeable


class KinematicChainVisualizer(
//    val root: Location,
    val world: World,
    val root: Vector,
    val segments: List<ChainSegment>,
    val segmentPlans: List<SegmentPlan>,
    val straightenRotation: Float
): Closeable {
    enum class Stage {
        Backwards,
        Forwards
    }

    val interruptions = mutableListOf<() -> Unit>()
    var iterator = 0
    var previous: Triple<Stage, Int, List<ChainSegment>>? = null
    var stage = Stage.Forwards
    var target: Location? = null

    var detailed = false
    set(value) {
        field = value
        interruptions.clear()
        render()
    }

    val renderer = GroupEntityRenderer()
    override fun close() {
        renderer.close()
    }

    init {
        reset()
        render()
    }

    companion object {
        fun create(
            segmentPlans: List<SegmentPlan>,
            root: Vector,
            world: World,
            straightenRotation: Float
        ): KinematicChainVisualizer {
            val segments = segmentPlans.map { ChainSegment(root.clone(), it.length, it.initDirection) }
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

        val direction = Vector(0, 1, 0)
        val rotAxis = Vector(1, 0, -1)
        val rotation = -0.2
        val pos = root.clone()
        for (segment in segments) {
            direction.rotateAroundAxis(rotAxis, rotation)
            pos.add(direction.clone().multiply(segment.length))
            segment.position.copy(pos)
        }

        render()
    }

    fun step() {
        if (interruptions.isNotEmpty()) {
            interruptions.removeAt(0)()
            return
        }

        val target = target?.toVector() ?: return

        previous = Triple(stage, iterator, segments.map { it.clone() })

        if (stage == Stage.Forwards) {
            val segment = segments[iterator]
            val nextSegment = segments.getOrNull(iterator + 1)

            if (nextSegment == null) {
                segment.position.copy(target)
            } else {
                fabrik_moveSegment(segment.position, nextSegment.position, nextSegment.length)
            }

            if (iterator == 0) stage = Stage.Backwards
            else iterator--
        } else {
            val segment = segments[iterator]
            val prevPosition = segments.getOrNull(iterator - 1)?.position ?: root

            fabrik_moveSegment(segment.position, prevPosition, segment.length)

            if (iterator == segments.size - 1) stage = Stage.Forwards
            else iterator++
        }

        render()
    }

    fun straighten(target: Vector) {
        resetIterator()

        val pivot = Quaternionf()

        val direction = target.clone().subtract(root).normalize()
        val rotation = direction.getRotationAroundAxis(pivot)

        rotation.x += straightenRotation
        val orientation = pivot.rotateYXZ(rotation.y, rotation.x, .0f)

        KinematicChain(root, segments).straightenDirection(orientation)

        render()
    }

    fun fabrik_moveSegment(point: Vector, pullTowards: Vector, segment: Double) {
        val direction = pullTowards.clone().subtract(point).normalize()
        point.copy(pullTowards).subtract(direction.multiply(segment))
    }

    fun render() {
        if (detailed) {
            renderer.render(renderDetailed())
        } else {
            val model = renderNormal()
            renderer.render(model)
        }

    }

    private fun renderNormal(): RenderEntityGroup {
        val group = RenderEntityGroup()

        val pivot = Quaternionf()

        val previous = previous
        for (i in segments.indices) {
            val segmentPlan = segmentPlans[i]
            val segment = segments[i]

            val list = if (previous == null || i == previous.second) segments else previous.third

            val prev = list.getOrNull(i - 1)?.position ?: root.clone()
            val vector = segment.position.clone().subtract(prev)
            if (!vector.isZero) vector.normalize().multiply(segment.length)
            val position = segment.position.clone().subtract(vector.clone())

            val rotation = KinematicChain(root, list).getRotations(pivot)[i]
            val transform = Matrix4f().rotate(rotation)

            for (piece in segmentPlan.model.pieces) {
                group.add(i to piece, blockRenderEntity(
                    world = world,
                    position = position,
                    init = {
                        it.teleportDuration = 3
                        it.interpolationDuration = 3
                    },
                    update = {
                        val pieceTransform = Matrix4f(transform).mul(piece.transform)
                        it.applyTransformationWithInterpolation(pieceTransform)
                        it.block = piece.block
                        it.brightness = piece.brightness
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

        if (previous != null) run arrow@{
            val (stage, iterator, segments) = previous

            val arrowStart = if (stage == Stage.Forwards)
                segments.getOrNull(iterator + 1)?.position else
                segments.getOrNull(iterator - 1)?.position ?: root

            if (arrowStart == null) return@arrow
            renderedSegments = segments

            if (subStage == 0) {
                interruptions += { renderer.render(renderDetailed(1)) }
                interruptions += { renderer.render(renderDetailed(2)) }
                interruptions += { renderer.render(renderDetailed(3)) }
                interruptions += { renderer.render(renderDetailed(4)) }
            }

            // stage 0: subtract vector
            val arrow = segments[iterator].position.clone().subtract(arrowStart)

            // stage 1: normalise vector
            if (subStage >= 1) arrow.normalize()

            // stage 2: multiply by length
            if (subStage >= 2) arrow.multiply(segments[iterator].length)

            // stage 3: move segment
            if (subStage >= 3) renderedSegments = this.segments

            // stage 4: hide arrow
            if (subStage >= 4) return@arrow


            val crossProduct = if (arrow == UP_VECTOR) FORWARD_VECTOR else
                arrow.clone().crossProduct(UP_VECTOR).normalize()

            val arrowCenter = arrowStart.clone()
                .add(arrow.clone().multiply(0.5))
                .add(crossProduct.rotateAroundAxis(arrow, Math.toRadians(-90.0)).multiply(.5))

            group.add("arrow_length", textRenderEntity(
                world = world,
                position = arrowCenter,
                text = String.format("%.2f", arrow.length()),
                interpolation = 3,
            ))

            group.add("arrow", arrowRenderEntity(
                world = world,
                position = arrowStart,
                vector = arrow,
                thickness = .101f,
                interpolation = 3,
            ))
        }

        group.add("root", pointRenderEntity(world, root, Material.DIAMOND_BLOCK))

        for (i in renderedSegments.indices) {
            val segment = renderedSegments[i]
            group.add("p$i", pointRenderEntity(world, segment.position, Material.EMERALD_BLOCK))

            val prev = renderedSegments.getOrNull(i - 1)?.position ?: root

            val (a,b) = prev to segment.position

            group.add(i, lineRenderEntity(
                world = world,
                position = a,
                vector = b.clone().subtract(a),
                thickness = .1f,
                interpolation = 3,
                update = {
                    it.brightness = Display.Brightness(0, 15)
                    it.block = Material.BLACK_STAINED_GLASS.createBlockData()
                }
            ))
        }

        return group
    }
}

fun pointRenderEntity(world: World, position: Vector, block: Material) = blockRenderEntity(
    world = world,
    position = position,
    init = {
        it.block = block.createBlockData()
        it.teleportDuration = 3
        it.brightness = Display.Brightness(15, 15)
        it.transformation = centredTransform(.26f, .26f, .26f)
    }
)

fun arrowRenderEntity(
    world: World,
    position: Vector,
    vector: Vector,
    thickness: Float,
    interpolation: Int
): RenderEntityGroup {
    val line = lineRenderEntity(
        world = world,
        position = position,
        vector = vector,
        thickness = thickness,
        interpolation = interpolation,
        init = {
            it.block = Material.GOLD_BLOCK.createBlockData()
            it.brightness = Display.Brightness(0, 15)
        },
    )

    val tipLength = 0.5
    val tip = position.clone().add(vector)
    val crossProduct = if (vector == UP_VECTOR) FORWARD_VECTOR else
        vector.clone().crossProduct(UP_VECTOR).normalize().multiply(tipLength)

    val tipDirection = vector.clone().normalize().multiply(-tipLength)
    val tipRotation = 30.0

    val top = lineRenderEntity(
        world = world,
        position = tip,
        vector = tipDirection.clone().rotateAroundAxis(crossProduct, Math.toRadians(tipRotation)),
        thickness = thickness,
        interpolation = interpolation,
        init = {
            it.block = Material.GOLD_BLOCK.createBlockData()
            it.brightness = Display.Brightness(0, 15)
        },
    )

    val bottom = lineRenderEntity(
        world = world,
        position = tip,
        vector = tipDirection.clone().rotateAroundAxis(crossProduct, Math.toRadians(-tipRotation)),
        thickness = thickness,
        interpolation = interpolation,
        init = {
            it.block = Material.GOLD_BLOCK.createBlockData()
            it.brightness = Display.Brightness(0, 15)
        },
    )

    return RenderEntityGroup().apply {
        add("line", line)
        add("top", top)
        add("bottom", bottom)
    }
}
