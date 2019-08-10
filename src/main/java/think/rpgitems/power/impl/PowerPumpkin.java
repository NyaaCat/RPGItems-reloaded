package think.rpgitems.power.impl;

import org.bukkit.Material;
import org.bukkit.entity.LivingEntity;
import org.bukkit.entity.Player;
import org.bukkit.entity.Skeleton;
import org.bukkit.entity.Zombie;
import org.bukkit.event.entity.EntityDamageByEntityEvent;
import org.bukkit.inventory.ItemStack;
import think.rpgitems.I18n;
import think.rpgitems.power.*;

import java.util.Random;

/**
 * Power pumpkin.
 * <p>
 * When hit skeleton or zombie, will have a 1/{@link #chance} chance
 * to make them wear pumpkin head.
 * And the pumpkin will have a chance of {@link #drop} to drop when the mobs die.
 * </p>
 */
@SuppressWarnings("WeakerAccess")
@PowerMeta(immutableTrigger = true, implClass = PowerPumpkin.Impl.class)
public class PowerPumpkin extends BasePower {
    private static final Random rand = new Random();
    @Property(order = 0)
    public int chance = 20;
    @Property(order = 1, required = true)
    public double drop = 0;
    @Property
    public int cost = 0;

    /**
     * Cost of this power
     */
    public int getCost() {
        return cost;
    }

    /**
     * Drop chance of the pumpkin
     */
    public double getDrop() {
        return drop;
    }

    @Override
    public String getName() {
        return "pumpkin";
    }

    @Override
    public String displayText() {
        return I18n.format("power.pumpkin", getChance());
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
            if (!getItem().consumeDurability(stack, getCost())) return PowerResult.cost();
            if (rand.nextInt(getChance()) != 0) return PowerResult.noop();
            if (entity instanceof Skeleton || entity instanceof Zombie) {
                if (entity.getEquipment().getHelmet() == null || entity.getEquipment().getHelmet().getType() == Material.AIR) {
                    entity.getEquipment().setHelmet(new ItemStack(Material.CARVED_PUMPKIN));
                    entity.getEquipment().setHelmetDropChance((float) getDrop());
                }
            }
            return PowerResult.ok(damage);
        }

        @Override
        public Power getPower() {
            return PowerPumpkin.this;
        }
    }
}
