package think.rpgitems.power.impl;

import org.bukkit.Effect;
import org.bukkit.entity.Entity;
import org.bukkit.entity.LivingEntity;
import org.bukkit.entity.Player;
import org.bukkit.event.entity.EntityDamageByEntityEvent;
import org.bukkit.event.entity.EntityShootBowEvent;
import org.bukkit.event.player.PlayerInteractEvent;
import org.bukkit.inventory.ItemStack;
import org.bukkit.potion.PotionEffect;
import org.bukkit.potion.PotionEffectType;
import think.rpgitems.I18n;
import think.rpgitems.power.*;
import think.rpgitems.utils.PotionEffectUtils;

import static think.rpgitems.power.Utils.checkCooldown;
import static think.rpgitems.power.Utils.getNearbyEntities;

/**
 * Power aoe.
 * <p>
 * On right click the aoe power will apply {@link #type effect}
 * to all entities within the {@link #range range} for {@link #duration duration} ticks
 * at power {@link #amplifier amplifier}.
 * By default, the user will be targeted by the potion
 * as well if not set via {@link #selfapplication selfapplication}.
 * </p>
 */
@Meta(defaultTrigger = "RIGHT_CLICK", withSelectors = true, generalInterface = PowerPlain.class, implClass = AOE.Impl.class)
public class AOE extends BasePower {

    @Property(order = 0)
    public int cooldown = 0;
    @Property(order = 4, required = true)
    public int amplifier = 1;
    @Property(order = 3)
    public int duration = 15;
    @Property(order = 1)
    public int range = 5;
    @Property(order = 5)
    public boolean selfapplication = true;

    @Property(order = 2)
    @Deserializer(PotionEffectUtils.class)
    @Serializer(PotionEffectUtils.class)
    @AcceptedValue(preset = Preset.POTION_EFFECT_TYPE)
    public PotionEffectType type;
    @Property(alias = "name")
    public String display = null;
    @Property
    public int cost = 0;

    /**
     * Cost of this power
     */
    public int getCost() {
        return cost;
    }

    @Override
    public String getName() {
        return "aoe";
    }

    @Override
    public String displayText() {
        return getDisplay() != null ? getDisplay() : I18n.format("power.aoe.display", getType().getName(), getAmplifier(), getDuration(), isSelfapplication() ? I18n.format("power.aoe.selfapplication.including") : I18n.format("power.aoe.selfapplication.excluding"), getRange(), (double) getCooldown() / 20d);
    }

    public String getDisplay() {
        return display;
    }

    /**
     * Type of the potion
     */
    public PotionEffectType getType() {
        return type;
    }

    /**
     * Amplifier of the potion
     */
    public int getAmplifier() {
        return amplifier;
    }

    /**
     * Duration of the potion
     */
    public int getDuration() {
        return duration;
    }

    /**
     * Whether the potion will be apply to the user
     */
    public boolean isSelfapplication() {
        return selfapplication;
    }

    /**
     * Range of the potion
     */
    public int getRange() {
        return range;
    }

    /**
     * Cooldown time of this power
     */
    public int getCooldown() {
        return cooldown;
    }

    public class Impl implements PowerRightClick, PowerLeftClick, PowerOffhandClick, PowerPlain, PowerHit, PowerBowShoot {
        @Override
        public PowerResult<Void> rightClick(final Player player, ItemStack stack, PlayerInteractEvent event) {
            return fire(player, stack);
        }

        @Override
        public PowerResult<Void> fire(Player player, ItemStack stack) {
            if (!checkCooldown(getPower(), player, getCooldown(), true, true)) return PowerResult.cd();
            if (!getItem().consumeDurability(stack, getCost())) return PowerResult.cost();
            PotionEffect effect = new PotionEffect(getType(), getDuration(), getAmplifier() - 1);
            if (isSelfapplication()) {
                player.addPotionEffect(effect);
            }
            player.getWorld().playEffect(player.getLocation(), Effect.POTION_BREAK, getType().getColor().asRGB());
            for (Entity ent : getNearbyEntities(getPower(), player.getLocation(), player, getRange())) {
                if (ent instanceof LivingEntity && !player.equals(ent)) {
                    ((LivingEntity) ent).addPotionEffect(effect);
                }
            }
            return PowerResult.ok();
        }

        @Override
        public Power getPower() {
            return AOE.this;
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
        public PowerResult<Float> bowShoot(Player player, ItemStack stack, EntityShootBowEvent event) {
            return fire(player, stack).with(event.getForce());
        }
    }
}
