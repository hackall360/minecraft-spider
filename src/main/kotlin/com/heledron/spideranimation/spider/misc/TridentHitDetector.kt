package com.heledron.spideranimation.spider.misc

import com.heledron.spideranimation.spider.Spider
import com.heledron.spideranimation.spider.SpiderComponent
import com.heledron.spideranimation.utilities.EventEmitter
import com.heledron.spideranimation.utilities.UP_VECTOR
import com.heledron.spideranimation.utilities.runLater
import net.minecraft.world.entity.projectile.ThrownTrident
import net.minecraft.world.phys.AABB
import org.joml.Vector3f

class TridentHitDetector(val spider: Spider): SpiderComponent {
    val onHit = EventEmitter()
    var stunned = false

    init {
        onHit.listen {
//            stunned = true
            runLater(2) { stunned = false }
        }
    }

    override fun update() {
        val aabb = AABB(
            spider.position.x - 1.5, spider.position.y - 1.5, spider.position.z - 1.5,
            spider.position.x + 1.5, spider.position.y + 1.5, spider.position.z + 1.5
        )
        val tridents = spider.world.getEntitiesOfClass(ThrownTrident::class.java, aabb) {
            it.owner != spider.mount.getRider()
        }
        for (trident in tridents) {
            if (trident.deltaMovement.length() > 2.0) {
                val tridentDirection = trident.deltaMovement.normalize()

                trident.setDeltaMovement(tridentDirection.scale(-0.3))
                trident.hasImpulse = true
                onHit.emit()

                spider.velocity = spider.velocity.add(tridentDirection.scale(spider.gait.tridentKnockBack))

                // apply rotational acceleration
                val hitDirection = spider.position.subtract(trident.position()).normalize()
                val axis = UP_VECTOR.cross(tridentDirection)
                val angle = hitDirection.toVector3f().angle(UP_VECTOR.toVector3f())

                val accelerationMagnitude = angle * spider.gait.tridentRotationalKnockBack.toFloat()

                spider.accelerateRotation(axis, accelerationMagnitude)
            }
        }
    }
}