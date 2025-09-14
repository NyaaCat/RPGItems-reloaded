package think.rpgitems.power.impl;

import org.bukkit.Sound;
import org.bukkit.entity.Arrow;
import org.bukkit.entity.Player;
import org.bukkit.event.player.PlayerInteractEvent;
import org.bukkit.inventory.ItemStack;
import org.bukkit.potion.PotionEffect;
import org.bukkit.potion.PotionEffectType;
import think.rpgitems.Events;
import think.rpgitems.I18n;
import think.rpgitems.event.PowerActivateEvent;
import think.rpgitems.power.*;
import think.rpgitems.utils.PotionEffectUtils;

import java.util.HashMap;

import static think.rpgitems.power.Utils.checkCooldown;

/**
 * Power tippedarrow.
 * <p>
 * The arrow power will fire an tipped arrow on right click
 * with {@link #type effect} for {@link #duration} ticks at power {@link #amplifier}
 * </p>
 */
@SuppressWarnings("WeakerAccess")
@Meta(immutableTrigger = true, implClass = TippedArrows.Impl.class)
public class TippedArrows extends BasePower {

    @Property(order = 0)
    public int cooldown = 0;
    @Property(order = 3, required = true)
    public int amplifier = 1;
    @Property(order = 2)
    public int duration = 15;
    @Deserializer(PotionEffectUtils.class)
    @Serializer(PotionEffectUtils.class)
    @AcceptedValue(preset = Preset.POTION_EFFECT_TYPE)
    @Property(order = 1)
    public PotionEffectType type = PotionEffectType.POISON;
    @Property
    public int cost = 0;
    @Property
    public boolean setShooter = false;

    /**
     * Cost of this power
     */
    public int getCost() {
        return cost;
    }

    public boolean isSetShooter() {
        return setShooter;
    }

    @Override
    public String getName() {
        return "tippedarrow";
    }

    @Override
    public String displayText() {
        return I18n.formatDefault("power.tippedarrow", getType().getName().toLowerCase().replaceAll("_", " "), getAmplifier() + 1, ((double) getDuration()) / 20d, (double) getCooldown() / 20d);
    }

    /**
     * Type of potion effect
     */
    public PotionEffectType getType() {
        return type;
    }

    /**
     * Amplifier of potion effect
     */
    public int getAmplifier() {
        return amplifier;
    }

    /**
     * Duration of potion effect, in ticks
     */
    public int getDuration() {
        return duration;
    }

    /**
     * Cooldown time of this power
     */
    public int getCooldown() {
        return cooldown;
    }

    public class Impl implements PowerRightClick {
        @Override
        public PowerResult<Void> rightClick(Player player, ItemStack stack, PlayerInteractEvent event) {
            PowerActivateEvent powerEvent = new PowerActivateEvent(player,stack,getPower());
            if(!powerEvent.callEvent()) {
                return PowerResult.fail();
            }
            if (!checkCooldown(getPower(), player, getCooldown(), showCooldownWarning(), true)) return PowerResult.cd();
            if (!getItem().consumeDurability(stack, getCost())) return PowerResult.cost();
            player.playSound(player.getLocation(), Sound.ENTITY_ARROW_SHOOT, 1.0f, 1.0f);
            Events.registerRPGProjectile(getPower().getItem(), stack, player);
            Arrow arrow = player.launchProjectile(Arrow.class);
            if(isSetShooter()) {
                arrow.setShooter(player);
            }
            arrow.addCustomEffect(new PotionEffect(getType(), getDuration(), getAmplifier()), true);
            Events.autoRemoveProjectile(arrow.getEntityId());
            return PowerResult.ok();
        }

        @Override
        public Power getPower() {
            return TippedArrows.this;
        }
    }
}
