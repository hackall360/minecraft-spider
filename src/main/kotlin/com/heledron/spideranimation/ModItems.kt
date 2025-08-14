package com.heledron.spideranimation

import net.minecraft.network.chat.Component
import net.minecraft.world.item.Item
import net.minecraft.world.item.ItemStack
import net.minecraft.world.item.Items

/**
 * Enumeration of control tools using existing vanilla items.
 * Each item stack carries a small NBT tag to identify its role.
 */
enum class ControlItem(val item: Item, val tag: String, val displayName: String) {
    SPIDER(Items.STICK, "spider", "Spawn/Remove Spider"),
    DISABLE_LEG(Items.SHEARS, "disable_leg", "Disable Selected Leg"),
    TOGGLE_DEBUG(Items.REDSTONE, "toggle_debug", "Toggle Debug Visuals"),
    SWITCH_RENDERER(Items.BLAZE_ROD, "switch_renderer", "Switch Renderer"),
    TOGGLE_CLOAK(Items.ENDER_PEARL, "toggle_cloak", "Toggle Cloak"),
    CHAIN_VIS_STEP(Items.FEATHER, "chain_vis_step", "Chain Step"),
    CHAIN_VIS_STRAIGHTEN(Items.BONE, "chain_vis_straighten", "Chain Straighten"),
    SWITCH_GAIT(Items.SADDLE, "switch_gait", "Switch Gait"),
    LASER_POINTER(Items.CARROT_ON_A_STICK, "laser_pointer", "Laser Pointer"),
    COME_HERE(Items.LEAD, "come_here", "Come Here");

    companion object {
        const val TAG_KEY = "SpiderTool"
    }

    /** Create an [ItemStack] representing this control tool. */
    fun createStack(): ItemStack =
        ItemStack(item).apply {
            orCreateTag.putString(TAG_KEY, tag)
            setHoverName(Component.literal(displayName))
        }
}

/** Retrieve the [ControlItem] represented by this [ItemStack], if any. */
fun ItemStack.getControlItem(): ControlItem? {
    val tag = tag?.getString(ControlItem.TAG_KEY) ?: return null
    return ControlItem.entries.firstOrNull { it.tag == tag }
}
