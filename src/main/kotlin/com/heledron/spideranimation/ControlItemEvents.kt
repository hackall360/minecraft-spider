package com.heledron.spideranimation

import com.heledron.spideranimation.spider.misc.DirectionBehaviour
import com.heledron.spideranimation.spider.misc.TargetBehaviour
import com.heledron.spideranimation.spider.rendering.SpiderParticleRenderer
import com.heledron.spideranimation.spider.rendering.SpiderRenderer
import com.heledron.spideranimation.utilities.AppState
import com.heledron.spideranimation.utilities.raycastGround
import net.minecraft.network.chat.Component
import net.minecraft.server.level.ServerLevel
import net.minecraft.server.level.ServerPlayer
import net.minecraft.sounds.SoundEvents
import net.minecraft.sounds.SoundSource
import net.minecraft.world.InteractionResult
import net.minecraft.world.phys.Vec3
import net.minecraftforge.event.TickEvent
import net.minecraftforge.event.entity.player.PlayerInteractEvent
import net.minecraftforge.eventbus.api.SubscribeEvent
import kotlin.math.roundToInt

/**
 * Handles interactions with control items using Forge events.
 */
object ControlItemEvents {
    @SubscribeEvent
    fun onRightClick(event: PlayerInteractEvent.RightClickItem) {
        val player = event.entity
        val level = player.level()
        if (level.isClientSide) return
        val serverPlayer = player as? ServerPlayer ?: return
        val tool = event.itemStack.getControlItem() ?: return
        when (tool) {
            ControlItem.SPIDER -> {
                val spider = AppState.spider
                if (spider == null) {
                    val yawIncrements = 45.0f
                    val yaw = serverPlayer.yHeadRot
                    val yawRounded = (yaw / yawIncrements).roundToInt() * yawIncrements
                    val eye = serverPlayer.eyePosition
                    val result = level.raycastGround(eye, serverPlayer.lookAngle, 100.0) ?: return
                    val hit = result.location
                    val orientation = org.joml.Quaternionf().rotateY(Math.toRadians(yawRounded.toDouble()).toFloat())
                    level.playSound(null, hit.x, hit.y, hit.z, SoundEvents.NETHERITE_BLOCK_PLACE, SoundSource.BLOCKS, 1.0f, 1.0f)
                    AppState.createSpider(level as ServerLevel, hit, orientation)
                    serverPlayer.displayClientMessage(Component.literal("Spider created"), true)
                } else {
                    level.playSound(null, serverPlayer.x, serverPlayer.y, serverPlayer.z, SoundEvents.ITEM_FRAME_REMOVE_ITEM, SoundSource.BLOCKS, 1.0f, 0.0f)
                    AppState.spider = null
                    serverPlayer.displayClientMessage(Component.literal("Spider removed"), true)
                }
            }
            ControlItem.DISABLE_LEG -> {
                val selectedLeg = AppState.spider?.pointDetector?.selectedLeg
                if (selectedLeg == null) {
                    level.playSound(null, serverPlayer.x, serverPlayer.y, serverPlayer.z, SoundEvents.DISPENSER_FAIL, SoundSource.BLOCKS, 1.0f, 2.0f)
                } else {
                    selectedLeg.isDisabled = !selectedLeg.isDisabled
                    level.playSound(null, serverPlayer.x, serverPlayer.y, serverPlayer.z, SoundEvents.LANTERN_PLACE, SoundSource.BLOCKS, 1.0f, 1.0f)
                }
            }
            ControlItem.TOGGLE_DEBUG -> {
                AppState.showDebugVisuals = !AppState.showDebugVisuals
                SpiderConfig.SHOW_DEBUG.set(AppState.showDebugVisuals)
                SpiderConfig.save()
                AppState.chainVisualizer?.detailed = AppState.showDebugVisuals
                val pitch = if (AppState.showDebugVisuals) 2.0f else 1.5f
                level.playSound(null, serverPlayer.x, serverPlayer.y, serverPlayer.z, SoundEvents.DISPENSER_FAIL, SoundSource.BLOCKS, 1.0f, pitch)
            }
            ControlItem.SWITCH_RENDERER -> {
                val spider = AppState.spider ?: return
                spider.renderer = if (spider.renderer is SpiderRenderer) {
                    level.playSound(null, serverPlayer.x, serverPlayer.y, serverPlayer.z, SoundEvents.AXOLOTL_ATTACK, SoundSource.BLOCKS, 1.0f, 1.0f)
                    SpiderParticleRenderer(spider)
                } else {
                    level.playSound(null, serverPlayer.x, serverPlayer.y, serverPlayer.z, SoundEvents.ARMOR_EQUIP_NETHERITE, SoundSource.BLOCKS, 1.0f, 1.0f)
                    SpiderRenderer(spider)
                }
            }
            ControlItem.TOGGLE_CLOAK -> {
                AppState.spider?.cloak?.toggleCloak()
            }
            ControlItem.CHAIN_VIS_STEP -> {
                val chain = AppState.chainVisualizer ?: return
                level.playSound(null, serverPlayer.x, serverPlayer.y, serverPlayer.z, SoundEvents.DISPENSER_FAIL, SoundSource.BLOCKS, 1.0f, 2.0f)
                chain.step()
            }
            ControlItem.CHAIN_VIS_STRAIGHTEN -> {
                val chain = AppState.chainVisualizer ?: return
                val target = chain.target ?: return
                level.playSound(null, serverPlayer.x, serverPlayer.y, serverPlayer.z, SoundEvents.DISPENSER_FAIL, SoundSource.BLOCKS, 1.0f, 2.0f)
                chain.straighten(target)
            }
            ControlItem.SWITCH_GAIT -> {
                level.playSound(null, serverPlayer.x, serverPlayer.y, serverPlayer.z, SoundEvents.DISPENSER_FAIL, SoundSource.BLOCKS, 1.0f, 2.0f)
                AppState.gallop = !AppState.gallop
                SpiderConfig.GALLOP.set(AppState.gallop)
                SpiderConfig.save()
                val message = if (!AppState.gallop) "Walk mode" else "Gallop mode"
                serverPlayer.displayClientMessage(Component.literal(message), true)
            }
            ControlItem.LASER_POINTER, ControlItem.COME_HERE -> return
        }
        event.cancellationResult = InteractionResult.SUCCESS
        event.isCanceled = true
    }

    @SubscribeEvent
    fun onPlayerTick(event: TickEvent.PlayerTickEvent) {
        if (event.phase != TickEvent.Phase.END) return
        val player = event.player
        if (player.level().isClientSide) return
        val serverPlayer = player as? ServerPlayer ?: return
        for (stack in listOf(serverPlayer.mainHandItem, serverPlayer.offhandItem)) {
            when (stack.getControlItem()) {
                ControlItem.DISABLE_LEG -> {
                    AppState.spider?.pointDetector?.player = serverPlayer
                }
                ControlItem.LASER_POINTER -> {
                    val eye = serverPlayer.eyePosition
                    val directionVec = serverPlayer.lookAngle
                    val result = player.level().raycastGround(eye, directionVec, 100.0)
                    if (result == null) {
                        val direction = Vec3(directionVec.x, 0.0, directionVec.z).normalize()
                        AppState.spider?.let { it.behaviour = DirectionBehaviour(it, direction, direction) }
                        AppState.chainVisualizer?.let {
                            it.target = null
                            it.resetIterator()
                        }
                    } else {
                        val targetVal = result.location
                        AppState.target = targetVal
                        AppState.chainVisualizer?.let {
                            it.target = targetVal
                            it.resetIterator()
                        }
                        AppState.spider?.let { it.behaviour = TargetBehaviour(it, targetVal, it.lerpedGait.bodyHeight) }
                    }
                }
                ControlItem.COME_HERE -> {
                    AppState.spider?.let {
                        val height = if (it.gait.straightenLegs) it.lerpedGait.bodyHeight * 2.0 else it.lerpedGait.bodyHeight * 5.0
                        val eye = serverPlayer.eyePosition
                        it.behaviour = TargetBehaviour(it, Vec3(eye.x, eye.y, eye.z), height)
                    }
                }
                else -> {}
            }
        }
    }
}
