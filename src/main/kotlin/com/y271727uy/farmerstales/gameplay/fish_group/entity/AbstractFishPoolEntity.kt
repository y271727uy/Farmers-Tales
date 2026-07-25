package com.y271727uy.farmerstales.gameplay.fish_group.entity

import com.y271727uy.farmerstales.FTMod
import com.y271727uy.farmerstales.gameplay.fish_group.pool.FishKingCooldownSavedData
import com.y271727uy.farmerstales.gameplay.fish_group.pool.FishPoolDefinition
import com.y271727uy.farmerstales.gameplay.fish_group.pool.FishPoolFactory
import com.y271727uy.farmerstales.gameplay.fish_group.pool.FishPoolLootManager
import net.minecraft.nbt.CompoundTag
import net.minecraft.nbt.Tag
import net.minecraft.resources.ResourceLocation
import net.minecraft.server.level.ServerLevel
import net.minecraft.util.Mth
import net.minecraft.world.entity.EntityType
import net.minecraft.world.entity.player.Player
import net.minecraft.world.item.ItemStack
import net.minecraft.world.item.Items
import net.minecraft.world.level.Level
import net.minecraft.world.phys.AABB
import net.minecraftforge.registries.ForgeRegistries

abstract class AbstractFishPoolEntity(type: EntityType<out FishPoolBaseEntity>, level: Level) :
    FishPoolBaseEntity(type, level) {
    private var definitionId: ResourceLocation? = null
    private var currentFishCount = 0
    private var currentMaxFishCount = 0
    private var fishKingAwarded = false

    protected abstract val environment: FishPoolDefinition.Environment
    protected abstract val defaultFishPoolId: ResourceLocation

    protected fun resolveDefinitionIdFromType(type: EntityType<*>): ResourceLocation {
        val typeId = ForgeRegistries.ENTITY_TYPES.getKey(type)
        return if (typeId != null && FishPoolFactory.get(typeId).isPresent) typeId else defaultFishPoolId
    }

    fun setFishPoolDefinition(fishPoolId: ResourceLocation) {
        val definition = FishPoolFactory.getOrDefault(fishPoolId, environment)
        val shouldRollFishCount = definition.id != definitionId || currentMaxFishCount <= 0
        definitionId = definition.id
        currentMaxFishCount = if (shouldRollFishCount) {
            definition.rollFishCount(random)
        } else {
            Mth.clamp(currentMaxFishCount, definition.minFishCount, definition.maxFishCount)
        }
        currentFishCount = Mth.clamp(currentFishCount, 0, currentMaxFishCount)
    }

    val fishPoolId: ResourceLocation
        get() {
            ensureFishPoolDefinition()
            return requireNotNull(definitionId)
        }

    val fishCount: Int
        get() = currentFishCount

    val maxFishCount: Int
        get() {
            ensureFishPoolDefinition()
            return currentMaxFishCount
        }

    val resolvedFishPoolDefinition: FishPoolDefinition
        get() = fishPoolDefinition

    fun appendJadeServerData(tag: CompoundTag, serverLevel: ServerLevel) {
        val definition = fishPoolDefinition
        tag.putString("fishKingKey", resolveFishKingTranslationKey(definition))
        tag.putString("weatherKey", resolveWeatherTranslationKey(definition))
        tag.putString("timeKey", resolveTimeTranslationKey(definition))
        tag.putString("stateKey", resolveJadeStateTranslationKey(serverLevel, definition))
    }

    override fun getHookInteractionBounds(): AABB = boundingBox.inflate(0.75, 2.0, 0.75)

    override fun tick() {
        super.tick()
        if (level().isClientSide || isRemoved || isDestroying) return
        val serverLevel = level() as? ServerLevel ?: return
        if (!fishPoolDefinition.matchesCurrentConditions(serverLevel)) removeWithEffects(serverLevel)
    }

    override fun readAdditionalSaveData(compound: CompoundTag) {
        super.readAdditionalSaveData(compound)
        definitionId = if (compound.contains(FISH_POOL_ID_TAG, Tag.TAG_STRING.toInt())) {
            ResourceLocation.tryParse(compound.getString(FISH_POOL_ID_TAG)) ?: defaultFishPoolId
        } else {
            defaultFishPoolId
        }
        currentFishCount = compound.getInt(FISH_COUNT_TAG).coerceAtLeast(0)
        fishKingAwarded = compound.getBoolean(FISH_KING_AWARDED_TAG)
        currentMaxFishCount = if (compound.contains(MAX_FISH_COUNT_TAG, Tag.TAG_INT.toInt())) {
            compound.getInt(MAX_FISH_COUNT_TAG).coerceAtLeast(1)
        } else {
            fishPoolDefinition.rollFishCount(random)
        }
        currentFishCount = Mth.clamp(currentFishCount, 0, currentMaxFishCount)
    }

    override fun addAdditionalSaveData(compound: CompoundTag) {
        super.addAdditionalSaveData(compound)
        compound.putString(FISH_POOL_ID_TAG, fishPoolId.toString())
        compound.putInt(FISH_COUNT_TAG, currentFishCount)
        compound.putInt(MAX_FISH_COUNT_TAG, maxFishCount)
        compound.putBoolean(FISH_KING_AWARDED_TAG, fishKingAwarded)
    }

    override fun onFishHookInteract(player: Player) {
        val serverLevel = level() as? ServerLevel ?: return
        if (isRemoved || isDestroying) return
        val definition = fishPoolDefinition
        if (!definition.matchesFishing(serverLevel)) return
        if (currentFishCount >= maxFishCount) {
            removeWithEffects(serverLevel)
            return
        }

        val reward = FishPoolLootManager.rollReward(fishPoolId, serverLevel, serverLevel.random)
        if (reward.isEmpty) {
            FTMod.LOGGER.warn("Fish pool {} produced no reward", fishPoolId)
            return
        }
        giveReward(player, reward)
        tryAwardFishKing(serverLevel, player, definition)
        triggerHurt()
        currentFishCount++
        if (currentFishCount >= maxFishCount) removeWithEffects(serverLevel)
    }

    private fun ensureFishPoolDefinition() {
        if (definitionId == null) setFishPoolDefinition(defaultFishPoolId)
    }

    private val fishPoolDefinition: FishPoolDefinition
        get() {
            ensureFishPoolDefinition()
            return FishPoolFactory.getOrDefault(requireNotNull(definitionId), environment).also { definitionId = it.id }
        }

    private fun resolveFishKingTranslationKey(definition: FishPoolDefinition): String = when {
        definition.fishKing == null -> "tooltip.farmerstales.fish_pool.fish_king.none"
        fishKingAwarded -> "tooltip.farmerstales.fish_pool.fish_king.awarded"
        else -> "tooltip.farmerstales.fish_pool.fish_king.available"
    }

    private fun resolveWeatherTranslationKey(definition: FishPoolDefinition): String =
        when (definition.weatherRequirement) {
            FishPoolDefinition.WeatherRequirement.CLEAR -> "tooltip.farmerstales.fish_pool.weather.clear"
            FishPoolDefinition.WeatherRequirement.RAIN -> "tooltip.farmerstales.fish_pool.weather.rain"
            FishPoolDefinition.WeatherRequirement.THUNDER -> "tooltip.farmerstales.fish_pool.weather.thunder"
            FishPoolDefinition.WeatherRequirement.ANY -> "tooltip.farmerstales.fish_pool.weather.any"
        }

    private fun resolveTimeTranslationKey(definition: FishPoolDefinition): String =
        when (definition.timeRequirement) {
            FishPoolDefinition.TimeRequirement.DAY -> "tooltip.farmerstales.fish_pool.time.day"
            FishPoolDefinition.TimeRequirement.NIGHT -> "tooltip.farmerstales.fish_pool.time.night"
            FishPoolDefinition.TimeRequirement.ANY -> "tooltip.farmerstales.fish_pool.time.any"
        }

    private fun resolveJadeStateTranslationKey(serverLevel: ServerLevel, definition: FishPoolDefinition): String =
        when {
            isHookInteracting() -> "tooltip.farmerstales.fish_pool.state.fishing"
            definition.matchesFishing(serverLevel) -> "tooltip.farmerstales.fish_pool.state.available"
            else -> "tooltip.farmerstales.fish_pool.state.unavailable"
        }

    private fun tryAwardFishKing(serverLevel: ServerLevel, player: Player, definition: FishPoolDefinition) {
        val fishKingId = definition.fishKing ?: return
        if (fishKingAwarded) return
        val cooldownData = FishKingCooldownSavedData.get(serverLevel)
        if (!cooldownData.canAward(player, serverLevel.gameTime) || serverLevel.random.nextInt(100) != 0) return

        val fishKingItem = ForgeRegistries.ITEMS.getValue(fishKingId)
        if (fishKingItem == null || fishKingItem == Items.AIR) {
            FTMod.LOGGER.warn("Fish pool {} is configured with an invalid FishKing item {}", definition.id, fishKingId)
            return
        }
        giveReward(player, ItemStack(fishKingItem))
        fishKingAwarded = true
        cooldownData.markAwarded(player, serverLevel.gameTime)
    }

    fun isFishKingAwarded(): Boolean = fishKingAwarded

    private fun giveReward(player: Player, stack: ItemStack) {
        if (!player.addItem(stack)) player.drop(stack, false)
    }

    companion object {
        private const val FISH_POOL_ID_TAG = "FishPoolId"
        private const val FISH_COUNT_TAG = "FishCount"
        private const val MAX_FISH_COUNT_TAG = "MaxFishCount"
        private const val FISH_KING_AWARDED_TAG = "FishKingAwarded"
    }
}
