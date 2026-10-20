package com.y271727uy.farmerstales.network

import com.y271727uy.farmerstales.gameplay.weather.dark_cumulonimbus.DarkCumulonimbusClientState
import net.minecraft.network.FriendlyByteBuf
import net.minecraftforge.network.NetworkEvent
import java.util.function.Supplier

/** Current dimension's dry-overcast flag. */
class DarkCumulonimbusSyncPacket(val active: Boolean) {
    fun encode(buf: FriendlyByteBuf) {
        buf.writeBoolean(active)
    }

    companion object {
        @JvmStatic
        fun decode(buf: FriendlyByteBuf) = DarkCumulonimbusSyncPacket(buf.readBoolean())

        @JvmStatic
        fun handle(packet: DarkCumulonimbusSyncPacket, ctx: Supplier<NetworkEvent.Context>) {
            val context = ctx.get()
            if (context.direction.receptionSide.isClient) {
                context.enqueueWork {
                    DarkCumulonimbusClientState.active = packet.active
                }
            }
            context.packetHandled = true
        }
    }
}
