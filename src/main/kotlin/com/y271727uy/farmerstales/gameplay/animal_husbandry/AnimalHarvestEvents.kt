package com.y271727uy.farmerstales.gameplay.animal_husbandry

import com.y271727uy.farmerstales.FTMod
import com.y271727uy.farmerstales.gameplay.special_event.crimson_moon.CrimsonMoonEvents
import net.minecraft.resources.ResourceLocation
import net.minecraft.server.level.ServerLevel
import net.minecraft.sounds.SoundEvents
import net.minecraft.tags.ItemTags
import net.minecraft.world.InteractionResult
import net.minecraft.world.entity.animal.Animal
import net.minecraft.world.entity.animal.Cow
import net.minecraft.world.item.ItemStack
import net.minecraft.world.item.ItemUtils
import net.minecraft.world.item.Items
import net.minecraftforge.event.entity.living.LivingDropsEvent
import net.minecraftforge.event.entity.player.PlayerInteractEvent
import net.minecraftforge.eventbus.api.EventPriority
import net.minecraftforge.eventbus.api.SubscribeEvent
import net.minecraftforge.fml.common.Mod

@Mod.EventBusSubscriber(modid = FTMod.MODID, bus = Mod.EventBusSubscriber.Bus.FORGE)
@Suppress("unused")
object AnimalHarvestEvents {
    private const val AFFECTION_COST = 20
    private const val MILKING_COOLDOWN_TICKS = 60 * 20L
    private const val MILKING_HUNGER_COST = 10
    private const val MILKING_COOLDOWN_KEY = "FarmersTalesMilkingCooldownUntil"
    private val knifeTag = ItemTags.create(ResourceLocation.fromNamespaceAndPath("forge", "tools/knives"))

    fun milkingCooldownRemaining(animal: Animal): Long =
        (animal.persistentData.getLong(MILKING_COOLDOWN_KEY) - animal.level().gameTime).coerceAtLeast(0L)

    @JvmStatic
    @SubscribeEvent(priority = EventPriority.HIGHEST)
    fun onAnimalInteractSpecific(event: PlayerInteractEvent.EntityInteractSpecific) {
        handleMilking(event, event.target)
    }

    private fun handleMilking(event: PlayerInteractEvent, target: net.minecraft.world.entity.Entity) {
        val cow = target as? Cow ?: return
        if (event.level.isClientSide || cow.isBaby || event.itemStack.item != Items.BUCKET ||
            !AnimalMetabolism.isManaged(cow)
        ) {
            return
        }
        val level = event.level as? ServerLevel
        if (level != null && CrimsonMoonEvents.isActive(level)) {
            event.setCancellationResult(InteractionResult.FAIL)
            event.isCanceled = true
            return
        }

        val now = cow.level().gameTime
        val cooldownUntil = cow.persistentData.getLong(MILKING_COOLDOWN_KEY)
        if (cooldownUntil > now || !AnimalMetabolism.consumeHungerFromFullyFed(cow, MILKING_HUNGER_COST)) {
            event.setCancellationResult(InteractionResult.FAIL)
            event.isCanceled = true
            return
        }

        cow.persistentData.putLong(MILKING_COOLDOWN_KEY, now + MILKING_COOLDOWN_TICKS)
        cow.playSound(SoundEvents.COW_MILK, 1.0F, 1.0F)
        event.entity.setItemInHand(
            event.hand,
            ItemUtils.createFilledResult(event.itemStack, event.entity, Items.MILK_BUCKET.defaultInstance),
        )
        event.setCancellationResult(InteractionResult.SUCCESS)
        event.isCanceled = true
    }

    @JvmStatic
    @SubscribeEvent(priority = EventPriority.HIGHEST)
    fun onAnimalInteract(event: PlayerInteractEvent.EntityInteract) {
        handleMilking(event, event.target)
        if (event.isCanceled) return

        val animal = event.target as? Animal ?: return
        if (event.level.isClientSide || !event.itemStack.`is`(knifeTag) || !AnimalMetabolism.isManaged(animal)) {
            return
        }

        val meat = AnimalHusbandryRules.meatItem(animal) ?: return
        if (!AnimalMetabolism.spendAffection(animal, AFFECTION_COST)) {
            event.setCancellationResult(InteractionResult.FAIL)
            event.isCanceled = true
            return
        }

        animal.spawnAtLocation(ItemStack(meat, animal.random.nextInt(3) + 1))
        event.setCancellationResult(InteractionResult.SUCCESS)
        event.isCanceled = true
    }

    @JvmStatic
    @SubscribeEvent(priority = EventPriority.LOWEST)
    fun onLivingDrops(event: LivingDropsEvent) {
        val animal = event.entity as? Animal ?: return
        val vitals = AnimalMetabolism.inspect(animal, applyStarvationDamage = false) ?: return
        val plan = AnimalHusbandryRules.metabolismFor(animal) ?: return
        val amount = when {
            vitals.weight > plan.highWeightThreshold -> animal.random.nextInt(3) + 3
            vitals.weight > plan.mediumWeightThreshold -> animal.random.nextInt(3) + 2
            else -> return
        }
        val primaryMeat = AnimalHusbandryRules.meatItem(animal)
        event.drops
            .filter { it.item.item != primaryMeat }
            .forEach { drop -> drop.item.grow(amount) }
    }
}
