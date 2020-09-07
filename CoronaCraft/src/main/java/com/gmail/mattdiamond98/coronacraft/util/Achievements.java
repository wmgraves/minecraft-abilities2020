package com.gmail.mattdiamond98.coronacraft.util;

import com.gmail.mattdiamond98.coronacraft.CoronaCraft;
import com.tommytony.war.event.WarPlayerLeaveEvent;
import net.md_5.bungee.api.chat.ComponentBuilder;
import net.md_5.bungee.api.chat.HoverEvent;
import net.md_5.bungee.api.chat.TextComponent;
import org.bukkit.Bukkit;
import org.bukkit.ChatColor;
import org.bukkit.Material;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.Listener;
import org.bukkit.event.player.PlayerJoinEvent;
import org.bukkit.inventory.ItemStack;
import org.bukkit.inventory.meta.BookMeta;
import org.bukkit.scheduler.BukkitRunnable;
import org.bukkit.util.io.BukkitObjectInputStream;
import org.bukkit.util.io.BukkitObjectOutputStream;
import java.io.FileInputStream;
import java.io.FileOutputStream;
import java.io.IOException;
import java.time.LocalDateTime;
import java.time.ZoneId;
import java.time.format.DateTimeFormatter;
import java.util.HashMap;
import java.util.LinkedList;
import java.util.Map;
import java.util.UUID;
import java.util.zip.GZIPInputStream;
import java.util.zip.GZIPOutputStream;

public class Achievements implements Listener {
    private static transient final long serialVersionUID = 4966211451999538092L;
    private static String folderPath;
    private static String fileType;
    private static int autosaveInterval = 30 * 60;
    private static HashMap<UUID, HashMap<String, String>> achievementData = new HashMap<>();
    private static HashMap<String, String> achievementInfo = new HashMap<>();
    private static HashMap<UUID, LinkedList<String>> bankedAchievements = new HashMap<>();

    public Achievements() {}

    /**
     * Creates autosave runnable and populates achievementInfo HashMap
     */
    public static void initialize() {
        loadData();

        // Create runnable to save regularly
        BukkitRunnable run = new BukkitRunnable() {
            @Override
            public void run() {
                saveData();
            }
        };
        run.runTaskTimer(CoronaCraft.instance, 20 * autosaveInterval, 20 * autosaveInterval);

        // Fill achievementInfo
        achievementInfo.put("First Kill", "Kill an enemy");
        achievementInfo.put("Ten Kills", "Kill ten enemies");
        achievementInfo.put("One Hundred Kills", "Kill one hundred enemies");
        achievementInfo.put("Two Hundred Kills", "Kill two hundred enemies");
        achievementInfo.put("First Assist", "Assist with killing an enemy");
        achievementInfo.put("Ten Assists", "Assist with killing ten enemies");
        achievementInfo.put("One Hundred Assists", "Assist with killing one hundred enemies");
        achievementInfo.put("Two Hundred Assists", "Assist with killing two hundred enemies");
        achievementInfo.put("First Death", "Die");
        achievementInfo.put("Ten Deaths", "Die ten times");
        achievementInfo.put("One Hundred Deaths", "Die one hundred times");
        achievementInfo.put("Two Hundred Deaths", "Die two hundred times");
        achievementInfo.put("First Capture", "Capture an enemy flag");
        achievementInfo.put("Ten Captures", "Capture ten enemy flags");
        achievementInfo.put("One Hundred Captures", "Capture one hundred enemy flags");
        achievementInfo.put("Two Hundred Captures", "Capture two hundred enemy flags");
        achievementInfo.put("First Win", "Win a game");
        achievementInfo.put("Ten Wins", "Win ten games");
        achievementInfo.put("One Hundred Wins", "Win one hundred games");
        achievementInfo.put("Two Hundred Wins", "Win two hundred games");
        achievementInfo.put("First Game", "Play a game");
        achievementInfo.put("Ten Games", "Play ten games");
        achievementInfo.put("One Hundred Games", "Play one hundred games");
        achievementInfo.put("Two Hundred Games", "Play two hundred games");
        achievementInfo.put("Kill an Admin", "Kill one of the server admins");
        achievementInfo.put("Kill a Developer", "Kill one of the plugin developers");
        achievementInfo.put("Kill the Owner", "Kill Malatak1");
        achievementInfo.put("Kill an Architect", "Kill one of the architects");
        achievementInfo.put("Best Time to Play", "Play on a Thursday at 8:00PM EST");
        achievementInfo.put("Triple Kill", "Kill three enemies without dying");
        achievementInfo.put("Quadruple Kill", "Kill four enemies without dying");
        achievementInfo.put("Ten Kills in a Game", "Kill ten enemies in one game");
        achievementInfo.put("Two Cap. in a Game", "Capture two flags in one game");
        achievementInfo.put("Play All Classes", "Play with all available classes");
        achievementInfo.put("Kill with All Classes", "Kill at least one enemy with each classes");
        achievementInfo.put("Kill All Classes", "Kill at least one enemy playing every class");
        achievementInfo.put("Guaranteed Win", "Win a game while on Arvein's team");
        achievementInfo.put("Extreme Death Streak", "Die twenty times in a row");
    }

    /**
     * Saves all achievements data to /CoronaCraft/achievements/data.txt
     * @return Whether the save was successful
     */
    public static boolean saveData() {
        System.out.println("Saving achievements data...");
        try {
            // Save achievements data
            BukkitObjectOutputStream out = new BukkitObjectOutputStream(new GZIPOutputStream(new
                    FileOutputStream(folderPath + "data" + fileType)));
            out.writeObject(achievementData);
            out.close();

            System.out.println("Achievements data saved successfully");
            return true;
        }
        catch (IOException e) {
            System.out.println(e.getStackTrace());
            Bukkit.broadcastMessage(ChatColor.AQUA + "ERROR: Failed to save achievements data - report" +
                    " this to @the_oshawott on discord immediately");
            return false;
        }
    }

    /**
     * Loads all data form /CoronaCraft/achievements/data.txt
     * @return Whether it was successful
     */
    private static boolean loadData() {
        System.out.println("Loading achievements data...");
        try {
            // Get file path and names
            folderPath = CoronaCraft.instance.getDataFolder().getAbsolutePath() + "/achievements/";
            fileType = ".txt";

            // Load data
            BukkitObjectInputStream in = new BukkitObjectInputStream(new GZIPInputStream(
                    new FileInputStream(folderPath + "data" + fileType)));
            achievementData = (HashMap<UUID, HashMap<String, String>>) in.readObject();
            in.close();

            System.out.println("Achievements data loaded successfully");
            return true;
        }
        catch (ClassNotFoundException | IOException e) {
            System.out.println("data.txt not found");
            return false;
        }
    }

    /**
     * Awards not-yet earned achievements to players and stores them in a list to be announced at
     * the end of the game.
     * @param player The player who earned the achievement
     * @param achievementName The nme of the achievement
     * @return Whether the achievement was awarded
     */
    private static boolean bankAchievement(Player player, String achievementName) {
        // Check that achievement exists
        if (!achievementInfo.containsKey(achievementName)) {
            Bukkit.broadcastMessage(ChatColor.AQUA + "ERROR: Achievement name \"" + achievementName +
                    "\" not recognized - report this to @the_oshawott on discord immediately");
            return false;
        }

        // Check whether player already earned the achievement
        HashMap<String, String> playerData = achievementData.get(player.getUniqueId());
        if (playerData.get(achievementName) != null) {
            return false;
        }

        // Update playerData and bank the achievement
        LocalDateTime date = LocalDateTime.now(ZoneId.of("America/New_York")); // EST IS BEST TIMEZONE
        String formattedDate = date.format(DateTimeFormatter.ofPattern("MM/dd/yy"));
        playerData.replace(achievementName, formattedDate);

        if (!bankedAchievements.containsKey(player.getUniqueId())) {
            bankedAchievements.put(player.getUniqueId(), new LinkedList<String>());
        }
        bankedAchievements.get(player.getUniqueId()).add(achievementName);
        return true;
    }

    /**
     * Announce new achievements for all players.
     */
    public static void announceAllAchievements() {
        // Announce all banked achievements
        if (bankedAchievements.size() > 0) {
            for (Map.Entry<UUID, LinkedList<String>> entry : bankedAchievements.entrySet()) {
                // Announce all achievements
                for (String achievementName : entry.getValue()) {
                    broadcastAchievement(Bukkit.getOfflinePlayer(entry.getKey()).getPlayer(),
                            achievementName);
                }

                // Remove from HashMap
                bankedAchievements.remove(entry.getKey());
            }
        }
    }

    /**
     * Displays an achievement a player earned to everyone on the server
     * NOTE: Does not check whether an achievement has been announced previously
     * @param player The player who earned the achievements
     * @param achievementName The name of the achievement (must match achievementInfo name)
     */
    private static void broadcastAchievement(Player player, String achievementName) {
        // Check that achievement exists
        if (!achievementInfo.containsKey(achievementName)) {
            Bukkit.broadcastMessage(ChatColor.AQUA + "ERROR: Achievement name \"" + achievementName +
                    "\" not recognized - report this to @the_oshawott on discord immediately");
            return;
        }

        // Broadcast the achievement with a hover description window
        String achievementDescription = achievementInfo.get(achievementName);
        TextComponent message = new TextComponent(player.getName() + " has completed the achievement ");
        TextComponent interactiveBit = new TextComponent("[" + achievementName + "]");
        interactiveBit.setColor(ChatColor.GREEN.asBungee());
        interactiveBit.setHoverEvent(new HoverEvent(HoverEvent.Action.SHOW_TEXT, new ComponentBuilder(
                achievementDescription).create()));
        message.addExtra(interactiveBit);
        Bukkit.getServer().spigot().broadcast(message);
    }

    /**
     * Cleans up all of the player's achievement data and puts a progress book in their inventory
     * @param event The PlayerJoinEvent
     */
    @EventHandler
    public void playerJoinedServer(PlayerJoinEvent event) {
        // Check whether player already exists in achievementsData
        Player player = event.getPlayer();
        if (!achievementData.containsKey(player.getUniqueId())) {
            achievementData.put(player.getUniqueId(), new HashMap<String, String>());
        }

        // Remove unused achievement names to reduce the size of the data file
        HashMap<String, String> playerData = achievementData.get(player.getUniqueId());
        for (Map.Entry<String, String> entry : playerData.entrySet()) {
            if (!achievementInfo.containsKey(entry.getKey())) {
                playerData.remove(entry.getKey());
            }
        }

        // Add any missing achievement names
        for (Map.Entry<String, String> entry : achievementInfo.entrySet()) {
            if (!playerData.containsKey(entry.getKey())) {
                playerData.put(entry.getKey(), null);
            }
        }

        // Add achievements status book to player's inventory
        updateAchievementsBook(player);
    }

    @EventHandler
    public void playerLeftGame(WarPlayerLeaveEvent event) {
        // Check whether the player is still online
        Player player = Bukkit.getPlayer(event.getQuitter());
        if (!player.isOnline()) { return; }

        // Update the achievements book in the player's inventory
        updateAchievementsBook(player);
    }

    /**
     * Removes existing progress books and creates a new one for the player to ue to check achievement
     * progress on the server.
     * @param player The player to create a new book for
     */
    private static void updateAchievementsBook(Player player) {
        // Make sure player is online before modifying inventory
        if (!player.isOnline()) { return; }
        String bookTitle = "§a§lAchievement Progress";

        // Clear any existing books from the player's inventory
        ItemStack[] items = player.getInventory().getContents();
        for (ItemStack item : items) {
            if (item == null || item.getType() != Material.WRITTEN_BOOK) { continue; }
            BookMeta bm = (BookMeta) item.getItemMeta();
            if (bm.getTitle().equals(bookTitle)) {
                player.getInventory().remove(item);
            }
        }

        // Add a custom book to the player's inventory
        ItemStack book = new ItemStack(Material.WRITTEN_BOOK, 1);
        BookMeta bm = (BookMeta) book.getItemMeta();
        bm.setAuthor("the_oshawott");
        bm.setTitle(bookTitle);

        // Book info:
        // Max number of pages: 50
        // Max number of lines per page: 13
        // Max number of characters per line: 19 (last line on each page is 18) (reduced by some formatting)
        String[] pages = new String[]{
                "§lCONTENTS§r\n" +
                        "\n" +
                        " 2: Overall Stats\n" +
                        " 3: Kill ACHV\n" +
                        " 4: Assist ACHV\n" +
                        " 5: Death ACHV\n" +
                        " 6: Capture ACHV\n" +
                        " 7: Win ACHV\n" +
                        " 8: Play ACHV\n" +
                        " 9: Miscellaneous\n" +
                        "11: Special Kills\n",
                "§lOVERALL STATS§r\n" +
                        "\n" +
                        "Kills: " + Leaderboard.getKills(player) + "\n" +
                        "Assists: " + Leaderboard.getAssists(player) + "\n" +
                        "Deaths: " + Leaderboard.getDeaths(player) + "\n" +
                        "Captures: " + Leaderboard.getCaptures(player) + "\n" +
                        "Wins: " + Leaderboard.getGamesWon(player) + "\n" +
                        "Games Played: " + Leaderboard.getGamesPlayed(player) + "\n" +
                        "\n" +
                        "Total Achievements: " + getAchievementCompletion(player),
                "§lKill ACHV§r\n" +
                        "\n" +
                        getAchievementStatus(player, "First Kill") +
                        getAchievementStatus(player, "Ten Kills") +
                        getAchievementStatus(player, "One Hundred Kills") +
                        getAchievementStatus(player, "Two Hundred Kills"),
                "§lAssist ACHV§r\n" +
                        "\n" +
                        getAchievementStatus(player, "First Assist") +
                        getAchievementStatus(player, "Ten Assists") +
                        getAchievementStatus(player, "One Hundred Assists") +
                        getAchievementStatus(player, "Two Hundred Assists"),
                "§lDeath ACHV§r\n" +
                        "\n" +
                        getAchievementStatus(player, "First Death") +
                        getAchievementStatus(player, "Ten Deaths") +
                        getAchievementStatus(player, "One Hundred Deaths") +
                        getAchievementStatus(player, "Two Hundred Deaths"),
                "§lCapture ACHV§r\n" +
                        "\n" +
                        getAchievementStatus(player, "First Capture") +
                        getAchievementStatus(player, "Ten Captures") +
                        getAchievementStatus(player, "One Hundred Captures") +
                        getAchievementStatus(player, "Two Hundred Captures"),
                "§lWin ACHV§r\n" +
                        "\n" +
                        getAchievementStatus(player, "First Win") +
                        getAchievementStatus(player, "Ten Wins") +
                        getAchievementStatus(player, "One Hundred Wins") +
                        getAchievementStatus(player, "Two Hundred Wins"),
                "§lPlay ACHV§r\n" +
                        "\n" +
                        getAchievementStatus(player, "First Game") +
                        getAchievementStatus(player, "Ten Games") +
                        getAchievementStatus(player, "One Hundred Games") +
                        getAchievementStatus(player, "Two Hundred Games"),
                "§lMiscellaneous§r\n" +
                        "\n" +
                        getAchievementStatus(player, "Best Time to Play") +
                        getAchievementStatus(player, "Play All Classes") +
                        getAchievementStatus(player, "Kill with All Classes") +
                        getAchievementStatus(player, "Kill All Classes") +
                        getAchievementStatus(player, "Guaranteed Win"),
                "§lMiscellaneous§r\n" +
                        "\n" +
                        getAchievementStatus(player, "Triple Kill") +
                        getAchievementStatus(player, "Quadruple Kill") +
                        getAchievementStatus(player, "Ten Kills in a Game") +
                        getAchievementStatus(player, "Two Cap. in a Game") +
                        getAchievementStatus(player, "Extreme Death Streak"),
                "§lSpecial Kills§r\n" +
                        "\n" +
                        getAchievementStatus(player, "Kill the Owner") +
                        getAchievementStatus(player, "Kill an Admin") +
                        getAchievementStatus(player, "Kill a Developer") +
                        getAchievementStatus(player, "Kill an Architect")
        };
        bm.setPages(pages);
        book.setItemMeta(bm);
        player.getInventory().addItem(book);
        player.updateInventory();
    }

    /**
     * Returns a formatted string showing a ratio fo achievements completed
     * @param player The player to check
     * @return The formatted string
     */
    private static String getAchievementCompletion(Player player) {
        // Safety checks
        HashMap<String, String> playerData = achievementData.get(player.getUniqueId());
        if (playerData == null || playerData.size() == 0) { return "§berror§r"; }

        // Count progress on all achievements
        int total = 0, completed = 0;
        for (Map.Entry<String, String> entry : playerData.entrySet()) {
            total++;
            if (entry.getValue() != null) {
                completed++;
            }
        }

        return completed + "/" + total;
    }

    /**
     * Returns a formatted string with achievement name and completion date
     * @param player The player to check
     * @param achievementName The name of the achievement to check
     * @return The formatted string
     */
    private static String getAchievementStatus(Player player, String achievementName) {
        // Safety checks
        HashMap<String, String> playerData = achievementData.get(player.getUniqueId());
        if (playerData == null || playerData.size() == 0 || !playerData.containsKey(achievementName)) {
            return "§berror§r";
        }

        // Create a string that contains achievement name and completion date
        String returnString = achievementName + "\n    ";
        String completionDate = playerData.get(achievementName);
        if (completionDate == null) {
            returnString += "not complete";
        }
        else {
            returnString += completionDate;
        }

        return returnString + "\n";
    }

    /**
     * Check all achievements based on total number of kills
     * @param player The player
     * @param numKills The total number of kills
     */
    public static void checkKillTotalAchievements(Player player, int numKills) {
        // Evaluate all kill total based achievements
        if (numKills >= 1) {
            bankAchievement(player, "First Kill");
        }
        if (numKills >= 10) {
            bankAchievement(player, "Ten Kills");
        }
        if (numKills >= 100) {
            bankAchievement(player, "One Hundred Kills");
        }
        if (numKills >= 200) {
            bankAchievement(player, "Two Hundred Kills");
        }
    }

    /**
     * Check all achievements based on total number of assists
     * @param player The player
     * @param numAssists Total number of assists
     */
    public static void checkAssistTotalAchievements(Player player, int numAssists) {
        // Evaluate all assist total based achievements
        if (numAssists >= 1) {
            bankAchievement(player, "First Assist");
        }
        if (numAssists >= 10) {
            bankAchievement(player, "Ten Assists");
        }
        if (numAssists >= 100) {
            bankAchievement(player, "One Hundred Assists");
        }
        if (numAssists >= 200) {
            bankAchievement(player, "Two Hundred Assists");
        }
    }

    /**
     * Checks all achievements based on total number of deaths
     * @param player The player
     * @param numDeaths Total number of deaths
     */
    public static void checkDeathTotalAchievements(Player player, int numDeaths) {
        // Evaluate all death total based achievements
        if (numDeaths >= 1) {
            bankAchievement(player, "First Death");
        }
        if (numDeaths >= 10) {
            bankAchievement(player, "Ten Deaths");
        }
        if (numDeaths >= 100) {
            bankAchievement(player, "One Hundred Deaths");
        }
        if (numDeaths >= 200) {
            bankAchievement(player, "Two Hundred Deaths");
        }
    }

    /**
     * Checks all achievements based on total number of captures
     * @param player The player
     * @param numCaptures Total number of captures
     */
    public static void checkCaptureTotalAchievements(Player player, int numCaptures) {
        // Evaluate all capture total based achievements
        if (numCaptures >= 1) {
            bankAchievement(player, "First Capture");
        }
        if (numCaptures >= 10) {
            bankAchievement(player, "Ten Captures");
        }
        if (numCaptures >= 100) {
            bankAchievement(player, "One Hundred Captures");
        }
        if (numCaptures >= 200) {
            bankAchievement(player, "Two Hundred Captures");
        }
    }

    /**
     * Checks all achievements based on total number of games played
     * @param player The player
     * @param numPlays Total number of games played
     */
    public static void checkPlayTotalAchievements(Player player, int numPlays) {
        // Evaluate all play total based achievements
        if (numPlays >= 1) {
            bankAchievement(player, "First Game");
        }
        if (numPlays >= 10) {
            bankAchievement(player, "Ten Games");
        }
        if (numPlays >= 100) {
            bankAchievement(player, "One Hundred Kills");
        }
        if (numPlays >= 200) {
            bankAchievement(player, "Two Hundred Kills");
        }
    }

    /**
     * Checks all achievements based on total number of games won
     * @param player The player
     * @param numWins Total number of games played
     */
    public static void checkWinTotalAchievements(Player player, int numWins) {
        // Evaluate all win total based achievements
        if (numWins >= 1) {
            bankAchievement(player, "First Win");
        }
        if (numWins >= 10) {
            bankAchievement(player, "Ten Wins");
        }
        if (numWins >= 100) {
            bankAchievement(player, "One Hundred Wins");
        }
        if (numWins >= 200) {
            bankAchievement(player, "Two Hundred Kills");
        }
    }

    /**
     * Checks all achievements based on special things about the killer or victim
     * @param killer The killer
     * @param victim The victim
     */
    public static void checkSpecialKillAchievements(Player killer, Player victim) {
        // Check if the victim was a special person
        if (victim != null) {
            switch (victim.getName().toLowerCase()) {
                case "malatak1":
                    bankAchievement(killer, "Kill the Owner");
                    break;
                case "the_oshawott":
                case "bucketofjava":
                case "mysterymask":
                    bankAchievement(killer, "Kill a Developer");
                    break;
                case "arcanechicken":
                case "tury13":
                case "arvein":
                    bankAchievement(killer, "Kill an Admin");
                    break;
                case "saraaaa_":
                case "charztgg":
                case "treepuncherxt":
                case "necroseus":
                case "hetbesjegaming69":
                case "fantus32":
                    bankAchievement(killer, "Kill an Architect");
                    break;
            }
        }
    }
}