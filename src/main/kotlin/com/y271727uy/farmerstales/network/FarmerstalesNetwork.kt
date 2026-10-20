package com.y271727uy.farmerstales.network

import com.y271727uy.farmerstales.FTMod
import net.minecraft.resources.ResourceLocation
import net.minecraftforge.network.NetworkRegistry
import net.minecraftforge.network.simple.SimpleChannel

object FarmerstalesNetwork {
    private const val PROTOCOL = "1"

    val CHANNEL: SimpleChannel = NetworkRegistry.newSimpleChannel(
        ResourceLocation.fromNamespaceAndPath(FTMod.MODID, "main"),
        { PROTOCOL },
        { it == PROTOCOL },
        { it == PROTOCOL },
    )

    private var nextId = 0

    fun register() {
        CHANNEL.messageBuilder(MoonPhaseSyncPacket::class.java, nextId++)
            .encoder(MoonPhaseSyncPacket::encode)
            .decoder(MoonPhaseSyncPacket::decode)
            .consumerMainThread(MoonPhaseSyncPacket::handle)
            .add()
        CHANNEL.messageBuilder(GreenRainSyncPacket::class.java, nextId++)
            .encoder(GreenRainSyncPacket::encode)
            .decoder(GreenRainSyncPacket::decode)
            .consumerMainThread(GreenRainSyncPacket::handle)
            .add()
        CHANNEL.messageBuilder(DarkCumulonimbusSyncPacket::class.java, nextId++)
            .encoder(DarkCumulonimbusSyncPacket::encode)
            .decoder(DarkCumulonimbusSyncPacket::decode)
            .consumerMainThread(DarkCumulonimbusSyncPacket::handle)
            .add()
        CHANNEL.messageBuilder(CuratedCropEffectPacket::class.java, nextId++)
            .encoder(CuratedCropEffectPacket::encode)
            .decoder(CuratedCropEffectPacket::decode)
            .consumerMainThread(CuratedCropEffectPacket::handle)
            .add()
    }
}
