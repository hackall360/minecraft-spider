package com.heledron.spideranimation

import com.heledron.spideranimation.utilities.*
import com.heledron.spideranimation.spider.misc.*
import com.heledron.spideranimation.spider.rendering.*
import net.minecraft.world.InteractionHand
import net.minecraft.world.InteractionResultHolder
import net.minecraft.world.entity.Entity
import net.minecraft.world.entity.player.Player
import net.minecraft.world.item.Item
import net.minecraft.world.item.ItemStack
import net.minecraft.world.level.Level
import net.minecraftforge.registries.DeferredRegister
import net.minecraftforge.registries.ForgeRegistries
import net.minecraftforge.registries.RegistryObject
import org.bukkit.Sound
import org.bukkit.entity.Player as BukkitPlayer
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

private fun toBukkit(player: Player): BukkitPlayer = player.bukkitEntity as BukkitPlayer

class SpiderItem(properties: Properties) : Item(properties) {
    override fun use(level: Level, player: Player, hand: InteractionHand): InteractionResultHolder<ItemStack> {
        if (level.isClientSide) return InteractionResultHolder.pass(player.getItemInHand(hand))
        val bukkitPlayer = toBukkit(player)
        val spider = AppState.spider
        if (spider == null) {
            val yawIncrements = 45.0f
            val yaw = bukkitPlayer.location.yaw
            val yawRounded = (yaw / yawIncrements).roundToInt() * yawIncrements
            val playerLocation = bukkitPlayer.eyeLocation
            val hit = raycastGround(playerLocation, playerLocation.direction, 100.0)?.hitPosition
                ?.toLocation(playerLocation.world!!) ?: return InteractionResultHolder.pass(player.getItemInHand(hand))
            hit.yaw = yawRounded
            playSound(hit, Sound.BLOCK_NETHERITE_BLOCK_PLACE, 1.0f, 1.0f)
            AppState.createSpider(hit)
            sendActionBar(bukkitPlayer, "Spider created")
        } else {
            playSound(bukkitPlayer.location, Sound.ENTITY_ITEM_FRAME_REMOVE_ITEM, 1.0f, 0.0f)
            AppState.spider = null
            sendActionBar(bukkitPlayer, "Spider removed")
        }
        return InteractionResultHolder.sidedSuccess(player.getItemInHand(hand), level.isClientSide)
    }
}

class DisableLegItem(properties: Properties) : Item(properties) {
    override fun inventoryTick(stack: ItemStack, level: Level, entity: Entity, slot: Int, selected: Boolean) {
        if (level.isClientSide) return
        if (entity is Player && (entity.mainHandItem == stack || entity.offhandItem == stack)) {
            AppState.spider?.pointDetector?.player = toBukkit(entity)
        }
    }

    override fun use(level: Level, player: Player, hand: InteractionHand): InteractionResultHolder<ItemStack> {
        if (level.isClientSide) return InteractionResultHolder.pass(player.getItemInHand(hand))
        val bukkitPlayer = toBukkit(player)
        val selectedLeg = AppState.spider?.pointDetector?.selectedLeg
        if (selectedLeg == null) {
            playSound(bukkitPlayer.location, Sound.BLOCK_DISPENSER_FAIL, 1.0f, 2.0f)
            return InteractionResultHolder.sidedSuccess(player.getItemInHand(hand), level.isClientSide)
        }
        selectedLeg.isDisabled = !selectedLeg.isDisabled
        playSound(bukkitPlayer.location, Sound.BLOCK_LANTERN_PLACE, 1.0f, 1.0f)
        return InteractionResultHolder.sidedSuccess(player.getItemInHand(hand), level.isClientSide)
    }
}

class ToggleDebugItem(properties: Properties) : Item(properties) {
    override fun use(level: Level, player: Player, hand: InteractionHand): InteractionResultHolder<ItemStack> {
        if (level.isClientSide) return InteractionResultHolder.pass(player.getItemInHand(hand))
        val bukkitPlayer = toBukkit(player)
        AppState.showDebugVisuals = !AppState.showDebugVisuals
        SpiderConfig.SHOW_DEBUG.set(AppState.showDebugVisuals)
        SpiderConfig.save()
        AppState.chainVisualizer?.detailed = AppState.showDebugVisuals
        val pitch = if (AppState.showDebugVisuals) 2.0f else 1.5f
        playSound(bukkitPlayer.location, Sound.BLOCK_DISPENSER_FAIL, 1.0f, pitch)
        return InteractionResultHolder.sidedSuccess(player.getItemInHand(hand), level.isClientSide)
    }
}

class SwitchRendererItem(properties: Properties) : Item(properties) {
    override fun use(level: Level, player: Player, hand: InteractionHand): InteractionResultHolder<ItemStack> {
        if (level.isClientSide) return InteractionResultHolder.pass(player.getItemInHand(hand))
        val bukkitPlayer = toBukkit(player)
        val spider = AppState.spider ?: return InteractionResultHolder.pass(player.getItemInHand(hand))
        spider.renderer = if (spider.renderer is SpiderRenderer) {
            playSound(bukkitPlayer.location, Sound.ENTITY_AXOLOTL_ATTACK, 1.0f, 1.0f)
            SpiderParticleRenderer(spider)
        } else {
            playSound(bukkitPlayer.location, Sound.ITEM_ARMOR_EQUIP_NETHERITE, 1.0f, 1.0f)
            SpiderRenderer(spider)
        }
        return InteractionResultHolder.sidedSuccess(player.getItemInHand(hand), level.isClientSide)
    }
}

class ToggleCloakItem(properties: Properties) : Item(properties) {
    override fun use(level: Level, player: Player, hand: InteractionHand): InteractionResultHolder<ItemStack> {
        if (!level.isClientSide) {
            AppState.spider?.cloak?.toggleCloak()
        }
        return InteractionResultHolder.sidedSuccess(player.getItemInHand(hand), level.isClientSide)
    }
}

class ChainVisStepItem(properties: Properties) : Item(properties) {
    override fun use(level: Level, player: Player, hand: InteractionHand): InteractionResultHolder<ItemStack> {
        if (level.isClientSide) return InteractionResultHolder.pass(player.getItemInHand(hand))
        val bukkitPlayer = toBukkit(player)
        val chain = AppState.chainVisualizer ?: return InteractionResultHolder.pass(player.getItemInHand(hand))
        playSound(bukkitPlayer.location, Sound.BLOCK_DISPENSER_FAIL, 1.0f, 2.0f)
        chain.step()
        return InteractionResultHolder.sidedSuccess(player.getItemInHand(hand), level.isClientSide)
    }
}

class ChainVisStraightenItem(properties: Properties) : Item(properties) {
    override fun use(level: Level, player: Player, hand: InteractionHand): InteractionResultHolder<ItemStack> {
        if (level.isClientSide) return InteractionResultHolder.pass(player.getItemInHand(hand))
        val bukkitPlayer = toBukkit(player)
        val chain = AppState.chainVisualizer ?: return InteractionResultHolder.pass(player.getItemInHand(hand))
        playSound(bukkitPlayer.location, Sound.BLOCK_DISPENSER_FAIL, 1.0f, 2.0f)
        chain.straighten(chain.target?.toVector() ?: return InteractionResultHolder.pass(player.getItemInHand(hand)))
        return InteractionResultHolder.sidedSuccess(player.getItemInHand(hand), level.isClientSide)
    }
}

class SwitchGaitItem(properties: Properties) : Item(properties) {
    override fun use(level: Level, player: Player, hand: InteractionHand): InteractionResultHolder<ItemStack> {
        if (level.isClientSide) return InteractionResultHolder.pass(player.getItemInHand(hand))
        val bukkitPlayer = toBukkit(player)
        playSound(bukkitPlayer.location, Sound.BLOCK_DISPENSER_FAIL, 1.0f, 2.0f)
        AppState.gallop = !AppState.gallop
        SpiderConfig.GALLOP.set(AppState.gallop)
        SpiderConfig.save()
        sendActionBar(bukkitPlayer, if (!AppState.gallop) "Walk mode" else "Gallop mode")
        return InteractionResultHolder.sidedSuccess(player.getItemInHand(hand), level.isClientSide)
    }
}

class LaserPointerItem(properties: Properties) : Item(properties) {
    override fun inventoryTick(stack: ItemStack, level: Level, entity: Entity, slot: Int, selected: Boolean) {
        if (level.isClientSide) return
        if (entity is Player && (entity.mainHandItem == stack || entity.offhandItem == stack)) {
            val bukkitPlayer = toBukkit(entity)
            val location = bukkitPlayer.eyeLocation
            val result = raycastGround(location, location.direction, 100.0)
            if (result == null) {
                val direction = bukkitPlayer.eyeLocation.direction.setY(0.0).normalize()
                AppState.spider?.let { it.behaviour = DirectionBehaviour(it, direction, direction) }
                AppState.chainVisualizer?.let {
                    it.target = null
                    it.resetIterator()
                }
            } else {
                val targetVal = result.hitPosition.toLocation(bukkitPlayer.world)
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
        if (entity is Player && (entity.mainHandItem == stack || entity.offhandItem == stack)) {
            val bukkitPlayer = toBukkit(entity)
            AppState.spider?.let {
                val height = if (it.gait.straightenLegs) it.lerpedGait.bodyHeight * 2.0 else it.lerpedGait.bodyHeight * 5.0
                it.behaviour = TargetBehaviour(it, bukkitPlayer.eyeLocation.toVector(), height)
            }
        }
    }
}
