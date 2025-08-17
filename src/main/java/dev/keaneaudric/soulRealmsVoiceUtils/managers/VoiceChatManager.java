package dev.keaneaudric.soulRealmsVoiceUtils.managers;

import de.maxhenkel.voicechat.api.VoicechatConnection;
import de.maxhenkel.voicechat.api.VoicechatServerApi;
import org.bukkit.entity.Player;
import java.util.Map;
import java.util.UUID;
import java.util.concurrent.ConcurrentHashMap;

public class VoiceChatManager {
    
    private VoicechatServerApi voicechatApi;
    private final Map<UUID, Long> lastTalkTime = new ConcurrentHashMap<>();
    private static final long TALK_THRESHOLD_MS = 250;
    
    public void setVoicechatApi(VoicechatServerApi api) {
        this.voicechatApi = api;
    }
    
    public void updatePlayerTalkTime(UUID playerUuid) {
        lastTalkTime.put(playerUuid, System.currentTimeMillis());
    }
    
    public boolean isPlayerTalking(UUID playerUuid) {
        Long lastTalk = lastTalkTime.get(playerUuid);
        return lastTalk != null && (System.currentTimeMillis() - lastTalk) < TALK_THRESHOLD_MS;
    }
    
    public VoiceStatus getPlayerVoiceStatus(Player player) {
        if (voicechatApi == null) {
            return VoiceStatus.NOT_INSTALLED;
        }
        
        UUID playerUuid = player.getUniqueId();
        VoicechatConnection connection = voicechatApi.getConnectionOf(playerUuid);
        
        if (connection == null || !connection.isConnected()) {
            return VoiceStatus.NOT_INSTALLED;
        }
        
        if (connection.isDisabled()) {
            return VoiceStatus.DISABLED;
        }
        
        if (isPlayerTalking(playerUuid)) {
            return VoiceStatus.SPEAKING;
        }
        
        return VoiceStatus.CONNECTED;
    }
    
    public void cleanup() {
        lastTalkTime.clear();
        voicechatApi = null;
    }
    
    public enum VoiceStatus {
        NOT_INSTALLED,
        CONNECTED,
        DISABLED,
        SPEAKING,
        MUTED
    }
}
