package think.rpgitems.power.impl;

import com.destroystokyo.paper.event.player.PlayerJumpEvent;
import org.bukkit.Location;
import org.bukkit.NamespacedKey;
import org.bukkit.configuration.ConfigurationSection;
import org.bukkit.enchantments.Enchantment;
import org.bukkit.entity.LivingEntity;
import org.bukkit.entity.Player;
import org.bukkit.event.entity.*;
import org.bukkit.event.player.*;
import org.bukkit.inventory.ItemStack;
import think.rpgitems.event.BeamEndEvent;
import think.rpgitems.event.BeamHitBlockEvent;
import think.rpgitems.event.BeamHitEntityEvent;
import think.rpgitems.event.PowerActivateEvent;
import think.rpgitems.power.*;

import java.util.HashMap;

import static think.rpgitems.power.Utils.checkAndSetCooldown;

/**
 * Power dummy.
 * <p>
 * Won't do anything but give you fine control.
 * </p>
 */
@Meta(defaultTrigger = "RIGHT_CLICK", generalInterface = {
        PowerLeftClick.class,
        PowerRightClick.class,
        PowerPlain.class,
        PowerSneak.class,
        PowerLivingEntity.class,
        PowerSprint.class,
        PowerHurt.class,
        PowerHit.class,
        PowerHitTaken.class,
        PowerBowShoot.class,
        PowerBeamHit.class,
        PowerLocation.class
}, implClass = Dummy.Impl.class)
public class Dummy extends BasePower {

    @Property
    public int cooldown = 0;

    @Property
    public int cost = 0;

    @Property
    public boolean checkDurabilityBound = true;

    @Property
    public String display;

    @Property
    public boolean costByEnchantment = false;

    @Property
    public boolean doEnchReduceCost = false;


    @Property
    public double enchCostPercentage = 6;

    @Property
    public String enchantmentType = "unbreaking";

    @Property
    public boolean costByDamage = false;

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

    @Property
    public boolean globalCooldown = false;

    @Override
    public void init(ConfigurationSection section) {
        if (section.isBoolean("ignoreDurabilityBound")) {
            checkDurabilityBound = section.isBoolean("ignoreDurabilityBound");
        }
        super.init(section);
    }

    /**
     * Cooldown time of this power
     */
    public int getCooldown() {
        return cooldown;
    }

    public String getCooldownKey() {
        return cooldownKey;
    }

    public TriggerResult getCooldownResult() {
        return cooldownResult;
    }

    /**
     * Cost of this power
     */
    public int getCost() {
        return cost;
    }

    public TriggerResult getCostResult() {
        return costResult;
    }

    /**
     * Percentage of cost per level of enchantment
     */
    public double getEnchCostPercentage() {
        return enchCostPercentage;
    }

    /**
     * Type of enchantment that reduces cost
     */
    public String getEnchantmentType() {
        return enchantmentType;
    }

    @Override
    public String getName() {
        return "dummy";
    }

    @Override
    public String displayText() {
        return getDisplay();
    }

    /**
     * Display message on item
     */
    public String getDisplay() {
        return display;
    }

    public TriggerResult getSuccessResult() {
        return successResult;
    }

    public boolean isCheckDurabilityBound() {
        return checkDurabilityBound;
    }

    /**
     * Whether cost by damage
     */
    public boolean isCostByDamage() {
        return costByDamage;
    }

    /**
     * Whether enchantments can determine cost
     */
    public boolean isCostByEnchantment() {
        return costByEnchantment;
    }

    /**
     * If reversed, enchantment reduces the cost instead of increasing
     */
    public boolean isDoEnchReduceCost() {
        return doEnchReduceCost;
    }

    /**
     * Whether to require hurt by entity for HURT trigger
     */
    public boolean isRequireHurtByEntity() {
        return requireHurtByEntity;
    }

    public boolean isShowCDWarning() {
        return showCDWarning;
    }

    public boolean isGlobalCooldown() {
        return globalCooldown;
    }

    public class Impl implements PowerHit, PowerHitTaken, PowerLeftClick, PowerRightClick, PowerOffhandClick, PowerProjectileHit, PowerSneak, PowerSneaking, PowerSprint, PowerOffhandItem, PowerMainhandItem, PowerTick, PowerPlain, PowerLivingEntity, PowerHurt, PowerBowShoot, PowerBeamHit, PowerConsume, PowerJump, PowerSwim {

        @Override
        public PowerResult<Void> leftClick(Player player, ItemStack stack, PlayerInteractEvent event) {
            return fire(player, stack);
        }

        @Override
        public PowerResult<Void> fire(Player player, ItemStack stack) {
            return fire(player, stack, null, null);
        }

        @Override
        public PowerResult<Void> fire(Player player, ItemStack stack, LivingEntity entity, Double damage) {
            HashMap<String,Object> argsMap = new HashMap<>();
            argsMap.put("target",entity);
            argsMap.put("damage",damage);
            PowerActivateEvent powerEvent = new PowerActivateEvent(player,stack,getPower(),argsMap);
            if(!powerEvent.callEvent()) {
                return PowerResult.fail();
            }
            if (!checkAndSetCooldown(getPower(), player, getCooldown(), isShowCDWarning(), false, (isGlobalCooldown() ? "" : (getItem().getUid() + ".")) + getCooldownKey()))
                return PowerResult.of(getCooldownResult());
            int damageCost = getCost();
            if (damage != null && isCostByDamage()) {
                if (damage < 0) damage = 0d;
                damageCost = (int) Math.round(damage * getCost() / 100d);
            }
            int finalCost = damageCost;
            if (isCostByEnchantment()) {
                Enchantment ench = Enchantment.getByKey(NamespacedKey.minecraft(getEnchantmentType()));
                if (ench == null) return PowerResult.fail();
                double costPercentage = (stack.getEnchantmentLevel(ench) * getEnchCostPercentage() / 100d);
                if (finalCost < 0) {
                    finalCost = (int) Math.round(Math.random() <= costPercentage ? Math.floor(damageCost * costPercentage) : Math.ceil(finalCost * costPercentage));
                } else {
                    finalCost = (int) Math.round(Math.random() <= costPercentage ? Math.ceil(damageCost * costPercentage) : Math.floor(finalCost * costPercentage));
                }
                if (isDoEnchReduceCost()) finalCost = damageCost - finalCost;
            }
            if (!getItem().consumeDurability(stack, finalCost, isCheckDurabilityBound()))
                return PowerResult.of(getCostResult());
            return PowerResult.of(getSuccessResult());
        }

        @Override
        public Power getPower() {
            return Dummy.this;
        }

        @Override
        public PowerResult<Void> rightClick(Player player, ItemStack stack, PlayerInteractEvent event) {
            return fire(player, stack);
        }

        @Override
        public PowerResult<Double> hit(Player player, ItemStack stack, LivingEntity entity, double damage, EntityDamageByEntityEvent event) {
            return fire(player, stack, entity, damage).with(damage);
        }

        @Override
        public PowerResult<Double> takeHit(Player target, ItemStack stack, double damage, EntityDamageEvent event) {
            if (!isRequireHurtByEntity() || event instanceof EntityDamageByEntityEvent) {
                return fire(target, stack, null, damage).with(damage);
            }
            return PowerResult.noop();
        }

        @Override
        public PowerResult<Void> hurt(Player target, ItemStack stack, EntityDamageEvent event) {
            if (!isRequireHurtByEntity() || event instanceof EntityDamageByEntityEvent) {
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
        public PowerResult<Float> bowShoot(Player player, ItemStack itemStack, EntityShootBowEvent e) {
            return fire(player, itemStack).with(e.getForce());
        }

        @Override
        public PowerResult<Double> hitEntity(Player player, ItemStack stack, LivingEntity entity, double damage, BeamHitEntityEvent event) {
            return fire(player, stack, entity, damage).with(damage);
        }

        @Override
        public PowerResult<Void> hitBlock(Player player, ItemStack stack, Location location, BeamHitBlockEvent event) {
            return fire(player, stack);
        }

        @Override
        public PowerResult<Void> beamEnd(Player player, ItemStack stack, Location location, BeamEndEvent event) {
            return fire(player, stack);
        }

        @Override
        public PowerResult<Void> consume(Player player, ItemStack stack, PlayerItemConsumeEvent event) {
            return fire(player, stack);
        }

        @Override
        public PowerResult<Void> jump(Player player, ItemStack stack, PlayerJumpEvent event) {
            return fire(player, stack);
        }

        @Override
        public PowerResult<Void> swim(Player player, ItemStack stack, EntityToggleSwimEvent event) {
            return fire(player, stack);
        }
    }
}
