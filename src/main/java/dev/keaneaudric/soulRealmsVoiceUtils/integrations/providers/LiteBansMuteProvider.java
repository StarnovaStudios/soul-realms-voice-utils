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
import java.util.concurrent.TimeUnit;

public class LiteBansMuteProvider implements MuteProvider {
    
    private final boolean enabled;
    private final Logger logger;
    
    public LiteBansMuteProvider() {
        this.enabled = Bukkit.getPluginManager().getPlugin("LiteBans") != null;
        SoulRealmsVoiceUtils plugin = SoulRealmsVoiceUtils.getInstance();
        this.logger = plugin.getSLF4JLogger();
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
        
        try {
            return Database.get().isPlayerMuted(player.getUniqueId(), null);
        } catch (Exception e) {
            logger.warn("Error checking mute status with LiteBans: {}", e.getMessage());
            return false;
        }
    }
    
    @Override
    @Nullable
    public String getMuteRemainingTime(Player player) {
        if (!enabled || !isPlayerMuted(player)) {
            return null;
        }
        
        try {
            String query = "SELECT until FROM {mutes} WHERE uuid = ? AND active = 1 AND (until = -1 OR until > ?) LIMIT 1";
            try (PreparedStatement ps = Database.get().prepareStatement(query)) {
                ps.setString(1, player.getUniqueId().toString());
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
        } catch (SQLException e) {
            if (SoulRealmsVoiceUtils.getInstance() != null && 
                SoulRealmsVoiceUtils.getInstance().getConfig().getBoolean("debug", false)) {
                logger.warn("[DEBUG] Error getting mute time: {}", e.getMessage());
            }
        }
        
        return "Active";
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
}
