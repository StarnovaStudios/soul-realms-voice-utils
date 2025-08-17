package dev.keaneaudric.soulRealmsVoiceUtils.hooks;

import dev.keaneaudric.soulRealmsVoiceUtils.SoulRealmsVoiceUtils;
import dev.keaneaudric.soulRealmsVoiceUtils.managers.VoiceChatManager;
import dev.keaneaudric.soulRealmsVoiceUtils.managers.MuteManager;
import dev.keaneaudric.soulRealmsVoiceUtils.managers.LanguageManager;
import me.clip.placeholderapi.expansion.PlaceholderExpansion;
import org.bukkit.entity.Player;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

public class PlaceholderAPIHook extends PlaceholderExpansion {
    
    private final SoulRealmsVoiceUtils plugin;
    private final VoiceChatManager voiceChatManager;
    private final MuteManager muteManager;
    private final LanguageManager languageManager;
    
    public PlaceholderAPIHook(SoulRealmsVoiceUtils plugin, VoiceChatManager voiceChatManager, MuteManager muteManager, LanguageManager languageManager) {
        this.plugin = plugin;
        this.voiceChatManager = voiceChatManager;
        this.muteManager = muteManager;
        this.languageManager = languageManager;
    }
    
    @Override
    public @NotNull String getIdentifier() {
        return "voicechat";
    }
    
    @Override
    public @NotNull String getAuthor() {
        return "KeaneAudric";
    }
    
    @Override
    public @NotNull String getVersion() {
        return plugin.getPluginMeta().getVersion();
    }
    
    @Override
    public boolean persist() {
        return true;
    }
    
    @Override
    public boolean canRegister() {
        return true;
    }
    
    @Override
    public @Nullable String onPlaceholderRequest(Player player, @NotNull String params) {
        if (player == null) {
            return null;
        }
        
        if (!params.equalsIgnoreCase("status")) {
            return null;
        }
        
        if (muteManager != null && muteManager.isPlayerMuted(player)) {
            return languageManager.getRawMessage("voice_status.muted");
        }
        
        VoiceChatManager.VoiceStatus status = voiceChatManager.getPlayerVoiceStatus(player);
        String key = "voice_status." + status.name().toLowerCase();
        return languageManager.getRawMessage(key);
    }
}
