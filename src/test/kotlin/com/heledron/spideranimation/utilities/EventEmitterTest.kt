package com.heledron.spideranimation.utilities

import kotlin.test.Test
import kotlin.test.assertEquals

class EventEmitterTest {
    @Test
    fun `listeners are invoked and can be removed`() {
        val emitter = EventEmitter()
        var count = 0
        val handle = emitter.listen { count++ }
        emitter.emit()
        assertEquals(1, count)
        handle.close()
        emitter.emit()
        assertEquals(1, count)
    }
}
