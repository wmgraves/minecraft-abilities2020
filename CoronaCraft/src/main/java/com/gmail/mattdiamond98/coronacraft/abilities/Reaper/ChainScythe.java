package com.gmail.mattdiamond98.coronacraft.abilities.Reaper;

import com.gmail.mattdiamond98.coronacraft.CoronaCraft;
import com.gmail.mattdiamond98.coronacraft.util.AbilityUtil;
import com.tommytony.war.Team;
import org.bukkit.*;
import org.bukkit.entity.Player;
import org.bukkit.inventory.ItemStack;
import org.bukkit.scheduler.BukkitRunnable;
import org.bukkit.util.BlockVector;
import org.bukkit.util.Vector;
import java.util.Set;
import java.util.stream.Collectors;

public class ChainScythe extends HoeStyle {
    private static int fuelTimeSeconds = 3;
    private int ticksPerSecond = 20;
    private final double fireSpeed = 20d / ticksPerSecond;
    private final double targetingTolerance = 0.5;

    private final int cyclesPerSecond = 20;
    private final int cycleTicks = ticksPerSecond / cyclesPerSecond;

    public ChainScythe() {
        super("Chain Scythe", new String[] {
                "Throw your scythe at a nearby enemy. If it",
                "hits one, it deals 3 hearts of damage and",
                "returns to your inventory immediately."
        }, "coronacraft.reaper.chainscythe", 0);
        cooldownTicks = 0;
        fuelCost = 0;
    }

    /**
     * Creates a stream of blindness particles
     * @param player The player who triggered this ability
     * @param data Unused
     * @return 0
     */
    @Override
    public int execute(Player player, Object... data) {
        // Remove the player's scythe
        ItemStack scythe = player.getInventory().getItemInMainHand();
        if (scythe == null || scythe.getType() != Material.DIAMOND_HOE) { return 0; }
        player.getInventory().remove(scythe);

        // Get player's team
        Team team = Team.getTeamByPlayerName(player.getName());
        if (team == null) return 0; // Do not allow spectators to use this ability

        // Maths
        final Location currentLocation = player.getLocation().add(0, 1.5, 0);
        final BlockVector velocity = (BlockVector) player.getLocation().getDirection()
                .toBlockVector().normalize().multiply(fireSpeed * (ticksPerSecond / cyclesPerSecond));
        final BlockVector gravity = new Vector(0, -0.08, 0).multiply(ticksPerSecond / cyclesPerSecond).toBlockVector();

        // Create a runnable that acts as a thrown scythe
        BukkitRunnable run = new BukkitRunnable() {
            @Override
            public void run() {
                // Handle stopping the runnable
                if (currentLocation.getBlockY() <= 1 || currentLocation.getBlock().getType() != Material.AIR) {
                    Bukkit.getScheduler().scheduleSyncDelayedTask(CoronaCraft.instance, () -> {
                        // Check that the player does not currently have a scythe in their inventory
                        boolean canReturnScythe = AbilityUtil.inWarzone(player);
                        if (canReturnScythe) {
                            for (ItemStack item : player.getInventory().getContents()) {
                                if (item != null && item.getType() == Material.DIAMOND_HOE) {
                                    canReturnScythe = false;
                                    break;
                                }
                            }

                            // Return the scythe
                            if (canReturnScythe) {
                                player.getInventory().addItem(scythe);
                                ReaperCooldownTracker.setCooldown(player, ticksPerSecond);
                            }
                        }
                    }, 5 * ticksPerSecond);
                    cancel();
                }

                // Update the current stream position
                currentLocation.add(velocity);
                velocity.add(gravity);
                currentLocation.getWorld().spawnParticle(Particle.REDSTONE, currentLocation, 1,
                        new Particle.DustOptions(Color.fromRGB(50, 50, 50), 1));

                // Hurt any nearby enemies
                Set<Player> enemies = currentLocation.getWorld().getNearbyEntities(currentLocation,
                        targetingTolerance, targetingTolerance, targetingTolerance).stream()
                        .filter(entity -> entity instanceof Player)
                        .map(player -> (Player) player)
                        .filter(player -> Team.getTeamByPlayerName(player.getName()) != null)
                        .filter(player -> !team.getPlayers().contains(player))
                        .collect(Collectors.toSet());

                if (!enemies.isEmpty()) {
                    Player enemy = (Player) enemies.toArray()[0];
                    enemy.damage(6);

                    // Check that the player does not currently have a scythe in their inventory
                    boolean canReturnScythe = AbilityUtil.inWarzone(player);
                    if (canReturnScythe) {
                        for (ItemStack item : player.getInventory().getContents()) {
                            if (item != null && item.getType() == Material.DIAMOND_HOE) {
                                canReturnScythe = false;
                                break;
                            }
                        }

                        // Return the scythe
                        if (canReturnScythe) {
                            player.getInventory().addItem(scythe);
                            ReaperCooldownTracker.setCooldown(player, ticksPerSecond);
                        }
                    }
                }
            }
        };
        run.runTaskTimer(CoronaCraft.instance, 0, cycleTicks);

        return 0;
    }
}
