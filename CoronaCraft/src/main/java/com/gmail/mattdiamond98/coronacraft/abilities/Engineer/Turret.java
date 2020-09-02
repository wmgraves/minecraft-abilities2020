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

import java.util.*;

public class Turret extends SchematicStyle implements Listener {

    public static final int COST_PLANKS = 32;
    public static final int COST_COBBLE = 32;
    public final int MAX_RANGE = 15;
    public final int MAX_SPREAD = 8;
    public static final int NUM_ARROWS = 32;
    public final int DAMAGE = 3;

    private static final ArrayList<UUID> playersWithActiveTurrets = new ArrayList<>();
    private static final Map<Material, Integer> costs = new TreeMap<>();
    static {
        costs.put(Material.OAK_PLANKS,  COST_PLANKS);
        costs.put(Material.COBBLESTONE, COST_COBBLE);
    }

    public Turret() {
        super("Turret", new String[] {
                "Create a turret that attacks enemies in front of it",
                "with up to " + NUM_ARROWS + " arrows. Limit: 1 active turret",
                String.format("Cost: %d Planks, %d Cobblestone", COST_PLANKS, COST_COBBLE),
                "Construction Time: 5 seconds"
        }, "coronacraft.engineer.turret", 0, costs);
    }

    public boolean playerHasActiveTurret(Player player) {
        return playersWithActiveTurrets.contains(player.getUniqueId());
    }

    public void activateTurret(Player player, Location location, double direction) {
        // Prepare for finding the turret
        final World world = player.getWorld();
        int rotation = (int) Math.round(direction);
        Vector temp = new Vector(0,0,0);;
        switch (rotation) {
            case 0:
                temp = new Vector(0, 1, -1);
                break;
            case 90:
                temp = new Vector(-1, 1, 0);
                break;
            case 180:
                temp = new Vector(0, 1, 1);
                break;
            case 270:
                temp = new Vector(1, 1, 0);
                break;
            default:
                Bukkit.broadcastMessage(ChatColor.AQUA + "ERROR: Unrecognized direction value - report this to @Developer on discord immediately");
        }
        Vector adjustment = temp.clone();
        Location roundedLocation = location.clone();
        roundedLocation.setX(roundedLocation.getBlockX());
        roundedLocation.setY(roundedLocation.getBlockY());
        roundedLocation.setZ(roundedLocation.getBlockZ());
        final Location turretLocation = roundedLocation.add(adjustment);

        // Determine locations used for targeting enemies with arrows
        temp.setY(0);
        final Location arrowSpawnLocation = turretLocation.clone().add(temp).add(new Vector(0.5, 0.5, 0.5));

        final Location endOfRange = arrowSpawnLocation.clone().add(temp.multiply(MAX_RANGE));
        if (temp.getBlockX() == 0) {
            temp.setX(MAX_SPREAD);
            temp.setZ(0);
        }
        else {
            temp.setX(0);
            temp.setZ(MAX_SPREAD);
        }
        final Location point2 = endOfRange.clone().add(temp);
        final Location point3 = endOfRange.clone().add(temp.multiply(-1));

        playersWithActiveTurrets.add(player.getUniqueId());

        // Handle firing arrows from the dispenser
        new BukkitRunnable() {
            private int arrowSupply = NUM_ARROWS;

            public void run() {
                // Handle stopping the turret
                if (world.getBlockAt(turretLocation).getType() != Material.DISPENSER) {
                    player.sendMessage(ChatColor.YELLOW + "Your turret was disabled");
                    playersWithActiveTurrets.remove(player.getUniqueId());
                    cancel();
                }
                else if (arrowSupply <= 0) {
                    player.sendMessage(ChatColor.YELLOW + "Your turret ran out of arrows");
                    world.getBlockAt(turretLocation).breakNaturally();
                    playersWithActiveTurrets.remove(player.getUniqueId());
                    cancel();
                }

                // Handle firing the turret
                else {
                    world.spawnParticle(Particle.REDSTONE, arrowSpawnLocation, 1,
                            new Particle.DustOptions(Color.fromRGB(0, 0, 255),
                                    2));
                    world.spawnParticle(Particle.REDSTONE, endOfRange, 1,
                            new Particle.DustOptions(Color.fromRGB(0, 255, 0),
                                    2));
                    world.spawnParticle(Particle.REDSTONE, point2, 1,
                            new Particle.DustOptions(Color.fromRGB(0, 0, 255),
                                    2));
                    world.spawnParticle(Particle.REDSTONE, point3, 1,
                            new Particle.DustOptions(Color.fromRGB(0, 0, 255),
                                    2));

                    // Determine if there are any enemies to target
                    //List<Player> enemies = (List<Player>) Bukkit.getOnlinePlayers();
                    List<Player> enemies = AbilityUtil.getEnemies(player);
                    if (enemies != null && enemies.size() > 0) {
                        // Find the closest enemy within range of turret
                        Player closestPlayer = null;
                        double closestDistance = MAX_RANGE * MAX_RANGE;

                        for (Player enemy : enemies) {
                            // Check y location
                            if (Math.abs(turretLocation.getBlockY() - enemy.getLocation().getY()) > 4) { continue; }

                            // Check whether enemy is within targeting triangle
                            if (!(isInTriangle(enemy, arrowSpawnLocation, point2, point3))) { continue; }

                            // Determine if enemy is the closest evaluated so far
                            double currentDistance = arrowSpawnLocation.distanceSquared(enemy.getLocation());
                            if (currentDistance < closestDistance /*&& hasLineOfSight(enemy, arrowSpawnLocation)*/) {
                                closestPlayer = enemy;
                                closestDistance = currentDistance;
                            }
                        }

                        if (closestPlayer != null) {
                            arrowSupply--;
                            Vector to = closestPlayer.getLocation().toVector();
                            Vector from = arrowSpawnLocation.toVector();
                            from.add(new Vector(0.5, 1.5, 0.5));
                            Vector direction = to.subtract(from).normalize();
                            Arrow arrow = world.spawnArrow(from.toLocation(world), direction, 7, 0);
                            arrow.setMetadata(MetadataKey.ON_HIT, new FixedMetadataValue(CoronaCraft.instance, "Turret"));
                            //((CraftArrow) arrow).getHandle().knockbackStrength = 1;
                        }
                    }
                }
            }
        }.runTaskTimer(CoronaCraft.instance, CoronaCraft.ABILITY_TICK_PER_SECOND * 10, 30);
    }

    private boolean isInTriangle(Player player, Location point1, Location point2, Location point3) {
        double triangle1 = sign(player.getLocation(), point1, point2);
        double triangle2 = sign(player.getLocation(), point2, point3);
        double triangle3 = sign(player.getLocation(), point3, point1);

        boolean hasNegative = (triangle1 < 0) || (triangle2 < 0) || (triangle3 < 0);
        boolean hasPositive = (triangle1 > 0) || (triangle2 > 0) || (triangle3 > 0);
        return !(hasNegative && hasPositive);
    }

    private double sign(Location point1, Location point2, Location point3) {
        return (point1.getX() - point3.getX()) * (point2.getZ() - point3.getZ()) - (point2.getX() - point3.getX()) * (point1.getZ()- point3.getZ());
    }

    private boolean hasLineOfSight(Player player, Location arrowSpawn) {
        Vector playerLocation = player.getLocation().toVector();
        Vector arrowSpawnLocation = arrowSpawn.toVector();
        World world = player.getWorld();
        Vector i = arrowSpawnLocation.subtract(playerLocation).normalize().multiply(-0.5);
        double maxDistanceSquared = arrowSpawnLocation.distanceSquared(playerLocation);
        Vector currentLocation = arrowSpawnLocation.clone();
        Bukkit.broadcastMessage("Player location: " + playerLocation.toString());
        Bukkit.broadcastMessage("Arrow spawn location: " + arrowSpawnLocation.toString());
        Bukkit.broadcastMessage("Increment vector: " + i.toString());
        Bukkit.broadcastMessage("maxDistanceSquared: " + maxDistanceSquared);
        Bukkit.broadcastMessage("actual distance: " + arrowSpawnLocation.distance(playerLocation));

        while (arrowSpawnLocation.distanceSquared(currentLocation) < maxDistanceSquared) {
            if (world.getBlockAt(currentLocation.toLocation(world)).getType() != Material.AIR) {
                Bukkit.broadcastMessage("No line of sight due to " + world.getBlockAt(currentLocation.toLocation(world)).getType().toString() + " at " + currentLocation.toString());
                return false;
            }

            currentLocation.add(i);
        }

        Bukkit.broadcastMessage("Has line of sight");
        return true;
    }

    @EventHandler
    public void onEntityDamageByEntityEvent(EntityDamageByEntityEvent event) {
        // Check that the damager is an arrow fired by a turret
        if (event.getDamager() instanceof Arrow && event.getEntity() instanceof Player) {
            Arrow arrow = (Arrow) event.getDamager();
            if (arrow.hasMetadata(MetadataKey.ON_HIT)) {
                if (arrow.getMetadata(MetadataKey.ON_HIT).get(0).value().equals("Turret")) {
                    event.setDamage(DAMAGE);
                }
            }
        }
    }
}
