package dev.keaneaudric.soulRealmsVoiceUtils.listeners;

import dev.keaneaudric.soulRealmsVoiceUtils.SoulRealmsVoiceUtils;
import dev.keaneaudric.soulRealmsVoiceUtils.managers.VoiceChatManager;
import de.maxhenkel.voicechat.api.VoicechatApi;
import de.maxhenkel.voicechat.api.VoicechatPlugin;
import de.maxhenkel.voicechat.api.VoicechatServerApi;
import de.maxhenkel.voicechat.api.events.EventRegistration;
import de.maxhenkel.voicechat.api.events.MicrophonePacketEvent;
import java.util.UUID;

public class VoiceChatListener implements VoicechatPlugin {
    
    private final VoiceChatManager manager;
    private final SoulRealmsVoiceUtils plugin;
    
    public VoiceChatListener(SoulRealmsVoiceUtils plugin, VoiceChatManager manager) {
        this.plugin = plugin;
        this.manager = manager;
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
        if (event.getSenderConnection() != null) {
            UUID playerUuid = event.getSenderConnection().getPlayer().getUuid();
            manager.updatePlayerTalkTime(playerUuid);
        }
    }
}
