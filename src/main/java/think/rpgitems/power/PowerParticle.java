package think.rpgitems.power;

import org.bukkit.Effect;
import org.bukkit.block.Block;
import org.bukkit.configuration.ConfigurationSection;
import org.bukkit.entity.Player;
import org.bukkit.inventory.ItemStack;
import think.rpgitems.I18n;
import think.rpgitems.RPGItems;
import think.rpgitems.commands.Property;
import think.rpgitems.commands.Validator;
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
    @Property(order = 0, required = true)
    @Validator(value = "acceptableEffect", message = "message.error.visualeffect")
    public String effect = Effect.MOBSPAWNER_FLAMES.name();
    /**
     * Cost of this power
     */
    @Property
    public int consumption = 0;

    /**
     * Acceptable effect boolean.
     *
     * @param effect the effect
     * @return the boolean
     */
    public boolean acceptableEffect(String effect) {
        try {
            Effect eff = Effect.valueOf(effect.toUpperCase());
            return eff.getType() == Effect.Type.VISUAL;
        } catch (IllegalArgumentException ex) {
            return false;
        }
    }

    @Override
    public void init(ConfigurationSection s) {
        effect = s.getString("effect", Effect.MOBSPAWNER_FLAMES.name());
        try {
            Effect.valueOf(effect);
        } catch (IllegalArgumentException e) {
            RPGItems.logger.warning("Invalid effect name: " + effect);
            effect = Effect.MOBSPAWNER_FLAMES.name();
        }
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
        return I18n.format("power.particle");
    }

    @Override
    public void rightClick(Player player, ItemStack stack, Block clicked) {
        if (!item.consumeDurability(stack, consumption)) return;
        if (effect.equalsIgnoreCase("SMOKE")) {
            player.getWorld().playEffect(player.getLocation().add(0, 2, 0), Effect.valueOf(effect), 4);
        } else {
            player.getWorld().playEffect(player.getLocation(), Effect.valueOf(effect), 0);
        }
    }

}
