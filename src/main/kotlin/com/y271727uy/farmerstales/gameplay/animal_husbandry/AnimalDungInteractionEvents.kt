package com.y271727uy.farmerstales.gameplay.animal_husbandry

import com.y271727uy.farmerstales.FTMod
import com.y271727uy.farmerstales.all.ModBlocks
import com.y271727uy.farmerstales.all.ModItems
import net.minecraft.server.level.ServerLevel
import net.minecraft.sounds.SoundEvents
import net.minecraft.sounds.SoundSource
import net.minecraft.world.InteractionResult
import net.minecraft.world.item.ItemStack
import net.minecraft.world.level.block.Blocks
import net.minecraft.world.level.block.ComposterBlock
import net.minecraftforge.common.ToolActions
import net.minecraftforge.event.entity.player.PlayerInteractEvent
import net.minecraftforge.eventbus.api.EventPriority
import net.minecraftforge.eventbus.api.SubscribeEvent
import net.minecraftforge.fml.common.Mod

/** Gives animal dung a small composting value and a shovel collection interaction. */
@Mod.EventBusSubscriber(modid = FTMod.MODID, bus = Mod.EventBusSubscriber.Bus.FORGE)
@Suppress("unused")
object AnimalDungInteractionEvents {
    private const val COMPOST_SUCCESS_CHANCE = 0.10f

    @JvmStatic
    @SubscribeEvent(priority = EventPriority.HIGHEST)
    fun onRightClickComposter(event: PlayerInteractEvent.RightClickBlock) {
        val level = event.level as? ServerLevel ?: return
        val state = level.getBlockState(event.pos)
        val dung = event.itemStack
        if (!state.`is`(Blocks.COMPOSTER) ||
            state.getValue(ComposterBlock.LEVEL) >= 7 ||
            dung.item != ModBlocks.ANIMAL_DUNG.get().asItem()
        ) {
            return
        }

        if (level.random.nextFloat() < COMPOST_SUCCESS_CHANCE) {
            val compostables = ComposterBlock.COMPOSTABLES
            val alreadyCompostable = compostables.containsKey(dung.item)
            val originalChance = compostables.getFloat(dung.item)
            compostables.put(dung.item, 1.0f)
            try {
                ComposterBlock.insertItem(event.entity, state, level, dung, event.pos)
            } finally {
                if (alreadyCompostable) compostables.put(dung.item, originalChance)
                else compostables.removeFloat(dung.item)
            }
            level.levelEvent(1500, event.pos, 1)
        } else {
            if (!event.entity.abilities.instabuild) dung.shrink(1)
            level.levelEvent(1500, event.pos, 0)
        }

        event.cancellationResult = InteractionResult.SUCCESS
        event.isCanceled = true
    }

    @JvmStatic
    @SubscribeEvent(priority = EventPriority.HIGHEST)
    fun onShovelDung(event: PlayerInteractEvent.RightClickBlock) {
        val level = event.level as? ServerLevel ?: return
        if (!level.getBlockState(event.pos).`is`(ModBlocks.ANIMAL_DUNG.get()) ||
            !event.itemStack.canPerformAction(ToolActions.SHOVEL_DIG)
        ) {
            return
        }

        val player = event.entity
        level.destroyBlock(event.pos, false)
        if (!player.abilities.instabuild) {
            event.itemStack.hurtAndBreak(1, player) { it.broadcastBreakEvent(event.hand) }
            val reward = ItemStack(ModItems.ANIMAL_DUNG.get())
            if (!player.addItem(reward)) player.drop(reward, false)
        }
        level.playSound(null, event.pos, SoundEvents.SHOVEL_FLATTEN, SoundSource.BLOCKS, 1.0f, 1.0f)
        event.cancellationResult = InteractionResult.SUCCESS
        event.isCanceled = true
    }
}
