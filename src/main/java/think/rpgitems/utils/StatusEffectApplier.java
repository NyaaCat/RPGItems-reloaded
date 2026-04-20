package think.rpgitems.utils;

import org.bukkit.entity.Entity;
import org.bukkit.entity.LivingEntity;
import org.bukkit.potion.PotionEffect;
import org.jetbrains.annotations.Nullable;
import think.rpgitems.event.RPGItemsStatusEffectEvent;

public final class StatusEffectApplier {
    private StatusEffectApplier() {}

    public static boolean applyPotionEffect(LivingEntity target, PotionEffect effect, @Nullable Entity source) {
        RPGItemsStatusEffectEvent event = RPGItemsStatusEffectEvent.forPotion(target, source, effect);
        if (!event.callEvent()) return false;
        return target.addPotionEffect(effect);
    }

    public static void applyFireTicks(LivingEntity target, int ticks, @Nullable Entity source) {
        RPGItemsStatusEffectEvent event = RPGItemsStatusEffectEvent.forFire(target, source, ticks);
        if (!event.callEvent()) return;
        target.setFireTicks(ticks);
    }
}
