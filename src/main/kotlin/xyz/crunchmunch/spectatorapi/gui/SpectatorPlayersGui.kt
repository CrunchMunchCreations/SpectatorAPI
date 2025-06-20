package xyz.crunchmunch.spectatorapi.gui

import eu.pb4.sgui.api.elements.GuiElementBuilder
import eu.pb4.sgui.api.gui.SimpleGui
import net.minecraft.core.component.DataComponents
import net.minecraft.network.chat.Component
import net.minecraft.server.level.ServerPlayer
import net.minecraft.world.inventory.MenuType
import net.minecraft.world.item.ItemStack
import net.minecraft.world.item.Items
import net.minecraft.world.item.component.ItemLore
import xyz.crunchmunch.spectatorapi.SpectatorAPI.Companion.isCustomSpectator
import xyz.crunchmunch.spectatorapi.SpectatorEvents

open class SpectatorPlayersGui(player: ServerPlayer, var page: Int = 0) : SimpleGui(MenuType.GENERIC_9x4, player, false) {
    init {
        this.title = Component.literal("Select a Player to Spectate")
        val players = this.player.server!!.playerList.players.toList() // Create a copy of the player list

        for (p in players) {
            if (p.isCustomSpectator() && !SpectatorEvents.CHECK_SPECTATOR_VISIBLE.invoker().shouldSpectatorBeVisibleTo(p, player))
                continue

            this.addSlot(GuiElementBuilder(ItemStack(Items.PLAYER_HEAD, 1).apply {
                this.set(DataComponents.ITEM_NAME, p.displayName ?: p.name)

                if (p.isCustomSpectator()) {
                    this.set(DataComponents.LORE, ItemLore(listOf(Component.literal("[Spectator]"))))
                }
            }).apply {
                this.setSkullOwner(p.gameProfile, p.server)
                this.setCallback { _, type, action, gui ->
                    gui.close()
                    gui.player.teleportTo(p.level(), p.x, p.y, p.z, setOf(), p.yRot, p.xRot, true)
                    gui.player.displayClientMessage(Component.literal("Teleported to ")
                        .append(p.displayName ?: p.name), true)
                }
            })
        }
    }

    override fun canPlayerClose(): Boolean {
        return true
    }
}