package dev.keaneaudric.soulRealmsVoiceUtils.listeners;

import de.maxhenkel.voicechat.api.*;
import de.maxhenkel.voicechat.api.packets.StaticSoundPacket;
import dev.keaneaudric.soulRealmsVoiceUtils.SoulRealmsVoiceUtils;
import dev.keaneaudric.soulRealmsVoiceUtils.managers.VoiceChatManager;
import de.maxhenkel.voicechat.api.events.EventRegistration;
import de.maxhenkel.voicechat.api.events.MicrophonePacketEvent;
import org.bukkit.configuration.file.FileConfiguration;
import org.bukkit.entity.Player;

public class BroadcastListener implements VoicechatPlugin {

    private final VoiceChatManager manager;
    private final SoulRealmsVoiceUtils plugin;
    private boolean broadcastEnabled;
    private String broadcastPermission;
    private String broadcastGroupName;

    public BroadcastListener(SoulRealmsVoiceUtils plugin, VoiceChatManager manager) {
        this.plugin = plugin;
        this.manager = manager;
        loadConfiguration();
    }

    private void loadConfiguration() {
        FileConfiguration config = plugin.getConfig();
        this.broadcastEnabled = config.getBoolean("broadcast.enabled", true);
        this.broadcastPermission = config.getString("broadcast.broadcast-permission", "voicechat.broadcast");
        this.broadcastGroupName = config.getString("broadcast.group-name", "Broadcast");
    }

    @Override
    public String getPluginId() {
        return "soul-realms-voice-utils";
    }

    @Override
    public void initialize(VoicechatApi api) {
        if (api instanceof VoicechatServerApi) {
            manager.setVoicechatApi((VoicechatServerApi) api);
            plugin.getSLF4JLogger().info("SimpleVoiceChat API initialized successfully!");
        }
    }

    @Override
    public void registerEvents(EventRegistration registration) {
        registration.registerEvent(MicrophonePacketEvent.class, this::onMicrophonePacket);
    }

    private void onMicrophonePacket(MicrophonePacketEvent event) {
        if (!broadcastEnabled) {
            return;
        }

        if (event.getSenderConnection() == null) {
            return;
        }

        if (!(event.getSenderConnection().getPlayer().getPlayer() instanceof Player player)) {
            return;
        }

        if (!player.hasPermission(broadcastPermission)) {
            return;
        }

        Group group = event.getSenderConnection().getGroup();

        if (group == null) {
            return;
        }

        if (!group.getName().strip().equalsIgnoreCase(broadcastGroupName)) {
            return;
        }

        event.cancel(); // So that people in the group don't hear the broadcaster twice.

        VoicechatServerApi api = event.getVoicechat();
        StaticSoundPacket staticPacket = event.getPacket().toStaticSoundPacket();

        // Iterating over every player on the server
        for (Player onlinePlayer : plugin.getServer().getOnlinePlayers()) {

            // don't duplicate audio to broadcaster
            if (onlinePlayer.getUniqueId().equals(player.getUniqueId())) {
                continue;
            }
            VoicechatConnection connection = api.getConnectionOf(onlinePlayer.getUniqueId());

            // Check if the player is actually connected to the voice chat
            if (connection == null) {
                continue;
            }

            // Send a static audio packet of the microphone data to the connection of each player
            api.sendStaticSoundPacketTo(connection, staticPacket);
        }
        
        if (plugin.getConfig().getBoolean("debug", false)) {
            plugin.getSLF4JLogger().info("[DEBUG] Broadcasted voice packet from {} to {} players", 
                player.getName(), plugin.getServer().getOnlinePlayers().size() - 1);
        }
    }
    
    public void reloadConfiguration() {
        loadConfiguration();
    }
}
