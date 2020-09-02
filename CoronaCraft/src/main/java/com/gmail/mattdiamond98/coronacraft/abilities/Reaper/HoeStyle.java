package com.gmail.mattdiamond98.coronacraft.abilities.Reaper;

import com.gmail.mattdiamond98.coronacraft.abilities.Ability;
import com.gmail.mattdiamond98.coronacraft.util.AbilityUtil;
import org.bukkit.Bukkit;
import org.bukkit.Material;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.EventPriority;
import org.bukkit.event.block.Action;
import org.bukkit.event.entity.EntityDamageByEntityEvent;
import org.bukkit.event.entity.EntityDamageEvent;
import org.bukkit.event.entity.PlayerDeathEvent;
import org.bukkit.event.player.PlayerDropItemEvent;
import org.bukkit.event.player.PlayerInteractEvent;
import org.bukkit.inventory.ItemStack;

public class HoeStyle extends Ability {
    public static short hoeMaxDurability = Material.DIAMOND_HOE.getMaxDurability();
    public static short hoeMinDurability = 1;

    public HoeStyle() {
        super("Hoe Style", Material.DIAMOND_HOE);
    }

    @Override
    public void initialize() {
        styles.add(new GhastlyScythe());
    }

    @EventHandler(priority = EventPriority.HIGH)
    public void onPlayerDropItem(PlayerDropItemEvent e) {
        if ((e.getItemDrop().getItemStack().getType() == item)) {
            AbilityUtil.toggleAbilityStyle(e.getPlayer(), item);
            e.setCancelled(true);
        }
    }

    @EventHandler
    public void onPlayerInteract(PlayerInteractEvent e) {
        Player p = e.getPlayer();
        if (e.hasItem() && e.getItem().getType() == item && e.getItem().getDurability() == 0) {
            if (e.getAction().equals(Action.RIGHT_CLICK_AIR) || e.getAction().equals(
                    Action.RIGHT_CLICK_BLOCK)) {
                // Do stuff
                getStyle(p).execute(p, e.getItem());
            }
        }
    }

    @EventHandler
    public void regenerateDurability(EntityDamageByEntityEvent event) {
        if (event.getEntity() instanceof Player && event.getDamager() instanceof Player) {
            Player attacker = (Player) event.getDamager();
            ItemStack heldItem = attacker.getInventory().getItemInMainHand();
            if (heldItem != null && heldItem.getType() == item && heldItem.getDurability() >= 0) {
                Bukkit.broadcastMessage("regenerated third of durability");
                heldItem.setDurability((short) Math.max(0, heldItem.getDurability() - hoeMaxDurability / 3));
            }
        }
    }

    @EventHandler
    public void fullyRegenerateDurability(PlayerDeathEvent event) {
        if (event.getEntity().getKiller() != null && event.getEntity().getKiller() instanceof Player) {
            Player attacker = (Player) event.getEntity().getKiller();
            ItemStack heldItem = attacker.getInventory().getItemInMainHand();
            if (heldItem != null && heldItem.getType() == item && heldItem.getDurability() >= 0) {
                Bukkit.broadcastMessage("fullyRegeneratedDurability");
                heldItem.setDurability((short) Math.max(0, heldItem.getDurability() - hoeMaxDurability / 3));
            }
        }
    }
}
