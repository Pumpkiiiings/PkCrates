package com.pumpkings.pkcrates.presentation.menu;

import java.util.HashMap;
import java.util.Map;
import java.util.UUID;

import com.pumpkings.pkcrates.presentation.listener.ChatPromptManager;
import com.pumpkings.pkcrates.infrastructure.config.MenuConfigManager;

/**
 * Service in charge of tracking active menus per player.
 */
public class MenuManager {

    private final Map<UUID, PkMenu> openMenus;
    private ChatPromptManager promptManager;
    private MenuConfigManager menuConfigManager;

    public MenuManager(MenuConfigManager menuConfigManager) {
        this.openMenus = new HashMap<>();
        this.menuConfigManager = menuConfigManager;
    }

    public void setPromptManager(ChatPromptManager promptManager) {
        this.promptManager = promptManager;
    }
    
    public MenuConfigManager getMenuConfigManager() {
        return menuConfigManager;
    }
    
    public void setMenuConfigManager(MenuConfigManager menuConfigManager) {
        this.menuConfigManager = menuConfigManager;
    }

    public ChatPromptManager getPromptManager() {
        return promptManager;
    }

    public void registerOpenMenu(UUID playerId, PkMenu menu) {
        openMenus.put(playerId, menu);
    }

    public void unregisterMenu(UUID playerId) {
        openMenus.remove(playerId);
    }

    public PkMenu getOpenMenu(UUID playerId) {
        return openMenus.get(playerId);
    }
}
