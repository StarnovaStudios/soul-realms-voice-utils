package dev.keaneaudric.soulRealmsVoiceUtils.managers;

import dev.keaneaudric.soulRealmsVoiceUtils.SoulRealmsVoiceUtils;
import net.kyori.adventure.text.Component;
import net.kyori.adventure.text.minimessage.MiniMessage;
import net.kyori.adventure.text.minimessage.tag.resolver.Placeholder;
import org.bukkit.configuration.file.FileConfiguration;
import org.bukkit.configuration.file.YamlConfiguration;
import java.io.File;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.util.HashMap;
import java.util.Map;

public class LanguageManager {
    
    private final SoulRealmsVoiceUtils plugin;
    private final MiniMessage miniMessage;
    private FileConfiguration languageConfig;
    private final Map<String, String> messages;
    
    public LanguageManager(SoulRealmsVoiceUtils plugin) {
        this.plugin = plugin;
        this.miniMessage = MiniMessage.miniMessage();
        this.messages = new HashMap<>();
        loadLanguageFile();
    }
    
    private void loadLanguageFile() {
        String language = plugin.getConfig().getString("language", "en_us");
        File languageFile = new File(plugin.getDataFolder(), "languages/" + language + ".yml");
        
        if (!languageFile.exists()) {
            if (language.equals("en_us")) {
                plugin.saveResource("languages/en_us.yml", false);
            } else {
                plugin.getSLF4JLogger().warn("Language file {}.yml not found! Falling back to en_us", language);
                language = "en_us";
                languageFile = new File(plugin.getDataFolder(), "languages/en_us.yml");
                if (!languageFile.exists()) {
                    plugin.saveResource("languages/en_us.yml", false);
                }
            }
        }
        
        languageConfig = YamlConfiguration.loadConfiguration(languageFile);
        
        InputStream defaultStream = plugin.getResource("languages/" + language + ".yml");
        if (defaultStream != null) {
            YamlConfiguration defaultConfig = YamlConfiguration.loadConfiguration(new InputStreamReader(defaultStream));
            languageConfig.setDefaults(defaultConfig);
        }
        
        loadMessages();
    }
    
    private void loadMessages() {
        messages.clear();
        for (String key : languageConfig.getKeys(true)) {
            if (!languageConfig.isConfigurationSection(key)) {
                messages.put(key, languageConfig.getString(key, ""));
            }
        }
    }
    
    public String getRawMessage(String key) {
        return messages.getOrDefault(key, key);
    }

    public Component getMessage(String key, String placeholder, String value) {
        String message = getRawMessage(key);
        if (message.contains("<prefix>")) {
            String prefix = getRawMessage("prefix");
            message = message.replace("<prefix>", prefix);
        }
        return miniMessage.deserialize(message, Placeholder.parsed(placeholder, value));
    }
    
    public void reload() {
        loadLanguageFile();
    }
}
