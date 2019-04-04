package think.rpgitems.power.impl;

import org.bukkit.Effect;
import org.bukkit.attribute.Attribute;
import org.bukkit.entity.Entity;
import org.bukkit.entity.LightningStrike;
import org.bukkit.entity.LivingEntity;
import org.bukkit.entity.Player;
import org.bukkit.event.Event;
import org.bukkit.event.entity.EntityDamageByEntityEvent;
import org.bukkit.event.entity.EntityDamageEvent;
import org.bukkit.event.player.PlayerInteractEvent;
import org.bukkit.event.player.PlayerToggleSneakEvent;
import org.bukkit.event.player.PlayerToggleSprintEvent;
import org.bukkit.inventory.ItemStack;
import org.bukkit.potion.PotionEffect;
import org.bukkit.potion.PotionEffectType;
import think.rpgitems.I18n;
import think.rpgitems.item.RPGItem;
import think.rpgitems.power.*;
import think.rpgitems.utils.PotionEffectUtils;

import static java.lang.Double.max;
import static java.lang.Double.min;
import static think.rpgitems.power.Utils.checkCooldown;
import static think.rpgitems.power.Utils.getNearbyEntities;

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
    public long cooldown = 20;
    /**
     * Range of the power
     */
    @Property
    public int range = 5;
    /**
     * Whether damage will be apply to the user
     */
    @Property
    public boolean selfapplication = false;
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
    public PowerResult<Double> hurt(Player target, ItemStack stack, double damage, EntityDamageEvent event) {
        return fire(target, stack).with(damage);
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
        if (selfapplication) dealDamage(player, damage);
        for (Entity ent : getNearbyEntities(this, player.getLocation(), player, range))
            if (ent instanceof LivingEntity && !player.equals(ent))
                dealDamage((LivingEntity) ent, damage);
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
