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
package think.rpgitems.power.impl;

import org.bukkit.Bukkit;
import org.bukkit.Sound;
import org.bukkit.block.Block;
import org.bukkit.configuration.ConfigurationSection;
import org.bukkit.entity.*;
import org.bukkit.event.entity.EntityDamageByEntityEvent;
import org.bukkit.event.entity.EntityDamageEvent;
import org.bukkit.event.player.PlayerInteractEvent;
import org.bukkit.inventory.ItemStack;
import org.bukkit.metadata.FixedMetadataValue;
import org.bukkit.util.Vector;
import think.rpgitems.Events;
import think.rpgitems.I18n;
import think.rpgitems.RPGItems;
import think.rpgitems.power.*;

import java.util.HashSet;
import java.util.Set;
import java.util.concurrent.ThreadLocalRandom;

import static think.rpgitems.utils.PowerUtils.*;

/**
 * Power deflect.
 * <p>
 * Deflect arrows or fireballs towards player within {@link #facing} when
 * 1. manual triggered when some of initiative trigger are enabled with a cooldown of {@link #cooldown} and duration {@link #duration}
 * 2. auto triggered when {@link TriggerType#HIT_TAKEN} is enabled with a chance of {@link #chance} and a cooldown of {@link #cooldownPassive}
 * </p>
 */
@SuppressWarnings("WeakerAccess")
@PowerMeta(defaultTrigger = TriggerType.RIGHT_CLICK)
public class PowerDeflect extends BasePower implements PowerHitTaken, PowerRightClick, PowerLeftClick {

    /**
     * Cooldown time of this power
     */
    @Property(order = 2)
    public int cooldown = 20;

    /**
     * Cooldown time of this power in passive mode
     */
    @Property(order = 4)
    public int cooldownPassive = 20;

    /**
     * Cost of this power
     */
    @Property
    public int cost = 0;

    /**
     * Chance in percentage of triggering this power in passive mode
     */
    @Property
    public int chance = 50;

    /**
     * Duration of this power
     */
    @Property
    public int duration = 50;

    /**
     * Maximum view angle
     */
    @Property(order = 0, required = true)
    public double facing = 30;

    private long time = 0;

    @Override
    public String displayText() {
        return I18n.format("power.deflect", (double) cooldown / 20d);
    }

    @Override
    public String getName() {
        return "deflect";
    }

    @Override
    public void init(ConfigurationSection section) {
        cooldownPassive = section.getInt("cooldownpassive", 20);
        boolean passive = section.getBoolean("passive", false);
        boolean initiative = section.getBoolean("initiative", true);
        boolean isRight = section.getBoolean("isRight", true);
        triggers = new HashSet<>();
        if(passive){
            triggers.add(TriggerType.HIT_TAKEN);
        }
        if(initiative){
            triggers.add(isRight ? TriggerType.RIGHT_CLICK : TriggerType.LEFT_CLICK);
        }
        super.init(section);
    }

    @Override
    public PowerResult<Double> takeHit(Player target, ItemStack stack, double damage, EntityDamageEvent event) {
        if (!((System.currentTimeMillis() / 50 < time)
                      || (ThreadLocalRandom.current().nextInt(0, 100) < chance) && checkCooldown(this, target, cooldownPassive, false))
                    || !getItem().consumeDurability(stack, cost))
            return PowerResult.noop();
        if (event instanceof EntityDamageByEntityEvent) {
            EntityDamageByEntityEvent byEntityEvent = (EntityDamageByEntityEvent) event;
            if (byEntityEvent.getDamager() instanceof Projectile) {
                Projectile p = (Projectile) byEntityEvent.getDamager();
                if (!(p.getShooter() instanceof LivingEntity)) return PowerResult.noop();
                LivingEntity source = (LivingEntity) p.getShooter();
                Vector relativePosition = target.getEyeLocation().toVector();
                relativePosition.subtract(source.getEyeLocation().toVector());
                if (getAngleBetweenVectors(target.getEyeLocation().getDirection(), relativePosition.multiply(-1)) < facing
                            && (p instanceof SmallFireball || p instanceof LargeFireball || p instanceof Arrow)) {
                    event.setCancelled(true);
                    p.remove();
                    target.getLocation().getWorld().playSound(target.getLocation(), Sound.ITEM_SHIELD_BLOCK, 1.0f, 3.0f);
                    Bukkit.getScheduler().runTaskLater(RPGItems.plugin, () -> {
                        if (!target.isOnline() || target.isDead()) {
                            return;
                        }
                        Projectile t = target.launchProjectile(p.getClass());
                        if (p instanceof TippedArrow) {
                            TippedArrow tippedArrowP = (TippedArrow) p;
                            TippedArrow tippedArrowT = (TippedArrow) t;
                            tippedArrowT.setBasePotionData(tippedArrowP.getBasePotionData());
                            tippedArrowP.getCustomEffects().forEach(potionEffect -> tippedArrowT.addCustomEffect(potionEffect, true));
                        }
                        t.setShooter(target);
                        t.setMetadata("rpgitems.force", new FixedMetadataValue(RPGItems.plugin, 1));
                        Events.removeArrows.add(t.getEntityId());
                    }, 1);
                    return PowerResult.ok(0.0);
                }
            }
        }
        return PowerResult.noop();
    }

    @Override
    public PowerResult<Void> rightClick(Player player, ItemStack stack, Block clicked, PlayerInteractEvent event) {
        if (!checkCooldownByString(player, getItem(), "deflect.initiative", cooldown, true))
            return PowerResult.noop();
        if (!getItem().consumeDurability(stack, cost))
            return PowerResult.cost();
        time = System.currentTimeMillis() / 50 + duration;
        return PowerResult.ok();
    }

    @Override
    public PowerResult<Void> leftClick(Player player, ItemStack stack, Block clicked, PlayerInteractEvent event) {
        if (!checkCooldownByString(player, getItem(), "deflect.initiative", cooldown, true))
            return PowerResult.noop();
        if (!getItem().consumeDurability(stack, cost))
            return PowerResult.cost();
        time = System.currentTimeMillis() / 50 + duration;
        return PowerResult.ok();
    }
}
