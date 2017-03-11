package think.rpgitems.power;

import org.bukkit.Effect;
import org.bukkit.configuration.ConfigurationSection;
import org.bukkit.entity.Player;
import org.bukkit.inventory.ItemStack;
import think.rpgitems.data.Locale;
import think.rpgitems.data.RPGValue;
import think.rpgitems.power.types.PowerTick;

public class PowerParticleTick extends Power implements PowerTick {
    public String effect = "FLAME";
    public int interval = 15;
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
    public void tick(Player player, ItemStack i) {
        long cdTick;
        RPGValue value = RPGValue.get(player, item, "particle.interval");
        if (value == null) {
            cdTick = System.currentTimeMillis() / 50;
            value = new RPGValue(player, item, "particle.interval", cdTick);
        } else {
            cdTick = value.asLong();
        }
        if (cdTick <= System.currentTimeMillis() / 50) {
            if (!item.consumeDurability(i, consumption)) return;
            value.set(System.currentTimeMillis() / 50 + interval);
            if (effect.equalsIgnoreCase("SMOKE")) {
                player.getWorld().playEffect(player.getLocation().add(0, 2, 0), Effect.valueOf(effect), 4);
            } else {
                player.getWorld().playEffect(player.getLocation(), Effect.valueOf(effect), 0);
            }
        }
    }
}
