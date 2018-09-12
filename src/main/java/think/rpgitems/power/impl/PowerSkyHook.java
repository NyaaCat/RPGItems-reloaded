package think.rpgitems.power.impl;

import org.bukkit.Bukkit;
import org.bukkit.GameMode;
import org.bukkit.Location;
import org.bukkit.Material;
import org.bukkit.block.Block;
import org.bukkit.configuration.ConfigurationSection;
import org.bukkit.entity.Player;
import org.bukkit.event.player.PlayerInteractEvent;
import org.bukkit.inventory.ItemStack;
import org.bukkit.scheduler.BukkitRunnable;
import org.bukkit.util.Vector;
import think.rpgitems.I18n;
import think.rpgitems.RPGItems;
import think.rpgitems.data.RPGValue;
import think.rpgitems.power.*;
import think.rpgitems.utils.MaterialUtils;

import static think.rpgitems.power.Utils.checkCooldown;

/**
 * Power skyhook.
 * <p>
 * The skyhook power will allow the user to hook on to {@link #railMaterial material}
 * up to {@link #hookDistance distance} blocks away
 * </p>
 */
@SuppressWarnings("WeakerAccess")
@PowerMeta(immutableTrigger = true)
public class PowerSkyHook extends BasePower implements PowerRightClick {

    /**
     * Material that can hooks on
     */
    @Property(order = 0)
    public Material railMaterial = Material.GLASS;
    /**
     * Cooldown time of this power
     */
    public long cooldown = 20;
    /**
     * Cost of this power
     */
    public int cost = 0;
    /**
     * Hooking Cost Pre-Tick
     */
    public int hookingTickCost = 0;
    /**
     * Maximum distance.
     */
    @Deserializer(MaterialUtils.class)
    @Property(order = 1, required = true)
    public int hookDistance = 10;

    @Override
    public PowerResult<Void> rightClick(final Player player, ItemStack stack, Block clicked, PlayerInteractEvent event) {
        if (!checkCooldown(this, player, cooldown, true)) return PowerResult.cd();
        if (!getItem().consumeDurability(stack, cost)) return PowerResult.cost();
        RPGValue isHooking = RPGValue.get(player, getItem(), "skyhook.isHooking");
        if (isHooking == null) {
            isHooking = new RPGValue(player, getItem(), "skyhook.isHooking", false);
        }
        if (isHooking.asBoolean()) {
            player.setVelocity(player.getLocation().getDirection());
            isHooking.set(false);
            return PowerResult.noop();
        }
        Block block = player.getTargetBlock(null, hookDistance);
        if (block.getType() != railMaterial) {
            player.sendMessage(I18n.format("message.skyhook.fail"));
            return PowerResult.fail();
        }
        isHooking.set(true);
        final Location location = player.getLocation();
        player.setAllowFlight(true);
        player.setVelocity(location.getDirection().multiply(block.getLocation().distance(location) / 2d));
        player.setFlying(true);
        (new BukkitRunnable() {

            private int delay = 0;

            @Override
            public void run() {
                if (!(player.getAllowFlight() && getItem().consumeDurability(stack, hookingTickCost))) {
                    cancel();
                    RPGValue.get(player, getItem(), "skyhook.isHooking").set(false);
                    return;
                }
                if (!RPGValue.get(player, getItem(), "skyhook.isHooking").asBoolean()) {
                    player.setFlying(false);
                    if (player.getGameMode() != GameMode.CREATIVE)
                        player.setAllowFlight(false);
                    cancel();
                    return;
                }
                player.setFlying(true);
                player.getLocation(location);
                location.add(0, 2.4, 0);
                if (delay < 20) {
                    delay++;
                    if (location.getBlock().getType() == railMaterial) {
                        delay = 20;
                    }
                    return;
                }
                Vector dir = location.getDirection().setY(0).normalize();
                location.add(dir);
                if (location.getBlock().getType() != railMaterial) {
                    player.setFlying(false);
                    if (player.getGameMode() != GameMode.CREATIVE)
                        player.setAllowFlight(false);
                    cancel();
                    RPGValue.get(player, PowerSkyHook.this.getItem(), "skyhook.isHooking").set(false);
                    return;
                }
                player.setVelocity(dir.multiply(0.5));

            }
        }).runTaskTimer(RPGItems.plugin, 0, 0);
        return PowerResult.ok();
    }

    @Override
    public void init(ConfigurationSection s) {
        railMaterial = MaterialUtils.getMaterial(s.getString("railMaterial", "GLASS"), Bukkit.getConsoleSender());
    }

    @Override
    public void save(ConfigurationSection s) {
        s.set("cost", cost);
        s.set("hookingTickCost", hookingTickCost);
        s.set("cooldown", cooldown);
        s.set("railMaterial", railMaterial.toString());
        s.set("hookDistance", hookDistance);
    }

    @Override
    public String getName() {
        return "skyhook";
    }

    @Override
    public String displayText() {
        return I18n.format("power.skyhook");
    }

}
