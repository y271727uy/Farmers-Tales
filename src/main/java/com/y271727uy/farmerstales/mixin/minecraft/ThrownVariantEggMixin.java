package com.y271727uy.farmerstales.mixin.minecraft;

import com.y271727uy.farmerstales.gameplay.animal_variants.VariantEggs;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.projectile.ThrownEgg;
import net.minecraft.world.level.Level;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Redirect;

@Mixin(ThrownEgg.class)
public abstract class ThrownVariantEggMixin {
    @Redirect(method = "onHit", at = @At(value = "INVOKE", target = "Lnet/minecraft/world/level/Level;addFreshEntity(Lnet/minecraft/world/entity/Entity;)Z"))
    private boolean farmerstales$hatchVariant(Level level, Entity entity) {
        VariantEggs.setHatchlingVariant((ThrownEgg) (Object) this, entity);
        return level.addFreshEntity(entity);
    }
}
