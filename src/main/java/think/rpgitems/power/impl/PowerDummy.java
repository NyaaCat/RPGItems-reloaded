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
import think.rpgitems.power.*;

import static think.rpgitems.power.Utils.checkCooldownByString;

/**
 * Power dummy.
 * <p>
 * Won't do anything but give you fine control.
 * </p>
 */
@PowerMeta(defaultTrigger = "RIGHT_CLICK", generalInterface = PowerPlain.class)
public class PowerDummy extends BasePower implements PowerHit, PowerHitTaken, PowerLeftClick, PowerRightClick, PowerOffhandClick, PowerProjectileHit, PowerSneak, PowerSprint, PowerOffhandItem, PowerMainhandItem, PowerTick, PowerPlain, PowerHurt {

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
     * Whether enchantments can reduce cost
     */
    @Property
    public boolean costReduceByEnchantment = false;
    
    /**
     * Percentage of cost reduced per level of enchantment
     */
    @Property
    public double costReducePercentage = 6;
    
    /**
     * Type of enchantment that reduces cost
     */
    @Property
    public Enchantment enchantmentType = Enchantment.DURABILITY;

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
        if (!checkCooldownByString(this, player, cooldownKey, cooldown, showCDWarning, false)) return PowerResult.of(cooldownResult);
        int finalcost = cost;
        if (costReduceByEnchantment) {
            double costpercentage = 1 - (stack.getEnchantmentLevel(enchantmentType) * costReducePercentage / 100d);
            finalcost = (int)(Math.random() <= costpercentage ? Math.floor(cost * costpercentage) : Math.ceil(cost * costpercentage));
        }
        if (!getItem().consumeDurability(stack, finalcost, checkDurabilityBound)) PowerResult.of(costResult);
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
        return fire(player, stack).with(damage);
    }

    @Override
    public PowerResult<Double> takeHit(Player target, ItemStack stack, double damage, EntityDamageEvent event) {
        return fire(target, stack).with(damage);
    }
    
    @Override
    public PowerResult<Void> hurt(Player target, ItemStack stack, EntityDamageEvent event) {
        return fire(target, stack);
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
    public void init(ConfigurationSection section) {
        if (section.isBoolean("ignoreDurabilityBound")) {
            checkDurabilityBound = section.isBoolean("ignoreDurabilityBound");
        }
        super.init(section);
    }
}
