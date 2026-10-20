package com.y271727uy.farmerstales.gameplay.tropical_climate

import com.y271727uy.farmerstales.FTMod
import com.y271727uy.farmerstales.config.Config
import net.minecraft.server.level.ServerLevel
import net.minecraft.world.level.block.Blocks
import net.minecraft.world.level.block.FarmBlock
import net.minecraftforge.event.level.BlockEvent
import net.minecraftforge.eventbus.api.Event
import net.minecraftforge.eventbus.api.SubscribeEvent
import net.minecraftforge.fml.common.Mod

/** Reduces only natural crop growth on dry vanilla farmland during a tropical drought. */
@Mod.EventBusSubscriber(modid = FTMod.MODID, bus = Mod.EventBusSubscriber.Bus.FORGE)
object TropicalCropGrowthEvents {
    @JvmStatic
    @SubscribeEvent
    fun onCropGrow(event: BlockEvent.CropGrowEvent.Pre) {
        val level = event.level as? ServerLevel ?: return
        if (!TropicalClimateEvents.isPatternActiveAt(level, event.pos, TropicalClimatePattern.DROUGHT)) {
            return
        }

        val farmland = level.getBlockState(event.pos.below())
        if (!farmland.`is`(Blocks.FARMLAND) || farmland.getValue(FarmBlock.MOISTURE) != 0) {
            return
        }

        if (level.random.nextDouble() < Config.tropicalDroughtDryFarmlandGrowthPenalty) {
            event.result = Event.Result.DENY
        }
    }
}
