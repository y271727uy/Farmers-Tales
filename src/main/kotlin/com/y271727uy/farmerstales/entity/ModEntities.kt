package com.y271727uy.farmerstales.entity

import com.y271727uy.farmerstales.FTMod
import com.y271727uy.farmerstales.entity.mobs.BambooRatEntity
import com.y271727uy.farmerstales.entity.mobs.RatEntity
import com.y271727uy.farmerstales.entity.registry.MobRegistry
import net.minecraft.world.entity.MobCategory
import net.minecraft.world.entity.animal.Cat
import net.minecraftforge.event.entity.EntityAttributeCreationEvent
import net.minecraftforge.eventbus.api.SubscribeEvent
import net.minecraftforge.fml.common.Mod

/** The single declaration point for Farmer's Tales mobs. */
@Mod.EventBusSubscriber(modid = FTMod.MODID, bus = Mod.EventBusSubscriber.Bus.MOD)
object ModEntities : MobRegistry(FTMod.MODID) {
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
