package com.y271727uy.farmerstales.integration.jade

import com.y271727uy.farmerstales.FTMod
import com.y271727uy.farmerstales.gameplay.animal_husbandry.shepherd.ShepherdDogState
import com.y271727uy.farmerstales.gameplay.animal_husbandry.shepherd.ShepherdDogTask
import net.minecraft.nbt.CompoundTag
import net.minecraft.network.chat.Component
import net.minecraft.resources.ResourceLocation
import net.minecraft.world.entity.animal.Wolf
import snownee.jade.api.EntityAccessor
import snownee.jade.api.IEntityComponentProvider
import snownee.jade.api.IServerDataProvider
import snownee.jade.api.ITooltip
import snownee.jade.api.config.IPluginConfig

/** Shows a tamed wolf's current Farmer's Tales job independently from vanilla pet data. */
object ShepherdDogTooltipProvider : IEntityComponentProvider, IServerDataProvider<EntityAccessor> {
    private const val TAMED_KEY = "tamed"
    private const val TASK_KEY = "task"
    private val uid = ResourceLocation.fromNamespaceAndPath(FTMod.MODID, "shepherd_dog")

    override fun getUid(): ResourceLocation = uid

    override fun appendServerData(tag: CompoundTag, accessor: EntityAccessor) {
        val wolf = accessor.entity as? Wolf ?: return
        if (wolf.isTame) {
            tag.putBoolean(TAMED_KEY, true)
            tag.putString(TASK_KEY, ShepherdDogState.task(wolf).serializedName)
        }
    }

    override fun appendTooltip(tooltip: ITooltip, accessor: EntityAccessor, config: IPluginConfig) {
        if (!accessor.serverData.getBoolean(TAMED_KEY)) {
            return
        }
        val task = ShepherdDogTask.fromSerializedName(accessor.serverData.getString(TASK_KEY))
        tooltip.add(
            Component.translatable(
                "tooltip.${FTMod.MODID}.shepherd_dog.task",
                Component.translatable("tooltip.${FTMod.MODID}.shepherd_dog.task.${task.serializedName}"),
            ),
        )
    }
}
