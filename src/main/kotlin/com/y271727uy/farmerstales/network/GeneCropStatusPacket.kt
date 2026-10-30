package com.y271727uy.farmerstales.network

import com.y271727uy.farmerstales.integration.jade.GeneCropJadeCache
import net.minecraft.core.BlockPos
import net.minecraft.network.FriendlyByteBuf
import net.minecraftforge.network.NetworkEvent
import java.util.function.Supplier

/** Server answer for one crop: identified, or not. */
class GeneCropStatusPacket(val pos: BlockPos, val identified: Boolean) {
    fun encode(buf: FriendlyByteBuf) {
        buf.writeBlockPos(pos)
        buf.writeBoolean(identified)
    }

    companion object {
        @JvmStatic
        fun decode(buf: FriendlyByteBuf) = GeneCropStatusPacket(buf.readBlockPos(), buf.readBoolean())

        @JvmStatic
        fun handle(packet: GeneCropStatusPacket, ctx: Supplier<NetworkEvent.Context>) {
            val context = ctx.get()
            if (context.direction.receptionSide.isClient) {
                context.enqueueWork {
                    GeneCropJadeCache.accept(packet.pos, packet.identified)
                }
            }
            context.packetHandled = true
        }
    }
}
