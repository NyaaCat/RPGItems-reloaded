package think.rpgitems.power;

import org.bukkit.Bukkit;
import org.bukkit.ChatColor;
import org.bukkit.Location;
import org.bukkit.configuration.ConfigurationSection;
import org.bukkit.entity.LivingEntity;
import org.bukkit.entity.Player;
import think.rpgitems.data.Locale;
import think.rpgitems.power.types.PowerHit;

import java.util.Random;

public class PowerDeathCommand extends Power implements PowerHit {
    public static final String name = "deathcommand";

    public String command = "";
    public int chance = 20;
    public String desc = "";
    public int count = 1;
    private static final Random rand = new Random();

    @Override
    public void init(ConfigurationSection s) {
        command = s.getString("command");
        chance = s.getInt("chance");
        desc = s.getString("desc");
        count = s.getInt("count");
    }

    @Override
    public void save(ConfigurationSection s) {
        s.set("command", command);
        s.set("chance", chance);
        s.set("desc", desc);
        s.set("count", count);
    }

    @Override
    public String getName() {
        return name;
    }

    @Override
    public String displayText() {
        return ChatColor.GREEN + String.format(Locale.get("power.deathcommand"), chance, desc.equals("") ? "execute some command" : desc);
    }

    @Override
    public void hit(Player player, LivingEntity e, double damage) {
        if (item.getHasPermission() && !player.hasPermission(item.getPermission())) return;
        if (e.spigot().isInvulnerable()) {
            return;
        }
        if (rand.nextInt(chance) == 0) {
            Location loc = e.getLocation();
            int x = (int) loc.getX();
            int y = (int) loc.getY();
            int z = (int) loc.getZ();
            e.setHealth(0);
            String cmd = command.replace("${x}", String.valueOf(x)).replace("${y}", String.valueOf(y)).replace("${z}", String.valueOf(z));
            for (int i = 0; i < count; i++) Bukkit.dispatchCommand(Bukkit.getConsoleSender(), cmd);
        }
    }
}
