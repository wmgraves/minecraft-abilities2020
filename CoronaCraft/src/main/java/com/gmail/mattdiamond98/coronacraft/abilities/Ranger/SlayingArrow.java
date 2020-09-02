package com.gmail.mattdiamond98.coronacraft.abilities.Ranger;

import com.gmail.mattdiamond98.coronacraft.CoronaCraft;
import com.gmail.mattdiamond98.coronacraft.Loadout;
import com.gmail.mattdiamond98.coronacraft.abilities.*;
import com.gmail.mattdiamond98.coronacraft.event.CoolDownEndEvent;
import com.gmail.mattdiamond98.coronacraft.event.CoolDownTickEvent;
import com.gmail.mattdiamond98.coronacraft.util.AbilityUtil;
import com.tommytony.war.event.WarPlayerDeathEvent;
import org.bukkit.ChatColor;
import org.bukkit.Material;
import org.bukkit.Particle;
import org.bukkit.entity.Arrow;
import org.bukkit.entity.Player;
import org.bukkit.entity.Projectile;
import org.bukkit.event.EventHandler;
import org.bukkit.event.EventPriority;
import org.bukkit.event.player.PlayerItemHeldEvent;
import org.bukkit.inventory.ItemStack;
import org.bukkit.inventory.meta.ItemMeta;
import org.bukkit.potion.PotionEffect;
import org.bukkit.potion.PotionEffectType;
import java.util.Arrays;
import java.util.stream.Collectors;

public class SlayingArrow extends UltimateAbility {

    public static final int DURATION_SECONDS = 25;
    public static final int DURATION_COOLDOWN_TICKS = DURATION_SECONDS * CoronaCraft.ABILITY_TICK_PER_SECOND;
    public static final int DURATION_MINECRAFT_TICKS = DURATION_SECONDS * 20;

    public SlayingArrow() {
        super("Slaying Arrow");
    }

    public static ProjectileAbilityStyle style = new ProjectileAbilityStyle("Slaying Arrow", new String[]{
            "When crouching, fire an arrow that",
            "kills anyone it hits for 10 arrows."
    }, 0) {
        @Override
        public int onShoot(Projectile projectile) {
            if (Longbow.makeSpecialArrow(projectile, this, 10)) {
                ((Arrow) projectile).addCustomEffect(new PotionEffect(PotionEffectType.HARM, 9, 5), false);
            }
            return 0;
        }

        @Override
        public int execute(Player player, Object... args) {
            return 0;
        }
    };

    @EventHandler(priority = EventPriority.HIGH)
    public void onPlayerItemHeld(PlayerItemHeldEvent e) {
        if (UltimateTracker.isUltimateActive(e.getPlayer()) && UltimateTracker.getLoadout(e.getPlayer()) == Loadout.GLADIATOR) {
            ItemStack item = e.getPlayer().getInventory().getItem(e.getNewSlot());
            if (item == null) return;
            if (item.getType() == Material.BOW) {
                formatBow(item);
            }
        }
    }

    public static ItemStack formatBow(ItemStack item) {
        if (item.hasItemMeta()
                && item.getItemMeta().hasDisplayName()
                && !item.getItemMeta().getDisplayName().equals(AbilityUtil.formatStyleName(style))) {
            ItemMeta meta = item.getItemMeta();
            meta.setDisplayName(AbilityUtil.formatStyleName(style));
            meta.setLore(Arrays.stream(style.getDescription()).map(AbilityUtil::formatLoreLine).collect(Collectors.toList()));
            item.setItemMeta(meta);
        }
        return item;
    }

    @Override
    public void activate(Player player) {
        UltimateListener.sendUltimateMessage(player);

        CoronaCraft.setCooldown(player, Material.NETHER_STAR, DURATION_COOLDOWN_TICKS);
    }

    @EventHandler
    public void onTick(CoolDownTickEvent e) {
        if (e.getPlayer() == null || !e.getPlayer().isOnline()) return;
        if (e.getItem() == Material.NETHER_STAR && UltimateTracker.getLoadout(e.getPlayer()) == Loadout.RANGER) {
            e.getPlayer().getWorld().spawnParticle(Particle.CAMPFIRE_COSY_SMOKE, e.getPlayer().getLocation(), 1);
            float remaining = (e.getTicksRemaining() * 1.0F) / DURATION_COOLDOWN_TICKS;
            if (remaining < 0.0) remaining = 0.0F;
            if (remaining > 1.0) remaining = 1.0F;
            e.getPlayer().setExp(remaining);
        }
    }

    @EventHandler
    public void onPlayerDeath(WarPlayerDeathEvent e) {
        if (UltimateTracker.isUltimateActive(e.getVictim()) && UltimateTracker.getLoadout(e.getVictim()) == Loadout.RANGER) {
            UltimateTracker.removeProgress(e.getVictim());
        }
    }

    @EventHandler
    public void onEnd(CoolDownEndEvent e) {
        if (e.getItem() == Material.NETHER_STAR && UltimateTracker.getLoadout(e.getPlayer()) == Loadout.RANGER) {
            UltimateTracker.removeProgress(e.getPlayer());
            e.getPlayer().sendMessage(ChatColor.YELLOW + "Your ultimate has ended.");
            for (ItemStack item : e.getPlayer().getInventory()) {
                if (item != null && item.getType() == Material.TRIDENT) {
                    AbilityUtil.formatItem(e.getPlayer(), item);
                }
            }
        }
    }
}