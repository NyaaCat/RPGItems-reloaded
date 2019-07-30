package think.rpgitems.power.impl;

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

import java.util.ArrayList;
import java.util.Iterator;
import java.util.Random;

import static think.rpgitems.power.Utils.checkCooldown;

/**
 * Power rainbow.
 * <p>
 * The rainbow power will fire {@link #count} of blocks of coloured wool
 * or {@link #isFire fire} on right click, the wool will remove itself.
 * </p>
 */
@SuppressWarnings("WeakerAccess")
@PowerMeta(defaultTrigger = "RIGHT_CLICK", generalInterface = PowerPlain.class, implClass = PowerRainbow.Impl.class)
public class PowerRainbow extends BasePower {

    @Property(order = 0)
    private long cooldown = 0;
    @Property(order = 1)
    private int count = 5;
    @Property(order = 2)
    private boolean isFire = false;
    @Property
    private int cost = 0;

    private Random random = new Random();

    public class Impl implements PowerRightClick, PowerPlain, PowerBowShoot {

        @Override
        public PowerResult<Void> rightClick(Player player, ItemStack stack, PlayerInteractEvent event) {
            return fire(player, stack);
        }

        @Override
        public PowerResult<Float> bowShoot(Player player, ItemStack stack, EntityShootBowEvent event) {
            return fire(player, stack).with(event.getForce());
        }

        @Override
        public PowerResult<Void> fire(Player player, ItemStack stack) {
            if (!checkCooldown(getPower(), player, getCooldown(), true, true)) return PowerResult.cd();
            if (!getItem().consumeDurability(stack, getCost())) return PowerResult.cost();
            player.playSound(player.getLocation(), Sound.ENTITY_ARROW_SHOOT, 1.0f, 1.0f);
            final ArrayList<FallingBlock> blocks = new ArrayList<>();
            for (int i = 0; i < getCount(); i++) {
                FallingBlock block;
                if (!isFire()) {
                    block = player.getWorld().spawnFallingBlock(player.getLocation().add(0, 1.8, 0), Tag.WOOL.getValues().toArray(new Material[16])[random.nextInt(16)].createBlockData());
                } else {
                    block = player.getWorld().spawnFallingBlock(player.getLocation().add(0, 1.8, 0), Material.FIRE.createBlockData());
                }
                block.setVelocity(player.getLocation().getDirection().multiply(new Vector(random.nextDouble() * 2d + 0.5, random.nextDouble() * 2d + 0.5, random.nextDouble() * 2d + 0.5)));
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
                            if ((isFire() && b.getType() == Material.FIRE) || (!isFire() && Tag.WOOL.isTagged(b.getType()))) {
                                loc.getWorld().playEffect(loc, Effect.STEP_SOUND, b.getType());
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
            }).runTaskTimer(RPGItems.plugin, 0, 5);
            return PowerResult.ok();
        }

        @Override
        public Power getPower() {
            return PowerRainbow.this;
        }
    }

    @Override
    public String displayText() {
        return I18n.format("power.rainbow", getCount(), (double) getCooldown() / 20d);
    }

    /**
     * Cooldown time of this power
     */
    public long getCooldown() {
        return cooldown;
    }

    /**
     * Cost of this power
     */
    public int getCost() {
        return cost;
    }

    /**
     * Count of blocks
     */
    public int getCount() {
        return count;
    }

    @Override
    public String getName() {
        return "rainbow";
    }

    /**
     * Whether launch fire instead of wool
     */
    public boolean isFire() {
        return isFire;
    }
}
