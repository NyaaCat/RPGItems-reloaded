package think.rpgitems.power.impl;

import static think.rpgitems.power.Utils.checkAndSetCooldown;

import org.bukkit.Location;
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
import think.rpgitems.event.BeamEndEvent;
import think.rpgitems.event.BeamHitBlockEvent;
import think.rpgitems.event.BeamHitEntityEvent;
import think.rpgitems.power.*;

/**
 * Power dummy.
 *
 * <p>Won't do anything but give you fine control.
 */
@Meta(
        defaultTrigger = "RIGHT_CLICK",
        generalInterface = {
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
        },
        implClass = Dummy.Impl.class)
public class Dummy extends BasePower {

    @Property public int cooldown = 0;

    @Property public int cost = 0;

    @Property public boolean checkDurabilityBound = true;

    @Property public String display;

    @Property public boolean costByEnchantment = false;

    @Property public boolean doEnchReduceCost = false;

    @Property public double enchCostPercentage = 6;

    @Property public String enchantmentType = "unbreaking";

    @Property public boolean costByDamage = false;

    @Property public boolean requireHurtByEntity = true;

    @Property public String cooldownKey = "dummy";

    @Property public TriggerResult successResult = TriggerResult.OK;

    @Property public TriggerResult costResult = TriggerResult.COST;

    @Property public TriggerResult cooldownResult = TriggerResult.COOLDOWN;

    @Property public boolean showCDWarning = true;

    @Property public boolean globalCooldown = false;

    @Override
    public void init(ConfigurationSection section) {
        if (section.isBoolean("ignoreDurabilityBound")) {
            checkDurabilityBound = section.isBoolean("ignoreDurabilityBound");
        }
        super.init(section);
    }

    /** Cooldown time of this power */
    public int getCooldown() {
        return cooldown;
    }

    public String getCooldownKey() {
        return cooldownKey;
    }

    public TriggerResult getCooldownResult() {
        return cooldownResult;
    }

    /** Cost of this power */
    public int getCost() {
        return cost;
    }

    public TriggerResult getCostResult() {
        return costResult;
    }

    /** Percentage of cost per level of enchantment */
    public double getEnchCostPercentage() {
        return enchCostPercentage;
    }

    /** Type of enchantment that reduces cost */
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

    /** Display message on item */
    public String getDisplay() {
        return display;
    }

    public TriggerResult getSuccessResult() {
        return successResult;
    }

    public boolean isCheckDurabilityBound() {
        return checkDurabilityBound;
    }

    /** Whether cost by damage */
    public boolean isCostByDamage() {
        return costByDamage;
    }

    /** Whether enchantments can determine cost */
    public boolean isCostByEnchantment() {
        return costByEnchantment;
    }

    /** If reversed, enchantment reduces the cost instead of increasing */
    public boolean isDoEnchReduceCost() {
        return doEnchReduceCost;
    }

    /** Whether to require hurt by entity for HURT trigger */
    public boolean isRequireHurtByEntity() {
        return requireHurtByEntity;
    }

    public boolean isShowCDWarning() {
        return showCDWarning;
    }

    public boolean isGlobalCooldown() {
        return globalCooldown;
    }

    public static class Impl
            implements PowerHit<Dummy>,
                    PowerHitTaken<Dummy>,
                    PowerLeftClick<Dummy>,
                    PowerRightClick<Dummy>,
                    PowerOffhandClick<Dummy>,
                    PowerProjectileHit<Dummy>,
                    PowerSneak<Dummy>,
                    PowerSneaking<Dummy>,
                    PowerSprint<Dummy>,
                    PowerOffhandItem<Dummy>,
                    PowerMainhandItem<Dummy>,
                    PowerTick<Dummy>,
                    PowerPlain<Dummy>,
                    PowerLivingEntity<Dummy>,
                    PowerHurt<Dummy>,
                    PowerBowShoot<Dummy>,
                    PowerBeamHit<Dummy> {

        @Override
        public PowerResult<Void> leftClick(
                Dummy power, Player player, ItemStack stack, PlayerInteractEvent event) {
            return fire(power, player, stack);
        }

        @Override
        public PowerResult<Void> fire(Dummy power, Player player, ItemStack stack) {
            return fire(power, player, stack, null, null);
        }

        @Override
        public PowerResult<Void> fire(
                Dummy power, Player player, ItemStack stack, LivingEntity entity, Double damage) {
            if (!checkAndSetCooldown(
                    power,
                    player,
                    power.getCooldown(),
                    power.isShowCDWarning(),
                    false,
                    (power.isGlobalCooldown() ? "" : (power.getItem().getUid() + "."))
                            + power.getCooldownKey()))
                return PowerResult.of(power.getCooldownResult());
            int damageCost = power.getCost();
            if (damage != null && power.isCostByDamage()) {
                if (damage < 0) damage = 0d;
                damageCost = (int) Math.round(damage * power.getCost() / 100d);
            }
            int finalCost = damageCost;
            if (power.isCostByEnchantment()) {
                Enchantment ench =
                        Enchantment.getByKey(NamespacedKey.minecraft(power.getEnchantmentType()));
                if (ench == null) return PowerResult.fail();
                double costPercentage =
                        (stack.getEnchantmentLevel(ench) * power.getEnchCostPercentage() / 100d);
                if (finalCost < 0) {
                    finalCost =
                            (int)
                                    Math.round(
                                            Math.random() <= costPercentage
                                                    ? Math.floor(damageCost * costPercentage)
                                                    : Math.ceil(finalCost * costPercentage));
                } else {
                    finalCost =
                            (int)
                                    Math.round(
                                            Math.random() <= costPercentage
                                                    ? Math.ceil(damageCost * costPercentage)
                                                    : Math.floor(finalCost * costPercentage));
                }
                if (power.isDoEnchReduceCost()) finalCost = damageCost - finalCost;
            }
            if (!power.getItem()
                    .consumeDurability(stack, finalCost, power.isCheckDurabilityBound()))
                return PowerResult.of(power.getCostResult());
            return PowerResult.of(power.getSuccessResult());
        }

        @Override
        public Class<? extends Dummy> getPowerClass() {
            return Dummy.class;
        }

        @Override
        public PowerResult<Void> rightClick(
                Dummy power, Player player, ItemStack stack, PlayerInteractEvent event) {
            return fire(power, player, stack);
        }

        @Override
        public PowerResult<Double> hit(
                Dummy power,
                Player player,
                ItemStack stack,
                LivingEntity entity,
                double damage,
                EntityDamageByEntityEvent event) {
            return fire(power, player, stack, entity, damage).with(damage);
        }

        @Override
        public PowerResult<Double> takeHit(
                Dummy power,
                Player target,
                ItemStack stack,
                double damage,
                EntityDamageEvent event) {
            if (!power.isRequireHurtByEntity() || event instanceof EntityDamageByEntityEvent) {
                return fire(power, target, stack, null, damage).with(damage);
            }
            return PowerResult.noop();
        }

        @Override
        public PowerResult<Void> hurt(
                Dummy power, Player target, ItemStack stack, EntityDamageEvent event) {
            if (!power.isRequireHurtByEntity() || event instanceof EntityDamageByEntityEvent) {
                return fire(power, target, stack, null, event.getDamage());
            }
            return PowerResult.noop();
        }

        @Override
        public PowerResult<Void> offhandClick(
                Dummy power, Player player, ItemStack stack, PlayerInteractEvent event) {
            return fire(power, player, stack);
        }

        @Override
        public PowerResult<Void> projectileHit(
                Dummy power, Player player, ItemStack stack, ProjectileHitEvent event) {
            return fire(power, player, stack);
        }

        @Override
        public PowerResult<Void> sneak(
                Dummy power, Player player, ItemStack stack, PlayerToggleSneakEvent event) {
            return fire(power, player, stack);
        }

        @Override
        public PowerResult<Void> sprint(
                Dummy power, Player player, ItemStack stack, PlayerToggleSprintEvent event) {
            return fire(power, player, stack);
        }

        @Override
        public PowerResult<Boolean> swapToMainhand(
                Dummy power, Player player, ItemStack stack, PlayerSwapHandItemsEvent event) {
            return fire(power, player, stack).with(true);
        }

        @Override
        public PowerResult<Boolean> swapToOffhand(
                Dummy power, Player player, ItemStack stack, PlayerSwapHandItemsEvent event) {
            return fire(power, player, stack).with(true);
        }

        @Override
        public PowerResult<Void> tick(Dummy power, Player player, ItemStack stack) {
            return fire(power, player, stack);
        }

        @Override
        public PowerResult<Void> sneaking(Dummy power, Player player, ItemStack stack) {
            return fire(power, player, stack);
        }

        @Override
        public PowerResult<Float> bowShoot(
                Dummy power, Player player, ItemStack itemStack, EntityShootBowEvent e) {
            return fire(power, player, itemStack).with(e.getForce());
        }

        @Override
        public PowerResult<Double> hitEntity(
                Dummy power,
                Player player,
                ItemStack stack,
                LivingEntity entity,
                double damage,
                BeamHitEntityEvent event) {
            return fire(power, player, stack, entity, damage).with(damage);
        }

        @Override
        public PowerResult<Void> hitBlock(
                Dummy power,
                Player player,
                ItemStack stack,
                Location location,
                BeamHitBlockEvent event) {
            return fire(power, player, stack);
        }

        @Override
        public PowerResult<Void> beamEnd(
                Dummy power,
                Player player,
                ItemStack stack,
                Location location,
                BeamEndEvent event) {
            return fire(power, player, stack);
        }
    }
}
