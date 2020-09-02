package com.gmail.mattdiamond98.coronacraft.abilities.Reaper;

import com.gmail.mattdiamond98.coronacraft.CoronaCraft;
import com.gmail.mattdiamond98.coronacraft.abilities.AbilityStyle;
import com.gmail.mattdiamond98.coronacraft.util.AbilityUtil;
import com.tommytony.war.Warzone;
import org.bukkit.Bukkit;
import org.bukkit.Color;
import org.bukkit.Location;
import org.bukkit.Particle;
import org.bukkit.entity.Player;
import org.bukkit.inventory.ItemStack;
import org.bukkit.potion.PotionEffect;
import org.bukkit.potion.PotionEffectType;
import org.bukkit.scheduler.BukkitRunnable;
import org.bukkit.util.BlockVector;
import java.util.List;

public class GhastlyScythe extends AbilityStyle {
    private int fuelTimeSeconds = 6;
    private int ticksPerSecond = 20;
    private final int maxBlockRange = 6;
    private final int targetingTolerance = 1;

    private final int streamsPerSecond = 2;
    private final int streamTicks = ticksPerSecond / streamsPerSecond;
    private final int streamDurability = (HoeStyle.hoeMaxDurability - HoeStyle.hoeMinDurability) /
            (fuelTimeSeconds * streamsPerSecond);

    private final int cyclesPerSecond = 4;
    private final int cycleTicks = ticksPerSecond / cyclesPerSecond;
    private final double cycleDistance = 1;

    public GhastlyScythe() {
        super("Ghastly Scythe", new String[] {
                "Sprays a stream of blinding particles",
                "up to 6 blocks away.",
                "Lasts 6 seconds"
        }, 0);
    }

    @Override
    public int execute(Player player, Object... data) {
        // Create a runnable that creates multiple streams
        BukkitRunnable run = new BukkitRunnable() {
            @Override
            public void run() {
                // Handle stopping the runnable
                ItemStack hoe = ((ItemStack) data[0]);
                if (hoe.getDurability() >= HoeStyle.hoeMaxDurability - HoeStyle.hoeMinDurability) {
                    Bukkit.broadcastMessage("out of durability");
                    cancel();
                }
                if (!AbilityUtil.inWarzone(player) || AbilityUtil.inSpawn(player)) {
                    Bukkit.broadcastMessage("player died/left zone");
                    cancel();
                }

                // Update item durability
                hoe.setDurability((short) Math.min(HoeStyle.hoeMaxDurability - HoeStyle.hoeMinDurability,
                        hoe.getDurability() + streamDurability));

                // Create list of enemies
                Warzone zone = Warzone.getZoneByPlayerName(player.getName());
                List<Player> players = zone.getPlayers();
                List<Player> teammates = zone.getPlayerTeam(player.getName()).getPlayers();
                players.removeAll(teammates);

                // Create stream
                final Location firedFrom = player.getLocation().add(0, 1.5, 0);
                final Location currentLocation = player.getLocation().add(0, 1.5, 0);
                final BlockVector increment = (BlockVector) player.getLocation().getDirection()
                        .toBlockVector().normalize().multiply(cycleDistance);
                BukkitRunnable run = new BukkitRunnable() {
                    @Override
                    public void run() {
                        // Handle stopping the runnable
                        if (currentLocation.distanceSquared(firedFrom) >= maxBlockRange*maxBlockRange) {
                            cancel();
                        }

                        // Apply blindness to nearby enemies
                        for (Player enemy : players) {
                            if (AbilityUtil.notInSpawn(enemy) && currentLocation.distanceSquared(
                                    enemy.getLocation()) <= targetingTolerance) {
                                enemy.addPotionEffect(new PotionEffect(PotionEffectType.BLINDNESS,
                                        5, 1));
                            }
                        }

                        // Create particle
                        currentLocation.getWorld().spawnParticle(Particle.REDSTONE, currentLocation, 1,
                                new Particle.DustOptions(Color.fromRGB(0, 0, 0),
                                        3));

                        // Move currentLocation
                        currentLocation.add(increment);
                    }
                };
                run.runTaskTimer(CoronaCraft.instance, cycleTicks, cycleTicks);
            }
        };
        run.runTaskTimer(CoronaCraft.instance, 0, streamTicks);

        return 0;
    }
}
