package com.y271727uy.farmerstales.gameplay.animal_husbandry

import net.minecraft.nbt.CompoundTag
import net.minecraft.nbt.Tag
import net.minecraft.world.entity.animal.Animal

enum class AnimalHusbandryStatus(val serializedName: String) {
    UNMANAGED("unmanaged"),
    WILD("wild"),
    DOMESTICATED("domesticated");

    companion object {
        fun fromSerializedName(value: String): AnimalHusbandryStatus =
            entries.firstOrNull { it.serializedName == value } ?: UNMANAGED
    }
}

/** Stores the feature's compact per-animal state in Forge persistent entity data. */
object AnimalHusbandryState {
    private const val DATA_KEY = "farmerstales.animal_husbandry"
    private const val STATUS_KEY = "status"

    fun status(animal: Animal): AnimalHusbandryStatus {
        val data = animal.persistentData
        if (!data.contains(DATA_KEY, Tag.TAG_COMPOUND.toInt())) {
            return AnimalHusbandryStatus.UNMANAGED
        }
        return AnimalHusbandryStatus.fromSerializedName(data.getCompound(DATA_KEY).getString(STATUS_KEY))
    }

    fun markWild(animal: Animal) = setStatus(animal, AnimalHusbandryStatus.WILD)

    fun markDomesticated(animal: Animal) = setStatus(animal, AnimalHusbandryStatus.DOMESTICATED)

    private fun setStatus(animal: Animal, status: AnimalHusbandryStatus) {
        val data = animal.persistentData
        val husbandryData = if (data.contains(DATA_KEY, Tag.TAG_COMPOUND.toInt())) {
            data.getCompound(DATA_KEY)
        } else {
            CompoundTag().also { data.put(DATA_KEY, it) }
        }
        husbandryData.putString(STATUS_KEY, status.serializedName)
    }
}
