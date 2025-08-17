package dev.keaneaudric.soulRealmsVoiceUtils.integrations;

import org.bukkit.entity.Player;
import org.jetbrains.annotations.Nullable;

public interface MuteProvider {
    
    String getName();
    
    boolean isEnabled();
    
    boolean isPlayerMuted(Player player);
    
    @Nullable
    String getMuteRemainingTime(Player player);
    
    int getPriority();
}
