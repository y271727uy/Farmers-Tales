package com.y271727uy.farmerstales.gameplay.weather.area_weather

import com.y271727uy.farmerstales.FTMod
import com.y271727uy.farmerstales.gameplay.common.PlayerLevelSync
import com.y271727uy.farmerstales.gameplay.common.dayIndex
import com.y271727uy.farmerstales.gameplay.common.sendPacket
import com.y271727uy.farmerstales.gameplay.common.sendToDimension
import com.y271727uy.farmerstales.integration.IntegrationManager
import com.y271727uy.farmerstales.gameplay.weather.dark_cumulonimbus.DarkCumulonimbusEvents
import com.y271727uy.farmerstales.gameplay.weather.green_rain.GreenRainEvents
import com.y271727uy.farmerstales.gameplay.weather.heavy_rain.HeavyRainEvents
import net.minecraft.nbt.CompoundTag
import net.minecraft.server.level.ServerLevel
import net.minecraft.server.level.ServerPlayer
import net.minecraft.util.RandomSource
import net.minecraft.world.level.GameRules
import net.minecraft.world.level.saveddata.SavedData
import net.minecraft.world.level.storage.ServerLevelData
import net.minecraftforge.event.CommandEvent
import net.minecraftforge.event.TickEvent
import net.minecraftforge.event.level.LevelEvent
import net.minecraftforge.eventbus.api.SubscribeEvent
import net.minecraftforge.fml.common.Mod
import java.util.WeakHashMap

@Mod.EventBusSubscriber(modid = FTMod.MODID, bus = Mod.EventBusSubscriber.Bus.FORGE)
object AreaWeatherEvents {
    private val cache = WeakHashMap<ServerLevel, DailyData>()

    init {
        PlayerLevelSync.register(::syncPlayer)
    }

    fun supported(level: net.minecraft.world.level.Level): Boolean =
        level.dimensionType().hasSkyLight() && !level.dimensionType().hasCeiling()

    @JvmStatic
    fun current(level: ServerLevel): AreaWeather {
        // Growth and harvest hooks only read the cached result. The level tick owns day changes.
        // Lazy initialization covers calls made before this level's first tick.
        return cache[level]?.weather
            ?: if (supported(level)) updateForDay(level).weather else AreaWeather.CLEAR
    }

    /** Clears every custom weather layer when vanilla /weather clear is executed. */
    @JvmStatic
    @SubscribeEvent
    fun onCommand(event: CommandEvent) {
        val parse = event.parseResults
        if (parse.reader.string.trim() != "weather clear") return
        val level = parse.context.source.level as? ServerLevel ?: return
        if (supported(level)) {
            val state = data(level)
            state.weather = AreaWeather.CLEAR
            state.day = level.dayIndex
            state.setDirty()
            level.sendToDimension(AreaWeatherSyncPacket(AreaWeather.CLEAR))
        }
        DarkCumulonimbusEvents.clear(level)
        GreenRainEvents.clear(level)
        HeavyRainEvents.clear(level)
    }

    fun force(level: ServerLevel, weather: AreaWeather): Boolean {
        if (!supported(level) || !weather.isEligible(IntegrationManager.calendarAt(level, 0L)?.monthIndex)) return false
        val state = data(level)
        state.day = level.dayIndex
        state.weather = weather
        state.setDirty()
        level.sendToDimension(AreaWeatherSyncPacket(weather))
        return true
    }

    private fun data(level: ServerLevel): DailyData = cache.getOrPut(level) {
        level.dataStorage.computeIfAbsent(::load, ::DailyData, "${FTMod.MODID}_area_weather")
    }

    private fun updateForDay(level: ServerLevel): DailyData {
        val state = data(level)
        val day = level.dayIndex
        if (state.day == day) return state
        val month = IntegrationManager.calendarAt(level, 0L)?.monthIndex
        // A deterministic daily seed also prevents re-rolling by turning the clock back.
        val random = RandomSource.create(level.seed xor (day * -7046029254386353131L) xor level.dimension().location().hashCode().toLong())
        state.weather = AreaWeather.select(month, random.nextDouble(), random::nextInt)
        state.day = day
        state.setDirty()
        level.sendToDimension(AreaWeatherSyncPacket(state.weather))
        return state
    }

    @JvmStatic
    @SubscribeEvent
    fun onLevelTick(event: TickEvent.LevelTickEvent) {
        val level = event.level as? ServerLevel ?: return
        if (event.phase != TickEvent.Phase.START || !supported(level)) return
        // One date comparison per dimension, regardless of the number of crops or animals.
        val weather = updateForDay(level).weather
        if (level.gameTime % 20L != 0L || !level.gameRules.getBoolean(GameRules.RULE_WEATHER_CYCLE)) return
        val vanilla = level.levelData as? ServerLevelData ?: return
        if (vanilla.clearWeatherTime > 0) return
        // Advance only the pending vanilla transition by 10%; never force weather or extend a storm.
        if (weather == AreaWeather.LIGHT_RAIN && !level.isRaining && vanilla.rainTime > 2) {
            vanilla.rainTime = (vanilla.rainTime - 2).coerceAtLeast(1)
        }
        if (weather == AreaWeather.THUNDERSHOWERS && !level.isThundering && vanilla.thunderTime > 2) {
            vanilla.thunderTime = (vanilla.thunderTime - 2).coerceAtLeast(1)
        }
    }

    private fun syncPlayer(player: ServerPlayer) {
        val level = player.serverLevel()
        player.sendPacket(AreaWeatherSyncPacket(if (supported(level)) updateForDay(level).weather else null))
    }

    @JvmStatic @SubscribeEvent
    fun onUnload(event: LevelEvent.Unload) { (event.level as? ServerLevel)?.let { cache.remove(it) } }

    private fun load(tag: CompoundTag) = DailyData().apply {
        val loaded = AreaWeather.byId(tag.getString("Weather"))
        day = if (loaded != null && tag.contains("Day")) tag.getLong("Day") else Long.MIN_VALUE
        weather = loaded ?: AreaWeather.CLEAR
    }

    private class DailyData : SavedData() {
        var day = Long.MIN_VALUE
        var weather = AreaWeather.CLEAR
        override fun save(tag: CompoundTag): CompoundTag = tag.apply {
            putLong("Day", day)
            putString("Weather", weather.id)
        }
    }
}
