package com.y271727uy.farmerstales.entity.data

import com.y271727uy.farmerstales.entity.ModEntities
import com.y271727uy.farmerstales.entity.registry.MobRegistry.MobDropDefinition
import net.minecraft.data.loot.EntityLootSubProvider
import net.minecraft.world.entity.EntityType
import net.minecraft.world.flag.FeatureFlags
import net.minecraft.world.level.storage.loot.LootPool
import net.minecraft.world.level.storage.loot.LootTable
import net.minecraft.world.level.storage.loot.entries.LootItem
import net.minecraft.world.level.storage.loot.functions.LootingEnchantFunction
import net.minecraft.world.level.storage.loot.functions.SetItemCountFunction
import net.minecraft.world.level.storage.loot.predicates.LootItemRandomChanceCondition
import net.minecraft.world.level.storage.loot.providers.number.ConstantValue
import net.minecraft.world.level.storage.loot.providers.number.UniformGenerator
import java.util.stream.Stream

/** Serializes mob loot declarations through Minecraft's validated loot-table provider. */
class ModEntityLootTableProvider : EntityLootSubProvider(FeatureFlags.REGISTRY.allFlags()) {
    override fun generate() {
        ModEntities.lootTables().forEach { entry ->
            val table = LootTable.lootTable()
            entry.loot.drops.forEach { drop -> table.withPool(poolFor(drop)) }
            add(entry.type, table)
        }
    }

    override fun getKnownEntityTypes(): Stream<EntityType<*>> =
        ModEntities.lootTables().stream().map { it.type }

    private fun poolFor(drop: MobDropDefinition): LootPool.Builder {
        val pool = LootPool.lootPool().setRolls(ConstantValue.exactly(1.0F))
        drop.items.forEach { itemReference ->
            val item = LootItem.lootTableItem(itemReference.resolve())
                .apply(SetItemCountFunction.setCount(range(drop.count)))
            drop.lootingBonus?.let { bonus ->
                item.apply(LootingEnchantFunction.lootingMultiplier(range(bonus)))
            }
            pool.add(item)
        }
        if (drop.chance < 1.0F) {
            pool.`when`(LootItemRandomChanceCondition.randomChance(drop.chance))
        }
        return pool
    }

    private fun range(values: IntRange): UniformGenerator =
        UniformGenerator.between(values.first.toFloat(), values.last.toFloat())
}
