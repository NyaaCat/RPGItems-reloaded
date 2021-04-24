package think.rpgitems.power.impl;

import java.util.ArrayList;
import java.util.Iterator;
import java.util.Random;
import org.bukkit.*;
import org.bukkit.block.Block;
import org.bukkit.entity.FallingBlock;
import org.bukkit.entity.Player;
import org.bukkit.event.entity.EntityShootBowEvent;
import org.bukkit.event.player.PlayerInteractEvent;
import org.bukkit.inventory.ItemStack;
import org.bukkit.scheduler.BukkitRunnable;
import org.bukkit.util.Vector;
import think.rpgitems.I18n;
import think.rpgitems.RPGItems;
import think.rpgitems.power.*;

/**
 * Power rainbow.
 *
 * <p>The rainbow power will fire {@link #count} of blocks of coloured wool or {@link #isFire fire}
 * on right click, the wool will remove itself.
 */
@SuppressWarnings("WeakerAccess")
@Meta(
        defaultTrigger = "RIGHT_CLICK",
        generalInterface = PowerPlain.class,
        implClass = Rainbow.Impl.class)
public class Rainbow extends BasePower {

    @Property(order = 1)
    public int count = 5;

    @Property(order = 2)
    public boolean isFire = false;

    public Random getRandom() {
        return random;
    }

    private final Random random = new Random();

    @Override
    public String getName() {
        return "rainbow";
    }

    @Override
    public String displayText() {
        return I18n.formatDefault("power.rainbow", getCount(), (double) 0 / 20d);
    }

    /** Count of blocks */
    public int getCount() {
        return count;
    }

    /** Whether launch fire instead of wool */
    public boolean isFire() {
        return isFire;
    }

    public static class Impl
            implements PowerRightClick<Rainbow>, PowerPlain<Rainbow>, PowerBowShoot<Rainbow> {

        @Override
        public PowerResult<Void> rightClick(
                Rainbow power, Player player, ItemStack stack, PlayerInteractEvent event) {
            return fire(power, player, stack);
        }

        @Override
        public PowerResult<Void> fire(Rainbow power, Player player, ItemStack stack) {
            player.playSound(player.getLocation(), Sound.ENTITY_ARROW_SHOOT, 1.0f, 1.0f);
            final ArrayList<FallingBlock> blocks = new ArrayList<>();
            Random random = power.getRandom();
            for (int i = 0; i < power.getCount(); i++) {
                FallingBlock block;
                if (!power.isFire()) {
                    block =
                            player.getWorld()
                                    .spawnFallingBlock(
                                            player.getLocation().add(0, 1.8, 0),
                                            Tag.WOOL
                                                    .getValues()
                                                    .toArray(new Material[16])[random.nextInt(16)]
                                                    .createBlockData());
                } else {
                    block =
                            player.getWorld()
                                    .spawnFallingBlock(
                                            player.getLocation().add(0, 1.8, 0),
                                            Material.FIRE.createBlockData());
                }
                block.setVelocity(
                        player.getLocation()
                                .getDirection()
                                .multiply(
                                        new Vector(
                                                random.nextDouble() * 2d + 0.5,
                                                random.nextDouble() * 2d + 0.5,
                                                random.nextDouble() * 2d + 0.5)));
                block.setDropItem(false);
                blocks.add(block);
            }
            (new BukkitRunnable() {

                        ArrayList<Location> fallLocs = new ArrayList<>();
                        Random random = new Random();

                        public void run() {

                            Iterator<Location> l = fallLocs.iterator();
                            while (l.hasNext()) {
                                Location loc = l.next();
                                if (random.nextBoolean()) {
                                    Block b = loc.getBlock();
                                    if ((power.isFire() && b.getType() == Material.FIRE)
                                            || (!power.isFire()
                                                    && Tag.WOOL.isTagged(b.getType()))) {
                                        loc.getWorld()
                                                .playEffect(loc, Effect.STEP_SOUND, b.getType());
                                        b.setType(Material.AIR);
                                    }
                                    l.remove();
                                }
                                if (random.nextInt(5) == 0) {
                                    break;
                                }
                            }

                            Iterator<FallingBlock> it = blocks.iterator();
                            while (it.hasNext()) {
                                FallingBlock block = it.next();
                                if (block.isDead()) {
                                    fallLocs.add(block.getLocation());
                                    it.remove();
                                }
                            }

                            if (fallLocs.isEmpty() && blocks.isEmpty()) {
                                cancel();
                            }
                        }
                    })
                    .runTaskTimer(RPGItems.plugin, 0, 5);
            return PowerResult.ok();
        }

        @Override
        public Class<? extends Rainbow> getPowerClass() {
            return Rainbow.class;
        }

        @Override
        public PowerResult<Float> bowShoot(
                Rainbow power, Player player, ItemStack stack, EntityShootBowEvent event) {
            return fire(power, player, stack).with(event.getForce());
        }
    }
}
