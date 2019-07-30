package think.rpgitems.power.impl;

import org.bukkit.configuration.ConfigurationSection;
import org.bukkit.entity.LivingEntity;
import org.bukkit.entity.Player;
import org.bukkit.event.entity.EntityDamageByEntityEvent;
import org.bukkit.event.entity.EntityDamageEvent;
import org.bukkit.event.entity.EntityShootBowEvent;
import org.bukkit.event.player.PlayerInteractEvent;
import org.bukkit.event.player.PlayerToggleSneakEvent;
import org.bukkit.event.player.PlayerToggleSprintEvent;
import org.bukkit.inventory.ItemStack;
import org.bukkit.potion.PotionEffect;
import org.bukkit.potion.PotionEffectType;
import think.rpgitems.I18n;
import think.rpgitems.power.*;
import think.rpgitems.utils.PotionEffectUtils;

import static think.rpgitems.power.Utils.checkCooldown;

/**
 * Power potionself.
 * <p>
 * On right click it will apply {@link #type effect}
 * for {@link #duration} ticks at power {@link #amplifier}.
 * </p>
 */
@SuppressWarnings("WeakerAccess")
@PowerMeta(defaultTrigger = "RIGHT_CLICK", generalInterface = PowerPlain.class, implClass = PowerPotionSelf.Impl.class)
public class PowerPotionSelf extends BasePower {

    @Property(order = 0)
    private long cooldown = 0;
    @Property(order = 2)
    private int amplifier = 1;
    @Property(order = 1)
    private int duration = 20;
    @Property
    private int cost = 0;
    @Deserializer(PotionEffectUtils.class)
    @Serializer(PotionEffectUtils.class)
    @Property(order = 3, required = true)
    @AcceptedValue(preset = Preset.POTION_EFFECT_TYPE)
    private PotionEffectType type = PotionEffectType.HEAL;
    @Property
    private boolean clear = false;

    @Property
    private boolean requireHurtByEntity = true;

    public class Impl implements PowerRightClick, PowerLeftClick, PowerSneak, PowerSprint, PowerHit, PowerHitTaken, PowerPlain, PowerHurt, PowerBowShoot {
        @Override
        public PowerResult<Double> takeHit(Player target, ItemStack stack, double damage, EntityDamageEvent event) {
            if (!isRequireHurtByEntity() || event instanceof EntityDamageByEntityEvent) {
                return fire(target, stack).with(damage);
            }
            return PowerResult.noop();
        }

        @Override
        public PowerResult<Void> hurt(Player target, ItemStack stack, EntityDamageEvent event) {
            if (!isRequireHurtByEntity() || event instanceof EntityDamageByEntityEvent) {
                return fire(target, stack);
            }
            return PowerResult.noop();
        }

        @Override
        public PowerResult<Void> rightClick(Player player, ItemStack stack, PlayerInteractEvent event) {
            return fire(player, stack);
        }

        @Override
        public PowerResult<Void> leftClick(Player player, ItemStack stack, PlayerInteractEvent event) {
            return fire(player, stack);
        }

        @Override
        public PowerResult<Float> bowShoot(Player player, ItemStack stack, EntityShootBowEvent event) {
            return fire(player, stack).with(event.getForce());
        }

        @Override
        public PowerResult<Void> fire(Player player, ItemStack stack) {
            if (!checkCooldown(getPower(), player, getCooldown(), true, true)) return PowerResult.cd();
            if (!getItem().consumeDurability(stack, getCost())) return PowerResult.cost();
            if (isClear()) {
                player.removePotionEffect(getType());
            } else {
                player.addPotionEffect(new PotionEffect(getType(), getDuration(), getAmplifier()), true);
            }
            return PowerResult.ok();
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
        public PowerResult<Double> hit(Player player, ItemStack stack, LivingEntity entity, double damage, EntityDamageByEntityEvent event) {
            return fire(player, stack).with(damage);
        }

        @Override
        public Power getPower() {
            return PowerPotionSelf.this;
        }
    }


    @Override
    public void init(ConfigurationSection section) {
        if (section.isInt("amp")) {
            amplifier = section.getInt("amp");
        }
        if (section.isInt("time")) {
            duration = section.getInt("time");
        }

        super.init(section);
    }

    /**
     * Amplifier of potion effect
     */
    public int getAmplifier() {
        return amplifier;
    }

    /**
     * Cooldown time of this power
     */
    public long getCooldown() {
        return cooldown;
    }

    /**
     * Cost of this power
     */
    public int getCost() {
        return cost;
    }

    /**
     * Time of potion effect, in ticks
     */
    public int getDuration() {
        return duration;
    }

    @Override
    public String getName() {
        return "potionself";
    }

    @Override
    public String displayText() {
        return I18n.format("power.potionself", getType().getName().toLowerCase().replaceAll("_", " "), getAmplifier() + 1, ((double) getDuration()) / 20d);
    }

    /**
     * Type of potion effect
     */
    public PotionEffectType getType() {
        return type;
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
}
