package think.rpgitems.power.impl;

import org.bukkit.Bukkit;
import org.bukkit.entity.LivingEntity;
import org.bukkit.entity.Player;
import org.bukkit.event.entity.EntityDamageByEntityEvent;
import org.bukkit.inventory.ItemStack;
import think.rpgitems.I18n;
import think.rpgitems.RPGItems;
import think.rpgitems.power.PowerHit;
import think.rpgitems.power.PowerMeta;
import think.rpgitems.power.PowerResult;
import think.rpgitems.power.Property;

import java.util.Random;

/**
 * Power knockup.
 * <p>
 * The knockup power will send the hit target flying
 * with a chance of 1/{@link #chance} and a power of {@link #power}.
 * </p>
 */
@PowerMeta(immutableTrigger = true)
public class PowerKnockup extends BasePower implements PowerHit {

    /**
     * Chance of triggering this power
     */
    @Property(order = 0)
    public int chance = 20;
    /**
     * Power of knock up
     */
    @Property(order = 1)
    public double power = 2;
    /**
     * Cost of this power
     */
    @Property
    public int cost = 0;

    private Random rand = new Random();

    @Override
    public PowerResult<Double> hit(Player player, ItemStack stack, LivingEntity entity, double damage, EntityDamageByEntityEvent event) {
        if (!getItem().consumeDurability(stack, cost)) return PowerResult.cost();
        if (rand.nextInt(chance) == 0) {
            Bukkit.getScheduler().runTask(RPGItems.plugin, () -> entity.setVelocity(player.getLocation().getDirection().setY(power)));
        }
        return PowerResult.ok(damage);
    }

    @Override
    public String displayText() {
        return I18n.format("power.knockup", (int) ((1d / (double) chance) * 100d));
    }

    @Override
    public String getName() {
        return "knockup";
    }
}
