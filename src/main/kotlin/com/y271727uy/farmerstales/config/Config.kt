package com.y271727uy.farmerstales.config

import com.y271727uy.farmerstales.FTMod
import net.minecraftforge.common.ForgeConfigSpec
import net.minecraftforge.eventbus.api.SubscribeEvent
import net.minecraftforge.fml.common.Mod
import net.minecraftforge.fml.event.config.ModConfigEvent

@Mod.EventBusSubscriber(modid = FTMod.Companion.MODID, bus = Mod.EventBusSubscriber.Bus.MOD)
object Config {
    private val builder = ForgeConfigSpec.Builder()

    private val restrictAnimalBreedingValue = builder
        .comment("Whether animal breeding is blocked outside each animal's breeding season")
        .define("restrictAnimalBreeding", true)

    private val animalBreedingLoveChanceValue = builder
        .comment("Chance for feeding a fully fed supported animal to trigger love mode")
        .defineInRange("animalBreedingLoveChance", 0.125, 0.0, 1.0)

    private val pigExtraBabyChanceValue = builder
        .comment("Chance for a pig breeding event to produce one additional piglet")
        .defineInRange("pigExtraBabyChance", 0.25, 0.0, 1.0)

    private val restrictFishingLootValue = builder
        .comment("Whether seasonal fishing weights apply to fishing catches")
        .define("restrictFishingLoot", true)

    private val sendActionBarFeedbackValue = builder
        .comment("Whether players receive an action bar hint when seasonal breeding is blocked")
        .define("sendActionBarFeedback", true)

    private val weatherAffectsCropGrowthValue = builder
        .comment("Whether rain and snow affect crop growth")
        .define("weatherAffectsCropGrowth", true)

    private val foodExpiryEnabledValue = builder
        .comment("Whether edible items expire after their configured shelf life")
        .define("foodExpiryEnabled", false)

    private val foodShelfLifeDaysValue = builder
        .comment("Number of Minecraft days edible items remain safe to eat when food expiry is enabled")
        .defineInRange("foodShelfLifeDays", 7, 1, 365)

    private val foodExpiryStackWindowHoursValue = builder
        .comment("Foods created within this many Minecraft hours share an expiry timestamp and can stack")
        .defineInRange("foodExpiryStackWindowHours", 6, 1, 100)

    private val rainGrowthBonusChanceValue = builder
        .comment("Chance for rain-exposed crops to receive one extra random growth tick")
        .defineInRange("rainGrowthBonusChance", 0.35, 0.0, 1.0)

    private val ironCropQualityChanceValue = builder
        .comment("Chance for an eligible crop drop to receive iron quality; crop quality chances should total at most 1.0")
        .defineInRange("ironCropQualityChance", 0.15, 0.0, 1.0)

    private val goldCropQualityChanceValue = builder
        .comment("Chance for an eligible crop drop to receive gold quality; crop quality chances should total at most 1.0")
        .defineInRange("goldCropQualityChance", 0.10, 0.0, 1.0)

    private val diamondCropQualityChanceValue = builder
        .comment("Chance for an eligible crop drop to receive diamond quality; crop quality chances should total at most 1.0")
        .defineInRange("diamondCropQualityChance", 0.05, 0.0, 1.0)

    private val requireScytheForCropQualityValue = builder
        .comment("Whether eligible crop drops require a Farmers' Tales scythe in the main hand to receive quality")
        .define("requireScytheForCropQuality", true)

    private val mdIronCropQualityChanceValue = builder
        .comment("Chance for an eligible MD crop drop to receive iron quality; MD crop quality chances should total at most 1.0")
        .defineInRange("mdIronCropQualityChance", 0.35, 0.0, 1.0)

    private val mdGoldCropQualityChanceValue = builder
        .comment("Chance for an eligible MD crop drop to receive gold quality; MD crop quality chances should total at most 1.0")
        .defineInRange("mdGoldCropQualityChance", 0.10, 0.0, 1.0)

    private val mdDiamondCropQualityChanceValue = builder
        .comment("Chance for an eligible MD crop drop to receive diamond quality; MD crop quality chances should total at most 1.0")
        .defineInRange("mdDiamondCropQualityChance", 0.05, 0.0, 1.0)

    private val metabolismSettlementsPerTickValue = builder
        .comment("Maximum domesticated animals whose daily metabolism is settled in one server tick")
        .defineInRange("metabolismSettlementsPerTick", 64, 1, 1_024)

    private val wildAnimalAvoidanceScanIntervalTicksValue = builder
        .comment("Ticks between local wild-animal avoidance scans for each eligible player")
        .defineInRange("wildAnimalAvoidanceScanIntervalTicks", 5, 1, 100)

    private val tropicalClimateEventsEnabledValue = builder
        .comment("Whether tropical season transitions can trigger monsoons and droughts")
        .define("tropicalClimateEventsEnabled", true)

    private val tropicalMonsoonChanceValue = builder
        .comment("Chance for a wet-season transition to trigger a monsoon")
        .defineInRange("tropicalMonsoonChance", 0.35, 0.0, 1.0)

    private val tropicalDroughtChanceValue = builder
        .comment("Chance for a dry-season transition to trigger a drought")
        .defineInRange("tropicalDroughtChance", 0.35, 0.0, 1.0)

    private val tropicalDroughtTreeWaterMultiplierValue = builder
        .comment("Tree water consumption multiplier during a tropical drought")
        .defineInRange("tropicalDroughtTreeWaterMultiplier", 1.5, 1.0, 10.0)

    private val tropicalDroughtDryFarmlandGrowthPenaltyValue = builder
        .comment("Chance to cancel a natural crop growth attempt on dry farmland during a tropical drought")
        .defineInRange("tropicalDroughtDryFarmlandGrowthPenalty", 0.25, 0.0, 1.0)

    private val tropicalMonsoonTreeFertilizerMultiplierValue = builder
        .comment("Tree fertilizer consumption multiplier during a tropical monsoon")
        .defineInRange("tropicalMonsoonTreeFertilizerMultiplier", 1.10, 0.0, 10.0)

    private val tropicalMonsoonTreeWaterMultiplierValue = builder
        .comment("Tree water consumption multiplier during a tropical monsoon")
        .defineInRange("tropicalMonsoonTreeWaterMultiplier", 0.50, 0.0, 10.0)

    private val greenRainEnabledValue = builder
        .comment("Whether summer rainstorms can fork into green rain")
        .define("greenRainEnabled", true)

    private val greenRainChanceValue = builder
        .comment("Chance for each summer rainstorm to become green rain. There is no pity timer.")
        .defineInRange("greenRainChance", 0.05, 0.0, 1.0)

    private val darkCumulonimbusEnabledValue = builder
        .comment("Whether clear skies can roll into dark cumulonimbus, a dry overcast weather")
        .define("darkCumulonimbusEnabled", true)

    private val darkCumulonimbusChanceValue = builder
        .comment("Chance, checked once per Minecraft day, that clear weather becomes dark cumulonimbus")
        .defineInRange("darkCumulonimbusChance", 0.08, 0.0, 1.0)

    private val darkCumulonimbusDurationTicksValue = builder
        .comment("How long dark cumulonimbus lasts before clearing. Real rain ends it immediately.")
        .defineInRange("darkCumulonimbusDurationTicks", 12_000, 200, 240_000)

    private val darkCumulonimbusThunderChanceValue = builder
        .comment("Chance, checked once per second, to play distant thunder during dark cumulonimbus. No lightning is spawned.")
        .defineInRange("darkCumulonimbusThunderChance", 0.04, 0.0, 1.0)

    private val wolfRaidEnabledValue = builder
        .comment("Whether packs of wild wolves can raid nearby domesticated sheep at night")
        .define("wolfRaidEnabled", true)

    private val wolfRaidChanceValue = builder
        .comment("Chance for a qualifying sheep flock to be raided when a nightly attempt is rolled")
        .defineInRange("wolfRaidChance", 0.12, 0.0, 1.0)

    private val wolfRaidCheckIntervalTicksValue = builder
        .comment("Ticks between livestock-raid spawn and despawn checks")
        .defineInRange("wolfRaidCheckIntervalTicks", 100, 1, 1_200)

    private val wolfRaidHuntingCooldownTicksValue = builder
        .comment("Ticks after a raid wolf kills a sheep before another raid can start")
        .defineInRange("wolfRaidHuntingCooldownTicks", 18_000, 0, 240_000)

    private val foxRaidEnabledValue = builder
        .comment("Whether a wild fox can raid nearby domesticated chickens at night")
        .define("foxRaidEnabled", true)

    private val foxRaidChanceValue = builder
        .comment("Chance for a qualifying chicken flock to be raided when a nightly attempt is rolled")
        .defineInRange("foxRaidChance", 0.15, 0.0, 1.0)

    private val foxRaidHuntingCooldownTicksValue = builder
        .comment("Ticks after a raid fox kills a chicken before another fox raid can start")
        .defineInRange("foxRaidHuntingCooldownTicks", 18_000, 0, 240_000)

    @JvmField
    val SPEC: ForgeConfigSpec = builder.build()

    var restrictAnimalBreeding: Boolean = true
        private set
    var animalBreedingLoveChance: Double = 0.125
        private set
    var pigExtraBabyChance: Double = 0.25
        private set
    var restrictFishingLoot: Boolean = true
        private set
    var sendActionBarFeedback: Boolean = true
        private set
    var weatherAffectsCropGrowth: Boolean = true
        private set
    var foodExpiryEnabled: Boolean = false
        private set
    var foodShelfLifeDays: Int = 7
        private set
    var foodExpiryStackWindowHours: Int = 6
        private set
    var rainGrowthBonusChance: Double = 0.35
        private set
    var ironCropQualityChance: Double = 0.15
        private set
    var goldCropQualityChance: Double = 0.10
        private set
    var diamondCropQualityChance: Double = 0.05
        private set
    var requireScytheForCropQuality: Boolean = true
        private set
    var mdIronCropQualityChance: Double = 0.35
        private set
    var mdGoldCropQualityChance: Double = 0.10
        private set
    var mdDiamondCropQualityChance: Double = 0.05
        private set
    var metabolismSettlementsPerTick: Int = 64
        private set
    var wildAnimalAvoidanceScanIntervalTicks: Int = 5
        private set
    var tropicalClimateEventsEnabled: Boolean = true
        private set
    var tropicalMonsoonChance: Double = 0.35
        private set
    var tropicalDroughtChance: Double = 0.35
        private set
    var tropicalDroughtTreeWaterMultiplier: Double = 1.5
        private set
    var tropicalDroughtDryFarmlandGrowthPenalty: Double = 0.25
        private set
    var tropicalMonsoonTreeFertilizerMultiplier: Double = 1.10
        private set
    var tropicalMonsoonTreeWaterMultiplier: Double = 0.50
        private set
    var greenRainEnabled: Boolean = true
        private set
    var greenRainChance: Double = 0.05
        private set
    var darkCumulonimbusEnabled: Boolean = true
        private set
    var darkCumulonimbusChance: Double = 0.08
        private set
    var darkCumulonimbusDurationTicks: Int = 12_000
        private set
    var darkCumulonimbusThunderChance: Double = 0.04
        private set
    var wolfRaidEnabled: Boolean = true
        private set
    var wolfRaidChance: Double = 0.12
        private set
    var wolfRaidCheckIntervalTicks: Int = 100
        private set
    var wolfRaidHuntingCooldownTicks: Int = 18_000
        private set
    var foxRaidEnabled: Boolean = true
        private set
    var foxRaidChance: Double = 0.15
        private set
    var foxRaidHuntingCooldownTicks: Int = 18_000
        private set

    @JvmStatic
    @SubscribeEvent
    fun onLoad(event: ModConfigEvent) {
        if (event.config.getSpec() !== SPEC) {
            return
        }

        restrictAnimalBreeding = restrictAnimalBreedingValue.get()
        animalBreedingLoveChance = animalBreedingLoveChanceValue.get()
        pigExtraBabyChance = pigExtraBabyChanceValue.get()
        restrictFishingLoot = restrictFishingLootValue.get()
        sendActionBarFeedback = sendActionBarFeedbackValue.get()
        weatherAffectsCropGrowth = weatherAffectsCropGrowthValue.get()
        foodExpiryEnabled = foodExpiryEnabledValue.get()
        foodShelfLifeDays = foodShelfLifeDaysValue.get()
        foodExpiryStackWindowHours = foodExpiryStackWindowHoursValue.get()
        rainGrowthBonusChance = rainGrowthBonusChanceValue.get()
        ironCropQualityChance = ironCropQualityChanceValue.get()
        goldCropQualityChance = goldCropQualityChanceValue.get()
        diamondCropQualityChance = diamondCropQualityChanceValue.get()
        requireScytheForCropQuality = requireScytheForCropQualityValue.get()
        mdIronCropQualityChance = mdIronCropQualityChanceValue.get()
        mdGoldCropQualityChance = mdGoldCropQualityChanceValue.get()
        mdDiamondCropQualityChance = mdDiamondCropQualityChanceValue.get()
        metabolismSettlementsPerTick = metabolismSettlementsPerTickValue.get()
        wildAnimalAvoidanceScanIntervalTicks = wildAnimalAvoidanceScanIntervalTicksValue.get()
        tropicalClimateEventsEnabled = tropicalClimateEventsEnabledValue.get()
        tropicalMonsoonChance = tropicalMonsoonChanceValue.get()
        tropicalDroughtChance = tropicalDroughtChanceValue.get()
        tropicalDroughtTreeWaterMultiplier = tropicalDroughtTreeWaterMultiplierValue.get()
        tropicalDroughtDryFarmlandGrowthPenalty = tropicalDroughtDryFarmlandGrowthPenaltyValue.get()
        tropicalMonsoonTreeFertilizerMultiplier = tropicalMonsoonTreeFertilizerMultiplierValue.get()
        tropicalMonsoonTreeWaterMultiplier = tropicalMonsoonTreeWaterMultiplierValue.get()
        greenRainEnabled = greenRainEnabledValue.get()
        greenRainChance = greenRainChanceValue.get()
        darkCumulonimbusEnabled = darkCumulonimbusEnabledValue.get()
        darkCumulonimbusChance = darkCumulonimbusChanceValue.get()
        darkCumulonimbusDurationTicks = darkCumulonimbusDurationTicksValue.get()
        darkCumulonimbusThunderChance = darkCumulonimbusThunderChanceValue.get()
        wolfRaidEnabled = wolfRaidEnabledValue.get()
        wolfRaidChance = wolfRaidChanceValue.get()
        wolfRaidCheckIntervalTicks = wolfRaidCheckIntervalTicksValue.get()
        wolfRaidHuntingCooldownTicks = wolfRaidHuntingCooldownTicksValue.get()
        foxRaidEnabled = foxRaidEnabledValue.get()
        foxRaidChance = foxRaidChanceValue.get()
        foxRaidHuntingCooldownTicks = foxRaidHuntingCooldownTicksValue.get()
    }
}
