package think.rpgitems.power.impl;

import org.bukkit.entity.LivingEntity;
import org.bukkit.entity.Player;
import org.bukkit.event.entity.EntityDamageByEntityEvent;
import org.bukkit.inventory.ItemStack;
import org.bukkit.scheduler.BukkitRunnable;
import think.rpgitems.I18n;
import think.rpgitems.RPGItems;
import think.rpgitems.event.PowerActivateEvent;
import think.rpgitems.item.ItemManager;
import think.rpgitems.item.RPGItem;
import think.rpgitems.power.*;
import think.rpgitems.utils.LightContext;

import java.util.HashMap;
import java.util.HashSet;
import java.util.Optional;
import java.util.Set;

import static think.rpgitems.Events.DAMAGE_SOURCE;
import static think.rpgitems.Events.DAMAGE_SOURCE_ITEM;

/**
 * Power noimmutabletick.
 * <p>
 * Cancel the damage delay (no-damage-tick)
 * </p>
 */
@Meta(defaultTrigger = "HIT", implClass = NoImmutableTick.Impl.class)
public class NoImmutableTick extends BasePower {

    @Property
    public int immuneTime = 1;

    @Property
    public Set<String> requiredTags = new HashSet<>();

    public int getImmuneTime() {
        return immuneTime;
    }

    public Set<String> getRequiredTags() {
        return requiredTags;
    }

    @Override
    public String getName() {
        return "noimmutabletick";
    }

    @Override
    public String displayText() {
        return I18n.formatDefault("power.noimmutabletick");
    }

    public class Impl implements PowerHit {

        @Override
        public PowerResult<Double> hit(Player player, ItemStack stack, LivingEntity entity, double damage, EntityDamageByEntityEvent event) {
            // If requiredTags is configured, check if damage source has matching tags
            if (!getRequiredTags().isEmpty()) {
                if (!checkDamageSourceTags(player)) {
                    return PowerResult.noop();
                }
            }

            HashMap<String, Object> argsMap = new HashMap<>();
            argsMap.put("target", entity);
            argsMap.put("damage", damage);
            PowerActivateEvent powerEvent = new PowerActivateEvent(player, stack, getPower(), argsMap);
            if (!powerEvent.callEvent()) {
                return PowerResult.fail();
            }
            new BukkitRunnable() {
                @Override
                public void run() {
                    entity.setNoDamageTicks(getImmuneTime());
                }
            }.runTaskLater(RPGItems.plugin, 1);
            return PowerResult.ok(damage);
        }

        /**
         * Check if the damage source power's tags match any of the requiredTags
         */
        private boolean checkDamageSourceTags(Player player) {
            Optional<String> damageSourceOpt = LightContext.getTemp(player.getUniqueId(), DAMAGE_SOURCE);
            Optional<ItemStack> sourceItemOpt = LightContext.getTemp(player.getUniqueId(), DAMAGE_SOURCE_ITEM);

            if (damageSourceOpt.isEmpty() || sourceItemOpt.isEmpty()) {
                return false;
            }

            String damageSourceKey = damageSourceOpt.get();
            ItemStack sourceItem = sourceItemOpt.get();

            Optional<RPGItem> rpgItemOpt = ItemManager.toRPGItem(sourceItem);
            if (rpgItemOpt.isEmpty()) {
                return false;
            }

            RPGItem rpgItem = rpgItemOpt.get();

            for (Power power : rpgItem.getPowers()) {
                if (power.getNamespacedKey().toString().equals(damageSourceKey)) {
                    Set<String> powerTags = power.getTags();
                    for (String requiredTag : getRequiredTags()) {
                        if (powerTags.contains(requiredTag)) {
                            return true;
                        }
                    }
                }
            }

            return false;
        }

        @Override
        public Power getPower() {
            return NoImmutableTick.this;
        }
    }
}
