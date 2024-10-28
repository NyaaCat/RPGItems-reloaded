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
import think.rpgitems.event.PowerActivateEvent;
import think.rpgitems.item.ItemManager;
import think.rpgitems.power.*;
import think.rpgitems.utils.PotionEffectUtils;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.HashMap;
import java.util.List;

import static think.rpgitems.power.Utils.checkCooldown;

/**
 * Power potionself.
 * <p>
 * On right click it will apply {@link #type effect}
 * for {@link #duration} ticks at power {@link #amplifier}.
 * </p>
 */
@SuppressWarnings("WeakerAccess")
@Meta(defaultTrigger = "RIGHT_CLICK", generalInterface = PowerPlain.class, implClass = PotionSelf.Impl.class)
public class PotionSelf extends BasePower {

    @Property(order = 0)
    public int cooldown = 0;
    @Property(order = 2)
    public int amplifier = 1;
    @Property(order = 1)
    public int duration = 20;
    @Property
    public int cost = 0;
    @Deserializer(PotionEffectUtils.class)
    @Serializer(PotionEffectUtils.class)
    @Property(order = 3, required = true)
    @AcceptedValue(preset = Preset.POTION_EFFECT_TYPE)
    public PotionEffectType type = PotionEffectType.HEAL;
    @Property
    public boolean clear = false;
    @Property
    public boolean summingUp = false;
    @Property
    public boolean requireHurtByEntity = true;

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
     * Cooldown time of this power
     */
    public int getCooldown() {
        return cooldown;
    }

    /**
     * Cost of this power
     */
    public int getCost() {
        return cost;
    }

    public boolean isSummingUp() {
        return summingUp;
    }

    @Override
    public String getName() {
        return "potionself";
    }

    @Override
    public String displayText() {
        return I18n.formatDefault("power.potionself", getType().getName().toLowerCase().replaceAll("_", " "), getAmplifier() + 1, ((double) getDuration()) / 20d);
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
     * Time of potion effect, in ticks
     */
    public int getDuration() {
        return duration;
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

    public class Impl implements PowerRightClick, PowerLeftClick, PowerSneak, PowerSprint, PowerHit, PowerHitTaken, PowerPlain, PowerHurt, PowerBowShoot {
        @Override
        public PowerResult<Double> takeHit(Player target, ItemStack stack, double damage, EntityDamageEvent event) {
            if (!isRequireHurtByEntity() || event instanceof EntityDamageByEntityEvent) {
                return fire(target, stack).with(damage);
            }
            return PowerResult.noop();
        }

        @Override
        public PowerResult<Void> fire(Player player, ItemStack stack) {
            final int[] summing = {0};
            List<ItemStack> items = new ArrayList<>(Arrays.asList(player.getInventory().getArmorContents()));
            items.add(player.getInventory().getItemInMainHand());
            for(ItemStack i : items){
                ItemManager.toRPGItemByMeta(i).ifPresent(rpgItem -> {
                    for (Power power : rpgItem.getPowers()){
                        if(power.getName().equals("potionself")) {
                            PotionSelf potionSelf = (PotionSelf) power;
                            if(potionSelf.getType()==getType()&&potionSelf.isSummingUp()){
                                summing[0] += potionSelf.getAmplifier();
                            }
                        }
                    }
                });
            }
            PowerActivateEvent powerEvent = new PowerActivateEvent(player,stack,getPower());
            if(!powerEvent.callEvent()) {
                return PowerResult.fail();
            }
            if (!checkCooldown(getPower(), player, getCooldown(), true, true)) return PowerResult.cd();
            if (!getItem().consumeDurability(stack, getCost())) return PowerResult.cost();
            if (isClear()) {
                player.removePotionEffect(getType());
            } else {
                player.addPotionEffect(new PotionEffect(getType(), getDuration(), getAmplifier()+summing[0]));
            }
            return PowerResult.ok();
        }

        @Override
        public Power getPower() {
            return PotionSelf.this;
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
    }
}
