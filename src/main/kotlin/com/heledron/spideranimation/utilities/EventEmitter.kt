package com.heledron.spideranimation.utilities

import java.io.Closeable
import java.util.concurrent.CopyOnWriteArrayList

class EventEmitter {
    private val listeners = CopyOnWriteArrayList<() -> Unit>()

    fun listen(listener: () -> Unit): Closeable {
        listeners += listener
        return Closeable { listeners.remove(listener) }
    }

    fun emit() {
        for (listener in listeners) listener()
    }
}

