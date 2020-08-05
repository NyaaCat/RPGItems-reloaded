package think.rpgitems.power.trigger;

import org.bukkit.event.Event;
import org.bukkit.event.entity.*;
import org.bukkit.event.inventory.InventoryClickEvent;
import org.bukkit.event.player.PlayerInteractEvent;
import org.bukkit.event.player.PlayerSwapHandItemsEvent;
import org.bukkit.event.player.PlayerToggleSneakEvent;
import org.bukkit.event.player.PlayerToggleSprintEvent;
import think.rpgitems.event.BeamEndEvent;
import think.rpgitems.event.BeamHitBlockEvent;
import think.rpgitems.event.BeamHitEntityEvent;
import think.rpgitems.power.*;

import java.util.Optional;

public final class BaseTriggers {
    public static final Trigger<EntityShootBowEvent, PowerBowShoot, Float, Optional<Float>> BOW_SHOOT = new BowShoot();
    public static final Trigger<EntityDamageByEntityEvent, PowerHit, Double, Optional<Double>> HIT = new Hit();
    public static final Trigger<EntityDamageByEntityEvent, PowerHit, Double, Optional<Double>> HIT_GLOBAL = new HitGlobal();
    public static final Trigger<ProjectileHitEvent, PowerProjectileHit, Void, Void> PROJECTILE_HIT = new ProjectileHit();
    public static final Trigger<EntityDamageEvent, PowerHitTaken, Double, Optional<Double>> HIT_TAKEN = new HitTaken();
    public static final Trigger<EntityDamageEvent, PowerHitTaken, Void, Optional<Void>> DYING = new Dying();
    public static final Trigger<EntityDamageEvent, PowerHurt, Void, Void> HURT = new Hurt();
    public static final Trigger<PlayerInteractEvent, PowerLeftClick, Void, Void> LEFT_CLICK = new LeftClick();
    public static final Trigger<PlayerInteractEvent, PowerRightClick, Void, Void> RIGHT_CLICK = new RightClick();
    public static final Trigger<PlayerInteractEvent, PowerOffhandClick, Void, Void> OFFHAND_CLICK = new OffhandClick();
    public static final Trigger<PlayerToggleSneakEvent, PowerSneak, Void, Void> SNEAK = new Sneak();
    public static final Trigger<PlayerToggleSprintEvent, PowerSprint, Void, Void> SPRINT = new Sprint();
    public static final Trigger<PlayerSwapHandItemsEvent, PowerMainhandItem, Boolean, Boolean> SWAP_TO_OFFHAND = new SwapToOffhand();
    public static final Trigger<PlayerSwapHandItemsEvent, PowerOffhandItem, Boolean, Boolean> SWAP_TO_MAINHAND = new SwapToMainhand();
    public static final Trigger<InventoryClickEvent, PowerMainhandItem, Boolean, Boolean> PLACE_OFF_HAND = new PlaceOffhand();
    public static final Trigger<InventoryClickEvent, PowerOffhandItem, Boolean, Boolean> PICKUP_OFF_HAND = new PickupOffhand();
    public static final Trigger<Event, PowerSneaking, Void, Void> SNEAKING = new Sneaking();
    public static final Trigger<ProjectileLaunchEvent, PowerProjectileLaunch, Void, Void> LAUNCH_PROJECTILE = new ProjectileLaunch();
    public static final Trigger<Event, PowerTick, Void, Void> TICK = new Tick();
    public static final Trigger<Event, PowerAttachment, Void, Void> ATTACHMENT = new Attachment();
    public static final Trigger<Event, PowerLocation, Void, Void> LOCATION = new Location();
    public static final Trigger<Event, PowerLivingEntity, Void, Void> LIVINGENTITY = new LivingEntity();
    public static final Trigger<Event, PowerTick, Void, Void> TICK_OFFHAND = new TickOffhand();
    public static final Trigger<BeamHitBlockEvent, PowerBeamHit, Void, Void> BEAM_HIT_BLOCK = new BeamHit<>(BeamHitBlockEvent.class, Void.class, Void.class, "BEAM_HIT_BLOCK");
    public static final Trigger<BeamHitEntityEvent, PowerBeamHit, Double, Optional<Double>> BEAM_HIT_ENTITY = new BeamHit<>(BeamHitEntityEvent.class, Double.class, Optional.class, "BEAM_HIT_ENTITY");
    public static final Trigger<BeamEndEvent, PowerBeamHit, Double, Optional<Double>> BEAM_END = new BeamHit<>(BeamEndEvent.class, Double.class, Optional.class, "BEAM_END");
}
