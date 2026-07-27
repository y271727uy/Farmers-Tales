package com.y271727uy.farmerstales.entity.registry

import com.tterrag.registrate.providers.RegistrateItemModelProvider
import com.tterrag.registrate.providers.RegistrateLangProvider
import com.y271727uy.farmerstales.entity.ai.MobAiBuilder
import com.y271727uy.farmerstales.entity.ai.PassiveAnimalAiDefinition
import net.minecraft.resources.ResourceLocation
import net.minecraft.core.registries.Registries
import net.minecraft.tags.TagKey
import net.minecraft.world.entity.EntityType
import net.minecraft.world.entity.Mob
import net.minecraft.world.entity.MobCategory
import net.minecraft.world.entity.SpawnPlacements
import net.minecraft.world.entity.TamableAnimal
import net.minecraft.world.entity.ai.attributes.AttributeSupplier
import net.minecraft.world.entity.player.Player
import net.minecraft.world.InteractionHand
import net.minecraft.world.InteractionResult
import net.minecraft.world.item.Item
import net.minecraft.world.item.ItemStack
import net.minecraft.world.item.crafting.Ingredient
import net.minecraft.world.level.ItemLike
import net.minecraft.world.level.levelgen.Heightmap
import net.minecraft.world.entity.animal.Animal
import net.minecraftforge.client.model.generators.ModelFile
import net.minecraftforge.common.ForgeSpawnEggItem
import net.minecraftforge.event.entity.EntityAttributeCreationEvent
import net.minecraftforge.eventbus.api.IEventBus
import net.minecraftforge.registries.DeferredRegister
import net.minecraftforge.registries.ForgeRegistries
import net.minecraftforge.registries.RegistryObject

/** Reusable backing registry for a mod's mob DSL. */
open class MobRegistry(private val modId: String) {
    private val entityTypes = DeferredRegister.create(ForgeRegistries.ENTITY_TYPES, modId)
    private val items = DeferredRegister.create(ForgeRegistries.ITEMS, modId)
    private val registeredMobs = mutableListOf<RegisteredMob<*>>()

    fun register(bus: IEventBus) {
        entityTypes.register(bus)
        items.register(bus)
    }

    fun registerAttributes(event: EntityAttributeCreationEvent) {
        registeredMobs.forEach { mob -> mob.attributeRegistration?.invoke(event) }
    }

    fun registerSpawnPlacements() {
        registeredMobs.forEach { mob -> mob.spawnPlacementRegistration?.invoke() }
    }

    fun generateLang(provider: RegistrateLangProvider) {
        registeredMobs.forEach { mob ->
            provider.add("entity.$modId.${mob.id}", mob.displayName)
            provider.add("item.$modId.${mob.spawnEggId}", mob.spawnEggName)
        }
    }

    fun generateItemModels(provider: RegistrateItemModelProvider) {
        val spawnEggTemplate = ModelFile.UncheckedModelFile(
            ResourceLocation.fromNamespaceAndPath("minecraft", "item/template_spawn_egg"),
        )
        registeredMobs.forEach { mob ->
            provider.getBuilder(mob.spawnEggId).parent(spawnEggTemplate)
        }
    }

    fun biomeSpawns(): List<BiomeSpawnEntry> = registeredMobs.mapNotNull { mob ->
        mob.naturalSpawn?.let { spawn -> BiomeSpawnEntry(mob.id, spawn) }
    }

    fun lootTables(): List<MobLootEntry> = registeredMobs.map { mob ->
        MobLootEntry(mob.type.get(), mob.loot)
    }

    protected fun <T : Mob> mob(
        id: String,
        factory: EntityType.EntityFactory<T>,
        configure: MobBuilder<T>.() -> Unit,
    ): RegisteredMob<T> {
        val definition = MobBuilder<T>().apply(configure)
        val type = entityTypes.register(id) {
            EntityType.Builder.of(factory, definition.category)
                .sized(definition.width, definition.height)
                .clientTrackingRange(8)
                .build("$modId:$id")
        }
        val spawnEgg = items.register<Item>("${id}_spawn_egg") {
            ForgeSpawnEggItem(
                { type.get() },
                definition.primaryEggColor,
                definition.secondaryEggColor,
                Item.Properties(),
            )
        }
        return RegisteredMob(
            id = id,
            type = type,
            spawnEgg = spawnEgg,
            displayName = definition.displayName,
            spawnEggName = definition.spawnEggName ?: "${definition.displayName} Spawn Egg",
            naturalSpawn = definition.naturalSpawn,
            breedingFood = definition.breedingFood,
            tamedFoodHealAmount = definition.tamedFoodHealAmount,
            passiveAnimalAi = definition.passiveAnimalAi,
            loot = definition.loot,
            attributeRegistration = definition.attributeFactory?.let { factory ->
                { event -> event.put(type.get(), factory().build()) }
            },
            spawnPlacementRegistration = definition.spawnPlacement?.let { placement ->
                {
                    SpawnPlacements.register(
                        type.get(),
                        placement.placementType,
                        placement.heightmapType,
                        placement.predicate,
                    )
                }
            },
        ).also(registeredMobs::add)
    }

    class MobBuilder<T : Mob> {
        var category: MobCategory = MobCategory.CREATURE
        var width: Float = 0.6F
            private set
        var height: Float = 0.6F
            private set
        private var entityName: String? = null
        var spawnEggName: String? = null
            private set
        var primaryEggColor: Int = 0xFFFFFF
            private set
        var secondaryEggColor: Int = 0xFFFFFF
            private set
        internal var attributeFactory: (() -> AttributeSupplier.Builder)? = null
        internal var spawnPlacement: SpawnPlacementDefinition<T>? = null
        internal var naturalSpawn: NaturalSpawnDefinition? = null
        internal var breedingFood: BreedingFoodDefinition? = null
        internal var tamedFoodHealAmount: Float? = null
        internal var passiveAnimalAi: PassiveAnimalAiDefinition? = null
        private var lootDefinition: MobLootDefinition? = null

        val displayName: String
            get() = requireNotNull(entityName) { "Each mob definition must declare lang(...)" }

        fun size(width: Float, height: Float) {
            require(width > 0.0F && height > 0.0F) { "Mob dimensions must be positive" }
            this.width = width
            this.height = height
        }

        fun attributes(factory: () -> AttributeSupplier.Builder) {
            attributeFactory = factory
        }

        fun spawnOnGround(
            predicate: SpawnPlacements.SpawnPredicate<T>,
            heightmapType: Heightmap.Types = Heightmap.Types.MOTION_BLOCKING_NO_LEAVES,
        ) {
            spawnPlacement = SpawnPlacementDefinition(SpawnPlacements.Type.ON_GROUND, heightmapType, predicate)
        }

        fun naturalSpawn(biomeTag: String, weight: Int, minCount: Int, maxCount: Int) {
            require(weight > 0 && minCount > 0 && maxCount >= minCount) { "Invalid natural spawn settings" }
            naturalSpawn = NaturalSpawnDefinition(ResourceLocation.parse(biomeTag), weight, minCount, maxCount)
        }

        fun breedingFood(vararg items: ItemLike) {
            require(items.isNotEmpty()) { "Breeding food cannot be empty" }
            breedingFood = BreedingFoodDefinition(items.map(ItemReference::Direct))
        }

        fun breedingFood(vararg itemIds: String) {
            require(itemIds.isNotEmpty()) { "Breeding food cannot be empty" }
            breedingFood = BreedingFoodDefinition(itemIds.map { ItemReference.Id(ResourceLocation.parse(it)) })
        }

        fun breedingFoodTag(vararg tagIds: String) {
            require(tagIds.isNotEmpty()) { "Breeding food tags cannot be empty" }
            breedingFood = BreedingFoodDefinition(tagIds.map { ItemReference.Tag(ResourceLocation.parse(it)) })
        }

        /** Lets an owner use breeding food to heal this mob after it has been tamed. */
        fun tamedFoodHealing(amount: Float) {
            require(amount > 0.0F) { "Tamed food healing must be positive" }
            tamedFoodHealAmount = amount
        }

        fun ai(configure: MobAiBuilder.() -> Unit) {
            passiveAnimalAi = MobAiBuilder().apply(configure).build()
        }

        fun loot(configure: MobLootBuilder.() -> Unit) {
            lootDefinition = MobLootBuilder().apply(configure).build()
        }

        val loot: MobLootDefinition
            get() = requireNotNull(lootDefinition) { "Each mob definition must declare loot { ... }" }

        fun lang(entityName: String, eggName: String? = null) {
            this.entityName = entityName
            spawnEggName = eggName
        }

        fun spawnEgg(primaryColor: Int, secondaryColor: Int) {
            this.primaryEggColor = primaryColor
            this.secondaryEggColor = secondaryColor
        }
    }

    data class RegisteredMob<T : Mob>(
        val id: String,
        val type: RegistryObject<EntityType<T>>,
        val spawnEgg: RegistryObject<Item>,
        val displayName: String,
        val spawnEggName: String,
        val naturalSpawn: NaturalSpawnDefinition?,
        val breedingFood: BreedingFoodDefinition?,
        val tamedFoodHealAmount: Float?,
        val passiveAnimalAi: PassiveAnimalAiDefinition?,
        val loot: MobLootDefinition,
        val attributeRegistration: ((EntityAttributeCreationEvent) -> Unit)?,
        val spawnPlacementRegistration: (() -> Unit)?,
    ) {
        val spawnEggId: String
            get() = "${id}_spawn_egg"

        fun isBreedingFood(stack: ItemStack): Boolean = breedingFood?.matches(stack) == true

        fun breedingIngredient(): Ingredient? = breedingFood?.ingredient

        /** Handles owner feeding before normal breeding interaction runs. */
        fun healTamedAnimal(animal: TamableAnimal, player: Player, hand: InteractionHand): InteractionResult? {
            val healing = tamedFoodHealAmount ?: return null
            val stack = player.getItemInHand(hand)
            if (!animal.isTame || !animal.isOwnedBy(player) || !isBreedingFood(stack) || animal.health >= animal.maxHealth) {
                return null
            }
            if (!animal.level().isClientSide) {
                if (!player.abilities.instabuild) stack.shrink(1)
                animal.heal(healing)
            }
            return InteractionResult.sidedSuccess(animal.level().isClientSide)
        }

        fun registerAnimalGoals(animal: Animal) {
            passiveAnimalAi?.install(animal, breedingIngredient())
        }
    }

    data class SpawnPlacementDefinition<T : Mob>(
        val placementType: SpawnPlacements.Type,
        val heightmapType: Heightmap.Types,
        val predicate: SpawnPlacements.SpawnPredicate<T>,
    )

    data class NaturalSpawnDefinition(
        val biomeTag: ResourceLocation,
        val weight: Int,
        val minCount: Int,
        val maxCount: Int,
    )

    data class MobLootDefinition(val drops: List<MobDropDefinition>)

    data class MobDropDefinition(
        val items: List<ItemReference>,
        val count: IntRange,
        val lootingBonus: IntRange?,
        val chance: Float,
    )

    sealed interface ItemReference {
        fun resolve(): ItemLike

        data class Direct(val item: ItemLike) : ItemReference {
            override fun resolve(): ItemLike = item
        }

        data class Id(val id: ResourceLocation) : ItemReference {
            override fun resolve(): ItemLike = requireNotNull(ForgeRegistries.ITEMS.getValue(id)) {
                "Unknown loot item '$id'"
            }
        }

        data class Tag(val id: ResourceLocation) : ItemReference {
            override fun resolve(): ItemLike = error("Item tags cannot be resolved as a single item")
        }
    }

    class MobLootBuilder {
        private val drops = mutableListOf<MobDropDefinition>()

        fun drop(
            item: ItemLike,
            count: IntRange = 1..1,
            lootingBonus: IntRange? = null,
            chance: Float = 1.0F,
        ) {
            require(!count.isEmpty() && count.first > 0) { "Drop count must be positive" }
            require(lootingBonus == null || (!lootingBonus.isEmpty() && lootingBonus.first >= 0)) {
                "Looting bonus must be non-negative"
            }
            require(chance in 0.0F..1.0F) { "Drop chance must be between zero and one" }
            drops += MobDropDefinition(listOf(ItemReference.Direct(item)), count, lootingBonus, chance)
        }

        fun drop(
            itemId: String,
            count: IntRange = 1..1,
            lootingBonus: IntRange? = null,
            chance: Float = 1.0F,
        ) {
            drop(ItemReference.Id(ResourceLocation.parse(itemId)), count, lootingBonus, chance)
        }

        private fun drop(
            item: ItemReference,
            count: IntRange,
            lootingBonus: IntRange?,
            chance: Float,
        ) {
            require(!count.isEmpty() && count.first > 0) { "Drop count must be positive" }
            require(lootingBonus == null || (!lootingBonus.isEmpty() && lootingBonus.first >= 0)) {
                "Looting bonus must be non-negative"
            }
            require(chance in 0.0F..1.0F) { "Drop chance must be between zero and one" }
            drops += MobDropDefinition(listOf(item), count, lootingBonus, chance)
        }

        fun randomDrop(
            vararg itemIds: String,
            count: IntRange = 1..1,
            lootingBonus: IntRange? = null,
            chance: Float = 1.0F,
        ) {
            require(itemIds.isNotEmpty()) { "Random drop choices cannot be empty" }
            require(!count.isEmpty() && count.first > 0) { "Drop count must be positive" }
            require(lootingBonus == null || (!lootingBonus.isEmpty() && lootingBonus.first >= 0)) {
                "Looting bonus must be non-negative"
            }
            require(chance in 0.0F..1.0F) { "Drop chance must be between zero and one" }
            drops += MobDropDefinition(
                itemIds.map { ItemReference.Id(ResourceLocation.parse(it)) },
                count,
                lootingBonus,
                chance,
            )
        }

        internal fun build(): MobLootDefinition = MobLootDefinition(drops.toList())
    }

    class BreedingFoodDefinition(private val items: List<ItemReference>) {
        val ingredient: Ingredient by lazy {
            Ingredient.merge(items.map { item ->
                when (item) {
                    is ItemReference.Tag -> Ingredient.of(TagKey.create(Registries.ITEM, item.id))
                    else -> Ingredient.of(item.resolve())
                }
            })
        }

        fun matches(stack: ItemStack): Boolean = ingredient.test(stack)
    }

    data class BiomeSpawnEntry(
        val entityId: String,
        val spawn: NaturalSpawnDefinition,
    )

    data class MobLootEntry(
        val type: EntityType<*>,
        val loot: MobLootDefinition,
    )
}
