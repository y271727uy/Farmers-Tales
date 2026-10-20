package com.y271727uy.farmerstales.gameplay.special_event.fertile_moon

import com.y271727uy.farmerstales.FTMod
import com.y271727uy.farmerstales.gameplay.special_event.common.SpecialEventGrowth
import net.minecraft.server.level.ServerLevel
import net.minecraftforge.event.level.BlockEvent
import net.minecraftforge.eventbus.api.SubscribeEvent
import net.minecraftforge.fml.common.Mod

/** Gives naturally ticking crops one additional growth attempt during bonus-growth events. */
@Mod.EventBusSubscriber(modid = FTMod.MODID, bus = Mod.EventBusSubscriber.Bus.FORGE)
object FertileMoonGrowthEvents {
    private val applyingBonus = ThreadLocal.withInitial { false }

    @JvmStatic
    @SubscribeEvent
    fun onCropGrowPost(event: BlockEvent.CropGrowEvent.Post) {
        val level = event.level as? ServerLevel ?: return
        if (applyingBonus.get() || !SpecialEventGrowth.isBonusGrowthActive(level, event.pos)) return
        if (event.originalState == event.state || !level.getBlockState(event.pos).isRandomlyTicking) return

        applyingBonus.set(true)
        try {
            level.getBlockState(event.pos).randomTick(level, event.pos, level.random)
        } finally {
            applyingBonus.set(false)
        }
    }
}
