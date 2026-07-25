package com.y271727uy.farmerstales.gameplay.fish_group.pool

import com.y271727uy.farmerstales.FTMod
import net.minecraft.nbt.CompoundTag
import net.minecraft.nbt.ListTag
import net.minecraft.nbt.Tag
import net.minecraft.server.level.ServerLevel
import net.minecraft.world.entity.player.Player
import net.minecraft.world.level.saveddata.SavedData
import java.util.UUID

class FishKingCooldownSavedData : SavedData() {
    private val cooldownExpiryByPlayer = mutableMapOf<UUID, Long>()

    fun canAward(player: Player, gameTime: Long): Boolean {
        val expiry = cooldownExpiryByPlayer[player.uuid] ?: return true
        if (expiry > gameTime) return false
        cooldownExpiryByPlayer.remove(player.uuid)
        setDirty()
        return true
    }

    fun markAwarded(player: Player, gameTime: Long) {
        cooldownExpiryByPlayer[player.uuid] = gameTime + COOLDOWN_TICKS
        setDirty()
    }

    override fun save(tag: CompoundTag): CompoundTag {
        val cooldowns = ListTag()
        cooldownExpiryByPlayer.forEach { (playerId, expiry) ->
            cooldowns += CompoundTag().apply {
                putUUID("Player", playerId)
                putLong("Expiry", expiry)
            }
        }
        tag.put("Cooldowns", cooldowns)
        return tag
    }

    companion object {
        const val COOLDOWN_TICKS = 72_000L
        private const val DATA_NAME = FTMod.MODID + "_fish_king_cooldowns"

        @JvmStatic
        fun get(level: ServerLevel): FishKingCooldownSavedData = level.server.overworld().dataStorage.computeIfAbsent(
            ::load,
            ::FishKingCooldownSavedData,
            DATA_NAME,
        )

        @JvmStatic
        fun load(tag: CompoundTag): FishKingCooldownSavedData = FishKingCooldownSavedData().apply {
            tag.getList("Cooldowns", Tag.TAG_COMPOUND.toInt()).forEach { element ->
                val cooldownTag = element as CompoundTag
                if (cooldownTag.hasUUID("Player")) {
                    cooldownExpiryByPlayer[cooldownTag.getUUID("Player")] = cooldownTag.getLong("Expiry")
                }
            }
        }
    }
}
