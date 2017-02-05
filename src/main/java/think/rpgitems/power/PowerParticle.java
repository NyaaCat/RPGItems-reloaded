package think.rpgitems.power;

import org.bukkit.Effect;
import org.bukkit.block.Block;
import org.bukkit.configuration.ConfigurationSection;
import org.bukkit.entity.Player;
import org.bukkit.inventory.ItemStack;
import think.rpgitems.data.Locale;
import think.rpgitems.power.types.PowerConsuming;
import think.rpgitems.power.types.PowerRightClick;

public class PowerParticle extends Power implements PowerRightClick, PowerConsuming {
    public String effect = "FLAME";
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
    public void rightClick(Player player, ItemStack i, Block clicked) {
        if(!item.consumeDurability(i,consumption))return;
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

    public int getConsumption(){
        return consumption;
    }

    public void setConsumption(int cost){
        consumption = cost;
    }
}
