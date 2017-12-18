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

import com.google.common.cache.Cache;
import com.google.common.cache.CacheBuilder;
import org.bukkit.ChatColor;
import org.bukkit.block.Block;
import org.bukkit.configuration.ConfigurationSection;
import org.bukkit.entity.LivingEntity;
import org.bukkit.entity.Player;
import org.bukkit.event.entity.EntityTeleportEvent;
import org.bukkit.inventory.ItemStack;
import org.bukkit.potion.PotionEffect;
import org.bukkit.potion.PotionEffectType;
import think.rpgitems.Plugin;
import think.rpgitems.data.Locale;
import think.rpgitems.power.types.PowerHit;
import think.rpgitems.power.types.PowerRightClick;

import java.util.Random;
import java.util.concurrent.ExecutionException;
import java.util.concurrent.TimeUnit;

/**
 * Power lightning.
 * <p>
 * The lightning power will strike the hit target with lightning with a chance of 1/{@link #chance}.
 * </p>
 */
public class PowerStuck extends Power implements PowerHit {
    /**
     * Chance of triggering this power
     */
    public int chance = 20;

    /**
     * Cost of this power
     */
    public int consumption = 0;

    /**
     * Duration of this power in tick
     */
    public int duration = 100;

    /**
     * Cooldown time of this power
     */
    public long cooldownTime = 200;

    private Random random = new Random();

    private Cache<Integer, Long> stucked = CacheBuilder.newBuilder().expireAfterAccess(10, TimeUnit.MINUTES).concurrencyLevel(2).build();

    @Override
    public void hit(Player player, ItemStack stack, LivingEntity entity, double damage) {
        if (!checkCooldown(player, cooldownTime, true)) return;
        if (!item.checkPermission(player, true)) return;
        if (!item.consumeDurability(stack, consumption)) return;
        if (random.nextInt(chance) == 0) {
            stucked.put(entity.getEntityId(), System.currentTimeMillis());
            entity.addPotionEffect(new PotionEffect(PotionEffectType.SLOW, duration, 10), true);
        }
    }

    @Override
    public String displayText() {
        return ChatColor.GREEN + String.format(Locale.get("power.stuck"), (int) ((1d / (double) chance) * 100d), duration, (double) cooldownTime / 20d);
    }

    @Override
    public String getName() {
        return "stuck";
    }

    @Override
    public void init(ConfigurationSection s) {
        Plugin.listener.addEventListener(EntityTeleportEvent.class, e -> {
            try {
                if (stucked.get(e.getEntity().getEntityId(), () -> Long.MAX_VALUE) <= System.currentTimeMillis() + duration * 50) {
                    e.setCancelled(true);
                }
            } catch (ExecutionException ex) {
                ex.printStackTrace();
            }
        });
        chance = s.getInt("chance", 20);
        consumption = s.getInt("consumption", 0);
        cooldownTime = s.getLong("cooldown", 200);
        duration = s.getInt("duration", 100);
    }

    @Override
    public void save(ConfigurationSection s) {
        s.set("chance", chance);
        s.set("consumption", consumption);
        s.set("duration", duration);
        s.set("cooldown", cooldownTime);
    }
}
