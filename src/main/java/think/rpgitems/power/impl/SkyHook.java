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
import think.rpgitems.event.PowerActivateEvent;
import think.rpgitems.power.*;
import think.rpgitems.utils.MaterialUtils;

import java.util.HashMap;
import java.util.Map;
import java.util.UUID;

import static think.rpgitems.power.Utils.checkCooldown;

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

    private static final Map<UUID, Boolean> hooking = new HashMap<>();
    @Property(order = 0)
    public Material railMaterial = Material.GLASS;
    @Property
    public int cooldown = 0;
    @Property
    public int cost = 0;
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
        s.set("cost", getCost());
        s.set("hookingTickCost", getHookingTickCost());
        s.set("cooldown", getCooldown());
        s.set("railMaterial", getRailMaterial().toString());
        s.set("hookDistance", getHookDistance());
    }

    /**
     * Cost of this power
     */
    public int getCost() {
        return cost;
    }

    /**
     * Hooking Cost Per-Tick
     */
    public int getHookingTickCost() {
        return hookingTickCost;
    }

    /**
     * Cooldown time of this power
     */
    public int getCooldown() {
        return cooldown;
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

    public class Impl implements PowerRightClick, PowerLeftClick, PowerSneak, PowerSprint, PowerPlain, PowerBowShoot {

        @Override
        public PowerResult<Void> leftClick(Player player, ItemStack stack, PlayerInteractEvent event) {
            return fire(player, stack);
        }

        @Override
        public PowerResult<Void> fire(final Player player, ItemStack stack) {
            PowerActivateEvent powerEvent = new PowerActivateEvent(player,stack,getPower());
            if(!powerEvent.callEvent()) {
                return PowerResult.fail();
            }
            if (!checkCooldown(getPower(), player, getCooldown(), true, true)) return PowerResult.cd();
            if (!getItem().consumeDurability(stack, getCost())) return PowerResult.cost();
            Boolean isHooking = hooking.get(player.getUniqueId());
            if (isHooking == null) {
                isHooking = false;
            }
            if (isHooking) {
                player.setVelocity(player.getLocation().getDirection());
                hooking.put(player.getUniqueId(), false);
                return PowerResult.noop();
            }
            Block block = player.getTargetBlock(null, getHookDistance());
            if (block.getType() != getRailMaterial()) {
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
                    if (!(player.getAllowFlight() && getItem().consumeDurability(stack, getHookingTickCost()))) {
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
                        if (location.getBlock().getType() == getRailMaterial()) {
                            delay = 20;
                        }
                        return;
                    }
                    Vector dir = location.getDirection().setY(0).normalize();
                    location.add(dir);
                    if (location.getBlock().getType() != getRailMaterial()) {
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
        public Power getPower() {
            return SkyHook.this;
        }

        @Override
        public PowerResult<Void> rightClick(Player player, ItemStack stack, PlayerInteractEvent event) {
            return fire(player, stack);
        }

        @Override
        public PowerResult<Void> sneak(Player player, ItemStack stack, PlayerToggleSneakEvent event) {
            return fire(player, stack);
        }

        @Override
        public PowerResult<Void> sprint(Player player, ItemStack stack, PlayerToggleSprintEvent event) {
            return fire(player, stack);
        }

        @Override
        public PowerResult<Float> bowShoot(Player player, ItemStack itemStack, EntityShootBowEvent e) {
            return fire(player, itemStack).with(e.getForce());
        }
    }
}
