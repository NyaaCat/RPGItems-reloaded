package think.rpgitems.power.impl;

import org.bukkit.configuration.ConfigurationSection;
import org.bukkit.entity.LivingEntity;
import org.bukkit.entity.Player;
import org.bukkit.event.entity.EntityDamageByEntityEvent;
import org.bukkit.event.entity.EntityDamageEvent;
import org.bukkit.event.entity.ProjectileHitEvent;
import org.bukkit.event.player.PlayerInteractEvent;
import org.bukkit.event.player.PlayerSwapHandItemsEvent;
import org.bukkit.event.player.PlayerToggleSneakEvent;
import org.bukkit.event.player.PlayerToggleSprintEvent;
import org.bukkit.inventory.ItemStack;
import org.bukkit.enchantments.Enchantment;
import org.bukkit.NamespacedKey;
import think.rpgitems.power.*;

import javax.annotation.Nullable;

import static think.rpgitems.power.Utils.checkCooldownByString;

/**
 * Power dummy.
 * <p>
 * Won't do anything but give you fine control.
 * </p>
 */
@PowerMeta(defaultTrigger = "RIGHT_CLICK", generalInterface = PowerLivingEntity.class)
public class PowerDummy extends BasePower implements PowerHit, PowerHitTaken, PowerLeftClick, PowerRightClick, PowerOffhandClick, PowerProjectileHit, PowerSneak, PowerSprint, PowerOffhandItem, PowerMainhandItem, PowerTick, PowerLivingEntity, PowerHurt {

    /**
     * Cooldown time of this power
     */
    @Property
    public long cooldown = 20;

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
    public PowerResult<Void> fire(Player player, ItemStack stack, @Nullable LivingEntity entity, @Nullable Double damage) {
        if (!checkCooldownByString(this, player, cooldownKey, cooldown, showCDWarning, false)) return PowerResult.of(cooldownResult);
        int damagecost = cost;
        if (damage != null && costByDamage) {
            damagecost = (int) Math.round(damage * cost / 100d);
        }
        int finalcost = damagecost;
        if (costByEnchantment) {
            Enchantment ench = Enchantment.getByKey(NamespacedKey.minecraft(enchantmentType));
            if (ench == null) return PowerResult.fail();
            double costpercentage = (stack.getEnchantmentLevel(ench) * enchCostPercentage / 100d);
            if (finalcost < 0){
                finalcost = (int) Math.round(Math.random() <= costpercentage ? Math.floor(damagecost * costpercentage) : Math.ceil(finalcost * costpercentage));
            } else {
                finalcost = (int) Math.round(Math.random() <= costpercentage ? Math.ceil(damagecost * costpercentage) : Math.floor(finalcost * costpercentage));
            }
            if (doEnchReduceCost) finalcost = damagecost - finalcost;
        }
        if (!getItem().consumeDurability(stack, finalcost, checkDurabilityBound)) return PowerResult.of(costResult);
        return PowerResult.of(successResult);
    }

    @Override
    public PowerResult<Void> leftClick(Player player, ItemStack stack, PlayerInteractEvent event) {
        return fire(player, stack, null, null);
    }

    @Override
    public PowerResult<Void> rightClick(Player player, ItemStack stack, PlayerInteractEvent event) {
        return fire(player, stack, null,null);
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
        return fire(target, stack, null, damage).with(damage);
    }
    
    @Override
    public PowerResult<Double> hurt(Player target, ItemStack stack, double damage,EntityDamageEvent event) {
        return fire(target, stack, null, damage).with(damage);
    }
    
    @Override
    public PowerResult<Void> offhandClick(Player player, ItemStack stack, PlayerInteractEvent event) {
        return fire(player, stack, null,null);
    }

    @Override
    public PowerResult<Void> projectileHit(Player player, ItemStack stack, ProjectileHitEvent event) {
        return fire(player, stack, null, null);
    }

    @Override
    public PowerResult<Void> sneak(Player player, ItemStack stack, PlayerToggleSneakEvent event) {
        return fire(player, stack, null, null);
    }

    @Override
    public PowerResult<Void> sprint(Player player, ItemStack stack, PlayerToggleSprintEvent event) {
        return fire(player, stack, null, null);
    }

    @Override
    public PowerResult<Boolean> swapToMainhand(Player player, ItemStack stack, PlayerSwapHandItemsEvent event) {
        return fire(player, stack, null, null).with(true);
    }

    @Override
    public PowerResult<Boolean> swapToOffhand(Player player, ItemStack stack, PlayerSwapHandItemsEvent event) {
        return fire(player, stack, null, null).with(true);
    }

    @Override
    public PowerResult<Void> tick(Player player, ItemStack stack) {
        return fire(player, stack, null, null);
    }

    @Override
    public void init(ConfigurationSection section) {
        if (section.isBoolean("ignoreDurabilityBound")) {
            checkDurabilityBound = section.isBoolean("ignoreDurabilityBound");
        }
        super.init(section);
    }
}
