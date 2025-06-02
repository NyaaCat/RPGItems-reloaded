package think.rpgitems.power.impl;

import com.google.common.cache.Cache;
import com.google.common.cache.CacheBuilder;
import io.papermc.paper.event.entity.EntityMoveEvent;
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
import org.bukkit.event.player.PlayerMoveEvent;
import org.bukkit.event.player.PlayerTeleportEvent;
import org.bukkit.inventory.ItemStack;
import org.bukkit.potion.PotionEffect;
import org.bukkit.potion.PotionEffectType;
import think.rpgitems.I18n;
import think.rpgitems.RPGItems;
import think.rpgitems.event.PowerActivateEvent;
import think.rpgitems.power.*;
import think.rpgitems.power.trigger.BaseTriggers;
import think.rpgitems.power.trigger.Trigger;

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
@Meta(defaultTrigger = "HIT", withSelectors = true, generalInterface = PowerPlain.class, implClass = Stuck.Impl.class)
public class Stuck extends BasePower {
    private static final AtomicInteger rc = new AtomicInteger(0);
    private static Listener listener;
    private static final Cache<UUID, Long> stucked = CacheBuilder.newBuilder().expireAfterAccess(10, TimeUnit.MINUTES).concurrencyLevel(2).build();
    private static final Cache<UUID, Long> unstucked = CacheBuilder.newBuilder().expireAfterAccess(10, TimeUnit.MINUTES).concurrencyLevel(2).build();
    @Property
    public int chance = 3;
    @Property
    public int cost = 0;
    @Property
    public int costAoe = 0;
    @Property
    public int costPerEntity = 0;
    @Property
    public int range = 10;
    @Property
    public double facing = 30;
    @Property(order = 1)
    public int duration = 100;
    @Property(order = 0)
    public int cooldown = 0;
    @Property
    public boolean requireHurtByEntity = true;
    @Property
    public boolean clear = false;
    private final Random random = new Random();

    @Override
    public void init(ConfigurationSection s) {
        int orc = rc.getAndIncrement();
        boolean allowHit = s.getBoolean("allowHit", true);
        boolean allowAoe = s.getBoolean("allowAoe", false);
        Set<Trigger> triggerTypes = new HashSet<>();
        if (allowHit) {
            triggerTypes.add(BaseTriggers.HIT);
        }
        if (allowAoe) {
            triggerTypes.add(BaseTriggers.RIGHT_CLICK);
        }
        triggers = triggerTypes;
        super.init(s);
        if (orc == 0) {
            listener = new Listener() {
                @EventHandler
                void onPlayerMove(PlayerMoveEvent e) {
                    try {
                        if (stucked.get(e.getPlayer().getUniqueId(), () -> Long.MIN_VALUE) >= (System.currentTimeMillis() - getDuration() * 50)) {
                            e.setCancelled(true);
                        }
                    } catch (ExecutionException ex) {
                        ex.printStackTrace();
                    }
                }

                @EventHandler
                void onEntityMove(EntityMoveEvent e) {
                    try {
                        if (stucked.get(e.getEntity().getUniqueId(), () -> Long.MIN_VALUE) >= (System.currentTimeMillis() - getDuration() * 50)) {
                            e.setCancelled(true);
                        }
                    } catch (ExecutionException ex) {
                        ex.printStackTrace();
                    }
                }

                @EventHandler
                void onEntityTeleport(EntityTeleportEvent e) {
                    try {
                        if (stucked.get(e.getEntity().getUniqueId(), () -> Long.MIN_VALUE) >= (System.currentTimeMillis() - getDuration() * 50)) {
                            e.setCancelled(true);
                        }
                    } catch (ExecutionException ex) {
                        ex.printStackTrace();
                    }
                }

                @EventHandler
                void onPlayerTeleport(PlayerTeleportEvent e) {
                    try {
                        if (stucked.get(e.getPlayer().getUniqueId(), () -> Long.MIN_VALUE) >= (System.currentTimeMillis() - getDuration() * 50)) {
                            if (e.getCause() != PlayerTeleportEvent.TeleportCause.COMMAND) {
                                e.getPlayer().sendMessage(I18n.formatDefault("message.stuck"));
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

    /**
     * Duration of this power in tick
     */
    public int getDuration() {
        return duration;
    }

    /**
     * Cost of this power (hit)
     */
    public int getCost() {
        return cost;
    }

    /**
     * Cost of this power (right click)
     */
    public int getCostAoe() {
        return costAoe;
    }

    /**
     * Cost of this power (right click per entity)
     */
    public int getCostPerEntity() {
        return costPerEntity;
    }

    /**
     * Maximum view angle
     */
    public double getFacing() {
        return facing;
    }

    @Override
    public String getName() {
        return "stuck";
    }

    @Override
    public String displayText() {
        return I18n.formatDefault("power.stuck", (int) ((1d / (double) getChance()) * 100d), getDuration(), (double) getCooldown() / 20d);
    }

    /**
     * Chance of triggering this power
     */
    public int getChance() {
        return chance;
    }

    /**
     * Cooldown time of this power
     */
    public int getCooldown() {
        return cooldown;
    }

    @Override
    public void deinit() {
        int nrc = rc.decrementAndGet();
        if (nrc == 0) {
            HandlerList.unregisterAll(listener);
        }
    }

    /**
     * Range of this power
     */
    public int getRange() {
        return range;
    }

    /**
     * Whether to remove the effect instead of adding it.
     */
    public boolean isClear() {
        return clear;
    }

    public boolean isRequireHurtByEntity() {
        return requireHurtByEntity;
    }

    public class Impl implements PowerLeftClick, PowerRightClick, PowerPlain, PowerHit, PowerBowShoot, PowerHitTaken, PowerHurt {

        @Override
        public PowerResult<Double> takeHit(Player target, ItemStack stack, double damage, EntityDamageEvent event) {
            if (!isRequireHurtByEntity() || event instanceof EntityDamageByEntityEvent) {
                return fire(target, stack).with(damage);
            }
            return PowerResult.noop();
        }

        @Override
        public PowerResult<Void> fire(Player player, ItemStack stack) {
            PowerActivateEvent powerEvent = new PowerActivateEvent(player,stack,getPower());
            if(!powerEvent.callEvent()) {
                return PowerResult.fail();
            }
            if (!checkCooldown(getPower(), player, getCooldown(), showCooldownWarning(), true)) return PowerResult.cd();
            if (!getItem().consumeDurability(stack, getCostAoe())) return PowerResult.cost();
            if (isClear()) {
                if (stucked.getIfPresent(player.getUniqueId()) != null) {
                    stucked.invalidate(player.getUniqueId());
                    unstucked.put(player.getUniqueId(), getDuration() * 50 + System.currentTimeMillis());
                    return PowerResult.ok();
                } else {
                    player.sendMessage(I18n.formatDefault("message.not_stucked"));
                    return PowerResult.noop();
                }
            }
            List<LivingEntity> entities = getLivingEntitiesInCone(getNearestLivingEntities(getPower(), player.getEyeLocation(), player, getRange(), 0), player.getLocation().toVector(), getFacing(), player.getLocation().getDirection());
            entities.forEach(entity -> {
                        if (!getItem().consumeDurability(stack, getCostPerEntity())) return;
                        try {
                            if (unstucked.get(entity.getUniqueId(), () -> Long.MIN_VALUE) >= (System.currentTimeMillis())) return;
                        } catch (ExecutionException e) {
                            // Do nothing
                        }
                        stucked.put(entity.getUniqueId(), System.currentTimeMillis());
                    }
            );
            return PowerResult.ok();
        }

        @Override
        public Power getPower() {
            return Stuck.this;
        }

        @Override
        public PowerResult<Void> hurt(Player target, ItemStack stack, EntityDamageEvent event) {
            if (!isRequireHurtByEntity() || event instanceof EntityDamageByEntityEvent) {
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

        @Override
        public PowerResult<Double> hit(Player player, ItemStack stack, LivingEntity entity, double damage, EntityDamageByEntityEvent event) {
            HashMap<String,Object> argsMap = new HashMap<>();
            argsMap.put("target",entity);
            argsMap.put("damage",damage);
            PowerActivateEvent powerEvent = new PowerActivateEvent(player,stack,getPower(),argsMap);
            if(!powerEvent.callEvent()) {
                return PowerResult.fail();
            }
            if (!checkCooldown(getPower(), player, getCooldown(), showCooldownWarning(), true)) return PowerResult.cd();
            if (random.nextInt(getChance()) == 0) {
                if (!getItem().consumeDurability(stack, getCost())) return PowerResult.cost();
                try {
                    if (unstucked.get(entity.getUniqueId(), () -> Long.MIN_VALUE) >= (System.currentTimeMillis())) return PowerResult.noop();
                } catch (ExecutionException e) {
                    // Do nothing
                }
                stucked.put(entity.getUniqueId(), System.currentTimeMillis());
                return PowerResult.ok(damage);
            }
            return PowerResult.noop();
        }
    }
}