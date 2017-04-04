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

import org.bukkit.Bukkit;
import org.bukkit.ChatColor;
import org.bukkit.Sound;
import org.bukkit.block.Block;
import org.bukkit.configuration.ConfigurationSection;
import org.bukkit.entity.*;
import org.bukkit.event.entity.EntityDamageByEntityEvent;
import org.bukkit.event.entity.EntityDamageEvent;
import org.bukkit.inventory.ItemStack;
import org.bukkit.metadata.FixedMetadataValue;
import org.bukkit.util.Vector;
import think.rpgitems.Events;
import think.rpgitems.Plugin;
import think.rpgitems.data.Locale;
import think.rpgitems.power.types.PowerHitTaken;
import think.rpgitems.power.types.PowerLeftClick;
import think.rpgitems.power.types.PowerRightClick;

import java.util.concurrent.ThreadLocalRandom;

/**
 * Power deflect.
 * <p>
 * Deflect arrows or fireballs towards player within {@link #facing} when
 * 1. manual triggered when {@link #initiative} is enabled with a cooldown of {@link #cooldownTime} and duration {@link #duration}
 * 2. auto triggered when {@link #passive} is enabled with a chance of {@link #chance} and a cooldown of {@link #cooldownTimePassive}
 * </p>
 */
public class PowerDeflect extends Power implements PowerHitTaken, PowerRightClick, PowerLeftClick {

    /**
     * Cooldown time of this power
     */
    public int cooldownTime = 20;

    /**
     * Cooldown time of this power in passive mode
     */
    public int cooldownTimePassive = 20;

    /**
     * Cost of this power
     */
    public int consumption = 0;

    /**
     * Chance in percentage of triggering this power in passive mode
     */
    public int chance = 50;

    /**
     * Whether it is passive
     */
    public boolean passive = true;

    /**
     * Whether it is passive
     */
    public boolean initiative = true;

    /**
     * Whether triggers when right click.
     */
    public boolean isRight = true;

    /**
     * Duration of this power
     */
    public int duration = 50;

    /**
     * Maximum view angle
     */
    public double facing = 30;

    private long time = 0;

    @Override
    public String displayText() {
        return ChatColor.GREEN + String.format(Locale.get("power.deflect"), (double) cooldownTime / 20d);
    }

    @Override
    public String getName() {
        return "deflect";
    }

    @Override
    public void init(ConfigurationSection s) {
        cooldownTime = s.getInt("cooldown", 20);
        cooldownTimePassive = s.getInt("cooldownpassive", 20);
        chance = s.getInt("chance", 50);
        consumption = s.getInt("consumption", 0);
        duration = s.getInt("duration", 50);
        facing = s.getInt("facing", 120);
        initiative = s.getBoolean("initiative", true);
        passive = s.getBoolean("passive", true);
        isRight = s.getBoolean("isRight", true);
    }

    @Override
    public void save(ConfigurationSection s) {
        s.set("cooldown", cooldownTime);
        s.set("cooldownpassive", cooldownTimePassive);
        s.set("consumption", consumption);
        s.set("chance", chance);
        s.set("duration", duration);
        s.set("facing", facing);
        s.set("passive", passive);
        s.set("initiative", initiative);
        s.set("isRight", isRight);
    }

    @Override
    public double takeHit(Player target, ItemStack stack, EntityDamageEvent event) {
        if (!item.checkPermission(target, true)
                || !((System.currentTimeMillis() / 50 < time) || (passive && (ThreadLocalRandom.current().nextInt(0, 100) < chance) && checkCooldown(target, cooldownTimePassive, false)))
                || !item.consumeDurability(stack, consumption))
            return event.getDamage();
        if (event instanceof EntityDamageByEntityEvent) {
            EntityDamageByEntityEvent byEntityEvent = (EntityDamageByEntityEvent) event;
            if (byEntityEvent.getDamager() instanceof Projectile) {
                Projectile p = (Projectile) byEntityEvent.getDamager();
                if (!(p.getShooter() instanceof LivingEntity)) return event.getDamage();
                LivingEntity source = (LivingEntity) p.getShooter();
                Vector relativePosition = target.getEyeLocation().toVector();
                relativePosition.subtract(source.getEyeLocation().toVector());
                if (getAngleBetweenVectors(target.getEyeLocation().getDirection(), relativePosition.multiply(-1)) < facing
                        && (p instanceof SmallFireball || p instanceof LargeFireball || p instanceof Arrow)) {
                    event.setCancelled(true);
                    p.remove();
                    target.getLocation().getWorld().playSound(target.getLocation(), Sound.ITEM_SHIELD_BLOCK, 1.0f, 3.0f);
                    Bukkit.getScheduler().runTaskLater(Plugin.plugin, () -> {
                        Projectile t = target.launchProjectile(p.getClass());
                        if (p instanceof TippedArrow) {
                            TippedArrow tippedArrowP = (TippedArrow) p;
                            TippedArrow tippedArrowT = (TippedArrow) t;
                            tippedArrowT.setBasePotionData(tippedArrowP.getBasePotionData());
                            tippedArrowP.getCustomEffects().forEach(potionEffect -> tippedArrowT.addCustomEffect(potionEffect, true));
                        }
                        t.setShooter(target);
                        t.setMetadata("rpgitems.force", new FixedMetadataValue(Plugin.plugin, 1));
                        Events.removeArrows.add(t.getEntityId());
                    }, 1);
                    return 0;
                }
            }
        }
        return event.getDamage();
    }

    @Override
    public void rightClick(Player player, ItemStack stack, Block clicked) {
        if (!isRight || !initiative
                || !item.checkPermission(player, true)
                || !checkCooldownByString(player, item, "deflect.initiative", cooldownTime)
                || !item.consumeDurability(stack, consumption))
            return;
        time = System.currentTimeMillis() / 50 + duration;
    }

    @Override
    public void leftClick(Player player, ItemStack stack, Block clicked) {
        if (isRight || !initiative
                || !item.checkPermission(player, true)
                || !checkCooldownByString(player, item, "deflect.initiative", cooldownTime)
                || !item.consumeDurability(stack, consumption))
            return;
        time = System.currentTimeMillis() / 50 + duration;
    }
}
