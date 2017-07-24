/*
 *  This file is part of RPG Items.
 *
 *  RPG Items is free software: you can redistribute it and/or modify
 *  it under the terms of the GNU General Public License as published by
 *  the Free Software Foundation, either version 3 of the License, or
 *  (at your option) any later version.
 *
 *  RPG Items is distributed in the hope that it will be useful,
 *  but WITHOUT ANY WARRANTY; without even the implied warranty of
 *  MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
 *  GNU General Public License for more details.
 *
 *  You should have received a copy of the GNU General Public License
 *  along with RPG Items.  If not, see <http://www.gnu.org/licenses/>.
 */
package think.rpgitems.power;

import org.bukkit.*;
import org.bukkit.block.Block;
import org.bukkit.configuration.ConfigurationSection;
import org.bukkit.entity.Player;
import org.bukkit.entity.Projectile;
import org.bukkit.inventory.ItemStack;
import org.bukkit.util.BlockIterator;
import think.rpgitems.I18n;
import think.rpgitems.RPGItems;
import think.rpgitems.commands.ArgumentPriority;
import think.rpgitems.power.types.PowerProjectileHit;
import think.rpgitems.power.types.PowerRightClick;

/**
 * Power teleport.
 * <p>
 * The teleport power will teleport you
 * in the direction you're looking in
 * or to the place where the projectile hit
 * with maximum distance of {@link #distance} blocks
 * </p>
 */
public class PowerTeleport extends Power implements PowerRightClick, PowerProjectileHit {

    /**
     * Maximum distance.
     */
    @ArgumentPriority(1)
    public int distance = 5;
    /**
     * Cooldown time of this power
     */
    @ArgumentPriority
    public long cooldownTime = 20;
    /**
     * Cost of this power
     */
    public int consumption = 0;

    @Override
    public void rightClick(Player player, ItemStack stack, Block clicked) {
        if (!item.checkPermission(player, true)) return;
        if (!checkCooldown(player, cooldownTime, true)) return;
        if (!item.consumeDurability(stack, consumption)) return;
        // float dist = 0;
        World world = player.getWorld();
        Location start = player.getLocation();
        start.setY(start.getY() + 1.6);
        // Location current = new Location(world, 0, 0, 0);
        Block lastSafe = world.getBlockAt(start);
        // Keeping the old method because BlockIterator could get removed (irc)
        // double dir = Math.toRadians(start.getYaw()) + (Math.PI / 2d);
        // double dirY = Math.toRadians(start.getPitch()) + (Math.PI / 2d);
        try {
            BlockIterator bi = new BlockIterator(player, distance);
            // while (dist < distance) {
            while (bi.hasNext()) {
                // current.setX(start.getX() + dist * Math.cos(dir) *
                // Math.sin(dirY));
                // current.setY(start.getY() + dist * Math.cos(dirY));
                // current.setZ(start.getZ() + dist * Math.sin(dir) *
                // Math.sin(dirY));
                Block block = bi.next();// world.getBlockAt(current);
                if (!block.getType().isSolid() || (block.getType() == Material.AIR)) {
                    lastSafe = block;
                } else {
                    break;
                }
                // dist+= 0.5;
            }
        } catch (IllegalStateException ex) {
            ex.printStackTrace();
            RPGItems.logger.info("This exception may be harmless");
        }
        Location newLoc = lastSafe.getLocation();
        newLoc.setPitch(start.getPitch());
        newLoc.setYaw(start.getYaw());
        player.teleport(newLoc);
        world.playEffect(newLoc, Effect.ENDER_SIGNAL, 0);
        world.playSound(newLoc, Sound.ENTITY_ENDERMEN_TELEPORT, 1.0f, 0.3f);
    }

    @Override
    public void projectileHit(Player player, ItemStack stack, Projectile p) {
        if (!item.checkPermission(player, true)) return;
        if (!checkCooldown(player, cooldownTime, true)) return;
        if (!item.consumeDurability(stack, consumption)) return;
        World world = player.getWorld();
        Location start = player.getLocation();
        Location newLoc = p.getLocation();
        if (start.distanceSquared(newLoc) >= distance * distance) {
            player.sendMessage(I18n.format("message.too.far"));
            return;
        }
        newLoc.setPitch(start.getPitch());
        newLoc.setYaw(start.getYaw());
        player.teleport(newLoc);
        world.playEffect(newLoc, Effect.ENDER_SIGNAL, 0);
        world.playSound(newLoc, Sound.ENTITY_ENDERMEN_TELEPORT, 1.0f, 0.3f);
    }

    @Override
    public void init(ConfigurationSection s) {
        cooldownTime = s.getLong("cooldown");
        distance = s.getInt("distance");
        consumption = s.getInt("consumption", 0);
    }

    @Override
    public void save(ConfigurationSection s) {
        s.set("cooldown", cooldownTime);
        s.set("distance", distance);
        s.set("consumption", consumption);
    }

    @Override
    public String getName() {
        return "teleport";
    }

    @Override
    public String displayText() {
        return I18n.format("power.teleport", distance, (double) cooldownTime / 20d);
    }

}
