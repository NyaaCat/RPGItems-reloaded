package think.rpgitems.power.impl;

import org.bukkit.Material;
import org.bukkit.configuration.ConfigurationSection;
import org.bukkit.entity.LivingEntity;
import org.bukkit.entity.Player;
import org.bukkit.entity.Skeleton;
import org.bukkit.entity.Zombie;
import org.bukkit.event.entity.EntityDamageByEntityEvent;
import org.bukkit.inventory.ItemStack;
import think.rpgitems.I18n;
import think.rpgitems.commands.Property;
import think.rpgitems.power.PowerHit;

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
public class PowerPumpkin extends BasePower implements PowerHit {
    private static final Random rand = new Random();
    /**
     * Chance of triggering this power
     */
    @Property(order = 0)
    public int chance = 20;
    /**
     * Drop chance of the pumpkin
     */
    @Property(order = 1, required = true)
    public double drop = 0;
    /**
     * Cost of this power
     */
    @Property
    public int consumption = 0;

    @Override
    public void init(ConfigurationSection s) {
        chance = s.getInt("chance");
        drop = s.getDouble("drop");
        consumption = s.getInt("consumption", 0);
    }

    @Override
    public void save(ConfigurationSection s) {
        s.set("chance", chance);
        s.set("drop", drop);
        s.set("consumption", consumption);
    }

    @Override
    public String getName() {
        return "pumpkin";
    }

    @Override
    public String displayText() {
        return I18n.format("power.pumpkin", chance);
    }

    @Override
    public void hit(Player player, ItemStack stack, LivingEntity entity, double damage, EntityDamageByEntityEvent event) {
        if (!getItem().consumeDurability(stack, consumption)) return;
        if (rand.nextInt(chance) != 0) return;
        if (entity instanceof Skeleton || entity instanceof Zombie)
            if (entity.getEquipment().getHelmet() == null || entity.getEquipment().getHelmet().getType() == Material.AIR) {
                entity.getEquipment().setHelmet(new ItemStack(Material.PUMPKIN));
                entity.getEquipment().setHelmetDropChance((float) drop);
            }
    }

}
