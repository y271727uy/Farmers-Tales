@file:Suppress("unused")

package com.y271727uy.farmerstales.gameplay.quality_crop.util

import net.minecraft.core.registries.Registries
import net.minecraft.resources.ResourceLocation
import net.minecraft.tags.TagKey
import net.minecraft.world.item.Item
import net.minecraft.world.item.ItemStack
import net.minecraftforge.registries.ForgeRegistries

/**
 * 质量白名单配置与匹配入口。
 *
 * 典型用法：
 * ```kotlin
 * val config = standardCropWhitelist {
 *     WhiteListTag("forge:crops")
 *     WhiteListItem("minecraft:wheat")
 * }
 * StandardCropWhitelist.install(config)
 * ```
 */
object StandardCropWhitelist {
	@Volatile
	private var active: Config = createDefaultStandardCropConfig()

	fun standardCropWhitelist(block: Builder.() -> Unit): Config = Builder().apply(block).build()

	fun install(config: Config) {
		active = config
	}

	fun clear() {
		active = Config.EMPTY
	}

	fun current(): Config = active

	fun matches(stack: ItemStack): Boolean = active.matches(stack)

	class Builder {
		private val itemIds = linkedSetOf<ResourceLocation>()
		private val tagIds = linkedSetOf<ResourceLocation>()

		fun WhiteListItem(id: String) {
			parseResourceLocation(id)?.let(itemIds::add)
		}

		fun WhiteListTag(id: String) {
			parseResourceLocation(id)?.let(tagIds::add)
		}

		fun build(): Config = Config(itemIds.toSet(), tagIds.toSet())

		private fun parseResourceLocation(raw: String): ResourceLocation? {
			val normalized = raw.trim().removePrefix("#")
			return ResourceLocation.tryParse(normalized)
		}
	}

	class Config internal constructor(
		private val itemIds: Set<ResourceLocation>,
		tagIds: Set<ResourceLocation>,
	) {
		private val tagKeys: Set<TagKey<Item>> = tagIds.mapTo(linkedSetOf()) { tagId -> TagKey.create(Registries.ITEM, tagId) }

		val isEmpty: Boolean
			get() = itemIds.isEmpty() && tagKeys.isEmpty()

		fun matches(stack: ItemStack): Boolean {
			if (stack.isEmpty) {
				return false
			}

			val item = stack.item
			val itemId = ForgeRegistries.ITEMS.getKey(item) ?: return false
			if (itemId in itemIds) {
				return true
			}

			if (tagKeys.isEmpty()) {
				return false
			}

			return tagKeys.any { tagKey -> stack.`is`(tagKey) }
		}

		fun matches(item: Item): Boolean {
			val itemId = ForgeRegistries.ITEMS.getKey(item) ?: return false
			return itemId in itemIds
		}

		companion object {
			val EMPTY: Config = Config(emptySet(), emptySet())
		}
	}

	/**
	 * 机械动力升级测试整合包的作物代表项。
	 *
	 * 来源是 FTB 任务书的野生作物章节和 SDM 商店「农作物」页。
	 * 同一作物被 Almost Unified（[all_tag.js] / [all_tag_two.js]）收成一个标签时，只保留优先级最高的那个。
	 * 庄园馀话、林业、千古乐事、辣条工艺、Vital Herbs 和无尽乐事各有自己的掉落物入口。oreberriesreplanted 不做。
	 */
	private fun createDefaultStandardCropConfig(): Config {
		return standardCropWhitelist {
			// 原版
			WhiteListItem("minecraft:wheat")
			WhiteListItem("minecraft:carrot")
			WhiteListItem("minecraft:potato")
			WhiteListItem("minecraft:beetroot")
			WhiteListItem("minecraft:pumpkin")
			WhiteListItem("minecraft:melon")
			WhiteListItem("minecraft:sweet_berries")
			WhiteListItem("minecraft:glow_berries")
			WhiteListItem("minecraft:cocoa_beans")
			WhiteListItem("minecraft:sugar_cane")
			WhiteListItem("minecraft:kelp")
			WhiteListItem("minecraft:nether_wart")
			WhiteListItem("minecraft:chorus_fruit")

			// 粮食
			WhiteListItem("farmersdelight:rice")
			WhiteListItem("vintagedelight:oat")
			WhiteListItem("yeastnfeast:barley")
			WhiteListItem("yeastnfeast:rye")
			WhiteListItem("braziliandelight:corn")
			WhiteListItem("braziliandelight:cassava_root")
			WhiteListItem("braziliandelight:bean_pod")
			WhiteListItem("vegandelight:soybean")
			WhiteListItem("neapolitan:adzuki_beans")
			WhiteListItem("vietnam_delight:sticky_rice")
			WhiteListItem("vietnam_delight:mung_beans")
			WhiteListItem("veggiesdelight:sweet_potato")
			WhiteListItem("ubesdelight:ube")
			WhiteListItem("ramadandelight:chickpea")
			WhiteListItem("trailandtales_delight:pitcher_taro")
			WhiteListItem("cosmopolitan:tuber")

			// 蔬菜
			WhiteListItem("farmersdelight:cabbage")
			WhiteListItem("farmersdelight:onion")
			WhiteListItem("farmersdelight:tomato")
			WhiteListItem("vintagedelight:cucumber")
			WhiteListItem("braziliandelight:garlic_bulb")
			WhiteListItem("braziliandelight:collard_greens")
			WhiteListItem("culturaldelights:eggplant")
			WhiteListItem("culturaldelights:white_eggplant")
			WhiteListItem("veggiesdelight:broccoli")
			WhiteListItem("veggiesdelight:cauliflower")
			WhiteListItem("veggiesdelight:turnip")
			WhiteListItem("veggiesdelight:zucchini")
			WhiteListItem("veggiesdelight:dandelion_leaf")
			WhiteListItem("farm_and_charm:lettuce")
			WhiteListItem("bountifulfares:leek")
			WhiteListItem("dumplings_delight:chinese_cabbage")
			WhiteListItem("dumplings_delight:fennel")
			WhiteListItem("dumplings_delight:garlic_chive")
			WhiteListItem("dumplings_delight:greenonion")
			WhiteListItem("argentinas_delight:espinacaitem")
			WhiteListItem("extravagantdelight:rhubarb")
			WhiteListItem("delighto_flight:lotus_root")
			WhiteListItem("delighto_flight:lotus_seeds")
			WhiteListItem("yeastnfeast:ginger")
			WhiteListItem("ramadandelight:parsley")
			WhiteListItem("muffins_thaidelight:pepper")
			WhiteListItem("vintagedelight:ghost_pepper")
			WhiteListItem("peruviansdelight:aji_amarillo")
			WhiteListItem("rusticdelight:bell_pepper_red")
			WhiteListItem("rusticdelight:bell_pepper_green")
			WhiteListItem("rusticdelight:bell_pepper_yellow")
			WhiteListItem("rusticdelight:bell_pepper_black")
			WhiteListItem("rusticdelight:bell_pepper_blue")
			WhiteListItem("rusticdelight:bell_pepper_pink")
			WhiteListItem("rusticdelight:bell_pepper_purple")
			WhiteListItem("rusticdelight:bell_pepper_white")
			WhiteListItem("spanishdelight:green_bean")
			WhiteListItem("miners_delight:cave_carrot")
			WhiteListItem("cosmopolitan:fiddlehead")
			WhiteListItem("cosmopolitan:wheatgrass")
			WhiteListItem("foragersinsight:dandelion_root")
			WhiteListItem("untamedwilds:food_aardvark_cucumber")
			WhiteListItem("vietnam_delight:basil_leaf")
			WhiteListItem("vietnam_delight:bean_sprouts")
			WhiteListItem("vietnam_delight:bird_eye_chilis")
			WhiteListItem("vietnam_delight:shallot")
			WhiteListItem("vietnam_delight:lotot_leaf")
			WhiteListItem("vietnam_delight:pandan_leaf")
			WhiteListItem("vietnam_delight:turmeric")
			WhiteListItem("vietnam_delight:young_lotus_stem")
			WhiteListItem("vietnam_delight:lotus_embryo")
			WhiteListItem("netherdepthsupgrade:crimson_kelp")
			WhiteListItem("netherdepthsupgrade:warped_kelp")
			WhiteListItem("ends_delight:chorus_succulent")
			WhiteListItem("dungeonsdelight:rotbulb")
			WhiteListItem("mynethersdelight:bullet_pepper")
			WhiteListItem("regions_unexplored:meadow_sage")
			WhiteListItem("youkaishomecoming:mandrake_root")

			// 水果与浆果。统一后的代表项才进表，重复变种不重复写。
			WhiteListItem("fruitsdelight:bayberry")
			WhiteListItem("fruitsdelight:blueberry")
			WhiteListItem("fruitsdelight:cranberry")
			WhiteListItem("fruitsdelight:durian")
			WhiteListItem("fruitsdelight:fig")
			WhiteListItem("fruitsdelight:hamimelon")
			WhiteListItem("fruitsdelight:hawberry")
			WhiteListItem("fruitsdelight:kiwi")
			WhiteListItem("fruitsdelight:lemon")
			WhiteListItem("fruitsdelight:lychee")
			WhiteListItem("fruitsdelight:mango")
			WhiteListItem("fruitsdelight:mangosteen")
			WhiteListItem("fruitsdelight:orange")
			WhiteListItem("fruitsdelight:peach")
			WhiteListItem("fruitsdelight:pear")
			WhiteListItem("fruitsdelight:persimmon")
			WhiteListItem("fruitsdelight:pineapple")
			WhiteListItem("collectorsreap:lime")
			WhiteListItem("collectorsreap:lucuma")
			WhiteListItem("collectorsreap:pink_dragon_fruit")
			WhiteListItem("collectorsreap:pomegranate")
			WhiteListItem("collectorsreap:stygian_pomegranate")
			WhiteListItem("bountifulfares:elderberries")
			WhiteListItem("bountifulfares:hoary_apple")
			WhiteListItem("bountifulfares:lapisberries")
			WhiteListItem("bountifulfares:orange")
			WhiteListItem("bountifulfares:passion_fruit")
			WhiteListItem("bountifulfares:spongekin")
			WhiteListItem("bountifulfares:tea_berries")
			WhiteListItem("bountifulfares:plum")
			WhiteListItem("braziliandelight:acai_berries")
			WhiteListItem("braziliandelight:coconut")
			WhiteListItem("braziliandelight:green_coconut")
			WhiteListItem("braziliandelight:guarana_fruit")
			WhiteListItem("neapolitan:banana_bunch")
			WhiteListItem("neapolitan:strawberries")
			WhiteListItem("neapolitan:white_strawberries")
			WhiteListItem("vinery:cherry")
			WhiteListItem("vinery:red_grape")
			WhiteListItem("vinery:white_grape")
			WhiteListItem("vinery:jungle_grapes_red")
			WhiteListItem("vinery:jungle_grapes_white")
			WhiteListItem("vinery:savanna_grapes_red")
			WhiteListItem("vinery:savanna_grapes_white")
			WhiteListItem("vinery:taiga_grapes_red")
			WhiteListItem("vinery:taiga_grapes_white")
			WhiteListItem("culturaldelights:avocado")
			WhiteListItem("delightful:cantaloupe")
			WhiteListItem("delightful:mini_melon")
			WhiteListItem("delightful:salmonberries")
			WhiteListItem("delighto_flight:cloud_berries")
			WhiteListItem("delighto_flight:thunder_fruit")
			WhiteListItem("hearthandharvest:raspberry")
			WhiteListItem("ramadandelight:date")
			WhiteListItem("yeastnfeast:rose_hips")
			WhiteListItem("brewincompatdelight:juniper_berries")
			WhiteListItem("argentinas_delight:membrillo")
			WhiteListItem("extravagantdelight:green_mango")
			WhiteListItem("extravagantdelight:lush_mango")
			WhiteListItem("extravagantdelight:muscadine_grapes")
			WhiteListItem("extravagantdelight:sloe_berries")
			WhiteListItem("fruitfulfun:citron")
			WhiteListItem("fruitfulfun:redlove")
			WhiteListItem("cosmopolitan:arbutus_berries")
			WhiteListItem("cosmopolitan:boilberry")
			WhiteListItem("cosmopolitan:spinalberry")
			WhiteListItem("cosmopolitan:wildberry")
			WhiteListItem("vintagedelight:gearo_berry")
			WhiteListItem("youkaishomecoming:black_grape")
			WhiteListItem("oceanic_delight:sea_grape")
			WhiteListItem("nethervinery:crimson_grape")
			WhiteListItem("nethervinery:warped_grape")
			WhiteListItem("nethers_exoticism:bouddha_s_hand")
			WhiteListItem("nethers_exoticism:jaboticaba")
			WhiteListItem("nethers_exoticism:kiwano")
			WhiteListItem("nethers_exoticism:ramboutan")
			WhiteListItem("frightsdelight:soul_berry")
			WhiteListItem("frightsdelight:wither_berry")
			WhiteListItem("trailandtales_delight:crystal_crabapple")
			WhiteListItem("trailandtales_delight:unripe_crystal_crabapple")
			WhiteListItem("trailandtales_delight:lantern_fruit")
			WhiteListItem("regions_unexplored:duskmelon_slice")
			WhiteListItem("regions_unexplored:hanging_earlight_fruit")
			WhiteListItem("vietnam_delight:green_banana_bunch")
			WhiteListItem("vietnam_delight:kumquat")
			WhiteListItem("vietnam_delight:green_kumquat")
			WhiteListItem("vietnam_delight:tamarind")
			WhiteListItem("alexsmobs:acacia_blossom")

			// 坚果与种子
			WhiteListItem("bountifulfares:walnut")
			WhiteListItem("seeddelight:acorn")
			WhiteListItem("seeddelight:pinecone")
			WhiteListItem("seeddelight:sunflower_seed")
			WhiteListItem("vintagedelight:peanut")
			WhiteListItem("foragersinsight:black_acorn")
			WhiteListItem("foragersinsight:poppy_seeds")
			WhiteListItem("mooncake_delight:black_sesame")
			WhiteListItem("mooncake_delight:white_sesame")

			// 经济作物。咖啡、茶、香料各留统一后的代表项。
			WhiteListItem("farmersrespite:coffee_berries")
			WhiteListItem("farmersrespite:green_tea_leaves")
			WhiteListItem("farmersrespite:black_tea_leaves")
			WhiteListItem("farmersrespite:yellow_tea_leaves")
			WhiteListItem("neapolitan:mint_leaves")
			WhiteListItem("neapolitan:vanilla_pods")
			WhiteListItem("youkaishomecoming:camellia")
			WhiteListItem("youkaishomecoming:udumbara_flower")
			WhiteListItem("festive_delight:cinnamon_sticks")
			WhiteListItem("rusticdelight:cotton_boll")
			WhiteListItem("alcocraftplus:hop")
			WhiteListItem("argentinas_delight:yerbamate")
			WhiteListItem("braziliandelight:yerba_mate_leaves")
			WhiteListItem("foragersinsight:roselle_calyx")
			WhiteListItem("foragersinsight:spruce_tips")
			WhiteListItem("endersdelight:chorusflame")
			WhiteListItem("endersdelight:ethereal_saffron")
			WhiteListItem("endersdelight:voidpepper")
			WhiteListItem("mynethersdelight:powder_cannon")
			WhiteListItem("untamedwilds:flora_canola")
			WhiteListItem("vietnam_delight:black_peppercorn")
			WhiteListItem("vietnam_delight:green_peppercorn")
			WhiteListItem("vietnam_delight:white_peppercorn")
			WhiteListItem("vietnam_delight:banyan_flower")
			WhiteListItem("vietnam_delight:lotus_flower")
			WhiteListItem("vietnam_delight:purple_lotus_flower")
			WhiteListItem("vietnam_delight:white_lotus_flower")
			WhiteListItem("vietnam_delight:yellow_lotus_flower")
		}
	}
}

fun standardCropWhitelist(block: StandardCropWhitelist.Builder.() -> Unit): StandardCropWhitelist.Config = StandardCropWhitelist.standardCropWhitelist(block)



