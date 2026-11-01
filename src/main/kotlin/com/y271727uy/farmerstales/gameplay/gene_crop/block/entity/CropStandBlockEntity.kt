package com.y271727uy.farmerstales.gameplay.gene_crop.block.entity

import com.y271727uy.farmerstales.gameplay.gene_crop.GeneCropGenome
import com.y271727uy.farmerstales.gameplay.gene_crop.GeneCropEvents
import com.y271727uy.farmerstales.gameplay.gene_crop.CropTemperatureStatus
import com.y271727uy.farmerstales.integration.forestry.genome.CropTemperature
import com.y271727uy.farmerstales.config.Config
import com.y271727uy.farmerstales.gameplay.gene_crop.pollen.CropPollenDefinitions
import com.y271727uy.farmerstales.gameplay.quality_crop.QualityHarvestContext
import net.minecraft.core.BlockPos
import net.minecraft.core.particles.ParticleTypes
import net.minecraft.core.registries.BuiltInRegistries
import net.minecraft.nbt.CompoundTag
import net.minecraft.nbt.NbtUtils
import net.minecraft.network.protocol.game.ClientboundBlockEntityDataPacket
import net.minecraft.server.level.ServerLevel
import net.minecraft.util.RandomSource
import net.minecraft.world.entity.player.Player
import net.minecraft.world.item.ItemStack
import net.minecraft.world.item.BlockItem
import net.minecraft.world.level.block.Block
import net.minecraft.world.level.block.Blocks
import net.minecraft.world.level.block.CropBlock
import net.minecraft.world.level.block.BeetrootBlock
import net.minecraft.world.level.block.FarmBlock
import net.minecraft.world.level.block.entity.BlockEntity
import net.minecraft.world.level.block.entity.BlockEntityType
import net.minecraft.world.level.block.state.BlockState
import net.minecraftforge.common.ForgeHooks
import net.minecraftforge.common.IPlantable
import net.minecraftforge.fml.ModList

/** The planted seed is retained intact; only surplus harvest drops leave the stand. */
class CropStandBlockEntity(type: BlockEntityType<*>, pos: BlockPos, state: BlockState) : BlockEntity(type, pos, state) {
    private var seed = ItemStack.EMPTY
    private var pollenParent = ItemStack.EMPTY
    var cropState: BlockState = Blocks.AIR.defaultBlockState()
        private set

    fun isEmpty(): Boolean = seed.isEmpty
    fun isMature(): Boolean = (cropState.block as? CropBlock)?.isMaxAge(cropState) == true
    fun canGrow(): Boolean = !isEmpty() && !isMature() && cropState.block is CropBlock

    fun temperatureStatus(): CropTemperatureStatus {
        val serverLevel = level as? ServerLevel ?: return CropTemperatureStatus.SUITABLE
        if (!Config.geneCropTemperatureEffects || isEmpty() || !ModList.get().isLoaded("forestry")) {
            return CropTemperatureStatus.SUITABLE
        }
        return CropTemperature.at(serverLevel, worldPosition, seed)
    }

    fun canFertilize(): Boolean = canGrow() && temperatureStatus().growthMultiplier > 0.0F

    /** Defensive snapshot: ordinary seeds never become genetic parents implicitly. */
    fun breedingSeed(): ItemStack? = seed.takeIf {
        GeneCropGenome.isGeneSeed(it) && GeneCropGenome.isIdentified(it)
    }?.copy()

    fun canAcceptPollen(donor: ItemStack): Boolean = isMature() && pollenParent.isEmpty &&
        breedingSeed() != null && donor.`is`(seed.item) &&
        CropPollenDefinitions.supports(donor) && GeneCropGenome.isIdentified(donor)

    fun acceptPollen(donor: ItemStack): Boolean {
        if (level?.isClientSide != false || !canAcceptPollen(donor)) return false
        pollenParent = donor.copy().also { it.count = 1 }
        sync()
        (level as? ServerLevel)?.sendParticles(ParticleTypes.HAPPY_VILLAGER,
            worldPosition.x + 0.5, worldPosition.y + 0.65, worldPosition.z + 0.5,
            6, 0.25, 0.25, 0.25, 0.0)
        return true
    }

    /** Persisted donor snapshot for this harvest only; later visitors cannot replace it. */
    fun pollenFather(): ItemStack? = pollenParent.takeIf {
        !it.isEmpty && it.`is`(seed.item) && CropPollenDefinitions.supports(it) && GeneCropGenome.isIdentified(it)
    }?.copy()

    private fun plantingState(stack: ItemStack): BlockState? {
        val currentLevel = level ?: return null
        if (stack.isEmpty) return null
        val plant = when (val item = stack.item) {
            is BlockItem -> item.block.defaultBlockState()
            is IPlantable -> item.getPlant(currentLevel, worldPosition)
            else -> return null
        }
        val crop = plant.block as? CropBlock ?: return null
        return crop.getStateForAge(0).takeIf { it.canSurvive(currentLevel, worldPosition) }
    }

    fun canPlant(stack: ItemStack): Boolean = isEmpty() && plantingState(stack) != null

    fun plant(stack: ItemStack, creative: Boolean): Boolean {
        if (!isEmpty() || level?.isClientSide != false) return false
        val initialState = plantingState(stack) ?: return false
        seed = stack.copy().also { it.count = 1 }
        pollenParent = ItemStack.EMPTY
        cropState = initialState
        if (!creative) stack.shrink(1)
        sync()
        return true
    }

    fun grow(amount: Int) {
        if (level?.isClientSide != false || !canGrow()) return
        if (amount <= 0 || temperatureStatus().growthMultiplier <= 0.0F) return
        val crop = cropState.block as CropBlock
        cropState = crop.getStateForAge((crop.getAge(cropState) + amount).coerceIn(0, crop.maxAge))
        sync()
    }

    fun growNaturally(level: ServerLevel, random: RandomSource) {
        if (!canGrow() || !level.isAreaLoaded(worldPosition, 1) || level.getRawBrightness(worldPosition, 0) < 9) return
        if (!cropState.canSurvive(level, worldPosition)) return
        val climateMultiplier = temperatureStatus().growthMultiplier
        // Check outside ForgeHooks so an ALLOW growth event cannot bypass a frozen crop.
        if (climateMultiplier <= 0.0F || (climateMultiplier < 1.0F && random.nextFloat() >= climateMultiplier)) return
        if (cropState.block is BeetrootBlock && random.nextInt(3) == 0) return
        // Vanilla's soil fertility calculation; adjacent stands also count as crop neighbors.
        var speed = 1.0f
        for (x in -1..1) for (z in -1..1) {
            val soil = level.getBlockState(worldPosition.offset(x, -1, z))
            if (soil.block is FarmBlock) {
                var fertility = if (soil.getValue(FarmBlock.MOISTURE) > 0) 3.0f else 1.0f
                if (x != 0 || z != 0) fertility /= 4.0f
                speed += fertility
            }
        }
        fun sameCrop(x: Int, z: Int): Boolean {
            val at = worldPosition.offset(x, 0, z)
            val state = (level.getBlockEntity(at) as? CropStandBlockEntity)?.cropState ?: level.getBlockState(at)
            return state.`is`(cropState.block)
        }
        if (((sameCrop(-1, 0) || sameCrop(1, 0)) && (sameCrop(0, -1) || sameCrop(0, 1))) ||
            sameCrop(-1, -1) || sameCrop(-1, 1) || sameCrop(1, -1) || sameCrop(1, 1)) speed /= 2.0f
        val oldState = cropState
        if (ForgeHooks.onCropsGrowPre(level, worldPosition, oldState, random.nextInt((25.0f / speed).toInt() + 1) == 0)) {
            grow(1)
            ForgeHooks.onCropsGrowPost(level, worldPosition, oldState)
        }
    }

    fun fertilize(amount: Int) {
        val multiplier = temperatureStatus().growthMultiplier
        if (amount <= 0 || multiplier <= 0.0F) return
        grow((amount * multiplier).toInt().coerceAtLeast(1))
    }

    fun harvest(player: Player): Boolean {
        val currentLevel = level as? ServerLevel ?: return false
        if (!isMature()) return false
        val harvestedState = cropState
        val plantedSeed = seed.copy()
        val generation = GeneCropEvents.prepare(currentLevel, worldPosition, harvestedState)
        val drops = Block.getDrops(harvestedState, currentLevel, worldPosition, null, player, ItemStack.EMPTY)
        // Pay for replanting from the actual loot; zero seed drops means the stand becomes empty.
        val replacement = drops.firstOrNull { !it.isEmpty && it.`is`(plantedSeed.item) }
        if (replacement != null) {
            replacement.shrink(1)
            cropState = (harvestedState.block as CropBlock).getStateForAge(0)
        } else {
            clearPlant()
        }
        pollenParent = ItemStack.EMPTY
        sync()
        QualityHarvestContext.capture(currentLevel, player, harvestedState)
        try {
            drops.filterNot(ItemStack::isEmpty).forEach { drop ->
                if (drop.`is`(plantedSeed.item) && generation != null) {
                    drop.getOrCreateTag().put(GeneCropGenome.TAG, generation.copy())
                }
                Block.popResource(currentLevel, worldPosition, drop)
            }
        } finally {
            QualityHarvestContext.clear()
        }
        return true
    }

    /** Used by both manual uprooting and block removal. Clear first to prevent duplicate returns. */
    fun uproot() {
        val currentLevel = level as? ServerLevel ?: return
        if (isEmpty()) return
        val returned = seed.copy()
        clearPlant()
        sync()
        Block.popResource(currentLevel, worldPosition, returned)
    }

    private fun clearPlant() {
        seed = ItemStack.EMPTY
        pollenParent = ItemStack.EMPTY
        cropState = Blocks.AIR.defaultBlockState()
    }

    private fun sync() {
        setChanged()
        (level as? ServerLevel)?.sendBlockUpdated(worldPosition, blockState, blockState, Block.UPDATE_CLIENTS)
    }

    override fun saveAdditional(tag: CompoundTag) {
        super.saveAdditional(tag)
        if (!isEmpty()) {
            tag.put("Seed", seed.save(CompoundTag()))
            tag.put("Crop", NbtUtils.writeBlockState(cropState))
            if (!pollenParent.isEmpty) tag.put("PollenParent", pollenParent.save(CompoundTag()))
        }
    }

    override fun load(tag: CompoundTag) {
        super.load(tag)
        seed = ItemStack.of(tag.getCompound("Seed"))
        cropState = NbtUtils.readBlockState(BuiltInRegistries.BLOCK.asLookup(), tag.getCompound("Crop"))
        pollenParent = ItemStack.of(tag.getCompound("PollenParent"))
        if (seed.isEmpty || cropState.block !is CropBlock) clearPlant() else seed.count = 1
    }

    override fun getUpdateTag(): CompoundTag = CompoundTag().also(::saveAdditional)
    override fun getUpdatePacket(): ClientboundBlockEntityDataPacket = ClientboundBlockEntityDataPacket.create(this)
}
