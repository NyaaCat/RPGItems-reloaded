package think.rpgitems.utils;

import org.bukkit.Location;
import org.bukkit.NamespacedKey;
import org.bukkit.entity.ArmorStand;
import org.bukkit.entity.Entity;
import org.bukkit.persistence.PersistentDataType;
import org.bukkit.scheduler.BukkitRunnable;
import think.rpgitems.RPGItems;

import java.util.LinkedHashMap;
import java.util.Map;

public class ArmorStandUtil {
    private static final NamespacedKey META_KEY = new NamespacedKey(RPGItems.plugin, "armorStand");
    private static final Map<Entity, ArmorStand> projectileSources = new LinkedHashMap<>();

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

    @SuppressWarnings("deprecation")
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
            e.getPersistentDataContainer().set(META_KEY, PersistentDataType.BOOLEAN, true);
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
