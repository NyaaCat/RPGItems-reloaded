package think.rpgitems.power.impl;

import com.google.common.cache.Cache;
import com.google.common.cache.CacheBuilder;
import org.bukkit.*;
import org.bukkit.entity.*;
import org.bukkit.event.inventory.InventoryClickEvent;
import org.bukkit.event.player.PlayerSwapHandItemsEvent;
import org.bukkit.inventory.ItemStack;
import org.bukkit.scheduler.BukkitRunnable;
import org.bukkit.util.Vector;
import think.rpgitems.I18n;
import think.rpgitems.event.PowerActivateEvent;
import think.rpgitems.power.*;

import java.util.HashMap;
import java.util.UUID;
import java.util.concurrent.TimeUnit;

import static think.rpgitems.RPGItems.plugin;
import static think.rpgitems.power.Utils.checkCooldown;

/**
 * Power translocator.
 * <p>
 * Throw an translocator and teleport to it later
 * </p>
 */
@Meta(immutableTrigger = true, implClass = Translocator.Impl.class)
public class Translocator extends BasePower {

    public static Cache<UUID, UUID> translocatorPlayerMap = CacheBuilder.newBuilder()
            .expireAfterAccess(10, TimeUnit.MINUTES)
            .removalListener(n -> {
                UUID armorStandUUID = (UUID) n.getKey();
                Bukkit.getScheduler().runTask(plugin, () -> {
                    Entity translocator = Bukkit.getServer().getEntity(armorStandUUID);
                    if (translocator != null) {
                        translocator.remove();
                    }
                });
            }).build();
    private static final Cache<UUID, UUID> playerTranslocatorMap = CacheBuilder.newBuilder()
            .expireAfterAccess(10, TimeUnit.MINUTES)
            .removalListener(n -> {
                UUID armorStandUUID = (UUID) n.getValue();
                Bukkit.getScheduler().runTask(plugin, () -> {
                    Entity translocator = Bukkit.getServer().getEntity(armorStandUUID);
                    if (translocator != null) {
                        translocator.remove();
                    }
                });
            }).build();
    @Property
    public int cooldown = 80;
    @Property
    public int setupCost = 0;

    @Property
    public int tpCost = 0;

    @Property
    public double speed = 1;

    @Override
    public String getName() {
        return "translocator";
    }

    @Override
    public String displayText() {
        return I18n.formatDefault("power.translocator", (double) getCooldown() / 20d);
    }

    /**
     * Cooldown time of this power
     */
    public int getCooldown() {
        return cooldown;
    }

    /**
     * Cost to set up a translocator
     */
    public int getSetupCost() {
        return setupCost;
    }

    public double getSpeed() {
        return speed;
    }

    /**
     * Cost to teleport to the translocator
     */
    public int getTpCost() {
        return tpCost;
    }

    public class Impl implements PowerMainhandItem, PowerOffhandItem {
        @Override
        public PowerResult<Boolean> swapToMainhand(Player player, ItemStack stack, PlayerSwapHandItemsEvent event) {
            HashMap<String,Object> argsMap = new HashMap<>();
            argsMap.put("status","TO_MAIN_HAND");
            PowerActivateEvent powerEvent = new PowerActivateEvent(player,stack,getPower(),argsMap);
            if(!powerEvent.callEvent()) {
                return PowerResult.fail();
            }
            checkCooldown(getPower(), player, getCooldown(), false, true);
            UUID translocatorUUID = playerTranslocatorMap.getIfPresent(player.getUniqueId());
            if (translocatorUUID == null) {
                return PowerResult.fail();
            }
            playerTranslocatorMap.invalidate(player.getUniqueId());
            translocatorPlayerMap.invalidate(translocatorUUID);
            Entity translocator = Bukkit.getServer().getEntity(translocatorUUID);
            if (translocator == null) {
                return PowerResult.fail();
            }
            if (translocator.isDead() || !translocator.isValid()) {
                translocator.remove();
                return PowerResult.fail();
            }
            translocator.remove();
            if (!getItem().consumeDurability(stack, getTpCost())) return PowerResult.cost();
            Location newLoc = translocator.getLocation();
            Vector direction = player.getLocation().getDirection();
            newLoc.setDirection(direction);
            World world = newLoc.getWorld();
            player.teleport(newLoc);
            world.playEffect(newLoc, Effect.ENDER_SIGNAL, 0);
            world.playSound(newLoc, Sound.ENTITY_ENDERMAN_TELEPORT, 1.0f, 0.3f);
            return PowerResult.ok(true);
        }

        @Override
        public PowerResult<Boolean> pickupOffhand(Player player, ItemStack stack, InventoryClickEvent event) {
            HashMap<String,Object> argsMap = new HashMap<>();
            argsMap.put("status","PICK_OFF_HAND");
            PowerActivateEvent powerEvent = new PowerActivateEvent(player,stack,getPower(),argsMap);
            if(!powerEvent.callEvent()) {
                return PowerResult.fail();
            }
            checkCooldown(getPower(), player, getCooldown(), false, true);
            UUID armorStandUUID = playerTranslocatorMap.getIfPresent(player.getUniqueId());
            if (armorStandUUID == null) {
                return PowerResult.fail();
            }
            playerTranslocatorMap.invalidate(player.getUniqueId());
            translocatorPlayerMap.invalidate(armorStandUUID);
            Entity armorStand = Bukkit.getServer().getEntity(armorStandUUID);
            if (armorStand != null) {
                armorStand.remove();
                return PowerResult.ok(true);
            }
            return PowerResult.fail();
        }

        @Override
        public Power getPower() {
            return Translocator.this;
        }

        @SuppressWarnings("deprecation")
        @Override
        public PowerResult<Boolean> swapToOffhand(Player player, ItemStack stack, PlayerSwapHandItemsEvent event) {
            HashMap<String,Object> argsMap = new HashMap<>();
            argsMap.put("status","TO_OFF_HAND");
            PowerActivateEvent powerEvent = new PowerActivateEvent(player,stack,getPower(),argsMap);
            if(!powerEvent.callEvent()) {
                return PowerResult.fail();
            }
            if (!checkCooldown(getPower(), player, 0, true, true)) return PowerResult.ok(false);
            if (!getItem().consumeDurability(stack, getSetupCost())) return PowerResult.cost();
            SpectralArrow arrow = player.launchProjectile(SpectralArrow.class, player.getLocation().getDirection().multiply(getSpeed()));
            arrow.setPickupStatus(Arrow.PickupStatus.DISALLOWED);
            arrow.setPersistent(false);
            arrow.setSilent(true);
            arrow.setInvulnerable(true);
            translocatorPlayerMap.put(arrow.getUniqueId(), player.getUniqueId());
            playerTranslocatorMap.put(player.getUniqueId(), arrow.getUniqueId());
            new BukkitRunnable() {
                @SuppressWarnings("deprecation")
                @Override
                public void run() {
                    if (arrow.isDead() || !arrow.isValid()) {
                        cancel();
                        return;
                    }
                    if (arrow.isInBlock() || arrow.isOnGround()) {
                        translocatorPlayerMap.invalidate(arrow.getUniqueId());
                        playerTranslocatorMap.invalidate(player.getUniqueId());
                        Location location = arrow.getLocation();
                        arrow.remove();
                        ArmorStand armorStand = location.getWorld().spawn(location, ArmorStand.class);
                        armorStand.setCanPickupItems(false);
                        armorStand.setSmall(true);
                        armorStand.setMarker(false);
                        armorStand.setPersistent(false);
                        armorStand.setCustomName(I18n.formatDefault("message.translocator", player.getName()));
                        armorStand.setCustomNameVisible(true);
                        playerTranslocatorMap.put(player.getUniqueId(), armorStand.getUniqueId());
                        translocatorPlayerMap.put(armorStand.getUniqueId(), player.getUniqueId());
                        cancel();
                    }
                }
            }.runTaskTimer(plugin, 0L, 5L);
            return PowerResult.ok(true);
        }

        @Override
        public PowerResult<Boolean> placeOffhand(Player player, ItemStack stack, InventoryClickEvent event) {
            return PowerResult.ok(false);
        }
    }
}
