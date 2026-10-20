package com.y271727uy.farmerstales.network

import com.y271727uy.farmerstales.gameplay.curated_crop.client.CuratedCropPhotonPlayback
import com.y271727uy.farmerstales.gameplay.curated_crop.client.CuratedCropVanillaPlayback
import net.minecraft.core.BlockPos
import net.minecraft.network.FriendlyByteBuf
import net.minecraftforge.fml.ModList
import net.minecraftforge.network.NetworkEvent
import java.util.function.Supplier

/** Asks nearby clients to play one curated-crop effect on a block. */
class CuratedCropEffectPacket(val pos: BlockPos, val kind: String) {
    fun encode(buf: FriendlyByteBuf) {
        buf.writeBlockPos(pos)
        buf.writeUtf(kind)
    }

    companion object {
        @JvmStatic
        fun decode(buf: FriendlyByteBuf) = CuratedCropEffectPacket(buf.readBlockPos(), buf.readUtf())

        @JvmStatic
        fun handle(packet: CuratedCropEffectPacket, ctx: Supplier<NetworkEvent.Context>) {
            val context = ctx.get()
            if (context.direction.receptionSide.isClient) {
                context.enqueueWork {
                    if (ModList.get().isLoaded("photon")) {
                        try {
                            CuratedCropPhotonPlayback.play(packet.pos, packet.kind)
                        } catch (failure: Throwable) {
                            CuratedCropVanillaPlayback.play(packet.pos, packet.kind, failure.toString())
                        }
                    } else {
                        CuratedCropVanillaPlayback.play(packet.pos, packet.kind, "photon 未加载")
                    }
                }
            }
            context.packetHandled = true
        }
    }
}
