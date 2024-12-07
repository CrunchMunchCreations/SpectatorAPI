package xyz.crunchmunch.spectatorapi.mixin;

import net.minecraft.server.level.ServerEntity;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.entity.Entity;
import org.spongepowered.asm.mixin.Final;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Shadow;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;
import xyz.crunchmunch.spectatorapi.SpectatorAPI;
import xyz.crunchmunch.spectatorapi.SpectatorEvents;

@Mixin(ServerEntity.class)
public class ServerEntityMixin {
    @Shadow @Final private Entity entity;

    @Inject(method = "addPairing", at = @At("HEAD"), cancellable = true)
    private void cc$checkIsEntitySpectator(ServerPlayer player, CallbackInfo ci) {
        if (this.entity instanceof ServerPlayer p && SpectatorAPI.isCustomSpectator(p)) {
            if (!SpectatorEvents.CHECK_SPECTATOR_VISIBLE.invoker().shouldSpectatorBeVisibleTo(p, player)) {
                ci.cancel();
            }
        }
    }
}
