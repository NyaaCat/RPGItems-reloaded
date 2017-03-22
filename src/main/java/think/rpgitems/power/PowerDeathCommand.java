package think.rpgitems.power;

import org.bukkit.Bukkit;
import org.bukkit.ChatColor;
import org.bukkit.Location;
import org.bukkit.configuration.ConfigurationSection;
import org.bukkit.entity.LivingEntity;
import org.bukkit.entity.Player;
import org.bukkit.inventory.ItemStack;
import think.rpgitems.data.Locale;
import think.rpgitems.power.types.PowerHit;

import java.util.Random;

/**
 * Power deathcommand.
 * <p>
 * With a 1/{@link #chance chance} to kill the target then execute the {@link #command}
 * for {@link #count} times. {@link #desc Description} will be displayed in item lore.
 * `${x}` `${y}` and `${z}` in the command will be replaced with the death location of the enemy.
 * </p>
 */
public class PowerDeathCommand extends Power implements PowerHit {

    /**
     * Command to be executed
     */
    public String command = "";
    /**
     * Chance of triggering this power
     */
    public int chance = 20;
    /**
     * Description in display text
     */
    public String desc = "";
    /**
     * Times to run the {@link #command}
     */
    public int count = 1;
    /**
     * Cost of this power
     */
    public int consumption = 0;

    private static final Random rand = new Random();

    @Override
    public void init(ConfigurationSection s) {
        command = s.getString("command");
        chance = s.getInt("chance");
        desc = s.getString("desc");
        count = s.getInt("count");
        consumption = s.getInt("consumption", 0);
    }

    @Override
    public void save(ConfigurationSection s) {
        s.set("command", command);
        s.set("chance", chance);
        s.set("desc", desc);
        s.set("count", count);
        s.set("consumption", consumption);
    }

    @Override
    public String getName() {
        return "deathcommand";
    }

    @Override
    public String displayText() {
        return ChatColor.GREEN + String.format(Locale.get("power.deathcommand"), chance, desc.equals("") ? "execute some command" : desc);
    }

    @Override
    public void hit(Player player, ItemStack item, LivingEntity entity, double damage) {
        if (this.item.getHasPermission() && !player.hasPermission(this.item.getPermission())) return;
        if (rand.nextInt(chance) == 0) {
            if (!this.item.consumeDurability(item, consumption)) return;
            Location loc = entity.getLocation();
            int x = (int) loc.getX();
            int y = (int) loc.getY();
            int z = (int) loc.getZ();
            entity.setHealth(0);
            String cmd = command.replace("${x}", String.valueOf(x)).replace("${y}", String.valueOf(y)).replace("${z}", String.valueOf(z));
            for (int i = 0; i < count; i++) Bukkit.dispatchCommand(Bukkit.getConsoleSender(), cmd);
        }
    }

}
