package cat.nyaa.rpgitems.power.impl;

import org.bukkit.attribute.Attribute;
import org.bukkit.entity.LivingEntity;
import org.bukkit.entity.Player;
import org.bukkit.event.entity.EntityDamageByEntityEvent;
import org.bukkit.inventory.ItemStack;
import org.bukkit.potion.PotionEffect;
import org.bukkit.potion.PotionEffectType;
import think.rpgitems.I18n;
import think.rpgitems.data.Context;
import think.rpgitems.power.*;

import static java.lang.Double.max;
import static java.lang.Double.min;
import static think.rpgitems.power.Utils.checkCooldown;


/**
 * Power realdamage.
 * <p>
 * The item will do {@link #realDamage} to {@link LivingEntity} player hits
 * </p>
 */
@SuppressWarnings("WeakerAccess")
@PowerMeta(immutableTrigger = true)
public class PowerRealDamage extends BasePower implements PowerHit {

    /**
     * Cooldown time of this power
     */
    @Property(order = 0)
    public long cooldown = 20;
    /**
     * Cost of this power
     */
    @Property
    public int cost = 0;
    /**
     * Damage of this power
     */
    @Property(order = 1, required = true)
    public double realDamage = 0;
    /**
     * Minimum damage to trigger
     */
    @Property
    public double minDamage = 0;

    @Override
    public PowerResult<Double> hit(Player player, ItemStack stack, LivingEntity entity, double damage, EntityDamageByEntityEvent event) {
        if (damage < minDamage) return PowerResult.noop();
        if (!checkCooldown(this, player, cooldown, true, true)) return PowerResult.cd();
        if (!getItem().consumeDurability(stack, cost)) return PowerResult.cost();
        if (entity.hasPotionEffect(PotionEffectType.DAMAGE_RESISTANCE)) {
            PotionEffect e = entity.getPotionEffect(PotionEffectType.DAMAGE_RESISTANCE);
            if (e.getAmplifier() >= 4) return PowerResult.noop();
        }
        Context.instance().putExpiringSeconds(player.getUniqueId(), "realdamage.target", entity, 3);

        double health = entity.getHealth();
        double newHealth = health - realDamage;
        newHealth = max(newHealth, 0.1);//Bug workaround
        newHealth = min(newHealth, entity.getAttribute(Attribute.GENERIC_MAX_HEALTH).getValue());
        entity.setHealth(newHealth);
        return PowerResult.ok(damage);
    }

    @Override
    public String displayText() {
        return I18n.format("power.realdamage", realDamage);
    }

    @Override
    public String getName() {
        return "realdamage";
    }
}
