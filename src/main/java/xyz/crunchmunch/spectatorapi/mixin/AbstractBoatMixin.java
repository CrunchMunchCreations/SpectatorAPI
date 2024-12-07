package xyz.crunchmunch.spectatorapi.mixin;

import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.vehicle.AbstractBoat;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;
import xyz.crunchmunch.spectatorapi.SpectatorAPI;

@Mixin(AbstractBoat.class)
public class AbstractBoatMixin {
    @Inject(method = "canVehicleCollide", at = @At("HEAD"), cancellable = true)
    private static void avoidCollideWithSpectators(Entity entity, Entity entity2, CallbackInfoReturnable<Boolean> cir) {
        if (entity2 instanceof ServerPlayer player && SpectatorAPI.isCustomSpectator(player)) {
            cir.setReturnValue(false);
        }
    }
}
