package xyz.crunchmunch.spectatorapi.mixin;

import com.llamalad7.mixinextras.injector.ModifyReturnValue;
import com.llamalad7.mixinextras.sugar.Local;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.level.block.entity.SculkSensorBlockEntity;
import net.minecraft.world.level.gameevent.GameEvent;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import xyz.crunchmunch.spectatorapi.SpectatorAPI;

@Mixin(SculkSensorBlockEntity.class)
public class SculkSensorBlockEntityMixin {
    @Mixin(targets = "net.minecraft.world.level.block.entity.SculkSensorBlockEntity.VibrationUser")
    protected static class VibrationUserMixin {
        @ModifyReturnValue(method = "canReceiveVibration", at = @At("RETURN"))
        private boolean cancelNonPlayerEvents(boolean original, @Local(argsOnly = true) GameEvent.Context context) {
            if (context != null) {
                var entity = context.sourceEntity();
                if (entity != null) {
                    if (entity instanceof ServerPlayer player && SpectatorAPI.isCustomSpectator(player))
                        return false;

                    return original;
                }
            }

            return original;
        }
    }
}
