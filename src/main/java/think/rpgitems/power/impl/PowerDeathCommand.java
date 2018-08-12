package think.rpgitems.power.impl;

import org.bukkit.Bukkit;
import org.bukkit.Location;
import org.bukkit.entity.LivingEntity;
import org.bukkit.entity.Player;
import org.bukkit.event.entity.EntityDamageByEntityEvent;
import org.bukkit.inventory.ItemStack;
import think.rpgitems.I18n;
import think.rpgitems.commands.Property;
import think.rpgitems.power.PowerHit;

import java.util.Random;

/**
 * Power deathcommand.
 * <p>
 * With a 1/{@link #chance chance} to kill the target then execute the {@link #command}
 * for {@link #count} times. {@link #desc Description} will be displayed in item lore.
 * `${x}` `${y}` and `${z}` in the command will be replaced with the death location of the enemy.
 * </p>
 */
@SuppressWarnings("WeakerAccess")
public class PowerDeathCommand extends BasePower implements PowerHit {

    private static final Random rand = new Random();
    /**
     * Command to be executed
     */
    @Property(order = 1, required = true)
    public String command = "";
    /**
     * Chance of triggering this power
     */
    @Property(order = 0)
    public int chance = 20;
    /**
     * Description in display text
     */
    @Property(order = 3)
    public String desc = "";
    /**
     * Times to run the {@link #command}
     */
    @Property(order = 2)
    public int count = 1;
    /**
     * Cost of this power
     */
    @Property
    public int consumption = 0;

    @Override
    public String getName() {
        return "deathcommand";
    }

    @Override
    public String displayText() {
        return I18n.format("power.deathcommand", chance, desc.equals("") ? "execute some command" : desc);
    }

    @Override
    public void hit(Player player, ItemStack stack, LivingEntity entity, double damage, EntityDamageByEntityEvent event) {
        if (rand.nextInt(chance) == 0) {
            if (!getItem().consumeDurability(stack, consumption)) return;
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
