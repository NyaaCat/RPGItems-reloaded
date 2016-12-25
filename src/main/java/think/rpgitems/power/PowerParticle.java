package think.rpgitems.power;

import org.bukkit.Effect;
import org.bukkit.block.Block;
import org.bukkit.configuration.ConfigurationSection;
import org.bukkit.entity.Player;
import think.rpgitems.data.Locale;
import think.rpgitems.power.types.PowerRightClick;

public class PowerParticle extends Power implements PowerRightClick {
    public String effect = "FLAME";

    @Override
    public void init(ConfigurationSection s) {
        effect = s.getString("effect", "FLAME");
    }

    @Override
    public void save(ConfigurationSection s) {
        s.set("effect", effect);
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
    public void rightClick(Player player, Block clicked) {
        if (effect.equalsIgnoreCase("SMOKE")) {
            player.getWorld().playEffect(player.getLocation().add(0,2,0), Effect.valueOf(effect), 4);
        } else {
            player.getWorld().playEffect(player.getLocation(), Effect.valueOf(effect), 0);
        }
    }

    public static boolean acceptableEffect(String effect) {
        try {
            Effect eff = Effect.valueOf(effect.toUpperCase());
            return eff.getType() == Effect.Type.VISUAL || eff.getType() == Effect.Type.PARTICLE;
        } catch (IllegalArgumentException ex) {
            return false;
        }
    }
}
