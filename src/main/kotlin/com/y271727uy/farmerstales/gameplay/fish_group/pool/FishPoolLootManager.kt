package com.y271727uy.farmerstales.gameplay.fish_group.pool

import com.google.gson.GsonBuilder
import com.google.gson.JsonArray
import com.google.gson.JsonElement
import com.google.gson.JsonObject
import com.google.gson.JsonParseException
import com.mojang.brigadier.exceptions.CommandSyntaxException
import com.y271727uy.farmerstales.FTMod
import net.minecraft.core.registries.Registries
import net.minecraft.nbt.TagParser
import net.minecraft.resources.ResourceLocation
import net.minecraft.server.level.ServerLevel
import net.minecraft.server.packs.resources.ResourceManager
import net.minecraft.server.packs.resources.SimpleJsonResourceReloadListener
import net.minecraft.tags.TagKey
import net.minecraft.util.GsonHelper
import net.minecraft.util.Mth
import net.minecraft.util.RandomSource
import net.minecraft.util.profiling.ProfilerFiller
import net.minecraft.world.item.Item
import net.minecraft.world.item.ItemStack
import net.minecraft.world.item.Items
import net.minecraftforge.registries.ForgeRegistries
import java.util.Locale

object FishPoolLootManager : SimpleJsonResourceReloadListener(
    GsonBuilder().create(),
    "fish_pool_loot_tables/gameplay/fishing_pools",
) {
    @Volatile
    private var lootTables: Map<ResourceLocation, FishPoolLootTable> = emptyMap()

    override fun apply(
        jsonEntries: Map<ResourceLocation, JsonElement>,
        resourceManager: ResourceManager,
        profiler: ProfilerFiller,
    ) {
        val parsedTables = mutableMapOf<ResourceLocation, FishPoolLootTable>()
        jsonEntries.forEach { (id, element) ->
            try {
                parsedTables[id] = parseTable(id, GsonHelper.convertToJsonObject(element, "fish_pool_loot"))
            } catch (exception: Exception) {
                FTMod.LOGGER.error("Failed to parse fish pool loot table {}", id, exception)
            }
        }
        lootTables = parsedTables.toMap()
        FTMod.LOGGER.info("Loaded {} fish pool loot tables", lootTables.size)
    }

    fun rollReward(poolId: ResourceLocation, level: ServerLevel, random: RandomSource): ItemStack {
        val lootTable = lootTables[poolId]
        if (lootTable != null) return lootTable.roll(level, random)

        val definition = FishPoolFactory.get(poolId).orElse(null)
        if (definition != null) return fromDefinition(definition).roll(level, random)

        FTMod.LOGGER.warn("Missing fish pool loot table for {}", poolId)
        return ItemStack.EMPTY
    }

    private fun fromDefinition(definition: FishPoolDefinition) = FishPoolLootTable(
        definition.minFishCount,
        definition.maxFishCount,
        definition.fishKing,
        definition.biomes,
        definition.weatherRequirement,
        definition.timeRequirement,
        definition.outputs.map { output ->
            FishPoolLootEntry(
                output.itemId,
                output.tagId,
                output.usesTag(),
                output.weight,
                CountRange(output.minCount, output.maxCount),
                output.nbt,
            )
        },
    )

    private fun parseTable(id: ResourceLocation, root: JsonObject): FishPoolLootTable {
        val entries = mutableListOf<FishPoolLootEntry>()
        val minFishCount = parseRootPositiveInt(root, "MinFishCount", "minFishCount")
        val maxFishCount = parseRootPositiveInt(root, "MaxFishCount", "maxFishCount")
        val fishKing = parseRootResourceLocation(id, root, "FishKing", "fishKing")
        val biomes = parseRootBiomeList(id, root, "Biome", "biome")
        val weatherRequirement = parseWeatherRequirement(id, root)
        val timeRequirement = parseTimeRequirement(id, root)

        if (root.has("entries")) addEntries(id, GsonHelper.getAsJsonArray(root, "entries"), entries)
        if (root.has("pools")) {
            GsonHelper.getAsJsonArray(root, "pools").forEach { poolElement ->
                val poolObject = GsonHelper.convertToJsonObject(poolElement, "pool")
                if (poolObject.has("entries")) {
                    addEntries(id, GsonHelper.getAsJsonArray(poolObject, "entries"), entries)
                }
            }
        }
        if (entries.isEmpty()) throw JsonParseException("Fish pool loot table $id does not contain any entries")

        return FishPoolLootTable(
            minFishCount,
            maxFishCount,
            fishKing,
            biomes,
            weatherRequirement,
            timeRequirement,
            entries.toList(),
        )
    }

    private fun addEntries(
        tableId: ResourceLocation,
        jsonEntries: JsonArray,
        parsedEntries: MutableList<FishPoolLootEntry>,
    ) {
        jsonEntries.forEach { entryElement ->
            parseEntry(tableId, GsonHelper.convertToJsonObject(entryElement, "entry"))?.let(parsedEntries::add)
        }
    }

    private fun parseEntry(tableId: ResourceLocation, entryObject: JsonObject): FishPoolLootEntry? {
        val configuredTag = getNullableString(entryObject, "tag")
        var configuredItem = getNullableString(entryObject, "item")
        if (configuredItem.isNullOrBlank() && entryObject.has("name")) {
            configuredItem = getNullableString(entryObject, "name")
        }

        val useTag = !configuredTag.isNullOrBlank()
        val itemId = configuredItem?.takeUnless(String::isBlank)?.let {
            parseResourceLocation(tableId, it, "item")
        }
        val tagId = if (useTag) parseResourceLocation(tableId, configuredTag, "tag") else null
        val countRange = parseCountRange(entryObject)
        val nbt = parseNbt(entryObject)
        val weight = GsonHelper.getAsInt(entryObject, "weight", 1).coerceAtLeast(1)

        if (useTag && tagId == null) {
            FTMod.LOGGER.warn("Fish pool loot table {} has an invalid tag entry: {}", tableId, configuredTag)
            return null
        }
        if (!useTag && itemId == null) {
            FTMod.LOGGER.warn("Fish pool loot table {} has an entry without a valid item: {}", tableId, entryObject)
            return null
        }
        return FishPoolLootEntry(itemId, tagId, useTag, weight, countRange, nbt)
    }

    private fun parseCountRange(entryObject: JsonObject): CountRange {
        parseNamedCountRange(entryObject)?.let { return it }
        if (entryObject.has("count")) return readCount(entryObject.get("count"))
        if (entryObject.has("functions")) {
            GsonHelper.getAsJsonArray(entryObject, "functions").forEach { functionElement ->
                val functionObject = GsonHelper.convertToJsonObject(functionElement, "function")
                if (GsonHelper.getAsString(functionObject, "function", "") == "minecraft:set_count" &&
                    functionObject.has("count")
                ) {
                    return readCount(functionObject.get("count"))
                }
            }
        }
        return CountRange(1, 1)
    }

    private fun parseNamedCountRange(entryObject: JsonObject): CountRange? {
        getFirstPresent(entryObject, "MinRange", "minRange")?.let { return readRange(it) }
        getFirstPresentInt(entryObject, "MaxFishAwarded", "maxFishAwarded")?.let {
            val count = it.coerceAtLeast(1)
            return CountRange(count, count)
        }
        return null
    }

    private fun readCount(countElement: JsonElement): CountRange {
        if (countElement.isJsonPrimitive) {
            val count = countElement.asInt.coerceAtLeast(1)
            return CountRange(count, count)
        }
        val countObject = GsonHelper.convertToJsonObject(countElement, "count")
        val min = GsonHelper.getAsInt(countObject, "min", 1).coerceAtLeast(1)
        return CountRange(min, GsonHelper.getAsInt(countObject, "max", min).coerceAtLeast(min))
    }

    private fun readRange(rangeElement: JsonElement): CountRange {
        if (!rangeElement.isJsonArray) return readCount(rangeElement)
        val rangeArray = GsonHelper.convertToJsonArray(rangeElement, "MinRange")
        if (rangeArray.size() < 2) throw JsonParseException("MinRange must contain at least [min, max]")
        val min = rangeArray[0].asInt.coerceAtLeast(1)
        return CountRange(min, rangeArray[1].asInt.coerceAtLeast(min))
    }

    private fun parseNbt(entryObject: JsonObject): String? {
        getNullableString(entryObject, "nbt")?.takeUnless(String::isBlank)?.let { return it }
        if (entryObject.has("functions")) {
            GsonHelper.getAsJsonArray(entryObject, "functions").forEach { functionElement ->
                val functionObject = GsonHelper.convertToJsonObject(functionElement, "function")
                if (GsonHelper.getAsString(functionObject, "function", "") == "minecraft:set_nbt") {
                    return getNullableString(functionObject, "tag")
                }
            }
        }
        return null
    }

    private fun parseResourceLocation(tableId: ResourceLocation, rawId: String, fieldName: String): ResourceLocation? =
        ResourceLocation.tryParse(rawId).also {
            if (it == null) FTMod.LOGGER.warn("Fish pool loot table {} has invalid {} id {}", tableId, fieldName, rawId)
        }

    private fun getNullableString(objectValue: JsonObject, memberName: String): String? =
        if (objectValue.has(memberName)) GsonHelper.getAsString(objectValue, memberName, "")?.trim() else null

    private fun parseRootResourceLocation(
        tableId: ResourceLocation,
        objectValue: JsonObject,
        vararg keys: String,
    ): ResourceLocation? {
        keys.forEach { key ->
            getNullableString(objectValue, key)?.takeUnless(String::isBlank)?.let {
                return parseResourceLocation(tableId, it, key)
            }
        }
        return null
    }

    private fun parseRootBiomeList(
        tableId: ResourceLocation,
        objectValue: JsonObject,
        vararg keys: String,
    ): List<ResourceLocation> {
        keys.forEach { key ->
            if (!objectValue.has(key) || objectValue.get(key).isJsonNull) return@forEach
            val value = objectValue.get(key)
            if (value.isJsonArray) {
                return GsonHelper.convertToJsonArray(value, key)
                    .mapNotNull { entry ->
                        entry.takeUnless(JsonElement::isJsonNull)?.asString?.trim()?.takeUnless(String::isBlank)
                            ?.let { parseResourceLocation(tableId, it, key) }
                    }
                    .distinct()
            }
            val rawValue = GsonHelper.convertToString(value, key).trim()
            if (rawValue.isBlank()) return emptyList()
            return listOfNotNull(parseResourceLocation(tableId, rawValue, key))
        }
        return emptyList()
    }

    private fun parseWeatherRequirement(
        tableId: ResourceLocation,
        root: JsonObject,
    ): FishPoolDefinition.WeatherRequirement? {
        val configuredWeather = getNullableString(root, "WeatherRequirement")
            ?.takeUnless(String::isBlank)
            ?: getNullableString(root, "weatherRequirement")
        if (configuredWeather.isNullOrBlank() || configuredWeather.equals("null", ignoreCase = true)) return null
        return try {
            FishPoolDefinition.WeatherRequirement.valueOf(configuredWeather.trim().uppercase(Locale.ROOT))
        } catch (_: IllegalArgumentException) {
            FTMod.LOGGER.warn("Fish pool loot table {} has invalid WeatherRequirement {}", tableId, configuredWeather)
            null
        }
    }

    private fun parseTimeRequirement(
        tableId: ResourceLocation,
        root: JsonObject,
    ): FishPoolDefinition.TimeRequirement? {
        val configuredTime = getNullableString(root, "TimeRequirement")
            ?.takeUnless(String::isBlank)
            ?: getNullableString(root, "timeRequirement")
        if (configuredTime.isNullOrBlank() || configuredTime.equals("null", ignoreCase = true)) return null
        return when (configuredTime.trim().lowercase(Locale.ROOT)) {
            "day" -> FishPoolDefinition.TimeRequirement.DAY
            "night" -> FishPoolDefinition.TimeRequirement.NIGHT
            else -> {
                FTMod.LOGGER.warn("Fish pool loot table {} has invalid TimeRequirement {}", tableId, configuredTime)
                null
            }
        }
    }

    private fun getFirstPresent(objectValue: JsonObject, vararg keys: String): JsonElement? =
        keys.firstOrNull(objectValue::has)?.let(objectValue::get)

    private fun getFirstPresentInt(objectValue: JsonObject, vararg keys: String): Int? =
        keys.firstOrNull(objectValue::has)?.let { GsonHelper.getAsInt(objectValue, it, 1) }

    private fun parseRootPositiveInt(objectValue: JsonObject, vararg keys: String): Int? =
        getFirstPresentInt(objectValue, *keys)?.coerceAtLeast(1)

    fun resolveDefinition(definition: FishPoolDefinition): FishPoolDefinition {
        val lootTable = lootTables[definition.id] ?: return definition
        val maxFishCount = lootTable.maxFishCount ?: definition.maxFishCount
        val minFishCount =
            (lootTable.minFishCount ?: lootTable.maxFishCount ?: definition.minFishCount).coerceAtLeast(1)
        val resolvedMaxFishCount = maxFishCount.coerceAtLeast(minFishCount)
        val fishKing = lootTable.fishKing ?: definition.fishKing
        val biomes = lootTable.biomes.ifEmpty { definition.biomes }
        val weatherRequirement = lootTable.weatherRequirement ?: definition.weatherRequirement
        val timeRequirement = lootTable.timeRequirement ?: definition.timeRequirement

        if (minFishCount == definition.minFishCount &&
            resolvedMaxFishCount == definition.maxFishCount &&
            fishKing == definition.fishKing &&
            biomes == definition.biomes &&
            weatherRequirement == definition.weatherRequirement &&
            timeRequirement == definition.timeRequirement
        ) {
            return definition
        }
        return FishPoolDefinition(
            definition.id,
            definition.environment,
            minFishCount,
            resolvedMaxFishCount,
            fishKing,
            biomes,
            weatherRequirement,
            timeRequirement,
            definition.outputs,
        )
    }

    private data class FishPoolLootTable(
        val minFishCount: Int?,
        val maxFishCount: Int?,
        val fishKing: ResourceLocation?,
        val biomes: List<ResourceLocation>,
        val weatherRequirement: FishPoolDefinition.WeatherRequirement?,
        val timeRequirement: FishPoolDefinition.TimeRequirement?,
        val entries: List<FishPoolLootEntry>,
    ) {
        fun roll(level: ServerLevel, random: RandomSource): ItemStack {
            val totalWeight = entries.sumOf(FishPoolLootEntry::weight)
            if (totalWeight <= 0) return ItemStack.EMPTY
            var roll = random.nextInt(totalWeight)
            entries.forEach { entry ->
                roll -= entry.weight
                if (roll < 0) return entry.createStack(level, random)
            }
            return ItemStack.EMPTY
        }
    }

    private data class FishPoolLootEntry(
        val itemId: ResourceLocation?,
        val tagId: ResourceLocation?,
        val useTag: Boolean,
        val weight: Int,
        val countRange: CountRange,
        val nbt: String?,
    ) {
        fun createStack(level: ServerLevel, random: RandomSource): ItemStack {
            val item = resolveItem(level, random)
            if (item == null || item == Items.AIR) return ItemStack.EMPTY
            return ItemStack(item, Mth.nextInt(random, countRange.min, countRange.max)).also { stack ->
                if (!nbt.isNullOrBlank()) {
                    try {
                        stack.tag = TagParser.parseTag(nbt)
                    } catch (exception: CommandSyntaxException) {
                        FTMod.LOGGER.warn("Failed to parse fish pool reward nbt {}", nbt, exception)
                    }
                }
            }
        }

        private fun resolveItem(level: ServerLevel, random: RandomSource): Item? {
            if (useTag) {
                val resolvedTagId = tagId ?: return null
                val itemRegistry = level.registryAccess().registryOrThrow(Registries.ITEM)
                val taggedItems = itemRegistry.getTag(TagKey.create(Registries.ITEM, resolvedTagId))
                    .map { named -> named.map { holder -> holder.value() }.filter { it != Items.AIR } }
                    .orElse(emptyList())
                if (taggedItems.isEmpty()) {
                    FTMod.LOGGER.warn("Fish pool tag {} does not contain any items", resolvedTagId)
                    return null
                }
                return taggedItems[random.nextInt(taggedItems.size)]
            }
            return itemId?.let(ForgeRegistries.ITEMS::getValue)
        }
    }

    private data class CountRange(val min: Int, val max: Int)
}
