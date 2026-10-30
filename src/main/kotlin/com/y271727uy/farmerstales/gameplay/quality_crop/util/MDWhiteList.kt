@file:Suppress("unused")

package com.y271727uy.farmerstales.gameplay.quality_crop.util

import net.minecraft.resources.ResourceLocation
import net.minecraft.world.item.ItemStack
import net.minecraft.world.level.block.Block
import net.minecraftforge.registries.ForgeRegistries

/**
 * Manors Bounty 专用浆果 / 果树白名单。
 *
 * 这个对象只负责 manors_bounty 的收获目标，避免继续污染通用白名单。
 */
object MDWhiteList {
	internal data class Entry(
		val dropItemId: ResourceLocation,
		val blockId: ResourceLocation,
	)

	@Volatile
	private var active: Config = createDefaultConfig()

	fun mdWhiteList(block: Builder.() -> Unit): Config = Builder().apply(block).build()

	fun install(config: Config) {
		active = config
	}

	fun clear() {
		active = createDefaultConfig()
	}

	fun current(): Config = active

	fun matches(stack: ItemStack, block: Block): Boolean = active.matches(stack, block)

	fun matches(stack: ItemStack): Boolean = active.matches(stack)

	fun matches(block: Block): Boolean = active.matches(block)

	fun expectedDropId(block: Block): ResourceLocation? = active.expectedDropId(block)

	fun dropIds(block: Block): List<ResourceLocation> = active.dropIds(block)

	class Builder {
		private val entries = linkedSetOf<Entry>()

		fun WhiteListItem(dropItemId: String, blockId: String) {
			val drop = parseResourceLocation(dropItemId)
			val block = parseResourceLocation(blockId)
			if (drop != null && block != null) {
				entries.add(Entry(drop, block))
			}
		}

		fun build(): Config = Config(entries.toSet())

		private fun parseResourceLocation(raw: String): ResourceLocation? {
			return ResourceLocation.tryParse(raw.trim().removePrefix("#"))
		}
	}

	class Config internal constructor(
		private val entries: Set<Entry>,
	) {
		val isEmpty: Boolean
			get() = entries.isEmpty()

		fun matches(stack: ItemStack, block: Block): Boolean {
			if (stack.isEmpty) {
				return false
			}

			val blockId = ForgeRegistries.BLOCKS.getKey(block) ?: return false
			val itemId = ForgeRegistries.ITEMS.getKey(stack.item) ?: return false
			return entries.any { entry -> entry.blockId == blockId && entry.dropItemId == itemId }
		}

		fun matches(stack: ItemStack): Boolean {
			if (stack.isEmpty) {
				return false
			}

			val itemId = ForgeRegistries.ITEMS.getKey(stack.item) ?: return false
			return entries.any { entry -> entry.dropItemId == itemId }
		}

		fun matches(block: Block): Boolean {
			val blockId = ForgeRegistries.BLOCKS.getKey(block) ?: return false
			return entries.any { entry -> entry.blockId == blockId }
		}

		fun expectedDropId(block: Block): ResourceLocation? = dropIds(block).firstOrNull()

		fun dropIds(block: Block): List<ResourceLocation> {
			val blockId = ForgeRegistries.BLOCKS.getKey(block) ?: return emptyList()
			return entries.filter { entry -> entry.blockId == blockId }.map { entry -> entry.dropItemId }
		}

		companion object {
			val EMPTY: Config = Config(emptySet())
		}
	}

	/**
	 * 庄园馀话的收获产物。方块自己生成掉落物，品质在掉落物进入世界时写入。
	 * 方块 id 只用来区分同一产物来自哪株植物。
	 */
	private fun createDefaultConfig(): Config {
		return mdWhiteList {
			WhiteListItem("manors_bounty:baby_corn", "manors_bounty:baby_corn_bush")
			WhiteListItem("manors_bounty:blackcurrant", "manors_bounty:blackcurrant_bush")
			WhiteListItem("manors_bounty:gooseberry", "manors_bounty:blackcurrant_bush")
			WhiteListItem("manors_bounty:cherry_tomatoes", "manors_bounty:cherry_tomatoes_bush")
			WhiteListItem("manors_bounty:end_pepper", "manors_bounty:end_pepper_bush")
			WhiteListItem("manors_bounty:blackberry", "manors_bounty:raspberry_bush")
			WhiteListItem("manors_bounty:wild_jalapeno", "manors_bounty:wild_jalapeno_bush")
			WhiteListItem("manors_bounty:jalapeno", "manors_bounty:jalapeno_crop")
			WhiteListItem("manors_bounty:tomato", "manors_bounty:tomato_crop")
			WhiteListItem("manors_bounty:purple_sugar_cane", "manors_bounty:purple_sugar_cane_block")

			WhiteListItem("minecraft:apple", "manors_bounty:apple_leaves")
			WhiteListItem("manors_bounty:green_apple", "manors_bounty:apple_leaves")
			WhiteListItem("manors_bounty:apricot", "manors_bounty:apricot_leaves")
			WhiteListItem("manors_bounty:ume", "manors_bounty:apricot_leaves")
			WhiteListItem("manors_bounty:cherries", "manors_bounty:cherries_tree_leaves")
			WhiteListItem("manors_bounty:gold_kiwifruit", "manors_bounty:kiwifruit_tree_leaves")
			WhiteListItem("manors_bounty:olive_fruit", "manors_bounty:olive_tree_leaves")
			WhiteListItem("manors_bounty:black_olive_fruit", "manors_bounty:olive_tree_leaves")
			WhiteListItem("manors_bounty:grapefruit", "manors_bounty:orange_leaves")
			WhiteListItem("manors_bounty:starfruit", "manors_bounty:starfruit_tree_leaves")
			WhiteListItem("manors_bounty:musang_king_durian", "manors_bounty:durian_fruit")
			WhiteListItem("manors_bounty:yuzumelo", "manors_bounty:yuzumelo_fruit")
			WhiteListItem("manors_bounty:holly_berry", "manors_bounty:holly_tree_leaves")
			WhiteListItem("manors_bounty:nutmeg", "manors_bounty:myriclave_tree_leaves")
			WhiteListItem("manors_bounty:star_anise", "manors_bounty:cinnise_tree_leaves")
			WhiteListItem("manors_bounty:hazelnut", "manors_bounty:hazelnut_leaves")
			WhiteListItem("manors_bounty:muscat_grape", "manors_bounty:grape_crop")
			WhiteListItem("manors_bounty:dragon_fruit", "manors_bounty:dragon_fruit_crop")
			WhiteListItem("manors_bounty:scots_pine_cone", "manors_bounty:scots_pine_leaves")
			WhiteListItem("manors_bounty:cashew", "manors_bounty:cashew_leaves")
			WhiteListItem("manors_bounty:almond", "manors_bounty:almond_leaves")
			WhiteListItem("manors_bounty:walpecan", "manors_bounty:walpecan_leaves")
			WhiteListItem("manors_bounty:pistachio", "manors_bounty:pistachio_leaves")
			WhiteListItem("manors_bounty:chestnut", "manors_bounty:chestnut_leaves")
		}
	}
}

fun mdWhiteList(block: MDWhiteList.Builder.() -> Unit): MDWhiteList.Config = MDWhiteList.mdWhiteList(block)
