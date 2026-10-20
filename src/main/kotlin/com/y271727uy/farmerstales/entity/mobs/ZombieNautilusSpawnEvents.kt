package com.y271727uy.farmerstales.entity.mobs

import com.y271727uy.farmerstales.FTMod
import com.y271727uy.farmerstales.entity.ModEntities
import net.minecraft.core.BlockPos
import net.minecraft.tags.FluidTags
import net.minecraft.world.entity.MobSpawnType
import net.minecraft.world.entity.monster.Drowned
import net.minecraft.world.level.ServerLevelAccessor
import net.minecraftforge.event.entity.living.LivingEvent
import net.minecraftforge.event.entity.living.MobSpawnEvent
import net.minecraftforge.eventbus.api.SubscribeEvent
import net.minecraftforge.fml.common.Mod

/** Adds the distinctive Mounts of Mayhem-style drowned rider without adding loose undead mounts. */
@Mod.EventBusSubscriber(modid = FTMod.MODID)
object ZombieNautilusSpawnEvents {
    private const val DROWNED_MOUNT_CHANCE = 0.08F

    @JvmStatic
    @SubscribeEvent
    fun addDrownedMount(event: MobSpawnEvent.FinalizeSpawn) {
        if (event.spawnType != MobSpawnType.NATURAL || event.entity !is Drowned) return
        val drowned = event.entity as Drowned
        val level = event.level as? ServerLevelAccessor ?: return
        val pos = BlockPos.containing(drowned.position())
        if (!level.getFluidState(pos).`is`(FluidTags.WATER) || drowned.random.nextFloat() >= DROWNED_MOUNT_CHANCE) return

        val mount = ModEntities.ZOMBIE_NAUTILUS.type.get().create(drowned.level()) ?: return
        mount.moveTo(drowned.x, drowned.y, drowned.z, drowned.yRot, 0.0F)
        if (drowned.level().addFreshEntity(mount)) drowned.startRiding(mount, true)
    }

    /** The mount owns movement and rotation; the Drowned is only a visual passenger. */
    @JvmStatic
    @SubscribeEvent
    fun alignDrownedRider(event: LivingEvent.LivingTickEvent) {
        val drowned = event.entity as? Drowned ?: return
        val mount = drowned.vehicle as? ZombieNautilusEntity ?: return
        drowned.yRot = mount.yRot
        drowned.yRotO = mount.yRot
        drowned.yBodyRot = mount.yRot
        drowned.yHeadRot = mount.yRot
    }
}
