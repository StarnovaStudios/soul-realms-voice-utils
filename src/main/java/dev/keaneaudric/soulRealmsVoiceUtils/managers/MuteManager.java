package dev.keaneaudric.soulRealmsVoiceUtils.managers;

import dev.keaneaudric.soulRealmsVoiceUtils.SoulRealmsVoiceUtils;
import dev.keaneaudric.soulRealmsVoiceUtils.integrations.MuteProvider;
import dev.keaneaudric.soulRealmsVoiceUtils.integrations.providers.LiteBansMuteProvider;
import org.bukkit.entity.Player;
import org.jetbrains.annotations.Nullable;
import java.util.*;
import java.util.concurrent.ConcurrentHashMap;

public class MuteManager {
    
    private final SoulRealmsVoiceUtils plugin;
    private final List<MuteProvider> providers;
    private final Map<UUID, Long> lastNotification;
    private final Set<UUID> notifiedPlayers;
    
    public MuteManager(SoulRealmsVoiceUtils plugin) {
        this.plugin = plugin;
        this.providers = new ArrayList<>();
        this.lastNotification = new ConcurrentHashMap<>();
        this.notifiedPlayers = ConcurrentHashMap.newKeySet();
        
        registerProviders();
    }
    
    private void registerProviders() {
        LiteBansMuteProvider liteBansProvider = new LiteBansMuteProvider();
        if (liteBansProvider.isEnabled()) {
            registerProvider(liteBansProvider);
        }
        
        plugin.getSLF4JLogger().info("Registered {} mute provider(s)", providers.size());
    }
    
    public void registerProvider(MuteProvider provider) {
        providers.add(provider);
        providers.sort(Comparator.comparingInt(MuteProvider::getPriority).reversed());
        plugin.getSLF4JLogger().info("Registered mute provider: {}", provider.getName());
    }
    
    public boolean isPlayerMuted(Player player) {
        boolean debug = plugin.getConfig().getBoolean("debug", false);
        
        if (debug) {
            plugin.getSLF4JLogger().info("[DEBUG] Checking mute status for {}", player.getName());
        }
        
        for (MuteProvider provider : providers) {
            if (debug) {
                plugin.getSLF4JLogger().info("[DEBUG] Checking provider {} for player {}", provider.getName(), player.getName());
            }
            
            if (provider.isEnabled() && provider.isPlayerMuted(player)) {
                if (debug) {
                    plugin.getSLF4JLogger().info("[DEBUG] Player {} is muted (provider: {})", player.getName(), provider.getName());
                }
                return true;
            }
        }
        
        if (debug) {
            plugin.getSLF4JLogger().info("[DEBUG] Player {} is not muted", player.getName());
        }
        
        return false;
    }
    
    @Nullable
    public String getMuteRemainingTime(Player player) {
        for (MuteProvider provider : providers) {
            if (provider.isEnabled() && provider.isPlayerMuted(player)) {
                String time = provider.getMuteRemainingTime(player);
                if (time != null) {
                    return time;
                }
            }
        }
        return null;
    }
    
    public boolean shouldNotifyPlayer(UUID playerUuid) {
        Long lastTime = lastNotification.get(playerUuid);
        long currentTime = System.currentTimeMillis();
        
        if (lastTime == null || currentTime - lastTime > 1000) {
            lastNotification.put(playerUuid, currentTime);
            return true;
        }
        return false;
    }
    
    public void addNotifiedPlayer(UUID playerUuid) {
        notifiedPlayers.add(playerUuid);
    }
    
    public void removeNotifiedPlayer(UUID playerUuid) {
        notifiedPlayers.remove(playerUuid);
    }
    
    public boolean hasBeenNotified(UUID playerUuid) {
        return notifiedPlayers.contains(playerUuid);
    }
    
    public void cleanup() {
        providers.clear();
        lastNotification.clear();
        notifiedPlayers.clear();
    }
}
