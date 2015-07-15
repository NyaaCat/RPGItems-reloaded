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

import org.bukkit.ChatColor;
import org.bukkit.Effect;
import org.bukkit.Location;
import org.bukkit.Material;
import org.bukkit.Sound;
import org.bukkit.World;
import org.bukkit.block.Block;
import org.bukkit.configuration.ConfigurationSection;
import org.bukkit.entity.Player;
import org.bukkit.entity.Projectile;
import org.bukkit.util.BlockIterator;

import think.rpgitems.data.Locale;
import think.rpgitems.data.RPGValue;
import think.rpgitems.power.types.PowerProjectileHit;
import think.rpgitems.power.types.PowerRightClick;

public class PowerTeleport extends Power implements PowerRightClick, PowerProjectileHit {

    public int distance = 5;
    public long cooldownTime = 20;

    @Override
    public void rightClick(Player player) {
        long cooldown;
        if (item.getHasPermission() == true && player.hasPermission(item.getPermission()) == false){
        }else{
        RPGValue value = RPGValue.get(player, item, "teleport.cooldown");
        if (value == null) {
            cooldown = System.currentTimeMillis() / 50;
            value = new RPGValue(player, item, "teleport.cooldown", cooldown);
        } else {
            cooldown = value.asLong();
        }
        if (cooldown <= System.currentTimeMillis() / 50) {
            value.set(System.currentTimeMillis() / 50 + cooldownTime);
            // float dist = 0;
            World world = player.getWorld();
            Location start = player.getLocation();
            start.setY(start.getY() + 1.6);
            // Location current = new Location(world, 0, 0, 0);
            Block lastSafe = world.getBlockAt(start);
            // Keeping the old method because BlockIterator could get removed (irc)
            // double dir = Math.toRadians(start.getYaw()) + (Math.PI / 2d);
            // double dirY = Math.toRadians(start.getPitch()) + (Math.PI / 2d);
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
            Location newLoc = lastSafe.getLocation();
            newLoc.setPitch(start.getPitch());
            newLoc.setYaw(start.getYaw());
            player.teleport(newLoc);
            world.playEffect(newLoc, Effect.ENDER_SIGNAL, 0);
            world.playSound(newLoc, Sound.ENDERMAN_TELEPORT, 1.0f, 0.3f);
        } else {
            player.sendMessage(ChatColor.AQUA + String.format(Locale.get("message.cooldown", Locale.getPlayerLocale(player)), ((double) (cooldown - System.currentTimeMillis() / 50)) / 20d));
        }
        }
    }

    @Override
    public void projectileHit(Player player, Projectile p) {
        long cooldown;
        RPGValue value = RPGValue.get(player, item, "teleport.cooldown");
        if (value == null) {
            cooldown = System.currentTimeMillis() / 50;
            value = new RPGValue(player, item, "teleport.cooldown", cooldown);
        } else {
            cooldown = value.asLong();
        }
        if (cooldown <= System.currentTimeMillis() / 50) {
            value.set(System.currentTimeMillis() / 50 + cooldownTime);
            World world = player.getWorld();
            Location start = player.getLocation();
            Location newLoc = p.getLocation();
            if (start.distanceSquared(newLoc) >= distance * distance) {
                player.sendMessage(ChatColor.AQUA + Locale.get("message.too.far", Locale.getPlayerLocale(player)));
                return;
            }
            newLoc.setPitch(start.getPitch());
            ;
            newLoc.setYaw(start.getYaw());
            player.teleport(newLoc);
            world.playEffect(newLoc, Effect.ENDER_SIGNAL, 0);
            world.playSound(newLoc, Sound.ENDERMAN_TELEPORT, 1.0f, 0.3f);
        } else {
            player.sendMessage(ChatColor.AQUA + String.format(Locale.get("message.cooldown", Locale.getPlayerLocale(player)), ((double) (cooldown - System.currentTimeMillis() / 50)) / 20d));
        }
    }

    @Override
    public void init(ConfigurationSection s) {
        cooldownTime = s.getLong("cooldown");
        distance = s.getInt("distance");
    }

    @Override
    public void save(ConfigurationSection s) {
        s.set("cooldown", cooldownTime);
        s.set("distance", distance);
    }

    @Override
    public String getName() {
        return "teleport";
    }

    @Override
    public String displayText(String locale) {
        return ChatColor.GREEN + String.format(Locale.get("power.teleport", locale), distance, (double) cooldownTime / 20d);
    }
}
