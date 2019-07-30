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
 *  Do more damage when gliding
 * </p>
 */
@PowerMeta(immutableTrigger = true, implClass = PowerAirborne.Impl.class)
public class PowerAirborne extends BasePower {
    @Property
    private int percentage = 50;

    @Property
    private double cap = 300.0;

    @Property
    private boolean setBaseDamage = false;

    public double getCap() {
        return cap;
    }

    public int getPercentage() {
        return percentage;
    }

    public boolean isSetBaseDamage() {
        return setBaseDamage;
    }

    public void setCap(double cap) {
        this.cap = cap;
    }

    public void setPercentage(int percentage) {
        this.percentage = percentage;
    }

    public void setSetBaseDamage(boolean setBaseDamage) {
        this.setBaseDamage = setBaseDamage;
    }

    @Override
    public String getName() {
        return "airborne";
    }

    @Override
    public String displayText() {
        return I18n.format("power.airborne", getPercentage());
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
            return PowerAirborne.this;
        }
    }
}
