package think.rpgitems.power;

import org.bukkit.Effect;
import org.bukkit.configuration.ConfigurationSection;
import org.bukkit.entity.Player;
import org.bukkit.inventory.ItemStack;
import think.rpgitems.data.Locale;
import think.rpgitems.data.RPGValue;
import think.rpgitems.power.types.PowerTick;

/**
 * Power particletick.
 * <p>
 * When item held in hand, spawn some particles around the user.
 * With the time {@link #interval} given in ticks.
 * </p>
 */
public class PowerParticleTick extends Power implements PowerTick {
    /**
     * Name of particle effect
     */
    public String effect = "FLAME";
    /**
     * Interval of particle effect
     */
    public int interval = 15;
    /**
     * Cost of this power
     */
    public int consumption = 0;

    @Override
    public void init(ConfigurationSection s) {
        effect = s.getString("effect", "FLAME");
        interval = s.getInt("interval");
        consumption = s.getInt("consumption", 0);
    }

    @Override
    public void save(ConfigurationSection s) {
        s.set("effect", effect);
        s.set("interval", interval);
        s.set("consumption", consumption);
    }

    @Override
    public String getName() {
        return "particletick";
    }

    @Override
    public String displayText() {
        return Locale.get("power.particletick");
    }

    @Override
    public void tick(Player player, ItemStack stack) {
        if (!checkCooldown(player, interval, false))return;
        if (!item.consumeDurability(stack, consumption)) return;
        if (effect.equalsIgnoreCase("SMOKE")) {
            player.getWorld().playEffect(player.getLocation().add(0, 2, 0), Effect.valueOf(effect), 4);
        } else {
            player.getWorld().playEffect(player.getLocation(), Effect.valueOf(effect), 0);
        }
    }
}
