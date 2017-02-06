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

import java.util.HashMap;

import org.bukkit.*;
import org.bukkit.block.Block;
import org.bukkit.configuration.ConfigurationSection;
import org.bukkit.entity.Player;

import org.bukkit.inventory.ItemStack;
import think.rpgitems.data.Locale;
import think.rpgitems.data.RPGValue;
import think.rpgitems.power.types.PowerLeftClick;
import think.rpgitems.power.types.PowerRightClick;
import think.rpgitems.support.WorldGuard;

public class PowerColor extends Power implements PowerRightClick, PowerLeftClick {

    public long cooldownTime = 0;
    public boolean glass = true;
    public boolean clay = true;
    public boolean wool = true;
    public int consumption = 0;

    private static HashMap<DyeColor, ChatColor> dyeToChatColor;

    static {
        dyeToChatColor = new HashMap<DyeColor, ChatColor>();
        dyeToChatColor.put(DyeColor.BLACK, ChatColor.DARK_GRAY);
        dyeToChatColor.put(DyeColor.BLUE, ChatColor.DARK_BLUE);
        dyeToChatColor.put(DyeColor.BROWN, ChatColor.GOLD);
        dyeToChatColor.put(DyeColor.CYAN, ChatColor.AQUA);
        dyeToChatColor.put(DyeColor.GRAY, ChatColor.GRAY);
        dyeToChatColor.put(DyeColor.GREEN, ChatColor.DARK_GREEN);
        dyeToChatColor.put(DyeColor.LIGHT_BLUE, ChatColor.BLUE);
        dyeToChatColor.put(DyeColor.LIME, ChatColor.GREEN);
        dyeToChatColor.put(DyeColor.MAGENTA, ChatColor.LIGHT_PURPLE);
        dyeToChatColor.put(DyeColor.ORANGE, ChatColor.GOLD);
        dyeToChatColor.put(DyeColor.PINK, ChatColor.LIGHT_PURPLE);
        dyeToChatColor.put(DyeColor.PURPLE, ChatColor.DARK_PURPLE);
        dyeToChatColor.put(DyeColor.RED, ChatColor.DARK_RED);
        dyeToChatColor.put(DyeColor.SILVER, ChatColor.GRAY);
        dyeToChatColor.put(DyeColor.WHITE, ChatColor.WHITE);
        dyeToChatColor.put(DyeColor.YELLOW, ChatColor.YELLOW);
    }

    @SuppressWarnings("deprecation")
    @Override
    public void rightClick(Player player, ItemStack i, Block clicked) {
        if (clicked == null)
            return;
        if(!WorldGuard.canBuild(player, clicked.getLocation()))
            return;
        if (clicked.getType().toString().contains("GLASS")) {
            if (!glass)
                return;
        } else if (clicked.getType().toString().contains("CLAY")) {
            if (!clay)
                return;
        } else if (clicked.getType().equals(Material.WOOL)) {
            if (!wool)
                return;
        } else {
            return;
        }

        long cooldown;
        if (item.getHasPermission() == true && player.hasPermission(item.getPermission()) == false) {
        } else {
            RPGValue color = RPGValue.get(player, item, "color.current");
            if (color == null) {
                color = new RPGValue(player, item, "color.current", 0);
            }

            RPGValue value = RPGValue.get(player, item, "color.cooldown");
            if (value == null) {
                cooldown = System.currentTimeMillis() / 50;
                value = new RPGValue(player, item, "color.cooldown", cooldown);
            } else {
                cooldown = value.asLong();
            }
            if (cooldown <= System.currentTimeMillis() / 50) {
                if(!item.consumeDurability(i,consumption))return;
                value.set(System.currentTimeMillis() / 50 + cooldownTime);
                
                if (clicked.getType().equals(Material.GLASS))
                    clicked.setType(Material.STAINED_GLASS);
                if (clicked.getType().equals(Material.THIN_GLASS))
                    clicked.setType(Material.STAINED_GLASS_PANE);
                if (clicked.getType().equals(Material.CLAY) || clicked.getType().equals(Material.HARD_CLAY))
                    clicked.setType(Material.STAINED_CLAY);

                clicked.setData(DyeColor.values()[color.asInt()].getDyeData());
            } else {
                player.sendMessage(ChatColor.AQUA + String.format(Locale.get("message.cooldown"), ((double) (cooldown - System.currentTimeMillis() / 50)) / 20d));
            }
        }

    }

    @Override
    public void leftClick(Player player, ItemStack i, Block clicked) {
        if (item.getHasPermission() == true && player.hasPermission(item.getPermission()) == false) {
        } else {
            RPGValue value = RPGValue.get(player, item, "color.current");
            if (value == null) {
                value = new RPGValue(player, item, "color.current", 0);
            } else {
                int newColorIndex = (value.asInt() + 1) % 16;
                value.set(newColorIndex);
                player.sendMessage(ChatColor.AQUA + ChatColor.translateAlternateColorCodes('&', String.format(Locale.get("message.color.next"), dyeToChatColor.get(DyeColor.values()[newColorIndex]))));
            }
        }
    }

    @Override
    public String displayText() {
        return ChatColor.GREEN + String.format(Locale.get("power.color"), (double) cooldownTime / 20d);
    }

    @Override
    public String getName() {
        return "color";
    }

    @Override
    public void init(ConfigurationSection s) {
        cooldownTime = s.getLong("cooldown", 0);
        glass = s.getBoolean("glass", true);
        wool = s.getBoolean("wool", true);
        clay = s.getBoolean("clay", true);
        consumption = s.getInt("consumption", 0);
    }

    @Override
    public void save(ConfigurationSection s) {
        s.set("cooldown", cooldownTime);
        s.set("glass", glass);
        s.set("clay", clay);
        s.set("wool", wool);
        s.set("consumption", consumption);
    }

}
