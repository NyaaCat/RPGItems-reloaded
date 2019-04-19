package think.rpgitems.power.impl;

import org.bukkit.*;
import org.bukkit.block.Block;
import org.bukkit.entity.Entity;
import org.bukkit.entity.LivingEntity;
import org.bukkit.entity.Player;
import org.bukkit.event.player.PlayerInteractEvent;
import org.bukkit.event.player.PlayerToggleSneakEvent;
import org.bukkit.event.player.PlayerToggleSprintEvent;
import org.bukkit.inventory.ItemStack;
import org.bukkit.scheduler.BukkitRunnable;
import org.bukkit.util.Vector;
import org.librazy.nclangchecker.LangKey;
import think.rpgitems.RPGItems;
import think.rpgitems.power.*;

import java.util.Arrays;
import java.util.Iterator;
import java.util.LinkedList;
import java.util.List;
import java.util.stream.Collectors;

@PowerMeta(defaultTrigger = "RIGHT_CLICK")
public class PowerBeam extends BasePower implements PowerRightClick, PowerLeftClick, PowerSneak, PowerSneaking, PowerSprint {
    @Property
    public int length = 10;

    @Property
    public Particle particle = Particle.LAVA;

    @Property
    public int amount = 200;

    @Property
    public Mode mode = Mode.PLAIN;

    @Property
    //todo support not pierce
    public boolean pierce = true;

    @Property
    public boolean ignoreWall = true;

    @Property
    public double damage = 20;

    @Property
    public int movementTicks = 40;


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
        return beam(player, player.getEyeLocation().getDirection(), length, amount, ignoreWall, pierce, mode, movementTicks);
    }

    @Override
    public PowerResult<Void> rightClick(Player player, ItemStack stack, PlayerInteractEvent event) {
        return beam(player, player.getEyeLocation().getDirection(), length, amount, ignoreWall, pierce, mode, movementTicks);
    }

    @Override
    public PowerResult<Void> sneak(Player player, ItemStack stack, PlayerToggleSneakEvent event) {
        return beam(player, player.getEyeLocation().getDirection(), length, amount, ignoreWall, pierce, mode, movementTicks);
    }

    @Override
    public PowerResult<Void> sneaking(Player player, ItemStack stack) {
        return beam(player, player.getEyeLocation().getDirection(), length, amount, ignoreWall, pierce, mode, movementTicks);
    }

    @Override
    public PowerResult<Void> sprint(Player player, ItemStack stack, PlayerToggleSprintEvent event) {
        return beam(player, player.getEyeLocation().getDirection(), length, amount, ignoreWall, pierce, mode, movementTicks);
    }

    private PowerResult<Void> beam(LivingEntity from, Vector towards, double length, int amount, boolean ignoreWall, boolean pierce, Mode mode, int movingTicks) {
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
                    targetBlock = from.getTargetBlockExact(((int) Math.ceil(length)), FluidCollisionMode.NEVER);
                }
                Location toLocation;
                if (targetBlock == null) {
                    toLocation = fromLocation.add(towards.multiply(length));
                } else {
                    toLocation = targetBlock.getLocation();
                }
                double actualLength = toLocation.distance(fromLocation);
                Location step = toLocation.subtract(fromLocation).multiply(1 / actualLength);

                List<Location> particleSpawnLocation = new LinkedList<>();
                Location temp = fromLocation.clone();
                int apS = amount / ((int) Math.floor(actualLength));
                for (int i = 0; i < length; i++, temp.add(step)) {
                    particleSpawnLocation.add(temp.clone());
                }

                List<Entity> nearbyEntities = from.getNearbyEntities(length, length, length).stream()
                        //mobs in front of player
                        .filter(entity -> entity.getLocation().subtract(fromLocation).toVector().angle(towards) < (Math.PI / 4))
                        .sorted((o1, o2) -> {
                            Vector o = from.getLocation().toVector();
                            return (int) (o1.getLocation().toVector().distanceSquared(o) - o2.getLocation().toVector().distanceSquared(o));
                        })
                        .collect(Collectors.toList());

                switch (mode) {
                    case PLAIN:
                        new PlainTask(particle, particleSpawnLocation, apS, nearbyEntities).runTask(RPGItems.plugin);
                        break;
                    case MOVING:
                        new MovingTask(particle, particleSpawnLocation, apS, movingTicks, nearbyEntities).runTask(RPGItems.plugin);
                        break;
                }
            }
        }.runTaskAsynchronously(RPGItems.plugin);
        return new PowerResult<>();
    }

    class PlainTask extends BukkitRunnable {
        private final Particle particle;
        private List<Location> particleSpawnLocation;
        private final int apS;

        PlainTask(Particle particle, List<Location> particleSpawnLocation, int apS, List<Entity> nearbyEntities) {
            this.particle = particle;
            this.particleSpawnLocation = particleSpawnLocation;
            this.apS = apS;
        }

        @Override
        public void run() {
            if (particleSpawnLocation.isEmpty()) return;
            Iterator<Location> iterator = particleSpawnLocation.iterator();
            World world = particleSpawnLocation.get(0).getWorld();
            if (world == null) return;
            while (iterator.hasNext()) {
                Location loc = iterator.next();
                world.spawnParticle(this.particle, loc, apS);
            }
        }
    }

    private enum Mode {
        PLAIN,
        MOVING,
        ;
    }

    private class MovingTask extends BukkitRunnable {
        private final Particle particle;
        private final List<Location> particleSpawnLocation;
        private final int amountPerSec;
        private final int ticks;

        public MovingTask(Particle particle, List<Location> particleSpawnLocation, int amountPerSec, int ticks, List<Entity> nearbyEntities) {
            this.particle = particle;
            this.particleSpawnLocation = particleSpawnLocation;
            this.amountPerSec = amountPerSec;
            this.ticks = ticks;
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
                new BukkitRunnable() {
                    @Override
                    public void run() {
                        for (int j = 0; j < spS; j++) {
                            if (!iterator.hasNext()) return;
                            world.spawnParticle(particle, iterator.next(), amountPerSec);
                        }
                    }
                }.runTaskLater(RPGItems.plugin, i);
            }
        }
    }
}
