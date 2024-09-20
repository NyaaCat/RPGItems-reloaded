package think.rpgitems.power.impl;

import org.bukkit.Sound;
import org.bukkit.attribute.Attribute;
import org.bukkit.entity.LivingEntity;
import org.bukkit.entity.Player;
import org.bukkit.event.entity.EntityDamageByEntityEvent;
import org.bukkit.inventory.ItemStack;
import think.rpgitems.I18n;
import think.rpgitems.event.PowerActivateEvent;
import think.rpgitems.power.*;

import java.util.HashMap;

/**
 * Power charge.
 * <p>
 * Do more damage when sprinting
 * </p>
 */
@Meta(defaultTrigger = "HIT", implClass = Charge.Impl.class)
public class Charge extends BasePower {

    @Property
    public int percentage = 30;

    @Property
    public int speedPercentage = 20;

    @Property
    public double cap = 300;

    @Property
    public boolean setBaseDamage = false;

    public double getCap() {
        return cap;
    }

    @Override
    public String getName() {
        return "charge";
    }

    @Override
    public String displayText() {
        return I18n.formatDefault("power.charge", getPercentage());
    }

    public int getPercentage() {
        return percentage;
    }

    public int getSpeedPercentage() {
        return speedPercentage;
    }

    public boolean isSetBaseDamage() {
        return setBaseDamage;
    }

    public class Impl implements PowerHit {
        @Override
        public PowerResult<Double> hit(Player player, ItemStack stack, LivingEntity entity, double damage, EntityDamageByEntityEvent event) {
            HashMap<String,Object> argsMap = new HashMap<>();
            argsMap.put("damage",damage);
            PowerActivateEvent powerEvent = new PowerActivateEvent(player,stack,getPower(),argsMap);
            if(!powerEvent.callEvent())
                return PowerResult.fail();
            if (!player.isSprinting())
                return PowerResult.noop();
            double originDamage = damage;
            damage = damage * (1 + getPercentage() / 100.0);
            damage = damage + damage * (player.getAttribute(Attribute.GENERIC_MOVEMENT_SPEED).getValue() / 0.13 - 1) * (getSpeedPercentage() / 100.0);
            damage = Math.max(Math.min(damage, getCap()), originDamage);
            event.setDamage(damage);
            if (damage > originDamage) {
                player.playSound(player.getLocation(), Sound.BLOCK_ANVIL_LAND, 0.1f, 0.1f);
            }
            if (isSetBaseDamage()) {
                event.setDamage(damage);
            }
            return PowerResult.ok(damage);
        }

        @Override
        public Power getPower() {
            return Charge.this;
        }
    }
}
