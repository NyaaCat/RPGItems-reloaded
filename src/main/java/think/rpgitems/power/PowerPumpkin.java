package think.rpgitems.power;

import org.bukkit.ChatColor;
import org.bukkit.Material;
import org.bukkit.configuration.ConfigurationSection;
import org.bukkit.entity.LivingEntity;
import org.bukkit.entity.Player;
import org.bukkit.entity.Skeleton;
import org.bukkit.entity.Zombie;
import org.bukkit.inventory.ItemStack;
import think.rpgitems.data.Locale;
import think.rpgitems.power.types.PowerHit;

import java.util.Random;

/**
 * Power pumpkin.
 * <p>
 * When hit skeleton or zombie, will have a 1/{@link #chance} chance
 * to make them wear pumpkin head.
 * And the pumpkin will have a chance of {@link #drop} to drop when the mobs die.
 * </p>
 */
public class PowerPumpkin extends Power implements PowerHit {
    private static final Random rand = new Random();
    /**
     * Chance of triggering this power
     */
    public int chance = 20;
    /**
     * Drop chance of the pumpkin
     */
    public double drop = 0;
    /**
     * Cost of this power
     */
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
        return ChatColor.GREEN + String.format(Locale.get("power.pumpkin"), chance);
    }

    @Override
    public void hit(Player player, ItemStack stack, LivingEntity entity, double damage) {
        if (item.getHasPermission() && !player.hasPermission(item.getPermission())) return;
        if (rand.nextInt(chance) != 0) return;
        if (!item.consumeDurability(stack, consumption)) return;
        if (entity instanceof Skeleton || entity instanceof Zombie)
            if (entity.getEquipment().getHelmet() == null || entity.getEquipment().getHelmet().getType() == Material.AIR) {
                entity.getEquipment().setHelmet(new ItemStack(Material.PUMPKIN));
                entity.getEquipment().setHelmetDropChance((float) drop);
            }
    }

}
