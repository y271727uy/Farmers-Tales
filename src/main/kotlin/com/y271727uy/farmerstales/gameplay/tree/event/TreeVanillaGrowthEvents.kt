package com.y271727uy.farmerstales.gameplay.tree.event

import com.y271727uy.farmerstales.FTMod
import net.minecraft.world.level.Level
import net.minecraftforge.event.level.SaplingGrowTreeEvent
import net.minecraftforge.eventbus.api.Event
import net.minecraftforge.eventbus.api.SubscribeEvent
import net.minecraftforge.fml.common.Mod

@Mod.EventBusSubscriber(modid = FTMod.MODID, bus = Mod.EventBusSubscriber.Bus.FORGE)
@Suppress("unused")
object TreeVanillaGrowthEvents {

	@JvmStatic
	@SubscribeEvent
	fun onSaplingGrowTree(event: SaplingGrowTreeEvent) {
		val level = event.level as? Level ?: return
		if (level.isClientSide) {
			return
		}

		if (!TreeGrowthEvents.isManagedSapling(level, event.pos)) {
			return
		}

		event.result = Event.Result.DENY
	}
}
