package dev.keaneaudric.soulRealmsVoiceUtils.integrations.providers;

import dev.keaneaudric.soulRealmsVoiceUtils.SoulRealmsVoiceUtils;
import dev.keaneaudric.soulRealmsVoiceUtils.integrations.MuteProvider;
import litebans.api.Database;
import org.bukkit.Bukkit;
import org.bukkit.entity.Player;
import org.slf4j.Logger;
import org.jetbrains.annotations.Nullable;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.util.Map;
import java.util.UUID;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.TimeUnit;

public class LiteBansMuteProvider implements MuteProvider {
    
    private static final long CACHE_TTL_MILLIS = 2000;
    private static final long ASYNC_REQUEST_COOLDOWN_MILLIS = 500;
    
    private final boolean enabled;
    private final Logger logger;
    private final SoulRealmsVoiceUtils plugin;
    private final Map<UUID, CacheEntry> cache;
    private final Map<UUID, Long> lastAsyncRequest;
    
    public LiteBansMuteProvider() {
        this.enabled = Bukkit.getPluginManager().getPlugin("LiteBans") != null;
        this.plugin = SoulRealmsVoiceUtils.getInstance();
        this.logger = plugin.getSLF4JLogger();
        this.cache = new ConcurrentHashMap<>();
        this.lastAsyncRequest = new ConcurrentHashMap<>();
        if (enabled) {
            logger.info("LiteBans integration enabled");
        }
    }
    
    @Override
    public String getName() {
        return "LiteBans";
    }
    
    @Override
    public boolean isEnabled() {
        return enabled;
    }
    
    @Override
    public boolean isPlayerMuted(Player player) {
        if (!enabled) {
            return false;
        }
        
        if (Bukkit.isPrimaryThread()) {
            ensureFreshAsync(player);
            CacheEntry entry = cache.get(player.getUniqueId());
            return entry != null && entry.muted;
        }
        
        try {
            boolean muted = Database.get().isPlayerMuted(player.getUniqueId(), null);
            String remainingTime = muted ? queryRemainingTime(player.getUniqueId()) : null;
            cache.put(player.getUniqueId(), new CacheEntry(muted, remainingTime, System.currentTimeMillis()));
            return muted;
        } catch (Exception e) {
            logger.warn("Error checking mute status with LiteBans: {}", e.getMessage());
            return false;
        }
    }
    
    @Override
    @Nullable
    public String getMuteRemainingTime(Player player) {
        if (!enabled) {
            return null;
        }
        
        if (Bukkit.isPrimaryThread()) {
            ensureFreshAsync(player);
            CacheEntry entry = cache.get(player.getUniqueId());
            if (entry == null || !entry.muted) {
                return null;
            }
            return entry.remainingTime != null ? entry.remainingTime : "Active";
        }
        
        try {
            String remainingTime = queryRemainingTime(player.getUniqueId());
            cache.put(player.getUniqueId(), new CacheEntry(true, remainingTime, System.currentTimeMillis()));
            return remainingTime != null ? remainingTime : "Active";
        } catch (SQLException e) {
            if (SoulRealmsVoiceUtils.getInstance() != null && 
                SoulRealmsVoiceUtils.getInstance().getConfig().getBoolean("debug", false)) {
                logger.warn("[DEBUG] Error getting mute time: {}", e.getMessage());
            }
        }
        
        return "Active";
    }
    
    private void ensureFreshAsync(Player player) {
        UUID uuid = player.getUniqueId();
        long now = System.currentTimeMillis();
        CacheEntry entry = cache.get(uuid);
        if (entry != null && now - entry.updatedAt <= CACHE_TTL_MILLIS) {
            return;
        }
        
        Long lastRequest = lastAsyncRequest.get(uuid);
        if (lastRequest != null && now - lastRequest < ASYNC_REQUEST_COOLDOWN_MILLIS) {
            return;
        }
        
        lastAsyncRequest.put(uuid, now);
        Bukkit.getScheduler().runTaskAsynchronously(plugin, () -> {
            try {
                boolean muted = Database.get().isPlayerMuted(uuid, null);
                String remainingTime = muted ? queryRemainingTime(uuid) : null;
                cache.put(uuid, new CacheEntry(muted, remainingTime, System.currentTimeMillis()));
            } catch (Exception e) {
                logger.warn("Error checking mute status with LiteBans: {}", e.getMessage());
            }
        });
    }
    
    @Nullable
    private String queryRemainingTime(UUID uuid) throws SQLException {
        String query = "SELECT until FROM {mutes} WHERE uuid = ? AND active = 1 AND (until = -1 OR until > ?) LIMIT 1";
        try (PreparedStatement ps = Database.get().prepareStatement(query)) {
            ps.setString(1, uuid.toString());
            ps.setLong(2, System.currentTimeMillis());
            
            try (ResultSet rs = ps.executeQuery()) {
                if (rs.next()) {
                    long until = rs.getLong("until");
                    
                    if (until == -1) {
                        return "Permanent";
                    }
                    
                    long remaining = until - System.currentTimeMillis();
                    if (remaining > 0) {
                        return formatTime(remaining);
                    }
                }
            }
        }
        
        return null;
    }
    
    private String formatTime(long millis) {
        long days = TimeUnit.MILLISECONDS.toDays(millis);
        long hours = TimeUnit.MILLISECONDS.toHours(millis) % 24;
        long minutes = TimeUnit.MILLISECONDS.toMinutes(millis) % 60;
        long seconds = TimeUnit.MILLISECONDS.toSeconds(millis) % 60;
        
        if (days > 0) {
            return String.format("%dd %dh %dm", days, hours, minutes);
        } else if (hours > 0) {
            return String.format("%dh %dm %ds", hours, minutes, seconds);
        } else if (minutes > 0) {
            return String.format("%dm %ds", minutes, seconds);
        } else {
            return String.format("%ds", seconds);
        }
    }
    
    @Override
    public int getPriority() {
        return 100;
    }
    
    private static final class CacheEntry {
        private final boolean muted;
        private final String remainingTime;
        private final long updatedAt;
        
        private CacheEntry(boolean muted, @Nullable String remainingTime, long updatedAt) {
            this.muted = muted;
            this.remainingTime = remainingTime;
            this.updatedAt = updatedAt;
        }
    }
}
