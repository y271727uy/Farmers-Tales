package com.y271727uy.farmerstales.all.items

import com.tterrag.registrate.util.entry.ItemEntry
import com.y271727uy.farmerstales.registrate.ModRegistrate
import net.minecraft.world.item.Tier
import net.minecraft.world.item.Tiers

/**
 * Scythes use vanilla shovel tiers so their mining level, repair ingredient,
 * mining speed, and durability stay aligned with the matching vanilla tool.
 */
object ToolItems {
    @JvmField
    val WOODEN_SCYTHE: ItemEntry<ScytheItem> = scythe("wooden_scythe", "Wooden Scythe", Tiers.WOOD, 1)

    @JvmField
    val GOLDEN_SCYTHE: ItemEntry<ScytheItem> = scythe("golden_scythe", "Golden Scythe", Tiers.GOLD, 2)

    @JvmField
    val COPPER_SCYTHE: ItemEntry<ScytheItem> = scythe("copper_scythe", "Copper Scythe", Tiers.IRON, 2)

    @JvmField
    val IRON_SCYTHE: ItemEntry<ScytheItem> = scythe("iron_scythe", "Iron Scythe", Tiers.IRON, 3)

    @JvmField
    val DIAMOND_SCYTHE: ItemEntry<ScytheItem> = scythe("diamond_scythe", "Diamond Scythe", Tiers.DIAMOND, 4)

    @JvmField
    val NETHERITE_SCYTHE: ItemEntry<ScytheItem> = scythe("netherite_scythe", "Netherite Scythe", Tiers.NETHERITE, 5)

    private fun scythe(id: String, displayName: String, tier: Tier, harvestWidth: Int): ItemEntry<ScytheItem> =
        ModRegistrate.REGISTRATE
            .item(id) { properties -> ScytheItem(tier, harvestWidth, properties) }
            .model { context, provider -> provider.generated(context, provider.modLoc("item/tool/${context.name}")) }
            .lang(displayName)
            .register()

    fun init() = Unit
}
