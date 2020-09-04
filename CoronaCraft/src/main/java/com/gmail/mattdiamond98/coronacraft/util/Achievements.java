package com.gmail.mattdiamond98.coronacraft.util;

import com.gmail.mattdiamond98.coronacraft.CoronaCraft;
import com.tommytony.war.Team;
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
    private static HashMap<UUID, HashMap<String, String>> data = new HashMap<>();
    private static HashMap<String, String> achievementInfo = new HashMap<>();
    private static HashMap<UUID, LinkedList<String>> bankedAchievements = new HashMap<>();

    public Achievements() {}

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
        achievementInfo.put("Kill an Architext", "Kill an Architect");
        achievementInfo.put("Play All Classes", "Play with all available classes");
        achievementInfo.put("Kill with All Classes", "Kill at least one enemy with each classes");
        achievementInfo.put("Kill All Classes", "Kill at least one enemy playing every class");
        achievementInfo.put("Guaranteed Win", "Win a game while on Arvein's team");
        achievementInfo.put("Triple Kill", "Kill three enemies without dying");
        achievementInfo.put("Quadruple Kill", "Kill four enemies without dying");
        achievementInfo.put("Ten Kills in a Game", "Kill ten enemies in one game");
        achievementInfo.put("Two Cap. in a Game", "Capture two flags in one game");
    }

    public static boolean saveData() {
        System.out.println("Saving achievements data...");
        try {
            // Save achievements data
            BukkitObjectOutputStream out = new BukkitObjectOutputStream(new GZIPOutputStream(new
                    FileOutputStream(folderPath + "data" + fileType)));
            out.writeObject(data);
            out.close();

            System.out.println("Achievements data saved successfully");
            return true;
        }
        catch (IOException e) {
            System.out.println(e.getStackTrace());
            Bukkit.broadcastMessage(ChatColor.AQUA + "ERROR: Failed to save achievements data - report this" +
                    " to @the_oshawott on discord immediately");
            return false;
        }
    }

    private static boolean loadData() {
        System.out.println("Loading achievements data...");
        try {
            // Get file path and names
            folderPath = CoronaCraft.instance.getDataFolder().getAbsolutePath() + "/achievements/";
            fileType = ".txt";

            // Load data
            BukkitObjectInputStream in = new BukkitObjectInputStream(new GZIPInputStream(new FileInputStream(
                    folderPath + "data" + fileType)));
            data = (HashMap<UUID, HashMap<String, String>>) in.readObject();
            in.close();

            System.out.println("Achievements data loaded successfully");
            return true;
        }
        catch (ClassNotFoundException | IOException e) {
            System.out.println("data.txt not found");
            return false;
        }
    }

    private static void broadcastAchievement(Player player, String achievementName) {
        // Check that the achievement exists
        if (achievementInfo.containsKey(achievementName)) {
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
        // Display an error message
        else {
            Bukkit.broadcastMessage(ChatColor.AQUA + "ERROR: Achievement name \"" + achievementName +
                    "\" not recognized - report this to @the_oshawott on discord immediately");
        }
    }

    @EventHandler
    public static void onPlayerJoin(PlayerJoinEvent event) {
        // Add new players to data
        UUID uuid = event.getPlayer().getUniqueId();
        if (!data.containsKey(uuid)) {
            data.put(uuid, new HashMap<String, String>());
        }

        // Check that the player's hashmap contains all achievement trackers
        HashMap<String, String> playerData = data.get(uuid);
        for (String name : achievementInfo.keySet()) {
            if (!playerData.containsKey(name)) {
                playerData.put(name, null);
            }
        }

        // Give the player a book that contains their achievement progress
        updateAchievementsBook(event.getPlayer());
    }

    private static void updateAchievementsBook(Player player) {
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
        // Max number of characters per line: 19 (last line on each page is 18)
        String[] pages = new String[]{
                "§6§lCONTENTS§r\n" +
                        "\n" +
                        " 2: Overall Stats\n" +
                        " 3: Kill ACHV\n" +
                        " 4: Assist ACHV\n" +
                        " 5: Death ACHV\n" +
                        " 6: Capture ACHV\n" +
                        " 7: Win ACHV\n" +
                        " 8: Play ACHV\n" +
                        " 9: Game ACHV\n" +
                        "10: Special Kills\n" +
                        "11: Miscellaneous",
                "§6§lOVERALL STATS§r\n" +
                        "\n" +
                        "Kills: " + Leaderboard.getKills(player) + "\n" +
                        "Assists: " + Leaderboard.getAssists(player) + "\n" +
                        "Deaths: " + Leaderboard.getDeaths(player) + "\n" +
                        "Captures: " + Leaderboard.getCaptures(player) + "\n" +
                        "Wins: " + Leaderboard.getGamesWon(player) + "\n" +
                        "Games Played: " + Leaderboard.getGamesPlayed(player) + "\n" +
                        "\n" +
                        "Total Achievements: " + getAchievementCompletion(player),
                "§6§lKill Achievements§r\n" +
                        "\n" +
                        getAchievementStatus(player, "First Kill") +
                        getAchievementStatus(player, "Ten Kills") +
                        getAchievementStatus(player, "One Hundred Kills") +
                        getAchievementStatus(player, "Two Hundred Kills"),
                "§6§lAssist Achievements§r\n" +
                        "\n" +
                        getAchievementStatus(player, "First Assist") +
                        getAchievementStatus(player, "Ten Assists") +
                        getAchievementStatus(player, "One Hundred Assists") +
                        getAchievementStatus(player, "Two Hundred Assists"),
                "§6§lDeath Achievements§r\n" +
                        "\n" +
                        getAchievementStatus(player, "First Death") +
                        getAchievementStatus(player, "Ten Deaths") +
                        getAchievementStatus(player, "One Hundred Deaths") +
                        getAchievementStatus(player, "Two Hundred Deaths"),
                "§6§lCapture Achievements§r\n" +
                        "\n" +
                        getAchievementStatus(player, "First Capture") +
                        getAchievementStatus(player, "Ten Captures") +
                        getAchievementStatus(player, "One Hundred Captures") +
                        getAchievementStatus(player, "Two Hundred Captures"),
                "§6§lWin Achievements§r\n" +
                        "\n" +
                        getAchievementStatus(player, "First Win") +
                        getAchievementStatus(player, "Ten Wins") +
                        getAchievementStatus(player, "One Hundred Wins") +
                        getAchievementStatus(player, "Two Hundred Wins"),
                "§6§lPlay Achievements§r\n" +
                        "\n" +
                        getAchievementStatus(player, "First Game") +
                        getAchievementStatus(player, "Ten Games") +
                        getAchievementStatus(player, "One Hundred Games") +
                        getAchievementStatus(player, "Two Hundred Games"),
                "§6§lGame Achievements§r\n" +
                        "\n" +
                        getAchievementStatus(player, "Triple Kill") +
                        getAchievementStatus(player, "Quadruple Kill") +
                        getAchievementStatus(player, "Ten Kills in a Game") +
                        getAchievementStatus(player, "Two Cap. in a Game"),
                "§6§lSpecial Kills§r\n" +
                        "\n" +
                        getAchievementStatus(player, "Kill the Owner") +
                        getAchievementStatus(player, "Kill an Admin") +
                        getAchievementStatus(player, "Kill a Developer") +
                        getAchievementStatus(player, "Kill an Architect"),
                "§6§lMiscellaneous§r\n" +
                        "\n" +
                        getAchievementStatus(player, "Play All Classes") +
                        getAchievementStatus(player, "Kill with All Classes") +
                        getAchievementStatus(player, "Kill All Classes") +
                        getAchievementStatus(player, "Guaranteed Win")
        };
        bm.setPages(pages);
        book.setItemMeta(bm);
        player.getInventory().addItem(book);
        player.updateInventory();
    }

    private static String getAchievementCompletion(Player player) {
        HashMap<String, String> achievementData = data.get(player.getUniqueId());
        if (achievementData == null || achievementData.size() == 0) { return "error"; }

        // Count progress on all achievements
        int total = 0, completed = 0;
        for (Map.Entry<String, String> entry : achievementData.entrySet()) {
            total++;
            if (entry.getValue() != null) {
                completed++;
            }
        }

        return completed + "/" + total;
    }

    private static String getAchievementStatus(Player player, String achievementName) {
        HashMap<String, String> achievementData = data.get(player.getUniqueId());
        if (achievementData == null || achievementData.size() == 0) { return "§berror§r"; }
        if (!achievementData.containsKey(achievementName)) { return "§berror§r"; }

        // Create a string that contains achievement name and completion date
        String returnString = achievementName + "\n    ";
        String completionDate = achievementData.get(achievementName);
        if (completionDate == null) {
            returnString += "not complete";
        }
        else {
            returnString += completionDate;
        }

        return returnString + "\n";
    }

    private static String getFormattedDate() {
        LocalDateTime date = LocalDateTime.now(ZoneId.of("America/New_York")); // EST IS BEST TIMEZONE
        return date.format(DateTimeFormatter.ofPattern("MM/dd/yy"));
    }

    public static void checkAchievements() {
        for (Player player : Bukkit.getOnlinePlayers()) {
            checkLeaderboardAchievements(player);
            checkBankedAchievements(player);
            updateAchievementsBook(player);
        }
    }

    private static boolean bankAchievement(Player player, String achievementName) {
        if (player == null) { return false; }

        // Check whether player was already awarded the achievement
        HashMap<String, String> achievementData = data.get(player.getUniqueId());
        if (achievementData.get(achievementName) == null) { return false; }

        // Give the player the achievement and bank the message
        achievementData.replace(achievementName, getFormattedDate());
        if (!bankedAchievements.containsKey(player)) {
            bankedAchievements.put(player.getUniqueId(), new LinkedList<String>());
        }
        bankedAchievements.get(player.getUniqueId()).add(achievementName);

        return true;
    }

    private static void checkBankedAchievements(Player player) {
        // Check whether player has banked special achievements
        if (bankedAchievements.containsKey(player.getUniqueId())) {
            LinkedList<String> achievementInfo = bankedAchievements.get(player.getUniqueId());
            if (achievementInfo == null || achievementInfo.size() == 0) {
                bankedAchievements.remove(player.getUniqueId());
                return;
            }

            // Award all banked achievements that were not previously awarded
            for (String name : achievementInfo) {
                broadcastAchievement(player, name);
            }

            // Remove the player from the banked achievements list
            bankedAchievements.remove(player.getUniqueId());
        }
    }

    public static boolean checkSpecialAchievements(Player killer, Player victim) {
        if (killer == null || victim == null) { return false; }

        // Check if the victim was a special person
        switch (victim.getName().toLowerCase()) {
            case "malatak1":
                bankAchievement(killer, "Kill the owner");
                break;
            case "the_oshawott":
            case "bucketofjava":
            case "mysterymask":
                bankAchievement(killer, "Kill a developer");
                break;
            case "arcanechicken":
            case "tury13":
            case "arvein":
                bankAchievement(killer, "Kill an admin");
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

        // Check loadouts


        return true;
    }

    public static void checkSpecialAchievements(Team team) {
        // Handle "Guaranteed Win" - play on Arvein's team
        for (Player player : team.getPlayers()) {
            if (player.getName().toLowerCase() == "arvein") {
                // Award all players on arvein's team, then break out of the loop
                for (Player awardedPlayer : team.getPlayers()) {
                    bankAchievement(player, "Guaranteed Win");
                }
                break;
            }
        }
    }

    private static void checkLeaderboardAchievements(Player player) {
        HashMap<String, String> playerData = data.get(player.getUniqueId());

        // Check kill achievements
        int count = Leaderboard.getKills(player);
        String name = "First Kill";
        if (playerData.get(name) == null && count >= 1) {
            playerData.replace(name, getFormattedDate());
            broadcastAchievement(player, name);
        }
        name = "Ten Kills";
        if (playerData.get(name) == null && count >= 10) {
            playerData.replace(name, getFormattedDate());
            broadcastAchievement(player, name);
        }
        name = "One Hundred Kills";
        if (playerData.get(name) == null && count >= 100) {
            playerData.replace(name, getFormattedDate());
            broadcastAchievement(player, name);
        }
        name = "Two Hundred Kills";
        if (playerData.get(name) == null && count >= 200) {
            playerData.replace(name, getFormattedDate());
            broadcastAchievement(player, name);
        }

        // Check assist achievements
        count = Leaderboard.getAssists(player);
        name = "First Assist";
        if (playerData.get(name) == null && count >= 1) {
            playerData.replace(name, getFormattedDate());
            broadcastAchievement(player, name);
        }
        name = "Ten Assists";
        if (playerData.get(name) == null && count >= 10) {
            playerData.replace(name, getFormattedDate());
            broadcastAchievement(player, name);
        }
        name = "One Hundred Assists";
        if (playerData.get(name) == null && count >= 100) {
            playerData.replace(name, getFormattedDate());
            broadcastAchievement(player, name);
        }
        name = "Two Hundred Assists";
        if (playerData.get(name) == null && count >= 200) {
            playerData.replace(name, getFormattedDate());
            broadcastAchievement(player, name);
        }

        // Check death achievements
        count = Leaderboard.getDeaths(player);
        name = "First Death";
        if (playerData.get(name) == null && count >= 1) {
            playerData.replace(name, getFormattedDate());
            broadcastAchievement(player, name);
        }
        name = "Ten Deaths";
        if (playerData.get(name) == null && count >= 10) {
            playerData.replace(name, getFormattedDate());
            broadcastAchievement(player, name);
        }
        name = "One Hundred Deaths";
        if (playerData.get(name) == null && count >= 100) {
            playerData.replace(name, getFormattedDate());
            broadcastAchievement(player, name);
        }
        name = "Two Hundred Deaths";
        if (playerData.get(name) == null && count >= 200) {
            playerData.replace(name, getFormattedDate());
            broadcastAchievement(player, name);
        }

        // Check capture achievements
        count = Leaderboard.getCaptures(player);
        name = "First Capture";
        if (playerData.get(name) == null && count >= 1) {
            playerData.replace(name, getFormattedDate());
            broadcastAchievement(player, name);
        }
        name = "Ten Captures";
        if (playerData.get(name) == null && count >= 10) {
            playerData.replace(name, getFormattedDate());
            broadcastAchievement(player, name);
        }
        name = "One Hundred Captures";
        if (playerData.get(name) == null && count >= 100) {
            playerData.replace(name, getFormattedDate());
            broadcastAchievement(player, name);
        }
        name = "Two Hundred Captures";
        if (playerData.get(name) == null && count >= 200) {
            playerData.replace(name, getFormattedDate());
            broadcastAchievement(player, name);
        }

        // Check win achievements
        count = Leaderboard.getGamesWon(player);
        name = "First Win";
        if (playerData.get(name) == null && count >= 1) {
            playerData.replace(name, getFormattedDate());
            broadcastAchievement(player, name);
        }
        name = "Ten Wins";
        if (playerData.get(name) == null && count >= 10) {
            playerData.replace(name, getFormattedDate());
            broadcastAchievement(player, name);
        }
        name = "One Hundred Wins";
        if (playerData.get(name) == null && count >= 100) {
            playerData.replace(name, getFormattedDate());
            broadcastAchievement(player, name);
        }
        name = "Two Hundred Wins";
        if (playerData.get(name) == null && count >= 200) {
            playerData.replace(name, getFormattedDate());
            broadcastAchievement(player, name);
        }

        // Check play achievements
        count = Leaderboard.getGamesPlayed(player);
        name = "First Game";
        if (playerData.get(name) == null && count >= 1) {
            playerData.replace(name, getFormattedDate());
            broadcastAchievement(player, name);
        }
        name = "Ten Games";
        if (playerData.get(name) == null && count >= 10) {
            playerData.replace(name, getFormattedDate());
            broadcastAchievement(player, name);
        }
        name = "One Hundred Games";
        if (playerData.get(name) == null && count >= 100) {
            playerData.replace(name, getFormattedDate());
            broadcastAchievement(player, name);
        }
        name = "Two Hundred Games";
        if (playerData.get(name) == null && count >= 200) {
            playerData.replace(name, getFormattedDate());
            broadcastAchievement(player, name);
        }
    }
}