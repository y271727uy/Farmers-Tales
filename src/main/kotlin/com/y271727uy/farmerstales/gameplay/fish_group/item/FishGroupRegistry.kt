package com.y271727uy.farmerstales.gameplay.fish_group.item

import com.y271727uy.farmerstales.FTMod
import com.y271727uy.farmerstales.gameplay.fish_group.entity.AbstractFishPoolEntity
import com.y271727uy.farmerstales.gameplay.fish_group.entity.OceanFishPoolEntity
import com.y271727uy.farmerstales.gameplay.fish_group.entity.RiverFishPoolEntity
import com.y271727uy.farmerstales.gameplay.fish_group.pool.FishPoolDefinition
import com.y271727uy.farmerstales.gameplay.fish_group.pool.FishPoolFactory
import net.minecraft.resources.ResourceLocation
import net.minecraft.world.entity.EntityType
import net.minecraft.world.entity.MobCategory
import net.minecraft.world.item.FishingRodItem
import net.minecraft.world.item.Item
import net.minecraft.world.level.Level
import net.minecraftforge.eventbus.api.IEventBus
import net.minecraftforge.registries.DeferredRegister
import net.minecraftforge.registries.ForgeRegistries
import net.minecraftforge.registries.RegistryObject

object FishGroupRegistry {
    private val items: DeferredRegister<Item> = DeferredRegister.create(ForgeRegistries.ITEMS, FTMod.MODID)
    private val entityTypes: DeferredRegister<EntityType<*>> =
        DeferredRegister.create(ForgeRegistries.ENTITY_TYPES, FTMod.MODID)
    private val fishPoolRegistrations = linkedMapOf<ResourceLocation, FishPoolRegistration>()

    @JvmField
    val BAMBOO_FISHING_ROD: RegistryObject<Item> = items.register("bamboo_fishing_rod") {
        FishingRodItem(Item.Properties().stacksTo(1).durability(32))
    }

    init {
        FishPoolFactory.getAll().forEach(::registerFishPool)
    }

    private fun registerFishPool(definition: FishPoolDefinition) {
        val path = definition.id.path
        val item = items.register<Item>(path) { FloatingPoolsItem(Item.Properties()) }
        val entityType: RegistryObject<EntityType<*>> = when (definition.environment) {
            FishPoolDefinition.Environment.RIVER -> entityTypes.register(path) {
                EntityType.Builder.of(::RiverFishPoolEntity, MobCategory.MISC)
                    .sized(2.0F, 2.5F)
                    .build(definition.id.toString())
            }

            FishPoolDefinition.Environment.OCEAN -> entityTypes.register(path) {
                EntityType.Builder.of(::OceanFishPoolEntity, MobCategory.MISC)
                    .sized(2.0F, 2.5F)
                    .build(definition.id.toString())
            }
        }
        val registration = FishPoolRegistration(definition.id, definition.environment, item, entityType)
        check(fishPoolRegistrations.putIfAbsent(definition.id, registration) == null) {
            "Duplicate fish pool registry entry: ${definition.id}"
        }
    }

    @JvmStatic
    fun getFishPoolRegistration(id: ResourceLocation): FishPoolRegistration =
        fishPoolRegistrations[id] ?: error("Missing fish pool registration for $id")

    @JvmStatic
    fun getFishPoolRegistrations(): Collection<FishPoolRegistration> = fishPoolRegistrations.values.toList()

    @JvmStatic
    fun register(bus: IEventBus) {
        items.register(bus)
        entityTypes.register(bus)
    }

    data class FishPoolRegistration(
        val id: ResourceLocation,
        val environment: FishPoolDefinition.Environment,
        val item: RegistryObject<Item>,
        val entityType: RegistryObject<EntityType<*>>,
    ) {
        fun create(level: Level): AbstractFishPoolEntity? = entityType.get().create(level) as? AbstractFishPoolEntity
    }
}
