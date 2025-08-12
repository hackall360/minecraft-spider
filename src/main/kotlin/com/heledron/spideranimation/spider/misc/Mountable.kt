package com.heledron.spideranimation.spider.misc

import com.heledron.spideranimation.spider.Spider
import com.heledron.spideranimation.spider.SpiderComponent
import com.heledron.spideranimation.utilities.RenderEntity
import com.heledron.spideranimation.utilities.SingleEntityRenderer
import com.heledron.spideranimation.utilities.onServerTick
import com.heledron.spideranimation.utilities.playSound
import net.minecraft.server.level.ServerPlayer
import net.minecraft.sounds.SoundEvents
import net.minecraft.world.InteractionHand
import net.minecraft.world.entity.animal.Pig
import net.minecraft.world.entity.EntityType
import net.minecraft.world.entity.decoration.ArmorStand
import net.minecraft.world.item.Items
import net.minecraft.world.phys.Vec3
import net.minecraftforge.common.MinecraftForge
import net.minecraftforge.event.entity.EntityMountEvent
import net.minecraftforge.event.entity.player.PlayerInteractEvent
import net.minecraftforge.eventbus.api.SubscribeEvent
import org.joml.Quaternionf
import java.io.Closeable

class Mountable(val spider: Spider): SpiderComponent {
    val pig = SingleEntityRenderer<Pig>()
    var marker = SingleEntityRenderer<ArmorStand>()

    var closable = mutableListOf<Closeable>()

    fun getRider() = marker.entity?.passengers?.firstOrNull() as? ServerPlayer

    init {
        closable += pig
        closable += marker

        val interactListener = object {
            @SubscribeEvent
            fun onInteract(event: PlayerInteractEvent.EntityInteract) {
                if (event.entity.level().isClientSide) return

                val pigEntity = pig.entity ?: return
                if (event.target != pigEntity) return
                if (event.hand != InteractionHand.MAIN_HAND) return

                val player = event.entity as? ServerPlayer ?: return
                val stack = player.mainHandItem

                // if right click with saddle, add saddle (automatic)
                if (stack.`is`(Items.SADDLE) && !pigEntity.isSaddled) {
                    playSound(pigEntity.level(), pigEntity.position(), SoundEvents.PIG_SADDLE, 1.0f, 1.0f)
                }

                // if right click with empty hand, remove saddle
                if (stack.isEmpty && getRider() == null && player.isShiftKeyDown) {
                    pigEntity.setSaddle(false)
                }
            }
        }
        MinecraftForge.EVENT_BUS.register(interactListener)
        closable += Closeable { MinecraftForge.EVENT_BUS.unregister(interactListener) }

        // when player mounts the pig, switch them to the marker entity
        val mountListener = object {
            @SubscribeEvent
            fun onMount(event: EntityMountEvent) {
                if (event.entityMounting.level().isClientSide) return
                if (!event.isMounting) return
                val pigEntity = pig.entity ?: return
                if (event.entityBeingMounted != pigEntity) return
                val player = event.entityMounting as? ServerPlayer ?: return
                val markerEntity = marker.entity ?: return

                event.isCanceled = true
                markerEntity.addPassenger(player)
            }
        }
        MinecraftForge.EVENT_BUS.register(mountListener)
        closable += Closeable { MinecraftForge.EVENT_BUS.unregister(mountListener) }

        closable += onServerTick {
            val player = getRider() ?: return@onServerTick

            var input = Vec3(player.xxa.toDouble(), 0.0, player.zza.toDouble())

            if (input.lengthSqr() != 0.0) {
                val yaw = Math.toRadians(player.yRot.toDouble()).toFloat()
                val rotation = Quaternionf().rotationYXZ(yaw, 0f, 0f)
                input = input.rotate(rotation).normalize()
            }

            val look = player.lookAngle
            spider.behaviour = DirectionBehaviour(
                spider,
                Vec3(look.x, look.y, look.z),
                Vec3(input.x, input.y, input.z)
            )

        }
    }

    override fun render() {
        val location = spider.position.add(spider.velocity)

        val pigLocation = location.add(Vec3(.0, -.6, .0))
        val markerLocation = location.add(Vec3(.0, .3, .0))

        pig.render(RenderEntity(
            type = EntityType.PIG,
            level = spider.world,
            position = pigLocation,
            init = {
                it.setNoGravity(true)
                it.setNoAi(true)
                it.isInvisible = true
                it.setInvulnerable(true)
                it.isSilent = true
                it.setNoPhysics(true)
            }
        ))

        marker.render(RenderEntity(
            type = EntityType.ARMOR_STAND,
            level = spider.world,
            position = markerLocation,
            init = {
                it.setNoGravity(true)
                it.isInvisible = true
                it.setInvulnerable(true)
                it.isSilent = true
                it.setNoPhysics(true)
                it.setMarker(true)
            },
            update = update@{ markerEntity ->
                if (getRider() == null) return@update

                // absMoveTo preserves passengers when teleporting.
                markerEntity.absMoveTo(markerLocation.x, markerLocation.y, markerLocation.z)
            }
        ))
    }

    override fun close() {
        closable.forEach { it.close() }
    }
}
