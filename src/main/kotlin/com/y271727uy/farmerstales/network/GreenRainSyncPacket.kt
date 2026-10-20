package com.y271727uy.farmerstales.network

import com.y271727uy.farmerstales.gameplay.weather.green_rain.GreenRainClientState
import net.minecraft.network.FriendlyByteBuf
import net.minecraftforge.network.NetworkEvent
import java.util.function.Supplier

/** Current dimension's green-rain flag. */
class GreenRainSyncPacket(val active: Boolean) {
    fun encode(buf: FriendlyByteBuf) {
        buf.writeBoolean(active)
    }

    companion object {
        @JvmStatic
        fun decode(buf: FriendlyByteBuf) = GreenRainSyncPacket(buf.readBoolean())

        @JvmStatic
        fun handle(packet: GreenRainSyncPacket, ctx: Supplier<NetworkEvent.Context>) {
            val context = ctx.get()
            if (context.direction.receptionSide.isClient) {
                context.enqueueWork {
                    GreenRainClientState.active = packet.active
                }
            }
            context.packetHandled = true
        }
    }
}
