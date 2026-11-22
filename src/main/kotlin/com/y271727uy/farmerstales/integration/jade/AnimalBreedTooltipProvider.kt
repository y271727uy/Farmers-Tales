package com.y271727uy.farmerstales.integration.jade

import com.y271727uy.farmerstales.FTMod
import com.y271727uy.farmerstales.entity.AnimalMutationEntities
import com.y271727uy.farmerstales.gameplay.animal_variants.AnimalAppearanceVariants
import net.minecraft.client.Minecraft
import net.minecraft.core.registries.BuiltInRegistries
import net.minecraft.locale.Language
import net.minecraft.network.chat.Component
import net.minecraft.resources.ResourceLocation
import net.minecraft.world.entity.EntityType
import net.minecraft.world.entity.animal.Animal
import net.minecraft.world.entity.animal.Cat
import net.minecraft.world.entity.animal.Fox
import net.minecraft.world.entity.animal.MushroomCow
import net.minecraft.world.entity.animal.Parrot
import net.minecraft.world.entity.animal.Rabbit
import net.minecraft.world.entity.animal.Sheep
import net.minecraft.world.entity.animal.axolotl.Axolotl
import net.minecraft.world.entity.animal.frog.Frog
import net.minecraft.world.entity.animal.horse.Horse
import net.minecraft.world.entity.animal.horse.Llama
import net.minecraft.world.item.DyeColor
import snownee.jade.api.EntityAccessor
import snownee.jade.api.IEntityComponentProvider
import snownee.jade.api.ITooltip
import snownee.jade.api.config.IPluginConfig

object AnimalBreedTooltipProvider : IEntityComponentProvider {
    private const val TOOLTIP_KEY = "tooltip.${FTMod.MODID}.jade.animal_breed"
    private val uid = ResourceLocation.fromNamespaceAndPath(FTMod.MODID, "animal_breed")

    override fun getUid(): ResourceLocation = uid

    override fun appendTooltip(tooltip: ITooltip, accessor: EntityAccessor, config: IPluginConfig) {
        val animal = accessor.entity as? Animal ?: return
        val mutation = AnimalMutationEntities.appearance(animal.type)
        if (mutation != null) {
            tooltip.add(Component.translatable(TOOLTIP_KEY, animal.type.description))
            val series = Component.translatable("$TOOLTIP_KEY.series.${mutation.series}")
            tooltip.add(Component.translatable("$TOOLTIP_KEY.series", series))
            if (animal is Sheep) appendWoolColor(tooltip, woolColor(animal))
            return
        }
        if (animal is Sheep) {
            appendSheepTooltip(tooltip, animal)
            return
        }
        val type = EntityType.getKey(animal.type)
        if (type.namespace == "untitledduckmod" && (type.path == "duck" || type.path == "goose")) {
            waterfowlTooltip(animal)?.let { tooltip.add(it) }
            return
        }
        val breed = breedName(animal) ?: return
        tooltip.add(Component.translatable(TOOLTIP_KEY, breed))
    }

    private fun appendSheepTooltip(tooltip: ITooltip, sheep: Sheep) {
        val woolColor = woolColor(sheep)
        val coat = AnimalAppearanceVariants.getSheepCoat(sheep)
        val breed = when {
            coat != null -> translatedBreed("sheep.$coat")
            sheep.type == EntityType.SHEEP && sheep.color == DyeColor.WHITE -> translatedBreed("sheep.white")
            else -> null
        }
        if (breed == null) {
            val wool = Component.translatable("$TOOLTIP_KEY.sheep.wool", woolColor)
            tooltip.add(Component.translatable(TOOLTIP_KEY, wool))
            return
        }
        tooltip.add(Component.translatable(TOOLTIP_KEY, breed))
        appendWoolColor(tooltip, woolColor)
    }

    private fun woolColor(sheep: Sheep): Component =
        Component.translatable("color.minecraft.${sheep.color.serializedName}")

    private fun appendWoolColor(tooltip: ITooltip, woolColor: Component) {
        tooltip.add(Component.translatable("$TOOLTIP_KEY.sheep.wool_color", woolColor))
    }

    private fun waterfowlTooltip(animal: Animal): Component? {
        // Resolve the same texture used for rendering, including age and name tag overrides.
        val renderer = Minecraft.getInstance().entityRenderDispatcher.getRenderer(animal) ?: return null
        val texture = renderer.getTextureLocation(animal)
        if (texture.namespace != "untitledduckmod") return null

        val skin = texture.path.removePrefix("textures/entity/").removeSuffix(".png")
        val name = translatedBreed("untitledduckmod.$skin") ?: return null
        val label = when (skin) {
            "duckling", "gosling", "ping_goose", "sus_goose", "untitled_goose" -> "$TOOLTIP_KEY.appearance"
            else -> TOOLTIP_KEY
        }
        return Component.translatable(label, name)
    }

    private fun breedName(animal: Animal): Component? = when (animal) {
        is Horse -> horseBreed(animal)
        is Llama -> translatedBreed("llama.${animal.variant.serializedName}")
        is MushroomCow -> translatedBreed("mooshroom.${animal.variant.serializedName}")
        is Fox -> translatedBreed("fox.${animal.variant.serializedName}")
        is Frog -> vanillaBreed("frog", BuiltInRegistries.FROG_VARIANT.getKey(animal.variant))
        is Axolotl -> translatedBreed("axolotl.${animal.variant.serializedName}")
        is Parrot -> translatedBreed("parrot.${animal.variant.serializedName}")
        is Rabbit -> rabbitBreed(animal)
        else -> when (animal.type) {
            EntityType.COW, EntityType.PIG, EntityType.CHICKEN ->
                farmBreed(animal.type, AnimalAppearanceVariants.getVariant(animal))
            EntityType.WOLF -> translatedBreed("wolf.${AnimalAppearanceVariants.getVariant(animal).ifEmpty { "pale" }}")
            EntityType.CAT -> catBreed(animal as Cat, AnimalAppearanceVariants.getVariant(animal))
            else -> null
        }
    }

    private fun horseBreed(horse: Horse): Component? {
        val coat = translatedBreed("horse.coat.${horse.variant.serializedName}") ?: return null
        val markings = translatedBreed("horse.markings.${horse.markings.name.lowercase()}") ?: return null
        return Component.translatable("$TOOLTIP_KEY.horse.coat_and_markings", coat, markings)
    }

    private fun farmBreed(type: EntityType<*>, variant: String): Component? {
        val id = variant.ifEmpty { "classic" }
        return translatedBreed("${EntityType.getKey(type).path}.$id") ?: translatedBreed("farm.$id")
    }

    private fun rabbitBreed(rabbit: Rabbit): Component? {
        if (AnimalAppearanceVariants.hasToastAppearance(rabbit)) return null
        val coat = AnimalAppearanceVariants.getRabbitCoat(rabbit) ?: rabbit.variant.serializedName
        return translatedBreed("rabbit.$coat")
    }

    private fun catBreed(cat: Cat, variant: String): Component? {
        if (variant.isNotEmpty()) return translatedBreed("cat.$variant")
        return vanillaBreed("cat", BuiltInRegistries.CAT_VARIANT.getKey(cat.variant))
    }

    private fun vanillaBreed(animal: String, variant: ResourceLocation?): Component? {
        if (variant?.namespace != "minecraft") return null
        return translatedBreed("$animal.${variant.path}")
    }

    private fun translatedBreed(suffix: String): Component? {
        val key = "$TOOLTIP_KEY.$suffix"
        return if (Language.getInstance().has(key)) Component.translatable(key) else null
    }
}
