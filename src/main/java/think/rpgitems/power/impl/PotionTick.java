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
import think.rpgitems.data.Context;
import think.rpgitems.event.PowerActivateEvent;
import think.rpgitems.item.ItemManager;
import think.rpgitems.power.*;
import think.rpgitems.power.trigger.Trigger;
import think.rpgitems.utils.PotionEffectUtils;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;

import static think.rpgitems.power.Utils.checkAndSetCooldown;

/**
 * Power potiontick.
 * <p>
 * The potiontick power will give the welder {@link #effect}
 * level {@link #amplifier} while held/worn
 * </p>
 */
@SuppressWarnings("WeakerAccess")
@Meta(defaultTrigger = {"TICK", "POTION_EFFECT"}, implClass = PotionTick.Impl.class, generalInterface = PowerPotionEffect.class)
public class PotionTick extends BasePower implements PowerPotion {

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
    public int interval = 20;
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
                I18n.formatDefault("power.potiontick.clear", "<lang:effect.minecraft." + getEffect().key().value() + ">")
                : I18n.formatDefault("power.potiontick.display", "<lang:effect.minecraft." + getEffect().key().value() + ">", getAmplifier() + 1);
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
            PowerActivateEvent powerEvent = new PowerActivateEvent(player, stack, getPower());
            if (!powerEvent.callEvent()) {
                return PowerResult.fail();
            }

            String cooldownKey = getItem().getUid() + ".potiontick." + getEffect().key().value();
            long now = Context.getCurrentMillis();

            // Check cooldown state without setting it
            Long cooldownExpiry = (Long) Context.instance().get(player.getUniqueId(), cooldownKey);
            boolean cooldownReady = cooldownExpiry == null || cooldownExpiry <= now;

            PotionEffect existingEffect = player.getPotionEffect(getEffect());
            boolean effectMissing = existingEffect == null || existingEffect.getDuration() <= 20;

            // Determine if we should bypass cooldown due to external effect removal
            boolean bypassCooldown = false;
            if (!isClear() && effectMissing && !cooldownReady && cooldownExpiry != null) {
                // Effect is missing but cooldown not ready - check if it was removed externally
                // Calculate when the effect should have naturally expired
                long lastApplyTime = cooldownExpiry - getInterval() * 50L;
                long expectedEffectExpiry = lastApplyTime + getDuration() * 50L;

                if (now < expectedEffectExpiry) {
                    // Effect disappeared before natural expiry → external removal (milk, death, etc.)
                    bypassCooldown = true;
                }
                // else: effect naturally expired, respect the intentional interval gap
            }

            if (!bypassCooldown) {
                // Normal cooldown check
                if (!checkAndSetCooldown(getPower(), player, getInterval(), showCooldownWarning(), true, cooldownKey)) {
                    return PowerResult.cd();
                }
            }

            if (!getItem().consumeDurability(stack, getCost())) {
                return PowerResult.cost();
            }

            int totalAmplifier = getAmplifier();
            if (isSummingUp()) {
                List<ItemStack> playerItems = new ArrayList<>(Arrays.asList(player.getInventory().getArmorContents()));
                playerItems.add(player.getInventory().getItemInMainHand());

                int additionalLevels = playerItems.stream()
                        .flatMap(item -> ItemManager.toRPGItemByMeta(item).stream())
                        .flatMap(rpgItem -> rpgItem.getPowers().stream())
                        .filter(power -> power.getName().equals("potiontick"))
                        .map(power -> (PotionTick) power)
                        .filter(potionTick -> potionTick.getEffect() == getEffect() && potionTick.isSummingUp())
                        .mapToInt(PotionTick::getAmplifier)
                        .sum();

                totalAmplifier += additionalLevels - (isSummingUp() ? getAmplifier() : 0);
            }

            double health = player.getHealth();

            if (isClear()) {
                if (existingEffect != null) {
                    player.removePotionEffect(getEffect());
                }
            } else {
                PotionEffect newEffect = new PotionEffect(getEffect(), getDuration(), totalAmplifier, isAmbient(), isShowParticles(), isShowIcon());
                player.addPotionEffect(newEffect);
            }

            // Reset cooldown if we bypassed it
            if (bypassCooldown) {
                long nextCooldown = now + getInterval() * 50L;
                Context.instance().put(player.getUniqueId(), cooldownKey, nextCooldown, nextCooldown);
            }

            if (getEffect().equals(PotionEffectType.HEALTH_BOOST) && health > 0) {
                double maxHealth = player.getAttribute(Attribute.MAX_HEALTH).getValue();
                player.setHealth(Math.min(health, maxHealth));
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
        public PowerResult<Void> potionEffect(Player player, ItemStack stack, EntityPotionEffectEvent event) {
            if (isClear()) {
                if (event.getModifiedType() == getEffect() && event.getAction() == EntityPotionEffectEvent.Action.ADDED) {
                    event.setCancelled(true);
                    return PowerResult.ok();
                }
            }
            return PowerResult.noop();
        }
    }
}
