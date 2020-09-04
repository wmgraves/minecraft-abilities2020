package com.gmail.mattdiamond98.coronacraft.abilities.Reaper;

import com.gmail.mattdiamond98.coronacraft.CoronaCraft;
import com.gmail.mattdiamond98.coronacraft.abilities.Ability;
import com.gmail.mattdiamond98.coronacraft.util.AbilityUtil;
import org.bukkit.Bukkit;
import org.bukkit.Material;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.EventPriority;
import org.bukkit.event.block.Action;
import org.bukkit.event.player.PlayerDropItemEvent;
import org.bukkit.event.player.PlayerInteractEvent;

public class Rose extends Ability {

    public Rose() {
        super("Wither Rose", Material.WITHER_ROSE);
    }

    @Override
    public void initialize() {
        styles.add(new GraveOmen());
    }

    /**
     * Handles switching rose abilities.  Empties scythe fuel whenever an ability is switched.
     * @param e The PlayerDropItemEvent event
     */
    @EventHandler(priority = EventPriority.HIGH)
    public void onPlayerDropItem(PlayerDropItemEvent e) {
        // Make sure the correct item was dropped
        if ((e.getItemDrop().getItemStack().getType() == item)) {
            AbilityUtil.toggleAbilityStyle(e.getPlayer(), item);
            e.setCancelled(true);
        }
    }

    /**
     * Handles triggering rose abilities.
     * @param event The PlayerInteractEvent event
     */
    @EventHandler
    public void onPlayerInteract(PlayerInteractEvent event) {
        Bukkit.broadcastMessage("1");
        Player player = event.getPlayer();
        // Check that it was a right click
        if (event.getAction().equals(Action.RIGHT_CLICK_AIR) || event.getAction().equals(
                Action.RIGHT_CLICK_BLOCK)) {
            // Check that player has the correct item
            if (event.hasItem() && event.getItem().getType() == item) {
                Bukkit.broadcastMessage("2");
                // Check that the player is not currently on cooldown
                if (!CoronaCraft.isOnCooldown(player, item)) {
                    Bukkit.broadcastMessage("3");
                    RoseStyle style = (RoseStyle) getStyle(player);
                    CoronaCraft.setCooldown(player, item, style.cooldownSeconds *
                            CoronaCraft.ABILITY_TICK_PER_SECOND);
                    style.execute(player, event);
                }
                event.setCancelled(true);
            }
        }
    }
}
