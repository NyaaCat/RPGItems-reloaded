package think.rpgitems.power.impl;

import java.util.Random;
import org.bukkit.Material;
import org.bukkit.entity.LivingEntity;
import org.bukkit.entity.Player;
import org.bukkit.entity.Skeleton;
import org.bukkit.entity.Zombie;
import org.bukkit.event.entity.EntityDamageByEntityEvent;
import org.bukkit.inventory.EntityEquipment;
import org.bukkit.inventory.ItemStack;
import think.rpgitems.I18n;
import think.rpgitems.power.*;

/**
 * Power pumpkin.
 *
 * <p>When hit skeleton or zombie, will have a 1/{@link #chance} chance to make them wear pumpkin
 * head. And the pumpkin will have a chance of {@link #drop} to drop when the mobs die.
 */
@SuppressWarnings("WeakerAccess")
@Meta(defaultTrigger = "HIT", implClass = Pumpkin.Impl.class)
public class Pumpkin extends BasePower {
    private static final Random rand = new Random();

    @Property(order = 0)
    public int chance = 20;

    @Property(order = 1, required = true)
    public double drop = 0;

    /** Drop chance of the pumpkin */
    public double getDrop() {
        return drop;
    }

    @Override
    public String getName() {
        return "pumpkin";
    }

    @Override
    public String displayText() {
        return I18n.formatDefault("power.pumpkin", getChance());
    }

    /** Chance of triggering this power */
    public int getChance() {
        return chance;
    }

    public static class Impl implements PowerHit<Pumpkin> {

        @Override
        public PowerResult<Double> hit(
                Pumpkin power,
                Player player,
                ItemStack stack,
                LivingEntity entity,
                double damage,
                EntityDamageByEntityEvent event) {
            if (rand.nextInt(power.getChance()) != 0) return PowerResult.noop();
            if (entity instanceof Skeleton || entity instanceof Zombie) {
                EntityEquipment equipment = entity.getEquipment();
                if (equipment == null) {
                    return PowerResult.noop();
                }
                if (equipment.getHelmet() == null
                        || equipment.getHelmet().getType() == Material.AIR) {
                    equipment.setHelmet(new ItemStack(Material.CARVED_PUMPKIN));
                    equipment.setHelmetDropChance((float) power.getDrop());
                }
            }
            return PowerResult.ok(damage);
        }

        @Override
        public Class<? extends Pumpkin> getPowerClass() {
            return Pumpkin.class;
        }
    }
}
