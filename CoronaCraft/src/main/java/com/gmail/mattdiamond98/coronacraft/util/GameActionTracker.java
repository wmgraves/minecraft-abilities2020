package com.gmail.mattdiamond98.coronacraft.util;

import com.tommytony.war.event.WarPlayerJoinEvent;
import com.tommytony.war.event.WarPlayerLeaveEvent;
import org.bukkit.Bukkit;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.Listener;
import java.util.HashMap;
import java.util.UUID;

public class GameActionTracker implements Listener {
    private static HashMap<UUID, HashMap<String, Integer>> playerActions = new HashMap<>();

    /**
     * Used for registering listener in CoronaCraft
     */
    public GameActionTracker() {}

    /**
     * Creates new HashMap for the player to track their actions in the game they just joined
     * @param event The WarPlayerJoinEvent
     */
    @EventHandler
    public void playerEnteredGame(WarPlayerJoinEvent event) {
        // Make sure player does not currently have any data associated with them
        if (playerActions.containsKey(event.getPlayer().getUniqueId())) {
            playerActions.remove(event.getPlayer().getUniqueId());
        }

        // Create a new hashmap to store player's actions for the current game
        HashMap<String, Integer> temp = new HashMap<>();
        temp.put("kills", 0);
        temp.put("killstreak", 0);
        temp.put("maxkillstreak", 0);
        temp.put("assists", 0);
        temp.put("assiststreak", 0);
        temp.put("maxassiststreak", 0);
        temp.put("deaths", 0);
        temp.put("deathstreak", 0);
        temp.put("maxdeathstreak", 0);
        temp.put("captures", 0);
        playerActions.put(event.getPlayer().getUniqueId(), temp);
    }

    /**
     * Checks and removes the list of the player's actions when they leave a game
     * @param event The WarPlayerLeaveEvent
     */
    @EventHandler
    public void playerLeft(WarPlayerLeaveEvent event) {
        // Check achievements and remove all records
        Player player = Bukkit.getPlayer(event.getQuitter());
        if (playerActions.containsKey(player.getUniqueId())) {
            Achievements.checkGameActionsAchievements(player, playerActions.get(
                    player.getUniqueId()));
            playerActions.remove(player.getUniqueId());
        }
    }

    /**
     * Updates kill-based records and cancels deathstreaks
     * @param player The player to update
     */
    public static void recordKill(Player player) {
        if (playerActions.containsKey(player.getUniqueId())) {
            HashMap<String, Integer> playerData = playerActions.get(player.getUniqueId());

            playerData.replace("kills", playerData.get("kills") + 1);
            playerData.replace("killstreak", playerData.get("killstreak") + 1);
            playerData.replace("deathstreak", 0);

            if (playerData.get("killstreak") > playerData.get("maxkillstreak")) {
                playerData.replace("maxkillstreak", playerData.get("killstreak"));
            }
        }
    }

    /**
     * Updates assist-based records
     * @param player The player to update
     */
    public static void recordAssist(Player player) {
        if (playerActions.containsKey(player.getUniqueId())) {
            HashMap<String, Integer> playerData = playerActions.get(player.getUniqueId());

            playerData.replace("assists", playerData.get("assists") + 1);
            playerData.replace("assiststreak", playerData.get("assiststreak") + 1);

            if (playerData.get("assiststreak") > playerData.get("maxassiststreak")) {
                playerData.replace("maxassiststreak", playerData.get("assiststreak"));
            }
        }
    }

    /**
     * Updates death-based records
     * @param player The player to update
     */
    public static void recordDeath(Player player) {
        if (playerActions.containsKey(player.getUniqueId())) {
            HashMap<String, Integer> playerData = playerActions.get(player.getUniqueId());

            playerData.replace("deaths", playerData.get("deaths") + 1);
            playerData.replace("deathstreak", playerData.get("deathstreak") + 1);
            playerData.replace("killstreak", 0);
            playerData.replace("assiststreak", 0);

            if (playerData.get("deathstreak") > playerData.get("maxdeathstreak")) {
                playerData.replace("maxdeathstreak", playerData.get("deathstreak"));
            }
        }
    }

    /**
     * Updates capture-based records
     * @param player The player to update
     */
    public static void recordCapture(Player player) {
        if (playerActions.containsKey(player.getUniqueId())) {
            HashMap<String, Integer> playerData = playerActions.get(player.getUniqueId());

            playerData.replace("captures", playerData.get("captures") + 1);
        }
    }
}
