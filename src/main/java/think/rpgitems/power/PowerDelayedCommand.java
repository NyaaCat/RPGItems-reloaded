package think.rpgitems.power;

import org.bukkit.block.Block;
import org.bukkit.configuration.ConfigurationSection;
import org.bukkit.entity.Player;
import org.bukkit.inventory.ItemStack;
import org.bukkit.scheduler.BukkitRunnable;
import think.rpgitems.Plugin;

public class PowerDelayedCommand extends PowerCommand {
    public int delay = 20;

    @Override
    public String getName() {
        return "delayedcommand";
    }

    @Override
    public void init(ConfigurationSection s) {
        delay = s.getInt("delay", 20);
        super.init(s);
    }

    @Override
    public void save(ConfigurationSection s) {
        s.set("delay", delay);
        super.save(s);
    }

    @Override
    public void rightClick(final Player player, ItemStack i, Block clicked) {
        if (item.getHasPermission() && !player.hasPermission(item.getPermission())) return;
        if (!isRight || !updateCooldown(player)) return;
        if (!item.consumeDurability(i, consumption)) return;
        (new BukkitRunnable() {
            @Override
            public void run() {
                executeCommand(player);
            }
        }).runTaskLater(Plugin.plugin, delay);
    }

    @Override
    public void leftClick(final Player player, ItemStack i, Block clicked) {
        if (item.getHasPermission() && !player.hasPermission(item.getPermission())) return;
        if (isRight || !updateCooldown(player)) return;
        if (!item.consumeDurability(i, consumption)) return;
        (new BukkitRunnable() {
            @Override
            public void run() {
                executeCommand(player);
            }
        }).runTaskLater(Plugin.plugin, delay);
    }
}
