package com.y271727uy.farmerstales.gameplay.food_expiry

import com.y271727uy.farmerstales.FTMod
import net.minecraft.server.level.ServerLevel
import net.minecraft.world.InteractionResult
import net.minecraft.world.item.ItemStack
import net.minecraft.world.level.block.Blocks
import net.minecraft.world.level.block.ComposterBlock
import net.minecraftforge.event.entity.player.PlayerInteractEvent
import net.minecraftforge.eventbus.api.EventPriority
import net.minecraftforge.eventbus.api.SubscribeEvent
import net.minecraftforge.fml.common.Mod

/** Lets rotten food be disposed of through the vanilla composter. */
@Mod.EventBusSubscriber(modid = FTMod.MODID, bus = Mod.EventBusSubscriber.Bus.FORGE)
@Suppress("unused")
object FoodExpiryCompostingEvents {
    private const val SUCCESS_CHANCE = 0.50f

    @JvmStatic
    @SubscribeEvent(priority = EventPriority.HIGHEST)
    fun onRightClickComposter(event: PlayerInteractEvent.RightClickBlock) {
        val level = event.level as? ServerLevel ?: return
        val state = level.getBlockState(event.pos)
        val food = event.itemStack
        if (!state.`is`(Blocks.COMPOSTER) ||
            state.getValue(ComposterBlock.LEVEL) >= 7 ||
            !FoodExpiry.isRotten(food, level.gameTime)
        ) {
            return
        }

        if (level.random.nextFloat() < SUCCESS_CHANCE) {
            compostSuccessfully(level, event, state, food)
        } else {
            consumeAndReturnRemainder(event, food)
            level.levelEvent(1500, event.pos, 0)
        }
        event.setCancellationResult(InteractionResult.SUCCESS)
        event.isCanceled = true
    }

    private fun compostSuccessfully(
        level: ServerLevel,
        event: PlayerInteractEvent.RightClickBlock,
        state: net.minecraft.world.level.block.state.BlockState,
        food: ItemStack,
    ) {
        val compostables = ComposterBlock.COMPOSTABLES
        val item = food.item
        val alreadyCompostable = compostables.containsKey(item)
        val originalChance = if (alreadyCompostable) compostables.getFloat(item) else 0.0f
        compostables.put(item, 1.0f)
        try {
            ComposterBlock.insertItem(event.entity, state, level, food, event.pos)
        } finally {
            if (alreadyCompostable) compostables.put(item, originalChance) else compostables.removeFloat(item)
        }
        giveRemainder(event, food)
        level.levelEvent(1500, event.pos, 1)
    }

    private fun consumeAndReturnRemainder(event: PlayerInteractEvent.RightClickBlock, food: ItemStack) {
        if (!event.entity.abilities.instabuild) {
            food.shrink(1)
            giveRemainder(event, food)
        }
    }

    private fun giveRemainder(event: PlayerInteractEvent.RightClickBlock, consumedFood: ItemStack) {
        if (event.entity.abilities.instabuild || !consumedFood.item.hasCraftingRemainingItem()) return
        val remainder = ItemStack(consumedFood.item.craftingRemainingItem)
        if (!event.entity.addItem(remainder)) {
            event.entity.drop(remainder, false)
        }
    }
}
