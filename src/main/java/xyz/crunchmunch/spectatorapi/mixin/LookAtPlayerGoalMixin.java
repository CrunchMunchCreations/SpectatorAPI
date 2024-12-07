package xyz.crunchmunch.spectatorapi.mixin;

import com.llamalad7.mixinextras.injector.ModifyReturnValue;
import com.llamalad7.mixinextras.sugar.Local;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.ai.goal.LookAtPlayerGoal;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import xyz.crunchmunch.spectatorapi.SpectatorAPI;

@Mixin(LookAtPlayerGoal.class)
public abstract class LookAtPlayerGoalMixin {
    @ModifyReturnValue(method = "method_18414", at = @At("RETURN"))
    private static boolean checkIfPlayerIsSpectator(boolean original, @Local(argsOnly = true) LivingEntity living) {
        if (living instanceof ServerPlayer player && SpectatorAPI.isCustomSpectator(player))
            return false;

        return original;
    }
}
