package think.rpgitems.power.impl;

import org.bukkit.Bukkit;
import org.bukkit.GameMode;
import org.bukkit.Location;
import org.bukkit.Material;
import org.bukkit.block.Block;
import org.bukkit.configuration.ConfigurationSection;
import org.bukkit.entity.Player;
import org.bukkit.event.entity.EntityShootBowEvent;
import org.bukkit.event.player.PlayerInteractEvent;
import org.bukkit.event.player.PlayerToggleSneakEvent;
import org.bukkit.event.player.PlayerToggleSprintEvent;
import org.bukkit.inventory.ItemStack;
import org.bukkit.scheduler.BukkitRunnable;
import org.bukkit.util.Vector;
import think.rpgitems.I18n;
import think.rpgitems.RPGItems;
import think.rpgitems.power.*;
import think.rpgitems.utils.MaterialUtils;

import java.util.HashMap;
import java.util.Map;
import java.util.UUID;

/**
 * Power skyhook.
 * <p>
 * The skyhook power will allow the user to hook on to {@link #railMaterial material}
 * up to {@link #hookDistance distance} blocks away
 * </p>
 */
@SuppressWarnings("WeakerAccess")
@Meta(defaultTrigger = "RIGHT_CLICK", generalInterface = PowerPlain.class, implClass = SkyHook.Impl.class)
public class SkyHook extends BasePower {

    private static Map<UUID, Boolean> hooking = new HashMap<>();
    @Property(order = 0)
    public Material railMaterial = Material.GLASS;
    @Property
    public int hookingTickCost = 0;
    @Deserializer(MaterialUtils.class)
    @Property(order = 1, required = true)
    public int hookDistance = 10;

    @Override
    public void init(ConfigurationSection s) {
        railMaterial = MaterialUtils.getMaterial(s.getString("railMaterial", "GLASS"), Bukkit.getConsoleSender());
    }

    @Override
    public void save(ConfigurationSection s) {
        s.set("hookingTickCost", getHookingTickCost());
        s.set("railMaterial", getRailMaterial().toString());
        s.set("hookDistance", getHookDistance());
    }

    /**
     * Hooking Cost Per-Tick
     */
    public int getHookingTickCost() {
        return hookingTickCost;
    }

    /**
     * Material that can hooks on
     */
    public Material getRailMaterial() {
        return railMaterial;
    }

    /**
     * Maximum distance.
     */
    public int getHookDistance() {
        return hookDistance;
    }

    @Override
    public String getName() {
        return "skyhook";
    }

    @Override
    public String displayText() {
        return I18n.formatDefault("power.skyhook");
    }

    public static class Impl implements PowerRightClick<SkyHook>, PowerLeftClick<SkyHook>, PowerSneak<SkyHook>, PowerSprint<SkyHook>, PowerPlain<SkyHook>, PowerBowShoot<SkyHook> {

        @Override
        public PowerResult<Void> leftClick(SkyHook power, Player player, ItemStack stack, PlayerInteractEvent event) {
            return fire(power, player, stack);
        }

        @Override
        public PowerResult<Void> fire(SkyHook power, final Player player, ItemStack stack) {
            Boolean isHooking = hooking.get(player.getUniqueId());
            if (isHooking == null) {
                isHooking = false;
            }
            if (isHooking) {
                player.setVelocity(player.getLocation().getDirection());
                hooking.put(player.getUniqueId(), false);
                return PowerResult.noop();
            }
            Block block = player.getTargetBlock(null, power.getHookDistance());
            if (block.getType() != power.getRailMaterial()) {
                player.sendMessage(I18n.formatDefault("message.skyhook.fail"));
                return PowerResult.fail();
            }
            hooking.put(player.getUniqueId(), true);
            final Location location = player.getLocation();
            player.setAllowFlight(true);
            player.setVelocity(location.getDirection().multiply(block.getLocation().distance(location) / 2d));
            player.setFlying(true);
            (new BukkitRunnable() {

                private int delay = 0;

                @Override
                public void run() {
                    if (!(player.getAllowFlight() && power.getItem().consumeDurability(stack, power.getHookingTickCost()))) {
                        cancel();
                        hooking.put(player.getUniqueId(), false);
                        return;
                    }
                    boolean isHooking = hooking.getOrDefault(player.getUniqueId(), false);
                    if (!isHooking) {
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
                        if (location.getBlock().getType() == power.getRailMaterial()) {
                            delay = 20;
                        }
                        return;
                    }
                    Vector dir = location.getDirection().setY(0).normalize();
                    location.add(dir);
                    if (location.getBlock().getType() != power.getRailMaterial()) {
                        player.setFlying(false);
                        if (player.getGameMode() != GameMode.CREATIVE)
                            player.setAllowFlight(false);
                        cancel();
                        hooking.put(player.getUniqueId(), false);
                        return;
                    }
                    player.setVelocity(dir.multiply(0.5));

                }
            }).runTaskTimer(RPGItems.plugin, 0, 0);
            return PowerResult.ok();
        }

        @Override
        public Class<? extends SkyHook> getPowerClass() {
            return SkyHook.class;
        }

        @Override
        public PowerResult<Void> rightClick(SkyHook power, Player player, ItemStack stack, PlayerInteractEvent event) {
            return fire(power, player, stack);
        }

        @Override
        public PowerResult<Void> sneak(SkyHook power, Player player, ItemStack stack, PlayerToggleSneakEvent event) {
            return fire(power, player, stack);
        }

        @Override
        public PowerResult<Void> sprint(SkyHook power, Player player, ItemStack stack, PlayerToggleSprintEvent event) {
            return fire(power, player, stack);
        }

        @Override
        public PowerResult<Float> bowShoot(SkyHook power, Player player, ItemStack itemStack, EntityShootBowEvent e) {
            return fire(power, player, itemStack).with(e.getForce());
        }
    }
}
