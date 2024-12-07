package xyz.crunchmunch.spectatorapi.mixin;

import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.monster.warden.AngerManagement;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;
import xyz.crunchmunch.spectatorapi.SpectatorAPI;

@Mixin(AngerManagement.class)
public abstract class AngerManagementMixin {
    @Inject(method = "increaseAnger", at = @At("HEAD"), cancellable = true)
    private void avoidAngerIncreaseIfSpectator(Entity entity, int offset, CallbackInfoReturnable<Integer> cir) {
        if (entity instanceof ServerPlayer player && SpectatorAPI.isCustomSpectator(player))
            cir.setReturnValue(0);
    }
}

