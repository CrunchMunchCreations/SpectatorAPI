package xyz.crunchmunch.spectatorapi

import com.mojang.brigadier.arguments.BoolArgumentType
import com.mojang.brigadier.context.CommandContext
import eu.pb4.sgui.api.gui.SimpleGui
import net.fabricmc.api.ModInitializer
import net.fabricmc.fabric.api.command.v2.CommandRegistrationCallback
import net.fabricmc.fabric.api.entity.event.v1.ServerLivingEntityEvents
import net.fabricmc.fabric.api.event.lifecycle.v1.ServerLifecycleEvents
import net.fabricmc.fabric.api.event.player.AttackEntityCallback
import net.fabricmc.fabric.api.event.player.UseBlockCallback
import net.fabricmc.fabric.api.event.player.UseEntityCallback
import net.fabricmc.fabric.api.event.player.UseItemCallback
import net.minecraft.ChatFormatting
import net.minecraft.commands.CommandSourceStack
import net.minecraft.commands.Commands
import net.minecraft.commands.arguments.EntityArgument
import net.minecraft.core.component.DataComponents
import net.minecraft.nbt.CompoundTag
import net.minecraft.network.chat.Component
import net.minecraft.server.level.ServerPlayer
import net.minecraft.server.notifications.EmptyNotificationService
import net.minecraft.world.InteractionResult
import net.minecraft.world.damagesource.DamageTypes
import net.minecraft.world.effect.MobEffectInstance
import net.minecraft.world.effect.MobEffects
import net.minecraft.world.item.ItemStack
import net.minecraft.world.item.Items
import net.minecraft.world.item.component.CustomData
import net.minecraft.world.level.GameType
import xyz.crunchmunch.spectatorapi.gui.SpectatorPlayersGui
import xyz.crunchmunch.spectatorapi.mixin.ChunkMapAccessor
import xyz.crunchmunch.spectatorapi.mixin.TrackedEntityAccessor
import java.util.*
import java.util.concurrent.ConcurrentLinkedDeque
import java.util.function.Function

class SpectatorAPI : ModInitializer {
    override fun onInitialize() {
        UseItemCallback.EVENT.register { player, level, hand ->
            if (level.isClientSide)
                return@register InteractionResult.PASS

            if (player !is ServerPlayer)
                return@register InteractionResult.PASS

            val stack = player.getItemInHand(hand)

            if (stack.isSpectatorCompass()) {
                val gui = spectatorGui.apply(player)
                gui.open()
                return@register InteractionResult.SUCCESS
            }

            if (player.isCustomSpectator()) {
                return@register InteractionResult.FAIL
            }

            InteractionResult.PASS
        }

        UseEntityCallback.EVENT.register { player, level, hand, entity, hit ->
            if (level.isClientSide)
                return@register InteractionResult.PASS

            if (player !is ServerPlayer)
                return@register InteractionResult.PASS

            if (player.isCustomSpectator()) {
                return@register InteractionResult.FAIL
            }

            if (entity is ServerPlayer && entity.isCustomSpectator()) { // idk how this would happen, but...
                return@register InteractionResult.FAIL
            }

            InteractionResult.PASS
        }

        UseBlockCallback.EVENT.register { player, level, hand, hit ->
            if (level.isClientSide)
                return@register InteractionResult.PASS

            if (player !is ServerPlayer)
                return@register InteractionResult.PASS

            if (player.isCustomSpectator()) {
                return@register InteractionResult.FAIL
            }

            InteractionResult.PASS
        }

        AttackEntityCallback.EVENT.register { player, level, hand, entity, hit ->
            if (level.isClientSide)
                return@register InteractionResult.PASS

            if (player !is ServerPlayer)
                return@register InteractionResult.PASS

            if (player.isCustomSpectator()) {
                return@register InteractionResult.FAIL
            }

            if (entity is ServerPlayer && entity.isCustomSpectator()) { // idk how this would happen, but...
                return@register InteractionResult.FAIL
            }

            InteractionResult.PASS
        }

        ServerLivingEntityEvents.ALLOW_DAMAGE.register { entity, source, amount ->
            if (entity !is ServerPlayer)
                return@register true

            if (source.typeHolder().`is`(DamageTypes.FELL_OUT_OF_WORLD) || source.typeHolder().`is`(DamageTypes.GENERIC_KILL))
                return@register true

            if (entity.isCustomSpectator())
                return@register false

            if (source.entity is ServerPlayer && (source.entity as ServerPlayer).isCustomSpectator()) {
                return@register false
            }

            if (source.directEntity is ServerPlayer && (source.directEntity as ServerPlayer).isCustomSpectator()) {
                return@register false
            }

            true
        }

        CommandRegistrationCallback.EVENT.register { dispatcher, _, _ ->
            dispatcher.register(Commands.literal("spectatorapi")
                .requires { it.hasPermission(2) }
                .then(
                    Commands.literal("set")
                        .then(
                            Commands.argument("value", BoolArgumentType.bool())
                                .then(
                                    Commands.argument("players", EntityArgument.players())
                                        .then(
                                            Commands.argument("withFlight", BoolArgumentType.bool())
                                                .then(
                                                    Commands.argument("withCompass", BoolArgumentType.bool())
                                                        .executes {
                                                            setPlayerSpectator(it, EntityArgument.getPlayers(it, "players"), BoolArgumentType.getBool(it, "value"), BoolArgumentType.getBool(it, "withFlight"), BoolArgumentType.getBool(it, "withCompass"))

                                                            1
                                                        }
                                                )
                                                .executes {
                                                    setPlayerSpectator(it, EntityArgument.getPlayers(it, "players"), BoolArgumentType.getBool(it, "value"), BoolArgumentType.getBool(it, "withFlight"))

                                                    1
                                                }
                                        )
                                        .executes {
                                            setPlayerSpectator(it, EntityArgument.getPlayers(it, "players"), BoolArgumentType.getBool(it, "value"))

                                            1
                                        }
                                )
                                .executes {
                                    if (!it.source.isPlayer) {
                                        it.source.sendFailure(Component.literal("Invalid player!"))
                                        return@executes 0
                                    }

                                    setPlayerSpectator(it, listOf(it.source.playerOrException), BoolArgumentType.getBool(it, "value"))

                                    1
                                }
                        )
                )
                .then(
                    Commands.literal("get")
                        .then(
                            Commands.argument("player", EntityArgument.player())
                                .executes {
                                    getPlayerSpectator(it, EntityArgument.getPlayer(it, "player"))
                                }
                        )
                        .executes {
                            if (!it.source.isPlayer) {
                                it.source.sendFailure(Component.literal("Not a player!"))
                                return@executes 0
                            }

                            getPlayerSpectator(it, it.source.playerOrException)
                        }
                )
            )
        }

        // If the server crashes, the spectator states wouldn't persist except for being in a tag,
        // so check the tags and add them back in.
        ServerLifecycleEvents.SERVER_STARTING.register { server ->
            server.notificationManager().registerService(object : EmptyNotificationService() {
                override fun playerJoined(player: ServerPlayer) {
                    super.playerJoined(player)

                    if (player.tags.contains(SPECTATOR_ENTITY_TAG) && !player.isCustomSpectator()) {
                        spectators.add(player.uuid)

                        val entityMap = (player.level().chunkSource.chunkMap as ChunkMapAccessor).entityMap
                        val serverEntity = (entityMap.get(player.id) as TrackedEntityAccessor).serverEntity

                        for (p in server.playerList.players) {
                            if (player == p)
                                continue

                            if (!SpectatorEvents.CHECK_SPECTATOR_VISIBLE.invoker().shouldSpectatorBeVisibleTo(player, p)) {
                                serverEntity.removePairing(p)
                            }
                        }
                    }
                }
            })
        }
    }

    private fun getPlayerSpectator(ctx: CommandContext<CommandSourceStack>, player: ServerPlayer): Int {
        if (player.isCustomSpectator()) {
            ctx.source.sendSystemMessage(Component.empty()
                .append(player.displayName ?: player.name)
                .append(" is spectator? ")
                .append(Component.literal("True").withStyle(ChatFormatting.GREEN))
            )
            return 1
        }

        ctx.source.sendSystemMessage(Component.empty()
            .append(player.displayName ?: player.name)
            .append(" is spectator? ")
            .append(Component.literal("False").withStyle(ChatFormatting.RED))
        )
        return 0
    }

    private fun setPlayerSpectator(ctx: CommandContext<CommandSourceStack>, players: Collection<ServerPlayer>, value: Boolean, withFlight: Boolean = true, withCompass: Boolean = true) {
        for (player in players) {
            if (value)
                player.enableCustomSpectator(withFlight, withCompass)
            else
                player.disableCustomSpectator()
        }

        ctx.source.sendSystemMessage(Component.literal("Set ${players.size} players' spectator state to $value."))
    }

    companion object {
        const val SPECTATOR_COMPASS_DATA = "SpectatorAPI_IsSpectatorCompass"
        const val SPECTATOR_ENTITY_TAG = "SpectatorAPI_IsSpectator"

        private val spectators = ConcurrentLinkedDeque<UUID>()
        @JvmStatic var spectatorGui: Function<ServerPlayer, out SimpleGui> = Function { player -> SpectatorPlayersGui(player) }

        /**
         * Set this to false if you don't want spectators to have the invisibility effect.
         * This only affects spectators being able to see themselves - this does not affect their visibility in-game.
         */
        @JvmStatic var giveSpectatorsInvisibility = true

        @JvmStatic
        fun ServerPlayer.enableCustomSpectator(enableFlight: Boolean = true, giveCompass: Boolean = true) {
            this.setGameMode(GameType.ADVENTURE)
            this.addTag(SPECTATOR_ENTITY_TAG)
            spectators.add(this.uuid)

            if (enableFlight) {
                this.abilities.mayfly = true
                this.abilities.flying = true
            }

            this.abilities.invulnerable = true
            this.abilities.mayBuild = false
            this.onUpdateAbilities()

            if (giveSpectatorsInvisibility) {
                this.isInvisible = true // idk if this even does anything
                this.addEffect(MobEffectInstance(MobEffects.INVISIBILITY, -1, 255, true, false, true))
            }

            if (giveCompass) {
                this.giveSpectatorCompass()
            }

            val entityMap = (this.level().chunkSource.chunkMap as ChunkMapAccessor).entityMap
            val serverEntity = (entityMap.get(this.id) as TrackedEntityAccessor).serverEntity

            for (player in this.server!!.playerList.players) {
                // Ignore self, because as it turns out, if you remove yourself, you end up having a VERY interesting
                // out-of-body experience
                if (player == this)
                    continue

                if (!SpectatorEvents.CHECK_SPECTATOR_VISIBLE.invoker().shouldSpectatorBeVisibleTo(this, player)) {
                    serverEntity.removePairing(player)
                }
            }
        }

        @JvmStatic
        fun ServerPlayer.giveSpectatorCompass() {
            val compass = ItemStack(Items.COMPASS, 1)
            compass.set(DataComponents.ITEM_NAME, Component.literal("Spectate Player"))
            compass.set(DataComponents.CUSTOM_DATA, CustomData.of(CompoundTag().apply {
                this.putBoolean(SPECTATOR_COMPASS_DATA, true)
            }))

            this.addItem(compass)
        }

        @JvmStatic
        fun ServerPlayer.disableCustomSpectator() {
            this.setGameMode(GameType.ADVENTURE)
            this.removeTag(SPECTATOR_ENTITY_TAG)

            spectators.remove(this.uuid)

            this.abilities.mayfly = false
            this.abilities.flying = false
            this.abilities.invulnerable = false
            this.isInvisible = false
            this.onUpdateAbilities()

            this.removeEffect(MobEffects.INVISIBILITY)

            val items = this.inventory.toList()
            for (stack in items) {
                if (stack.isSpectatorCompass()) {
                    this.inventory.removeItem(stack)
                    this.inventory.setChanged()
                }
            }

            val entityMap = (this.level().chunkSource.chunkMap as ChunkMapAccessor).entityMap
            val serverEntity = (entityMap.get(this.id) as TrackedEntityAccessor).serverEntity

            for (player in this.server!!.playerList.players) {
                if (player == this)
                    continue

                serverEntity.addPairing(player)
            }
        }

        @JvmStatic
        fun ServerPlayer.isCustomSpectator(): Boolean {
            return spectators.contains(this.uuid)
        }

        @JvmStatic
        fun ItemStack.isSpectatorCompass(): Boolean {
            if (!this.`is`(Items.COMPASS))
                return false

            val data = this.get(DataComponents.CUSTOM_DATA)?.copyTag() ?: return false
            return data.contains(SPECTATOR_COMPASS_DATA) && data.getBoolean(SPECTATOR_COMPASS_DATA).orElse(false)
        }
    }
}
