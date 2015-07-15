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

import java.util.ArrayList;
import java.util.Iterator;
import java.util.Random;

import org.bukkit.ChatColor;
import org.bukkit.Effect;
import org.bukkit.Location;
import org.bukkit.Material;
import org.bukkit.Sound;
import org.bukkit.block.Block;
import org.bukkit.configuration.ConfigurationSection;
import org.bukkit.entity.FallingBlock;
import org.bukkit.entity.Player;
import org.bukkit.scheduler.BukkitRunnable;
import org.bukkit.util.Vector;

import think.rpgitems.Plugin;
import think.rpgitems.data.Locale;
import think.rpgitems.data.RPGValue;
import think.rpgitems.power.types.PowerRightClick;

public class PowerRainbow extends Power implements PowerRightClick {

    public long cooldownTime = 20;
    public int count = 5;
    private Random random = new Random();

    @Override
    public void rightClick(Player player) {
        long cooldown;
        if (item.getHasPermission() == true && player.hasPermission(item.getPermission()) == false){
        }else{
        RPGValue value = RPGValue.get(player, item, "arrow.rainbow");
        if (value == null) {
            cooldown = System.currentTimeMillis() / 50;
            value = new RPGValue(player, item, "arrow.rainbow", cooldown);
        } else {
            cooldown = value.asLong();
        }
        if (cooldown <= System.currentTimeMillis() / 50) {
            value.set(System.currentTimeMillis() / 50 + cooldownTime);
            player.playSound(player.getLocation(), Sound.SHOOT_ARROW, 1.0f, 1.0f);
            final ArrayList<FallingBlock> blocks = new ArrayList<FallingBlock>();
            for (int i = 0; i < count; i++) {
                FallingBlock block = player.getWorld().spawnFallingBlock(player.getLocation().add(0, 1.8, 0), Material.WOOL, (byte) random.nextInt(16));
                block.setVelocity(player.getLocation().getDirection().multiply(new Vector(random.nextDouble() * 2d + 0.5, random.nextDouble() * 2d + 0.5, random.nextDouble() * 2d + 0.5)));
                block.setDropItem(false);
                blocks.add(block);
            }
            (new BukkitRunnable() {

                ArrayList<Location> fallLocs = new ArrayList<Location>();
                Random random = new Random();

                public void run() {

                    Iterator<Location> l = fallLocs.iterator();
                    while (l.hasNext()) {
                        Location loc = l.next();
                        if (random.nextBoolean()) {
                            Block b = loc.getBlock();
                            if (b.getType() == Material.WOOL) {
                                loc.getWorld().playEffect(loc, Effect.STEP_SOUND, Material.WOOL.getId(), b.getData());
                                b.setType(Material.AIR);
                            }
                            l.remove();
                        }
                        if (random.nextInt(5) == 0) {
                            break;
                        }
                    }

                    Iterator<FallingBlock> it = blocks.iterator();
                    while (it.hasNext()) {
                        FallingBlock block = it.next();
                        if (block.isDead()) {
                            fallLocs.add(block.getLocation());
                            it.remove();
                        }
                    }

                    if (fallLocs.isEmpty() && blocks.isEmpty()) {
                        cancel();
                    }

                }
            }).runTaskTimer(Plugin.plugin, 0, 5);
        } else {
            player.sendMessage(ChatColor.AQUA + String.format(Locale.get("message.cooldown", Locale.getPlayerLocale(player)), ((double) (cooldown - System.currentTimeMillis() / 50)) / 20d));
        }
        }
    }

    @Override
    public String displayText(String locale) {
        return ChatColor.GREEN + String.format(Locale.get("power.rainbow", locale), count, (double) cooldownTime / 20d);
    }

    @Override
    public String getName() {
        return "rainbow";
    }

    @Override
    public void init(ConfigurationSection s) {
        cooldownTime = s.getLong("cooldown", 20);
        count = s.getInt("count", 5);
    }

    @Override
    public void save(ConfigurationSection s) {
        s.set("cooldown", cooldownTime);
        s.set("count", count);
    }
}
