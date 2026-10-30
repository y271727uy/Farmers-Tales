@file:Suppress("unused")

package com.y271727uy.farmerstales.gameplay.quality_crop.util

import net.minecraft.resources.ResourceLocation
import net.minecraft.world.item.ItemStack
import net.minecraft.world.level.block.Block
import net.minecraftforge.registries.ForgeRegistries

/**
 * 非标准浆果 / 果树收获白名单：一条配置同时绑定“掉落物 id + 方块 id”。
 *
 * 这样浆果 / 树果 / 非标准 crop 都可以复用同一套入口，不再拆成两份配置。
 */
object BerryFruitHarvestWhitelist {
	internal data class Entry(
		val dropItemId: ResourceLocation,
		val blockId: ResourceLocation,
	)

	@Volatile
	private var active: Config = createDefaultConfig()

	fun berryFruitHarvestWhitelist(block: Builder.() -> Unit): Config = Builder().apply(block).build()

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

		companion object {
			val EMPTY: Config = Config(emptySet())
		}
	}

	private fun createDefaultConfig(): Config {
		return berryFruitHarvestWhitelist {
			WhiteListItem("minecraft:sweet_berries", "minecraft:sweet_berry_bush")
			WhiteListItem("minecraft:glow_berries", "minecraft:cave_vines")

			WhiteListItem("fruitsdelight:hawberry", "fruitsdelight:hawberry_leaves")
			WhiteListItem("fruitsdelight:apple", "fruitsdelight:apple_leaves")
			WhiteListItem("fruitsdelight:bayberry", "fruitsdelight:bayberry_leaves")
			WhiteListItem("fruitsdelight:fig", "fruitsdelight:fig_leaves")
			WhiteListItem("fruitsdelight:pear", "fruitsdelight:pear_leaves")
			WhiteListItem("fruitsdelight:lychee", "fruitsdelight:lychee_leaves")
			WhiteListItem("fruitsdelight:kiwi", "fruitsdelight:kiwi_leaves")
			WhiteListItem("fruitsdelight:mango", "fruitsdelight:mango_leaves")
			WhiteListItem("fruitsdelight:mangosteen", "fruitsdelight:mangosteen_leaves")
			WhiteListItem("fruitsdelight:orange", "fruitsdelight:orange_leaves")
			WhiteListItem("fruitsdelight:peach", "fruitsdelight:peach_leaves")
			WhiteListItem("fruitsdelight:persimmon", "fruitsdelight:persimmon_leaves")
		}
	}
}

fun berryFruitHarvestWhitelist(block: BerryFruitHarvestWhitelist.Builder.() -> Unit): BerryFruitHarvestWhitelist.Config = BerryFruitHarvestWhitelist.berryFruitHarvestWhitelist(block)






