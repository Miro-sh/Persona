package com.persona.mixin.flashback;

import com.llamalad7.mixinextras.injector.wrapmethod.WrapMethod;
import com.llamalad7.mixinextras.injector.wrapoperation.Operation;
import com.persona.flashback.FlashbackCapeIntegration;
import net.minecraft.client.network.AbstractClientPlayerEntity;
import net.minecraft.entity.player.SkinTextures;
import org.spongepowered.asm.mixin.Mixin;

@Mixin(AbstractClientPlayerEntity.class)
public class AbstractClientPlayerEntityCapeMixin {

    /**
     * Wraps the entire {@code getSkin()} method as the outermost layer.
     *
     * <p>Flashback's own {@code MixinAbstractClientPlayer#getSkin} injects at
     * {@code @At("HEAD")} with {@code cancellable = true}: when a "Load Skin From File"
     * override exists for this entity's UUID, it calls {@code cir.setReturnValue(...)}
     * and returns early, cancelling the method before a {@code @ModifyReturnValue} at
     * {@code RETURN} could ever run. That is why the Persona cape used to vanish as soon
     * as a custom file skin was applied.
     *
     * <p>{@code @WrapMethod} sidesteps the injection-ordering problem entirely:
     * {@code original.call()} runs the full original method (including Flashback's cancel)
     * and hands us back whatever it actually returned — the file skin. We then layer the
     * Persona cape on top of it, so the cape survives no matter which skin source won.
     */
    @WrapMethod(method = "getSkin")
    private SkinTextures persona$applyCape(Operation<SkinTextures> original) {
        SkinTextures result = original.call();
        AbstractClientPlayerEntity self = (AbstractClientPlayerEntity) (Object) this;
        return FlashbackCapeIntegration.applyOverrides(self, result);
    }
}
