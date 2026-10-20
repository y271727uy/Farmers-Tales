package com.y271727uy.farmerstales.gameplay.animal_husbandry

import com.y271727uy.farmerstales.FTMod
import net.minecraft.server.level.ServerPlayer
import net.minecraft.sounds.SoundEvents
import net.minecraft.sounds.SoundSource
import net.minecraft.world.InteractionResult
import net.minecraft.world.entity.Entity
import net.minecraft.world.entity.Mob
import net.minecraft.world.entity.ambient.Bat
import net.minecraft.world.entity.animal.PolarBear
import net.minecraft.world.effect.MobEffectInstance
import net.minecraft.world.effect.MobEffects
import net.minecraft.world.item.Items
import net.minecraftforge.event.entity.player.PlayerInteractEvent
import net.minecraftforge.eventbus.api.EventPriority
import net.minecraftforge.eventbus.api.SubscribeEvent
import net.minecraftforge.fml.common.Mod

@Mod.EventBusSubscriber(modid = FTMod.MODID, bus = Mod.EventBusSubscriber.Bus.FORGE)
@Suppress("unused")
object UsefulVanillaAnimalEvents {
    private const val BAT_RADIUS = 10.0
    private const val BAT_GLOW_DURATION = 15 * 20
    private const val POLAR_BEAR_RADIUS = 8.0
    private const val POLAR_BEAR_STRENGTH_DURATION = 15 * 20
    private const val POLAR_BEAR_COOLDOWN = 30 * 20
    private const val POLAR_BEAR_COOLDOWN_KEY = "FarmersTalesPolarBearCooldownUntil"

    @JvmStatic
    @SubscribeEvent(priority = EventPriority.HIGHEST)
    fun onEntityInteractSpecific(event: PlayerInteractEvent.EntityInteractSpecific) {
        handleInteraction(event, event.target)
    }

    @JvmStatic
    @SubscribeEvent(priority = EventPriority.HIGHEST)
    fun onEntityInteract(event: PlayerInteractEvent.EntityInteract) {
        handleInteraction(event, event.target)
    }

    private fun handleInteraction(event: PlayerInteractEvent, target: Entity) {
        if (event.level.isClientSide) return

        when (target) {
            is Bat -> handleBat(event, target)
            is PolarBear -> handlePolarBear(event, target)
        }
    }

    private fun handleBat(event: PlayerInteractEvent, bat: Bat) {
        if (event.itemStack.item != Items.GLOW_BERRIES) return

        val mob = bat.level().getEntitiesOfClass(
            Mob::class.java,
            bat.boundingBox.inflate(BAT_RADIUS),
        ).filter { it !== bat }
            .minByOrNull { it.distanceToSqr(bat) }
            ?: bat

        mob.addEffect(MobEffectInstance(MobEffects.GLOWING, BAT_GLOW_DURATION, 0))
        bat.level().playSound(null, bat.blockPosition(), SoundEvents.GENERIC_EAT, SoundSource.AMBIENT, 1.0f, 1.0f)
        consume(event)
    }

    private fun handlePolarBear(event: PlayerInteractEvent, bear: PolarBear) {
        if (event.itemStack.item != Items.SALMON) return

        val cooldownUntil = bear.persistentData.getLong(POLAR_BEAR_COOLDOWN_KEY)
        if (cooldownUntil > bear.level().gameTime) return

        bear.level().getEntitiesOfClass(
            ServerPlayer::class.java,
            bear.boundingBox.inflate(POLAR_BEAR_RADIUS),
        ).forEach { player ->
            player.addEffect(
                MobEffectInstance(MobEffects.DAMAGE_BOOST, POLAR_BEAR_STRENGTH_DURATION, 0),
            )
        }

        bear.persistentData.putLong(
            POLAR_BEAR_COOLDOWN_KEY,
            bear.level().gameTime + POLAR_BEAR_COOLDOWN,
        )
        bear.level().playSound(null, bear.blockPosition(), SoundEvents.GENERIC_EAT, SoundSource.AMBIENT, 1.0f, 1.0f)
        consume(event)
    }

    private fun consume(event: PlayerInteractEvent) {
        if (!event.entity.abilities.instabuild) {
            event.itemStack.shrink(1)
        }
        event.setCancellationResult(InteractionResult.SUCCESS)
        event.isCanceled = true
    }
}
