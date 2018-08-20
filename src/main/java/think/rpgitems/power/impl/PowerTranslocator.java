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
import think.rpgitems.commands.Property;
import think.rpgitems.power.PowerResult;
import think.rpgitems.power.PowerSwapToMainhand;
import think.rpgitems.power.PowerSwapToOffhand;

import java.util.UUID;
import java.util.concurrent.TimeUnit;

import static think.rpgitems.RPGItems.plugin;
import static think.rpgitems.utils.PowerUtils.checkCooldown;

/**
 * Power translocator.
 * <p>
 *
 * </p>
 */
public class PowerTranslocator extends BasePower implements PowerSwapToOffhand, PowerSwapToMainhand {

    private static Cache<UUID, UUID> playerTranslocatorMap = CacheBuilder.newBuilder().expireAfterAccess(10, TimeUnit.MINUTES).build();
    public static Cache<UUID, UUID> translocatorPlayerMap = CacheBuilder.newBuilder().expireAfterAccess(10, TimeUnit.MINUTES).removalListener(n -> {
        UUID armorStandUUID = (UUID) n.getKey();
        Bukkit.getScheduler().runTask(plugin, () -> {
            Entity translocator = Bukkit.getServer().getEntity(armorStandUUID);
            if (translocator != null) {
                translocator.remove();
            }
        });
    }).build();

    /**
     * Cooldown time of this power
     */
    @Property
    public long cooldown = 80;
    /**
     * Cost of this power
     */
    @Property
    public int setupCost = 0;

    /**
     * Cost of this power
     */
    @Property
    public int tpCost = 0;

    @Override
    public PowerResult<Boolean> swapToMainhand(Player player, ItemStack stack, PlayerSwapHandItemsEvent event) {
        checkCooldown(this, player, cooldown, false);
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
        if (!getItem().consumeDurability(stack, tpCost)) return PowerResult.cost();
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
        checkCooldown(this, player, cooldown, false);
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

    @SuppressWarnings("deprecation")
    @Override
    public PowerResult<Boolean> swapToOffhand(Player player, ItemStack stack, PlayerSwapHandItemsEvent event) {
        if (!checkCooldown(this, player, 0, true)) return PowerResult.ok(false);
        if (!getItem().consumeDurability(stack, setupCost)) return PowerResult.cost();

        SpectralArrow arrow = player.launchProjectile(SpectralArrow.class);
        arrow.setPickupStatus(Arrow.PickupStatus.DISALLOWED);
        arrow.setPersistent(false);
        arrow.setBounce(true);
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
                    armorStand.setCustomName(I18n.format("message.translocator", player.getName()));
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

    @Override
    public String getName() {
        return "translocator";
    }

    @Override
    public String displayText() {
        return I18n.format("power.translocator", (double) cooldown / 20d);
    }
}
