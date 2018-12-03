package think.rpgitems.power.impl;

import org.bukkit.Effect;
import org.bukkit.configuration.ConfigurationSection;
import org.bukkit.entity.Entity;
import org.bukkit.entity.LivingEntity;
import org.bukkit.entity.Player;
import org.bukkit.event.entity.EntityDamageByEntityEvent;
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
@SuppressWarnings("WeakerAccess")
@PowerMeta(implClass = PowerAOE.Impl.class, defaultTrigger = "RIGHT_CLICK", withSelectors = true, generalInterface = PowerPlain.class)
public class PowerAOE extends BasePower {

    @Override
    public void init(ConfigurationSection section) {
        if (section.isString("name")) {
            display = section.getString("name");
        }
        super.init(section);
    }

    /**
     * Cooldown time of this power
     */
    @Property(order = 0)
    protected long cooldown = 20;
    /**
     * Amplifier of the potion
     */
    @Property(order = 4, required = true)
    protected int amplifier = 1;
    /**
     * Duration of the potion
     */
    @Property(order = 3)
    protected int duration = 15;
    /**
     * Range of the potion
     */
    @Property(order = 1)
    protected int range = 5;
    /**
     * Whether the potion will be apply to the user
     */
    @Property(order = 5)
    protected boolean selfapplication = true;
    /**
     * Type of the potion
     */
    @Property(order = 2)
    @Deserializer(PotionEffectUtils.class)
    @Serializer(PotionEffectUtils.class)
    @AcceptedValue(preset = Preset.POTION_EFFECT_TYPE)
    protected PotionEffectType type;
    /**
     * Display text of this power. Will use default text in case of null
     */
    @Property
    protected String display = null;
    /**
     * Cost of this power
     */
    @Property
    protected int cost = 0;

    @Override
    public String getName() {
        return "aoe";
    }

    @Override
    public String displayText() {
        return display != null ? display : I18n.format("power.aoe.display", getType().getName(), getAmplifier(), getDuration(), isSelfapplication() ? I18n.format("power.aoe.selfapplication.including") : I18n.format("power.aoe.selfapplication.excluding"), getRange(), (double) getCooldown() / 20d);
    }

    public long getCooldown() {
        return cooldown;
    }

    public int getAmplifier() {
        return amplifier;
    }

    public int getDuration() {
        return duration;
    }

    public int getRange() {
        return range;
    }

    public boolean isSelfapplication() {
        return selfapplication;
    }

    public PotionEffectType getType() {
        return type;
    }

    public int getCost() {
        return cost;
    }

    public class Impl implements PowerRightClick, PowerLeftClick, PowerPlain, PowerHit {

        @Override
        public PowerResult<Void> rightClick(final Player player, ItemStack stack, PlayerInteractEvent event) {
            return fire(player, stack);
        }

        @Override
        public PowerResult<Void> leftClick(final Player player, ItemStack stack, PlayerInteractEvent event) {
            return fire(player, stack);
        }

        @Override
        public PowerResult<Double> hit(Player player, ItemStack stack, LivingEntity entity, double damage, EntityDamageByEntityEvent event) {
            return fire(player, stack).with(damage);
        }

        @Override
        public PowerResult<Void> fire(Player player, ItemStack stack) {
            if (!checkCooldown(PowerAOE.this, player, getCooldown(), true, true)) return PowerResult.cd();
            if (!getItem().consumeDurability(stack, getCost())) return PowerResult.cost();
            PotionEffect effect = new PotionEffect(getType(), getDuration(), getAmplifier() - 1);
            if (isSelfapplication()) {
                player.addPotionEffect(effect);
            }
            player.getWorld().playEffect(player.getLocation(), Effect.POTION_BREAK, getType().getColor().asRGB());
            for (Entity ent : getNearbyEntities(this, player.getLocation(), player, getRange())) {
                if (ent instanceof LivingEntity && !player.equals(ent)) {
                    ((LivingEntity) ent).addPotionEffect(effect);
                }
            }
            return PowerResult.ok();
        }

        @Override
        public Power getPower() {
            return PowerAOE.this;
        }
    }
}
