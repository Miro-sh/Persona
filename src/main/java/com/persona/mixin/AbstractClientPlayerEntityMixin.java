package com.persona.mixin;

import com.persona.identity.IdentityManager;
import net.minecraft.client.network.AbstractClientPlayerEntity;
import net.minecraft.entity.player.SkinTextures;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;

/** Overrides the local player's skin + cape (local render only). */
@Mixin(AbstractClientPlayerEntity.class)
public abstract class AbstractClientPlayerEntityMixin {

    @Inject(method = "getSkin", at = @At("RETURN"), cancellable = true)
    private void persona$overrideSkin(CallbackInfoReturnable<SkinTextures> cir) {
        AbstractClientPlayerEntity self = (AbstractClientPlayerEntity) (Object) this;
        if (IdentityManager.get().isLocalPlayer(self)) {
            SkinTextures original = cir.getReturnValue();
            SkinTextures replaced = IdentityManager.get().applyTo(original);
            if (replaced != original) {
                cir.setReturnValue(replaced);
            }
        }
    }
}
