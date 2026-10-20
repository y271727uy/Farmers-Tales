package com.y271727uy.farmerstales.gameplay.animal_husbandry.shepherd

import net.minecraft.nbt.CompoundTag
import net.minecraft.nbt.Tag
import net.minecraft.world.entity.animal.Wolf

enum class ShepherdDogTask(val serializedName: String) {
    IDLE("idle"),
    HERDING("herding"),
    RETURNING("returning");

    companion object {
        fun fromSerializedName(value: String): ShepherdDogTask =
            entries.firstOrNull { it.serializedName == value } ?: IDLE
    }
}

/** Stores a wolf's current job separately from Minecraft's native owner and sitting state. */
object ShepherdDogState {
    private const val DATA_KEY = "farmerstales.shepherd_dog"
    private const val TASK_KEY = "task"

    fun task(wolf: Wolf): ShepherdDogTask {
        val data = wolf.persistentData
        if (!data.contains(DATA_KEY, Tag.TAG_COMPOUND.toInt())) {
            return ShepherdDogTask.IDLE
        }
        return ShepherdDogTask.fromSerializedName(data.getCompound(DATA_KEY).getString(TASK_KEY))
    }

    fun setTask(wolf: Wolf, task: ShepherdDogTask) {
        val data = wolf.persistentData
        val dogData = if (data.contains(DATA_KEY, Tag.TAG_COMPOUND.toInt())) {
            data.getCompound(DATA_KEY)
        } else {
            CompoundTag().also { data.put(DATA_KEY, it) }
        }
        dogData.putString(TASK_KEY, task.serializedName)
    }
}
