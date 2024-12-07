package xyz.crunchmunch.spectatorapi.mixin;

import net.minecraft.server.level.ChunkMap;
import net.minecraft.server.level.ServerEntity;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.gen.Accessor;

@Mixin(ChunkMap.TrackedEntity.class)
public interface TrackedEntityAccessor {
    @Accessor
    ServerEntity getServerEntity();
}

