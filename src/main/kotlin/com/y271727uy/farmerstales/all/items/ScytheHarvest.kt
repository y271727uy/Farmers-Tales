package com.y271727uy.farmerstales.all.items

import com.y271727uy.farmerstales.gameplay.quality_crop.event.harvest.BerryHarvestEvents
import com.y271727uy.farmerstales.gameplay.quality_crop.util.BerryFruitHarvestWhitelist
import com.y271727uy.farmerstales.gameplay.quality_crop.util.MDWhiteList
import net.minecraft.world.level.block.BonemealableBlock
import net.minecraft.world.level.block.BushBlock
import net.minecraft.world.level.block.CaveVines
import net.minecraft.world.level.block.CropBlock
import net.minecraft.world.level.block.state.BlockState
import net.minecraft.world.level.block.state.properties.BooleanProperty
import net.minecraft.world.level.block.state.properties.IntegerProperty
import net.minecraftforge.registries.ForgeRegistries

/** Maturity rules for plants whose right-click harvest does not follow CropBlock. */
internal object ScytheHarvest {
    const val MAIZE = "bountifulfares:maize_crop"

    private val stalkTops = mapOf(
        "farmersdelight:rice" to "farmersdelight:rice_panicles",
        "list:upland_rice" to "list:upland_rice_panicles",
        "ubesdelight:lemongrass_stalk_crop" to "ubesdelight:lemongrass_leaf_crop",
        "vietnam_delight:sticky_rice_crop" to "vietnam_delight:sticky_rice_panicle_crop",
    )

    private val nativeHarvest = setOf(
        "vintagedelight:gearo_berry_bush",
        "collectorsreap:lime_bush",
        "collectorsreap:pomegranate_bush",
        "delighto_flight:cloud_berry_bush",
        "extravagantdelight:hops_crop",
        "extravagantdelight:starfruit_crop",
        "farm_and_charm:tomato_crop",
        "farm_and_charm:tomato_crop_body",
        "farmersrespite:coffee_stem",
        "farmersrespite:coffee_stem_middle",
        "manors_bounty:cucumber_crop",
        "manors_bounty:grape_crop",
        "manors_bounty:passion_fruit_crop",
        "manors_bounty:coffee_berry_crop",
        "manors_bounty:jalapeno_crop",
        "manors_bounty:rice_crop",
        "manors_bounty:tomato_crop",
        "manors_bounty:baby_corn_bush",
        "manors_bounty:blackcurrant_bush",
        "manors_bounty:blueberry_bush",
        "manors_bounty:cherry_tomatoes_bush",
        "manors_bounty:cranberry_bush",
        "manors_bounty:end_pepper_bush",
        "manors_bounty:green_bell_pepper_bush",
        "manors_bounty:pimento_bush",
        "manors_bounty:raspberry_bush",
        "manors_bounty:strawberry_bush",
        "manors_bounty:wild_jalapeno_bush",
        "fruitsdelight:blueberry_bush",
        "fruitsdelight:cranberry_bush",
        "fruitsdelight:durian_leaves",
        "vietnam_delight:chilis_crop",
        "vietnam_delight:pandan_crop",
        "culturaldelights:fruiting_avocado_leaves",
        "immortalers_delight:oxygrape",
        "muffins_thaidelight:lime_bush",
        "foragersinsight:bountiful_dark_oak_leaves",
        "foragersinsight:bountiful_oak_leaves",
        "foragersinsight:hanging_lilac_leaves",
        "yeastnfeast:elderberry_bush",
        "yeastnfeast:rose_hips_bush",
        "yeastnfeast:flowering_hawthorn_tree_leaves",
        "yeastnfeast:flowering_lemon_tree_leaves",
    )

    // These crops expose their produce through getDrops and use "blockstate" for growth.
    private val vietnamReplantAges = mapOf(
        "vietnam_delight:garlic_chives_crop" to 5,
        "vietnam_delight:garlic_crop" to 6,
        "vietnam_delight:gingers_crop" to 5,
        "vietnam_delight:lemongrass_crop" to 6,
        "vietnam_delight:lotot_crop" to 5,
        "vietnam_delight:mung_beans_crop" to 4,
        "vietnam_delight:onion_crop" to 6,
        "vietnam_delight:scallion_crop" to 5,
        "vietnam_delight:shallot_crop" to 6,
        "vietnam_delight:sticky_rice_panicle_crop" to 3,
        "vietnam_delight:turmeric_crop" to 5,
    )

    fun isReady(state: BlockState): Boolean {
        val id = id(state)
        val crop = state.block as? CropBlock
        return when {
            id in stalkTops -> false
            id == "farmersrespite:coffee_stem_double" -> state.age("age") == 2 || state.age("age1") == 2
            id == "vietnam_delight:white_peppercorn_crop" -> state.age("blockstate").let { it == 9 || it == 13 }
            id == "vietnam_delight:kumquat_bush" -> state.age("blockstate") in 2..3
            id == "vinery:apple_leaves" -> state.flag("has_apples") && state.age("age") == 3
            id.startsWith("vinery:jungle_grape_bush_") -> state.age("age") in 2..3
            id == "extravagantdelight:muscadine_crop" -> state.age("age") == 5
            id == "extravagantdelight:flourishing_mango_leaves" -> state.age("stage") == 2
            id in alwaysRipe -> true
            id == "bountifulfares:tea_shrub" -> state.flag("berries")
            id in mushroomColonies -> state.age("age") == 3
            id == "farmersrespite:tea_bush" -> state.age("age") == 3
            id == "youkaishomecoming:cucumber" && !state.flag("ropelogged") -> false
            id in vietnamReplantAges -> state.age("blockstate") == vietnamReplantAges[id]
            crop != null -> crop.isMaxAge(state)
            state.block is CaveVines -> state.flag("berries")
            state.block is BushBlock && state.block is BonemealableBlock -> BerryHarvestEvents.isRipe(state)
            id in nativeHarvest || isNativeFamily(id) || hasLegacyBushDrops(state) -> BerryHarvestEvents.isRipe(state)
            else -> false
        }
    }

    private val alwaysRipe = setOf(
        "extravagantdelight:fruit_grapefruit_vine",
        "extravagantdelight:fruit_quince_leaves",
        "immortalers_delight:himekaido_fruited_leaves",
        "immortalers_delight:travastruggler_leaves_travarice",
    )

    private val mushroomColonies = setOf(
        "farmersdelight:brown_mushroom_colony",
        "farmersdelight:red_mushroom_colony",
        "mynethersdelight:crimson_fungus_colony",
        "mynethersdelight:warped_fungus_colony",
    )

    private fun isNativeFamily(id: String): Boolean =
        id.startsWith("vinery:") && (id.endsWith("_grape_bush") || "_grape_bush_" in id) ||
            id == "nethervinery:crimson_grape_bush" || id == "nethervinery:warped_grape_bush" ||
            id.startsWith("youkaishomecoming:") && "_grape_" in id &&
            (id.endsWith("_fruits") || id.endsWith("_vines") || id.endsWith("_branches"))

    fun replantAge(state: BlockState): IntegerProperty? {
        if (id(state) in vietnamReplantAges) return state.intProperty("blockstate")
        val crop = state.block as? CropBlock ?: return null
        // Compare CropBlock's own generated states to find the growth property even with a custom name.
        val initial = crop.defaultBlockState()
        val mature = crop.getStateForAge(crop.maxAge)
        return state.properties.filterIsInstance<IntegerProperty>().firstOrNull { initial.getValue(it) != mature.getValue(it) }
    }

    fun hasLegacyBushDrops(state: BlockState): Boolean =
        BerryFruitHarvestWhitelist.matches(state.block) || MDWhiteList.matches(state.block) &&
            id(state).startsWith("manors_bounty:") && id(state).endsWith("_bush")

    fun harvestableTopId(base: BlockState): String? = stalkTops[id(base)]

    fun useShearsForPick(state: BlockState): Boolean = id(state) in mushroomColonies

    fun regrowsFromStalk(state: BlockState): Boolean = id(state) in stalkTops.values

    private fun BlockState.age(name: String): Int =
        intProperty(name)?.let(::getValue) ?: -1

    private fun BlockState.intProperty(name: String): IntegerProperty? =
        properties.filterIsInstance<IntegerProperty>().firstOrNull { it.name == name }

    private fun BlockState.flag(name: String): Boolean =
        properties.filterIsInstance<BooleanProperty>().firstOrNull { it.name == name }?.let(::getValue) == true

    private fun id(state: BlockState): String = ForgeRegistries.BLOCKS.getKey(state.block).toString()
}
