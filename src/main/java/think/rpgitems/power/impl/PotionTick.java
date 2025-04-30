package think.rpgitems.power.impl;

import net.kyori.adventure.text.Component;
import org.bukkit.Bukkit;
import org.bukkit.attribute.Attribute;
import org.bukkit.entity.Item;
import org.bukkit.entity.Player;
import org.bukkit.event.entity.EntityPotionEffectEvent;
import org.bukkit.inventory.ItemStack;
import org.bukkit.potion.PotionEffect;
import org.bukkit.potion.PotionEffectType;
import think.rpgitems.I18n;
import think.rpgitems.event.PowerActivateEvent;
import think.rpgitems.item.ItemManager;
import think.rpgitems.power.*;
import think.rpgitems.power.trigger.Trigger;
import think.rpgitems.utils.PotionEffectUtils;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.HashMap;
import java.util.List;

import static java.lang.Double.min;
import static think.rpgitems.power.Utils.checkAndSetCooldown;

/**
 * Power potiontick.
 * <p>
 * The potiontick power will give the welder {@link #effect}
 * level {@link #amplifier} while held/worn
 * </p>
 */
@SuppressWarnings("WeakerAccess")
@Meta(defaultTrigger = {"TICK","POTION_EFFECT"}, implClass = PotionTick.Impl.class, generalInterface = PowerPotionEffect.class)
public class PotionTick extends BasePower {

    @Deserializer(PotionEffectUtils.class)
    @Serializer(PotionEffectUtils.class)
    @Property(order = 1, required = true)
    @AcceptedValue(preset = Preset.POTION_EFFECT_TYPE)
    public PotionEffectType effect = PotionEffectType.SPEED;
    @Property(order = 0)
    public int amplifier = 1;
    @Property
    public int cost = 0;
    @Property(order = 2)
    public int interval = 0;
    @Property(order = 3)
    public int duration = 60;
    @Property
    public boolean clear = false;
    @Property
    public boolean summingUp = false;

    public boolean showCooldownWarning = false;

    /**
     * Cost of this power
     */
    public int getCost() {
        return cost;
    }

    /**
     * Duration of this power
     */
    public int getDuration() {
        return duration;
    }

    /**
     * Interval of this power
     */
    public int getInterval() {
        return interval;
    }

    public boolean isSummingUp() {
        return summingUp;
    }

    @Override
    public String getName() {
        return "potiontick";
    }

    @Override
    public String displayText() {
        return isClear() ?
                I18n.formatDefault("power.potiontick.clear", "<lang:effect.minecraft."+getEffect().key().value()+">")
                : I18n.formatDefault("power.potiontick.display", "<lang:effect.minecraft."+getEffect().key().value()+">", getAmplifier() + 1);
    }

    /**
     * Whether to remove the effect instead of adding it.
     */
    public boolean isClear() {
        return clear;
    }

    /**
     * Type of potion effect
     */
    public PotionEffectType getEffect() {
        return effect;
    }

    /**
     * Amplifier of potion effect
     */
    public int getAmplifier() {
        return amplifier;
    }

    public class Impl implements PowerTick, PowerSneaking, PowerPotionEffect {
        @Override
        public PowerResult<Void> tick(Player player, ItemStack stack) {
            return fire(player, stack);
        }

        private PowerResult<Void> fire(Player player, ItemStack stack) {
            final int[] summing = {0};
            List<ItemStack> items = new ArrayList<>(Arrays.asList(player.getInventory().getArmorContents()));
            items.add(player.getInventory().getItemInMainHand());
            for(ItemStack i : items){
                ItemManager.toRPGItemByMeta(i).ifPresent(rpgItem -> {
                    for (Power power : rpgItem.getPowers()){
                        if(power.getName().equals("potiontick")) {
                            PotionTick potionTick = (PotionTick) power;
                            if(potionTick.getEffect()==getEffect()&&potionTick.isSummingUp()){
                                summing[0] += potionTick.getAmplifier();
                            }
                        }
                    }
                });
            }
            PowerActivateEvent powerEvent = new PowerActivateEvent(player,stack,getPower());
            if(!powerEvent.callEvent()) {
                return PowerResult.fail();
            }
            if (!checkAndSetCooldown(getPower(), player, getInterval(), showCooldownWarning(), true, getItem().getUid() + "." + "potiontick." + getEffect().getName()))
                return PowerResult.cd();
            if (!getItem().consumeDurability(stack, getCost())) return PowerResult.cost();
            double health = player.getHealth();
            boolean hasEffect = false;
            for (PotionEffect potionEffect : player.getActivePotionEffects()) {
                if (potionEffect.getType().equals(getEffect())) {
                    hasEffect = true;
                    if (isClear()&&player.hasPotionEffect(getEffect())) {
                        player.removePotionEffect(getEffect());
                    }
                    else player.addPotionEffect(new PotionEffect(getEffect(), getDuration(), getAmplifier()+summing[0], true));
                    break;
                }
            }
            if (!hasEffect && !isClear()) {
                player.addPotionEffect(new PotionEffect(getEffect(), getDuration(), getAmplifier()+summing[0], true));
            }
            if (getEffect().equals(PotionEffectType.HEALTH_BOOST) && health > 0) {
                health = min(health, player.getAttribute(Attribute.MAX_HEALTH).getValue());
                player.setHealth(health);
            }
            return PowerResult.ok();
        }

        @Override
        public Power getPower() {
            return PotionTick.this;
        }

        @Override
        public PowerResult<Void> sneaking(Player player, ItemStack stack) {
            return fire(player, stack);
        }

        @Override
        public PowerResult<Void> potionEffect(Player player, ItemStack stack, EntityPotionEffectEvent event){
            if(isClear()){
                if(event.getModifiedType()==getEffect()&&event.getAction()== EntityPotionEffectEvent.Action.ADDED){
                    event.setCancelled(true);
                    return PowerResult.ok();
                }
            }
            return PowerResult.noop();
        }
    }
}
