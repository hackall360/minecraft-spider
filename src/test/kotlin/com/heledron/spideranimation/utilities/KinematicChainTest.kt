package com.heledron.spideranimation.utilities

import net.minecraft.world.phys.Vec3
import org.joml.Quaternionf
import org.joml.Vector3f
import kotlin.math.PI
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertNotSame
import kotlin.test.assertTrue

class KinematicChainTest {
    @Test
    fun `clone produces deep copy`() {
        val original = ChainSegment(
            Vec3(1.0, 2.0, 3.0),
            1.0,
            Vec3(0.0, 1.0, 0.0),
        )
        val copy = original.clone()

        original.position = Vec3(4.0, 5.0, 6.0)
        original.initDirection = Vec3(1.0, 0.0, 0.0)

        assertEquals(Vec3(1.0, 2.0, 3.0), copy.position)
        assertEquals(Vec3(0.0, 1.0, 0.0), copy.initDirection)
        assertNotSame(original.position, copy.position)
        assertNotSame(original.initDirection, copy.initDirection)
    }

    @Test
    fun `straightenDirection rotates segments and updates vectors and rotations`() {
        val root = Vec3(0.0, 0.0, 0.0)
        val segments = listOf(
            ChainSegment(Vec3(5.0, 5.0, 5.0), 1.0, Vec3(1.0, 0.0, 0.0)),
            ChainSegment(Vec3(5.0, 5.0, 5.0), 1.0, Vec3(1.0, 0.0, 0.0)),
            ChainSegment(Vec3(5.0, 5.0, 5.0), 1.0, Vec3(1.0, 0.0, 0.0)),
        )
        val chain = KinematicChain(root, segments)

        val rotation = Quaternionf().rotationY((PI / 2).toFloat())
        chain.straightenDirection(rotation)

        assertEquals(Vec3(0.0, 0.0, -1.0), segments[0].position)
        assertEquals(Vec3(0.0, 0.0, -2.0), segments[1].position)
        assertEquals(Vec3(0.0, 0.0, -3.0), segments[2].position)

        val vectors = chain.getVectors()
        assertEquals(Vec3(0.0, 0.0, -1.0), vectors[0])
        assertEquals(Vec3(0.0, 0.0, -1.0), vectors[1])
        assertEquals(Vec3(0.0, 0.0, -1.0), vectors[2])

        val rotations = chain.getRotations(Quaternionf())
        val euler = rotations[0].getEulerAnglesYXZ(Vector3f())
        assertEquals(PI.toFloat(), euler.y, 1e-5f)
        assertEquals(0f, euler.x, 1e-5f)
    }

    @Test
    fun `fabrikForward and fabrikBackward constrain chain within reach`() {
        val chain = KinematicChain(
            Vec3(0.0, 0.0, 0.0),
            listOf(
                ChainSegment(Vec3(1.0, 0.0, 0.0), 1.0, Vec3(1.0, 0.0, 0.0)),
                ChainSegment(Vec3(2.0, 0.0, 0.0), 1.0, Vec3(1.0, 0.0, 0.0)),
            )
        )

        chain.fabrikForward(Vec3(3.0, 0.0, 0.0))
        chain.fabrikBackward()

        assertEquals(Vec3(2.0, 0.0, 0.0), chain.getEndEffector())
        val vectors = chain.getVectors()
        assertEquals(Vec3(1.0, 0.0, 0.0), vectors[0])
        assertEquals(Vec3(1.0, 0.0, 0.0), vectors[1])

        val rotations = chain.getRotations(Quaternionf())
        val euler = rotations[0].getEulerAnglesYXZ(Vector3f())
        assertEquals((-PI / 2).toFloat(), euler.y, 1e-5f)
        assertEquals(0f, euler.x, 1e-5f)
    }

    @Test
    fun `fabrik reaches target and updates chain`() {
        val chain = KinematicChain(
            Vec3(0.0, 0.0, 0.0),
            listOf(
                ChainSegment(Vec3(1.0, 0.0, 0.0), 1.0, Vec3(1.0, 0.0, 0.0)),
                ChainSegment(Vec3(2.0, 0.0, 0.0), 1.0, Vec3(1.0, 0.0, 0.0)),
            )
        )

        val target = Vec3(0.0, 0.0, 2.0)
        chain.fabrik(target)

        val end = chain.getEndEffector()
        assertEquals(target.x, end.x, 1e-6)
        assertEquals(target.y, end.y, 1e-6)
        assertEquals(target.z, end.z, 1e-6)

        val vectors = chain.getVectors()
        assertEquals(Vec3(0.0, 0.0, 1.0), vectors[0])
        assertEquals(Vec3(0.0, 0.0, 1.0), vectors[1])

        val rotations = chain.getRotations(Quaternionf())
        val euler = rotations[0].getEulerAnglesYXZ(Vector3f())
        assertEquals(0f, euler.y, 1e-5f)
        assertEquals(0f, euler.x, 1e-5f)
    }

    @Test
    fun `fabrik handles zero length segment`() {
        val chain = KinematicChain(
            Vec3(0.0, 0.0, 0.0),
            listOf(
                ChainSegment(Vec3(0.0, 0.0, 0.0), 0.0, Vec3(1.0, 0.0, 0.0)),
                ChainSegment(Vec3(1.0, 0.0, 0.0), 1.0, Vec3(1.0, 0.0, 0.0)),
            )
        )

        chain.fabrik(Vec3(1.0, 0.0, 0.0))

        assertEquals(Vec3(0.0, 0.0, 0.0), chain.segments[0].position)
        assertEquals(Vec3(1.0, 0.0, 0.0), chain.segments[1].position)

        val vectors = chain.getVectors()
        assertEquals(Vec3(0.0, 0.0, 0.0), vectors[0])
        assertEquals(Vec3(1.0, 0.0, 0.0), vectors[1])

        val rotations = chain.getRotations(Quaternionf())
        assertTrue(rotations.all { !it.x.isNaN() && !it.y.isNaN() && !it.z.isNaN() && !it.w.isNaN() })
    }
}
