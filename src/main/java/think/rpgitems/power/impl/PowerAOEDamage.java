package think.rpgitems.power.impl;

import org.bukkit.attribute.Attribute;
import org.bukkit.entity.LivingEntity;
import org.bukkit.entity.Player;
import org.bukkit.event.entity.EntityDamageByEntityEvent;
import org.bukkit.event.entity.EntityDamageEvent;
import org.bukkit.event.player.PlayerInteractEvent;
import org.bukkit.event.player.PlayerToggleSneakEvent;
import org.bukkit.event.player.PlayerToggleSprintEvent;
import org.bukkit.inventory.ItemStack;
import org.bukkit.potion.PotionEffect;
import org.bukkit.potion.PotionEffectType;
import org.bukkit.scheduler.BukkitRunnable;
import think.rpgitems.RPGItems;
import think.rpgitems.data.Context;
import think.rpgitems.power.*;

import java.util.List;

import static java.lang.Double.max;
import static java.lang.Double.min;
import static think.rpgitems.Events.*;
import static think.rpgitems.power.Utils.*;

/**
 * Power AOEDamage.
 * <p>
 * On trigger the power will deal {@link #damage damage}
 * to all entities within the {@link #range range}.
 * By default, the user will not be targeted
 * as well if not set via {@link #selfapplication selfapplication}.
 * </p>
 */
@PowerMeta(defaultTrigger = "RIGHT_CLICK", withSelectors = true, generalInterface = PowerPlain.class)
public class PowerAOEDamage extends BasePower implements PowerOffhandClick, PowerPlain, PowerLeftClick, PowerRightClick, PowerHit, PowerSprint, PowerSneak, PowerHurt, PowerHitTaken, PowerTick {

    /**
     * Cooldown time of this power
     */
    @Property
    public long cooldown = 0;
    /**
     * Range of the power
     */
    @Property
    public int range = 10;
    /**
     * Minimum radius
     */
    @Property
    public int minrange = 0;
    /**
     * Maximum view angle
     */
    @Property
    public double angle = 180;
    /**
     * Maximum count, excluding the user
     */
    @Property
    public int count = 100;
    /**
     * Whether include players
     */
    @Property
    public boolean incluePlayers = false;
    /**
     * Whether damage will be apply to the user
     */
    @Property
    public boolean selfapplication = false;
    /**
     * Whether only apply to the entities that player have line of sight
     */
    @Property
    public boolean mustsee = false;
    /**
     * Display text of this power. Will use default text in case of null
     */
    @Property
    public String name = null;
    /**
     * Cost of this power
     */
    @Property
    public int cost = 0;
    /**
     * Damage of this power
     */
    @Property
    public double damage = 0;

    /**
     * Delay of the damage
     */
    @Property
    public long delay = 0;

    /**
     * Whether to suppress the hit trigger
     */
    @Property
    public boolean suppressMelee = false;

    @Override
    public PowerResult<Void> rightClick(final Player player, ItemStack stack, PlayerInteractEvent event) {
        return fire(player, stack);
    }

    @Override
    public PowerResult<Void> leftClick(final Player player, ItemStack stack, PlayerInteractEvent event) {
        return fire(player, stack);
    }

    @Override
    public PowerResult<Void> offhandClick(final Player player, ItemStack stack, PlayerInteractEvent event) {
        return fire(player, stack);
    }

    @Override
    public PowerResult<Double> hit(Player player, ItemStack stack, LivingEntity entity, double damage, EntityDamageByEntityEvent event) {
        return fire(player, stack).with(damage);
    }

    @Override
    public PowerResult<Void> sprint(Player player, ItemStack stack, PlayerToggleSprintEvent event) {
        return fire(player, stack);
    }

    @Override
    public PowerResult<Void> sneak(Player player, ItemStack stack, PlayerToggleSneakEvent event) {
        return fire(player, stack);
    }

    @Override
    public PowerResult<Void> hurt(Player target, ItemStack stack, EntityDamageEvent event) {
        return fire(target, stack);
    }

    @Override
    public PowerResult<Double> takeHit(Player target, ItemStack stack, double damage, EntityDamageEvent event) {
        return fire(target, stack).with(damage);
    }

    @Override
    public PowerResult<Void> tick(Player player, ItemStack stack) {
        return fire(player, stack);
    }


    @Override
    public PowerResult<Void> fire(Player player, ItemStack stack) {
        if (!checkCooldown(this, player, cooldown, true, true)) return PowerResult.cd();
        if (!getItem().consumeDurability(stack, cost)) return PowerResult.cost();
        Context.instance().putTemp(player.getUniqueId(), DAMAGE_SOURCE, getNamespacedKey().toString());
        Context.instance().putTemp(player.getUniqueId(), OVERRIDING_DAMAGE, damage);
        Context.instance().putTemp(player.getUniqueId(), SUPPRESS_MELEE, suppressMelee);
        if (selfapplication) dealDamage(player, damage);
        List<LivingEntity> nearbyEntities = getNearestLivingEntities(this, player.getLocation(), player, range, minrange);
        List<LivingEntity> ent = getLivingEntitiesInCone(nearbyEntities, player.getEyeLocation().toVector(), angle, player.getEyeLocation().getDirection());
        LivingEntity[] entities = ent.toArray(new LivingEntity[0]);
        int c = count;
        for (int i = 0; i < c && i < entities.length; ++i) {
            LivingEntity e = entities[i];
            if ((mustsee && !player.hasLineOfSight(e))
                        || (e == player)
                        || (!incluePlayers && e instanceof Player)
            ) {
                c++;
                continue;
            }
            if (delay <= 0) {
                e.damage(damage, player);
            } else {
                (new BukkitRunnable() {
                    @Override
                    public void run() {
                        Context.instance().putTemp(player.getUniqueId(), OVERRIDING_DAMAGE, damage);
                        Context.instance().putTemp(player.getUniqueId(), SUPPRESS_MELEE, suppressMelee);
                        Context.instance().putTemp(player.getUniqueId(), DAMAGE_SOURCE_ITEM, stack);
                        e.damage(damage, player);
                        Context.instance().putTemp(player.getUniqueId(), SUPPRESS_MELEE, suppressMelee);
                        Context.instance().putTemp(player.getUniqueId(), OVERRIDING_DAMAGE, null);
                        Context.instance().putTemp(player.getUniqueId(), DAMAGE_SOURCE_ITEM, null);
                    }
                }).runTaskLater(RPGItems.plugin, delay);
            }

        }
        return PowerResult.ok();
    }

    private static void dealDamage(LivingEntity entity, double damage) {
        if (entity.hasPotionEffect(PotionEffectType.DAMAGE_RESISTANCE)) {
            PotionEffect e = entity.getPotionEffect(PotionEffectType.DAMAGE_RESISTANCE);
            if (e.getAmplifier() >= 4) return;
        }
        double health = entity.getHealth();
        double newHealth = health - damage;
        newHealth = max(newHealth, 0.1);
        newHealth = min(newHealth, entity.getAttribute(Attribute.GENERIC_MAX_HEALTH).getValue());
        entity.setHealth(newHealth);
    }

    @Override
    public String getName() {
        return "AOEDamage";
    }

    @Override
    public String displayText() {
        return name != null ? name : "Deal damage to nearby mobs";
    }
}
