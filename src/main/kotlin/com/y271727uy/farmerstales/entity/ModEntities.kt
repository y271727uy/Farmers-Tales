package com.y271727uy.farmerstales.entity

import com.y271727uy.farmerstales.FTMod
import com.y271727uy.farmerstales.entity.mobs.BambooRatEntity
import com.y271727uy.farmerstales.entity.mobs.ArmadilloEntity
import com.y271727uy.farmerstales.entity.mobs.NautilusEntity
import com.y271727uy.farmerstales.entity.mobs.RatEntity
import com.y271727uy.farmerstales.entity.mobs.ZombieNautilusEntity
import com.y271727uy.farmerstales.entity.registry.MobRegistry
import net.minecraft.world.entity.MobCategory
import net.minecraft.world.entity.animal.Cat
import net.minecraft.world.entity.animal.IronGolem
import net.minecraft.world.entity.npc.Villager
import net.minecraftforge.event.entity.EntityAttributeCreationEvent
import net.minecraftforge.eventbus.api.SubscribeEvent
import net.minecraftforge.fml.common.Mod

/** The single declaration point for Farmer's Tales mobs. */
@Mod.EventBusSubscriber(modid = FTMod.MODID, bus = Mod.EventBusSubscriber.Bus.MOD)
object ModEntities : MobRegistry(FTMod.MODID) {
    @JvmField
    val ARMADILLO = mob("armadillo", ::ArmadilloEntity) {
        category = MobCategory.CREATURE
        size(0.8F, 0.65F)
        attributes(ArmadilloEntity::createAttributes)
        spawnOnGround(ArmadilloEntity::checkSpawnRules)
        naturalSpawn("minecraft:is_savanna", weight = 8, minCount = 2, maxCount = 3)
        breedingFood("minecraft:spider_eye")
        ai {
            passiveAnimal {
                panicSpeed = 1.25
            }
        }
        loot {
            drop("farmerstales:armadillo_scute", count = 1..1, chance = 0.15F)
        }
        lang("Armadillo")
        spawnEgg(primaryColor = 0x7A6756, secondaryColor = 0xB7A58D)
    }

    @JvmField
    val NAUTILUS = mob("nautilus", ::NautilusEntity) {
        category = MobCategory.WATER_CREATURE
        size(1.15F, 0.9F)
        attributes(NautilusEntity::createAttributes)
        spawnInWater(NautilusEntity::checkSpawnRules)
        naturalSpawn("minecraft:is_ocean", weight = 4, minCount = 1, maxCount = 2)
        breedingFood("minecraft:pufferfish")
        loot {
            drop("minecraft:nautilus_shell", count = 1..2)
        }
        lang("Nautilus")
        spawnEgg(primaryColor = 0xD2B189, secondaryColor = 0x5C8C72)
    }

    @JvmField
    val ZOMBIE_NAUTILUS = mob("zombie_nautilus", ::ZombieNautilusEntity) {
        category = MobCategory.MONSTER
        size(1.15F, 0.9F)
        attributes(NautilusEntity::createAttributes)
        spawnInWater(predicate = { _, level, _, pos, _ ->
            NautilusEntity.canSpawnInWater(level, pos)
        })
        loot {
            drop("minecraft:rotten_flesh", count = 1..2)
            drop("minecraft:nautilus_shell", count = 1..1, chance = 0.25F)
        }
        lang("Zombie Nautilus")
        spawnEgg(primaryColor = 0x3E776B, secondaryColor = 0x9CB69B)
    }

    @JvmField
    val BAMBOO_RAT = mob("bamboo_rat", ::BambooRatEntity) {
        category = MobCategory.CREATURE
        size(0.7F, 0.55F)
        attributes(BambooRatEntity::createAttributes)
        spawnOnGround(BambooRatEntity::checkSpawnRules)
        naturalSpawn("minecraft:is_jungle", weight = 8, minCount = 2, maxCount = 4)
        breedingFood("minecraft:bamboo")
        ai {
            passiveAnimal {
                panicSpeed = 1.35
            }
        }
        loot {
            drop("minecraft:bamboo", count = 1..2, lootingBonus = 0..1)
        }
        lang("Bamboo Rat")
        spawnEgg(primaryColor = 0x725540, secondaryColor = 0xD1A58D)
    }

    @JvmField
    val RAT = mob("rat", ::RatEntity) {
        size(0.5F, 0.45F)
        attributes(RatEntity::createAttributes)
        spawnOnGround(RatEntity::checkSpawnRules)
        naturalSpawn("minecraft:is_overworld", weight = 8, minCount = 1, maxCount = 1)
        breedingFoodTag("forge:seeds")
        tamedFoodHealing(2.0F)
        ai {
            passiveAnimal {
                panicSpeed = 1.45
                avoid(Cat::class.java) {
                    range = 12.0F
                    walkSpeed = 1.1
                    sprintSpeed = 1.15
                    untamedOnly()
                }
                avoid(Villager::class.java) {
                    range = 8.0F
                    walkSpeed = 1.1
                    sprintSpeed = 1.25
                    untamedOnly()
                }
                avoid(IronGolem::class.java) {
                    range = 12.0F
                    walkSpeed = 1.1
                    sprintSpeed = 1.35
                    untamedOnly()
                }
                avoidMovingPlayers {
                    range = 8.0F
                    walkSpeed = 1.3
                    sprintSpeed = 1.6
                    untamedOnly()
                }
            }
        }
        loot {
            randomDrop(
                "minecraft:wheat_seeds",
                "minecraft:beetroot_seeds",
                "minecraft:melon_seeds",
                "minecraft:pumpkin_seeds",
                count = 1..2,
            )
        }
        lang("Rat")
        spawnEgg(primaryColor = 0x666666, secondaryColor = 0xD5A3B3)
    }

    @JvmStatic
    @SubscribeEvent
    fun onAttributeCreation(event: EntityAttributeCreationEvent) {
        registerAttributes(event)
    }
}
