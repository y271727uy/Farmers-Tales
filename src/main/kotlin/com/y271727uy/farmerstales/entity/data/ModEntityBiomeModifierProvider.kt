package com.y271727uy.farmerstales.entity.data

import com.google.gson.JsonArray
import com.google.gson.JsonObject
import com.google.gson.JsonPrimitive
import com.y271727uy.farmerstales.FTMod
import com.y271727uy.farmerstales.entity.ModEntities
import net.minecraft.data.CachedOutput
import net.minecraft.data.DataProvider
import net.minecraft.data.PackOutput
import java.util.concurrent.CompletableFuture

/** Writes Forge biome modifiers from the naturalSpawn declarations in ModEntities. */
class ModEntityBiomeModifierProvider(private val output: PackOutput) : DataProvider {
    override fun run(cache: CachedOutput): CompletableFuture<*> {
        val writes = ModEntities.biomeSpawns().map { entry ->
            val json = JsonObject().apply {
                addProperty("type", "forge:add_spawns")
                addProperty("biomes", "#${entry.spawn.biomeTag}")
                add("spawners", JsonArray().apply {
                    add(JsonObject().apply {
                        addProperty("type", "${FTMod.MODID}:${entry.entityId}")
                        addProperty("weight", entry.spawn.weight)
                        addProperty("minCount", entry.spawn.minCount)
                        addProperty("maxCount", entry.spawn.maxCount)
                    })
                })
            }
            val path = output.outputFolder.resolve(
                "data/${FTMod.MODID}/forge/biome_modifier/add_${entry.entityId}_spawns.json",
            )
            DataProvider.saveStable(cache, json, path)
        }
        return CompletableFuture.allOf(*writes.toTypedArray())
    }

    override fun getName(): String = "Farmer's Tales Entity Biome Modifiers"
}
