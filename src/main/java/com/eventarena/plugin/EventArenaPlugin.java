package com.eventarena.plugin;

import com.eventarena.plugin.command.EventCommand;
import com.eventarena.plugin.gui.EventGuiListener;
import com.eventarena.plugin.listener.EventListener;
import com.eventarena.plugin.manager.ConfigManager;
import com.eventarena.plugin.manager.EventBorderManager;
import com.eventarena.plugin.manager.EventKitManager;
import com.eventarena.plugin.manager.EventManager;
import com.eventarena.plugin.manager.EventWorldManager;
import com.eventarena.plugin.scoreboard.EventScoreboardManager;
import org.bukkit.plugin.java.JavaPlugin;

public class EventArenaPlugin extends JavaPlugin {

    private ConfigManager configManager;
    private EventManager eventManager;
    private EventWorldManager worldManager;
    private EventBorderManager borderManager;
    private EventKitManager kitManager;
    private EventScoreboardManager scoreboardManager;

    @Override
    public void onEnable() {
        configManager = new ConfigManager(this);
        configManager.load();

        worldManager = new EventWorldManager(this);
        borderManager = new EventBorderManager(this);
        kitManager = new EventKitManager(this);
        kitManager.load();
        scoreboardManager = new EventScoreboardManager(this);
        eventManager = new EventManager(this);

        getServer().getPluginManager().registerEvents(new EventListener(this), this);
        getServer().getPluginManager().registerEvents(new EventGuiListener(this), this);

        var eventCommand = new EventCommand(this);
        getCommand("event").setExecutor(eventCommand);
        getCommand("event").setTabCompleter(eventCommand);

        // Restart-safety: if the server stopped mid-event, don't try to resume
        // combat state we can't trust - just make sure nothing is left dangling.
        if (configManager.getSavedMode() != null) {
            getLogger().warning("An event (" + configManager.getSavedMode() + ") was active when the " +
                    "server last stopped. It has been reset to IDLE for safety; no players were carried over.");
            configManager.clearActiveEvent();
        }

        getLogger().info("EventArena enabled.");
    }

    @Override
    public void onDisable() {
        if (eventManager != null && eventManager.hasEvent()) {
            eventManager.stopEvent("event-stopped");
        }
        getLogger().info("EventArena disabled.");
    }

    public ConfigManager configManager() {
        return configManager;
    }

    public EventManager eventManager() {
        return eventManager;
    }

    public EventWorldManager worldManager() {
        return worldManager;
    }

    public EventBorderManager borderManager() {
        return borderManager;
    }

    public EventKitManager kitManager() {
        return kitManager;
    }

    public EventScoreboardManager scoreboardManager() {
        return scoreboardManager;
    }
}
