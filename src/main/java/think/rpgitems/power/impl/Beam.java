package think.rpgitems.power.impl;

import static think.rpgitems.Events.*;
import static think.rpgitems.power.Utils.*;
import static think.rpgitems.utils.cast.CastUtils.makeCone;

import cat.nyaa.nyaacore.Message;
import com.google.common.util.concurrent.AtomicDouble;
import com.udojava.evalex.Expression;
import java.math.BigDecimal;
import java.util.*;
import java.util.concurrent.atomic.AtomicInteger;
import java.util.function.Supplier;
import java.util.stream.Collectors;
import java.util.stream.Stream;
import javax.annotation.Nullable;
import org.bukkit.*;
import org.bukkit.block.Block;
import org.bukkit.block.BlockFace;
import org.bukkit.configuration.ConfigurationSection;
import org.bukkit.entity.*;
import org.bukkit.event.entity.EntityDamageByEntityEvent;
import org.bukkit.event.entity.EntityDamageEvent;
import org.bukkit.event.entity.EntityShootBowEvent;
import org.bukkit.event.player.PlayerInteractEvent;
import org.bukkit.event.player.PlayerToggleSneakEvent;
import org.bukkit.event.player.PlayerToggleSprintEvent;
import org.bukkit.inventory.ItemStack;
import org.bukkit.inventory.MainHand;
import org.bukkit.scheduler.BukkitRunnable;
import org.bukkit.util.BoundingBox;
import org.bukkit.util.RayTraceResult;
import org.bukkit.util.Vector;
import think.rpgitems.RPGItems;
import think.rpgitems.event.BeamEndEvent;
import think.rpgitems.event.BeamHitBlockEvent;
import think.rpgitems.event.BeamHitEntityEvent;
import think.rpgitems.power.*;
import think.rpgitems.utils.LightContext;
import think.rpgitems.utils.cast.CastUtils;
import think.rpgitems.utils.cast.RangedDoubleValue;
import think.rpgitems.utils.cast.RangedValueSerializer;
import think.rpgitems.utils.cast.RoundedConeInfo;

/**
 * @author <a href="mailto:ReinWDD@gmail.com">ReinWD</a>
 *     <p>Wrote &amp; Maintained by ReinWD if you have any issue, please send me email or @ReinWD in
 *     issues. Accepted language: 中文, English.
 */
@Meta(
        defaultTrigger = "RIGHT_CLICK",
        generalInterface = {
            PowerLeftClick.class,
            PowerRightClick.class,
            PowerPlain.class,
            PowerSneak.class,
            PowerLivingEntity.class,
            PowerSprint.class,
            PowerHurt.class,
            PowerHit.class,
            PowerHitTaken.class,
            PowerBowShoot.class,
            PowerBeamHit.class,
            PowerLocation.class
        },
        implClass = Beam.Impl.class)
public class Beam extends BasePower {
    @Property public int length = 10;

    @Property public int ttl = 100;

    @Property public Particle particle = Particle.LAVA;

    @Property public Mode mode = Mode.BEAM;

    @Property public int pierce = 0;

    @Property public boolean ignoreWall = false;

    @Property public double damage = 20;

    @Property public double speed = 20;

    @Property public double offsetX = 0;

    @Property public double offsetY = 0;

    @Property public double offsetZ = 0;

    @Property public double particleSpeed = 0;

    @Property public double particleDensity = 2;

    //  used to judge legacy 1.0
    //    @Property
    //    public boolean cone = false;

    @Property public double cone = 0;

    @Property public double homing = 0;

    @Property public double homingAngle = 30;

    @Property public double homingRange = 50;

    @Property public HomingMode homingMode = HomingMode.ONE_TARGET;

    @Property public Target homingTarget = Target.MOBS;

    @Property public int ticksBeforeHoming = 0;

    @Property public int burstCount = 1;

    @Property public int beamAmount = 1;

    @Property public int burstInterval = 10;

    @Property public int bounce = 0;

    @Property public boolean hitSelfWhenBounced = false;

    @Property public double gravity = 0;

    @Property
    @Serializer(ExtraDataSerializer.class)
    @Deserializer(ExtraDataSerializer.class)
    public Object extraData;

    @Property public boolean requireHurtByEntity = true;

    @Property public boolean suppressMelee = false;

    @Property public String speedBias = "";

    @Property public List<Behavior> behavior = new ArrayList<>();

    @Property public String behaviorParam = "{}";

    @Property public double initialRotation = 0;

    @Property public FiringLocation firingLocation = FiringLocation.SELF;

    @Property public boolean effectOnly = false;

    /*
     *   following 3 property format like:
     *   "<lower_value>,<upper_value>:<weight> <lower_value2>,<upper_value2> <fixed_value>:<weight> ......"
     *   actual value will works like this:
     *   weighted pick one <>,<>:<> from all defined values. Weight is 1 by default.
     *   random generate a value from <lower_value> to <upper_value>, or <fixed_value>.
     *
     */
    @Property
    @Serializer(RangedValueSerializer.class)
    @Deserializer(RangedValueSerializer.class)
    public RangedDoubleValue firingR = RangedDoubleValue.of("10,15");

    @Property
    @Serializer(RangedValueSerializer.class)
    @Deserializer(RangedValueSerializer.class)
    public RangedDoubleValue firingTheta = RangedDoubleValue.of("0,10");

    @Property
    @Serializer(RangedValueSerializer.class)
    @Deserializer(RangedValueSerializer.class)
    public RangedDoubleValue firingPhi = RangedDoubleValue.of("0,360");

    @Property public double firingRange = 64;

    @Property public boolean castOff = false;

    private static Set<Material> transp =
            Stream.of(Material.values())
                    .filter(material -> material.isBlock())
                    .filter(material -> !material.isSolid() || !material.isOccluding())
                    .collect(Collectors.toSet());

    private static Random random = new Random();

    public FiringLocation getFiringLocation() {
        return firingLocation;
    }

    public RangedDoubleValue getFiringR() {
        return firingR;
    }

    public RangedDoubleValue getFiringTheta() {
        return firingTheta;
    }

    public RangedDoubleValue getFiringPhi() {
        return firingPhi;
    }

    public double getFiringRange() {
        return firingRange;
    }

    public int getBeamAmount() {
        return beamAmount;
    }

    public double getInitialRotation() {
        return initialRotation;
    }

    public List<Behavior> getBehavior() {
        return behavior;
    }

    public String getBehaviorParam() {
        return behaviorParam;
    }

    public int getBounce() {
        return bounce;
    }

    public int getBurstCount() {
        return burstCount;
    }

    public int getBurstInterval() {
        return burstInterval;
    }

    public double getCone() {
        return cone;
    }

    public double getDamage() {
        return damage;
    }

    public Object getExtraData() {
        return extraData;
    }

    public double getGravity() {
        return gravity;
    }

    public double getHoming() {
        return homing;
    }

    public double getHomingAngle() {
        return homingAngle;
    }

    public HomingMode getHomingMode() {
        return homingMode;
    }

    public double getHomingRange() {
        return homingRange;
    }

    public Target getHomingTarget() {
        return homingTarget;
    }

    public int getLength() {
        return length;
    }

    public Mode getMode() {
        return mode;
    }

    @Override
    public String getName() {
        return "beam";
    }

    @Override
    public String displayText() {
        return null;
    }

    public double getOffsetX() {
        return offsetX;
    }

    public double getOffsetY() {
        return offsetY;
    }

    public double getOffsetZ() {
        return offsetZ;
    }

    public Particle getParticle() {
        return particle;
    }

    public double getParticleSpeed() {
        return particleSpeed;
    }

    public int getPierce() {
        return pierce;
    }

    public double getParticleDensity() {
        return particleDensity;
    }

    public double getSpeed() {
        return speed;
    }

    public String getSpeedBias() {
        return speedBias;
    }

    public int getTicksBeforeHoming() {
        return ticksBeforeHoming;
    }

    public int getTtl() {
        return ttl;
    }

    public boolean isHitSelfWhenBounced() {
        return hitSelfWhenBounced;
    }

    public boolean isIgnoreWall() {
        return ignoreWall;
    }

    /** Whether to suppress the hit trigger */
    public boolean isSuppressMelee() {
        return suppressMelee;
    }

    public boolean isRequireHurtByEntity() {
        return requireHurtByEntity;
    }

    public boolean isCastOff() {
        return castOff;
    }

    public boolean isEffectOnly() {
        return effectOnly;
    }

    @Override
    public void init(ConfigurationSection section) {
        // check new version var name
        if (section.contains("coneRange")) {
            updateFromV1(section);
        }
        if (section.contains("spawnsPerBlock")) {
            int spawnsPerBlock = section.getInt("spawnsPerBlock");
            section.set("particleDensity", spawnsPerBlock);
        }
        super.init(section);
    }

    private void updateFromV1(ConfigurationSection section) {
        boolean originalCone = section.getBoolean("cone");
        boolean pierce = section.getBoolean("pierce");
        double cone = section.getDouble("coneRange");
        int movementTicks = section.getInt("movementTicks");
        int length = section.getInt("length");
        double originSpeed = section.getDouble("speed");
        double homing = 0;
        double homingAngle = section.getDouble("homingAngle");
        double homingRange = section.getDouble("homingRange");
        String homingTargetMode = section.getString("homingTargetMode");
        int stepsBeforeHoming = section.getInt("stepsBeforeHoming");

        double spd = ((double) length * 20) / ((double) movementTicks);
        int spawnsPerBlock = section.getInt("spawnsPerBlock");
        double blockPerSpawn = 1 / ((double) spawnsPerBlock);
        double stepPerSecond = spd / blockPerSpawn;

        if (!section.getBoolean("homing")) {
            homingAngle = 0;
        } else {
            homing = blockPerSpawn / (2 * Math.cos(Math.toRadians(homingAngle)));
        }
        if (originalCone) {
            section.set("cone", cone);
        } else {
            section.set("cone", 0);
        }
        int pierceNum = 0;
        if (pierce) {
            pierceNum = 50;
        }
        section.set("speed", spd);
        section.set("particleSpeed", originSpeed);
        section.set("homing", homing);
        section.set("homingAngle", homingRange);
        section.set("homingMode", homingTargetMode);
        section.set("pierce", pierceNum);
        section.set("behavior", "LEGACY_HOMING");
        section.set("ticksBeforeHoming", stepsBeforeHoming);
        section.set("ttl", ((int) Math.floor(length * 20 / spd)));
        section.set("homingRange", length);
    }

    private static class MovingTask extends BukkitRunnable {
        public Player player = null;
        private double homingRange;
        private double length = 10;
        private int ttl = 200;
        private Particle particle = Particle.LAVA;
        private Mode mode = Mode.BEAM;
        private int pierce = 0;
        private boolean ignoreWall = true;
        private double damage = 20;
        private double speed = 20;
        private double offsetX = 0;
        private double offsetY = 0;
        private double offsetZ = 0;
        private double particleSpeed = 0;
        private double particleDensity = 2;
        private double homing = 0;
        private double homingAngle = 30;
        private Target homingTarget = Target.MOBS;
        private HomingMode homingMode = HomingMode.ONE_TARGET;
        private int ticksBeforeHoming = 5;
        private int bounce = 0;
        private boolean hitSelfWhenBounced = false;
        private double gravity = 0;
        private boolean suppressMelee = false;
        private List<Behavior> behavior = new ArrayList<>();
        private String behaviorParam = "{}";
        private Object extraData = null;
        private Beam power;
        private String speedBias = "";
        private boolean effectOnly = false;
        private final FiringLocation firingLocation;
        public double firingR = 0;
        private int triggerDepth = 0;

        private Queue<Entity> targets = new LinkedList<>();
        private Entity fromEntity;
        private Location fromLocation;
        private Vector towards;
        double lengthPerSpawn;

        AtomicDouble lengthRemains = new AtomicDouble(0);
        AtomicDouble spawnedLength = new AtomicDouble(0);
        AtomicInteger currentTick = new AtomicInteger(0);
        Vector gravityVector = new Vector(0, 0, 0);
        Location lastLocation;
        private ItemStack itemStack;
        boolean bounced = false;
        World world;
        Set<UUID> hitMob = new HashSet<>();
        int cycle = 0;
        private double initialBias = 0.2;

        MovingTask(Beam config) {
            this.length = config.getLength();
            this.ttl = config.getTtl();
            this.particle = config.getParticle();
            this.mode = config.getMode();
            this.pierce = config.getPierce();
            this.ignoreWall = config.isIgnoreWall();
            this.damage = config.getDamage();
            this.speed = config.getSpeed();
            this.offsetX = config.getOffsetX();
            this.offsetY = config.getOffsetY();
            this.offsetZ = config.getOffsetZ();
            this.particleDensity = config.getParticleDensity();
            this.homing = config.getHoming();
            this.homingMode = config.getHomingMode();
            this.ticksBeforeHoming = config.getTicksBeforeHoming();
            this.bounce = config.getBounce();
            this.hitSelfWhenBounced = config.isHitSelfWhenBounced();
            this.gravity = config.getGravity();
            this.particleSpeed = config.getParticleSpeed();
            this.suppressMelee = config.isSuppressMelee();
            this.behavior = config.getBehavior();
            this.behaviorParam = config.getBehaviorParam();
            this.extraData = config.getExtraData();
            this.speedBias = config.getSpeedBias();
            this.homingAngle = config.getHomingAngle();
            this.homingTarget = config.getHomingTarget();
            this.homingRange = config.getHomingRange();
            this.effectOnly = config.isEffectOnly();
            this.firingLocation = config.getFiringLocation();
            power = config;
            lengthPerSpawn = 1 / particleDensity;
        }

        @Override
        public void run() {
            world = fromLocation.getWorld();
            if (world == null) return;
            if (Double.isInfinite(lengthPerSpawn)) {
                return;
            }
            lastLocation = fromLocation;
            towards.normalize();
            new RecursiveTask().runTask(RPGItems.plugin);
        }

        public void setItemStack(ItemStack stack) {
            this.itemStack = stack;
        }

        class RecursiveTask extends BukkitRunnable {
            @Override
            public void run() {
                try {
                    double lengthInThisTick =
                            getNextLength(spawnedLength, length) + lengthRemains.get();

                    double lengthToSpawn = lengthInThisTick;
                    if (mode.equals(Mode.BEAM)) {
                        lengthToSpawn = length;
                    }
                    int hitCount = 0;
                    while ((lengthToSpawn -= lengthPerSpawn) > 0) {
                        hitMob.addAll(
                                tryHit(
                                        fromEntity,
                                        lastLocation,
                                        itemStack,
                                        bounced && hitSelfWhenBounced,
                                        hitMob));

                        if (cycle++ > 2 / lengthPerSpawn) {
                            hitMob.clear();
                            hitCount = 0;
                            cycle = 0;
                            if (homingMode.equals(HomingMode.MOUSE_TRACK)) {
                                Location location = fromEntity.getLocation();
                                if (fromEntity instanceof LivingEntity) {
                                    location = ((LivingEntity) fromEntity).getEyeLocation();
                                }
                                targets =
                                        new LinkedList<>(
                                                getTargets(
                                                        location.getDirection(),
                                                        location,
                                                        fromEntity,
                                                        homingRange,
                                                        homingAngle,
                                                        homingTarget));
                            }
                        }

                        spawnParticle(fromEntity, world, lastLocation, 1);
                        Vector step = towards.clone().normalize().multiply(lengthPerSpawn);
                        if (gravity != 0
                                && (homing == 0 || currentTick.get() < ticksBeforeHoming)) {
                            double partsPerTick = lengthInThisTick / lengthPerSpawn;
                            step.setY(step.getY() + getGravity(partsPerTick));
                        }
                        Location nextLoc = lastLocation.clone().add(step);
                        if (!ignoreWall
                                && (nextLoc.getBlockX() != lastLocation.getBlockX()
                                        || nextLoc.getBlockY() != lastLocation.getBlockY()
                                        || nextLoc.getBlockZ() != lastLocation.getBlockZ())) {
                            if (!(firingLocation.equals(FiringLocation.TARGET)
                                    && spawnedLength.get() < (firingR - 1))) {
                                Block block = nextLoc.getBlock();
                                if (!transp.contains(block.getType())) {
                                    if (!MovingTask.this.effectOnly) {
                                        BeamHitBlockEvent beamHitBlockEvent =
                                                new BeamHitBlockEvent(
                                                        player,
                                                        fromEntity,
                                                        block,
                                                        lastLocation,
                                                        itemStack,
                                                        triggerDepth);
                                        Bukkit.getPluginManager().callEvent(beamHitBlockEvent);
                                    }
                                    if (bounce > 0) {
                                        bounce--;
                                        bounced = true;
                                        makeBounce(nextLoc.getBlock(), towards, step, lastLocation);
                                    } else {
                                        return;
                                    }
                                }
                            }
                        }
                        lastLocation = nextLoc;
                        spawnedLength.addAndGet(lengthPerSpawn);
                        int dHit = hitMob.size() - hitCount;
                        if (dHit > 0) {
                            hitCount = hitMob.size();
                            pierce -= dHit;
                            if (pierce > 0) {
                                if (homingMode.equals(HomingMode.MULTI_TARGET)) {
                                    if (targets != null) {
                                        targets.removeIf(
                                                entity -> hitMob.contains(entity.getUniqueId()));
                                    }
                                }
                            } else {
                                return;
                            }
                        }
                        if (targets != null
                                && homing > 0
                                && currentTick.get() >= ticksBeforeHoming) {
                            towards =
                                    homingCorrect(
                                            step,
                                            lastLocation,
                                            targets.peek(),
                                            () -> {
                                                targets.removeIf(Entity::isDead);
                                                return targets.peek();
                                            });
                        }
                    }

                    lengthRemains.set(lengthToSpawn + lengthPerSpawn);
                    if (spawnedLength.get() >= length
                            || currentTick.addAndGet(1) > ttl
                            || mode == Mode.BEAM) {
                        if (!effectOnly) {
                            callEnd();
                        }
                        return;
                    }
                    new RecursiveTask().runTaskLater(RPGItems.plugin, 1);
                } catch (Exception e) {
                    this.cancel();
                    e.printStackTrace();
                }
            }

            private BeamEndEvent callEnd() {
                BeamEndEvent beamEndEvent =
                        new BeamEndEvent(player, fromEntity, lastLocation, itemStack, triggerDepth);
                Bukkit.getPluginManager().callEvent(beamEndEvent);
                return beamEndEvent;
            }
        }

        boolean reported = false;

        private double getNextLength(AtomicDouble spawnedLength, double length) {
            Expression eval =
                    new Expression(speedBias)
                            .with(
                                    "x",
                                    new Expression.LazyNumber() {
                                        @Override
                                        public BigDecimal eval() {
                                            return BigDecimal.valueOf(
                                                    spawnedLength.get() / ((double) length));
                                        }

                                        @Override
                                        public String getString() {
                                            return String.valueOf(
                                                    spawnedLength.get() / ((double) length));
                                        }
                                    })
                            .with(
                                    "t",
                                    new Expression.LazyNumber() {
                                        @Override
                                        public BigDecimal eval() {
                                            return BigDecimal.valueOf(currentTick.get() / 20d);
                                        }

                                        @Override
                                        public String getString() {
                                            return null;
                                        }
                                    });
            double v = 1;
            if (!"".equals(speedBias)) {
                try {
                    v = (eval.eval().doubleValue());
                } catch (Exception ignored) {
                    // todo: lang
                    if (!reported) {
                        new Message("invalid expression, please contact Admin").send(fromEntity);
                        reported = true;
                    }
                }
            }
            return (speed + v) / 20;
        }

        private void makeBounce(Block block, Vector towards, Vector step, Location lastLocation) {
            RayTraceResult rayTraceResult =
                    block.rayTrace(
                            lastLocation, step, towards.length() * 2, FluidCollisionMode.NEVER);
            if (rayTraceResult == null) {
                return;
            } else {
                BlockFace hitBlockFace = rayTraceResult.getHitBlockFace();
                if (hitBlockFace == null) return;
                Block relative = block.getRelative(hitBlockFace);
                if (!transp.contains(relative.getType())) {
                    RayTraceResult relativeResult =
                            relative.rayTrace(
                                    lastLocation,
                                    step,
                                    towards.length() * 2,
                                    FluidCollisionMode.NEVER);
                    if (relativeResult != null) {
                        if (relativeResult.getHitBlockFace().getDirection().getY() > 0) {
                            gravityVector.multiply(-1);
                        }
                        towards.rotateAroundAxis(
                                        relativeResult.getHitBlockFace().getDirection(),
                                        Math.toRadians(180))
                                .multiply(-1);
                        step.rotateAroundAxis(
                                        relativeResult.getHitBlockFace().getDirection(),
                                        Math.toRadians(180))
                                .multiply(-1);
                    }
                } else {
                    if (hitBlockFace.getDirection().getY() > 0) {
                        gravityVector.multiply(-1);
                    }
                    towards.rotateAroundAxis(hitBlockFace.getDirection(), Math.toRadians(180))
                            .multiply(-1);
                    step.rotateAroundAxis(hitBlockFace.getDirection(), Math.toRadians(180))
                            .multiply(-1);
                }
            }
        }

        double legacyBonus = 0;
        double lastCorrected = 0;

        private Vector homingCorrect(
                Vector towards, Location lastLocation, Entity target, Supplier<Entity> runnable) {
            if (target == null) {
                return towards;
            }
            if (target.isDead()) {
                target = runnable.get();
                if (target == null) return towards;
            }
            Location targetLocation;
            if (target instanceof LivingEntity) {
                targetLocation = ((LivingEntity) target).getEyeLocation();
            } else {
                targetLocation = target.getLocation();
            }

            Vector clone = towards.clone();
            Vector targetDirection = targetLocation.toVector().subtract(lastLocation.toVector());
            float angle = clone.angle(targetDirection);
            Vector crossProduct = clone.clone().getCrossProduct(targetDirection);
            // make sure path is a circle
            if (lastCorrected > 0) {
                clone.rotateAroundAxis(crossProduct, lastCorrected);
            }
            // legacy
            //            double actualAng = (homing / 20) / (lengthInThisTick / lengthPerSpawn);
            double actualAng = Math.asin(towards.length() / (2 * homing));
            if (angle > actualAng) {
                if (this.behavior.contains(Behavior.LEGACY_HOMING)) {
                    double lastActualAngle =
                            Math.asin(towards.length() / (2 * (homing + legacyBonus)));
                    legacyBonus += (lastActualAngle / (Math.PI));
                    actualAng = Math.asin(towards.length() / (2 * (homing + legacyBonus)));
                }
                // ↓a better way to rotate.
                // will create a exact circle.
                clone.rotateAroundAxis(crossProduct, actualAng);
                lastCorrected = actualAng;
            } else {
                clone = targetDirection.normalize();
                lastCorrected = 0;
            }
            return clone;
        }

        private double spawnInWorld = 0;

        private void spawnParticle(Entity from, World world, Location lastLocation, int i) {
            Location eyeLocation;
            if (this.mode.equals(Mode.PROJECTILE)
                    && this.behavior.contains(Behavior.RAINBOW_COLOR)
                    && extraData instanceof Particle.DustOptions) {
                extraData =
                        new Particle.DustOptions(
                                getNextColor(), ((Particle.DustOptions) extraData).getSize());
            }
            if (from instanceof Player) {
                eyeLocation = ((Player) from).getEyeLocation();
                if ((lastLocation.distance(eyeLocation) < 1)) {
                    return;
                }

                // color for note need count to be 0.
                if (this.particle.equals(Particle.NOTE)) {
                    i = 0;
                }
                if (spawnInWorld >= 3) {
                    ((Player) from)
                            .spawnParticle(
                                    this.particle,
                                    lastLocation,
                                    i,
                                    offsetX,
                                    offsetY,
                                    offsetZ,
                                    particleSpeed,
                                    extraData);
                    spawnInWorld = 0;
                } else {
                    world.spawnParticle(
                            this.particle,
                            lastLocation,
                            i,
                            offsetX,
                            offsetY,
                            offsetZ,
                            particleSpeed,
                            extraData,
                            false);
                }
                spawnInWorld++;
            } else {
                world.spawnParticle(
                        this.particle,
                        lastLocation,
                        i,
                        offsetX,
                        offsetY,
                        offsetZ,
                        particleSpeed,
                        extraData,
                        false);
            }
        }

        private Collection<? extends UUID> tryHit(
                Entity from, Location loc, ItemStack stack, boolean canHitSelf, Set<UUID> hitMob) {
            HashSet<UUID> hitMobs = new HashSet<>();
            if (from == null || this.effectOnly) return hitMobs;
            double offsetLength = new Vector(offsetX, offsetY, offsetZ).length();
            double length = Double.isNaN(offsetLength) ? 0.1 : Math.max(offsetLength, 10);
            Collection<Entity> candidates =
                    from.getWorld().getNearbyEntities(loc, length, length, length);
            List<Entity> collect =
                    candidates.stream()
                            .filter(
                                    entity ->
                                            (entity instanceof LivingEntity)
                                                    && (!isUtilArmorStand((LivingEntity) entity))
                                                    && (canHitSelf || !entity.equals(from))
                                                    && !entity.isDead()
                                                    && !hitMob.contains(entity.getUniqueId()))
                            .filter(entity -> canHit(loc, entity))
                            .limit(Math.max(pierce, 1))
                            .collect(Collectors.toList());
            if (!collect.isEmpty()) {
                Entity entity = collect.get(0);
                if (entity instanceof LivingEntity) {
                    BeamHitEntityEvent beamHitEntityEvent =
                            new BeamHitEntityEvent(
                                    player,
                                    from,
                                    ((LivingEntity) entity),
                                    stack,
                                    damage,
                                    loc,
                                    getBoundingBox(loc),
                                    towards.clone()
                                            .normalize()
                                            .multiply(getNextLength(spawnedLength, length * 20)),
                                    triggerDepth);
                    Bukkit.getPluginManager().callEvent(beamHitEntityEvent);
                    double damage = beamHitEntityEvent.getDamage();
                    if (damage > 0) {
                        LightContext.putTemp(
                                from.getUniqueId(),
                                DAMAGE_SOURCE,
                                power.getNamespacedKey().toString());
                        LightContext.putTemp(from.getUniqueId(), OVERRIDING_DAMAGE, damage);
                        LightContext.putTemp(from.getUniqueId(), SUPPRESS_MELEE, suppressMelee);
                        LightContext.putTemp(from.getUniqueId(), DAMAGE_SOURCE_ITEM, stack);
                        ((LivingEntity) entity).damage(damage, from);
                        LightContext.clear();
                    }
                    hitMobs.add(entity.getUniqueId());
                }
            }
            return hitMobs;
        }

        private boolean canHit(Location loc, Entity entity) {
            BoundingBox boundingBox = entity.getBoundingBox();
            BoundingBox particleBox = getBoundingBox(loc);

            return boundingBox.overlaps(particleBox) || particleBox.overlaps(boundingBox);
        }

        private BoundingBox getBoundingBox(Location loc) {
            double initalBias = 0.2;
            double x = Math.max(offsetX, initalBias);
            double y = Math.max(offsetY, initalBias);
            double z = Math.max(offsetZ, initalBias);
            return BoundingBox.of(loc, x + initalBias, y + initalBias, z + initalBias);
        }

        private double getGravity(double partsPerTick) {
            double gravityPerTick = (-gravity / 20d) / partsPerTick;
            gravityVector.setY(gravityVector.getY() + gravityPerTick);
            return (gravityVector.getY() / 20d) / partsPerTick;
        }

        public void setTarget(Queue<Entity> targets) {
            this.targets = targets;
        }

        public void setFromEntity(Entity fromEntity) {
            this.fromEntity = fromEntity;
            if (fromEntity instanceof LivingEntity) {
                if (fromEntity instanceof Player) {
                    MainHand mainHand = ((Player) fromEntity).getMainHand();
                    Vector direction = ((Player) fromEntity).getEyeLocation().getDirection();
                    Vector upDirection = crossProduct(direction).multiply(this.initialBias);
                    if (mainHand.equals(MainHand.LEFT)) {
                        upDirection.rotateAroundAxis(direction, Math.toRadians(60));
                    } else if (mainHand.equals(MainHand.RIGHT)) {
                        upDirection.rotateAroundAxis(direction, Math.toRadians(-60));
                    }
                    this.fromLocation = ((LivingEntity) fromEntity).getEyeLocation();

                    fromLocation.add(upDirection);
                } else {
                    this.fromLocation = ((LivingEntity) fromEntity).getEyeLocation();
                }
            } else {
                this.fromLocation = fromEntity.getLocation();
            }
            this.towards = fromLocation.getDirection();
        }

        final Vector yAxis = new Vector(0, 1, 0);
        final Vector xAxis = new Vector(1, 0, 0);

        private Vector crossProduct(Vector towards) {
            Vector cross1 = null;
            Vector upDirection = null;
            if (towards.getX() == 0 && towards.getZ() == 0) {
                cross1 = towards.clone().getCrossProduct(xAxis);
                upDirection = xAxis;
            } else {
                cross1 = towards.clone().getCrossProduct(yAxis);
                upDirection = towards.clone().getCrossProduct(cross1);
            }
            return upDirection.normalize();
        }

        public void setFromLocation(Location from) {
            this.fromLocation = from;
        }

        public void setTowards(Vector towards) {
            this.towards = towards;
        }
    }

    public static class ExtraDataSerializer implements Getter<Object>, Setter<Object> {
        @Override
        public String get(Object object) {
            if (object instanceof Particle.DustOptions) {
                Color color = ((Particle.DustOptions) object).getColor();
                return color.getRed()
                        + ","
                        + color.getGreen()
                        + ","
                        + color.getBlue()
                        + ","
                        + ((Particle.DustOptions) object).getSize();
            }
            return "";
        }

        @Override
        public Optional set(String value) throws IllegalArgumentException {
            if ("null".equals(value)) {
                return Optional.empty();
            }
            String[] split = value.split(",", 4);
            int r = Integer.parseInt(split[0]);
            int g = Integer.parseInt(split[1]);
            int b = Integer.parseInt(split[2]);
            float size = Float.parseFloat(split[3]);
            return Optional.of(new Particle.DustOptions(Color.fromRGB(r, g, b), size));
        }
    }

    // can be called anywhere, maybe
    public static class MovingTaskBuilder {
        MovingTask movingTask;

        public MovingTaskBuilder(Beam power) {
            this.movingTask = new MovingTask(power);
        }

        public MovingTaskBuilder towards(Vector towards) {
            movingTask.setTowards(towards);
            return this;
        }

        public MovingTaskBuilder fromLocation(Location location) {
            movingTask.setFromLocation(location);
            return this;
        }

        public MovingTaskBuilder fromEntity(Entity entity) {
            movingTask.setFromEntity(entity);
            return this;
        }

        public MovingTaskBuilder targets(Queue<Entity> targets) {
            movingTask.setTarget(targets);
            return this;
        }

        public MovingTaskBuilder itemStack(ItemStack stack) {
            movingTask.setItemStack(stack);
            return this;
        }

        public MovingTask build() {
            return movingTask;
        }

        public MovingTaskBuilder color(Color nextColor) {
            if (movingTask.extraData != null
                    && movingTask.extraData instanceof Particle.DustOptions) {
                movingTask.extraData =
                        new Particle.DustOptions(
                                nextColor, ((Particle.DustOptions) movingTask.extraData).getSize());
            }
            return this;
        }

        public MovingTaskBuilder firingR(double r) {
            movingTask.firingR = r;
            return this;
        }

        public MovingTaskBuilder triggerDepth(int depth) {
            movingTask.triggerDepth = depth;
            return this;
        }

        public MovingTaskBuilder player(Player player) {
            movingTask.player = player;
            return this;
        }
    }

    private static List<Entity> getTargets(
            Vector direction,
            Location fromLocation,
            Entity from,
            double range,
            double homingAngle,
            Target homingTarget) {
        double radius = Math.min(range, 300);
        return Utils.getLivingEntitiesInConeSorted(
                        from.getNearbyEntities(radius, range * 1.5, range * 1.5).stream()
                                .filter(
                                        entity ->
                                                entity instanceof LivingEntity
                                                        && !entity.equals(from)
                                                        && !isUtilArmorStand(entity)
                                                        && !entity.getScoreboardTags()
                                                                .contains(INVALID_TARGET)
                                                        && !entity.isDead()
                                                        && from.getLocation()
                                                                        .distance(
                                                                                entity
                                                                                        .getLocation())
                                                                < range)
                                .map(entity -> ((LivingEntity) entity))
                                .collect(Collectors.toList()),
                        fromLocation.toVector(),
                        homingAngle,
                        direction)
                .stream()
                .filter(
                        livingEntity -> {
                            switch (homingTarget) {
                                case MOBS:
                                    return !(livingEntity instanceof Player);
                                case PLAYERS:
                                    return livingEntity instanceof Player
                                            && !((Player) livingEntity)
                                                    .getGameMode()
                                                    .equals(GameMode.SPECTATOR);
                                case ALL:
                                    return !(livingEntity instanceof Player)
                                            || !((Player) livingEntity)
                                                    .getGameMode()
                                                    .equals(GameMode.SPECTATOR);
                            }
                            return true;
                        })
                .collect(Collectors.toList());
    }

    enum Mode {
        BEAM,
        PROJECTILE,
        ;
    }

    enum Target {
        MOBS,
        PLAYERS,
        ALL
    }

    enum HomingMode {
        ONE_TARGET,
        MULTI_TARGET,
        MOUSE_TRACK
    }

    enum FiringLocation {
        SELF,
        TARGET;
    }

    public enum Behavior {
        PLAIN(PlainBias.class, Void.class),
        DNA(DnaBias.class, DnaBias.DnaParams.class),
        CIRCLE(CircleBias.class, CircleBias.CircleParams.class),
        LEGACY_HOMING(PlainBias.class, Void.class),
        RAINBOW_COLOR(RainbowColor.class, Void.class),
        CONED(Coned.class, Void.class),
        FLAT(Flat.class, Void.class),
        UNIFORMED(Uniformed.class, Void.class),
        CAST_LOCATION_ROTATED(CastLocationRotated.class, Void.class);

        private Class<? extends IBias> iBias;
        private Class<?> paramType;

        Behavior(Class<? extends IBias> iBias, Class<?> paramType) {
            this.iBias = iBias;
            this.paramType = paramType;
        }

        public List<Vector> getBiases(
                Location location, Vector towards, MovingTask context, String params) {
            return null;
        }
    }

    // behavior params
    // todo: config them in specific class
    static int currentColor = 0;
    static final Color[] colors = {
        Color.fromRGB(0xFF2040), // RED
        Color.fromRGB(0xFF9020), // ORANGE
        Color.fromRGB(0xFFFF40), // YELLOW
        Color.fromRGB(0x40FF40), // LIME
        Color.fromRGB(0x40FFFF), // AQUA
        Color.fromRGB(0x4040FF), // BLUE
        Color.fromRGB(0xFF40FF) // FUCHSIA
    };

    static Color getNextColor() {
        int l = (int) ((System.currentTimeMillis() / 200) % colors.length);
        Color r = colors[l];
        return r;
    }

    interface IBias<T> {
        List<Vector> getBiases(Location location, Vector towards, MovingTask context, T params);
    }

    static class RainbowColor implements IBias<Void> {
        @Override
        public List<Vector> getBiases(
                Location location, Vector towards, MovingTask context, Void params) {
            return null;
        }
    }

    static class Flat implements IBias<Void> {
        @Override
        public List<Vector> getBiases(
                Location location, Vector towards, MovingTask context, Void params) {
            return null;
        }
    }

    static class Coned implements IBias<Void> {
        @Override
        public List<Vector> getBiases(
                Location location, Vector towards, MovingTask context, Void params) {
            return null;
        }
    }

    static class PlainBias implements IBias<Void> {
        @Override
        public List<Vector> getBiases(
                Location location, Vector towards, MovingTask context, Void params) {
            return null;
        }
    }

    static class CircleBias implements IBias<CircleBias.CircleParams> {
        private CircleParams params;

        @Override
        public List<Vector> getBiases(
                Location location, Vector towards, MovingTask context, CircleParams params) {
            return null;
        }

        static class CircleParams {
            public double r = 1;
            public String rFunc = "";
        }
    }

    static class DnaBias implements IBias<DnaBias.DnaParams> {
        @Override
        public List<Vector> getBiases(
                Location location, Vector towards, MovingTask context, DnaParams params) {
            return null;
        }

        static class DnaParams {
            double amount = 2;
            double r = 1;
            String rFunc = "";
        }
    }

    static class Uniformed implements IBias<Void> {
        @Override
        public List<Vector> getBiases(
                Location location, Vector towards, MovingTask context, Void params) {
            return null;
        }
    }

    private static class CastLocationRotated implements IBias<Void> {
        @Override
        public List<Vector> getBiases(
                Location location, Vector towards, MovingTask context, Void params) {
            return null;
        }
    }

    private final Vector yAxis = new Vector(0, 1, 0);

    private static Queue<RoundedConeInfo> internalCones(Beam beam, int amount, int burstCount) {
        List<Behavior> behaviors = beam.getBehavior();
        boolean uniformed = behaviors.contains(Behavior.UNIFORMED);
        boolean flat = behaviors.contains(Behavior.FLAT);
        Queue<RoundedConeInfo> infos = new LinkedList<>();
        for (int i = 0; i < burstCount; i++) {
            int steps = Math.max(amount, 1);
            double phiStep = 360 / steps;
            double thetaStep = beam.getCone() * 2 / steps;
            for (int j = 0; j < amount; j++) {
                RoundedConeInfo roundedConeInfo = internalCone(beam);
                if (behaviors.contains(Behavior.CONED)) {
                    roundedConeInfo.setTheta(beam.getCone());
                }
                if (uniformed) {
                    roundedConeInfo.setPhi(j * phiStep);
                    roundedConeInfo.setRPhi(beam.getFiringPhi().uniformed(j, steps));
                }
                if (flat) {
                    if (uniformed) {
                        roundedConeInfo.setTheta((thetaStep * j) - beam.getCone());
                        roundedConeInfo.setPhi(90);
                    } else {
                        roundedConeInfo.setPhi(random.nextBoolean() ? 90 : 270);
                    }
                }

                infos.offer(roundedConeInfo);
            }
        }
        return infos;
    }

    private static RoundedConeInfo internalCone(Beam beam) {
        double phi = random.nextDouble() * 360;
        double theta = 0;
        if (beam.getCone() != 0) {
            theta = random.nextDouble() * beam.getCone();
        }

        double r = beam.getFiringR().random();
        double rPhi = beam.getFiringPhi().random();
        double rTheta = beam.getFiringTheta().random();

        return new RoundedConeInfo(theta, phi, r, rPhi, rTheta, beam.getInitialRotation());
    }

    public static class Impl
            implements PowerPlain<Beam>,
                    PowerRightClick<Beam>,
                    PowerLeftClick<Beam>,
                    PowerSneak<Beam>,
                    PowerSneaking<Beam>,
                    PowerSprint<Beam>,
                    PowerBowShoot<Beam>,
                    PowerHitTaken<Beam>,
                    PowerHit<Beam>,
                    PowerHurt<Beam>,
                    PowerTick<Beam>,
                    PowerBeamHit<Beam>,
                    PowerLivingEntity<Beam> {
        @Override
        public PowerResult<Void> leftClick(
                Beam power, Player player, ItemStack stack, PlayerInteractEvent event) {
            return fire(power, player, stack);
        }

        @Override
        public PowerResult<Void> fire(Beam power, Player player, ItemStack stack) {
            return beam(power, player, player, stack);
        }

        public PowerResult<Void> fire(
                Beam power,
                Player player,
                ItemStack stack,
                Location castLocation,
                LivingEntity target,
                int depth) {
            CastUtils.CastLocation loc = CastUtils.of(castLocation, target, power.yAxis.clone());
            return beam(power, player, player, stack, loc, depth);
        }

        @Override
        public Class<? extends Beam> getPowerClass() {
            return Beam.class;
        }

        private PowerResult<Void> beam(
                Beam power, Player player, LivingEntity from, ItemStack stack) {
            Location fromLocation = from.getEyeLocation().clone();
            Vector towards = from.getEyeLocation().getDirection().clone();

            CastUtils.CastLocation result =
                    CastUtils.rayTrace(from, fromLocation, towards, power.getFiringRange());
            return beam(power, player, from, stack, result, 0);
        }

        private PowerResult<Void> beam(
                Beam power,
                Player player,
                LivingEntity from,
                ItemStack stack,
                CastUtils.CastLocation castLocation,
                int depth) {
            Location fromLocation = from.getEyeLocation().clone();
            Vector towards = from.getEyeLocation().getDirection().clone();
            Vector normal = power.yAxis.clone();

            Queue<RoundedConeInfo> roundedConeInfo =
                    internalCones(power, power.getBeamAmount(), Math.max(power.getBurstCount(), 1));
            Deque<Entity> targets = null;

            if (power.getHoming() > 0) {
                targets =
                        new LinkedList<>(
                                getTargets(
                                        towards,
                                        fromLocation,
                                        from,
                                        power.getHomingRange(),
                                        power.getHomingAngle(),
                                        power.getHomingTarget()));
            }
            if (castLocation != null) {
                fromLocation = castLocation.getTargetLocation();
                normal = castLocation.getNormalDirection();
                if (castLocation.getHitEntity() != null && targets != null) {
                    targets.addFirst(castLocation.getHitEntity());
                }
            }

            if (power.getBurstCount() > 0) {
                final int currentBurstCount = power.getBurstCount();
                final int currentBurstInterval = power.getBurstInterval();
                AtomicInteger bursted = new AtomicInteger(0);

                Location finalFromLocation = fromLocation;
                Vector finalNormal = normal;
                Deque<Entity> finalTargets = targets;
                class FireTask extends BukkitRunnable {
                    @Override
                    public void run() {
                        for (int j = 0; j < power.getBeamAmount(); j++) {
                            internalFireBeam(
                                    power,
                                    player,
                                    from,
                                    finalFromLocation,
                                    towards,
                                    finalNormal,
                                    stack,
                                    roundedConeInfo,
                                    finalTargets,
                                    depth);
                        }
                        if (bursted.addAndGet(1) < currentBurstCount) {
                            new FireTask().runTaskLater(RPGItems.plugin, currentBurstInterval);
                        }
                    }
                }
                new FireTask().runTask(RPGItems.plugin);
                return PowerResult.ok();
            } else {
                return internalFireBeam(
                        power, player, from, stack, roundedConeInfo, targets, depth);
            }
        }

        private PowerResult<Void> internalFireBeam(
                Beam power,
                Player player,
                LivingEntity from,
                ItemStack stack,
                Queue<RoundedConeInfo> coneInfo,
                Deque<Entity> targets,
                int depth) {
            return internalFireBeam(
                    power,
                    player,
                    from,
                    from.getEyeLocation(),
                    from.getEyeLocation().getDirection(),
                    power.yAxis.clone(),
                    stack,
                    coneInfo,
                    targets,
                    depth);
        }

        private PowerResult<Void> internalFireBeam(
                Beam power,
                Player player,
                LivingEntity from,
                Location castLocation,
                Vector towards,
                Vector normalDir,
                ItemStack stack,
                Queue<RoundedConeInfo> coneInfo,
                Deque<Entity> targets,
                int depth) {
            Location fromLocation = castLocation;

            if (!power.isCastOff()) {
                fromLocation = from.getEyeLocation();
                towards = from.getEyeLocation().getDirection();
                if (power.getHoming() > 0
                        && (!power.isCastOff()
                                || power.getHomingMode().equals(HomingMode.MULTI_TARGET)
                                || power.getHomingMode().equals(HomingMode.MOUSE_TRACK))) {
                    targets =
                            new LinkedList<>(
                                    getTargets(
                                            towards,
                                            fromLocation,
                                            from,
                                            power.getHomingRange(),
                                            power.getHomingAngle(),
                                            power.getHomingTarget()));
                }

                if (power.getFiringLocation().equals(FiringLocation.TARGET)) {
                    CastUtils.CastLocation result =
                            CastUtils.rayTrace(from, fromLocation, towards, power.getFiringRange());
                    castLocation = result.getTargetLocation();
                    normalDir = result.getNormalDirection();
                }
            }

            RoundedConeInfo poll = coneInfo.poll();
            if (poll == null) {
                poll = internalCone(power);
            }

            if (power.getFiringLocation().equals(FiringLocation.TARGET)) {
                if (!power.getBehavior().contains(Behavior.CAST_LOCATION_ROTATED)) {
                    normalDir = power.yAxis.clone();
                }
                fromLocation =
                        CastUtils.parseFiringLocation(castLocation, normalDir, fromLocation, poll);
                towards = castLocation.clone().subtract(fromLocation).toVector();
            }

            towards = makeCone(fromLocation, towards, poll);

            MovingTaskBuilder movingTaskBuilder =
                    new MovingTaskBuilder(power)
                            .player(player)
                            .fromEntity(from)
                            .towards(towards)
                            .targets(targets)
                            .itemStack(stack)
                            .triggerDepth(depth);
            if (power.getBehavior().contains(Behavior.RAINBOW_COLOR)) {
                Color nextColor = getNextColor();
                movingTaskBuilder.color(nextColor);
            }
            if (!power.getFiringLocation().equals(FiringLocation.SELF)) {
                movingTaskBuilder.fromLocation(fromLocation).firingR(poll.getR());
            }
            MovingTask movingTask = movingTaskBuilder.build();
            movingTask.runTask(RPGItems.plugin);
            return PowerResult.ok();
        }

        @Override
        public PowerResult<Void> rightClick(
                Beam power, Player player, ItemStack stack, PlayerInteractEvent event) {
            return fire(power, player, stack);
        }

        @Override
        public PowerResult<Void> sneak(
                Beam power, Player player, ItemStack stack, PlayerToggleSneakEvent event) {
            return fire(power, player, stack);
        }

        @Override
        public PowerResult<Void> sneaking(Beam power, Player player, ItemStack stack) {
            return fire(power, player, stack);
        }

        @Override
        public PowerResult<Void> sprint(
                Beam power, Player player, ItemStack stack, PlayerToggleSprintEvent event) {
            return fire(power, player, stack);
        }

        @Override
        public PowerResult<Float> bowShoot(
                Beam power, Player player, ItemStack itemStack, EntityShootBowEvent e) {
            return fire(power, player, itemStack).with(e.getForce());
        }

        @Override
        public PowerResult<Double> hit(
                Beam power,
                Player player,
                ItemStack stack,
                LivingEntity entity,
                double damage,
                EntityDamageByEntityEvent event) {
            return fire(power, player, stack, entity.getLocation(), entity, 1)
                    .with(event.getDamage());
        }

        @Override
        public PowerResult<Double> takeHit(
                Beam power,
                Player target,
                ItemStack stack,
                double damage,
                EntityDamageEvent event) {
            if (!power.isRequireHurtByEntity() || event instanceof EntityDamageByEntityEvent) {
                return fire(power, target, stack).with(event.getDamage());
            }
            return PowerResult.noop();
        }

        @Override
        public PowerResult<Void> hurt(
                Beam power, Player target, ItemStack stack, EntityDamageEvent event) {
            if (!power.isRequireHurtByEntity() || event instanceof EntityDamageByEntityEvent) {
                return fire(power, target, stack);
            }
            return PowerResult.noop();
        }

        @Override
        public PowerResult<Void> tick(Beam power, Player player, ItemStack stack) {
            return fire(power, player, stack);
        }

        @Override
        public PowerResult<Double> hitEntity(
                Beam power,
                Player player,
                ItemStack stack,
                LivingEntity entity,
                double damage,
                BeamHitEntityEvent event) {
            if (event.getDepth() >= 1) return PowerResult.noop();
            return fire(power, player, stack, event.getLoc(), entity, event.getDepth() + 1)
                    .with(damage);
        }

        @Override
        public PowerResult<Void> hitBlock(
                Beam power,
                Player player,
                ItemStack stack,
                Location location,
                BeamHitBlockEvent event) {
            if (event.getDepth() >= 1) return PowerResult.noop();
            return fire(power, player, stack, location, null, event.getDepth() + 1);
        }

        @Override
        public PowerResult<Void> beamEnd(
                Beam power, Player player, ItemStack stack, Location location, BeamEndEvent event) {
            if (event.getDepth() >= 1) return PowerResult.noop();
            return fire(power, player, stack, location, null, event.getDepth() + 1);
        }

        @Override
        public PowerResult<Void> fire(
                Beam power,
                Player player,
                ItemStack stack,
                LivingEntity entity,
                @Nullable Double value) {
            return beam(power, player, entity, stack);
        }
    }
}
