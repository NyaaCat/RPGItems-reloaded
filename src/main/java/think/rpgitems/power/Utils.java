package think.rpgitems.power;

import org.bukkit.ChatColor;
import org.bukkit.Location;
import org.bukkit.configuration.ConfigurationSection;
import org.bukkit.entity.Entity;
import org.bukkit.entity.LivingEntity;
import org.bukkit.entity.Player;
import org.bukkit.permissions.PermissionAttachment;
import org.bukkit.util.BoundingBox;
import org.bukkit.util.Vector;
import think.rpgitems.I18n;
import think.rpgitems.RPGItems;
import think.rpgitems.data.Font;
import think.rpgitems.item.RPGItem;
import think.rpgitems.power.impl.PowerSelector;
import think.rpgitems.utils.Pair;

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
                val = field.getType().isEnum() ? ((Enum<?>)val).name() : val;
                section.set(property, val);
            }
        }
    }

    public static Vector distance(BoundingBox bb, Vector vec) {
        double x = Math.max(0, Math.max(bb.getMinX() - vec.getX(), vec.getX() - bb.getMaxX()));
        double y = Math.max(0, Math.max(bb.getMinY() - vec.getY(), vec.getY() - bb.getMaxY()));
        double z = Math.max(0, Math.max(bb.getMinZ() - vec.getZ(), vec.getZ() - bb.getMaxZ()));
        return new Vector(x, y, z);
    }

    public static Vector hitPoint(BoundingBox bb, Vector hitNormal) {
        if (hitNormal.getX() > 0) {
            return new Vector(bb.getMinX(), bb.getCenterY(), bb.getCenterZ());
        }
        if (hitNormal.getX() < 0) {
            return new Vector(bb.getMaxX(), bb.getCenterY(), bb.getCenterZ());
        }
        if (hitNormal.getY() > 0) {
            return new Vector(bb.getCenterX(), bb.getMinY(), bb.getCenterZ());
        }
        if (hitNormal.getY() < 0) {
            return new Vector(bb.getCenterX(), bb.getMaxY(), bb.getCenterZ());
        }
        if (hitNormal.getZ() > 0) {
            return new Vector(bb.getCenterX(), bb.getCenterY(), bb.getMinZ());
        }
        if (hitNormal.getZ() < 0) {
            return new Vector(bb.getCenterX(), bb.getCenterY(), bb.getMaxZ());
        }
        throw new IllegalArgumentException("hitNormal: " + hitNormal.toString());
    }

    // Sweep a in the direction of v against b, returns non null & info if there was a hit
    // ===================================================================
    public static Pair<Vector, Vector> sweep(BoundingBox a, BoundingBox b, Vector vel) {
        double outTime = 1.0;
        // Return early if a & b are already overlapping
        if (a.overlaps(b)) return Pair.of(new Vector(), null);

        // Treat b as stationary, so invert v to get relative velocity
        Vector v = vel.clone().multiply(-1);

        double hitTime = 0.0;
        Vector overlapTime = new Vector();

        // X axis overlap
        if (v.getX() < 0) {
            if (b.getMax().getX() < a.getMin().getX()) return null;
            if (b.getMax().getX() > a.getMin().getX())
                outTime = Math.min((a.getMin().getX() - b.getMax().getX()) / v.getX(), outTime);

            if (a.getMax().getX() < b.getMin().getX()) {
                overlapTime.setX((a.getMax().getX() - b.getMin().getX()) / v.getX());
                hitTime = Math.max(overlapTime.getX(), hitTime);
            }
        } else if (v.getX() > 0) {
            if (b.getMin().getX() > a.getMax().getX()) return null;
            if (a.getMax().getX() > b.getMin().getX())
                outTime = Math.min((a.getMax().getX() - b.getMin().getX()) / v.getX(), outTime);

            if (b.getMax().getX() < a.getMin().getX()) {
                overlapTime.setX((a.getMin().getX() - b.getMax().getX()) / v.getX());
                hitTime = Math.max(overlapTime.getX(), hitTime);
            }
        }

        if (hitTime > outTime) return null;

        //=================================

        // Y axis overlap
        if (v.getY() < 0) {
            if (b.getMax().getY() < a.getMin().getY()) return null;
            if (b.getMax().getY() > a.getMin().getY())
                outTime = Math.min((a.getMin().getY() - b.getMax().getY()) / v.getY(), outTime);

            if (a.getMax().getY() < b.getMin().getY()) {
                overlapTime.setY((a.getMax().getY() - b.getMin().getY()) / v.getY());
                hitTime = Math.max(overlapTime.getY(), hitTime);
            }
        } else if (v.getY() > 0) {
            if (b.getMin().getY() > a.getMax().getY()) return null;
            if (a.getMax().getY() > b.getMin().getY())
                outTime = Math.min((a.getMax().getY() - b.getMin().getY()) / v.getY(), outTime);

            if (b.getMax().getY() < a.getMin().getY()) {
                overlapTime.setY((a.getMin().getY() - b.getMax().getY()) / v.getY());
                hitTime = Math.max(overlapTime.getY(), hitTime);
            }
        }

        if (hitTime > outTime) return null;

        //=================================

        // Z axis overlap
        if (v.getZ() < 0) {
            if (b.getMax().getZ() < a.getMin().getZ()) return null;
            if (b.getMax().getZ() > a.getMin().getZ())
                outTime = Math.min((a.getMin().getZ() - b.getMax().getZ()) / v.getZ(), outTime);

            if (a.getMax().getZ() < b.getMin().getZ()) {
                overlapTime.setZ((a.getMax().getZ() - b.getMin().getZ()) / v.getZ());
                hitTime = Math.max(overlapTime.getZ(), hitTime);
            }
        } else if (v.getZ() > 0) {
            if (b.getMin().getZ() > a.getMax().getZ()) return null;
            if (a.getMax().getZ() > b.getMin().getZ())
                outTime = Math.min((a.getMax().getZ() - b.getMin().getZ()) / v.getZ(), outTime);

            if (b.getMax().getZ() < a.getMin().getZ()) {
                overlapTime.setZ((a.getMin().getZ() - b.getMax().getZ()) / v.getZ());
                hitTime = Math.max(overlapTime.getZ(), hitTime);
            }
        }

        if (hitTime > outTime) return null;
        Vector hitNormal;
        // Scale resulting velocity by normalized hit time
        Vector outVel = vel.clone().multiply(hitTime);

        // Hit normal is along axis with the highest overlap time
        if (overlapTime.getX() > overlapTime.getY()) {
            if (overlapTime.getZ() > overlapTime.getX()) {
                hitNormal = new Vector(0, 0, v.getZ());
            } else {
                hitNormal = new Vector(v.getX(), 0, 0);
            }
        } else {
            if (overlapTime.getZ() > overlapTime.getY()) {
                hitNormal = new Vector(0, 0, v.getZ());
            } else {
                hitNormal = new Vector(0, v.getY(), 0);
            }
        }

        return Pair.of(outVel, hitNormal);
    }

    @Deprecated
    public static List<String> wrapLines(String txt, int maxwidth) {
        List<String> words = new ArrayList<>();
        for (String word : txt.split(" ")) {
            if (word.length() > 0)
                words.add(word);
        }
        if (words.size() <= 0) return Collections.emptyList();

        for (String str : words) {
            int len = getStringWidth(ChatColor.stripColor(str));
            if (len > maxwidth) maxwidth = len;
        }

        List<String> ans = new ArrayList<>();
        int idx = 0, currlen = getStringWidth(ChatColor.stripColor(words.get(0)));
        ans.add(words.remove(0));
        while (words.size() > 0) {
            String tmp = words.remove(0);
            int word_len = getStringWidth(ChatColor.stripColor(tmp));
            if (currlen + 4 + word_len <= maxwidth) {
                currlen += 4 + word_len;
                ans.set(idx, ans.get(idx) + " " + tmp);
            } else {
                currlen = word_len;
                ans.add(tmp);
                idx++;
            }
        }
        for (int i = 1; i < ans.size(); i++) {
            ans.set(i, getLastFormat(ans.get(i - 1)) + ans.get(i));
        }
        return ans;
    }

    @Deprecated
    public static int getStringWidth(String str) {
        int width = 0;
        for (int i = 0; i < str.length(); i++) {
            char c = str.charAt(i);
            width += Font.widths[c] + 1;
        }
        return width;
    }

    @Deprecated
    private static String getLastFormat(String str) {
        String format = null;
        int length = str.length();

        for (int index = length - 2; index > -1; index--) {
            char chr = str.charAt(index);
            if (chr == ChatColor.COLOR_CHAR) {
                char c = str.charAt(index + 1);
                ChatColor style = ChatColor.getByChar(c);
                if (style == null) continue;
                if (style.isColor()) return style.toString() + (format == null ? "" : format);
                if (style.isFormat() && format == null) format = style.toString();
            }
        }

        return (format == null ? "" : format);
    }
}
