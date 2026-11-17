package com.y271727uy.farmerstales.gameplay.weather.area_weather

import com.mojang.brigadier.arguments.StringArgumentType
import com.y271727uy.farmerstales.FTMod
import com.y271727uy.farmerstales.integration.IntegrationManager
import net.minecraft.commands.Commands
import net.minecraft.commands.SharedSuggestionProvider
import net.minecraft.network.chat.Component
import net.minecraftforge.event.RegisterCommandsEvent
import net.minecraftforge.eventbus.api.SubscribeEvent
import net.minecraftforge.fml.common.Mod

@Mod.EventBusSubscriber(modid = FTMod.MODID, bus = Mod.EventBusSubscriber.Bus.FORGE)
object AreaWeatherCommands {
    @JvmStatic @SubscribeEvent
    fun register(event: RegisterCommandsEvent) {
        event.dispatcher.register(Commands.literal(FTMod.MODID).then(
            Commands.literal("area_weather").requires { it.hasPermission(2) }
                .executes { context ->
                    val level = context.source.level
                    val key = if (AreaWeatherEvents.supported(level)) AreaWeatherEvents.current(level).translationKey
                        else "hud.${FTMod.MODID}.weather.unavailable"
                    context.source.sendSuccess({ Component.translatable("hud.${FTMod.MODID}.area_weather", Component.translatable(key)) }, false)
                    1
                }
                .then(Commands.argument("id", StringArgumentType.word())
                    .suggests { context, builder ->
                        val month = IntegrationManager.calendarAt(context.source.level, 0L)?.monthIndex
                        SharedSuggestionProvider.suggest(AreaWeather.entries.filter { it.isEligible(month) }.map { it.id }, builder)
                    }
                    .executes { context ->
                        val weather = AreaWeather.byId(StringArgumentType.getString(context, "id"))
                        if (weather == null || !AreaWeatherEvents.force(context.source.level, weather)) {
                            context.source.sendFailure(Component.translatable("commands.${FTMod.MODID}.area_weather.invalid"))
                            0
                        } else {
                            context.source.sendSuccess({ Component.translatable("commands.${FTMod.MODID}.area_weather.success", Component.translatable(weather.translationKey)) }, true)
                            1
                        }
                    })
        ))
    }
}
