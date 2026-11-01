package com.y271727uy.farmerstales.integration.forestry.pollen

import com.y271727uy.farmerstales.FTMod
import com.y271727uy.farmerstales.all.items.CropPollenItem
import com.y271727uy.farmerstales.gameplay.gene_crop.block.entity.CropStandBlockEntity
import com.y271727uy.farmerstales.gameplay.gene_crop.pollen.CropPollenDefinitions
import com.y271727uy.farmerstales.gameplay.gene_crop.pollen.CropPollenSample
import forestry.api.genetics.pollen.IPollen
import forestry.api.genetics.pollen.IPollenType
import net.minecraft.core.BlockPos
import net.minecraft.nbt.CompoundTag
import net.minecraft.nbt.Tag
import net.minecraft.resources.ResourceLocation
import net.minecraft.server.level.ServerLevel
import net.minecraft.world.item.ItemStack
import net.minecraft.world.level.LevelAccessor

/** A single Forestry adapter for all seeds admitted by the DSL's tags. */
object CropPollenType : IPollenType<CropPollenSample> {
    private val ID = ResourceLocation.fromNamespaceAndPath(FTMod.MODID, "crop")
    override fun id(): ResourceLocation = ID

    private fun stand(level: LevelAccessor, pos: BlockPos): CropStandBlockEntity? =
        if (level.hasChunkAt(pos)) level.getBlockEntity(pos) as? CropStandBlockEntity else null

    override fun canPollinate(level: LevelAccessor, pos: BlockPos, pollinator: Any?): Boolean {
        val stand = stand(level, pos) ?: return false
        val seed = stand.breedingSeed() ?: return false
        return stand.isMature() && CropPollenDefinitions.supports(seed)
    }

    override fun tryCollectPollen(level: LevelAccessor, pos: BlockPos, pollinator: Any?): IPollen<CropPollenSample>? {
        if (level !is ServerLevel || !canPollinate(level, pos, pollinator)) return null
        val seed = stand(level, pos)?.breedingSeed() ?: return null
        return Pollen(CropPollenSample(seed, pos.immutable(), level.dimension().location()))
    }

    override fun tryPollinate(level: LevelAccessor, pos: BlockPos, pollen: CropPollenSample, pollinator: Any?): Boolean {
        if (level !is ServerLevel || !pollen.isViable()) return false
        if (pollen.source == pos && pollen.dimension == level.dimension().location()) return false
        return stand(level, pos)?.acceptPollen(pollen.seed()) == true
    }

    override fun readNbt(nbt: Tag): IPollen<CropPollenSample> =
        Pollen(CropPollenSample.read(nbt as? CompoundTag ?: CompoundTag()))

    private class Pollen(private val sample: CropPollenSample) : IPollen<CropPollenSample> {
        override fun getType(): IPollenType<CropPollenSample> = CropPollenType
        override fun getPollen(): CropPollenSample = sample
        override fun writeNbt(): Tag = sample.write()
        override fun createStack(): ItemStack = if (sample.isViable()) CropPollenItem.create(sample) else ItemStack.EMPTY
    }
}
