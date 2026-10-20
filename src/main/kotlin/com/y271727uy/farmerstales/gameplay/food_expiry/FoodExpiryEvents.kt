package com.y271727uy.farmerstales.gameplay.food_expiry

import com.y271727uy.farmerstales.FTMod
import com.y271727uy.farmerstales.config.Config
import com.y271727uy.farmerstales.gameplay.special_event.crimson_moon.CrimsonMoonEvents
import com.y271727uy.farmerstales.integration.IntegrationManager
import com.y271727uy.farmerstales.integration.SeasonCalendarDate
import net.minecraft.ChatFormatting
import net.minecraft.core.BlockPos
import net.minecraft.network.chat.Component
import net.minecraft.resources.ResourceLocation
import net.minecraft.server.level.ServerLevel
import net.minecraft.server.level.ServerPlayer
import net.minecraft.tags.ItemTags
import net.minecraft.tags.TagKey
import net.minecraft.world.effect.MobEffectInstance
import net.minecraft.world.effect.MobEffects
import net.minecraft.world.item.Item
import net.minecraft.world.entity.item.ItemEntity
import net.minecraft.world.item.ItemStack
import net.minecraft.world.level.Level
import net.minecraftforge.api.distmarker.Dist
import net.minecraftforge.event.TickEvent
import net.minecraftforge.event.entity.EntityJoinLevelEvent
import net.minecraftforge.event.entity.living.LivingEntityUseItemEvent
import net.minecraftforge.event.entity.player.ItemTooltipEvent
import net.minecraftforge.eventbus.api.SubscribeEvent
import net.minecraftforge.fml.common.Mod
import net.minecraftforge.server.ServerLifecycleHooks
import kotlin.math.ceil

/**
 * A lightweight food-expiry system. Per-stack expiry state is held in a Forge
 * capability so it cannot interfere with NBT-sensitive item models.
 */
object FoodExpiry {
    private const val TICKS_PER_DAY = 24_000L
    private const val TICKS_PER_HOUR = 1_000L
    private const val TICKS_PER_MINUTE = TICKS_PER_HOUR / 60f
    private const val DAYS_IN_MONTH = 8
    private const val MONTHS_IN_YEAR = 12
    private const val START_YEAR = 1000L
    private const val NEVER_EXPIRES = Long.MAX_VALUE
    private val MONTH_KEYS = arrayOf(
        "january", "february", "march", "april", "may", "june",
        "july", "august", "september", "october", "november", "december",
    )
    private val NEVER_EXPIRES_TAG = foodTag("never_expires")
    private val CATEGORY_DURATIONS = listOf(
        foodTag("preserved") to 30,
        foodTag("cooked_food") to 10,
        foodTag("raw_meat") to 5,
        foodTag("fruit") to 4,
        foodTag("grain") to 30,
    )

    enum class Freshness {
        FRESH,
        AGING,
        ROTTEN,
        UNTRACKED,
    }

    @JvmStatic
    fun initialize(stack: ItemStack, gameTime: Long) {
        initialize(stack, gameTime, 1.0)
    }

    /** Records the shelf life selected for this stack at its creation location. */
    @JvmStatic
    fun initialize(stack: ItemStack, gameTime: Long, biomeDecayMultiplier: Double) {
        if (!isTrackedFood(stack)) return
        val data = FoodExpiryCapabilities.get(stack) ?: return
        val duration = (shelfLifeTicks(stack).toDouble() / biomeDecayMultiplier).toLong().coerceAtLeast(1L)
        if (data.expiry == 0L) {
            val created = roundedCreationTime(gameTime)
            data.created = created
            data.duration = duration
            data.expiry = created + duration
            // Matching stacks must begin seasonal settlement at the same time.
            data.lastDecayUpdate = created
        } else {
            // Migrate stacks created by the first version, which only stored expiry.
            if (data.duration == 0L) data.duration = duration
            if (data.created == 0L) {
                data.created = data.expiry - data.duration
            }
            if (data.lastDecayUpdate == 0L) data.lastDecayUpdate = gameTime
        }
    }

    /** Applies elapsed seasonal decay before inspecting or using a food stack. */
    @JvmStatic
    fun settleSeasonalDecay(stack: ItemStack, level: Level) {
        settleSeasonalDecay(stack, level, null)
    }

    /**
     * @param exposedPos sky-exposed position of a carried or dropped stack. Null means the stack
     * is stored, so a crimson night does not hurry it.
     */
    @JvmStatic
    fun settleSeasonalDecay(stack: ItemStack, level: Level, exposedPos: BlockPos?) {
        val data = FoodExpiryCapabilities.get(stack) ?: return
        if (data.expiry == 0L || data.duration == NEVER_EXPIRES) return

        val now = level.gameTime
        val lastUpdate = data.lastDecayUpdate
        if (lastUpdate <= 0L) {
            data.lastDecayUpdate = now
            return
        }
        val elapsed = now - lastUpdate
        if (elapsed <= 0L) return

        val seasonalRate = decayRate(level, exposedPos)
        val expiryAdjustment = kotlin.math.round(elapsed * (seasonalRate - 1.0)).toLong()
        data.expiry -= expiryAdjustment
        data.lastDecayUpdate = now
    }

    @JvmStatic
    fun isExpired(stack: ItemStack, gameTime: Long): Boolean =
        FoodExpiryCapabilities.get(stack)?.expiry?.let { it != 0L && it <= gameTime } == true

    @JvmStatic
    fun isRotten(stack: ItemStack, gameTime: Long): Boolean {
        val data = FoodExpiryCapabilities.get(stack) ?: return false
        if (data.rotten) return true
        if (isExpired(stack, gameTime)) {
            data.rotten = true
            return true
        }
        return false
    }

    /**
     * Used by the global Ingredient hook. Recipe matching does not provide a
     * level, so the server's shared game time is used for unmarked stacks.
     */
    @JvmStatic
    fun isRottenForRecipe(stack: ItemStack?): Boolean {
        if (!Config.foodExpiryEnabled || stack == null || !isTrackedFood(stack)) return false
        val data = FoodExpiryCapabilities.get(stack) ?: return false
        if (data.rotten) return true
        val gameTime = ServerLifecycleHooks.getCurrentServer()?.overworld()?.gameTime ?: return false
        return isRotten(stack, gameTime)
    }

    @JvmStatic
    fun markRotten(stack: ItemStack): ItemStack {
        FoodExpiryCapabilities.get(stack)?.rotten = true
        return stack
    }

    @JvmStatic
    fun remainingDays(stack: ItemStack, gameTime: Long): Long? =
        FoodExpiryCapabilities.get(stack)?.takeIf {
            it.expiry != 0L && it.duration != 0L && it.duration != NEVER_EXPIRES
        }?.expiry?.let { expiry ->
            ceil((expiry - gameTime).toDouble() / TICKS_PER_DAY).toLong().coerceAtLeast(0)
        }

    /** TFC-style calendar expiry line: date plus approximate time remaining. */
    @JvmStatic
    fun expiryDateAndLeft(stack: ItemStack, gameTime: Long, level: Level? = null): Component? =
        FoodExpiryCapabilities.get(stack)?.takeIf {
            it.expiry != 0L && it.duration != 0L && it.duration != NEVER_EXPIRES
        }?.expiry?.let { expiry ->
            val remaining = (expiry - gameTime).coerceAtLeast(0L)
            val calendar = level?.let { IntegrationManager.calendarAt(it, expiry - gameTime) }
            Component.translatable(
                "tooltip.${FTMod.MODID}.food_expiry_date_and_left",
                calendarTimeAndDate(expiry, calendar),
                timeDelta(remaining, calendar),
            )
        }

    private fun calendarTimeAndDate(time: Long, calendar: SeasonCalendarDate?): Component {
        if (calendar != null) {
            return formatCalendar(
                calendar.hour,
                calendar.minute,
                calendar.monthIndex,
                calendar.dayOfMonth,
                calendar.year,
            )
        }
        val hour = ((time / TICKS_PER_HOUR) % 24).toInt()
        val minute = ((time % TICKS_PER_HOUR) / TICKS_PER_MINUTE).toInt()
        val day = 1 + ((time / TICKS_PER_DAY) % DAYS_IN_MONTH).toInt()
        val monthIndex = ((time / (DAYS_IN_MONTH * TICKS_PER_DAY)) % MONTHS_IN_YEAR).toInt()
        val year = START_YEAR + time / (MONTHS_IN_YEAR * DAYS_IN_MONTH * TICKS_PER_DAY)
        return formatCalendar(hour, minute, monthIndex, day, year)
    }

    private fun formatCalendar(hour: Int, minute: Int, monthIndex: Int, day: Int, year: Long): Component {
        return Component.translatable(
            "tooltip.${FTMod.MODID}.calendar_hour_minute_month_day_year",
            String.format("%d:%02d", hour, minute),
            Component.translatable("tooltip.${FTMod.MODID}.month.${MONTH_KEYS[monthIndex]}"),
            day,
            year,
        )
    }

    private fun timeDelta(ticks: Long, calendar: SeasonCalendarDate?): Component {
        val ticksPerDay = calendar?.dayDuration?.toLong()?.coerceAtLeast(1L) ?: TICKS_PER_DAY
        val daysInMonth = calendar?.daysInMonth?.coerceAtLeast(1) ?: DAYS_IN_MONTH
        val ticksPerHour = (ticksPerDay / 24).coerceAtLeast(1L)
        val hours = ticks / ticksPerHour
        if (hours < 1L) {
            return Component.translatable(
                "tooltip.${FTMod.MODID}.time_delta_hours_minutes",
                "00",
                String.format("%02d", ((ticks % ticksPerHour) / (ticksPerHour / 60f)).toInt()),
            )
        }
        val days = ticks / ticksPerDay
        if (days < 1L) {
            return Component.translatable(
                "tooltip.${FTMod.MODID}.time_delta_hours_minutes",
                hours,
                String.format("%02d", ((ticks % ticksPerHour) / (ticksPerHour / 60f)).toInt()),
            )
        }
        val months = ticks / (daysInMonth * ticksPerDay)
        if (months < 1L) {
            return Component.translatable("tooltip.${FTMod.MODID}.time_delta_days", days)
        }
        val years = ticks / (MONTHS_IN_YEAR * daysInMonth * ticksPerDay)
        if (years < 1L) {
            return Component.translatable(
                "tooltip.${FTMod.MODID}.time_delta_months_days",
                months,
                days % daysInMonth,
            )
        }
        return Component.translatable(
            "tooltip.${FTMod.MODID}.time_delta_years_months_days",
            years,
            months % MONTHS_IN_YEAR,
            days % daysInMonth,
        )
    }

    /** Returns the three gameplay grades used by integrations, or UNTRACKED. */
    @JvmStatic
    fun freshness(stack: ItemStack, gameTime: Long): Freshness {
        val data = FoodExpiryCapabilities.get(stack) ?: return Freshness.UNTRACKED
        if (data.rotten) return Freshness.ROTTEN
        if (data.expiry == 0L || data.duration == 0L) {
            return Freshness.UNTRACKED
        }
        val remaining = data.expiry - gameTime
        if (data.duration == NEVER_EXPIRES) return Freshness.FRESH
        if (remaining <= 0L) return Freshness.ROTTEN
        val fraction = remaining.toDouble() / data.duration.coerceAtLeast(1L)
        return if (fraction >= 2.0 / 3.0) Freshness.FRESH else Freshness.AGING
    }

    @JvmStatic
    fun remainingFraction(stack: ItemStack, gameTime: Long): Double? {
        val data = FoodExpiryCapabilities.get(stack) ?: return null
        if (data.expiry == 0L || data.duration == 0L) return null
        return ((data.expiry - gameTime).toDouble() / data.duration.coerceAtLeast(1L)).coerceIn(0.0, 1.0)
    }

    private fun isTrackedFood(stack: ItemStack): Boolean = !stack.isEmpty && stack.item.foodProperties != null

    private fun shelfLifeTicks(stack: ItemStack): Long {
        if (stack.`is`(NEVER_EXPIRES_TAG)) return NEVER_EXPIRES
        val categoryDays = CATEGORY_DURATIONS.firstOrNull { (tag, _) -> stack.`is`(tag) }?.second
        return (categoryDays ?: Config.foodShelfLifeDays) * TICKS_PER_DAY
    }

    /** Mirrors TFC: foods produced in one window receive one creation timestamp. */
    private fun roundedCreationTime(gameTime: Long): Long {
        val window = Config.foodExpiryStackWindowHours * TICKS_PER_HOUR
        return ((gameTime - 1L) / window + 1L) * window
    }

    private fun foodTag(path: String): TagKey<Item> =
        ItemTags.create(ResourceLocation(FTMod.MODID, "food/$path"))

    private fun biomeDecayMultiplier(level: Level, pos: BlockPos): Double = when (level.getBiome(pos).value().baseTemperature) {
        in Double.NEGATIVE_INFINITY..0.2 -> 0.5
        in 1.0..Double.POSITIVE_INFINITY -> 1.5
        else -> 1.0
    }

    @JvmStatic
    fun initializeForBiome(stack: ItemStack, gameTime: Long, level: Level, pos: BlockPos) {
        initialize(stack, gameTime, biomeDecayMultiplier(level, pos))
    }

    private fun decayRate(level: Level, exposedPos: BlockPos?): Double {
        val seasonal = when (IntegrationManager.currentSeasonName(level)) {
            "winter" -> 0.5
            "summer" -> 1.2
            else -> 1.0
        }
        val serverLevel = level as? ServerLevel ?: return seasonal
        if (exposedPos == null || !CrimsonMoonEvents.isActive(serverLevel) || !serverLevel.canSeeSky(exposedPos)) {
            return seasonal
        }
        return seasonal * CRIMSON_OUTDOOR_DECAY
    }

    private const val CRIMSON_OUTDOOR_DECAY = 2.0

}

@Mod.EventBusSubscriber(modid = FTMod.MODID, bus = Mod.EventBusSubscriber.Bus.FORGE)
@Suppress("unused")
object FoodExpiryServerEvents {
    @JvmStatic
    @SubscribeEvent
    fun onPlayerTick(event: TickEvent.PlayerTickEvent) {
        if (!Config.foodExpiryEnabled || event.phase != TickEvent.Phase.END) return
        val player = event.player as? ServerPlayer ?: return
        val gameTime = player.level().gameTime
        val pos = player.blockPosition()
        sequenceOf(player.inventory.items, player.inventory.armor, player.inventory.offhand)
            .flatten()
            .forEach { stack ->
                FoodExpiry.initializeForBiome(stack, gameTime, player.level(), pos)
                FoodExpiry.settleSeasonalDecay(stack, player.level(), player.blockPosition())
            }
    }

    @JvmStatic
    @SubscribeEvent
    fun onItemJoinLevel(event: EntityJoinLevelEvent) {
        if (!Config.foodExpiryEnabled || event.level.isClientSide) return
        val itemEntity = event.entity as? ItemEntity ?: return
        FoodExpiry.initializeForBiome(itemEntity.item, event.level.gameTime, event.level, itemEntity.blockPosition())
        FoodExpiry.settleSeasonalDecay(itemEntity.item, event.level, itemEntity.blockPosition())
    }

    @JvmStatic
    @SubscribeEvent
    fun onUseFood(event: LivingEntityUseItemEvent.Start) {
        if (!Config.foodExpiryEnabled || event.entity.level().isClientSide) return
        val level = event.entity.level() as? ServerLevel ?: return
        FoodExpiry.initializeForBiome(event.item, level.gameTime, level, event.entity.blockPosition())
        FoodExpiry.settleSeasonalDecay(event.item, level, event.entity.blockPosition())
        FoodExpiry.isRotten(event.item, level.gameTime)
    }

    @JvmStatic
    @SubscribeEvent
    fun onFoodFinished(event: LivingEntityUseItemEvent.Finish) {
        if (!Config.foodExpiryEnabled || event.entity.level().isClientSide) return
        val level = event.entity.level() as? ServerLevel ?: return
        val food = event.item
        FoodExpiry.settleSeasonalDecay(food, level, event.entity.blockPosition())
        if (FoodExpiry.isRotten(food, level.gameTime)) {
            if (level.random.nextFloat() < 0.50f) {
                event.entity.addEffect(MobEffectInstance(MobEffects.POISON, 200, 0))
            }
            if (level.random.nextFloat() < 0.70f) {
                event.entity.addEffect(MobEffectInstance(MobEffects.HUNGER, 600, 0))
            }
            return
        }

        if (FoodExpiry.freshness(food, level.gameTime) == FoodExpiry.Freshness.FRESH) {
            val player = event.entity as? ServerPlayer ?: return
            if (level.random.nextBoolean()) {
                player.addEffect(MobEffectInstance(MobEffects.REGENERATION, 200, 0))
            } else {
                player.foodData.eat(0, 2.0f)
            }
        }
    }

}

@Mod.EventBusSubscriber(modid = FTMod.MODID, bus = Mod.EventBusSubscriber.Bus.FORGE, value = [Dist.CLIENT])
@Suppress("unused")
object FoodExpiryTooltipEvents {
    @JvmStatic
    @SubscribeEvent
    fun onItemTooltip(event: ItemTooltipEvent) {
        if (!Config.foodExpiryEnabled) return
        val level = event.entity?.level() ?: return
        when (FoodExpiry.freshness(event.itemStack, level.gameTime)) {
            FoodExpiry.Freshness.FRESH -> event.toolTip.add(Component.translatable("tooltip.${FTMod.MODID}.food_freshness.fresh").withStyle(ChatFormatting.GREEN))
            FoodExpiry.Freshness.AGING -> event.toolTip.add(Component.translatable("tooltip.${FTMod.MODID}.food_freshness.aging").withStyle(ChatFormatting.YELLOW))
            FoodExpiry.Freshness.ROTTEN -> {
                event.toolTip.add(Component.translatable("tooltip.${FTMod.MODID}.food_freshness.rotten").withStyle(ChatFormatting.RED))
                return
            }
            FoodExpiry.Freshness.UNTRACKED -> return
        }
        FoodExpiry.expiryDateAndLeft(event.itemStack, level.gameTime, level)?.let { line ->
            event.toolTip.add(line.copy().withStyle(ChatFormatting.DARK_GREEN))
        }
    }
}
