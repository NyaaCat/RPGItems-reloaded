package think.rpgitems.power.impl;

import org.bukkit.entity.LivingEntity;
import org.bukkit.entity.Player;
import org.bukkit.event.entity.EntityDamageByEntityEvent;
import org.bukkit.inventory.ItemStack;
import org.bukkit.scheduler.BukkitRunnable;
import think.rpgitems.I18n;
import think.rpgitems.RPGItems;
import think.rpgitems.event.PowerActivateEvent;
import think.rpgitems.power.*;

import java.util.HashMap;

/**
 * Power noimmutabletick.
 * <p>
 * Cancel the damage delay (no-damage-tick)
 * </p>
 */
@Meta(defaultTrigger = "HIT", implClass = NoImmutableTick.Impl.class)
public class NoImmutableTick extends BasePower {

    @Property
    public int immuneTime = 1;

    public int getImmuneTime() {
        return immuneTime;
    }

    @Override
    public String getName() {
        return "noimmutabletick";
    }

    @Override
    public String displayText() {
        return I18n.formatDefault("power.noimmutabletick");
    }

    public class Impl implements PowerHit {

        @Override
        public PowerResult<Double> hit(Player player, ItemStack stack, LivingEntity entity, double damage, EntityDamageByEntityEvent event) {
            HashMap<String,Object> argsMap = new HashMap<>();
            argsMap.put("target",entity);
            argsMap.put("damage",damage);
            PowerActivateEvent powerEvent = new PowerActivateEvent(player,stack,getPower(),argsMap);
            if(!powerEvent.callEvent()) {
                return PowerResult.fail();
            }
            new BukkitRunnable() {
                @Override
                public void run() {
                    entity.setNoDamageTicks(getImmuneTime());
                }
            }.runTaskLater(RPGItems.plugin, 1);
            return PowerResult.ok(damage);
        }

        @Override
        public Power getPower() {
            return NoImmutableTick.this;
        }
    }
}
