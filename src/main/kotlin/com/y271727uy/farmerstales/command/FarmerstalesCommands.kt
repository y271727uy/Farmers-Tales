package com.y271727uy.farmerstales.command

import com.mojang.brigadier.CommandDispatcher
import com.mojang.brigadier.arguments.StringArgumentType
import com.y271727uy.farmerstales.FTMod
import com.y271727uy.farmerstales.gameplay.animal_husbandry.raid.PredatorRaidEvents
import com.y271727uy.farmerstales.gameplay.weather.dark_cumulonimbus.DarkCumulonimbusEvents
import com.y271727uy.farmerstales.gameplay.weather.green_rain.GreenRainEvents
import com.y271727uy.farmerstales.network.CuratedCropEffectPacket
import com.y271727uy.farmerstales.network.FarmerstalesNetwork
import com.y271727uy.farmerstales.gameplay.special_event.common.MoonPhaseEvents
import net.minecraft.commands.CommandSourceStack
import net.minecraft.commands.arguments.coordinates.BlockPosArgument
import net.minecraft.commands.Commands
import net.minecraft.commands.SharedSuggestionProvider
import net.minecraft.network.chat.Component
import net.minecraftforge.event.RegisterCommandsEvent
import net.minecraftforge.network.PacketDistributor
import java.util.function.Supplier
import net.minecraftforge.eventbus.api.SubscribeEvent
import net.minecraftforge.fml.common.Mod

@Mod.EventBusSubscriber(modid = FTMod.MODID, bus = Mod.EventBusSubscriber.Bus.FORGE)
object FarmerstalesCommands {
    @JvmStatic
    @SubscribeEvent
    fun register(event: RegisterCommandsEvent) {
        register(event.dispatcher)
    }

    private fun register(dispatcher: CommandDispatcher<CommandSourceStack>) {
        dispatcher.register(
            Commands.literal(FTMod.MODID)
                .then(
                    Commands.literal("special_event")
                        .then(
                            Commands.argument("id", StringArgumentType.word())
                                .requires { it.hasPermission(2) }
                                .suggests { _, builder ->
                                    SharedSuggestionProvider.suggest(MoonPhaseEvents.ids(), builder)
                                }
                                .executes { context ->
                                    val id = StringArgumentType.getString(context, "id")
                                    if (!MoonPhaseEvents.contains(id)) {
                                        context.source.sendFailure(
                                            Component.translatable("commands.${FTMod.MODID}.special_event.unknown", id),
                                        )
                                        return@executes 0
                                    }
                                    context.source.server.allLevels.forEach { level ->
                                        MoonPhaseEvents.forceForCurrentNight(id, level)
                                    }
                                    context.source.sendSuccess(
                                        { Component.translatable("commands.${FTMod.MODID}.special_event.$id.success") },
                                        true,
                                    )
                                    1
                                },
                        ),
                )
                .then(
                    Commands.literal("green_rain")
                        .requires { it.hasPermission(2) }
                        .executes { context ->
                            val level = context.source.level
                            if (!GreenRainEvents.force(level)) {
                                context.source.sendFailure(
                                    Component.translatable("commands.${FTMod.MODID}.green_rain.not_raining"),
                                )
                                return@executes 0
                            }
                            context.source.sendSuccess(
                                { Component.translatable("commands.${FTMod.MODID}.green_rain.success") },
                                true,
                            )
                            1
                        },
                )
                .then(
                    Commands.literal("dark_cumulonimbus")
                        .requires { it.hasPermission(2) }
                        .executes { context ->
                            val level = context.source.level
                            if (!DarkCumulonimbusEvents.force(level)) {
                                context.source.sendFailure(
                                    Component.translatable("commands.${FTMod.MODID}.dark_cumulonimbus.raining"),
                                )
                                return@executes 0
                            }
                            context.source.sendSuccess(
                                { Component.translatable("commands.${FTMod.MODID}.dark_cumulonimbus.success") },
                                true,
                            )
                            1
                        }
                        .then(
                            Commands.literal("clear")
                                .executes { context ->
                                    DarkCumulonimbusEvents.clear(context.source.level)
                                    context.source.sendSuccess(
                                        { Component.translatable("commands.${FTMod.MODID}.dark_cumulonimbus.cleared") },
                                        true,
                                    )
                                    1
                                },
                        ),
                )
                .then(
                    Commands.literal("curated_crop")
                        .requires { it.hasPermission(2) }
                        .then(
                            Commands.argument("pos", BlockPosArgument.blockPos())
                                .then(
                                    Commands.argument("kind", StringArgumentType.word())
                                        .suggests { _, builder ->
                                            SharedSuggestionProvider.suggest(listOf("moon", "rainbow", "popular", "stars", "all"), builder)
                                        }
                                        .executes { context ->
                                            val pos = BlockPosArgument.getLoadedBlockPos(context, "pos")
                                            val kind = StringArgumentType.getString(context, "kind")
                                            if (kind !in setOf("moon", "rainbow", "popular", "stars", "all")) {
                                                context.source.sendFailure(
                                                    Component.translatable("commands.${FTMod.MODID}.curated_crop.unknown", kind),
                                                )
                                                return@executes 0
                                            }
                                            val level = context.source.level
                                            FarmerstalesNetwork.CHANNEL.send(
                                                PacketDistributor.NEAR.with(
                                                    Supplier {
                                                        PacketDistributor.TargetPoint(
                                                            pos.x + 0.5,
                                                            pos.y + 0.5,
                                                            pos.z + 0.5,
                                                            64.0,
                                                            level.dimension(),
                                                        )
                                                    },
                                                ),
                                                CuratedCropEffectPacket(pos, kind),
                                            )
                                            context.source.sendSuccess(
                                                { Component.translatable("commands.${FTMod.MODID}.curated_crop.success", kind) },
                                                true,
                                            )
                                            1
                                        },
                                ),
                        ),
                )
                .then(
                    Commands.literal("wolf_raid")
                        .requires { it.hasPermission(2) }
                        .executes { context ->
                            val player = context.source.player
                            if (player == null) {
                                context.source.sendFailure(
                                    Component.translatable("commands.${FTMod.MODID}.wolf_raid.player_required"),
                                )
                                return@executes 0
                            }
                            when (PredatorRaidEvents.forceWolfRaid(player)) {
                                PredatorRaidEvents.ForceWolfRaidResult.SPAWNED -> {
                                    context.source.sendSuccess(
                                        { Component.translatable("commands.${FTMod.MODID}.wolf_raid.success") },
                                        true,
                                    )
                                    1
                                }
                                PredatorRaidEvents.ForceWolfRaidResult.NO_FLOCK -> {
                                    context.source.sendFailure(
                                        Component.translatable("commands.${FTMod.MODID}.wolf_raid.no_flock"),
                                    )
                                    0
                                }
                                PredatorRaidEvents.ForceWolfRaidResult.FAILED -> {
                                    context.source.sendFailure(
                                        Component.translatable("commands.${FTMod.MODID}.wolf_raid.failed"),
                                    )
                                    0
                                }
                            }
                        }
                        .then(
                            Commands.literal("break_fence")
                                .executes { context ->
                                    val player = context.source.player
                                    if (player == null) {
                                        context.source.sendFailure(
                                            Component.translatable("commands.${FTMod.MODID}.wolf_raid.player_required"),
                                        )
                                        return@executes 0
                                    }
                                    when (PredatorRaidEvents.forceFenceBreak(player)) {
                                        PredatorRaidEvents.ForceFenceBreakResult.GRANTED -> {
                                            context.source.sendSuccess(
                                                { Component.translatable("commands.${FTMod.MODID}.wolf_raid.break_fence.success") },
                                                true,
                                            )
                                            1
                                        }
                                        PredatorRaidEvents.ForceFenceBreakResult.NO_RAIDERS -> {
                                            context.source.sendFailure(
                                                Component.translatable("commands.${FTMod.MODID}.wolf_raid.break_fence.no_raiders"),
                                            )
                                            0
                                        }
                                    }
                                },
                        ),
                ),
        )
    }

}
