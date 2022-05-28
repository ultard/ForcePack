package com.convallyria.forcepack.velocity.listener;

import com.convallyria.forcepack.api.utils.ClientVersion;
import com.convallyria.forcepack.api.utils.GeyserUtil;
import com.convallyria.forcepack.velocity.ForcePackVelocity;
import com.convallyria.forcepack.velocity.config.VelocityConfig;
import com.velocitypowered.api.command.CommandSource;
import com.velocitypowered.api.event.PostOrder;
import com.velocitypowered.api.event.Subscribe;
import com.velocitypowered.api.event.player.PlayerResourcePackStatusEvent;
import com.velocitypowered.api.event.player.ServerPostConnectEvent;
import com.velocitypowered.api.proxy.Player;
import com.velocitypowered.api.proxy.ServerConnection;
import com.velocitypowered.api.proxy.server.ServerInfo;
import net.kyori.adventure.text.Component;

import java.util.Optional;

public class ResourcePackListener {

    private final ForcePackVelocity plugin;

    public ResourcePackListener(final ForcePackVelocity plugin) {
        this.plugin = plugin;
    }

    @Subscribe(order = PostOrder.EARLY)
    public void onPackStatus(PlayerResourcePackStatusEvent event) {
        final Player player = event.getPlayer();
        final Optional<ServerConnection> currentServer = player.getCurrentServer();
        if (currentServer.isEmpty()) {
            plugin.log(player.getUsername() + "'s server does not exist.");
            return;
        }

        // Check if the server they're on has a resource pack
        final String serverName = currentServer.get().getServerInfo().getName();
        if (plugin.getPackByServer(serverName).isEmpty()) {
            plugin.log(serverName + " does not have a ResourcePack, ignoring.");
            return;
        }

        final PlayerResourcePackStatusEvent.Status status = event.getStatus();
        boolean geyser = plugin.getConfig().getBoolean("geyser") && GeyserUtil.isBedrockPlayer(player.getUniqueId());
        boolean canBypass = player.hasPermission("ForcePack.bypass") && plugin.getConfig().getBoolean("bypass-permission");
        if (!canBypass && !geyser) {
            plugin.log(player.getUsername() + " sent status: " + event.getStatus());

            final VelocityConfig actions = plugin.getConfig().getConfig("servers").getConfig(serverName).getConfig("actions").getConfig(status.name());
            for (String cmd : actions.getStringList("commands")) {
                final CommandSource console = plugin.getServer().getConsoleCommandSource();
                plugin.getServer().getCommandManager().executeAsync(console, cmd);
            }

            final boolean kick = actions.getBoolean("kick");
            final String text = actions.getString("message");
            if (text == null) return;

            final Component component = plugin.getMiniMessage().deserialize(text);
            if (kick) {
                player.disconnect(component);
            } else {
                player.sendMessage(component);
            }
        } else {
            plugin.log("Ignoring player " + player.getUsername() + " as they do not have permissions or are a geyser player.");
        }
    }

    @Subscribe(order = PostOrder.EARLY)
    public void onJoin(ServerPostConnectEvent event) {
        final Player player = event.getPlayer();
        final Optional<ServerConnection> currentServer = player.getCurrentServer();
        if (currentServer.isEmpty()) return;

        // Find whether the config contains this server
        final ServerInfo serverInfo = currentServer.get().getServerInfo();

        boolean geyser = plugin.getConfig().getBoolean("geyser") && GeyserUtil.isBedrockPlayer(player.getUniqueId());
        boolean canBypass = player.hasPermission("ForcePack.bypass") && plugin.getConfig().getBoolean("bypass-permission");
        if (!canBypass && !geyser) {
            plugin.getPackHandler().setPack(player, serverInfo);
        }
    }
}
