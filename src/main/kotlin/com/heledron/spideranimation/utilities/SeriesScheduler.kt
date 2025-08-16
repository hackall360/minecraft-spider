package com.heledron.spideranimation.utilities

class SeriesScheduler {
    var time: Long = 0

    fun sleep(duration: Long) = apply {
        time += duration
    }

    fun run(task: () -> Unit) = apply {
        Scheduler.runLater(time, task)
    }
}
