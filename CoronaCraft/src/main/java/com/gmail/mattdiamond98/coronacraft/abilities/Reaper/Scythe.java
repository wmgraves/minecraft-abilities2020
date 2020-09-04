package com.gmail.mattdiamond98.coronacraft.abilities.Reaper;

import com.gmail.mattdiamond98.coronacraft.abilities.Ability;
import com.gmail.mattdiamond98.coronacraft.util.AbilityUtil;
import com.tommytony.war.event.WarPlayerLeaveSpawnEvent;
import org.bukkit.Bukkit;
import org.bukkit.Material;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.EventPriority;
import org.bukkit.event.block.Action;
import org.bukkit.event.entity.EntityDamageByEntityEvent;
import org.bukkit.event.entity.PlayerDeathEvent;
import org.bukkit.event.player.PlayerDropItemEvent;
import org.bukkit.event.player.PlayerInteractEvent;
import org.bukkit.inventory.ItemStack;
import org.bukkit.potion.PotionEffect;
import org.bukkit.potion.PotionEffectType;

public class Scythe extends Ability {
    public static short hoeMaxDurability = Material.DIAMOND_HOE.getMaxDurability();
    public static short hoeMinDurability = 1;

    public Scythe() {
        super("Scythe", Material.DIAMOND_HOE);
    }

    @Override
    public void initialize() {
        styles.add(new GhastlyScythe());
    }

    /**
     * Handles switching scythe abilities.  Empties scythe fuel whenever an ability is switched.
     * @param e The PlayerDropItemEvent event
     */
    @EventHandler(priority = EventPriority.HIGH)
    public void onPlayerDropItem(PlayerDropItemEvent e) {
        // Make sure the correct item was dropped
        if ((e.getItemDrop().getItemStack().getType() == item)) {
            AbilityUtil.toggleAbilityStyle(e.getPlayer(), item);
            e.getItemDrop().getItemStack().setDurability((short) (hoeMaxDurability - hoeMinDurability));
            e.setCancelled(true);
        }
    }

    /**
     * Gives all reapers "permanent" slowness 1 when they leave spawn.
     * @param event
     */
    @EventHandler
    public void onPlayerLeaveSpawn(WarPlayerLeaveSpawnEvent event) {
        if (AbilityUtil.inventoryContains(event.getPlayer(), item)) {
            event.getPlayer().addPotionEffect(new PotionEffect(PotionEffectType.SLOW,
                    10000000, 1, true, false));
        }
    }

    /**
     * Handles triggering scythe abilities.
     * @param event The PlayerInteractEvent event
     */
    @EventHandler
    public void onPlayerInteract(PlayerInteractEvent event) {
        Player player = event.getPlayer();
        // check that the item was a right click
        if (event.getAction().equals(Action.RIGHT_CLICK_AIR) || event.getAction().equals(
                Action.RIGHT_CLICK_BLOCK)) {
            // Check that player has the correct item
            if (event.hasItem() && event.getItem().getType() == item) {
                // Check that the player is not currently on cooldown
                if (!ReaperCooldownTracker.isOnCooldown(player)) {
                    // Check that there is enough fuel left
                    HoeStyle style = (HoeStyle) getStyle(player);
                    if (consumeDurability(player, style)) {
                        ReaperCooldownTracker.setCooldown(player, style);
                        style.execute(player, event);
                    }
                }
                event.setCancelled(true);
            }
        }

    }

    /**
     * Regenerates a third of the player's fuel when they hit an enemy.
     * @param event The EntityDamageByEntityEvent event
     */
    @EventHandler
    public void regenerateDurability(EntityDamageByEntityEvent event) {
        // Check that a player hit someone while holding the correct item
        if (event.getEntity() instanceof Player && event.getDamager() instanceof Player) {
            Player attacker = (Player) event.getDamager();
            ItemStack heldItem = attacker.getInventory().getItemInMainHand();
            if (heldItem != null && heldItem.getType() == item && heldItem.getDurability() > 0) {
                // Give back a third of the original durability
                Bukkit.broadcastMessage("regenerated third of durability");
                heldItem.setDurability((short) Math.max(0, heldItem.getDurability() - hoeMaxDurability / 3));
            }
        }
    }

    /**
     * Regenerates all of the player's fuel when they kill an enemy.
     * @param event The EntityDamageByEntity event
     */
    @EventHandler
    public void fullyRegenerateDurability(PlayerDeathEvent event) {
        // Check that a player killed someone while holding the correct item
        if (event.getEntity().getKiller() != null && event.getEntity().getKiller() instanceof Player) {
            Player attacker = (Player) event.getEntity().getKiller();
            ItemStack heldItem = attacker.getInventory().getItemInMainHand();
            if (heldItem != null && heldItem.getType() == item && heldItem.getDurability() > 0) {
                // Reset the durability of the hoe
                Bukkit.broadcastMessage("fullyRegeneratedDurability");
                heldItem.setDurability((short) 0);
            }
        }
    }

    /**
     * Verifies that enough fuel is available and, if so, uses it.
     * @param player The player who is attempting to trigger a scythe ability
     * @param style The scythe ability
     * @return True if enough fuel was available and was consumed successfully, false otherwise
     */
    public boolean consumeDurability(Player player, HoeStyle style) {
        // Check that the item held by the player is a valid item and that the player is not in an invalid
        // condition
        ItemStack heldItem = player.getInventory().getItemInMainHand();
        if (heldItem == null || heldItem.getType() != item) { return false; }
        if (AbilityUtil.inSpawn(player) || !AbilityUtil.inWarzone(player)) { return false; }

        // Check that there is durability left to remove from the item
        if (heldItem.getDurability() >= hoeMaxDurability - hoeMinDurability) { return false; }

        // Remove the durability specified
        heldItem.setDurability((short) Math.min(Scythe.hoeMaxDurability - Scythe.hoeMinDurability,
                heldItem.getDurability() + style.fuelCost));
        return true; // Lets caller know that an ability can be executed
    }
}
