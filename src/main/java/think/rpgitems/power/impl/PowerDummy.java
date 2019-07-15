package think.rpgitems.power.impl;

import org.bukkit.NamespacedKey;
import org.bukkit.configuration.ConfigurationSection;
import org.bukkit.enchantments.Enchantment;
import org.bukkit.entity.LivingEntity;
import org.bukkit.entity.Player;
import org.bukkit.event.entity.EntityDamageByEntityEvent;
import org.bukkit.event.entity.EntityDamageEvent;
import org.bukkit.event.entity.EntityShootBowEvent;
import org.bukkit.event.entity.ProjectileHitEvent;
import org.bukkit.event.player.PlayerInteractEvent;
import org.bukkit.event.player.PlayerSwapHandItemsEvent;
import org.bukkit.event.player.PlayerToggleSneakEvent;
import org.bukkit.event.player.PlayerToggleSprintEvent;
import org.bukkit.inventory.ItemStack;
import think.rpgitems.power.*;

import static think.rpgitems.power.Utils.checkCooldownByString;

/**
 * Power dummy.
 * <p>
 * Won't do anything but give you fine control.
 * </p>
 */
@PowerMeta(defaultTrigger = "RIGHT_CLICK", generalInterface = {PowerLivingEntity.class, PowerPlain.class})
public class PowerDummy extends BasePower implements PowerHit, PowerHitTaken, PowerLeftClick, PowerRightClick, PowerOffhandClick, PowerProjectileHit, PowerSneak, PowerSneaking, PowerSprint, PowerOffhandItem, PowerMainhandItem, PowerTick, PowerPlain, PowerLivingEntity, PowerHurt, PowerBowShoot {

    /**
     * Cooldown time of this power
     */
    @Property
    public long cooldown = 0;

    /**
     * Cost of this power
     */
    @Property
    public int cost = 0;

    @Property
    public boolean checkDurabilityBound = true;

    /**
     * Display message on item
     */
    @Property
    public String display;

    /**
     * Whether enchantments can determine cost
     */
    @Property
    public boolean costByEnchantment = false;

    /**
     * If reversed, enchantment reduces the cost instead of increasing
     */
    @Property
    public boolean doEnchReduceCost = false;


    /**
     * Percentage of cost per level of enchantment
     */
    @Property
    public double enchCostPercentage = 6;

    /**
     * Type of enchantment that reduces cost
     */
    @Property
    public String enchantmentType = "unbreaking";

    /**
     * Whether cost by damage
     */
    @Property
    public boolean costByDamage = false;

    /**
     * Whether to require hurt by entity for HURT trigger
     */
    @Property
    public boolean requireHurtByEntity = true;

    @Property
    public String cooldownKey = "dummy";

    @Property
    public TriggerResult successResult = TriggerResult.OK;

    @Property
    public TriggerResult costResult = TriggerResult.COST;

    @Property
    public TriggerResult cooldownResult = TriggerResult.COOLDOWN;

    @Property
    public boolean showCDWarning = true;

    @Override
    public PowerResult<Void> fire(Player player, ItemStack stack) {
        return fire(player, stack, null, null);
    }

    @Override
    public PowerResult<Void> fire(Player player, ItemStack stack, LivingEntity entity, Double damage) {
        if (!checkCooldownByString(this, player, cooldownKey, cooldown, showCDWarning, false))
            return PowerResult.of(cooldownResult);
        int damageCost = cost;
        if (damage != null && costByDamage) {
            if (damage < 0) damage = 0d;
            damageCost = (int) Math.round(damage * cost / 100d);
        }
        int finalCost = damageCost;
        if (costByEnchantment) {
            Enchantment ench = Enchantment.getByKey(NamespacedKey.minecraft(enchantmentType));
            if (ench == null) return PowerResult.fail();
            double costPercentage = (stack.getEnchantmentLevel(ench) * enchCostPercentage / 100d);
            if (finalCost < 0){
                finalCost = (int) Math.round(Math.random() <= costPercentage ? Math.floor(damageCost * costPercentage) : Math.ceil(finalCost * costPercentage));
            } else {
                finalCost = (int) Math.round(Math.random() <= costPercentage ? Math.ceil(damageCost * costPercentage) : Math.floor(finalCost * costPercentage));
            }
            if (doEnchReduceCost) finalCost = damageCost - finalCost;
        }
        if (!getItem().consumeDurability(stack, finalCost, checkDurabilityBound)) return PowerResult.of(costResult);
        return PowerResult.of(successResult);
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
    public String getName() {
        return "dummy";
    }

    @Override
    public String displayText() {
        return display;
    }

    @Override
    public PowerResult<Double> hit(Player player, ItemStack stack, LivingEntity entity, double damage, EntityDamageByEntityEvent event) {
        return fire(player, stack, entity, damage).with(damage);
    }

    @Override
    public PowerResult<Double> takeHit(Player target, ItemStack stack, double damage, EntityDamageEvent event) {
        if (!requireHurtByEntity || event instanceof EntityDamageByEntityEvent) {
            return fire(target, stack, null, damage).with(damage);
        }
        return PowerResult.noop();
    }

    @Override
    public PowerResult<Void> hurt(Player target, ItemStack stack, EntityDamageEvent event) {
        if (!requireHurtByEntity || event instanceof EntityDamageByEntityEvent) {
            return fire(target, stack, null, event.getDamage());
        }
        return PowerResult.noop();
    }

    @Override
    public PowerResult<Void> offhandClick(Player player, ItemStack stack, PlayerInteractEvent event) {
        return fire(player, stack);
    }

    @Override
    public PowerResult<Void> projectileHit(Player player, ItemStack stack, ProjectileHitEvent event) {
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
    public PowerResult<Boolean> swapToMainhand(Player player, ItemStack stack, PlayerSwapHandItemsEvent event) {
        return fire(player, stack).with(true);
    }

    @Override
    public PowerResult<Boolean> swapToOffhand(Player player, ItemStack stack, PlayerSwapHandItemsEvent event) {
        return fire(player, stack).with(true);
    }

    @Override
    public PowerResult<Void> tick(Player player, ItemStack stack) {
        return fire(player, stack);
    }

    @Override
    public PowerResult<Void> sneaking(Player player, ItemStack stack) {
        return fire(player, stack);
    }

    @Override
    public void init(ConfigurationSection section) {
        if (section.isBoolean("ignoreDurabilityBound")) {
            checkDurabilityBound = section.isBoolean("ignoreDurabilityBound");
        }
        super.init(section);
    }

    @Override
    public PowerResult<Float> bowShoot(Player player, ItemStack itemStack, EntityShootBowEvent e) {
        return fire(player, itemStack).with(e.getForce());
    }
}
