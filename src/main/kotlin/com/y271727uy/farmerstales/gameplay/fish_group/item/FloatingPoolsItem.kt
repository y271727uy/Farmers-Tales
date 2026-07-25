package com.y271727uy.farmerstales.gameplay.fish_group.item

import com.y271727uy.farmerstales.gameplay.fish_group.entity.AbstractFishPoolEntity
import net.minecraft.core.particles.ParticleTypes
import net.minecraft.network.chat.Component
import net.minecraft.network.chat.Style
import net.minecraft.network.chat.TextColor
import net.minecraft.world.InteractionHand
import net.minecraft.world.InteractionResultHolder
import net.minecraft.world.entity.player.Player
import net.minecraft.world.item.Item
import net.minecraft.world.item.ItemStack
import net.minecraft.world.item.TooltipFlag
import net.minecraft.world.level.ClipContext
import net.minecraft.world.level.Level
import net.minecraft.world.level.gameevent.GameEvent
import net.minecraft.world.phys.AABB
import net.minecraft.world.phys.HitResult

class FloatingPoolsItem(properties: Properties) : Item(properties) {
    override fun use(level: Level, player: Player, hand: InteractionHand): InteractionResultHolder<ItemStack> {
        val itemStack = player.getItemInHand(hand)
        val hitResult = getPlayerPOVHitResult(level, player, ClipContext.Fluid.ANY)
        if (hitResult.type != HitResult.Type.BLOCK) return InteractionResultHolder.pass(itemStack)

        val location = hitResult.location
        val checkArea = AABB(
            location.x - 4.0,
            location.y - 4.0,
            location.z - 4.0,
            location.x + 4.0,
            location.y + 4.0,
            location.z + 4.0,
        )
        if (level.getEntitiesOfClass(AbstractFishPoolEntity::class.java, checkArea).isNotEmpty()) {
            return InteractionResultHolder.fail(itemStack)
        }

        if (!level.isClientSide) {
            val fishPool = FishGroupRegistry.getFishPoolRegistrations()
                .firstOrNull { itemStack.`is`(it.item.get()) }
                ?.create(level)
            if (fishPool != null) {
                fishPool.setPos(location.x, location.y - 1.85, location.z)
                fishPool.yRot = player.yRot
                if (!level.noCollision(fishPool, fishPool.boundingBox)) {
                    return InteractionResultHolder.fail(itemStack)
                }
                level.addFreshEntity(fishPool)
                level.gameEvent(player, GameEvent.ENTITY_PLACE, location)
                if (!player.abilities.instabuild) itemStack.shrink(1)
            }
        }

        showParticles(level, hitResult)
        return InteractionResultHolder.sidedSuccess(itemStack, level.isClientSide)
    }

    private fun showParticles(level: Level, hitResult: HitResult) {
        repeat(20) {
            val xOffset = (level.random.nextDouble() - 0.5) * 2.0
            val yOffset = (level.random.nextDouble() - 0.5) * 2.0
            val zOffset = (level.random.nextDouble() - 0.5) * 2.0
            val location = hitResult.location
            level.addParticle(
                ParticleTypes.BUBBLE_POP,
                location.x + xOffset,
                location.y + yOffset,
                location.z + zOffset,
                0.0,
                0.0,
                0.0
            )
            level.addParticle(
                ParticleTypes.SPLASH,
                location.x + xOffset,
                location.y + yOffset,
                location.z + zOffset,
                0.0,
                0.0,
                0.0
            )
        }
    }

    override fun appendHoverText(stack: ItemStack, level: Level?, tooltip: MutableList<Component>, flag: TooltipFlag) {
        tooltip += Component.translatable("tooltip.farmerstales.fish_group.not_obtainable")
            .setStyle(Style.EMPTY.withColor(TextColor.fromRgb(0xD27D46)).withItalic(true))
    }
}
