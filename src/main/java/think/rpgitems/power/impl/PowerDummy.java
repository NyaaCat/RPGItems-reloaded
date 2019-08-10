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

import static think.rpgitems.power.Utils.checkAndSetCooldown;

/**
 * Power dummy.
 * <p>
 * Won't do anything but give you fine control.
 * </p>
 */
@PowerMeta(defaultTrigger = "RIGHT_CLICK", generalInterface = {PowerLivingEntity.class, PowerPlain.class}, implClass = PowerDummy.Impl.class)
public class PowerDummy extends BasePower {

    @Property
    private long cooldown = 0;

    @Property
    private int cost = 0;

    @Property
    private boolean checkDurabilityBound = true;

    @Property
    private String display;

    @Property
    private boolean costByEnchantment = false;

    @Property
    private boolean doEnchReduceCost = false;


    @Property
    private double enchCostPercentage = 6;

    @Property
    private String enchantmentType = "unbreaking";

    @Property
    private boolean costByDamage = false;

    @Property
    private boolean requireHurtByEntity = true;

    @Property
    private String cooldownKey = "dummy";

    @Property
    private TriggerResult successResult = TriggerResult.OK;

    @Property
    private TriggerResult costResult = TriggerResult.COST;

    @Property
    private TriggerResult cooldownResult = TriggerResult.COOLDOWN;

    @Property
    private boolean showCDWarning = true;

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
    public long getCooldown() {
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

    public class Impl implements PowerHit, PowerHitTaken, PowerLeftClick, PowerRightClick, PowerOffhandClick, PowerProjectileHit, PowerSneak, PowerSneaking, PowerSprint, PowerOffhandItem, PowerMainhandItem, PowerTick, PowerPlain, PowerLivingEntity, PowerHurt, PowerBowShoot {

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
            if (!checkAndSetCooldown(getPower(), player, getCooldown(), isShowCDWarning(), false, getCooldownKey()))
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
            return PowerDummy.this;
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
    }
}
