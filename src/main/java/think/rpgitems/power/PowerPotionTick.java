package think.rpgitems.power;

import org.bukkit.ChatColor;
import org.bukkit.attribute.Attribute;
import org.bukkit.configuration.ConfigurationSection;
import org.bukkit.entity.Player;
import org.bukkit.inventory.ItemStack;
import org.bukkit.potion.PotionEffect;
import org.bukkit.potion.PotionEffectType;
import think.rpgitems.data.Locale;
import think.rpgitems.power.types.PowerTick;

import static java.lang.Double.min;

/**
 * Power potiontick.
 * <p>
 * The potiontick power will give the welder {@link #effect}
 * level {@link #amplifier} while held/worn
 * </p>
 */
public class PowerPotionTick    extends Power implements PowerTick {

    /**
     * Type of potion effect
     */
    public PotionEffectType effect = PotionEffectType.SPEED;
    /**
     * Amplifier of potion effect
     */
    public int amplifier = 1;
    /**
     * Cost of this power
     */
    public int consumption = 0;
    /**
     * Interval of this power
     */
    public int interval = 0;

    @Override
    public void tick(Player player, ItemStack stack) {
        if (!item.checkPermission(player, true))return;
        if (!checkCooldown(player, interval, false))return;
        if (!item.consumeDurability(stack, consumption)) return;
        double health = player.getHealth();
        boolean hasEffect = false;
        for (PotionEffect potionEffect : player.getActivePotionEffects()) {
            if (potionEffect.getType().equals(effect)) {
                hasEffect = true;
                if (potionEffect.getDuration() <= 5 || potionEffect.getAmplifier() < amplifier || amplifier == 0)
                    player.addPotionEffect(new PotionEffect(effect, 60, amplifier, true), true);
                break;
            }
        }
        if (!hasEffect && amplifier != 0) {
            player.addPotionEffect(new PotionEffect(effect, 60, amplifier, true), true);
        }
        if(effect.equals(PotionEffectType.HEALTH_BOOST) && health > 0) {
            health = min(health, player.getAttribute(Attribute.GENERIC_MAX_HEALTH).getValue());
            player.setHealth(health);
        }
    }

    @Override
    public void init(ConfigurationSection s) {
        amplifier = s.getInt("amplifier");
        effect = PotionEffectType.getByName(s.getString("effect", "heal"));
        consumption = s.getInt("consumption", 0);
        interval = s.getInt("interval", 0);
    }

    @Override
    public void save(ConfigurationSection s) {
        s.set("amplifier", amplifier);
        s.set("effect", effect.getName());
        s.set("consumption", consumption);
        s.set("interval", interval);
    }

    @Override
    public String getName() {
        return "potiontick";
    }

    @Override
    public String displayText() {
        return ChatColor.GREEN + String.format(Locale.get("power.potiontick"), effect.getName().toLowerCase().replaceAll("_", " "), amplifier + 1);
    }
}
