package think.rpgitems.power.impl;

import org.bukkit.*;
import org.bukkit.block.Block;
import org.bukkit.entity.Entity;
import org.bukkit.entity.LivingEntity;
import org.bukkit.entity.Player;
import org.bukkit.entity.Snowball;
import org.bukkit.event.entity.EntityDamageByEntityEvent;
import org.bukkit.event.entity.EntityDamageEvent;
import org.bukkit.event.entity.EntityShootBowEvent;
import org.bukkit.event.player.PlayerInteractEvent;
import org.bukkit.event.player.PlayerToggleSneakEvent;
import org.bukkit.event.player.PlayerToggleSprintEvent;
import org.bukkit.inventory.ItemStack;
import org.bukkit.scheduler.BukkitRunnable;
import org.bukkit.util.Vector;
import org.librazy.nclangchecker.LangKey;
import think.rpgitems.Events;
import think.rpgitems.RPGItems;
import think.rpgitems.power.*;
import think.rpgitems.utils.ArmorStandUtil;

import java.util.*;
import java.util.stream.Collectors;
import java.util.stream.Stream;

@PowerMeta(defaultTrigger = "RIGHT_CLICK")
public class PowerBeam extends BasePower implements PowerRightClick, PowerLeftClick, PowerSneak, PowerSneaking, PowerSprint, PowerBowShoot {
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

    @Property
    @Serializer(ExtraDataSerializer.class)
    @Deserializer(ExtraDataSerializer.class)
    public Object extraData;

    @Property
    public double speed = 0;

    private Set<Material> transp = Stream.of(Material.values())
            .filter(material -> !material.isSolid())
            .collect(Collectors.toSet());

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
        return beam(player);
    }

    @Override
    public PowerResult<Void> rightClick(Player player, ItemStack stack, PlayerInteractEvent event) {
        return beam(player);
    }

    @Override
    public PowerResult<Void> sneak(Player player, ItemStack stack, PlayerToggleSneakEvent event) {
        return beam(player);
    }

    @Override
    public PowerResult<Void> sneaking(Player player, ItemStack stack) {
        return beam(player);
    }

    @Override
    public PowerResult<Void> sprint(Player player, ItemStack stack, PlayerToggleSprintEvent event) {
        return beam(player);
    }

    private PowerResult<Void> beam(LivingEntity from) {
        new BukkitRunnable() {
            @Override
            public void run() {
                Location fromLocation = from.getEyeLocation();
                Block targetBlock;
                if (ignoreWall) {
                    targetBlock = from.getTargetBlock(
                            Arrays.stream(Material.values()).collect(Collectors.toSet())
                            , ((int) Math.ceil(length)));
                } else {

                    targetBlock = from.getTargetBlock(transp,((int) Math.ceil(length)));
                }
                Location toLocation;
                Vector towards = from.getEyeLocation().getDirection();
                if (targetBlock == null) {
                    toLocation = fromLocation.clone();
                    toLocation.add(towards.multiply(length));
                } else {
                    toLocation = fromLocation.clone();
                    int lth = (int) Math.ceil(targetBlock.getLocation().distance(fromLocation));
                    toLocation.add(towards.multiply(lth));
                }
                double actualLength = toLocation.distance(fromLocation);
                Location step = toLocation.clone();
                step.subtract(fromLocation).multiply(1 / actualLength);

                List<Location> particleSpawnLocation = new LinkedList<>();
                Location temp = fromLocation.clone();
                int apS = amount / ((int) Math.floor(actualLength));
                for (int i = 0; i < actualLength; i++, temp.add(step)) {
                    particleSpawnLocation.add(temp.clone());
                }

                List<Entity> nearbyEntities = from.getNearbyEntities(actualLength, actualLength, actualLength).stream()
                        .filter(entity -> entity instanceof LivingEntity)
                        //mobs in front of player
                        .filter(entity -> entity.getLocation().subtract(fromLocation).toVector().angle(towards) < (Math.PI / 4))
                        .sorted((o1, o2) -> {
                            Vector o = from.getLocation().toVector();
                            return (int) (o1.getLocation().toVector().distanceSquared(o) - o2.getLocation().toVector().distanceSquared(o));
                        })
                        .collect(Collectors.toList());

                switch (mode) {
                    case BEAM:
                        new PlainTask(from, particle, particleSpawnLocation, apS, nearbyEntities).runTask(RPGItems.plugin);
                        break;
                    case PROJECTILE:
                        new MovingTask(from, particle, particleSpawnLocation, apS, movementTicks, nearbyEntities).runTask(RPGItems.plugin);
                        break;
                }
            }
        }.runTaskAsynchronously(RPGItems.plugin);
        return new PowerResult<>();
    }

    @Override
    public PowerResult<Void> bowShoot(Player player, ItemStack itemStack, EntityShootBowEvent e) {
        return beam(player);
    }

    class PlainTask extends BukkitRunnable {
        private LivingEntity from;
        private final Particle particle;
        private List<Location> particleSpawnLocation;
        private final int apS;
        private List<Entity> nearbyEntities;

        PlainTask(LivingEntity from, Particle particle, List<Location> particleSpawnLocation, int apS, List<Entity> nearbyEntities) {
            this.from = from;
            this.particle = particle;
            this.particleSpawnLocation = particleSpawnLocation;
            this.apS = apS;
            this.nearbyEntities = nearbyEntities;
        }

        @Override
        public void run() {
            if (particleSpawnLocation.isEmpty()) return;
            Iterator<Location> iterator = particleSpawnLocation.iterator();
            World world = particleSpawnLocation.get(0).getWorld();
            if (world == null) return;
            Location lastLocation = particleSpawnLocation.get(0);
            while (iterator.hasNext()) {
                Location loc = iterator.next();
//                world.spawnParticle(this.particle, loc, apS);
                if (!loc.equals(lastLocation)) {
                    Vector step = loc.toVector().subtract(lastLocation.toVector()).multiply(0.25);
                    for (int i = 0; i < 4; i++) {
                        spawnParticle(from, world, lastLocation, apS / 4);
                        lastLocation.add(step);
                    }
                }
//                else {
//                    spawnParticle(from, world, loc, apS);
//                }
                Class<?> dataType = this.particle.getDataType();
                lastLocation = loc;
                if (tryHit(from, loc, nearbyEntities)) return;
            }
        }


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
            List<Entity> collect = nearbyEntities.stream()
                    .filter(entity -> canHit(loc, entity))
                    .limit(1)
                    .collect(Collectors.toList());
            if (!collect.isEmpty()) {
                Entity entity = collect.get(0);
                if (entity instanceof LivingEntity) {
                    Snowball snowball = ArmorStandUtil.asProjectileSource(from).launchProjectile(Snowball.class);
                    Events.registerRPGProjectile(snowball.getEntityId(), getItem().getUid());
                    snowball.setShooter(from);
                    EntityDamageByEntityEvent event = new EntityDamageByEntityEvent(snowball, entity, EntityDamageEvent.DamageCause.PROJECTILE, damage);
                    Bukkit.getServer().getPluginManager().callEvent(event);
                    double actualDamage = event.getDamage();
                    ((LivingEntity) entity).damage(actualDamage, snowball);
                    snowball.remove();
                }
                return true;
            }
        } else {
            List<Entity> collect = nearbyEntities.stream()
                    .filter(entity -> entity instanceof LivingEntity)
                    .filter(entity -> canHit(loc, entity))
                    .collect(Collectors.toList());
            if (!collect.isEmpty()) {
                collect.stream()
                        .map(entity -> ((LivingEntity) entity))
                        .forEach(livingEntity -> {
                            Snowball snowball = ArmorStandUtil.asProjectileSource(from).launchProjectile(Snowball.class, new Vector(0, 0, 0));
                            Events.registerRPGProjectile(snowball.getEntityId(), getItem().getUid());
                            snowball.setShooter(from);
                            EntityDamageByEntityEvent event = new EntityDamageByEntityEvent(snowball, livingEntity, EntityDamageEvent.DamageCause.PROJECTILE, damage);
                            Bukkit.getServer().getPluginManager().callEvent(event);
                            double actualDamage = event.getDamage();
                            livingEntity.damage(actualDamage, snowball);
                            snowball.remove();
                        });
                nearbyEntities.removeAll(collect);
            }
        }
        return false;
    }

    private boolean canHit(Location loc, Entity entity) {
        Location eyeLocation = ((LivingEntity) entity).getEyeLocation();
        Location location = entity.getLocation();

        return loc.getY() > Math.min(location.getY(), eyeLocation.getY()) &&
                loc.getY() < Math.max(location.getY(), eyeLocation.getY()) &&
                Math.pow((loc.getX() - location.getX()), 2) + Math.pow((loc.getZ() - location.getZ()), 2) < 1; // TODO use actual paticle radius and entity hitbox as result
    }

    private enum Mode {
        BEAM,
        PROJECTILE,
        ;
    }

    private class MovingTask extends BukkitRunnable {
        private final LivingEntity from;
        private final Particle particle;
        private final List<Location> particleSpawnLocation;
        private final int amountPerSec;
        private final int ticks;
        private final List<Entity> nearbyEntities;
        private final List<BukkitRunnable> runnables = new LinkedList<>();

        public MovingTask(LivingEntity from, Particle particle, List<Location> particleSpawnLocation, int amountPerSec, int ticks, List<Entity> nearbyEntities) {
            this.from = from;
            this.particle = particle;
            this.particleSpawnLocation = particleSpawnLocation;
            this.amountPerSec = amountPerSec;
            this.ticks = ticks;
            this.nearbyEntities = nearbyEntities;
        }

        @Override
        public void run() {
            if (particleSpawnLocation.isEmpty()) return;
            Location location = particleSpawnLocation.get(0);
            World world = location.getWorld();
            if (world == null) return;
            int size = particleSpawnLocation.size();
            int spS = (int) Math.ceil(((double) size) / ((double) ticks));
            Iterator<Location> iterator = particleSpawnLocation.iterator();
            for (int i = 0; i < ticks; i++) {
                BukkitRunnable bukkitRunnable = new BukkitRunnable() {
                    @Override
                    public void run() {
                        Location lastLocation = particleSpawnLocation.get(0);
                        for (int j = 0; j < spS; j++) {
                            if (!iterator.hasNext()) return;
                            Location loc = iterator.next();
                            if (!loc.equals(lastLocation)) {
                                Vector step = loc.toVector().subtract(lastLocation.toVector()).multiply(0.25);
                                for (int i = 0; i < 4; i++) {
                                    spawnParticle(from, world, lastLocation, amountPerSec / 4);
                                    lastLocation.add(step);
                                }
                            }
//                            else {
//                                spawnParticle(from, world, loc, amountPerSec);
//                            }
//                            spawnParticle(from, world, loc, amountPerSec);
                            if (tryHit(from, loc, nearbyEntities)) {
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
