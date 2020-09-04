package com.gmail.mattdiamond98.coronacraft.abilities.Reaper;

import com.gmail.mattdiamond98.coronacraft.CoronaCraft;
import com.gmail.mattdiamond98.coronacraft.abilities.AbilityStyle;
import com.tommytony.war.Team;
import org.bukkit.Bukkit;
import org.bukkit.entity.Player;
import org.bukkit.potion.PotionEffect;
import org.bukkit.potion.PotionEffectType;

import java.util.Set;
import java.util.stream.Collectors;

public class GraveOmen extends RoseStyle {
    private int blockRange = 5;

    public GraveOmen() {
        super("Grave Omen", new String[] {
                "Give 5 seconds of Wither II to all",
                "enemies within a 5 block radius.",
                "Cooldown: 15 seconds"
        }, 0);
        cooldownSeconds = 20;
    }

    @Override
    public int execute(Player player, Object... data) {
        // Get player's team
        Team team = Team.getTeamByPlayerName(player.getName());
        if (team == null) return 0; // Do not allow spectators to use this ability

        // Give wither 2 to all nearby enemies
        Set<Player> enemies = player.getWorld().getNearbyEntities(player.getLocation(),
                blockRange, blockRange, blockRange).stream()
                .filter(entity -> entity instanceof Player)
                .map(currentPlayer -> (Player) player)
                .filter(currentPlayer -> Team.getTeamByPlayerName(player.getName()) != null)
                .filter(currentPlayer -> !team.getPlayers().contains(player))
                .collect(Collectors.toSet());

        if (!enemies.isEmpty()) {
            for (Player enemy : enemies) {
                enemy.addPotionEffect(new PotionEffect(PotionEffectType.WITHER,
                        5 * 20, 2));
            }
        }
        return 0;
    }
}
