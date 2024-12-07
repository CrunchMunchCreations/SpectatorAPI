package xyz.crunchmunch.spectatorapi

import net.fabricmc.fabric.api.event.EventFactory
import net.minecraft.server.level.ServerPlayer
import xyz.crunchmunch.spectatorapi.SpectatorEvents.CheckSpectatorVisible

interface SpectatorEvents {
    companion object {
        @JvmField val CHECK_SPECTATOR_VISIBLE = EventFactory.createArrayBacked(CheckSpectatorVisible::class.java) { array -> CheckSpectatorVisible { spectator, other ->
            for (check in array) {
                if (check.shouldSpectatorBeVisibleTo(spectator, other))
                    return@CheckSpectatorVisible true
            }

            false
        }}
    }

    fun interface CheckSpectatorVisible {
        fun shouldSpectatorBeVisibleTo(spectator: ServerPlayer, other: ServerPlayer): Boolean
    }
}