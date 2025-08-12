package com.heledron.spideranimation

import com.heledron.spideranimation.utilities.*
import com.heledron.spideranimation.spider.misc.*
import com.heledron.spideranimation.spider.rendering.*
import net.minecraft.world.InteractionHand
import net.minecraft.world.InteractionResultHolder
import net.minecraft.world.entity.Entity
import net.minecraft.server.level.ServerLevel
import net.minecraft.server.level.ServerPlayer
import net.minecraft.world.item.Item
import net.minecraft.world.item.ItemStack
import net.minecraft.world.level.Level
import net.minecraft.world.phys.Vec3
import net.minecraft.sounds.SoundEvents
import net.minecraft.sounds.SoundSource
import net.minecraft.network.chat.Component
import net.minecraftforge.registries.DeferredRegister
import net.minecraftforge.registries.ForgeRegistries
import net.minecraftforge.registries.RegistryObject
import kotlin.math.roundToInt

object ModItems {
    val ITEMS: DeferredRegister<Item> = DeferredRegister.create(ForgeRegistries.ITEMS, SpiderAnimationMod.MOD_ID)

    val SPIDER: RegistryObject<Item> = ITEMS.register("spider") { SpiderItem(Item.Properties()) }
    val DISABLE_LEG: RegistryObject<Item> = ITEMS.register("disable_leg") { DisableLegItem(Item.Properties()) }
    val TOGGLE_DEBUG: RegistryObject<Item> = ITEMS.register("toggle_debug") { ToggleDebugItem(Item.Properties()) }
    val SWITCH_RENDERER: RegistryObject<Item> = ITEMS.register("switch_renderer") { SwitchRendererItem(Item.Properties()) }
    val TOGGLE_CLOAK: RegistryObject<Item> = ITEMS.register("toggle_cloak") { ToggleCloakItem(Item.Properties()) }
    val CHAIN_VIS_STEP: RegistryObject<Item> = ITEMS.register("chain_vis_step") { ChainVisStepItem(Item.Properties()) }
    val CHAIN_VIS_STRAIGHTEN: RegistryObject<Item> = ITEMS.register("chain_vis_straighten") { ChainVisStraightenItem(Item.Properties()) }
    val SWITCH_GAIT: RegistryObject<Item> = ITEMS.register("switch_gait") { SwitchGaitItem(Item.Properties()) }
    val LASER_POINTER: RegistryObject<Item> = ITEMS.register("laser_pointer") { LaserPointerItem(Item.Properties()) }
    val COME_HERE: RegistryObject<Item> = ITEMS.register("come_here") { ComeHereItem(Item.Properties()) }
}

class SpiderItem(properties: Properties) : Item(properties) {
    override fun use(level: Level, player: ServerPlayer, hand: InteractionHand): InteractionResultHolder<ItemStack> {
        if (level.isClientSide) return InteractionResultHolder.pass(player.getItemInHand(hand))
        val spider = AppState.spider
        if (spider == null) {
            val yawIncrements = 45.0f
            val yaw = player.yHeadRot
            val yawRounded = (yaw / yawIncrements).roundToInt() * yawIncrements
            val eye = player.eyePosition
            val result = level.raycastGround(eye, player.lookAngle, 100.0)
                ?: return InteractionResultHolder.pass(player.getItemInHand(hand))
            val hit = result.location
            val orientation = org.joml.Quaternionf().rotateY(Math.toRadians(yawRounded.toDouble()).toFloat())
            level.playSound(null, hit.x, hit.y, hit.z, SoundEvents.NETHERITE_BLOCK_PLACE, SoundSource.BLOCKS, 1.0f, 1.0f)
            AppState.createSpider(level as ServerLevel, hit, orientation)
            player.displayClientMessage(Component.literal("Spider created"), true)
        } else {
            level.playSound(null, player.x, player.y, player.z, SoundEvents.ITEM_FRAME_REMOVE_ITEM, SoundSource.BLOCKS, 1.0f, 0.0f)
            AppState.spider = null
            player.displayClientMessage(Component.literal("Spider removed"), true)
        }
        return InteractionResultHolder.sidedSuccess(player.getItemInHand(hand), level.isClientSide)
    }
}

class DisableLegItem(properties: Properties) : Item(properties) {
    override fun inventoryTick(stack: ItemStack, level: Level, entity: Entity, slot: Int, selected: Boolean) {
        if (level.isClientSide) return
        if (entity is ServerPlayer && (entity.mainHandItem == stack || entity.offhandItem == stack)) {
            AppState.spider?.pointDetector?.player = entity
        }
    }

    override fun use(level: Level, player: ServerPlayer, hand: InteractionHand): InteractionResultHolder<ItemStack> {
        if (level.isClientSide) return InteractionResultHolder.pass(player.getItemInHand(hand))
        val selectedLeg = AppState.spider?.pointDetector?.selectedLeg
        if (selectedLeg == null) {
            level.playSound(null, player.x, player.y, player.z, SoundEvents.DISPENSER_FAIL, SoundSource.BLOCKS, 1.0f, 2.0f)
            return InteractionResultHolder.sidedSuccess(player.getItemInHand(hand), level.isClientSide)
        }
        selectedLeg.isDisabled = !selectedLeg.isDisabled
        level.playSound(null, player.x, player.y, player.z, SoundEvents.LANTERN_PLACE, SoundSource.BLOCKS, 1.0f, 1.0f)
        return InteractionResultHolder.sidedSuccess(player.getItemInHand(hand), level.isClientSide)
    }
}

class ToggleDebugItem(properties: Properties) : Item(properties) {
    override fun use(level: Level, player: ServerPlayer, hand: InteractionHand): InteractionResultHolder<ItemStack> {
        if (level.isClientSide) return InteractionResultHolder.pass(player.getItemInHand(hand))
        AppState.showDebugVisuals = !AppState.showDebugVisuals
        SpiderConfig.SHOW_DEBUG.set(AppState.showDebugVisuals)
        SpiderConfig.save()
        AppState.chainVisualizer?.detailed = AppState.showDebugVisuals
        val pitch = if (AppState.showDebugVisuals) 2.0f else 1.5f
        level.playSound(null, player.x, player.y, player.z, SoundEvents.DISPENSER_FAIL, SoundSource.BLOCKS, 1.0f, pitch)
        return InteractionResultHolder.sidedSuccess(player.getItemInHand(hand), level.isClientSide)
    }
}

class SwitchRendererItem(properties: Properties) : Item(properties) {
    override fun use(level: Level, player: ServerPlayer, hand: InteractionHand): InteractionResultHolder<ItemStack> {
        if (level.isClientSide) return InteractionResultHolder.pass(player.getItemInHand(hand))
        val spider = AppState.spider ?: return InteractionResultHolder.pass(player.getItemInHand(hand))
        spider.renderer = if (spider.renderer is SpiderRenderer) {
            level.playSound(null, player.x, player.y, player.z, SoundEvents.AXOLOTL_ATTACK, SoundSource.BLOCKS, 1.0f, 1.0f)
            SpiderParticleRenderer(spider)
        } else {
            level.playSound(null, player.x, player.y, player.z, SoundEvents.ARMOR_EQUIP_NETHERITE, SoundSource.BLOCKS, 1.0f, 1.0f)
            SpiderRenderer(spider)
        }
        return InteractionResultHolder.sidedSuccess(player.getItemInHand(hand), level.isClientSide)
    }
}

class ToggleCloakItem(properties: Properties) : Item(properties) {
    override fun use(level: Level, player: ServerPlayer, hand: InteractionHand): InteractionResultHolder<ItemStack> {
        if (level.isClientSide) return InteractionResultHolder.pass(player.getItemInHand(hand))
        AppState.spider?.cloak?.toggleCloak()
        return InteractionResultHolder.sidedSuccess(player.getItemInHand(hand), level.isClientSide)
    }
}

class ChainVisStepItem(properties: Properties) : Item(properties) {
    override fun use(level: Level, player: ServerPlayer, hand: InteractionHand): InteractionResultHolder<ItemStack> {
        if (level.isClientSide) return InteractionResultHolder.pass(player.getItemInHand(hand))
        val chain = AppState.chainVisualizer ?: return InteractionResultHolder.pass(player.getItemInHand(hand))
        level.playSound(null, player.x, player.y, player.z, SoundEvents.DISPENSER_FAIL, SoundSource.BLOCKS, 1.0f, 2.0f)
        chain.step()
        return InteractionResultHolder.sidedSuccess(player.getItemInHand(hand), level.isClientSide)
    }
}

class ChainVisStraightenItem(properties: Properties) : Item(properties) {
    override fun use(level: Level, player: ServerPlayer, hand: InteractionHand): InteractionResultHolder<ItemStack> {
        if (level.isClientSide) return InteractionResultHolder.pass(player.getItemInHand(hand))
        val chain = AppState.chainVisualizer ?: return InteractionResultHolder.pass(player.getItemInHand(hand))
        level.playSound(null, player.x, player.y, player.z, SoundEvents.DISPENSER_FAIL, SoundSource.BLOCKS, 1.0f, 2.0f)
        chain.straighten(chain.target ?: return InteractionResultHolder.pass(player.getItemInHand(hand)))
        return InteractionResultHolder.sidedSuccess(player.getItemInHand(hand), level.isClientSide)
    }
}

class SwitchGaitItem(properties: Properties) : Item(properties) {
    override fun use(level: Level, player: ServerPlayer, hand: InteractionHand): InteractionResultHolder<ItemStack> {
        if (level.isClientSide) return InteractionResultHolder.pass(player.getItemInHand(hand))
        level.playSound(null, player.x, player.y, player.z, SoundEvents.DISPENSER_FAIL, SoundSource.BLOCKS, 1.0f, 2.0f)
        AppState.gallop = !AppState.gallop
        SpiderConfig.GALLOP.set(AppState.gallop)
        SpiderConfig.save()
        val message = if (!AppState.gallop) "Walk mode" else "Gallop mode"
        player.displayClientMessage(Component.literal(message), true)
        return InteractionResultHolder.sidedSuccess(player.getItemInHand(hand), level.isClientSide)
    }
}

class LaserPointerItem(properties: Properties) : Item(properties) {
    override fun inventoryTick(stack: ItemStack, level: Level, entity: Entity, slot: Int, selected: Boolean) {
        if (level.isClientSide) return
        if (entity is ServerPlayer && (entity.mainHandItem == stack || entity.offhandItem == stack)) {
            val eye = entity.eyePosition
            val directionVec = entity.lookAngle
            val result = level.raycastGround(eye, directionVec, 100.0)
            if (result == null) {
                val direction = org.bukkit.util.Vector(directionVec.x, 0.0, directionVec.z).normalize()
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
                AppState.spider?.let { it.behaviour = TargetBehaviour(it, targetVal.toVector(), it.lerpedGait.bodyHeight) }
            }
        }
    }
}

class ComeHereItem(properties: Properties) : Item(properties) {
    override fun inventoryTick(stack: ItemStack, level: Level, entity: Entity, slot: Int, selected: Boolean) {
        if (level.isClientSide) return
        if (entity is ServerPlayer && (entity.mainHandItem == stack || entity.offhandItem == stack)) {
            AppState.spider?.let {
                val height = if (it.gait.straightenLegs) it.lerpedGait.bodyHeight * 2.0 else it.lerpedGait.bodyHeight * 5.0
                val eye = entity.eyePosition
                it.behaviour = TargetBehaviour(it, org.bukkit.util.Vector(eye.x, eye.y, eye.z), height)
            }
        }
    }
}
