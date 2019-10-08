package think.rpgitems.power.impl;

import com.google.common.util.concurrent.AtomicDouble;
import com.udojava.evalex.Expression;
import org.bukkit.*;
import org.bukkit.block.Block;
import org.bukkit.configuration.ConfigurationSection;
import org.bukkit.entity.ArmorStand;
import org.bukkit.entity.Entity;
import org.bukkit.entity.LivingEntity;
import org.bukkit.entity.Player;
import org.bukkit.event.entity.EntityDamageByEntityEvent;
import org.bukkit.event.entity.EntityDamageEvent;
import org.bukkit.event.entity.EntityShootBowEvent;
import org.bukkit.event.player.PlayerInteractEvent;
import org.bukkit.event.player.PlayerToggleSneakEvent;
import org.bukkit.event.player.PlayerToggleSprintEvent;
import org.bukkit.inventory.ItemStack;
import org.bukkit.scheduler.BukkitRunnable;
import org.bukkit.util.BoundingBox;
import org.bukkit.util.RayTraceResult;
import org.bukkit.util.Vector;
import org.librazy.nclangchecker.LangKey;
import think.rpgitems.RPGItems;
import think.rpgitems.data.LightContext;
import think.rpgitems.power.*;

import java.math.BigDecimal;
import java.util.*;
import java.util.concurrent.atomic.AtomicInteger;
import java.util.stream.Collectors;
import java.util.stream.Stream;

import static think.rpgitems.Events.*;
import static think.rpgitems.power.Utils.checkCooldown;

/**
 * @Author ReinWD
 * @email ReinWDD@gmail.com
 * Wrote & Maintained by ReinWD
 * if you have any issue, please send me email or @ReinWD in issues.
 * Accepted language: 中文, English.
 * <p>
 * Beam version 2.0
 */
@PowerMeta(defaultTrigger = "RIGHT_CLICK", generalInterface = PowerPlain.class)
public class PowerBeam extends BasePower implements PowerPlain, PowerRightClick, PowerLeftClick, PowerSneak, PowerSneaking, PowerSprint, PowerBowShoot, PowerHitTaken, PowerHit, PowerHurt {
    @Property
    public int length = 10;

    @Property
    public Particle particle = Particle.LAVA;

    @Property
    public Mode mode = Mode.BEAM;

    @Property
    public int pierce = 0;

    @Property
    public boolean ignoreWall = true;

    @Property
    public double damage = 20;

    @Property
    public int speed = 20;
//    public int movementTicks = 40;

    @Property
    public double offsetX = 0;

    @Property
    public double offsetY = 0;

    @Property
    public double offsetZ = 0;

    @Property
    public double particleSpeed = 0;

    @Property
    public double spawnsPerBlock = 2;

    /**
     * Cost of this power
     */
    @Property
    public int cost = 0;
    /**
     * Cooldown time of this power
     */
    @Property
    public long cooldown = 0;

//  used to judge legacy 1.0
//    @Property
//    public boolean cone = false;

    @Property
    public double cone = 10;
//    changed Name
//    public double coneRange = 30;

    @Property
    public double homing = 0;

    @Property
    public double homingAngle = 30;

//    @Property
//    public double homingRange = 30;

    @Property
    public HomingMode homingMode = HomingMode.ONE_TARGET;
//    public HomingTargetMode homingTargetMode = HomingTargetMode.ONE_TARGET;

    @Property
    public Target homingTarget = Target.MOBS;

    @Property
    public int stepsBeforeHoming = 0;

    @Property
    public int burstCount = 1;

    @Property
    public int beamAmount = 1;

    @Property
    public int burstInterval = 10;

    @Property
    public int bounce = 0;

    @Property
    public boolean hitSelfWhenBounced = false;

    @Property
    public double gravity = 0;

    @Property
    @Serializer(ExtraDataSerializer.class)
    @Deserializer(ExtraDataSerializer.class)
    public Object extraData;

    @Property
    public boolean requireHurtByEntity = true;


    /**
     * Whether to suppress the hit trigger
     */
    @Property
    public boolean suppressMelee = false;

    @Property
    public String speedBias = "";

    @Property
    public BeamShape shape = BeamShape.PLAIN;

    @Property
    public String shapeParam = "{}";

    @Override
    public void init(ConfigurationSection section) {
        if (section.contains("coneRange")) {
            updateFromV1(section);
        }
        super.init(section);
    }

    private void updateFromV1(ConfigurationSection section) {
        double cone = section.getDouble("coneRange");
        int movementTicks = section.getInt("movementTicks");
        int length = section.getInt("length");
        double originSpeed = section.getDouble("speed");
        double homingAngle = section.getDouble("homingAngle");
        double homingRange = section.getDouble("homingRange");
        String homingTargetMode = section.getString("homingTargetMode");
        section.set("cone", cone);
        section.set("speed", ((double) length) / ((double) movementTicks));
        section.set("particleSpeed", originSpeed);
        section.set("homing", homingAngle);
        section.set("homingAngle", homingRange);
        section.set("homingMode", homingTargetMode);
    }

    private static Set<Material> transp = Stream.of(Material.values())
            .filter(material -> material.isBlock())
            .filter(material -> !material.isSolid() || !material.isOccluding())
            .collect(Collectors.toSet());

    final Vector crosser = new Vector(1, 1, 1);
    private Random random = new Random();

    @Override
    public @LangKey(skipCheck = true) String getName() {
        return "beam";
    }

    @Override
    public String displayText() {
        return null;
    }

    @Override
    public PowerResult<Void> fire(Player player, ItemStack stack) {
        if (!checkCooldown(this, player, cooldown, true, true)) return PowerResult.cd();
        if (!getItem().consumeDurability(stack, cost)) return PowerResult.cost();
        return beam(player, stack);
    }

    @Override
    public PowerResult<Void> leftClick(Player player, ItemStack stack, PlayerInteractEvent event) {
        return fire(player, stack);
    }

    @Override
    public PowerResult<Void> rightClick(Player player, ItemStack stack, PlayerInteractEvent event) {
        return fire(player, stack);
    }

    @Override
    public PowerResult<Void> sneak(Player player, ItemStack stack, PlayerToggleSneakEvent event) {
        return fire(player, stack);
    }

    @Override
    public PowerResult<Void> sneaking(Player player, ItemStack stack) {
        return fire(player, stack);
    }

    @Override
    public PowerResult<Void> sprint(Player player, ItemStack stack, PlayerToggleSprintEvent event) {
        return fire(player, stack);
    }

    @Override
    public PowerResult<Float> bowShoot(Player player, ItemStack itemStack, EntityShootBowEvent e) {
        return fire(player, itemStack).with(e.getForce());
    }

    @Override
    public PowerResult<Double> hit(Player player, ItemStack stack, LivingEntity entity, double damage, EntityDamageByEntityEvent event) {
        return fire(player, stack).with(event.getDamage());
    }

    @Override
    public PowerResult<Double> takeHit(Player target, ItemStack stack, double damage, EntityDamageEvent event) {
        if (!requireHurtByEntity || event instanceof EntityDamageByEntityEvent) {
            return fire(target, stack).with(event.getDamage());
        }
        return PowerResult.noop();
    }

    private PowerResult<Void> beam(LivingEntity from, ItemStack stack) {
        if (burstCount > 0) {
            final int currentBurstCount = burstCount;
            final int currentBurstInterval = burstInterval;
            AtomicInteger bursted = new AtomicInteger(0);
            class FireTask extends BukkitRunnable {
                @Override
                public void run() {
                    for (int j = 0; j < beamAmount; j++) {
                        internalFireBeam(from, stack);
                    }
                    if (bursted.getAndAdd(1) < currentBurstCount) {
                        new FireTask().runTaskLaterAsynchronously(RPGItems.plugin, currentBurstInterval);
                    }
                }
            }
            new FireTask().runTaskAsynchronously(RPGItems.plugin);
            return PowerResult.ok();
        } else {
            return internalFireBeam(from, stack);
        }
    }

    private PowerResult<Void> internalFireBeam(LivingEntity from, ItemStack stack) {
        Location fromLocation = from.getEyeLocation();
        Vector towards = from.getEyeLocation().getDirection();

        if (cone != 0) {
            double phi = random.nextDouble() * 360;
            double theta;
            theta = random.nextDouble() * cone;
            Vector clone = towards.clone();
            Vector cross = clone.clone().add(crosser);
            Vector vertical = clone.getCrossProduct(cross).getCrossProduct(towards);
            towards.rotateAroundAxis(vertical, Math.toRadians(theta));
            towards.rotateAroundAxis(clone, Math.toRadians(phi));
        }


        Queue<Entity> targets = null;
        if (from instanceof Player && homing > 0) {
            targets = new LinkedList<>(getTargets(from.getEyeLocation().getDirection(), fromLocation, from));
        }
        MovingTask movingTask = new MovingTaskBuilder(this)
                .fromEntity(from)
                .towards(towards)
                .targets(targets)
                .itemStack(stack)
                .build();
        movingTask.runTask(RPGItems.plugin);
        return PowerResult.ok();
    }

    @Override
    public PowerResult<Void> hurt(Player target, ItemStack stack, EntityDamageEvent event) {
        if (!requireHurtByEntity || event instanceof EntityDamageByEntityEvent) {
            return fire(target, stack);
        }
        return PowerResult.noop();
    }

    private static class MovingTask extends BukkitRunnable {
        private int length = 10;
        private Particle particle = Particle.LAVA;
        private Mode mode = Mode.BEAM;
        private int pierce = 0;
        private boolean ignoreWall = true;
        private double damage = 20;
        private int speed = 20;
        private double offsetX = 0;
        private double offsetY = 0;
        private double offsetZ = 0;
        private double particleSpeed = 0;
        private double spawnsPerBlock = 2;
        private double homing = 0;
        private HomingMode homingMode = HomingMode.ONE_TARGET;
        private int ticksBeforeHoming = 5;
        private int bounce = 0;
        private boolean hitSelfWhenBounced = false;
        private double gravity = 0;
        private boolean suppressMelee = false;
        private BeamShape shape = BeamShape.PLAIN;
        private String shapeParam = "{}";
        private Object extraData = null;
        private PowerBeam power;
        private String speedBias = "";

        private Queue<Entity> targets;
        private Entity fromEntity;
        private Location fromLocation;
        private Vector towards;
        double lengthPerSpawn;

        AtomicDouble lengthRemains = new AtomicDouble(0);
        AtomicDouble spawnedLength = new AtomicDouble(0);
        AtomicInteger currentStep = new AtomicInteger(0);
        Location lastLocation = fromLocation;
        private ItemStack itemStack;
        boolean bounced = false;
        World world;

        MovingTask() {

        }

        MovingTask(PowerBeam config) {
            this.length = config.length;
            this.particle = config.particle;
            this.mode = config.mode;
            this.pierce = config.pierce;
            this.ignoreWall = config.ignoreWall;
            this.damage = config.damage;
            this.speed = config.speed;
            this.offsetX = config.offsetX;
            this.offsetY = config.offsetY;
            this.offsetZ = config.offsetZ;
            this.spawnsPerBlock = config.spawnsPerBlock;
            this.homing = config.homing;
            this.homingMode = config.homingMode;
            this.ticksBeforeHoming = config.stepsBeforeHoming;
            this.bounce = config.bounce;
            this.hitSelfWhenBounced = config.hitSelfWhenBounced;
            this.gravity = config.gravity;
            this.particleSpeed = config.particleSpeed;
            this.suppressMelee = config.suppressMelee;
            this.shape = config.shape;
            this.shapeParam = config.shapeParam;
            this.extraData = config.extraData;
            this.speedBias = config.speedBias;
            power = config;
            lengthPerSpawn = 1 / spawnsPerBlock;
        }

        @Override
        public void run() {
            world = fromLocation.getWorld();
            if (world == null) return;
            if (Double.isInfinite(lengthPerSpawn)) {
                return;
            }
            if (mode.equals(Mode.BEAM)) {
                this.speed = 20 * length;
            }
            new RecursiveTask().runTaskAsynchronously(RPGItems.plugin);
        }

        public void setItemStack(ItemStack stack) {
            this.itemStack = stack;
        }

        class RecursiveTask extends BukkitRunnable {
            @Override
            public void run() {
                try {
                    double lengthInThisTick = getNextLength(spawnedLength, length) + lengthRemains.get();
                    int cycle = 0;
                    while ((lengthInThisTick -= lengthPerSpawn) > 0) {
                        double lengthInThisSpawn = lengthPerSpawn;
                        if (spawnedLength.get() + lengthInThisTick > length) {
                            lengthInThisSpawn = length - spawnedLength.get() + 0.1;
                        }
                        boolean isHit = tryHit(fromEntity, lastLocation, itemStack, bounced && hitSelfWhenBounced);
                        if (cycle++ > 2) {
                            if (!ignoreWall && !transp.contains(lastLocation.getBlock().getType())) {
                                if (bounce > 0) {
                                    bounce--;
                                    bounced = true;
                                    makeBounce(lastLocation.getBlock(), towards, lastLocation);
                                } else {
                                    return;
                                }
                            }
                            cycle = 0;
                        }
                        spawnParticle(fromEntity, world, lastLocation, 1);
                        Vector step = towards.normalize().multiply(lengthInThisSpawn);
                        lastLocation.add(step);
                        spawnedLength.addAndGet(lengthPerSpawn);
                        homingCorrect(towards, lastLocation, lengthInThisTick, targets.peek(), ticksBeforeHoming, () -> {
                            if (homingMode.equals(HomingMode.ONE_TARGET)) targets.poll();
                        });
                        if (isHit) {
                            if (pierce > 0) {
                                pierce--;
                                if (homingMode.equals(HomingMode.MULTI_TARGET)) {
                                    targets.poll();
                                }
                            } else {
                                return;
                            }
                        }
                    }
                    addGravity(towards, 1);
                    lengthRemains.set(lengthInThisTick + lengthPerSpawn);
                    if (spawnedLength.get() >= length) {
                        return;
                    }
                    new RecursiveTask().runTaskLaterAsynchronously(RPGItems.plugin, 1);
                } catch (Exception e) {
                    this.cancel();
                }
            }

            private double getNextLength(AtomicDouble spawnedLength, int length) {
                Expression eval = new Expression(speedBias).with("x", new Expression.LazyNumber() {
                    @Override
                    public BigDecimal eval() {
                        return BigDecimal.valueOf(spawnedLength.get() / ((double) length));
                    }

                    @Override
                    public String getString() {
                        return String.valueOf(spawnedLength.get() / ((double) length));
                    }
                });
                return speed * (eval.eval().doubleValue()) / 20;
            }
        }

//        private final LivingEntity from;
//        private int bounces;
//        private Vector towards;
//        private final ItemStack stack;
//        private final int amountPerSec;
//        private final List<BukkitRunnable> runnables = new LinkedList<>();
//        private Entity target;
//        boolean bounced = false;
//
//        public MovingTask(LivingEntity from, Vector towards, int apS, double actualLength, Entity target, int bounces, ItemStack stack) {
//            this.from = from;
//            this.towards = towards;
//            this.stack = stack;
//            this.amountPerSec = apS / ((int) Math.floor(actualLength));
//            this.target = target;
//            this.bounces = bounces;
//        }
//
//        @Override
//        public void run() {
//            World world = from.getWorld();
//            double lpT = ((double) length) / ((double) movementTicks);
//            double partsPerTick = lpT / lengthPerSpawn;
//            Location lastLocation = from.getEyeLocation();
//            towards.normalize();
//            final int[] finalI = {0};
//            BukkitRunnable bukkitRunnable = new BukkitRunnable() {
//                @Override
//                public void run() {
//                    try {
//                        boolean isStepHit = false;
//                        Vector step = new Vector(0, 0, 0);
//                        for (int k = 0; k < partsPerTick; k++) {
//                            boolean isHit = tryHit(from, lastLocation, stack, bounced && hitSelfWhenBounced);
//                            isStepHit = isHit || isStepHit;
//                            Block block = lastLocation.getBlock();
//                            if (transp.contains(block.getType())) {
//                                spawnParticle(from, world, lastLocation, (int) (amountPerSec / spawnsPerBlock));
//                            } else if (!ignoreWall) {
//                                if (bounces > 0) {
//                                    bounces--;
//                                    bounced = true;
//                                    makeBounce(block, towards, lastLocation.clone().subtract(step));
//                                } else {
//                                    this.cancel();
//                                    return;
//                                }
//                            }
//                            step = towards.clone().normalize().multiply(lengthPerSpawn);
//                            lastLocation.add(step);
//                            towards = addGravity(towards, partsPerTick);
//
//                            towards = homingCorrect(towards, lastLocation, target, finalI[0], () -> target = getNextTarget(from.getEyeLocation().getDirection(), from.getEyeLocation(), from));
//                        }
//                        if (isStepHit && homingTargetMode.equals(HomingTargetMode.MULTI_TARGET)) {
//                            target = getNextTarget(from.getEyeLocation().getDirection(), from.getEyeLocation(), from);
//                        }
//                        if (isStepHit && !pierce) {
//                            this.cancel();
//                            LightContext.clear();
//                            return;
//                        }
//                        if (finalI[0] >= movementTicks) {
//                            this.cancel();
//                            LightContext.clear();
//                        }
//                        finalI[0]++;
//                    } catch (Exception ex) {
//                        from.getServer().getLogger().log(Level.WARNING, "", ex);
//                        this.cancel();
//                        LightContext.clear();
//                    }
//                }
//            };
//            bukkitRunnable.runTaskTimer(RPGItems.plugin, 0, 1);
//        }

        private void makeBounce(Block block, Vector towards, Location lastLocation) {
            RayTraceResult rayTraceResult = block.rayTrace(lastLocation, towards, towards.length(), FluidCollisionMode.NEVER);
            if (rayTraceResult == null) {
                return;
            } else {
                towards.rotateAroundNonUnitAxis(rayTraceResult.getHitBlockFace().getDirection(), Math.toRadians(180)).multiply(-1);
            }
        }

        private Vector homingCorrect(Vector towards, Location lastLocation, double lengthInThisTick, Entity target, int i, Runnable runnable) {
            if (target == null || i < ticksBeforeHoming) {
                return towards;
            }
            if (target.isDead()) {
                runnable.run();
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
            double actualAng = homing / 20 / (lengthInThisTick / lengthPerSpawn);
            if (angle > Math.toRadians(actualAng)) {
                if (this.shape.equals(BeamShape.LEGACY_HOMING)) {
                    //↓a legacy but functionable way to rotate.
                    //will create a enlarging circle
                    clone.add(clone.clone().getCrossProduct(crossProduct).normalize().multiply(-1 * Math.tan(actualAng)));
                } else {
                    // ↓a better way to rotate.
                    // will create a exact circle.}
                    clone.rotateAroundAxis(crossProduct, actualAng);
                }
            } else {
                clone = targetDirection.normalize();
            }
            return clone;
        }

//        private LivingEntity getNextTarget(Vector towards, Location lastLocation, Entity from) {
//            int radius = Math.min(this.length, 300);
//            return Utils.getLivingEntitiesInCone(from.getNearbyEntities(radius, this.length, this.length).stream()
//                            .filter(entity -> entity instanceof LivingEntity && !entity.equals(from) && !entity.isDead())
//                            .map(entity -> ((LivingEntity) entity))
//                            .collect(Collectors.toList())
//                    , lastLocation.toVector(), homingAngle, towards).stream()
//                    .filter(livingEntity -> {
//                        if (isUtilArmorStand(livingEntity)) {
//                            return false;
//                        }
//                        switch (homingTarget) {
//                            case MOBS:
//                                return !(livingEntity instanceof Player);
//                            case PLAYERS:
//                                return livingEntity instanceof Player && !((Player) livingEntity).getGameMode().equals(GameMode.SPECTATOR);
//                            case ALL:
//                                return !(livingEntity instanceof Player) || !((Player) livingEntity).getGameMode().equals(GameMode.SPECTATOR);
//                        }
//                        return true;
//                    })
//                    .findFirst().orElse(null);
//        }

        private boolean spawnInWorld = false;

        private void spawnParticle(Entity from, World world, Location lastLocation, int i) {
            Location eyeLocation;
            if (from instanceof Player) {
                eyeLocation = ((Player) from).getEyeLocation();
                if ((lastLocation.distance(eyeLocation) < 1)) {
                    return;
                }
                if (spawnInWorld) {
                    ((Player) from).spawnParticle(this.particle, lastLocation, i / 2, offsetX, offsetY, offsetZ, particleSpeed, extraData);
                } else {
                    world.spawnParticle(this.particle, lastLocation, i, offsetX, offsetY, offsetZ, particleSpeed, extraData, false);
                }
                spawnInWorld = !spawnInWorld;
            } else {
                world.spawnParticle(this.particle, lastLocation, i, offsetX, offsetY, offsetZ, particleSpeed, extraData, false);
            }

        }

        private boolean tryHit(Entity from, Location loc, ItemStack stack, boolean canHitSelf) {
            if (from == null) return false;
            double offsetLength = new Vector(offsetX, offsetY, offsetZ).length();
            double length = Double.isNaN(offsetLength) ? 0 : Math.max(offsetLength, 10);
            Collection<Entity> candidates = from.getWorld().getNearbyEntities(loc, length, length, length);
            boolean result = false;
            if (pierce > 0) {
                List<Entity> collect = candidates.stream()
                        .filter(entity -> (entity instanceof LivingEntity) && (!isUtilArmorStand((LivingEntity) entity)) && (canHitSelf || !entity.equals(from)) && !entity.isDead())
                        .filter(entity -> canHit(loc, entity))
                        .limit(1)
                        .collect(Collectors.toList());
                if (!collect.isEmpty()) {
                    Entity entity = collect.get(0);
                    if (entity instanceof LivingEntity) {
                        LightContext.putTemp(from.getUniqueId(), DAMAGE_SOURCE, power.getNamespacedKey().toString());
                        LightContext.putTemp(from.getUniqueId(), OVERRIDING_DAMAGE, damage);
                        LightContext.putTemp(from.getUniqueId(), SUPPRESS_MELEE, suppressMelee);
                        LightContext.putTemp(from.getUniqueId(), DAMAGE_SOURCE_ITEM, stack);
                        ((LivingEntity) entity).damage(damage, from);
                        LightContext.clear();
                    }
                    return true;
                }
            } else {
                List<Entity> collect = candidates.stream()
                        .filter(entity -> (entity instanceof LivingEntity) && (!isUtilArmorStand((LivingEntity) entity)) && (canHitSelf || !entity.equals(from)))
                        .filter(entity -> canHit(loc, entity))
                        .collect(Collectors.toList());
                LightContext.putTemp(from.getUniqueId(), DAMAGE_SOURCE, power.getNamespacedKey().toString());
                LightContext.putTemp(from.getUniqueId(), OVERRIDING_DAMAGE, damage);
                LightContext.putTemp(from.getUniqueId(), SUPPRESS_MELEE, suppressMelee);
                LightContext.putTemp(from.getUniqueId(), DAMAGE_SOURCE_ITEM, stack);

                if (!collect.isEmpty()) {
                    collect.stream()
                            .map(entity -> ((LivingEntity) entity))
                            .forEach(livingEntity -> {
                                livingEntity.damage(damage, from);
                            });
                    result = true;
                }
                LightContext.clear();

            }
            return result;
        }

        private boolean canHit(Location loc, Entity entity) {
            BoundingBox boundingBox = entity.getBoundingBox();
            BoundingBox particleBox;
            double x = Math.max(offsetX, 0.1);
            double y = Math.max(offsetY, 0.1);
            double z = Math.max(offsetZ, 0.1);
            particleBox = BoundingBox.of(loc, x + 0.1, y + 0.1, z + 0.1);
            return boundingBox.overlaps(particleBox) || particleBox.overlaps(boundingBox);
        }

        Vector gravityVector = new Vector(0, -gravity / 20, 0);

        private Vector addGravity(Vector towards, double partsPerTick) {
            double gravityPerTick = (-gravity / 20d) / partsPerTick;
            gravityVector.setY(gravityPerTick);
            return towards.add(gravityVector);
        }

        public void setTarget(Queue<Entity> targets) {
            this.targets = targets;
        }

        public void setFromEntity(Entity fromEntity) {
            this.fromEntity = fromEntity;
            this.fromLocation = fromEntity.getLocation();
            this.towards = fromLocation.getDirection();
        }

        public void setFromLocation(Location from) {
            this.fromLocation = from;
        }

        public void setTowards(Vector towards) {
            this.towards = towards;
        }
    }

    // can be called anywhere, maybe
    class MovingTaskBuilder {
        MovingTask movingTask;

        public MovingTaskBuilder(PowerBeam power) {
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
    }

    private static boolean isUtilArmorStand(LivingEntity livingEntity) {
        if (livingEntity instanceof ArmorStand) {
            ArmorStand arm = (ArmorStand) livingEntity;
            return arm.isMarker() && !arm.isVisible();
        }
        return false;
    }

    private List<Entity> getTargets(Vector direction, Location fromLocation, LivingEntity from) {
        int radius = Math.min(this.length, 300);
        return Utils.getLivingEntitiesInConeSorted(from.getNearbyEntities(radius, this.length * 1.5, this.length * 1.5).stream()
                        .filter(entity -> entity instanceof LivingEntity && !entity.equals(from) && !entity.isDead())
                        .map(entity -> ((LivingEntity) entity))
                        .collect(Collectors.toList())
                , fromLocation.toVector(), homingAngle, direction).stream()
                .filter(livingEntity -> {
                    if (isUtilArmorStand(livingEntity)) {
                        return false;
                    }
                    switch (homingTarget) {
                        case MOBS:
                            return !(livingEntity instanceof Player);
                        case PLAYERS:
                            return livingEntity instanceof Player && !((Player) livingEntity).getGameMode().equals(GameMode.SPECTATOR);
                        case ALL:
                            return !(livingEntity instanceof Player) || !((Player) livingEntity).getGameMode().equals(GameMode.SPECTATOR);
                    }
                    return true;
                }).collect(Collectors.toList());
    }

    private enum Mode {
        BEAM,
        PROJECTILE,
        ;

    }

    public class ExtraDataSerializer implements Getter, Setter {
        @Override
        public String get(Object object) {
            if (object instanceof Particle.DustOptions) {
                Color color = ((Particle.DustOptions) object).getColor();
                return color.getRed() + "," + color.getGreen() + "," + color.getBlue() + "," + ((Particle.DustOptions) object).getSize();
            }
            return "";
        }

        @Override
        public Optional set(String value) throws IllegalArgumentException {
            String[] split = value.split(",", 4);
            int r = Integer.parseInt(split[0]);
            int g = Integer.parseInt(split[1]);
            int b = Integer.parseInt(split[2]);
            float size = Float.parseFloat(split[3]);
            return Optional.of(new Particle.DustOptions(Color.fromRGB(r, g, b), size));
        }
    }

    enum Target {
        MOBS, PLAYERS, ALL
    }

    public enum BeamShape {
        PLAIN(PlainBias.class, Void.class),
        DNA(DnaBias.class, DnaBias.DnaParams.class),
        CIRCLE(CircleBias.class, CircleBias.CircleParams.class),
        LEGACY_HOMING(PlainBias.class, Void.class);

        private Class<? extends IBias> iBias;
        private Class<?> paramType;

        BeamShape(Class<? extends IBias> iBias, Class<?> paramType) {
            this.iBias = iBias;
            this.paramType = paramType;
        }

        public List<Vector> getBiases(Location location, Vector towards, MovingTask context, String params) {
            return null;
        }
    }

    interface IBias<T> {
        List<Vector> getBiases(Location location, Vector towards, MovingTask context, T params);
    }

    static class PlainBias implements IBias<Void> {
        @Override
        public List<Vector> getBiases(Location location, Vector towards, MovingTask context, Void params) {
            return null;
        }
    }

    static class CircleBias implements IBias<CircleBias.CircleParams> {
        private CircleParams params;

        @Override
        public List<Vector> getBiases(Location location, Vector towards, MovingTask context, CircleParams params) {
            return null;
        }

        static class CircleParams {
            public double r = 1;
            public String rFunc = "";

        }
    }

    static class DnaBias implements IBias<DnaBias.DnaParams> {
        @Override
        public List<Vector> getBiases(Location location, Vector towards, MovingTask context, DnaParams params) {
            return null;
        }

        static class DnaParams {
            double amount = 2;
            double r = 1;
            String rFunc = "";
        }
    }

    private enum HomingMode {
        ONE_TARGET, MULTI_TARGET, MOUSE_TRACK
    }
}
