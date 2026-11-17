package com.y271727uy.farmerstales.gameplay.weather.area_weather

import net.minecraft.network.FriendlyByteBuf
import net.minecraftforge.network.NetworkEvent
import java.util.function.Supplier

class AreaWeatherSyncPacket(val weather: AreaWeather?) {
    fun encode(buf: FriendlyByteBuf) {
        buf.writeBoolean(weather != null)
        weather?.let { buf.writeEnum(it) }
    }
    companion object {
        @JvmStatic fun decode(buf: FriendlyByteBuf) = AreaWeatherSyncPacket(
            if (buf.readBoolean()) buf.readEnum(AreaWeather::class.java) else null
        )
        @JvmStatic fun handle(packet: AreaWeatherSyncPacket, ctx: Supplier<NetworkEvent.Context>) {
            val context = ctx.get()
            context.enqueueWork { AreaWeatherClientState.weather = packet.weather }
            context.packetHandled = true
        }
    }
}
