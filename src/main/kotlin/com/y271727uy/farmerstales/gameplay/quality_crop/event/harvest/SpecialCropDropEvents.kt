package com.y271727uy.farmerstales.gameplay.quality_crop.event.harvest

import com.y271727uy.farmerstales.FTMod
import com.y271727uy.farmerstales.gameplay.advancements.FarmersTalesAdvancements
import com.y271727uy.farmerstales.gameplay.quality_crop.QualityHarvestRules
import com.y271727uy.farmerstales.gameplay.quality_crop.harvestPickupDelay
import com.y271727uy.farmerstales.gameplay.quality_crop.QualityNbt
import com.y271727uy.farmerstales.gameplay.quality_crop.QualityRoller
import net.minecraft.resources.ResourceLocation
import net.minecraft.server.level.ServerLevel
import net.minecraft.world.entity.item.ItemEntity
import net.minecraftforge.event.entity.EntityJoinLevelEvent
import net.minecraftforge.eventbus.api.SubscribeEvent
import net.minecraftforge.fml.common.Mod
import net.minecraftforge.registries.ForgeRegistries

/**
 * 这些模组的收获最后都会变成掉落物：千古乐事、辣条工艺自己弹出，
 * Vital Herbs 走破坏战利品，无尽乐事的作物被破坏或右键摘果后也会弹出。
 * 泰式乐事的木瓜长在树干上，右键或破坏后弹出熟木瓜和生木瓜。
 * 这里只认收获物。种子、野生植株、成品和原版副产物不打品质。
 */
@Mod.EventBusSubscriber(modid = FTMod.MODID, bus = Mod.EventBusSubscriber.Bus.FORGE)
@Suppress("unused")
object SpecialCropDropEvents {

	@JvmStatic
	@SubscribeEvent
	fun onItemJoinLevel(event: EntityJoinLevelEvent) {
		val level = event.level
		if (level.isClientSide || level !is ServerLevel) {
			return
		}

		val itemEntity = event.entity as? ItemEntity ?: return
		if (itemEntity.harvestPickupDelay() != FRESH_PICKUP_DELAY) {
			return
		}

		val stack = itemEntity.item
		val itemId = ForgeRegistries.ITEMS.getKey(stack.item)
		if (stack.isEmpty || itemId == null || itemId !in HARVEST_ITEMS || QualityNbt.hasValidQuality(stack)) {
			return
		}

		val harvester = level.getNearestPlayer(itemEntity, HARVESTER_RANGE)
		if (!QualityHarvestRules.canReceiveQuality(harvester)) {
			return
		}

		QualityRoller.roll(level.random)?.let { quality ->
			QualityNbt.write(stack, quality, harvester)
			FarmersTalesAdvancements.awardIfRaining(harvester, level)
		}
	}

	private val HARVEST_ITEMS: Set<ResourceLocation> = listOf(
		// 千古乐事
		"immortalers_delight:alfalfa",
		"immortalers_delight:kwat_wheat",
		"immortalers_delight:evolutcorn",
		"immortalers_delight:gelpitaya",
		"immortalers_delight:contains_tea_leisamboo",
		"immortalers_delight:obsidian_walnut",
		"immortalers_delight:oxygrape",
		"immortalers_delight:pearlip",
		"immortalers_delight:sextlotus_leaf",
		"immortalers_delight:sextlotus_root",
		"immortalers_delight:warped_laurel",
		"immortalers_delight:himekaido",
		"immortalers_delight:travarice",
		// 辣条工艺。红椒、甜椒、黄椒和墨西哥椒会被换成任务书里的另一种辣椒。
		"ltc2:pepper_white",
		"ltc2:pepper_green",
		"ltc2:pepper_frost",
		"ltc2:pepper_sahara",
		"ltc2:pepper_bloody",
		"ltc2:pepper_complained",
		"ltc2:pepper_umbrvol",
		"ltc2:pepper_endbond",
		"ltc2:pepper_black",
		"ltc2:pepper_wither",
		// Vital Herbs。干荧光叶没有收获物。
		"vital_herbs:bleeding_heart",
		"vital_herbs:blue_spar",
		"vital_herbs:burst_bud",
		"vital_herbs:crimson_lily",
		"vital_herbs:fizz_fruit",
		"vital_herbs:frost_mint",
		"vital_herbs:glow_leaf",
		"vital_herbs:grim_vine",
		"vital_herbs:iron_root",
		"vital_herbs:mourning_bloom",
		"vital_herbs:needle_heart",
		"vital_herbs:razor_leaf",
		"vital_herbs:snap_pepper",
		"vital_herbs:sooth_blossom",
		"vital_herbs:tox_kiss",
		// 无尽乐事。番茄藤右键摘果，其余是作物破坏。
		"avaritia_delight:blaze_tomato",
		"avaritia_delight:crystal_cabbage",
		"avaritia_delight:diamond_lattice_potato",
		"avaritia_delight:neutronium_wheat",
		// 泰式乐事。木瓜方块 age 1 是生木瓜，age 2 是熟木瓜。
		"muffins_thaidelight:papaya",
		"muffins_thaidelight:raw_papaya",
	).mapNotNull(ResourceLocation::tryParse).toSet()

	private const val FRESH_PICKUP_DELAY = 10
	private const val HARVESTER_RANGE = 8.0
}
