package com.gmail.mattdiamond98.coronacraft.abilities.Engineer;

import com.gmail.mattdiamond98.coronacraft.CoronaCraft;
import com.gmail.mattdiamond98.coronacraft.util.AbilityUtil;
import com.gmail.mattdiamond98.coronacraft.util.MetadataKey;
import com.sk89q.worldedit.math.BlockVector3;
import com.sk89q.worldedit.math.Vector3;
import com.tommytony.war.Team;
import org.bukkit.*;
import org.bukkit.block.Block;
import org.bukkit.craftbukkit.v1_15_R1.entity.CraftArrow;
import org.bukkit.entity.Arrow;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.Listener;
import org.bukkit.event.entity.EntityDamageByEntityEvent;
import org.bukkit.event.player.PlayerInteractEvent;
import org.bukkit.metadata.FixedMetadataValue;
import org.bukkit.scheduler.BukkitRunnable;
import org.bukkit.util.BlockVector;
import org.bukkit.util.Vector;

import java.util.Collection;
import java.util.List;
import java.util.Map;
import java.util.TreeMap;

public class Healer extends SchematicStyle {

    public static final int COST_PLANKS = 32;
    public static final int COST_COBBLE = 32;
    public static final int MAX_DISTANCE = 8;
    public static final int HEAL_AMOUNT = 2;

    private static final Map<Material, Integer> costs = new TreeMap<>();
    static {
        costs.put(Material.OAK_PLANKS,  COST_PLANKS);
        costs.put(Material.COBBLESTONE, COST_COBBLE);
    }

    public Healer() {
        super("Healer", new String[] {
                "Create a tower that heals",
                "team members standing nearby.",
                String.format("Cost: %d Planks, %d Cobblestone", COST_PLANKS, COST_COBBLE),
                "Construction Time: 5 seconds"
        }, "coronacraft.engineer.healer", 0, costs);
    }

    public void activateHealer(Player player, Location location, double direction) {
        // Prepare for finding the orb
        final World world = player.getWorld();
        int rotation = (int) Math.round(direction);
        Vector temp = new Vector(0,0,0);;
        switch (rotation) {
            case 0:
                temp = new Vector(0, 1, -2);
                break;
            case 90:
                temp = new Vector(-2, 1, 0);
                break;
            case 180:
                temp = new Vector(0, 1, 2);
                break;
            case 270:
                temp = new Vector(2, 1, 0);
                break;
            default:
                Bukkit.broadcastMessage(ChatColor.AQUA + "ERROR: UNRECOGNIZED DIRECTION VALUE - REPORT THIS");
        }
        Vector adjustment = temp.clone();
        Location roundedLocation = location.clone();
        roundedLocation.setX(roundedLocation.getBlockX());
        roundedLocation.setY(roundedLocation.getBlockY());
        roundedLocation.setZ(roundedLocation.getBlockZ());
        final Location orbLocation = roundedLocation.add(adjustment);

        // Handle healing nearby teammates
        new BukkitRunnable() {

            public void run() {
                // Handle stopping the healer
                if (orbLocation.getBlock().getType() != Material.SEA_LANTERN) {
                    player.sendMessage(ChatColor.YELLOW + "Your healer was disabled");
                    cancel();
                }
                else if (player == null) {
                    cancel();
                }

                // Heal all teammates close to the healer
                Team playerTeam = Team.getTeamByPlayerName(player.getName());
                if (playerTeam != null) {
                    for (Player current : playerTeam.getPlayers()) {
                        if (orbLocation.distanceSquared(current.getLocation()) <= MAX_DISTANCE * MAX_DISTANCE) {
                            if (current.getHealth() < 20) {
                                current.setHealth(Math.min(20, current.getHealth() + HEAL_AMOUNT));
                                current.sendMessage(ChatColor.YELLOW + "You were healed");
                            }
                        }
                    }
                }
            }
        }.runTaskTimer(CoronaCraft.instance, CoronaCraft.ABILITY_TICK_PER_SECOND * 10, 80);

        return;
    }
}
