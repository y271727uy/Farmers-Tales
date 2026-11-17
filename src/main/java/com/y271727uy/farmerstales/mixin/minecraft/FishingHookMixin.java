package com.y271727uy.farmerstales.mixin.minecraft;

import com.y271727uy.farmerstales.all.ModEnchantments;
import com.y271727uy.farmerstales.gameplay.fish_group.FishingHookEnchantments;
import com.y271727uy.farmerstales.gameplay.season_fishing.FocusedFishing;
import com.y271727uy.farmerstales.gameplay.season_fishing.SeasonFishingLoot;
import net.minecraft.core.BlockPos;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.entity.projectile.FishingHook;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.enchantment.EnchantmentHelper;
import org.objectweb.asm.Opcodes;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Shadow;
import org.spongepowered.asm.mixin.Unique;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.ModifyArg;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

import java.util.Collection;
import java.util.List;

@Mixin(FishingHook.class)
abstract class FishingHookMixin implements FishingHookEnchantments {
    @Shadow private int timeUntilLured;
    @Shadow public abstract Player getPlayerOwner();

    @Unique private int farmerstales$fishKingCatcherLevel;
    @Unique private boolean farmerstales$fishingFrenzy;
    @Unique private boolean farmerstales$saltyBait;
    @Unique private int farmerstales$curseOfTheSeaLevel;
    @Unique private boolean farmerstales$focusedFishing;
    @Unique private FocusedFishing.PreviousCatch farmerstales$previousCatch;

    @Override
    public void farmerstales$setCastingRod(ItemStack rod) {
        farmerstales$fishKingCatcherLevel = Math.min(3,
                EnchantmentHelper.getItemEnchantmentLevel(ModEnchantments.FISH_KING_CATCHER.get(), rod));
        farmerstales$saltyBait =
                EnchantmentHelper.getItemEnchantmentLevel(ModEnchantments.CURSE_OF_SALTY_BAIT.get(), rod) > 0;
        farmerstales$curseOfTheSeaLevel =
                EnchantmentHelper.getItemEnchantmentLevel(ModEnchantments.CURSE_OF_THE_SEA.get(), rod);
        farmerstales$fishingFrenzy = !farmerstales$saltyBait &&
                EnchantmentHelper.getItemEnchantmentLevel(ModEnchantments.FISHING_FRENZY.get(), rod) > 0;
        farmerstales$focusedFishing =
                EnchantmentHelper.getItemEnchantmentLevel(ModEnchantments.FOCUSED_FISHING.get(), rod) > 0;
        if (getPlayerOwner() instanceof ServerPlayer player) {
            farmerstales$previousCatch = FocusedFishing.takePreviousCatch(player);
        }
    }

    @Override
    public int farmerstales$getFishKingCatcherLevel() {
        return farmerstales$fishKingCatcherLevel;
    }

    @Override
    public boolean farmerstales$hasFishingFrenzy() {
        return farmerstales$fishingFrenzy;
    }

    @ModifyArg(method = "retrieve", at = @At(
            value = "INVOKE",
            target = "Lnet/minecraft/world/level/storage/loot/LootParams$Builder;withLuck(F)Lnet/minecraft/world/level/storage/loot/LootParams$Builder;"
    ))
    private float farmerstales$applyCurseOfTheSea(float luck) {
        return luck - farmerstales$curseOfTheSeaLevel;
    }

    // This is the ordinary loot branch after Forge has handled cancellation and fish-pool rewards.
    @ModifyArg(method = "retrieve", at = @At(
            value = "INVOKE",
            target = "Lnet/minecraft/advancements/critereon/FishingRodHookedTrigger;trigger(Lnet/minecraft/server/level/ServerPlayer;Lnet/minecraft/world/item/ItemStack;Lnet/minecraft/world/entity/projectile/FishingHook;Ljava/util/Collection;)V",
            ordinal = 1
    ), index = 3)
    private Collection<ItemStack> farmerstales$resolveCatch(ServerPlayer player, ItemStack rod, FishingHook hook,
                                                             Collection<ItemStack> caught) {
        if (caught instanceof List<ItemStack> drops) {
            SeasonFishingLoot.apply(player, rod, hook, drops);
            if (farmerstales$focusedFishing) {
                FocusedFishing.apply(player, hook, drops, farmerstales$previousCatch);
            }
        }
        return caught;
    }

    // The final write initializes the wait after applying vanilla Lure.
    @Inject(method = "catchingFish", at = @At(
            value = "FIELD",
            target = "Lnet/minecraft/world/entity/projectile/FishingHook;timeUntilLured:I",
            opcode = Opcodes.PUTFIELD,
            ordinal = 3,
            shift = At.Shift.AFTER
    ))
    private void farmerstales$adjustBiteWait(BlockPos pos, CallbackInfo ci) {
        if (farmerstales$saltyBait) {
            timeUntilLured *= 2;
        } else if (farmerstales$fishingFrenzy) {
            timeUntilLured = Math.max(1, timeUntilLured - 100);
        }
    }
}
