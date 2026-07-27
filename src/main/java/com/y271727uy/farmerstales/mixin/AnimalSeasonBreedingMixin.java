package com.y271727uy.farmerstales.mixin;

import com.y271727uy.farmerstales.gameplay.season_breeding.SeasonBreedingEvents;
import net.minecraft.world.entity.animal.Animal;
import net.minecraft.world.entity.player.Player;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

/** Prevents only love mode, leaving the vanilla feeding interaction intact. */
@Mixin(Animal.class)
public abstract class AnimalSeasonBreedingMixin {
    @Inject(method = "setInLove(Lnet/minecraft/world/entity/player/Player;)V", at = @At("HEAD"), cancellable = true)
    private void farmerstales$blockOutOfSeasonLove(Player player, CallbackInfo ci) {
        Animal animal = (Animal) (Object) this;
        if (animal.level().isClientSide || !SeasonBreedingEvents.isLoveBlocked(animal)) {
            return;
        }

        if (player != null) {
            SeasonBreedingEvents.sendLoveBlockedFeedback(animal, player);
        }
        ci.cancel();
    }
}
