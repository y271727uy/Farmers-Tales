package com.y271727uy.farmerstales.integration.jade

import com.y271727uy.farmerstales.FTMod
import com.y271727uy.farmerstales.gameplay.season_breeding.AnimalBreedingTooltipProvider
import com.y271727uy.farmerstales.gameplay.fish_group.entity.OceanFishPoolEntity
import com.y271727uy.farmerstales.gameplay.fish_group.entity.RiverFishPoolEntity
import com.y271727uy.farmerstales.gameplay.tree.block.TreeStumpBlock
import snownee.jade.api.IWailaCommonRegistration
import snownee.jade.api.IWailaPlugin
import snownee.jade.api.IWailaClientRegistration
import snownee.jade.api.WailaPlugin

object JadeIntegration {
    fun init() {
        FTMod.LOGGER.debug("Jade compatibility enabled")
    }
}

@WailaPlugin
class FarmerTalesJadePlugin : IWailaPlugin {
    override fun register(registration: IWailaCommonRegistration) {
        registration.registerEntityDataProvider(FishPoolEntityProvider, RiverFishPoolEntity::class.java)
        registration.registerEntityDataProvider(FishPoolEntityProvider, OceanFishPoolEntity::class.java)
    }

    override fun registerClient(registration: IWailaClientRegistration) {
        registration.registerBlockComponent(TreeStumpTooltipProvider, TreeStumpBlock::class.java)
        registration.registerEntityComponent(
            AnimalBreedingTooltipProvider,
            net.minecraft.world.entity.animal.Animal::class.java
        )
        registration.registerEntityComponent(FishPoolEntityProvider, RiverFishPoolEntity::class.java)
        registration.registerEntityComponent(FishPoolEntityProvider, OceanFishPoolEntity::class.java)
    }
}
