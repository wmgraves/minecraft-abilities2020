package com.gmail.mattdiamond98.coronacraft.abilities.Engineer;

import com.gmail.mattdiamond98.coronacraft.abilities.AbilityStyle;
import com.gmail.mattdiamond98.coronacraft.CoronaCraft;
import com.gmail.mattdiamond98.coronacraft.util.AbilityUtil;
import com.sk89q.worldedit.EditSession;
import com.sk89q.worldedit.WorldEdit;
import com.sk89q.worldedit.WorldEditException;
import com.sk89q.worldedit.bukkit.BukkitAdapter;
import com.sk89q.worldedit.extent.clipboard.Clipboard;
import com.sk89q.worldedit.extent.clipboard.io.ClipboardFormat;
import com.sk89q.worldedit.extent.clipboard.io.ClipboardFormats;
import com.sk89q.worldedit.extent.clipboard.io.ClipboardReader;
import com.sk89q.worldedit.function.operation.Operation;
import com.sk89q.worldedit.function.operation.Operations;
import com.sk89q.worldedit.math.BlockVector3;
import com.sk89q.worldedit.math.transform.AffineTransform;
import com.sk89q.worldedit.regions.Region;
import com.sk89q.worldedit.regions.RegionOperationException;
import com.sk89q.worldedit.session.ClipboardHolder;
import com.sk89q.worldedit.world.block.BlockType;
import com.tommytony.war.Team;
import com.tommytony.war.Warzone;
import com.tommytony.war.config.WarzoneConfig;
import com.tommytony.war.volume.Volume;
import com.tommytony.war.volume.ZoneVolume;
import org.bukkit.*;
import org.bukkit.entity.Player;
import org.bukkit.event.player.PlayerInteractEvent;
import org.bukkit.util.Vector;

import javax.annotation.Nullable;
import java.io.File;
import java.io.FileInputStream;
import java.io.IOException;
import java.util.*;
import java.util.stream.Collectors;

import static com.sk89q.worldedit.bukkit.BukkitAdapter.*;

public abstract class SchematicStyle extends AbilityStyle {

    public int STEPS = 5; // Eventually can be configurable

    private Map<UUID, Location> REFERENCE_POINTS = new HashMap<>();

    private List<Clipboard> schematics = new ArrayList<>(STEPS);

    private Map<Material, Integer> materials;

    public SchematicStyle(String name, String[] description, @Nullable String permission, Integer ModelData, Map<Material, Integer> materials) {
        super(name, description, permission, ModelData);
        this.materials = materials;

        if (schematics.size() == 0) {
            for (int i = 1; i <= STEPS; i++) {
                String file = Bukkit.getServer().getPluginManager().getPlugin("WorldEdit").getDataFolder()
                        .getAbsolutePath() + "/schematics/" + getName().toLowerCase() + "-" + i + ".schem";
                ClipboardFormat format = ClipboardFormats.findByFile(new File(file));
                try (ClipboardReader reader = format.getReader(new FileInputStream(file))) {
                    schematics.add(reader.read());
                } catch (IOException e) {
                    e.printStackTrace();
                }
            }
        }
    }

    public int execute(Player p, Object... args) {
        int state = (Integer) args[0];
        if (state == -1) {
            if (p.getVelocity().lengthSquared() > 0.01) {
                System.out.println("Debug: RateablePlayer Velocity Squared: " + p.getVelocity().lengthSquared());
                return -4;
            }
            Warzone zone = Warzone.getZoneByPlayerName(p.getName());
            List<Volume> zoneVolumes = new ArrayList<>();
            zone.getTeams().stream().map(Team::getFlagVolume).filter(Objects::nonNull).collect(Collectors.toCollection(() -> zoneVolumes));
            zone.getTeams().stream().map(Team::getSpawnVolumes)
                    .map(Map::values)
                    .flatMap(Collection::stream)
                    .peek(volume -> {
                        if (volume.getCornerTwo().getY() > volume.getCornerOne().getY()) {
                            volume.setCornerTwo(volume.getCornerTwo().add(new Vector(0, 5, 0)));
                        } else {
                            volume.setCornerOne(volume.getCornerOne().add(new Vector(0, 5, 0)));
                        }
                    })
                    .collect(Collectors.toCollection(() -> zoneVolumes));
            ZoneVolume fullVolume = zone.getVolume(); // get inside vol
            Clipboard schematic = schematics.get(0);
            Region schematicVolume = schematic.getRegion().clone();
            try {
                schematicVolume.shift(asBlockVector(p.getLocation()).subtract(schematic.getOrigin()));
            } catch (RegionOperationException e) {
                e.printStackTrace();
            }
            if (zone.getWarzoneConfig().contains(WarzoneConfig.UNBREAKABLE) && zone.getWarzoneConfig().getBoolean(WarzoneConfig.UNBREAKABLE)) {
                return -3;
            }
            Location minSchematicLoc = adapt(p.getWorld(), schematicVolume.getMinimumPoint());
            Location maxSchematicLoc = adapt(p.getWorld(), schematicVolume.getMaximumPoint());
            if (!fullVolume.contains(minSchematicLoc)
                    || !fullVolume.contains(maxSchematicLoc)
                    || zoneVolumes.stream().anyMatch(v ->
                    v.contains(minSchematicLoc)
                            || v.contains(maxSchematicLoc)
                            || schematicVolume.contains(asBlockVector(v.getCornerOne()))
                            || schematicVolume.contains(asBlockVector(v.getCornerTwo()))
            )) {
                return -1;
            }
            Map<Material, Integer> newAmounts = new TreeMap<>();
            for (Map.Entry<Material, Integer> materialCost : materials.entrySet()) {
                Material material = materialCost.getKey();
                int cost = materialCost.getValue();
                int count = AbilityUtil.getTotalCount(p, material);
                if (count < cost) {
                    AbilityUtil.notifyAbilityRequiresResources(p, materials);
                    return -2;
                }
                newAmounts.put(material, count - cost);
            }
            for (Map.Entry<Material, Integer> newAmount : newAmounts.entrySet()) {
                AbilityUtil.setStackCount(p, newAmount.getKey(), newAmount.getValue());
            }
            REFERENCE_POINTS.put(p.getUniqueId(), p.getLocation());
            return STEPS;
        } else if (state % CoronaCraft.ABILITY_TICK_PER_SECOND != 0){
            return 0;
        }
        state /= CoronaCraft.ABILITY_TICK_PER_SECOND;
        Clipboard schematic = schematics.get(state - 1);
        try (EditSession editSession = WorldEdit.getInstance().getEditSessionFactory()
                .getEditSession(adapt(p.getWorld()), -1)) {
            ClipboardHolder holder = new ClipboardHolder(schematic);
            AffineTransform transform = new AffineTransform().rotateY(rotation(REFERENCE_POINTS.get(p.getUniqueId())));
            holder.setTransform(holder.getTransform().combine(transform));
            Operation operation = holder
                    .createPaste(editSession)
                    .to(asBlockVector(REFERENCE_POINTS.get(p.getUniqueId())))
                    .ignoreAirBlocks(true)
                    .build();
            Operations.complete(operation);

            // Handle special schematic functions
            if (state == 1) {
                if (this instanceof Turret) {
                    // Activate turret
                    Location turretLocation = REFERENCE_POINTS.get(p.getUniqueId());
                    ((Turret) this).activateTurret(p, turretLocation, rotation(REFERENCE_POINTS.get(p.getUniqueId())));
                }
                else if (this instanceof Healer) {
                    // Activate healer
                    Location healerLocation = REFERENCE_POINTS.get(p.getUniqueId());
                    ((Healer) this).activateHealer(p, healerLocation, rotation(REFERENCE_POINTS.get(p.getUniqueId())));
                }
            }
        } catch (WorldEditException e) {
            e.printStackTrace();
        }
        p.getWorld().playSound(p.getLocation(), Sound.BLOCK_ANVIL_USE, 1, (float) Math.random());
        return 0;
    }

    private double rotation(Location location) {
        int angle = (Math.round(location.getYaw()) + 270) % 360;
        if (angle <= 45)  return 90.0; //90
        if (angle <= 135) return 0.0; //0
        if (angle <= 225) return 270.0; //270
        if (angle <= 315) return 180.0; //180
        return 90.0;
    }
}
