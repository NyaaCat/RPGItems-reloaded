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
import org.bukkit.DyeColor;
import org.bukkit.Material;
import org.bukkit.block.Block;
import org.bukkit.configuration.ConfigurationSection;
import org.bukkit.entity.Player;
import org.bukkit.inventory.ItemStack;
import think.rpgitems.I18n;
import think.rpgitems.commands.ArgumentPriority;
import think.rpgitems.data.RPGValue;
import think.rpgitems.power.types.PowerLeftClick;
import think.rpgitems.power.types.PowerRightClick;
import think.rpgitems.support.WorldGuard;

import java.util.HashMap;

/**
 * Power color.
 * <p>
 * The power allows changing the color of glass, clay and wool.
 * You can choose which blocks are paintable by providing three true/false statements
 * for the order: glass, clay, wool.
 * </p>
 */
public class PowerColor extends Power implements PowerRightClick, PowerLeftClick {

    private static HashMap<DyeColor, ChatColor> dyeToChatColor;

    static {
        dyeToChatColor = new HashMap<>();
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

    /**
     * Cooldown time of this power
     */
    @ArgumentPriority
    public long cooldownTime = 0;
    /**
     * Whether enabled on glass.
     */
    @ArgumentPriority(1)
    public boolean glass = true;
    /**
     * Whether enabled on clay.
     */
    @ArgumentPriority(2)
    public boolean clay = true;
    /**
     * Whether enabled on wool.
     */
    @ArgumentPriority(3)
    public boolean wool = true;
    /**
     * Cost of this power
     */
    public int consumption = 0;

    @SuppressWarnings("deprecation")
    @Override
    public void rightClick(Player player, ItemStack stack, Block clicked) {
        if (clicked == null)
            return;
        if (!WorldGuard.canBuild(player, clicked.getLocation()))
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

        if (!item.checkPermission(player, true)) return;
        if (!checkCooldown(player, cooldownTime, true)) return;
        if (!item.consumeDurability(stack, consumption)) return;
        RPGValue color = RPGValue.get(player, item, "color.current");
        if (color == null) {
            color = new RPGValue(player, item, "color.current", 0);
        }
        if (clicked.getType().equals(Material.GLASS))
            clicked.setType(Material.STAINED_GLASS);
        if (clicked.getType().equals(Material.THIN_GLASS))
            clicked.setType(Material.STAINED_GLASS_PANE);
        if (clicked.getType().equals(Material.CLAY) || clicked.getType().equals(Material.HARD_CLAY))
            clicked.setType(Material.STAINED_CLAY);

        clicked.setData(DyeColor.values()[color.asInt()].getDyeData());
    }

    @Override
    public void leftClick(Player player, ItemStack stack, Block clicked) {
        if (!item.checkPermission(player, true)) return;
        RPGValue value = RPGValue.get(player, item, "color.current");
        if (value == null) {
            new RPGValue(player, item, "color.current", 0);
        } else {
            int newColorIndex = (value.asInt() + 1) % 16;
            value.set(newColorIndex);
            player.sendMessage(I18n.format("message.color.next", dyeToChatColor.get(DyeColor.values()[newColorIndex]).toString()));
        }
    }

    @Override
    public String displayText() {
        return I18n.format("power.color", (double) cooldownTime / 20d);
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
