package think.rpgitems.power;

import org.bukkit.ChatColor;
import org.bukkit.configuration.ConfigurationSection;
import org.bukkit.entity.Player;
import org.bukkit.inventory.ItemStack;
import org.bukkit.potion.PotionEffect;
import org.bukkit.potion.PotionEffectType;
import think.rpgitems.data.Locale;
import think.rpgitems.power.types.PowerTick;

/**
 * Power potiontick.
 * <p>
 * The potiontick power will give the welder {@link #effect}
 * level {@link #amplifier} while held/worn
 * </p>
 */
public class PowerPotionTick extends Power implements PowerTick {

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

    @Override
    public void tick(Player player, ItemStack stack) {
        if (item.getHasPermission() && !player.hasPermission(item.getPermission())) {
            player.sendMessage(ChatColor.RED + Locale.get("message.error.permission"));
        } else {
            if (!item.consumeDurability(stack, consumption)) return;
            boolean hasEffect = false;
            for (PotionEffect potionEffect : player.getActivePotionEffects()) {
                if (potionEffect.getType().equals(effect)) {
                    hasEffect = true;
                    if (potionEffect.getDuration() <= 5 || potionEffect.getAmplifier() < amplifier)
                        player.addPotionEffect(new PotionEffect(effect, 60, amplifier, true), true);
                    break;
                }
            }
            if (!hasEffect) {
                player.addPotionEffect(new PotionEffect(effect, 60, amplifier, true), true);
            }
        }

    }

    @Override
    public void init(ConfigurationSection s) {
        amplifier = s.getInt("amplifier");
        effect = PotionEffectType.getByName(s.getString("effect", "heal"));
        consumption = s.getInt("consumption", 0);
    }

    @Override
    public void save(ConfigurationSection s) {
        s.set("amplifier", amplifier);
        s.set("effect", effect.getName());
        s.set("consumption", consumption);
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
