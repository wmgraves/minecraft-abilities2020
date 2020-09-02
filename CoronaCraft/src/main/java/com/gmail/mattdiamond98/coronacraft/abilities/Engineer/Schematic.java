package com.gmail.mattdiamond98.coronacraft.abilities.Engineer;

import com.gmail.mattdiamond98.coronacraft.abilities.Ability;
import com.gmail.mattdiamond98.coronacraft.abilities.AbilityStyle;
import com.gmail.mattdiamond98.coronacraft.CoronaCraft;
import com.gmail.mattdiamond98.coronacraft.event.CoolDownEndEvent;
import com.gmail.mattdiamond98.coronacraft.event.CoolDownTickEvent;
import com.gmail.mattdiamond98.coronacraft.util.AbilityUtil;
import org.bukkit.ChatColor;
import org.bukkit.Material;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.EventPriority;
import org.bukkit.event.Listener;
import org.bukkit.event.block.Action;
import org.bukkit.event.player.PlayerDropItemEvent;
import org.bukkit.event.player.PlayerInteractEvent;

import static com.gmail.mattdiamond98.coronacraft.util.AbilityUtil.notInSpawn;
import static org.bukkit.Bukkit.getServer;

public class Schematic extends Ability {

    public static final Material ITEM = Material.IRON_INGOT;

    public Schematic() {
        super("Schematic", ITEM);
    }

    @Override
    public void initialize() {
        styles.add(new Tower());
        styles.add(new Wall());
        styles.add(new Bridge());
        styles.add(new Turret());
        styles.add(new Healer());

        for (AbilityStyle style : styles) {
            if (!(style instanceof Listener)) { continue; }
            getServer().getPluginManager().registerEvents((Listener) style, CoronaCraft.instance);
        }
    }

    @EventHandler
    public void onPlayerInteract(PlayerInteractEvent e) {
        Player p = e.getPlayer();
        if (e.hasItem() && e.getItem().getType() == item && notInSpawn(p)) {
            if (e.getAction().equals(Action.RIGHT_CLICK_AIR) || e.getAction().equals(Action.RIGHT_CLICK_BLOCK)) {
                SchematicStyle schematic = (SchematicStyle)getStyle(p);

                if (schematic instanceof Turret) {
                    if (((Turret) schematic).playerHasActiveTurret(p)) {
                        p.sendMessage(ChatColor.RED + "You cannot have more than one active turret.");
                        return;
                    }
                }

                int steps = schematic.execute(p, -1);
                if (steps == -4) p.sendMessage(ChatColor.RED + "You must stand still to place a schematic.");
                else if (steps == -3) p.sendMessage(ChatColor.RED + "Building is disabled on this map.");
                else if (steps == -2) return; // Materials message already handled
                else if (steps == -1) {
                    p.sendMessage(ChatColor.RED + "Invalid position. Place away from borders and game objects.");
                } else {
                    p.sendMessage(ChatColor.GREEN + "Constructing " + schematic.getName());
                    CoronaCraft.setCooldown(p, item, steps * CoronaCraft.ABILITY_TICK_PER_SECOND + 1);
                }
            }
        }
    }

    @EventHandler(priority = EventPriority.HIGH)
    public void onPlayerDropItem(PlayerDropItemEvent e) {
        if ((e.getItemDrop().getItemStack().getType() == item) /*&& notInSpawn(e.getPlayer())*/) {
            if (CoronaCraft.isOnCooldown(e.getPlayer(), item)) {
                e.getPlayer().sendMessage(ChatColor.RED + "Finish your current schematic first!");
            } else {
                AbilityUtil.toggleAbilityStyle(e.getPlayer(), item);
            }
            e.setCancelled(true);
        }
    }

    @EventHandler
    public void onCoolDownEnd(CoolDownEndEvent e) {
        if (e.getItem() != item) return;
        e.getPlayer().sendMessage(ChatColor.GREEN + "Completed " + e.getAbility().getStyle(e.getPlayer()).getName());
    }

    @EventHandler
    public void onCoolDownTick(CoolDownTickEvent e) {
        if (e.getItem() != item || AbilityUtil.getTotalCount(e.getPlayer(), item) == 0 || !AbilityUtil.notInSpawn(e.getPlayer())) {
            // TODO: Maybe remove from coolDown?
            return;
        }
        e.getAbility().getStyle(e.getPlayer()).execute(e.getPlayer(), e.getTicksRemaining());
    }
}
