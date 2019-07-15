package think.rpgitems.power.impl;

import com.google.common.cache.Cache;
import com.google.common.cache.CacheBuilder;
import org.bukkit.Bukkit;
import org.bukkit.configuration.ConfigurationSection;
import org.bukkit.entity.LivingEntity;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.HandlerList;
import org.bukkit.event.Listener;
import org.bukkit.event.entity.EntityDamageByEntityEvent;
import org.bukkit.event.entity.EntityDamageEvent;
import org.bukkit.event.entity.EntityShootBowEvent;
import org.bukkit.event.entity.EntityTeleportEvent;
import org.bukkit.event.player.PlayerInteractEvent;
import org.bukkit.event.player.PlayerTeleportEvent;
import org.bukkit.inventory.ItemStack;
import org.bukkit.potion.PotionEffect;
import org.bukkit.potion.PotionEffectType;
import think.rpgitems.I18n;
import think.rpgitems.RPGItems;
import think.rpgitems.power.*;

import java.util.*;
import java.util.concurrent.ExecutionException;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicInteger;

import static think.rpgitems.power.Utils.*;

/**
 * Power Stuck.
 * <p>
 * The stuck power will make the hit target stuck with a chance of 1/{@link #chance}.
 * </p>
 */
@PowerMeta(defaultTrigger = "HIT", withSelectors = true, generalInterface = PowerPlain.class)
public class PowerStuck extends BasePower implements PowerLeftClick, PowerRightClick, PowerPlain, PowerHit, PowerBowShoot, PowerHitTaken, PowerHurt {
    /**
     * Chance of triggering this power
     */
    @Property
    public int chance = 3;

    /**
     * Cost of this power (hit)
     */
    @Property
    public int cost = 0;

    /**
     * Cost of this power (right click)
     */
    @Property
    public int costAoe = 0;

    /**
     * Cost of this power (right click per entity)
     */
    @Property
    public int costPerEntity = 0;

    /**
     * Range of this power
     */
    @Property
    public int range = 10;

    /**
     * Maximum view angle
     */
    @Property
    public double facing = 30;

    /**
     * Duration of this power in tick
     */
    @Property(order = 1)
    public int duration = 100;

    /**
     * Cooldown time of this power
     */
    @Property(order = 0)
    public long cooldown = 0;

    @Property
    public boolean requireHurtByEntity = true;

    @Override
    public PowerResult<Double> takeHit(Player target, ItemStack stack, double damage, EntityDamageEvent event) {
        if (!requireHurtByEntity || event instanceof EntityDamageByEntityEvent) {
            return fire(target, stack).with(damage);
        }
        return PowerResult.noop();
    }

    @Override
    public PowerResult<Void> hurt(Player target, ItemStack stack, EntityDamageEvent event) {
        if (!requireHurtByEntity || event instanceof EntityDamageByEntityEvent) {
            return fire(target, stack);
        }
        return PowerResult.noop();
    }

    @Override
    public PowerResult<Void> leftClick(Player player, ItemStack stack, PlayerInteractEvent event) {
        return fire(player, stack);
    }

    @Override
    public PowerResult<Void> rightClick(Player player, ItemStack stack, PlayerInteractEvent event) {
        return fire(player, stack);
    }

    @Override
    public PowerResult<Float> bowShoot(Player player, ItemStack stack, EntityShootBowEvent event) {
        return fire(player, stack).with(event.getForce());
    }

    private static AtomicInteger rc = new AtomicInteger(0);

    private Random random = new Random();

    private static Listener listener;

    private static Cache<UUID, Long> stucked = CacheBuilder.newBuilder().expireAfterAccess(10, TimeUnit.MINUTES).concurrencyLevel(2).build();

    @Override
    public PowerResult<Double> hit(Player player, ItemStack stack, LivingEntity entity, double damage, EntityDamageByEntityEvent event) {
        if (!checkCooldown(this, player, cooldown, true, true)) return PowerResult.cd();
        if (random.nextInt(chance) == 0) {
            if (!getItem().consumeDurability(stack, cost)) return PowerResult.cost();
            stucked.put(entity.getUniqueId(), System.currentTimeMillis());
            entity.addPotionEffect(new PotionEffect(PotionEffectType.SLOW, duration, 10), true);
//            entity.addPotionEffect(new PotionEffect(PotionEffectType.JUMP, duration, 128), true);
            //todo change implementation to lock entity mobilability
            return PowerResult.ok(damage);
        }
        return PowerResult.noop();
    }

    @Override
    public PowerResult<Void> fire(Player player, ItemStack stack) {
        if (!checkCooldown(this, player, cooldown, true, true)) return PowerResult.cd();
        if (!getItem().consumeDurability(stack, costAoe)) return PowerResult.cost();
        List<LivingEntity> entities = getLivingEntitiesInCone(getNearestLivingEntities(this, player.getEyeLocation(), player, range, 0), player.getLocation().toVector(), facing, player.getLocation().getDirection());
        entities.forEach(entity -> {
                    if (!getItem().consumeDurability(stack, costPerEntity)) return;
                    stucked.put(entity.getUniqueId(), System.currentTimeMillis());
                    entity.addPotionEffect(new PotionEffect(PotionEffectType.SLOW, duration, 10), true);
//                    entity.addPotionEffect(new PotionEffect(PotionEffectType.JUMP, duration, 128), true);
                }
        );
        return PowerResult.ok();
    }

    @Override
    public String displayText() {
        return I18n.format("power.stuck", (int) ((1d / (double) chance) * 100d), duration, (double) cooldown / 20d);
    }

    @Override
    public String getName() {
        return "stuck";
    }

    @Override
    public void init(ConfigurationSection s) {
        int orc = rc.getAndIncrement();
        boolean allowHit = s.getBoolean("allowHit", true);
        boolean allowAoe = s.getBoolean("allowAoe", false);
        Set<Trigger> triggerTypes = new HashSet<>();
        if (allowHit) {
            triggerTypes.add(Trigger.HIT);
        }
        if (allowAoe) {
            triggerTypes.add(Trigger.RIGHT_CLICK);
        }
        triggers = triggerTypes;
        super.init(s);
        if (orc == 0) {
            listener = new Listener() {
                @EventHandler
                void onEntityTeleport(EntityTeleportEvent e) {
                    try {
                        if (stucked.get(e.getEntity().getUniqueId(), () -> Long.MIN_VALUE) >= (System.currentTimeMillis() - duration * 50)) {
                            e.setCancelled(true);
                        }
                    } catch (ExecutionException ex) {
                        ex.printStackTrace();
                    }
                }

                @EventHandler
                void onPlayerTeleport(PlayerTeleportEvent e) {
                    try {
                        if (stucked.get(e.getPlayer().getUniqueId(), () -> Long.MIN_VALUE) >= (System.currentTimeMillis() - duration * 50)) {
                            if (e.getCause() != PlayerTeleportEvent.TeleportCause.COMMAND) {
                                e.getPlayer().sendMessage(I18n.format("message.stuck"));
                                e.setCancelled(true);
                            }
                        }
                    } catch (ExecutionException ex) {
                        ex.printStackTrace();
                    }
                }
            };
            Bukkit.getPluginManager().registerEvents(listener, RPGItems.plugin);
        }
    }

    @Override
    public void deinit() {
        int nrc = rc.decrementAndGet();
        if (nrc == 0) {
            HandlerList.unregisterAll(listener);
        }
    }
}