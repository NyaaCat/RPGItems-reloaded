package think.rpgitems.power;

import org.bukkit.Effect;
import org.bukkit.block.Block;
import org.bukkit.configuration.ConfigurationSection;
import org.bukkit.entity.Player;
import org.bukkit.inventory.ItemStack;
import think.rpgitems.data.Locale;
import think.rpgitems.power.types.PowerRightClick;

/**
 * Power particle.
 * <p>
 * When right clicked, spawn some particles around the user.
 * </p>
 */
public class PowerParticle extends Power implements PowerRightClick {
    /**
     * Name of particle effect
     */
    public String effect = "FLAME";
    /**
     * Cost of this power
     */
    public int consumption = 0;

    @Override
    public void init(ConfigurationSection s) {
        effect = s.getString("effect", "FLAME");
        consumption = s.getInt("consumption", 0);
    }

    @Override
    public void save(ConfigurationSection s) {
        s.set("effect", effect);
        s.set("consumption", consumption);
    }

    @Override
    public String getName() {
        return "particle";
    }

    @Override
    public String displayText() {
        return Locale.get("power.particle");
    }

    @Override
    public void rightClick(Player player, ItemStack item, Block clicked) {
        if (!this.item.consumeDurability(item, consumption)) return;
        if (effect.equalsIgnoreCase("SMOKE")) {
            player.getWorld().playEffect(player.getLocation().add(0, 2, 0), Effect.valueOf(effect), 4);
        } else {
            player.getWorld().playEffect(player.getLocation(), Effect.valueOf(effect), 0);
        }
    }

    /**
     * Acceptable effect boolean.
     *
     * @param effect the effect
     * @return the boolean
     */
    public static boolean acceptableEffect(String effect) {
        try {
            Effect eff = Effect.valueOf(effect.toUpperCase());
            return eff.getType() == Effect.Type.VISUAL || eff.getType() == Effect.Type.PARTICLE;
        } catch (IllegalArgumentException ex) {
            return false;
        }
    }

}
