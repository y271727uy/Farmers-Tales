package com.y271727uy.farmerstales.gameplay.fish_group.pool

import com.google.gson.JsonObject
import net.minecraft.resources.ResourceLocation

class FishPoolLootEntryDefinition(
    itemId: ResourceLocation?,
    val tagId: ResourceLocation?,
    weight: Int,
    minCount: Int,
    maxCount: Int,
    nbt: String?,
) {
    val itemId = if (tagId == null) itemId else null
    val weight = weight.coerceAtLeast(1)
    val minCount = minCount.coerceAtLeast(1)
    val maxCount = maxCount.coerceAtLeast(this.minCount)
    val nbt = nbt?.takeUnless(String::isBlank)

    init {
        require(itemId != null || tagId != null) { "Fish pool loot output must define either an item or a tag" }
    }

    fun withNbt(nbt: String): FishPoolLootEntryDefinition =
        FishPoolLootEntryDefinition(itemId, tagId, weight, minCount, maxCount, nbt)

    fun usesTag(): Boolean = tagId != null

    fun toJson(): JsonObject = JsonObject().apply {
        when {
            tagId != null -> addProperty("tag", tagId.toString())
            itemId != null -> addProperty("item", itemId.toString())
        }
        if (weight != 1) addProperty("weight", weight)
        if (minCount != 1 || maxCount != 1) {
            if (minCount == maxCount) {
                addProperty("MaxFishAwarded", maxCount)
            } else {
                add("MinRange", JsonObject().apply {
                    addProperty("min", minCount)
                    addProperty("max", maxCount)
                })
            }
        }
        this@FishPoolLootEntryDefinition.nbt?.let { addProperty("nbt", it) }
    }

    companion object {
        @JvmStatic
        fun item(itemId: String, weight: Int, minCount: Int, maxCount: Int) =
            FishPoolLootEntryDefinition(parse(itemId, "item"), null, weight, minCount, maxCount, null)

        @JvmStatic
        fun item(itemId: String, weight: Int) = item(itemId, weight, 1, 1)

        @JvmStatic
        fun tag(tagId: String, weight: Int, minCount: Int, maxCount: Int) =
            FishPoolLootEntryDefinition(null, parse(tagId, "tag"), weight, minCount, maxCount, null)

        @JvmStatic
        fun tag(tagId: String, weight: Int) = tag(tagId, weight, 1, 1)

        private fun parse(id: String, type: String): ResourceLocation =
            requireNotNull(ResourceLocation.tryParse(id)) { "Invalid fish pool loot $type id: $id" }
    }
}
