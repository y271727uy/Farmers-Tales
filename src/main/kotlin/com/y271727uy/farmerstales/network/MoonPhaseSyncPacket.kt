package com.y271727uy.farmerstales.network

import com.y271727uy.farmerstales.gameplay.special_event.common.MoonPhaseClientState
import net.minecraft.network.FriendlyByteBuf
import net.minecraftforge.network.NetworkEvent
import java.util.function.Supplier

/** Keep the daytime forecast separate from the currently active visuals and gameplay. */
class MoonPhaseSyncPacket(val winnerId: String, val forecastId: String, val forecastDay: Long) {
    fun encode(buf: FriendlyByteBuf) {
        buf.writeUtf(winnerId, 64)
        buf.writeUtf(forecastId, 64)
        buf.writeLong(forecastDay)
    }

    companion object {
        @JvmStatic
        fun decode(buf: FriendlyByteBuf) = MoonPhaseSyncPacket(buf.readUtf(64), buf.readUtf(64), buf.readLong())

        @JvmStatic
        fun handle(packet: MoonPhaseSyncPacket, ctx: Supplier<NetworkEvent.Context>) {
            val context = ctx.get()
            if (context.direction.receptionSide.isClient) {
                context.enqueueWork {
                    MoonPhaseClientState.winnerId = packet.winnerId
                    MoonPhaseClientState.forecastId = packet.forecastId
                    MoonPhaseClientState.forecastDay = packet.forecastDay
                }
            }
            context.packetHandled = true
        }
    }
}
