package xyz.crunchmunch.spectatorapi.mixin;

import com.llamalad7.mixinextras.injector.v2.WrapWithCondition;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.entity.Entity;
import org.spongepowered.asm.mixin.Final;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Shadow;
import org.spongepowered.asm.mixin.injection.At;
import xyz.crunchmunch.spectatorapi.SpectatorAPI;

import java.util.Set;

@Mixin(Entity.class)
public abstract class EntityMixin {
    @Shadow @Final private Set<String> tags;

    @WrapWithCondition(method = "push(Lnet/minecraft/world/entity/Entity;)V", at = @At(value = "INVOKE", target = "Lnet/minecraft/world/entity/Entity;push(DDD)V", ordinal = 1))
    private boolean disablePushIfSpectator(Entity instance, double x, double y, double z) {
        if (!((Object) this instanceof ServerPlayer player))
            return true;

        return !SpectatorAPI.isCustomSpectator(player);
    }
}
