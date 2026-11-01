package com.y271727uy.farmerstales.network

import com.y271727uy.farmerstales.gameplay.weather.heavy_rain.HeavyRainClientState
import com.y271727uy.farmerstales.gameplay.weather.heavy_rain.RainIntensity
import net.minecraft.network.FriendlyByteBuf
import net.minecraftforge.network.NetworkEvent
import java.util.function.Supplier

class RainIntensitySyncPacket(val temperate: RainIntensity, val tropical: RainIntensity) {
    fun encode(buf: FriendlyByteBuf) {
        buf.writeEnum(temperate)
        buf.writeEnum(tropical)
    }

    companion object {
        @JvmStatic
        fun decode(buf: FriendlyByteBuf) = RainIntensitySyncPacket(
            buf.readEnum(RainIntensity::class.java), buf.readEnum(RainIntensity::class.java),
        )

        @JvmStatic
        fun handle(packet: RainIntensitySyncPacket, ctx: Supplier<NetworkEvent.Context>) {
            val context = ctx.get()
            if (context.direction.receptionSide.isClient) context.enqueueWork {
                HeavyRainClientState.temperate = packet.temperate
                HeavyRainClientState.tropical = packet.tropical
            }
            context.packetHandled = true
        }
    }
}
