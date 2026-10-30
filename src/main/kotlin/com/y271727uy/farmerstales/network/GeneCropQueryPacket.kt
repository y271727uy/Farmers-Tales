package com.y271727uy.farmerstales.network

import com.y271727uy.farmerstales.gameplay.gene_crop.GeneCropGenome
import com.y271727uy.farmerstales.gameplay.gene_crop.GeneCropParents
import net.minecraft.core.BlockPos
import net.minecraft.network.FriendlyByteBuf
import net.minecraft.world.phys.Vec3
import net.minecraftforge.network.NetworkEvent
import net.minecraftforge.network.PacketDistributor
import java.util.function.Supplier

/** Client asks whether the crop it is looking at has an identified genome. */
class GeneCropQueryPacket(val pos: BlockPos) {
    fun encode(buf: FriendlyByteBuf) {
        buf.writeBlockPos(pos)
    }

    companion object {
        private const val REACH_SQR = 64.0 * 64.0

        @JvmStatic
        fun decode(buf: FriendlyByteBuf) = GeneCropQueryPacket(buf.readBlockPos())

        @JvmStatic
        fun handle(packet: GeneCropQueryPacket, ctx: Supplier<NetworkEvent.Context>) {
            val context = ctx.get()
            if (context.direction.receptionSide.isServer) {
                context.enqueueWork {
                    val player = context.sender ?: return@enqueueWork
                    val pos = packet.pos
                    if (player.distanceToSqr(Vec3.atCenterOf(pos)) > REACH_SQR) {
                        return@enqueueWork
                    }
                    val level = player.serverLevel()
                    val state = level.getBlockState(pos)
                    val identified = GeneCropGenome.isGeneCrop(state, level, pos) &&
                        GeneCropParents.get(level).read(level, pos) != null
                    FarmerstalesNetwork.CHANNEL.send(
                        PacketDistributor.PLAYER.with(Supplier { player }),
                        GeneCropStatusPacket(pos, identified),
                    )
                }
            }
            context.packetHandled = true
        }
    }
}
