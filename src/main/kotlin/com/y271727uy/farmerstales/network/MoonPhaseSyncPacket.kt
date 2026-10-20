package com.y271727uy.farmerstales.network

import com.y271727uy.farmerstales.gameplay.special_event.common.MoonPhaseClientState
import net.minecraft.network.FriendlyByteBuf
import net.minecraftforge.network.NetworkEvent
import java.util.function.Supplier

/** Current dimension's moon-event winner. Empty string means no event. */
class MoonPhaseSyncPacket(val winnerId: String) {
    fun encode(buf: FriendlyByteBuf) {
        buf.writeUtf(winnerId, 64)
    }

    companion object {
        @JvmStatic
        fun decode(buf: FriendlyByteBuf) = MoonPhaseSyncPacket(buf.readUtf(64))

        @JvmStatic
        fun handle(packet: MoonPhaseSyncPacket, ctx: Supplier<NetworkEvent.Context>) {
            val context = ctx.get()
            if (context.direction.receptionSide.isClient) {
                context.enqueueWork {
                    MoonPhaseClientState.winnerId = packet.winnerId
                }
            }
            context.packetHandled = true
        }
    }
}
