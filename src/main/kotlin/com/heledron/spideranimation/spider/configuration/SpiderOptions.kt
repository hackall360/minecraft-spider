package com.heledron.spideranimation.spider.configuration

import com.heledron.spideranimation.utilities.playSound
import net.minecraft.sounds.SoundEvent
import net.minecraft.sounds.SoundEvents
import net.minecraft.world.level.Level
import net.minecraft.world.phys.Vec3
import kotlin.random.Random

class SpiderOptions {
    var walkGait = Gait.defaultWalk()
    var gallopGait = Gait.defaultGallop()

    var cloak = CloakOptions()

    var bodyPlan = BodyPlan()
    var debug = SpiderDebugOptions()

    var sound = SoundOptions()

    fun scale(scale: Double) {
        walkGait.scale(scale)
        gallopGait.scale(scale)
        bodyPlan.scale(scale)
    }
}



class SoundOptions {
    var step = SoundPlayer(
        sound = SoundEvents.NETHERITE_BLOCK_STEP,
        volume = .3f,
        pitch = 1.0f
    )
}


class SoundPlayer(
    val sound: SoundEvent,
    val volume: Float,
    val pitch: Float,
    val volumeVary: Float = 0.1f,
    val pitchVary: Float = 0.1f
) {
    fun play(level: Level, position: Vec3) {
        val volume = volume + Random.nextFloat() * volumeVary
        val pitch = pitch + Random.nextFloat() * pitchVary
        playSound(level, position, sound, volume, pitch)
    }
}