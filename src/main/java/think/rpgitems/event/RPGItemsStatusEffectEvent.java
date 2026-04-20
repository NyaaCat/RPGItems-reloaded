package think.rpgitems.event;

import org.bukkit.entity.Entity;
import org.bukkit.entity.LivingEntity;
import org.bukkit.event.Cancellable;
import org.bukkit.event.Event;
import org.bukkit.event.HandlerList;
import org.bukkit.potion.PotionEffect;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

public class RPGItemsStatusEffectEvent extends Event implements Cancellable {
    public enum Kind { POTION, FIRE }

    public static final HandlerList handlerList = new HandlerList();

    private final LivingEntity target;
    private final Entity source;
    private final Kind kind;
    private final PotionEffect potionEffect;
    private final int fireTicks;
    private boolean cancelled;

    private RPGItemsStatusEffectEvent(LivingEntity target, @Nullable Entity source, Kind kind,
                                      @Nullable PotionEffect potionEffect, int fireTicks) {
        this.target = target;
        this.source = source;
        this.kind = kind;
        this.potionEffect = potionEffect;
        this.fireTicks = fireTicks;
    }

    public static RPGItemsStatusEffectEvent forPotion(LivingEntity target, @Nullable Entity source, PotionEffect effect) {
        return new RPGItemsStatusEffectEvent(target, source, Kind.POTION, effect, 0);
    }

    public static RPGItemsStatusEffectEvent forFire(LivingEntity target, @Nullable Entity source, int fireTicks) {
        return new RPGItemsStatusEffectEvent(target, source, Kind.FIRE, null, fireTicks);
    }

    public LivingEntity getTarget() {
        return target;
    }

    public @Nullable Entity getSource() {
        return source;
    }

    public Kind getKind() {
        return kind;
    }

    public @Nullable PotionEffect getPotionEffect() {
        return potionEffect;
    }

    public int getFireTicks() {
        return fireTicks;
    }

    @Override
    public boolean isCancelled() {
        return cancelled;
    }

    @Override
    public void setCancelled(boolean cancel) {
        this.cancelled = cancel;
    }

    @Override
    public @NotNull HandlerList getHandlers() {
        return handlerList;
    }

    public static HandlerList getHandlerList() {
        return handlerList;
    }
}
