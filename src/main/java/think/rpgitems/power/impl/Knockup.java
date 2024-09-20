package think.rpgitems.power.impl;

import org.bukkit.Bukkit;
import org.bukkit.entity.LivingEntity;
import org.bukkit.entity.Player;
import org.bukkit.event.entity.EntityDamageByEntityEvent;
import org.bukkit.inventory.ItemStack;
import think.rpgitems.I18n;
import think.rpgitems.RPGItems;
import think.rpgitems.event.PowerActivateEvent;
import think.rpgitems.power.*;

import java.util.HashMap;
import java.util.Random;

/**
 * Power knockup.
 * <p>
 * The knockup power will send the hit target flying
 * with a chance of 1/{@link #getChance()} and a power of {@link #getKnockUpPower()}.
 * </p>
 */
@Meta(defaultTrigger = "HIT", implClass = Knockup.Impl.class)
public class Knockup extends BasePower {

    @Property(order = 0)
    public int chance = 20;
    @Property(order = 1, alias = "power")
    public double knockUpPower = 2;
    @Property
    public int cost = 0;

    private final Random rand = new Random();

    /**
     * Cost of this power
     */
    public int getCost() {
        return cost;
    }

    /**
     * Power of knock up
     */
    public double getKnockUpPower() {
        return knockUpPower;
    }

    @Override
    public String getName() {
        return "knockup";
    }

    @Override
    public String displayText() {
        return I18n.formatDefault("power.knockup", (int) ((1d / (double) getChance()) * 100d));
    }

    /**
     * Chance of triggering this power
     */
    public int getChance() {
        return chance;
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
            if (!getItem().consumeDurability(stack, getCost())) return PowerResult.cost();
            if (rand.nextInt(getChance()) == 0) {
                Bukkit.getScheduler().runTask(RPGItems.plugin, () -> entity.setVelocity(player.getLocation().getDirection().setY(getKnockUpPower())));
            }
            return PowerResult.ok(damage);
        }

        @Override
        public Power getPower() {
            return Knockup.this;
        }
    }
}
