package com.y271727uy.farmerstales.entity.mobs

import com.y271727uy.farmerstales.FTMod
import net.minecraft.server.level.ServerLevel
import net.minecraft.world.level.block.CropBlock
import net.minecraft.world.phys.AABB
import net.minecraftforge.event.level.BlockEvent
import net.minecraftforge.eventbus.api.SubscribeEvent
import net.minecraftforge.fml.common.Mod
import net.minecraftforge.registries.ForgeRegistries

/** Lets nearby wild rats occasionally eat a stage of a crop as it grows. */
@Mod.EventBusSubscriber(modid = FTMod.MODID, bus = Mod.EventBusSubscriber.Bus.FORGE)
object RatCropDamageEvents {
    private const val DAMAGE_CHANCE = 0.25F
    private const val RAT_RANGE = 2.5

    @JvmStatic
    @SubscribeEvent
    fun onCropGrow(event: BlockEvent.CropGrowEvent.Post) {
        val level = event.level as? ServerLevel ?: return
        if (event.originalState == event.state || level.random.nextFloat() >= DAMAGE_CHANCE) return

        val crop = event.state.block as? CropBlock ?: return
        val age = crop.getAge(event.state)
        if (age <= 0) return
        val rat = findWildRatNearby(level, event.pos)
        if (rat == null) return

        level.setBlock(event.pos, crop.getStateForAge(age - 1), 3)
        FTMod.LOGGER.debug(
            "Wild rat {} at {} regressed {} at {} from age {} to {}",
            rat.uuid,
            rat.blockPosition(),
            ForgeRegistries.BLOCKS.getKey(crop),
            event.pos,
            age,
            age - 1,
        )
    }

    private fun findWildRatNearby(level: ServerLevel, pos: net.minecraft.core.BlockPos): RatEntity? =
        level.getEntitiesOfClass(RatEntity::class.java, AABB(pos).inflate(RAT_RANGE)).firstOrNull { !it.isTame }
}
