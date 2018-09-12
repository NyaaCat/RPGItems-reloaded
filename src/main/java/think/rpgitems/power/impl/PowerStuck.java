package think.rpgitems.power.impl;

import com.google.common.cache.Cache;
import com.google.common.cache.CacheBuilder;
import org.bukkit.block.Block;
import org.bukkit.configuration.ConfigurationSection;
import org.bukkit.entity.LivingEntity;
import org.bukkit.entity.Player;
import org.bukkit.event.entity.EntityDamageByEntityEvent;
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
import java.util.function.Consumer;

import static think.rpgitems.power.Utils.*;

/**
 * Power Stuck.
 * <p>
 * The stuck power will make the hit target stuck with a chance of 1/{@link #chance}.
 * </p>
 */
@PowerMeta(defaultTrigger = TriggerType.HIT, withSelectors = true)
public class PowerStuck extends BasePower implements PowerHit, PowerRightClick {
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
    public long cooldown = 200;

    private static AtomicInteger rc = new AtomicInteger(0);

    private Random random = new Random();

    private static Consumer<EntityTeleportEvent> tpl;

    private static Consumer<PlayerTeleportEvent> pml;

    private static Cache<UUID, Long> stucked = CacheBuilder.newBuilder().expireAfterAccess(10, TimeUnit.MINUTES).concurrencyLevel(2).build();

    @Override
    public PowerResult<Double> hit(Player player, ItemStack stack, LivingEntity entity, double damage, EntityDamageByEntityEvent event) {
        if (!checkCooldown(this, player, cooldown, true)) return PowerResult.cd();
        if (random.nextInt(chance) == 0) {
            if (!getItem().consumeDurability(stack, cost)) return PowerResult.cost();
            stucked.put(entity.getUniqueId(), System.currentTimeMillis());
            entity.addPotionEffect(new PotionEffect(PotionEffectType.SLOW, duration, 10), true);
            entity.addPotionEffect(new PotionEffect(PotionEffectType.JUMP, duration, 128), true);
            return PowerResult.ok(damage);
        }
        return PowerResult.noop();
    }

    @Override
    public PowerResult<Void> rightClick(Player player, ItemStack stack, Block clicked, PlayerInteractEvent event) {
        if (!checkCooldown(this, player, cooldown, true)) return PowerResult.cd();
        if (!getItem().consumeDurability(stack, costAoe)) return PowerResult.cost();
        List<LivingEntity> entities = getLivingEntitiesInCone(getNearestLivingEntities(this, player.getEyeLocation(), player, range, 0), player.getLocation().toVector(), facing, player.getLocation().getDirection());
        entities.forEach(entity -> {
                    if (!getItem().consumeDurability(stack, costPerEntity)) return;
                    stucked.put(entity.getUniqueId(), System.currentTimeMillis());
                    entity.addPotionEffect(new PotionEffect(PotionEffectType.SLOW, duration, 10), true);
                    entity.addPotionEffect(new PotionEffect(PotionEffectType.JUMP, duration, 128), true);
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
        Set<TriggerType> triggerTypes = new HashSet<>();
        if (allowHit) {
            triggerTypes.add(TriggerType.HIT);
        }
        if (allowAoe) {
            triggerTypes.add(TriggerType.RIGHT_CLICK);
        }
        triggers = triggerTypes;
        super.init(s);
        if (orc == 0) {
            tpl = e -> {
                try {
                    if (stucked.get(e.getEntity().getUniqueId(), () -> Long.MIN_VALUE) >= (System.currentTimeMillis() - duration * 50)) {
                        e.setCancelled(true);
                    }
                } catch (ExecutionException ex) {
                    ex.printStackTrace();
                }
            };
            RPGItems.listener.addEventListener(EntityTeleportEvent.class, tpl);
            pml = e -> {
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
            };
            RPGItems.listener.addEventListener(PlayerTeleportEvent.class, pml);
        }
    }

    @Override
    public void deinit() {
        int nrc = rc.decrementAndGet();
        if (nrc == 0) {
            RPGItems.listener.removeEventListener(EntityTeleportEvent.class, tpl).removeEventListener(PlayerTeleportEvent.class, pml);
        }
    }
}