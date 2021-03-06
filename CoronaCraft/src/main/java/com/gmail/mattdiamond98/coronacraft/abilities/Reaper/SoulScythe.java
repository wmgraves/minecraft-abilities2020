package com.gmail.mattdiamond98.coronacraft.abilities.Reaper;

import com.gmail.mattdiamond98.coronacraft.CoronaCraft;
import com.gmail.mattdiamond98.coronacraft.util.PlayerInteraction;
import com.tommytony.war.Team;
import org.bukkit.Color;
import org.bukkit.Location;
import org.bukkit.Particle;
import org.bukkit.attribute.Attribute;
import org.bukkit.entity.Player;
import org.bukkit.scheduler.BukkitRunnable;
import org.bukkit.util.BlockVector;
import java.util.Set;
import java.util.stream.Collectors;

public class SoulScythe extends HoeStyle {
    private static int fuelTimeSeconds = 2;
    private int ticksPerSecond = 20;
    private final int maxBlockRange = 6;
    private final double targetingTolerance = 1.5;

    private final int streamsPerSecond = 4;
    private final int streamTicks = ticksPerSecond / streamsPerSecond;
    private final int streamDurability = (Scythe.hoeMaxDurability - Scythe.hoeMinDurability) /
            (fuelTimeSeconds * streamsPerSecond);

    private final int cyclesPerSecond = 10;
    private final int cycleTicks = ticksPerSecond / cyclesPerSecond;
    private final double cycleDistance = 15 / cyclesPerSecond;

    public SoulScythe() {
        super("Soul Scythe", new String[] {
                "Spray nearby enemies with particles",
                "that steal their health and give it to you.",
                "Cost: " + Math.round(1000f / fuelTimeSeconds) / 10f + "% / second"
        }, "coronacraft.reaper.soulscythe", 0);
        cooldownTicks = streamTicks;
        fuelCost = streamDurability;
    }

    /**
     * Creates a stream of blindness particles
     * @param player The player who triggered this ability
     * @param data Unused
     * @return 0
     */
    @Override
    public int execute(Player player, Object... data) {
        // Get player's team
        Team team = Team.getTeamByPlayerName(player.getName());
        if (team == null) return 0; // Do not allow spectators to use this ability

        // Maths
        final Location firedFrom = player.getLocation().add(0, 1.5, 0);
        final Location currentLocation = player.getLocation().add(0, 1.5, 0);
        final BlockVector increment = (BlockVector) player.getLocation().getDirection()
                .toBlockVector().normalize().multiply(cycleDistance);

        // Create a runnable that acts as a stream
        BukkitRunnable run = new BukkitRunnable() {
            @Override
            public void run() {
                // Handle stopping the runnable
                if (currentLocation.distanceSquared(firedFrom) > maxBlockRange*maxBlockRange) {
                    cancel();
                }

                // Update the current stream position
                currentLocation.add(increment);
                currentLocation.getWorld().spawnParticle(Particle.REDSTONE, currentLocation, 1,
                        new Particle.DustOptions(Color.fromRGB(85, 107, 47), 3));

                // Steal a small amount of health form all nearby enemies
                Set<Player> enemies = currentLocation.getWorld().getNearbyEntities(currentLocation,
                        targetingTolerance, targetingTolerance, targetingTolerance).stream()
                        .filter(entity -> entity instanceof Player)
                        .map(player -> (Player) player)
                        .filter(player -> Team.getTeamByPlayerName(player.getName()) != null)
                        .filter(player -> !team.getPlayers().contains(player))
                        .collect(Collectors.toSet());

                if (!enemies.isEmpty()) {
                    for (Player enemy : enemies) {
                        enemy.damage(1);
                        PlayerInteraction.playerHarm(enemy, player);

                        if (player.getHealth() < player.getAttribute(Attribute.GENERIC_MAX_HEALTH).getValue()) {
                            player.setHealth(Math.min(player.getAttribute(Attribute.GENERIC_MAX_HEALTH)
                                    .getValue(), player.getHealth() + 0.5));
                        }
                    }
                }
            }
        };
        run.runTaskTimer(CoronaCraft.instance, 0, cycleTicks);

        return 0;
    }
}
