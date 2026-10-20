package com.y271727uy.farmerstales.gameplay.animal_variants

import com.y271727uy.farmerstales.FTMod
import com.y271727uy.farmerstales.variant.AnimalAppearanceVariantHolder
import net.minecraft.core.BlockPos
import net.minecraft.core.registries.Registries
import net.minecraft.resources.ResourceLocation
import net.minecraft.tags.TagKey
import net.minecraft.util.RandomSource
import net.minecraft.world.entity.EntityType
import net.minecraft.world.entity.Mob
import net.minecraft.world.entity.MobSpawnType
import net.minecraft.world.entity.animal.Animal
import net.minecraft.world.entity.animal.Cat
import net.minecraft.world.entity.animal.Chicken
import net.minecraft.world.entity.animal.Cow
import net.minecraft.world.entity.animal.Pig
import net.minecraft.world.entity.animal.Wolf
import net.minecraft.world.level.ServerLevelAccessor
import net.minecraft.server.level.ServerLevel
import net.minecraft.world.level.biome.Biome
import net.minecraftforge.event.entity.living.BabyEntitySpawnEvent
import net.minecraftforge.event.entity.living.MobSpawnEvent
import net.minecraftforge.event.entity.EntityJoinLevelEvent
import net.minecraftforge.eventbus.api.SubscribeEvent
import net.minecraftforge.fml.common.Mod

/** Recreates the biome-based cosmetic animal variants introduced in 1.20.5 and 1.21.5. */
@Mod.EventBusSubscriber(modid = FTMod.MODID, bus = Mod.EventBusSubscriber.Bus.FORGE)
object AnimalAppearanceVariants {
    private const val PALE_WOLF = "pale"
    private const val TEMPERATE = "temperate"
    private const val COLD = "cold"
    private const val WARM = "warm"
    private val quarkCowVariants = List(4) { "quark_${it + 1}" }
    private val quarkPigVariants = List(3) { "quark_${it + 1}" }
    private val quarkChickenVariants = List(6) { "quark_${it + 1}" }
    private val mobVariantsPigVariants = listOf(
        "mobvariants_mottled", "mobvariants_piebald", "mobvariants_pink_footed",
        "mobvariants_sooty", "mobvariants_spotted",
    )
    private val mobVariantsCatVariants = listOf(
        "mobvariants_anita_hart", "mobvariants_doug", "mobvariants_gray_tabby",
        "mobvariants_handsome", "mobvariants_tortoiseshell",
    )
    // An empty entry preserves the Cat entity's own vanilla variant selection.
    // There are 11 vanilla cat variants and five MobVariants additions.
    private val catVariants = List(11) { "" } + mobVariantsCatVariants
    private val mobVariantsDogVariants = listOf(
        "mobvariants_basenji", "mobvariants_french_bulldog", "mobvariants_german_shepherd",
        "mobvariants_golden_retriever", "mobvariants_husky", "mobvariants_jupiter",
    )

    private val coldFarmAnimalBiomes = biomeTag("spawns_cold_variant_farm_animals")
    private val warmFarmAnimalBiomes = biomeTag("spawns_warm_variant_farm_animals")
    private val vanillaWolfVariants = listOf(
        "pale", "ashen", "black", "chestnut", "rusty", "snowy", "spotted", "striped", "woods",
    )
    private val wolfVariants = vanillaWolfVariants + mobVariantsDogVariants
    private val wolfBiomeVariants = listOf(
        "ashen" to biomeTag("spawns_ashen_wolves"),
        "black" to biomeTag("spawns_black_wolves"),
        "chestnut" to biomeTag("spawns_chestnut_wolves"),
        "rusty" to biomeTag("spawns_rusty_wolves"),
        "snowy" to biomeTag("spawns_snowy_wolves"),
        "spotted" to biomeTag("spawns_spotted_wolves"),
        "striped" to biomeTag("spawns_striped_wolves"),
        "woods" to biomeTag("spawns_woods_wolves"),
    )

    @JvmStatic
    @SubscribeEvent
    fun onFinalizeSpawn(event: MobSpawnEvent.FinalizeSpawn) {
        val mob = event.entity
        if (!isSupported(mob)) return
        if ((mob as? AnimalAppearanceVariantHolder)?.`farmerstales$hasAppearanceVariant`() == true) return

        val variant = if (event.spawnType == MobSpawnType.SPAWN_EGG) {
            randomVariant(mob, mob.random)
        } else {
            variantForBiome(mob, event.level, event.x, event.y, event.z)
        }
        setVariant(mob, variant)
    }

    /** Covers loaded/command-spawned entities that do not pass FinalizeSpawn. */
    @JvmStatic
    @SubscribeEvent
    fun onEntityJoinLevel(event: EntityJoinLevelEvent) {
        if (event.level.isClientSide) return
        val mob = event.entity as? Mob ?: return
        if (!isSupported(mob) || (mob as? AnimalAppearanceVariantHolder)?.`farmerstales$hasAppearanceVariant`() == true) return

        // Carry On can add a freshly created entity before it has finished loading
        // the saved NBT. Defer the fallback assignment so a persisted variant wins.
        // Cats use an empty value to intentionally preserve their vanilla skin.
        if (mob is Cat) return
        val serverLevel = event.level as? ServerLevel ?: return
        serverLevel.server.execute {
            if (mob.isRemoved || mob.level() !== serverLevel ||
                (mob as? AnimalAppearanceVariantHolder)?.`farmerstales$hasAppearanceVariant`() == true
            ) return@execute
            val position = mob.blockPosition()
            setVariant(
                mob,
                variantForBiome(
                    mob,
                    serverLevel,
                    position.x.toDouble(),
                    position.y.toDouble(),
                    position.z.toDouble(),
                ),
            )
        }
    }

    @JvmStatic
    @SubscribeEvent
    fun onBabyEntitySpawn(event: BabyEntitySpawnEvent) {
        val child = event.child as? Animal ?: return
        if (!isSupported(child)) return

        val parentVariants = listOfNotNull(
            (event.parentA as? Mob)?.let(::getVariant)?.takeIf { it.isNotEmpty() },
            (event.parentB as? Mob)?.let(::getVariant)?.takeIf { it.isNotEmpty() },
        )
        val variant = parentVariants.randomOrNull(child.random) ?: randomVariant(child, child.random)
        setVariant(child, variant)
    }

    @JvmStatic
    fun getVariant(mob: Mob): String = (mob as? AnimalAppearanceVariantHolder)?.`farmerstales$getAppearanceVariant`().orEmpty()

    /** Quark and MobVariants pig textures use the pre-1.21.5 cow/pig UV layout. */
    @JvmStatic
    fun usesLegacyModel(variant: String): Boolean = variant.startsWith("quark_") || variant.startsWith("mobvariants_")

    private fun setVariant(mob: Mob, variant: String) {
        (mob as? AnimalAppearanceVariantHolder)?.`farmerstales$setAppearanceVariant`(variant)
    }

    private fun variantForBiome(mob: Mob, level: ServerLevelAccessor, x: Double, y: Double, z: Double): String {
        val biome = level.getBiome(BlockPos.containing(x, y, z))
        return when (mob) {
            is Wolf -> wolfBiomeVariants.firstOrNull { (_, tag) -> biome.`is`(tag) }?.first
                ?: (listOf(PALE_WOLF) + mobVariantsDogVariants).randomOrNull(mob.random)!!
            is Cat -> catVariants.randomOrNull(mob.random)!!
            is Cow, is Pig, is Chicken -> when {
                biome.`is`(coldFarmAnimalBiomes) -> COLD
                biome.`is`(warmFarmAnimalBiomes) -> WARM
                else -> temperateVariants(mob).randomOrNull(mob.random) ?: TEMPERATE
            }
            else -> ""
        }
    }

    private fun randomVariant(mob: Mob, random: RandomSource): String = when (mob) {
        is Wolf -> wolfVariants[random.nextInt(wolfVariants.size)]
        is Cat -> catVariants[random.nextInt(catVariants.size)]
        is Cow, is Pig, is Chicken -> allFarmAnimalVariants(mob).let { variants -> variants[random.nextInt(variants.size)] }
        else -> ""
    }

    private fun temperateVariants(mob: Mob): List<String> = when (mob) {
        is Cow -> listOf(TEMPERATE) + quarkCowVariants
        is Pig -> listOf(TEMPERATE) + quarkPigVariants + mobVariantsPigVariants
        is Chicken -> listOf(TEMPERATE) + quarkChickenVariants
        else -> emptyList()
    }

    private fun allFarmAnimalVariants(mob: Mob): List<String> = listOf(COLD, TEMPERATE, WARM) + temperateVariants(mob).drop(1)

    private fun isSupported(mob: Mob): Boolean = mob is Wolf || mob is Cat || mob is Cow || mob is Pig || mob is Chicken

    private fun biomeTag(path: String): TagKey<Biome> = TagKey.create(
        Registries.BIOME,
        ResourceLocation.fromNamespaceAndPath(FTMod.MODID, path),
    )
}

private fun <T> List<T>.randomOrNull(random: RandomSource): T? = if (isEmpty()) null else this[random.nextInt(size)]
