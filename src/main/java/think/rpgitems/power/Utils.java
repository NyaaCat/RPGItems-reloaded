package think.rpgitems.power;

import org.bukkit.Location;
import org.bukkit.configuration.ConfigurationSection;
import org.bukkit.entity.Entity;
import org.bukkit.entity.LivingEntity;
import org.bukkit.entity.Player;
import org.bukkit.permissions.PermissionAttachment;
import think.rpgitems.I18n;
import think.rpgitems.RPGItems;
import think.rpgitems.item.RPGItem;
import think.rpgitems.power.impl.PowerSelector;

import java.lang.reflect.Field;
import java.util.*;
import java.util.stream.Collectors;

public class Utils {

    public static Map<String, Long> cooldowns = new HashMap<>();

    public static List<Entity> getNearbyEntities(Power power, Location l, Player player, double radius, double dx, double dy, double dz) {
        List<Entity> entities = new ArrayList<>();
        for (Entity e : l.getWorld().getNearbyEntities(l, dx, dy, dz)) {
            try {
                if (l.distance(e.getLocation()) <= radius) {
                    entities.add(e);
                }
            } catch (RuntimeException ex) {
                ex.printStackTrace();
            }
        }
        power.getItem().powers.stream().filter(pow -> pow instanceof PowerSelector).forEach(
                selector -> {
                    if (power.getSelectors().contains(((PowerSelector) selector).id)) {
                        ((PowerSelector) selector).inPlaceFilter(player, entities);
                    }
                }
        );
        return entities;
    }

    /**
     * Get nearby entities.
     *
     * @param power  power
     * @param l      location
     * @param player player
     * @param radius radius
     * @return nearby entities
     */
    public static List<Entity> getNearbyEntities(Power power, Location l, Player player, double radius) {
        return getNearbyEntities(power, l, player, radius, radius, radius, radius);
    }

    /**
     * Get nearby living entities ordered by distance.
     *
     * @param power  power
     * @param l      location
     * @param player player
     * @param radius radius
     * @param min    min radius
     * @return nearby living entities ordered by distance
     */
    public static List<LivingEntity> getNearestLivingEntities(Power power, Location l, Player player, double radius, double min) {
        final java.util.List<java.util.Map.Entry<LivingEntity, Double>> entities = new java.util.ArrayList<>();
        for (Entity e : getNearbyEntities(power, l, player, radius)) {
            try {
                if (e instanceof LivingEntity && !player.equals(e)) {
                    double d = l.distance(e.getLocation());
                    if (d <= radius && d >= min) {
                        entities.add(new AbstractMap.SimpleImmutableEntry<>((LivingEntity) e, d));
                    }
                }
            } catch (RuntimeException ex) {
                ex.printStackTrace();
            }
        }
        java.util.List<LivingEntity> entity = new java.util.ArrayList<>();
        entities.sort(Comparator.comparing(java.util.Map.Entry::getValue));
        entities.forEach((k) -> entity.add(k.getKey()));
        return entity;
    }

    /**
     * Gets entities in cone.
     *
     * @param entities  List of nearby entities
     * @param startPos  starting position
     * @param degrees   angle of cone
     * @param direction direction of the cone
     * @return All entities inside the cone
     */
    public static List<LivingEntity> getLivingEntitiesInCone(List<LivingEntity> entities, org.bukkit.util.Vector startPos, double degrees, org.bukkit.util.Vector direction) {
        List<LivingEntity> newEntities = new ArrayList<>();
        for (LivingEntity e : entities) {
            org.bukkit.util.Vector relativePosition = e.getEyeLocation().toVector();
            relativePosition.subtract(startPos);
            if (getAngleBetweenVectors(direction, relativePosition) > degrees) continue;
            newEntities.add(e);
        }
        return newEntities;
    }

    /**
     * Gets angle between vectors.
     *
     * @param v1 the v 1
     * @param v2 the v 2
     * @return the angle between vectors
     */
    public static float getAngleBetweenVectors(org.bukkit.util.Vector v1, org.bukkit.util.Vector v2) {
        return Math.abs((float) Math.toDegrees(v1.angle(v2)));
    }

    /**
     * Check cooldown boolean.
     *
     * @param power    Power
     * @param p        the p
     * @param cdTicks  the cd ticks
     * @param showWarn whether to show warning to player
     * @return the boolean
     */
    public static boolean checkCooldown(Power power, Player p, long cdTicks, boolean showWarn) {
        long cooldown;

        String key = p.getName() + "." + power.getItem().getUID() + "." + power.getNamespacedKey().toString() + ".cooldown";
        Long value = cooldowns.get(key);
        long nowTick = System.currentTimeMillis() / 50;
        if (value == null) {
            cooldown = nowTick;
            cooldowns.put(key, cooldown);
        } else {
            cooldown = value;
        }
        return checkAndSetCooldown(p, cdTicks, showWarn, cooldown, key, nowTick);
    }

    public static boolean checkCooldownByString(Player player, RPGItem item, String command, long cooldownTime, boolean showWarn) {
        long cooldown;
        String key = player.getName() + "." + item.getUID() + "." + "command." + command + ".cooldown";
        Long value = cooldowns.get(key);
        long nowTick = System.currentTimeMillis() / 50;
        if (value == null) {
            cooldown = nowTick;
            cooldowns.put(key, cooldown);
        } else {
            cooldown = value;
        }
        return checkAndSetCooldown(player, cooldownTime, showWarn, cooldown, key, nowTick);
    }

    public static boolean checkAndSetCooldown(Player player, long cooldownTime, boolean showWarn, long cooldown, String key, long nowTick) {
        if (cooldown <= nowTick) {
            cooldowns.put(key, nowTick + cooldownTime);
            return true;
        } else {
            if (showWarn)
                player.sendMessage(I18n.format("message.cooldown", ((double) (cooldown - nowTick)) / 20d));
            return false;
        }
    }

    public static void attachPermission(Player player, String permission) {
        if (permission.length() != 0 && !permission.equals("*")) {
            PermissionAttachment attachment = player.addAttachment(RPGItems.plugin, 1);
            String[] perms = permission.split("\\.");
            StringBuilder p = new StringBuilder();
            for (String perm : perms) {
                p.append(perm);
                attachment.setPermission(p.toString(), true);
                p.append('.');
            }
        }
    }

    @SuppressWarnings("unchecked")
    public static void saveProperty(Power p, ConfigurationSection section, String property, Field field) throws IllegalAccessException {
        Serializer getter = field.getAnnotation(Serializer.class);
        Object val = field.get(p);
        if (val == null) return;
        if (getter != null) {
            section.set(property, Getter.from(p, getter.value()).get(val));
        } else {
            if (Collection.class.isAssignableFrom(field.getType())) {
                Collection c = (Collection) val;
                if (c.isEmpty()) return;
                section.set(property, c.stream().map(Object::toString).collect(Collectors.joining(",")));
            } else {
                section.set(property, val);
            }
        }
    }
}
