package com.y271727uy.farmerstales.gameplay.common

import com.y271727uy.farmerstales.network.FarmerstalesNetwork
import net.minecraft.network.chat.Component
import net.minecraft.server.level.ServerLevel
import net.minecraft.server.level.ServerPlayer
import net.minecraftforge.network.PacketDistributor
import java.util.function.Supplier

/** Sends a mod-channel packet to everyone currently in this dimension. */
fun ServerLevel.sendToDimension(packet: Any) {
    FarmerstalesNetwork.CHANNEL.send(PacketDistributor.DIMENSION.with(Supplier { dimension() }), packet)
}

/** Sends a mod-channel packet to this player only. */
fun ServerPlayer.sendPacket(packet: Any) {
    FarmerstalesNetwork.CHANNEL.send(PacketDistributor.PLAYER.with(Supplier { this }), packet)
}

/** System chat message to every player in this dimension. */
fun ServerLevel.announce(message: Component) {
    players().forEach { it.sendSystemMessage(message) }
}

/** System chat message to the players in this dimension accepted by [filter]. */
inline fun ServerLevel.announce(message: Component, filter: (ServerPlayer) -> Boolean) {
    players().forEach { if (filter(it)) it.sendSystemMessage(message) }
}
