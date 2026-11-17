package com.y271727uy.farmerstales.integration.jade

import com.y271727uy.farmerstales.FTMod
import com.y271727uy.farmerstales.gameplay.animal_husbandry.AnimalCondition
import com.y271727uy.farmerstales.gameplay.animal_husbandry.AnimalConditions
import com.y271727uy.farmerstales.gameplay.animal_husbandry.AnimalEmotion
import com.y271727uy.farmerstales.gameplay.animal_husbandry.AnimalEmotionState
import com.y271727uy.farmerstales.gameplay.animal_husbandry.AnimalHusbandryRules
import com.y271727uy.farmerstales.gameplay.animal_husbandry.AnimalMetabolism
import com.y271727uy.farmerstales.gameplay.animal_husbandry.AnimalHusbandryState
import com.y271727uy.farmerstales.gameplay.animal_husbandry.AnimalHusbandryStatus
import com.y271727uy.farmerstales.gameplay.animal_husbandry.AnimalHarvestEvents
import net.minecraft.nbt.CompoundTag
import net.minecraft.network.chat.Component
import net.minecraft.resources.ResourceLocation
import net.minecraft.world.entity.animal.Animal
import net.minecraft.world.entity.animal.Cow
import snownee.jade.api.EntityAccessor
import snownee.jade.api.IEntityComponentProvider
import snownee.jade.api.IServerDataProvider
import snownee.jade.api.ITooltip
import snownee.jade.api.config.IPluginConfig

/** Sends husbandry state only when Jade is inspecting a supported animal. */
object AnimalHusbandryTooltipProvider : IEntityComponentProvider, IServerDataProvider<EntityAccessor> {
    private const val STATUS_KEY = "status"
    private const val HUNGER_KEY = "hunger"
    private const val AFFECTION_KEY = "affection"
    private const val WEIGHT_KEY = "weight"
    private const val MAX_HUNGER_KEY = "maxHunger"
    private const val MAX_WEIGHT_KEY = "maxWeight"
    private const val MILKING_COOLDOWN_KEY = "milkingCooldown"
    private const val MILKING_READY_KEY = "milkingReady"
    private const val EMOTION_KEY = "emotion"
    private const val CONDITIONS_KEY = "conditions"
    private val uid = ResourceLocation.fromNamespaceAndPath(FTMod.MODID, "animal_husbandry")

    override fun getUid(): ResourceLocation = uid

    override fun appendServerData(tag: CompoundTag, accessor: EntityAccessor) {
        val animal = accessor.entity as? Animal ?: return
        if (!AnimalHusbandryRules.isSupported(animal)) {
            return
        }

        val status = AnimalHusbandryState.status(animal)
        if (status != AnimalHusbandryStatus.UNMANAGED) {
            tag.putString(STATUS_KEY, status.serializedName)
        }
        val vitals = AnimalMetabolism.inspect(animal)
        val emotion = AnimalEmotion.evaluate(animal, vitals)
        emotion?.let { tag.putString(EMOTION_KEY, it.serializedName) }
        AnimalConditions.evaluate(animal, vitals, emotion).takeIf { it.isNotEmpty() }?.let { conditions ->
            tag.putInt(CONDITIONS_KEY, conditions.fold(0) { bits, condition -> bits or condition.bit })
        }
        vitals?.let { vitals ->
            val plan = AnimalHusbandryRules.metabolismFor(animal) ?: return@let
            tag.putInt(HUNGER_KEY, vitals.hunger)
            tag.putInt(AFFECTION_KEY, vitals.affection)
            tag.putInt(WEIGHT_KEY, vitals.weight)
            tag.putInt(MAX_HUNGER_KEY, plan.maxHunger)
            tag.putInt(MAX_WEIGHT_KEY, plan.maxWeight)
        }
        if (animal is Cow && AnimalMetabolism.isManaged(animal)) {
            val cooldown = AnimalHarvestEvents.milkingCooldownRemaining(animal)
            tag.putLong(MILKING_COOLDOWN_KEY, cooldown)
            tag.putBoolean(
                MILKING_READY_KEY,
                cooldown == 0L && AnimalMetabolism.isFullyFed(animal),
            )
        }
    }

    override fun appendTooltip(tooltip: ITooltip, accessor: EntityAccessor, config: IPluginConfig) {
        when (accessor.serverData.getString(STATUS_KEY)) {
            AnimalHusbandryStatus.WILD.serializedName ->
                tooltip.add(Component.translatable("tooltip.${FTMod.MODID}.animal_husbandry.wild"))
            AnimalHusbandryStatus.DOMESTICATED.serializedName ->
                tooltip.add(Component.translatable("tooltip.${FTMod.MODID}.animal_husbandry.domesticated"))
        }
        AnimalEmotionState.fromSerializedName(accessor.serverData.getString(EMOTION_KEY))?.let { emotion ->
            tooltip.add(
                Component.translatable(
                    "tooltip.${FTMod.MODID}.animal_husbandry.emotion",
                    Component.translatable(emotion.translationKey),
                ),
            )
        }
        AnimalCondition.fromBits(accessor.serverData.getInt(CONDITIONS_KEY))
            .take(AnimalConditions.MAX_DISPLAYED)
            .takeIf { it.isNotEmpty() }?.let { conditions ->
            val separator = Component.translatable("tooltip.${FTMod.MODID}.animal_husbandry.condition.separator")
            val joined = Component.empty()
            conditions.forEachIndexed { index, condition ->
                if (index > 0) joined.append(separator)
                joined.append(Component.translatable(condition.translationKey))
            }
            tooltip.add(Component.translatable("tooltip.${FTMod.MODID}.animal_husbandry.condition", joined))
        }
        if (accessor.serverData.contains(HUNGER_KEY)) {
            tooltip.add(Component.translatable("tooltip.${FTMod.MODID}.animal_husbandry.hunger", accessor.serverData.getInt(HUNGER_KEY), accessor.serverData.getInt(MAX_HUNGER_KEY)))
            tooltip.add(Component.translatable("tooltip.${FTMod.MODID}.animal_husbandry.affection", accessor.serverData.getInt(AFFECTION_KEY)))
            tooltip.add(Component.translatable("tooltip.${FTMod.MODID}.animal_husbandry.weight", accessor.serverData.getInt(WEIGHT_KEY), accessor.serverData.getInt(MAX_WEIGHT_KEY)))
        }
        if (accessor.serverData.contains(MILKING_COOLDOWN_KEY)) {
            val cooldownSeconds = (accessor.serverData.getLong(MILKING_COOLDOWN_KEY) + 19L) / 20L
            when {
                cooldownSeconds > 0L -> tooltip.add(
                    Component.translatable(
                        "tooltip.${FTMod.MODID}.animal_husbandry.milking_cooldown",
                        cooldownSeconds,
                    ),
                )
                accessor.serverData.getBoolean(MILKING_READY_KEY) -> tooltip.add(
                    Component.translatable("tooltip.${FTMod.MODID}.animal_husbandry.milking_ready"),
                )
                else -> tooltip.add(
                    Component.translatable("tooltip.${FTMod.MODID}.animal_husbandry.milking_needs_food"),
                )
            }
        }
    }
}
