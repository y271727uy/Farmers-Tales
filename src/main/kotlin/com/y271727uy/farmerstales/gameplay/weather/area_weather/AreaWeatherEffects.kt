package com.y271727uy.farmerstales.gameplay.weather.area_weather

import com.y271727uy.farmerstales.FTMod
import com.y271727uy.farmerstales.gameplay.gene_crop.block.entity.CropStandBlockEntity
import net.minecraft.core.BlockPos
import net.minecraft.server.level.ServerLevel
import net.minecraft.world.entity.AgeableMob
import net.minecraft.world.entity.animal.Animal
import net.minecraft.world.item.ItemStack
import net.minecraft.world.level.Level
import net.minecraftforge.event.level.BlockEvent
import net.minecraftforge.eventbus.api.Event
import net.minecraftforge.eventbus.api.EventPriority
import net.minecraftforge.eventbus.api.SubscribeEvent
import net.minecraftforge.fml.common.Mod

/** All tuning and effect rules live here; callers only supply their existing event context. */
@Mod.EventBusSubscriber(modid = FTMod.MODID, bus = Mod.EventBusSubscriber.Bus.FORGE)
object AreaWeatherEffects {
    fun growthPenalty(weather: AreaWeather): Double = when (weather) {
        AreaWeather.NUCLEAR_RADIATION -> 0.10
        AreaWeather.HEATWAVE, AreaWeather.HAIL -> 0.05
        else -> 0.0
    }

    @JvmStatic @SubscribeEvent(priority = EventPriority.LOWEST)
    fun onCropGrowth(event: BlockEvent.CropGrowEvent.Pre) {
        if (event.result == Event.Result.DENY) return
        val level = event.level as? ServerLevel ?: return
        val penalty = growthPenalty(AreaWeatherEvents.current(level))
        if (penalty > 0.0 && level.random.nextDouble() < penalty) event.result = Event.Result.DENY
    }

    /** Called only for the natural age increment, preserving adult cooldowns, feeding and age locks. */
    @JvmStatic
    fun allowAnimalGrowth(mob: AgeableMob): Boolean {
        if (mob !is Animal || mob.age >= 0 || mob.level() !is ServerLevel) return true
        val penalty = growthPenalty(AreaWeatherEvents.current(mob.level() as ServerLevel))
        if (penalty == 0.0) return true
        // Stagger by entity id: no extra RNG, NBT writes, entity lists or per-animal timers.
        return Math.floorMod(mob.tickCount + mob.id, 20) >= (penalty * 20).toInt()
    }

    @JvmStatic
    fun allowFishPoolSpawn(level: ServerLevel): Boolean =
        AreaWeatherEvents.current(level) != AreaWeather.HEATWAVE || level.random.nextDouble() >= 0.05

    @JvmStatic
    fun mutationChance(level: Level, original: Float): Float {
        if (original <= 0f || level !is ServerLevel) return original
        return if (AreaWeatherEvents.current(level) == AreaWeather.BREEZE) (original * 1.2f).coerceAtMost(100f) else original
    }

    /** Per-stack stochastic rounding gives a true 5% loss even when a harvest yields just one item. */
    @JvmStatic
    fun reduceHarvest(level: ServerLevel, stack: ItemStack) {
        if (stack.isEmpty || AreaWeatherEvents.current(level) != AreaWeather.NUCLEAR_RADIATION) return
        val loss = stack.count * 0.05
        val whole = loss.toInt()
        stack.shrink(whole + if (level.random.nextDouble() < loss - whole) 1 else 0)
    }

    /**
     * Nearby stands already cross every harvest, so boosting that probability would do nothing.
     * A breeze instead gives a 20% chance to bring a compatible father from the next ring.
     * Only runs at harvest when the existing eight adjacent stands supplied no father.
     */
    fun windFather(level: ServerLevel, pos: BlockPos, mother: ItemStack): ItemStack? {
        if (AreaWeatherEvents.current(level) != AreaWeather.BREEZE || level.random.nextDouble() >= 0.20) return null
        val cursor = BlockPos.MutableBlockPos()
        var selected: ItemStack? = null
        var count = 0
        for (x in -2..2) for (z in -2..2) {
            if (kotlin.math.abs(x) != 2 && kotlin.math.abs(z) != 2) continue
            cursor.set(pos.x + x, pos.y, pos.z + z)
            if (!level.hasChunkAt(cursor)) continue
            val stand = level.getBlockEntity(cursor) as? CropStandBlockEntity ?: continue
            if (!stand.isMature()) continue
            val donor = stand.breedingSeed()?.takeIf { it.`is`(mother.item) } ?: continue
            if (level.random.nextInt(++count) == 0) selected = donor
        }
        return selected
    }
}
