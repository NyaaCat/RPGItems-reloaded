package think.rpgitems.power.impl;

import org.bukkit.*;
import org.bukkit.block.Block;
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
import org.bukkit.util.Vector;
import org.librazy.nclangchecker.LangKey;
import think.rpgitems.RPGItems;
import think.rpgitems.data.Context;
import think.rpgitems.power.*;

import java.util.*;
import java.util.stream.Collectors;
import java.util.stream.Stream;

import static think.rpgitems.Events.*;
import static think.rpgitems.power.Utils.checkCooldown;

@PowerMeta(defaultTrigger = "RIGHT_CLICK")
public class PowerBeam extends BasePower implements PowerPlain, PowerRightClick, PowerLeftClick, PowerSneak, PowerSneaking, PowerSprint, PowerBowShoot, PowerHitTaken, PowerHit {
    @Property
    public int length = 10;

    @Property
    public Particle particle = Particle.LAVA;

    @Property
    public int amount = 200;

    @Property
    public Mode mode = Mode.BEAM;

    @Property
    public boolean pierce = true;

    @Property
    public boolean ignoreWall = true;

    @Property
    public double damage = 20;

    @Property
    public int movementTicks = 40;

    @Property
    public double offsetX = 0;

    @Property
    public double offsetY = 0;

    @Property
    public double offsetZ = 0;

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

    @Property
    public boolean cone = false;

    @Property
    public double coneRange = 30;

    @Property
    public boolean homing = false;

    @Property
    public double homingAngle = 1;

    @Property
    public double homingRange = 30;

    @Property
    public int burstCount = 1;

    @Property
    public int beamAmount = 1;

    @Property
    public int burstInterval = 1;

    @Property
    @Serializer(ExtraDataSerializer.class)
    @Deserializer(ExtraDataSerializer.class)
    public Object extraData;

    @Property
    public double speed = 0;


    /**
     * Whether to suppress the hit trigger
     */
    @Property
    public boolean suppressMelee = false;

    private Set<Material> transp = Stream.of(Material.values())
            .filter(material -> material.isBlock())
            .filter(material -> !material.isSolid() || !material.isOccluding())
            .collect(Collectors.toSet());

    @Override
    public PowerResult<Void> fire(Player player, ItemStack stack) {
        if (!checkCooldown(this, player, cooldown, true, true)) return PowerResult.cd();
        if (!getItem().consumeDurability(stack, cost)) return PowerResult.cost();
        return beam(player);
    }

    @Override
    public @LangKey(skipCheck = true) String getName() {
        return "beam";
    }

    @Override
    public String displayText() {
        return null;
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

    private Random random = new Random();
    private Vector yUnit = new Vector(0, 1, 0);

    private PowerResult<Void> beam(LivingEntity from) {
        Location fromLocation = from.getEyeLocation();
        Block targetBlock;
        if (ignoreWall) {
            targetBlock = from.getTargetBlock(
                    Arrays.stream(Material.values())
                            .collect(Collectors.toSet())
                    , ((int) Math.ceil(length)));
        } else {
            targetBlock = from.getTargetBlock(transp, ((int) Math.ceil(length)));
        }
        Vector towards = from.getEyeLocation().getDirection();
        Location toLocation;
        if (targetBlock.getType() == Material.AIR) {
            toLocation = fromLocation.clone();
            toLocation.add(towards.clone().multiply(length));
        } else {
            toLocation = fromLocation.clone();
            int lth = (int) Math.ceil(targetBlock.getLocation().distance(fromLocation));
            toLocation.add(towards.clone().multiply(lth));
        }
        double actualLength = toLocation.distance(fromLocation);
        if (actualLength < 0.05) return PowerResult.noop();
        if (burstCount > 0) {
            for (int i = 0; i < burstCount; i++) {
                new BukkitRunnable(){
                    @Override
                    public void run() {
                        if (cone){
                            for (int j = 0; j < beamAmount; j++) {
                                fire(from, fromLocation, toLocation, towards, actualLength);
                            }
                        }else {
                            fire(from, fromLocation, toLocation, towards, actualLength);
                        }
                    }
                }.runTaskLater(RPGItems.plugin, i * burstInterval);
            }
            return PowerResult.ok();
        } else {
            return fire(from, fromLocation, toLocation, towards, actualLength);
        }
    }

    private PowerResult<Void> fire(LivingEntity from, Location fromLocation, Location toLocation, Vector toward, double actualLength) {
        Vector towards = toward.clone();
        if (cone) {
            double phi = random.nextInt(360);
            double theta;
            if (coneRange > 0) {
                theta = random.nextInt(((int) Math.round(coneRange)));
                double shiftLen = towards.length() * Math.tan(Math.toRadians(theta));
                Vector clone = towards.clone();
                Vector shift = clone.crossProduct(yUnit).normalize().multiply(shiftLen);
                shift.rotateAroundNonUnitAxis(towards, Math.toRadians(phi));
                towards = towards.add(shift);
            }
        }

        Location step = toLocation.clone();
        step.subtract(fromLocation).multiply(1 / actualLength);

        Entity target = null;
        if (from instanceof Player && homing) {
            target = Utils.getLivingEntitiesInCone(Utils.getNearestLivingEntities(this, fromLocation, ((Player) from), actualLength, 0), fromLocation.toVector(), homingRange, from.getEyeLocation().getDirection()).stream()
                    .findFirst().orElse(null);
        }

        Vector finalTowards = towards;
        List<Entity> nearbyEntities = from.getNearbyEntities(actualLength, actualLength, actualLength).stream()
                .filter(entity -> entity instanceof LivingEntity)
                //mobs in front of player
                .filter(entity -> entity.getLocation().subtract(fromLocation).toVector().angle(finalTowards) < (Math.PI / 4))
                .sorted((o1, o2) -> {
                    Vector o = from.getLocation().toVector();
                    return (int) (o1.getLocation().toVector().distanceSquared(o) - o2.getLocation().toVector().distanceSquared(o));
                })
                .collect(Collectors.toList());

        switch (mode) {
            case BEAM:
                new PlainTask(from, towards, amount, actualLength, nearbyEntities, target).runTask(RPGItems.plugin);
                break;
            case PROJECTILE:
                new MovingTask(from, towards, amount, actualLength, nearbyEntities, target).runTask(RPGItems.plugin);
                break;
        }
        return PowerResult.ok();
    }

    @Override
    public PowerResult<Float> bowShoot(Player player, ItemStack itemStack, EntityShootBowEvent e) {
        return beam(player).with(e.getForce());
    }

    @Override
    public PowerResult<Double> hit(Player player, ItemStack stack, LivingEntity entity, double damage, EntityDamageByEntityEvent event) {
        return beam(player).with(event.getDamage());
    }

    @Override
    public PowerResult<Double> takeHit(Player target, ItemStack stack, double damage, EntityDamageEvent event) {
        return beam(target).with(event.getDamage());
    }

    class PlainTask extends BukkitRunnable {
        private double length;
        private LivingEntity from;
        private Vector towards;
        private final int apS;
        private List<Entity> nearbyEntities;
        private Entity target;

        public PlainTask(LivingEntity from, Vector towards, int amount, double actualLength, List<Entity> nearbyEntities, Entity target) {
            this.from = from;
            this.towards = towards;
            this.length = actualLength;
            this.apS = amount / ((int) Math.floor(actualLength));
            this.nearbyEntities = nearbyEntities;
            this.target = target;
        }

        @Override
        public void run() {
            World world = from.getWorld();
            towards.normalize();
            Location lastLocation = from.getEyeLocation();
            for (int i = 0; i < length; i++) {
                boolean isHit = false;
                for (int j = 0; j < 4; j++) {
                    Vector step = towards.clone().normalize().multiply(0.25);
                    isHit = tryHit(from, lastLocation, nearbyEntities) || isHit;
                    Block block = world.getBlockAt(lastLocation);
                    if (transp.contains(block.getType())) {
                        spawnParticle(from, world, lastLocation, apS / 4);
                    } else if (!ignoreWall) {
                        return;
                    }
                    lastLocation.add(step);
                    towards = homingCorrect(towards, lastLocation, target, i);
                }
                if (isHit) return;
            }
        }


    }

    private class MovingTask extends BukkitRunnable {
        private final LivingEntity from;
        private Vector towards;
        private final int amountPerSec;
        private final List<Entity> nearbyEntities;
        private final List<BukkitRunnable> runnables = new LinkedList<>();
        private Entity target;

        public MovingTask(LivingEntity from, Vector towards, int apS, double actualLength, List<Entity> entities, Entity target) {
            this.from = from;
            this.towards = towards;
            this.amountPerSec = apS;
            this.nearbyEntities = entities;
            this.target = target;
        }

        @Override
        public void run() {
            World world = from.getWorld();
            int spS = (int) Math.ceil(((double) length) / ((double) movementTicks));
            Location lastLocation = from.getEyeLocation();
            towards.normalize();
            for (int i = 0; i < movementTicks; i++) {
                int finalI = i;
                BukkitRunnable bukkitRunnable = new BukkitRunnable() {
                    @Override
                    public void run() {
                        for (int j = 0; j < spS; j++) {
                            boolean isHit = false;
                            for (int k = 0; k < 4; k++) {
                                Vector step = towards.clone().normalize().multiply(0.25);
                                isHit = tryHit(from, lastLocation, nearbyEntities) || isHit;
                                if (transp.contains(world.getBlockAt(lastLocation).getType())) {
                                    spawnParticle(from, world, lastLocation, amountPerSec / 4);
                                } else if (!ignoreWall) {
                                    runnables.forEach(BukkitRunnable::cancel);
                                    return;
                                }
                                lastLocation.add(step);
                                towards = homingCorrect(towards, lastLocation, target, finalI);
                            }
                            if (isHit) {
                                if (!runnables.isEmpty()) {
                                    runnables.forEach(BukkitRunnable::cancel);
                                }
                                return;
                            }
                        }
                    }
                };
                runnables.add(bukkitRunnable);
                bukkitRunnable.runTaskLater(RPGItems.plugin, i);
            }
        }
    }

    private Vector homingCorrect(Vector towards, Location lastLocation, Entity target, int i) {
        if (target == null || i < 5) {
            return towards;
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
        double actualAng = homingAngle / 4;
        if (angle > Math.toRadians(actualAng)) {
            clone.add(clone.clone().getCrossProduct(crossProduct).normalize().multiply(-1 * Math.tan(actualAng)));
        } else {
            clone = targetDirection.normalize();
        }
        return clone;
    }

    private void spawnParticle(LivingEntity from, World world, Location lastLocation, int i) {
        if ((lastLocation.distance(from.getEyeLocation()) < 1)) {
            return;
        }
        if (from instanceof Player) {
            ((Player) from).spawnParticle(this.particle, lastLocation, i / 2, offsetX, offsetY, offsetZ, speed, extraData);
        }
        world.spawnParticle(this.particle, lastLocation, i, offsetX, offsetY, offsetZ, speed, extraData);
    }

    private boolean tryHit(LivingEntity from, Location loc, List<Entity> nearbyEntities) {
        if (!pierce) {
            double length = Math.max(new Vector(offsetX, offsetY, offsetZ).lengthSquared(), 10);
            Collection<Entity> candidates = from.getWorld().getNearbyEntities(loc, length, length, length);
            List<Entity> collect = candidates.stream()
                    .filter(entity -> (entity instanceof LivingEntity) && !entity.equals(from) && canHit(loc, entity))
                    .limit(1)
                    .collect(Collectors.toList());
            if (!collect.isEmpty()) {
                Entity entity = collect.get(0);
                if (entity instanceof LivingEntity) {
                    Context.instance().putTemp(from.getUniqueId(), DAMAGE_SOURCE, getNamespacedKey().toString());
                    Context.instance().putTemp(from.getUniqueId(), OVERRIDING_DAMAGE, damage);
                    Context.instance().putTemp(from.getUniqueId(), SUPPRESS_MELEE, suppressMelee);
                    ((LivingEntity) entity).damage(damage, from);
                    Context.instance().putTemp(from.getUniqueId(), SUPPRESS_MELEE, null);
                    Context.instance().putTemp(from.getUniqueId(), OVERRIDING_DAMAGE, null);
                    Context.instance().putTemp(from.getUniqueId(), DAMAGE_SOURCE, null);
                }
                return true;
            }
        } else {
            List<Entity> collect = nearbyEntities.stream()
                    .filter(entity -> (entity instanceof LivingEntity) && !entity.equals(from) && canHit(loc, entity))
                    .collect(Collectors.toList());
            Context.instance().putTemp(from.getUniqueId(), DAMAGE_SOURCE, getNamespacedKey().toString());
            Context.instance().putTemp(from.getUniqueId(), OVERRIDING_DAMAGE, damage);
            Context.instance().putTemp(from.getUniqueId(), SUPPRESS_MELEE, suppressMelee);

            if (!collect.isEmpty()) {
                collect.stream()
                        .map(entity -> ((LivingEntity) entity))
                        .forEach(livingEntity -> {
                            livingEntity.damage(damage, from);
                        });
                nearbyEntities.removeAll(collect);
            }
            Context.instance().putTemp(from.getUniqueId(), SUPPRESS_MELEE, null);
            Context.instance().putTemp(from.getUniqueId(), OVERRIDING_DAMAGE, null);
            Context.instance().putTemp(from.getUniqueId(), DAMAGE_SOURCE, null);
        }
        return false;
    }

    private WeakHashMap<Location, BoundingBox> boxCache = new WeakHashMap<>();

    private boolean canHit(Location loc, Entity entity) {
        BoundingBox boundingBox = entity.getBoundingBox();
        BoundingBox particleBox = boxCache.get(loc);
        if (particleBox == null) {
            double x = Math.min(offsetX, 0.1);
            double y = Math.min(offsetY, 0.1);
            double z = Math.min(offsetZ, 0.1);
            particleBox = BoundingBox.of(loc, x, y, z);
        }
        return boundingBox.overlaps(particleBox);
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
}
