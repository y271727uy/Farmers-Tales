package com.y271727uy.farmerstales.integration.untitledduckmod

import com.y271727uy.farmerstales.gameplay.animal_husbandry.AnimalHusbandryState
import com.y271727uy.farmerstales.gameplay.animal_husbandry.AnimalHusbandryStatus
import com.y271727uy.farmerstales.gameplay.animal_husbandry.AnimalMetabolismScheduler
import net.minecraft.resources.ResourceLocation
import net.minecraft.world.entity.TamableAnimal
import net.minecraft.world.entity.animal.Animal
import net.minecraftforge.registries.ForgeRegistries

object UntitledDuckIntegration {
    const val DUCK = "untitledduckmod:duck"
    const val GOOSE = "untitledduckmod:goose"

    private val waterfowlIds = setOf(ResourceLocation.tryParse(DUCK), ResourceLocation.tryParse(GOOSE))

    fun waterfowl(animal: Animal): TamableAnimal? =
        (animal as? TamableAnimal)?.takeIf { ForgeRegistries.ENTITY_TYPES.getKey(it.type) in waterfowlIds }

    @JvmStatic
    fun isWildWaterfowl(animal: Animal): Boolean =
        waterfowl(animal) != null && AnimalHusbandryState.status(animal) != AnimalHusbandryStatus.DOMESTICATED

    @JvmStatic
    fun onNativeTame(animal: TamableAnimal) {
        if (animal.level().isClientSide || waterfowl(animal) == null ||
            AnimalHusbandryState.status(animal) == AnimalHusbandryStatus.DOMESTICATED
        ) return
        AnimalHusbandryState.markDomesticated(animal)
        AnimalMetabolismScheduler.track(animal)
    }
}
