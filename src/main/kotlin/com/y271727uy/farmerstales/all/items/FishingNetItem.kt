package com.y271727uy.farmerstales.all.items

import net.minecraft.core.BlockPos
import net.minecraft.core.registries.Registries
import net.minecraft.resources.ResourceLocation
import net.minecraft.server.level.ServerLevel
import net.minecraft.sounds.SoundEvents
import net.minecraft.sounds.SoundSource
import net.minecraft.tags.BiomeTags
import net.minecraft.tags.TagKey
import net.minecraft.world.InteractionResult
import net.minecraft.world.entity.EntityType
import net.minecraft.world.entity.EquipmentSlot
import net.minecraft.world.entity.LivingEntity
import net.minecraft.world.item.Item
import net.minecraft.world.item.ItemStack
import net.minecraft.world.item.Items
import net.minecraft.world.item.context.UseOnContext
import net.minecraft.world.level.Level
import net.minecraft.world.level.block.Block
import net.minecraft.world.level.block.Blocks
import net.minecraft.world.level.block.state.BlockState
import net.minecraft.world.phys.Vec3
import net.minecraftforge.registries.ForgeRegistries

/** A hand-held net for collecting environment-specific fish roe from aquatic plants. */
class FishingNetItem(properties: Item.Properties) : Item(properties.durability(MAX_DURABILITY)) {
    override fun useOn(context: UseOnContext): InteractionResult {
        val level = context.level
        val pos = context.clickedPos
        val harvestKind = findHarvestKind(level, pos, level.getBlockState(pos))
            ?: return InteractionResult.PASS

        val player = context.player ?: return InteractionResult.PASS
        if (player.cooldowns.isOnCooldown(this)) {
            return InteractionResult.sidedSuccess(level.isClientSide)
        }
        if (level.isClientSide) return InteractionResult.sidedSuccess(true)

        val serverLevel = level as? ServerLevel ?: return InteractionResult.PASS
        player.cooldowns.addCooldown(this, HARVEST_COOLDOWN_TICKS)

        val roeTag = when (harvestKind) {
            HarvestKind.SEA -> SEA_ROE_TAG
            HarvestKind.RIVER -> RIVER_ROE_TAG
            HarvestKind.LAVA -> LAVA_ROE_TAG
        }
        val drop = if (serverLevel.random.nextBoolean()) {
            rollRoeDrop(serverLevel, roeTag)
        } else {
            rollGarbageDrop(serverLevel)
        }
        if (!drop.isEmpty) Block.popResource(serverLevel, pos, drop)

        serverLevel.playSound(null, pos, SoundEvents.ARMOR_EQUIP_LEATHER, SoundSource.PLAYERS, 1.0f, 1.1f)
        if (!player.abilities.instabuild) {
            context.itemInHand.hurtAndBreak(1, player) { broken -> broken.broadcastBreakEvent(context.hand) }
        }

        val brokeKelpPlant = harvestKind == HarvestKind.SEA &&
            serverLevel.getBlockState(pos).`is`(Blocks.KELP_PLANT)
        if (serverLevel.random.nextFloat() < harvestKind.breakChance) {
            serverLevel.destroyBlock(pos, false)
            if (brokeKelpPlant && serverLevel.random.nextFloat() < PUFFERFISH_SPAWN_CHANCE) {
                spawnPufferfish(serverLevel, pos)
            }
        }

        return InteractionResult.SUCCESS
    }

    override fun getDestroySpeed(stack: ItemStack, state: BlockState): Float = 1.0f

    override fun mineBlock(
        stack: ItemStack,
        level: Level,
        state: BlockState,
        pos: BlockPos,
        entity: LivingEntity,
    ): Boolean {
        stack.hurtAndBreak(1, entity) { broken -> broken.broadcastBreakEvent(EquipmentSlot.MAINHAND) }
        return true
    }

    override fun hurtEnemy(stack: ItemStack, target: LivingEntity, attacker: LivingEntity): Boolean {
        stack.hurtAndBreak(2, attacker) { broken -> broken.broadcastBreakEvent(EquipmentSlot.MAINHAND) }
        return true
    }

    private fun findHarvestKind(level: Level, pos: BlockPos, state: BlockState): HarvestKind? {
        val blockId = ForgeRegistries.BLOCKS.getKey(state.block)
        if (level.dimension() == Level.NETHER) {
            return when (blockId) {
                WARPED_KELP_ID, WARPED_KELP_PLANT_ID,
                CRIMSON_KELP_ID, CRIMSON_KELP_PLANT_ID,
                WARPED_SEAGRASS_ID, TALL_WARPED_SEAGRASS_ID,
                CRIMSON_SEAGRASS_ID, TALL_CRIMSON_SEAGRASS_ID -> HarvestKind.LAVA
                else -> null
            }
        }

        val biome = level.getBiome(pos)
        return when {
            biome.`is`(BiomeTags.IS_OCEAN) && (state.`is`(Blocks.KELP) || state.`is`(Blocks.KELP_PLANT)) -> {
                HarvestKind.SEA
            }

            biome.`is`(BiomeTags.IS_RIVER) && (
                state.`is`(Blocks.SEAGRASS) ||
                    state.`is`(Blocks.TALL_SEAGRASS) ||
                    blockId == WATERGRASS_ID
                ) -> {
                HarvestKind.RIVER
            }

            else -> null
        }
    }

    private fun rollRoeDrop(level: ServerLevel, tag: TagKey<Item>): ItemStack {
        val itemRegistry = level.registryAccess().registryOrThrow(Registries.ITEM)
        val taggedItems = itemRegistry.getTag(tag)
            .map { named -> named.map { holder -> holder.value() }.filter { it != Items.AIR } }
            .orElse(emptyList())
        if (taggedItems.isEmpty()) return ItemStack.EMPTY
        return ItemStack(taggedItems[level.random.nextInt(taggedItems.size)])
    }

    private fun rollGarbageDrop(level: ServerLevel): ItemStack =
        ItemStack(GARBAGE_ITEMS[level.random.nextInt(GARBAGE_ITEMS.size)])

    private fun spawnPufferfish(level: ServerLevel, pos: BlockPos) {
        val fish = EntityType.PUFFERFISH.create(level) ?: return
        fish.moveTo(pos.x + 0.5, pos.y.toDouble(), pos.z + 0.5, 0.0f, 0.0f)
        fish.deltaMovement = Vec3.ZERO
        level.addFreshEntity(fish)
    }

    private enum class HarvestKind(val breakChance: Float) {
        SEA(SEA_BREAK_CHANCE),
        RIVER(RIVER_BREAK_CHANCE),
        LAVA(LAVA_BREAK_CHANCE),
    }

    companion object {
        private const val MAX_DURABILITY = 92
        private const val HARVEST_COOLDOWN_TICKS = 50
        private const val SEA_BREAK_CHANCE = 0.20f
        private const val RIVER_BREAK_CHANCE = 0.05f
        private const val LAVA_BREAK_CHANCE = 0.20f
        private const val PUFFERFISH_SPAWN_CHANCE = 0.20f
        private const val TAG_NAMESPACE = "list"
        private val GARBAGE_ITEMS = listOf(
            Items.BOWL,
            Items.BONE,
            Items.STICK,
            Items.ROTTEN_FLESH,
            Items.LEATHER_BOOTS,
            Items.INK_SAC,
            Items.STRING,
            Items.CLAY_BALL,
        )

        private val SEA_ROE_TAG = itemTag("sea_roe")
        // Keep the tag path exactly as specified by the list integration: reiver_roe.
        private val RIVER_ROE_TAG = itemTag("reiver_roe")
        private val LAVA_ROE_TAG = itemTag("lava_roe")

        private val WATERGRASS_ID = ResourceLocation.fromNamespaceAndPath(
            "projectvibrantjourneys",
            "watergrass",
        )
        private val WARPED_SEAGRASS_ID = ResourceLocation.fromNamespaceAndPath(
            "netherdepthsupgrade",
            "warped_seagrass",
        )
        private val TALL_WARPED_SEAGRASS_ID = ResourceLocation.fromNamespaceAndPath(
            "netherdepthsupgrade",
            "tall_warped_seagrass",
        )
        private val CRIMSON_SEAGRASS_ID = ResourceLocation.fromNamespaceAndPath(
            "netherdepthsupgrade",
            "crimson_seagrass",
        )
        private val TALL_CRIMSON_SEAGRASS_ID = ResourceLocation.fromNamespaceAndPath(
            "netherdepthsupgrade",
            "tall_crimson_seagrass",
        )
        private val WARPED_KELP_PLANT_ID = ResourceLocation.fromNamespaceAndPath(
            "netherdepthsupgrade",
            "warped_kelp_plant",
        )
        private val CRIMSON_KELP_PLANT_ID = ResourceLocation.fromNamespaceAndPath(
            "netherdepthsupgrade",
            "crimson_kelp_plant",
        )
        private val WARPED_KELP_ID = ResourceLocation.fromNamespaceAndPath(
            "netherdepthsupgrade",
            "warped_kelp",
        )
        private val CRIMSON_KELP_ID = ResourceLocation.fromNamespaceAndPath(
            "netherdepthsupgrade",
            "crimson_kelp",
        )

        private fun itemTag(path: String): TagKey<Item> =
            TagKey.create(Registries.ITEM, ResourceLocation.fromNamespaceAndPath(TAG_NAMESPACE, path))
    }
}
