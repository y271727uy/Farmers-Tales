package com.y271727uy.farmerstales.mixin.minecraft;

import com.y271727uy.farmerstales.gameplay.fish_group.FishingHookEnchantments;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.InteractionResultHolder;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.FishingRodItem;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.Level;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;

@Mixin(FishingRodItem.class)
abstract class FishingRodItemMixin {
    @Inject(method = "use", at = @At(
            value = "INVOKE",
            target = "Lnet/minecraft/world/level/Level;addFreshEntity(Lnet/minecraft/world/entity/Entity;)Z"
    ))
    private void farmerstales$readCastingRod(Level level, Player player, InteractionHand hand,
                                             CallbackInfoReturnable<InteractionResultHolder<ItemStack>> cir) {
        if (player.fishing instanceof FishingHookEnchantments enchantments) {
            enchantments.farmerstales$setCastingRod(player.getItemInHand(hand));
        }
    }
}
