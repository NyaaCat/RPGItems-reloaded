package think.rpgitems.power.impl;

import org.bukkit.Sound;
import org.bukkit.entity.LivingEntity;
import org.bukkit.entity.Player;
import org.bukkit.event.entity.EntityDamageByEntityEvent;
import org.bukkit.inventory.ItemStack;
import think.rpgitems.I18n;
import think.rpgitems.power.*;

/**
 * Power airborne.
 * <p>
 * Do more damage when gliding
 * </p>
 */
@Meta(immutableTrigger = true, implClass = Airborne.Impl.class)
public class Airborne extends BasePower {
    @Property
    public int percentage = 50;

    @Property
    public double cap = 300.0;

    @Property
    public boolean setBaseDamage = false;

    public double getCap() {
        return cap;
    }

    @Override
    public String getName() {
        return "airborne";
    }

    @Override
    public String displayText() {
        return I18n.formatDefault("power.airborne", getPercentage());
    }

    public int getPercentage() {
        return percentage;
    }

    public boolean isSetBaseDamage() {
        return setBaseDamage;
    }

    class Impl implements PowerHit {

        @Override
        public PowerResult<Double> hit(Player player, ItemStack stack, LivingEntity entity, double damage, EntityDamageByEntityEvent event) {
            if (!player.isGliding())
                return PowerResult.noop();
            double originDamage = damage;
            damage = damage * (1 + getPercentage() / 100.0);
            damage = Math.max(Math.min(damage, getCap()), originDamage);
            if (damage > originDamage) {
                player.playSound(player.getLocation(), Sound.ENTITY_BAT_TAKEOFF, 0.1f, 0.1f);
            }
            if (isSetBaseDamage()) {
                event.setDamage(damage);
            }
            return PowerResult.ok(damage);
        }

        @Override
        public Power getPower() {
            return Airborne.this;
        }
    }
}
