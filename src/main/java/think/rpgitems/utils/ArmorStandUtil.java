package think.rpgitems.utils;

import org.bukkit.Location;
import org.bukkit.entity.ArmorStand;
import org.bukkit.entity.Entity;
import org.bukkit.metadata.FixedMetadataValue;
import org.bukkit.scheduler.BukkitRunnable;
import think.rpgitems.RPGItems;

import java.util.LinkedHashMap;
import java.util.Map;

public class ArmorStandUtil {
    private static Map<Entity, ArmorStand> projectileSources = new LinkedHashMap<>();
    private static final String META_KEY = "rpgitem-armor-stand";

    public static ArmorStand asProjectileSource(Entity player) {
        return projectileSources.computeIfAbsent(player, player1 -> {
            Location clone = player1.getLocation().clone();
            clone.setY(255);
            return summonAndRemoveLater(player, clone, 1200);
        });
    }

    private static ArmorStand summonAndRemoveLater(Entity entity, Location location, int delay) {
        ArmorStand summon = summon(location);
        removeLater(entity, summon, delay);
        return summon;
    }

    private static ArmorStand summon(Location location) {
        ArmorStand armorStand;
        armorStand = location.getWorld().spawn(location, ArmorStand.class, (e) -> {
            e.setVisible(false);
            e.setPersistent(false);
            e.setCanPickupItems(false);
            e.setGlowing(false);
            e.setBasePlate(false);
            e.setArms(false);
            e.setMarker(true);
            e.setInvulnerable(true);
            e.setGravity(false);
            e.setCollidable(false);
            e.setMetadata(META_KEY, new FixedMetadataValue(RPGItems.plugin, true));
        });
        return armorStand;
    }

    private static void removeLater(Entity entity, ArmorStand armorStand, int delay) {
        new BukkitRunnable() {
            @Override
            public void run() {
                projectileSources.remove(entity);
                armorStand.remove();
            }
        }.runTaskLater(RPGItems.plugin, delay);
    }
}
