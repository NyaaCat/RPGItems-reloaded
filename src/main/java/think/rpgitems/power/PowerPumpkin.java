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

public class PowerPumpkin extends Power implements PowerHit {
    public static final String name ="pumpkin";
    public int chance = 20;
    public double drop = 0;
    public int consumption = 0;

    private static final Random rand = new Random();

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
        return name;
    }

    @Override
    public String displayText() {
        return ChatColor.GREEN + String.format(Locale.get("power.pumpkin"), chance);
    }

    @Override
    public void hit(Player player, ItemStack i, LivingEntity e, double damage) {
        if (item.getHasPermission() && !player.hasPermission(item.getPermission())) return;
        if (rand.nextInt(chance) != 0) return;
        if(!item.consumeDurability(i,consumption))return;
        if (e instanceof Skeleton || e instanceof Zombie)
        if (e.getEquipment().getHelmet() == null || e.getEquipment().getHelmet().getType() == Material.AIR) {
            e.getEquipment().setHelmet(new ItemStack(Material.PUMPKIN));
            e.getEquipment().setHelmetDropChance((float)drop);
        }
    }

}
