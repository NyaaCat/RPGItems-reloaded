package think.rpgitems.power.impl;

import org.bukkit.*;
import org.bukkit.block.Block;
import org.bukkit.block.BlockFace;
import org.bukkit.block.data.BlockData;
import org.bukkit.block.data.Directional;
import org.bukkit.entity.FallingBlock;
import org.bukkit.entity.Player;
import org.bukkit.event.entity.EntityShootBowEvent;
import org.bukkit.event.player.PlayerInteractEvent;
import org.bukkit.inventory.ItemStack;
import org.bukkit.scheduler.BukkitRunnable;
import think.rpgitems.I18n;
import think.rpgitems.RPGItems;
import think.rpgitems.event.PowerActivateEvent;
import think.rpgitems.power.*;
import think.rpgitems.utils.TempBlockManager;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Random;

import static think.rpgitems.power.Utils.checkCooldown;

/**
 * Power torch.
 * <p>
 * The torch power will shoots torches to light up an area.
 * </p>
 */
@Meta(defaultTrigger = "RIGHT_CLICK", generalInterface = PowerPlain.class, implClass = Torch.Impl.class)
public class Torch extends BasePower {
    @Property(order = 0)
    public int cooldown = 0;
    @Property
    public int cost = 0;

    private List<BlockFace> getPossibleFaces(Location loc) {
        List<BlockFace> faces = new ArrayList<>();
        Block relative = loc.getBlock().getRelative(BlockFace.DOWN);
        if (relative.getType().isSolid() && relative.getType().isOccluding()) {
            faces.add(BlockFace.DOWN);
        }
        for (BlockFace f : ((Directional) Material.WALL_TORCH.createBlockData()).getFaces()) {
            Block block = loc.getBlock().getRelative(f);
            if (block.getType().isSolid() && block.getType().isOccluding()) {
                faces.add(f.getOppositeFace());
            }
        }
        return faces;
    }

    /**
     * Cost of this power
     */
    public int getCost() {
        return cost;
    }

    @Override
    public String getName() {
        return "torch";
    }

    @Override
    public String displayText() {
        return I18n.formatDefault("power.torch", (double) getCooldown() / 20d);
    }

    /**
     * Cooldown time of this power
     */
    public int getCooldown() {
        return cooldown;
    }

    public class Impl implements PowerPlain, PowerLeftClick, PowerRightClick, PowerBowShoot {


        @Override
        public PowerResult<Void> leftClick(Player player, ItemStack stack, PlayerInteractEvent event) {
            return fire(player, stack);
        }

        @Override
        public PowerResult<Void> fire(final Player player, ItemStack stack) {
            PowerActivateEvent powerEvent = new PowerActivateEvent(player,stack,getPower());
            if(!powerEvent.callEvent()) {
                return PowerResult.fail();
            }
            if (!checkCooldown(getPower(), player, getCooldown(), showCooldownWarning(), true)) return PowerResult.cd();
            if (!getItem().consumeDurability(stack, getCost())) return PowerResult.cost();
            player.playSound(player.getLocation(), Sound.ITEM_FLINTANDSTEEL_USE, 1.0f, 0.8f);
            final FallingBlock block = player.getWorld().spawn(player.getLocation().add(0, 1.8, 0), FallingBlock.class, fallingBlock -> {
                fallingBlock.setBlockData(Material.TORCH.createBlockData());
            });
            block.setVelocity(player.getLocation().getDirection().multiply(2d));
            block.setDropItem(false);
            BukkitRunnable run = new BukkitRunnable() {

                public void run() {
                    World world = block.getWorld();
                    final Random random = new Random();
                    if (block.isDead()) {
                        block.remove();
                        // The falling block placed a real torch on landing (vanilla FallingBlock
                        // conversion); start tracking it so it reverts to air and is crash-safe, just
                        // like the torches we place ourselves below.
                        if (block.getLocation().getBlock().getType().equals(Material.TORCH)) {
                            TempBlockManager.track(block.getLocation().getBlock(), Material.AIR.createBlockData());
                        }
                        cancel();
                        final HashMap<Location, Material> changedBlocks = new HashMap<>();
                        for (int x = -2; x <= 2; x++) {
                            for (int y = -2; y <= 3; y++) {
                                for (int z = -2; z <= 2; z++) {
                                    Location loc = block.getLocation().add(x, y, z);
                                    Block b = world.getBlockAt(loc);
                                    if (b.getType().equals(Material.AIR) && random.nextInt(100) < 20) {
                                        List<BlockFace> faces = getPossibleFaces(loc);
                                        if (faces.size() > 0) {
                                            changedBlocks.put(b.getLocation(), b.getType());
                                            BlockFace o = faces.get(random.nextInt(faces.size()));
                                            BlockData data = (o == BlockFace.DOWN ? Material.TORCH : Material.WALL_TORCH).createBlockData();
                                            if (o != BlockFace.DOWN) {
                                                ((Directional) data).setFacing(o);
                                            }
                                            TempBlockManager.place(b, data);
                                        }
                                    }
                                }
                            }
                        }
                        (new BukkitRunnable() {

                            @Override
                            public void run() {
                                if (changedBlocks.isEmpty()) {
                                    cancel();
                                    Location landing = block.getLocation();
                                    if (TempBlockManager.isTracked(landing)) {
                                        TempBlockManager.revert(landing);
                                    }
                                    return;
                                }
                                int index = random.nextInt(changedBlocks.size());
                                Location loc = (Location) changedBlocks.keySet().toArray()[index];
                                changedBlocks.remove(loc);
                                Block c = loc.getBlock();
                                loc.getWorld().playEffect(loc, Effect.DESTROY_BLOCK, c.getBlockData());
                                TempBlockManager.revert(loc);
                            }
                        }).runTaskTimer(RPGItems.plugin, 4 * 20 + new Random().nextInt(40), 3);
                    }

                }
            };
            run.runTaskTimer(RPGItems.plugin, 0, 1);
            return PowerResult.ok();
        }

        @Override
        public Power getPower() {
            return Torch.this;
        }

        @Override
        public PowerResult<Void> rightClick(Player player, ItemStack stack, PlayerInteractEvent event) {
            return fire(player, stack);
        }

        @Override
        public PowerResult<Float> bowShoot(Player player, ItemStack stack, EntityShootBowEvent event) {
            return fire(player, stack).with(event.getForce());
        }
    }
}
