package think.rpgitems.power.trigger;

import java.util.Optional;
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

public final class BaseTriggers {
    public static final Trigger<
                    EntityShootBowEvent, Power, PowerBowShoot<Power>, Float, Optional<Float>>
            BOW_SHOOT = new BowShoot<>();
    public static final Trigger<
                    EntityDamageByEntityEvent, Power, PowerHit<Power>, Double, Optional<Double>>
            HIT = new Hit<>();
    public static final Trigger<
                    EntityDamageByEntityEvent, Power, PowerHit<Power>, Double, Optional<Double>>
            HIT_GLOBAL = new HitGlobal();
    public static final Trigger<ProjectileHitEvent, Power, PowerProjectileHit<Power>, Void, Void>
            PROJECTILE_HIT = new ProjectileHit();
    public static final Trigger<
                    EntityDamageEvent, Power, PowerHitTaken<Power>, Double, Optional<Double>>
            HIT_TAKEN = new HitTaken();
    public static final Trigger<
                    EntityDamageEvent, Power, PowerHitTaken<Power>, Void, Optional<Void>>
            DYING = new Dying();
    public static final Trigger<EntityDamageEvent, Power, PowerHurt<Power>, Void, Void> HURT =
            new Hurt();
    public static final Trigger<PlayerInteractEvent, Power, PowerLeftClick<Power>, Void, Void>
            LEFT_CLICK = new LeftClick();
    public static final Trigger<PlayerInteractEvent, Power, PowerRightClick<Power>, Void, Void>
            RIGHT_CLICK = new RightClick();
    public static final Trigger<PlayerInteractEvent, Power, PowerOffhandClick<Power>, Void, Void>
            OFFHAND_CLICK = new OffhandClick();
    public static final Trigger<PlayerToggleSneakEvent, Power, PowerSneak<Power>, Void, Void>
            SNEAK = new Sneak();
    public static final Trigger<PlayerToggleSprintEvent, Power, PowerSprint<Power>, Void, Void>
            SPRINT = new Sprint();
    public static final Trigger<
                    PlayerSwapHandItemsEvent, Power, PowerMainhandItem<Power>, Boolean, Boolean>
            SWAP_TO_OFFHAND = new SwapToOffhand();
    public static final Trigger<
                    PlayerSwapHandItemsEvent, Power, PowerOffhandItem<Power>, Boolean, Boolean>
            SWAP_TO_MAINHAND = new SwapToMainhand();
    public static final Trigger<
                    InventoryClickEvent, Power, PowerMainhandItem<Power>, Boolean, Boolean>
            PLACE_OFF_HAND = new PlaceOffhand();
    public static final Trigger<
                    InventoryClickEvent, Power, PowerOffhandItem<Power>, Boolean, Boolean>
            PICKUP_OFF_HAND = new PickupOffhand();
    public static final Trigger<Event, Power, PowerSneaking<Power>, Void, Void> SNEAKING =
            new Sneaking();
    public static final Trigger<
                    ProjectileLaunchEvent, Power, PowerProjectileLaunch<Power>, Void, Void>
            LAUNCH_PROJECTILE = new ProjectileLaunch();
    public static final Trigger<Event, Power, PowerTick<Power>, Void, Void> TICK = new Tick();
    public static final Trigger<Event, Power, PowerAttachment<Power>, Void, Void> ATTACHMENT =
            new Attachment();
    public static final Trigger<Event, Power, PowerLocation<Power>, Void, Void> LOCATION =
            new Location<>();
    public static final Trigger<Event, Power, PowerLivingEntity<Power>, Void, Void> LIVINGENTITY =
            new LivingEntity();
    public static final Trigger<Event, Power, PowerTick<Power>, Void, Void> TICK_OFFHAND =
            new TickOffhand();
    public static final Trigger<BeamHitBlockEvent, Power, PowerBeamHit<Power>, Void, Void>
            BEAM_HIT_BLOCK =
                    new BeamHit<>(
                            BeamHitBlockEvent.class, Void.class, Void.class, "BEAM_HIT_BLOCK");
    public static final Trigger<
                    BeamHitEntityEvent, Power, PowerBeamHit<Power>, Double, Optional<Double>>
            BEAM_HIT_ENTITY =
                    new BeamHit<>(
                            BeamHitEntityEvent.class,
                            Double.class,
                            Optional.class,
                            "BEAM_HIT_ENTITY");
    public static final Trigger<BeamEndEvent, Power, PowerBeamHit<Power>, Double, Optional<Double>>
            BEAM_END = new BeamHit<>(BeamEndEvent.class, Double.class, Optional.class, "BEAM_END");
}
