package xyz.crunchmunch.spectatorapi.mixin;

import com.llamalad7.mixinextras.injector.wrapoperation.Operation;
import com.llamalad7.mixinextras.injector.wrapoperation.WrapOperation;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.entity.ExperienceOrb;
import net.minecraft.world.entity.player.Player;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;
import xyz.crunchmunch.spectatorapi.SpectatorAPI;

@Mixin(ExperienceOrb.class)
public abstract class ExperienceOrbMixin {
    @Inject(method = "playerTouch", at = @At("HEAD"), cancellable = true)
    private void preventSpectatorPickingUpExperience(Player player, CallbackInfo ci) {
        if (player instanceof ServerPlayer serverPlayer && SpectatorAPI.isCustomSpectator(serverPlayer)) {
            ci.cancel();
        }
    }

    @WrapOperation(method = "followNearbyPlayer", at = @At(value = "INVOKE", target = "Lnet/minecraft/world/entity/player/Player;isSpectator()Z"))
    private boolean checkIsFakeSpectator(Player instance, Operation<Boolean> original) {
        return original.call(instance) || (instance instanceof ServerPlayer serverPlayer && SpectatorAPI.isCustomSpectator(serverPlayer));
    }
}
