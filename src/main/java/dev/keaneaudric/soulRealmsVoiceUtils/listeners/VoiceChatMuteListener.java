package dev.keaneaudric.soulRealmsVoiceUtils.listeners;

import dev.keaneaudric.soulRealmsVoiceUtils.SoulRealmsVoiceUtils;
import dev.keaneaudric.soulRealmsVoiceUtils.managers.MuteManager;
import dev.keaneaudric.soulRealmsVoiceUtils.managers.LanguageManager;
import de.maxhenkel.voicechat.api.VoicechatApi;
import de.maxhenkel.voicechat.api.VoicechatPlugin;
import de.maxhenkel.voicechat.api.events.EventRegistration;
import de.maxhenkel.voicechat.api.events.MicrophonePacketEvent;
import net.kyori.adventure.text.Component;
import org.bukkit.configuration.file.FileConfiguration;
import org.bukkit.entity.Player;
import java.util.UUID;

public class VoiceChatMuteListener implements VoicechatPlugin {
    
    private final SoulRealmsVoiceUtils plugin;
    private final MuteManager muteManager;
    private final LanguageManager languageManager;
    private boolean actionBarEnabled;
    private boolean chatMessageEnabled;
    
    public VoiceChatMuteListener(SoulRealmsVoiceUtils plugin, MuteManager muteManager, LanguageManager languageManager) {
        this.plugin = plugin;
        this.muteManager = muteManager;
        this.languageManager = languageManager;
        loadConfiguration();
    }
    
    private void loadConfiguration() {
        FileConfiguration config = plugin.getConfig();
        this.actionBarEnabled = config.getBoolean("mute.actionbar.enabled", true);
        this.chatMessageEnabled = config.getBoolean("mute.message.enabled", false);
    }
    
    @Override
    public String getPluginId() {
        return "soul-realms-voice-mute";
    }
    
    @Override
    public void initialize(VoicechatApi api) {
        plugin.getSLF4JLogger().info("Voice chat mute system initialized");
    }
    
    @Override
    public void registerEvents(EventRegistration registration) {
        registration.registerEvent(MicrophonePacketEvent.class, this::onMicrophonePacket);
    }
    
    private void onMicrophonePacket(MicrophonePacketEvent event) {
        if (event.getSenderConnection() == null) {
            return;
        }
        
        if (!(event.getSenderConnection().getPlayer().getPlayer() instanceof Player player)) {
            return;
        }
        
        if (muteManager.isPlayerMuted(player)) {
            event.cancel();
            
            UUID playerUuid = player.getUniqueId();
            String remainingTime = muteManager.getMuteRemainingTime(player);
            
            if (remainingTime == null) {
                remainingTime = "Active";
            }
            
            if (actionBarEnabled && muteManager.shouldNotifyPlayer(playerUuid)) {
                Component actionBar = languageManager.getMessage("mute.actionbar", "time", remainingTime);
                player.sendActionBar(actionBar);
            }
            
            if (chatMessageEnabled && !muteManager.hasBeenNotified(playerUuid)) {
                Component chatMessage = languageManager.getMessage("mute.chat", "time", remainingTime);
                player.sendMessage(chatMessage);
                muteManager.addNotifiedPlayer(playerUuid);
            }
            
            if (plugin.getConfig().getBoolean("debug", false)) {
                plugin.getSLF4JLogger().info("[DEBUG] Cancelled voice packet from muted player {}", player.getName());
            }
        } else {
            muteManager.removeNotifiedPlayer(player.getUniqueId());
        }
    }
    
    public void reloadConfiguration() {
        loadConfiguration();
    }
}
