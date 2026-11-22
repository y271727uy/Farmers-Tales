package com.y271727uy.farmerstales.entity

import com.tterrag.registrate.providers.RegistrateItemModelProvider
import com.y271727uy.farmerstales.FTMod
import net.minecraft.resources.ResourceLocation
import net.minecraft.server.level.ServerLevel
import net.minecraft.world.entity.AgeableMob
import net.minecraft.world.entity.EntityType
import net.minecraft.world.entity.ai.attributes.AttributeSupplier
import net.minecraft.world.entity.animal.Animal
import net.minecraft.world.entity.animal.Chicken
import net.minecraft.world.entity.animal.Cow
import net.minecraft.world.entity.animal.Pig
import net.minecraft.world.entity.animal.Rabbit
import net.minecraft.world.entity.animal.Sheep
import net.minecraft.world.item.Item
import net.minecraft.world.item.SpawnEggItem
import net.minecraftforge.client.model.generators.ModelFile
import net.minecraftforge.common.ForgeSpawnEggItem
import net.minecraftforge.event.entity.EntityAttributeCreationEvent
import net.minecraftforge.eventbus.api.IEventBus
import net.minecraftforge.registries.DeferredRegister
import net.minecraftforge.registries.ForgeRegistries
import net.minecraftforge.registries.RegistryObject

/** Mutations are summonable entities with their own appearance and no biome spawn entries. */
object AnimalMutationEntities {
    data class Appearance(
        val series: String,
        val baseType: EntityType<*>,
        val body: ResourceLocation,
        val fur: ResourceLocation? = null,
        val sheared: ResourceLocation = body,
        val emissive: ResourceLocation? = null,
    )

    private val entityTypes = DeferredRegister.create(ForgeRegistries.ENTITY_TYPES, FTMod.MODID)
    private val items = DeferredRegister.create(ForgeRegistries.ITEMS, FTMod.MODID)
    private val appearances = mutableMapOf<ResourceLocation, Appearance>()

    val rabbits = species(EntityType.RABBIT, Rabbit::createAttributes, { type, level ->
        object : Rabbit(type, level) {
            override fun getDefaultLootTable() = EntityType.RABBIT.defaultLootTable

            override fun getBreedOffspring(level: ServerLevel, mate: AgeableMob): Rabbit? =
                if (mate.type == type) type.create(level) else super.getBreedOffspring(level, mate)
        }
    }, advanced = listOf("white_brown", "silver_gray", "warm_orange", "smoky_purple", "golden"))

    val chickens = species(EntityType.CHICKEN, Chicken::createAttributes, { type, level ->
        object : Chicken(type, level) {
            override fun getDefaultLootTable() = EntityType.CHICKEN.defaultLootTable

            override fun getBreedOffspring(level: ServerLevel, mate: AgeableMob): Chicken? =
                if (mate.type == type) type.create(level) else super.getBreedOffspring(level, mate)
        }
    }, advanced = listOf("black_brown", "golden_black", "red_brown", "gray_white", "purple_white", "peach_blossom", "firefly"))

    val cows = species(EntityType.COW, Cow::createAttributes, { type, level ->
        object : Cow(type, level) {
            override fun getDefaultLootTable() = EntityType.COW.defaultLootTable

            override fun getBreedOffspring(level: ServerLevel, mate: AgeableMob): Cow? =
                if (mate.type == type) type.create(level) else super.getBreedOffspring(level, mate)
        }
    }, advanced = listOf("light_brown", "dark_brown", "red_brown", "black_white", "dark_purple", "skeleton"))

    val sheep = species(EntityType.SHEEP, Sheep::createAttributes, { type, level ->
        object : Sheep(type, level) {
            override fun getDefaultLootTable() =
                if (isSheared) EntityType.SHEEP.defaultLootTable else super.getDefaultLootTable()

            override fun getBreedOffspring(level: ServerLevel, mate: AgeableMob): Sheep? {
                val offspring = super.getBreedOffspring(level, mate) ?: return null
                return if (mate.type == type) type.create(level)?.apply { color = offspring.color }
                else offspring
            }
        }
    }, advanced = listOf("tawny", "light_gray", "dark_gray", "coffee", "blue_gray"))

    val pigs = species(EntityType.PIG, Pig::createAttributes, { type, level ->
        object : Pig(type, level) {
            override fun getDefaultLootTable() = EntityType.PIG.defaultLootTable

            override fun getBreedOffspring(level: ServerLevel, mate: AgeableMob): Pig? =
                if (mate.type == type) type.create(level) else super.getBreedOffspring(level, mate)
        }
    }, advanced = listOf("dark_brown", "light_brown", "soot_black", "red_brown", "crimson_purple"))

    fun register(bus: IEventBus) {
        entityTypes.register(bus)
        items.register(bus)
        bus.addListener(::registerAttributes)
    }

    fun generateItemModels(provider: RegistrateItemModelProvider) {
        val template = ModelFile.UncheckedModelFile("minecraft:item/template_spawn_egg")
        for (egg in items.entries) provider.getBuilder(egg.id.path).parent(template)
    }

    @JvmStatic
    fun appearance(type: EntityType<*>): Appearance? = appearances[EntityType.getKey(type)]

    private fun registerAttributes(event: EntityAttributeCreationEvent) {
        for (group in listOf(rabbits, chickens, cows, sheep, pigs)) {
            val attributes = group.attributes().build()
            group.types.forEach { event.put(it.get(), attributes) }
        }
    }

    class Species<T : Animal> internal constructor(
        val types: List<RegistryObject<EntityType<T>>>,
        internal val attributes: () -> AttributeSupplier.Builder,
    )

    private fun <T : Animal> species(
        vanilla: EntityType<T>,
        attributes: () -> AttributeSupplier.Builder,
        factory: EntityType.EntityFactory<T>,
        advanced: List<String>,
    ): Species<T> {
        val animal = EntityType.getKey(vanilla).path
        val isSheep = vanilla == EntityType.SHEEP
        val skins = buildList {
            for (id in advanced) add("advanced" to id)
            add("infection" to "infected")
            add("colorwhite" to "colorwhite")
            add("start" to "star")
            add("gold" to "gold")
        }

        return Species(skins.map { (folder, variant) ->
            val id = "${variant}_$animal"
            val body = texture(folder, id)
            val appearance = when {
                isSheep && folder == "infection" ->
                    Appearance(folder, vanilla, body, fur = texture(folder, "infected_sheep_fur"))
                isSheep -> Appearance(
                    folder, vanilla, body, fur = texture(folder, "${variant}_fur_sheep"),
                    sheared = texture(folder, "${variant}_sheared_sheep"),
                )
                vanilla == EntityType.CHICKEN && variant == "firefly" ->
                    Appearance(folder, vanilla, body, emissive = texture(folder, "firefly_emissive_chicken"))
                else -> Appearance(folder, vanilla, body)
            }
            val registryId = ResourceLocation.fromNamespaceAndPath(FTMod.MODID, id)
            check(appearances.put(registryId, appearance) == null) { "Duplicate mutation entity $registryId" }
            val entity = entityTypes.register(id) {
                EntityType.Builder.of(factory, vanilla.category)
                    .sized(vanilla.width, vanilla.height)
                    .clientTrackingRange(vanilla.clientTrackingRange())
                    .updateInterval(vanilla.updateInterval())
                    .build(registryId.toString())
            }
            items.register<Item>("${id}_spawn_egg") {
                val primaryColor = when (folder) {
                    "infection" -> 0x191D36
                    "colorwhite" -> 0xDBD4EE
                    "start" -> 0x5636AC
                    "gold" -> 0xE7A831
                    else -> 0x7D9561
                }
                val secondaryColor = SpawnEggItem.byId(vanilla)?.getColor(0) ?: 0xFFFFFF
                ForgeSpawnEggItem(entity, primaryColor, secondaryColor, Item.Properties())
            }
            entity
        }, attributes)
    }

    private fun texture(folder: String, name: String): ResourceLocation =
        ResourceLocation.fromNamespaceAndPath(FTMod.MODID, "textures/entity/mutation/$folder/$name.png")
}
