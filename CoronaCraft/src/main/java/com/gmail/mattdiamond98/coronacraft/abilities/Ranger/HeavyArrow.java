package com.gmail.mattdiamond98.coronacraft.abilities.Ranger;

import com.gmail.mattdiamond98.coronacraft.CoronaCraft;
import com.gmail.mattdiamond98.coronacraft.abilities.ProjectileAbilityStyle;
import com.tommytony.war.Warzone;
import org.bukkit.*;
import org.bukkit.craftbukkit.v1_15_R1.entity.CraftArrow;
import org.bukkit.entity.Player;
import org.bukkit.entity.Projectile;
import org.bukkit.event.entity.ProjectileHitEvent;
import org.bukkit.scheduler.BukkitRunnable;

public class HeavyArrow extends ProjectileAbilityStyle {

    public static int ARROW_COST = 6;

    public HeavyArrow() {
        super("Heavy Arrow", new String[] {
                "When crouching, fire an arrow that",
                "hits an enemy with Punch II.",
                "Cost: " + ARROW_COST + " Arrows"
        }, 0);
    }

    /***
     * @param p the RateablePlayer
     * @param args arg0 instanceof Arrow the projectile
     * @return cooldown (currently 0)
     */
    public int execute(Player p, Object... args) {
        ProjectileHitEvent event = (ProjectileHitEvent) args[0];
        Projectile arrow = event.getEntity();
        Location loc = arrow.getLocation();

        // Safety checks
        if (arrow.isDead()) arrow.remove();

        // Handle arrow effects
        ((CraftArrow) arrow).getHandle().knockbackStrength = 2;

        return 0;
    }

    @Override
    public int onShoot(Projectile projectile) {
        if (Longbow.makeSpecialArrow(projectile, this, ARROW_COST)) {
            // Create runnable
            BukkitRunnable run = new BukkitRunnable() {
                @Override
                public void run() {
                    if (projectile.isDead() || projectile.isOnGround() || projectile.getTicksLived() > 1000) {
                        this.cancel();
                        return;
                    }
                    Location loc = projectile.getLocation();
                    Warzone zone = Warzone.getZoneByLocation(loc);
                    if (zone == null || zone.isReinitializing()) {
                        this.cancel();
                        projectile.remove();
                        return;
                    }
                    loc.getWorld().spawnParticle(Particle.REDSTONE, loc, 1,
                            new Particle.DustOptions(Color.fromRGB(128, 0,
                                    128), 1));
                }
            };
            run.runTaskTimer(CoronaCraft.instance, 2, 2);
        }
        return 0;
    }
}