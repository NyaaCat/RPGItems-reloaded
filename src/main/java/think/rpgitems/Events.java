package think.rpgitems;

import cat.nyaa.nyaacore.utils.RayTraceUtils;
import com.destroystokyo.paper.event.entity.ThrownEggHatchEvent;
import com.destroystokyo.paper.event.player.PlayerJumpEvent;
import io.papermc.paper.tag.EntityTags;
import org.bukkit.*;
import org.bukkit.block.Block;
import org.bukkit.enchantments.Enchantment;
import org.bukkit.entity.*;
import org.bukkit.event.*;
import org.bukkit.event.block.Action;
import org.bukkit.event.block.BlockBreakEvent;
import org.bukkit.event.block.BlockPhysicsEvent;
import org.bukkit.event.block.BlockPlaceEvent;
import org.bukkit.event.enchantment.EnchantItemEvent;
import org.bukkit.event.entity.*;
import org.bukkit.event.hanging.HangingBreakEvent;
import org.bukkit.event.hanging.HangingBreakEvent.RemoveCause;
import org.bukkit.event.inventory.*;
import org.bukkit.event.player.*;
import org.bukkit.inventory.*;
import org.bukkit.inventory.meta.ItemMeta;
import org.bukkit.inventory.meta.PotionMeta;
import org.bukkit.metadata.FixedMetadataValue;
import org.bukkit.persistence.PersistentDataType;
import org.bukkit.potion.PotionEffect;
import org.bukkit.potion.PotionEffectType;
import org.bukkit.potion.PotionType;
import org.bukkit.projectiles.ProjectileSource;
import org.bukkit.scheduler.BukkitRunnable;
import think.rpgitems.data.Context;
import think.rpgitems.event.BeamEndEvent;
import think.rpgitems.event.BeamHitBlockEvent;
import think.rpgitems.event.BeamHitEntityEvent;
import think.rpgitems.item.ItemManager;
import think.rpgitems.item.RPGItem;
import think.rpgitems.power.*;
import think.rpgitems.power.impl.Undead;
import think.rpgitems.power.marker.Ranged;
import think.rpgitems.power.trigger.BaseTriggers;
import think.rpgitems.power.trigger.Trigger;
import think.rpgitems.support.WGHandler;
import think.rpgitems.support.WGSupport;
import think.rpgitems.utils.LightContext;

import java.util.*;
import java.util.concurrent.ThreadLocalRandom;
import java.util.concurrent.atomic.AtomicBoolean;
import java.util.logging.Level;
import java.util.stream.Stream;

import static think.rpgitems.RPGItems.logger;
import static think.rpgitems.RPGItems.plugin;
import static think.rpgitems.item.RPGItem.DAMAGE_TYPE;

public class Events implements Listener {

    public static final String DAMAGE_SOURCE = "DamageSource";
    public static final String OVERRIDING_DAMAGE = "OverridingDamage";
    public static final String SUPPRESS_MELEE = "SuppressMelee";
    public static final String SUPPRESS_PROJECTILE = "SuppressProjectile";
    public static final String DAMAGE_SOURCE_ITEM = "DamageSourceItem";

    private static final HashSet<Integer> removeProjectiles = new HashSet<>();
    private static final HashMap<Integer, Integer> rpgProjectiles = new HashMap<>();
    private static final Map<UUID, ItemStack> localItemStacks = new HashMap<>();

    private static RPGItem projectileRpgItem;
    private static ItemStack projectileItemStack;
    private static Player projectilePlayer;
    private static final Map<UUID, UUID> projectileRegisterMap = new WeakHashMap<>();
    List<UUID> switchCooldown = new ArrayList<>();

    static private boolean canStack(ItemStack a, ItemStack b) {
        if (a != null && a.getType() == Material.AIR) a = null;
        if (b != null && b.getType() == Material.AIR) b = null;
        if (a == null && b == null) return true;
        if (a != null && b != null) {
            ItemStack ap = a.clone(), bp = b.clone();
            ap.setAmount(1);
            bp.setAmount(1);
            return ap.equals(bp);
        } else {
            return false;
        }
    }

    public static void registerLocalItemStack(UUID entityId, ItemStack item) {
        localItemStacks.put(entityId, item);
    }

    public static boolean hasLocalItemStack(UUID entityId) {
        return localItemStacks.containsKey(entityId);
    }

    public static ItemStack removeLocalItemStack(UUID entityId) {
        return localItemStacks.remove(entityId);
    }

    public static ItemStack getLocalItemStack(UUID entityId) {
        return localItemStacks.get(entityId);
    }

    public static void registerRPGProjectile(RPGItem rpgItem, ItemStack itemStack, Player player, LivingEntity source) {
        if (projectilePlayer != null) {
            projectilePlayer = null;
            throw new IllegalStateException();
        }
        Events.projectileRpgItem = rpgItem;
        Events.projectileItemStack = itemStack;
        Events.projectilePlayer = player;
        projectileRegisterMap.put(source.getUniqueId(), player.getUniqueId());
    }

    public static void registerRPGProjectile(RPGItem rpgItem, ItemStack itemStack, Player player) {
        registerRPGProjectile(rpgItem, itemStack, player, player);
    }

    public static void registerRPGProjectile(int entityId, int uid) {
        rpgProjectiles.put(entityId, uid);
    }

    public static void autoRemoveProjectile(int entityId) {
        removeProjectiles.add(entityId);
    }

    @EventHandler(ignoreCancelled = true, priority = EventPriority.HIGH)
    public void onPlayerExpChange(PlayerExpChangeEvent e) {
        Player p = e.getPlayer();
        Trigger<PlayerExpChangeEvent, PowerExpChange, Void, Void> trigger = BaseTriggers.EXP_CHANGE;

        if (ItemManager.toRPGItem(p.getInventory().getItemInMainHand()).orElse(null) != null) {
            trigger(p, e, p.getInventory().getItemInMainHand(), trigger);
        }
        if (ItemManager.toRPGItem(p.getInventory().getItemInOffHand()).orElse(null) != null) {
            trigger(p, e, p.getInventory().getItemInOffHand(), trigger);
        }

        ItemStack[] armorContents = p.getInventory().getArmorContents();
        Stream.of(armorContents)
                .forEach(i -> {
                    if (ItemManager.toRPGItem(i).orElse(null) != null) {
                        trigger(p, e, i, trigger);
                    }
                });
    }
    @EventHandler(ignoreCancelled = true, priority = EventPriority.HIGH)
    public void onItemMend(PlayerItemMendEvent e) {
        Player p = e.getPlayer();
        Trigger<PlayerItemMendEvent, PowerMending, Void, Void> trigger = BaseTriggers.MENDING;
        if (ItemManager.toRPGItem(p.getInventory().getItemInMainHand()).orElse(null) != null) {
            trigger(p, e, p.getInventory().getItemInMainHand(), trigger);
        }
        if (ItemManager.toRPGItem(p.getInventory().getItemInOffHand()).orElse(null) != null) {
            trigger(p, e, p.getInventory().getItemInOffHand(), trigger);
        }

        ItemStack[] armorContents = p.getInventory().getArmorContents();
        Stream.of(armorContents)
                .forEach(i -> {
                    if (ItemManager.toRPGItem(i).orElse(null) != null) {
                        trigger(p, e, i, trigger);
                    }
                });
    }
    @EventHandler
    public void onItemEnchant(EnchantItemEvent e) {
        Optional<RPGItem> opt = ItemManager.toRPGItem(e.getItem());
        Player p = e.getEnchanter();
        if (opt.isPresent()) {
            RPGItem item = opt.get();
            checkEnchantPerm(e, p, item);
        }
    }

    @EventHandler
    public void onHangingBreak(HangingBreakEvent e) {
        if (e.getCause().equals(RemoveCause.EXPLOSION))
            if (e.getEntity().hasMetadata("RPGItems.Rumble")) {
                e.getEntity().removeMetadata("RPGItems.Rumble", plugin); // Allow the entity to be broken again
                e.setCancelled(true);
            }
    }

    @EventHandler
    public void onBreak(BlockPhysicsEvent e) { // Is not triggered when the block a torch is attached to is removed
        if (e.getChangedType().equals(Material.TORCH))
            if (e.getBlock().hasMetadata("RPGItems.Torch")) {
                e.setCancelled(true); // Cancelling this does not work
                e.getBlock().removeMetadata("RPGItems.Torch", plugin);
                e.getBlock().setType(Material.AIR);
            }
    }

    @EventHandler(ignoreCancelled = true)
    public void onBlockBreak(BlockBreakEvent e) {
        Block block = e.getBlock();
        if (block.getType().equals(Material.TORCH))
            if (block.hasMetadata("RPGItems.Torch"))
                e.setCancelled(true);

        Player player = e.getPlayer();
        ItemStack item = player.getInventory().getItemInMainHand();
        RPGItem rItem;
        if ((rItem = ItemManager.toRPGItem(item).orElse(null)) == null) {
            return;
        }

        boolean can = rItem.breakBlock(player, item, block);
        if (!can) {
            e.setCancelled(true);
        }
        if (rItem.getItemStackDurability(item).map(d -> d <= 0).orElse(false)) {
            player.getInventory().setItemInMainHand(null);
        } else {
            player.getInventory().setItemInMainHand(item);
        }
    }

    @EventHandler
    public void onProjectileHit(ProjectileHitEvent e) {
        final Projectile entity = e.getEntity();
        if (removeProjectiles.contains(entity.getEntityId())) {
            Bukkit.getScheduler().runTask(plugin, () -> {
                if (e.getHitEntity() != null && e.getEntity() instanceof AbstractArrow && ((AbstractArrow) e.getEntity()).getPierceLevel() > 0) {
                    return;
                }
                removeProjectiles.remove(entity.getEntityId());
                entity.remove();
            });
        }
        if (rpgProjectiles.containsKey(entity.getEntityId())) {
            try {
                if (entity instanceof Trident && entity.getScoreboardTags().contains("rgi_projectile")) {
                    ((Trident) entity).setPickupStatus(AbstractArrow.PickupStatus.DISALLOWED);
                }
                RPGItem rItem = ItemManager.getItem(rpgProjectiles.get(entity.getEntityId())).orElse(null);

                if (rItem == null || !(entity.getShooter() instanceof Player player))
                    return;
                if (player.isOnline() && !player.isDead()) {
                    ItemStack item = player.getInventory().getItemInMainHand();
                    RPGItem hItem = ItemManager.toRPGItem(item).orElse(null);

                    final UUID projectileUuid = entity.getUniqueId();
                    if (hasLocalItemStack(projectileUuid)) {
                        item = getLocalItemStack(projectileUuid);
                        new BukkitRunnable() {
                            @Override
                            public void run() {
                                removeLocalItemStack(projectileUuid);
                            }
                        }.runTaskLater(plugin, 1);
                        rItem = ItemManager.toRPGItem(item).orElse(null);
                        if (rItem == null) throw new IllegalStateException();
                    } else {
                        if (rItem != hItem) {
//                            item = player.getInventory().getItemInOffHand();
                            hItem = ItemManager.toRPGItem(item).orElse(null);
                            if (rItem != hItem) {
                                return;
                            }
                        }
                    }

                    List<Ranged> ranged = rItem.getMarker(Ranged.class, true);
                    if (!ranged.isEmpty()) {

                        Location locationP = player.getLocation();
                        Location locationE = e.getEntity().getLocation();
                        if (!locationE.getWorld().equals(locationP.getWorld())) {
                            return;
                        }
                        double distance = locationP.distance(locationE);
                        if (ranged.getFirst().rm > distance || distance > ranged.getFirst().r) {
                            return;
                        }
                    }
                    rItem.power(player, item, e, BaseTriggers.PROJECTILE_HIT);
                }
            } finally {
                Bukkit.getScheduler().runTask(plugin, () -> {
                    if (e.getHitEntity() != null && e.getEntity() instanceof AbstractArrow && ((AbstractArrow) e.getEntity()).getPierceLevel() > 0) {
                        return;
                    }
                    rpgProjectiles.remove(entity.getEntityId());
                });
            }
        }
    }

    @EventHandler(priority = EventPriority.LOW)
    public void onPlayerShootBow(EntityShootBowEvent e) {
        LivingEntity entity = e.getEntity();
        float force = e.getForce();
        if (entity instanceof Player) {
            ItemStack bow = e.getBow();
            Optional<RPGItem> rpgItem = ItemManager.toRPGItem(bow);
            force = rpgItem.flatMap(rpgItem1 -> {
                registerRPGProjectile(e.getProjectile().getEntityId(), rpgItem1.getUid());
                return rpgItem1.power(((Player) entity), bow, e, BaseTriggers.BOW_SHOOT);
            }).orElse(force);
            if (e.isCancelled()) {
                autoRemoveProjectile(entity.getEntityId());
                return;
            }
        }
        if (force == -1) {
            e.setCancelled(true);
            return;
        }
        if (e.getForce() != 0) {
            e.getProjectile().setMetadata("RPGItems.OriginalForce", new FixedMetadataValue(plugin, e.getForce()));
        }
        e.getProjectile().setMetadata("RPGItems.Force", new FixedMetadataValue(plugin, force));
    }

    @EventHandler
    public void onProjectileFire(ProjectileLaunchEvent e) {
        Projectile entity = e.getEntity();
        ProjectileSource shooter = entity.getShooter();
        if (!(shooter instanceof Player) &&
                !((shooter instanceof Entity) && projectileRegisterMap.containsKey(((Entity) shooter).getUniqueId()))
        ) return;
        OfflinePlayer ofp = (shooter instanceof Player) ? (Player) shooter : Bukkit.getOfflinePlayer(projectileRegisterMap.get(((LivingEntity) shooter).getUniqueId()));
        if (!ofp.isOnline()) {
            projectileRpgItem = null;
            projectilePlayer = null;
            projectileItemStack = null;
            projectileRegisterMap.remove(((LivingEntity) shooter).getUniqueId());
            e.setCancelled(true);
            return;
        }
        Player player = ofp.getPlayer();
        if (projectilePlayer != null) {
            if (projectilePlayer != player) {
                projectileRpgItem = null;
                projectilePlayer = null;
                projectileItemStack = null;
                projectileRegisterMap.remove(((LivingEntity) shooter).getUniqueId());
                throw new IllegalStateException();
            }
            registerLocalItemStack(e.getEntity().getUniqueId(), projectileItemStack);
            registerRPGProjectile(e.getEntity().getEntityId(), projectileRpgItem.getUid());
            projectileRpgItem.power(player, projectileItemStack, e, BaseTriggers.LAUNCH_PROJECTILE);
            projectileRpgItem = null;
            projectilePlayer = null;
            projectileItemStack = null;
            projectileRegisterMap.remove(((LivingEntity) shooter).getUniqueId());
            return;
        }

        ItemStack itemInMainHand = player.getInventory().getItemInMainHand();
        ItemStack itemInOffHand = player.getInventory().getItemInOffHand();
        ItemStack item = itemInMainHand;
        RPGItem rItem = ItemManager.toRPGItem(item).orElse(null);
        if (entity instanceof Trident) {
            item = ((Trident) entity).getItemStack();
            rItem = ItemManager.toRPGItem(item).orElse(null);
            if (rItem == null) return;
            UUID uuid = entity.getUniqueId();
            registerLocalItemStack(uuid, item);
            ItemStack fakeItem = rItem.toItemStack();
            List<String> fakeLore = new ArrayList<>(1);
            fakeLore.add(uuid.toString());
            ItemMeta fakeItemItemMeta = fakeItem.getItemMeta();
            fakeItemItemMeta.setLore(fakeLore);
            fakeItem.setItemMeta(fakeItemItemMeta);
            ((Trident) entity).setItemStack(fakeItem);
        } else {
            if (rItem == null) {
                item = itemInOffHand;
                rItem = ItemManager.toRPGItem(item).orElse(null);
                if (rItem == null) {
                    return;
                }
            }
        }
        if (!isThrowable(item)) {
            return;
        }
        boolean isOffhand = itemInOffHand == item;
        // treated in bow shoot event & trident util
        // if minecraft update again, this should be considered.
        if (isChargeable(item) || (isOffhand && isThrowable(itemInMainHand))) {
            return;
        }

        if (ItemManager.canUse(player, rItem) == Event.Result.DENY) {
            return;
        }
        registerRPGProjectile(e.getEntity().getEntityId(), rItem.getUid());
        rItem.power(player, item, e, BaseTriggers.LAUNCH_PROJECTILE);
    }

    private boolean isChargeable(ItemStack itemStack) {
        return itemStack.getType().equals(Material.BOW)
                || itemStack.getType().equals(Material.CROSSBOW);
    }

    private boolean isThrowable(ItemStack itemStack) {
        return itemStack.getType().equals(Material.BOW)
                || itemStack.getType().equals(Material.CROSSBOW)
                || itemStack.getType() == Material.SNOWBALL
                || itemStack.getType() == Material.EGG
                || itemStack.getType() == Material.POTION
                || itemStack.getType() == Material.TRIDENT
                || itemStack.getType() == Material.ENDER_PEARL
                || itemStack.getType() == Material.ENDER_EYE;
    }

    @EventHandler
    public void onPlayerAction(PlayerInteractEvent e) {
        Player player = e.getPlayer();
        Action action = e.getAction();
        Material im = e.getMaterial();
        if (action == Action.PHYSICAL || im == Material.AIR) return;
        if ((action == Action.RIGHT_CLICK_AIR || action == Action.RIGHT_CLICK_BLOCK) && (im == Material.BOW || im == Material.SNOWBALL || im == Material.EGG || im == Material.POTION || im == Material.TRIDENT))
            return;
        RPGItem rItem = ItemManager.toRPGItem(e.getItem()).orElse(null);
        if (rItem == null) return;
        Entity playerTarget = RayTraceUtils.getTargetEntity(player);
        if (!(playerTarget instanceof ItemFrame) && (im.isEdible() || im.isRecord() || isPlacable(im) || isItemConsumer(e.getClickedBlock()))) {
            e.setCancelled(true);
        }
        if (e.getHand() == EquipmentSlot.OFF_HAND) {
            rItem.power(player, e.getItem(), e, BaseTriggers.OFFHAND_CLICK);
        } else if (action == Action.RIGHT_CLICK_AIR) {
            rItem.power(player, e.getItem(), e, BaseTriggers.RIGHT_CLICK);
        } else if (action == Action.RIGHT_CLICK_BLOCK &&
                !(e.getClickedBlock().getType().isInteractable() && !player.isSneaking())) {
            rItem.power(player, e.getItem(), e, BaseTriggers.RIGHT_CLICK);
        } else if (action == Action.LEFT_CLICK_AIR || action == Action.LEFT_CLICK_BLOCK) {
            rItem.power(player, e.getItem(), e, BaseTriggers.LEFT_CLICK);
        }
    }

    private boolean isPlacable(Material im) {
        return switch (im) {
            case ARMOR_STAND, MINECART, CHEST_MINECART, COMMAND_BLOCK_MINECART, FURNACE_MINECART, TNT_MINECART,
                 HOPPER_MINECART, END_CRYSTAL, PRISMARINE_CRYSTALS, BUCKET, LAVA_BUCKET, WATER_BUCKET, COD_BUCKET,
                 PUFFERFISH_BUCKET, SALMON_BUCKET, TROPICAL_FISH_BUCKET, SADDLE, LEAD, BOWL -> true;
            default -> false;
        };
    }

    private boolean isTools(Material im) {
        return switch (im) {
            //<editor-fold>
            case BOW, CROSSBOW, NETHERITE_AXE, DIAMOND_AXE, GOLDEN_AXE, IRON_AXE, STONE_AXE, WOODEN_AXE, NETHERITE_PICKAXE, DIAMOND_PICKAXE,
                 GOLDEN_PICKAXE, IRON_PICKAXE, STONE_PICKAXE, WOODEN_PICKAXE, NETHERITE_HOE, DIAMOND_HOE, GOLDEN_HOE, IRON_HOE,
                 STONE_HOE, WOODEN_HOE, NETHERITE_SHOVEL, DIAMOND_SHOVEL, GOLDEN_SHOVEL, IRON_SHOVEL, STONE_SHOVEL, WOODEN_SHOVEL,
                 FISHING_ROD, SHEARS, LEAD, FLINT_AND_STEEL ->
                //</editor-fold>
                    true;
            default -> false;
        };
    }

    public boolean isItemConsumer(Block im) {
        if (im == null) return false;
        return switch (im.getType()) {
            // <editor-fold defaultstate="collapsed" desc="isInteractable">
            case COMPOSTER, JUKEBOX, FLOWER_POT ->

                // </editor-fold>
                    true;
            default -> false;
        };
    }

    @EventHandler
    public void onPlayerSneak(PlayerToggleSneakEvent e) {
        if (!e.isSneaking()) {
            return;
        }
        Player p = e.getPlayer();
        Trigger<PlayerToggleSneakEvent, PowerSneak, Void, Void> trigger = BaseTriggers.SNEAK;

        trigger(p, e, p.getInventory().getItemInMainHand(), trigger);

        ItemStack[] armorContents = p.getInventory().getArmorContents();
        Stream.of(armorContents)
                .forEach(i -> trigger(p, e, i, trigger));
    }

    <TEvent extends Event, TPower extends Pimpl, TResult, TReturn> TReturn trigger(Player player, TEvent event, ItemStack itemStack, Trigger<TEvent, TPower, TResult, TReturn> trigger) {
        Optional<RPGItem> rpgItem = ItemManager.toRPGItem(itemStack);
        return rpgItem.map(r -> r.power(player, itemStack, event, trigger)).orElse(null);
    }

    @EventHandler
    public void onPlayerSwim(EntityToggleSwimEvent e) {
        if(!(e.getEntity() instanceof Player p)||!e.isSwimming()){
            return;
        }
        Trigger<EntityToggleSwimEvent, PowerSwim, Void, Void> swim = BaseTriggers.SWIM;

        trigger(p, e, p.getInventory().getItemInMainHand(), swim);
        ItemStack[] armorContents = p.getInventory().getArmorContents();
        Stream.of(armorContents)
                .forEach(i -> trigger(p, e, i, swim));
    }

    @EventHandler
    public void onPlayerJump(PlayerJumpEvent e) {
        Player p = e.getPlayer();
        Trigger<PlayerJumpEvent, PowerJump, Void, Void> jump = BaseTriggers.JUMP;

        trigger(p, e, p.getInventory().getItemInMainHand(), jump);
        ItemStack[] armorContents = p.getInventory().getArmorContents();
        Stream.of(armorContents)
                .forEach(i -> trigger(p, e, i, jump));
    }

    @EventHandler
    public void onPlayerSprint(PlayerToggleSprintEvent e) {
        if (!e.isSprinting()) {
            return;
        }
        Player p = e.getPlayer();
        Trigger<PlayerToggleSprintEvent, PowerSprint, Void, Void> sprint = BaseTriggers.SPRINT;

        trigger(p, e, p.getInventory().getItemInMainHand(), sprint);
        ItemStack[] armorContents = p.getInventory().getArmorContents();
        Stream.of(armorContents)
                .forEach(i -> trigger(p, e, i, sprint));

    }

    @EventHandler
    public void onSwapHand(PlayerSwapHandItemsEvent e) {
        Player player = e.getPlayer();
        ItemStack ois = e.getOffHandItem();
        ItemStack mis = e.getMainHandItem();
        RPGItem mitem = ItemManager.toRPGItem(mis).orElse(null);
        RPGItem oitem = ItemManager.toRPGItem(ois).orElse(null);

        if (mitem != null) {
            Boolean cont = mitem.power(player, mis, e, BaseTriggers.SWAP_TO_MAINHAND);
            if (!cont) e.setCancelled(true);
        }

        if (oitem != null) {
            Boolean cont = oitem.power(player, ois, e, BaseTriggers.SWAP_TO_OFFHAND);
            if (!cont) e.setCancelled(true);
        }
    }

    @EventHandler
    public void onOffhandInventoryClick(InventoryClickEvent e) {
        if (e.getInventory().getType() != InventoryType.CRAFTING || e.getSlotType() != InventoryType.SlotType.QUICKBAR || e.getSlot() != 40)
            return;
        if (!(e.getWhoClicked() instanceof Player player)) return;
        ItemStack currentIs = e.getCurrentItem();
        ItemStack cursorIs = e.getCursor();
        RPGItem currentItem = ItemManager.toRPGItem(currentIs).orElse(null);
        RPGItem cursorItem = ItemManager.toRPGItem(cursorIs).orElse(null);

        if (currentItem != null && (e.getAction() == InventoryAction.PICKUP_SOME || e.getAction() == InventoryAction.PICKUP_ALL || e.getAction() == InventoryAction.PICKUP_ONE || e.getAction() == InventoryAction.PICKUP_HALF || e.getAction() == InventoryAction.DROP_ALL_SLOT || e.getAction() == InventoryAction.DROP_ONE_CURSOR)) {
            Boolean cont = currentItem.power(player, currentIs, e, BaseTriggers.PICKUP_OFF_HAND);
            if (!cont) e.setCancelled(true);
        }

        if (cursorItem != null && (e.getAction() == InventoryAction.PLACE_SOME || e.getAction() == InventoryAction.PLACE_ONE || e.getAction() == InventoryAction.PLACE_ALL)) {
            Boolean cont = cursorItem.power(player, cursorIs, e, BaseTriggers.PLACE_OFF_HAND);
            if (!cont) e.setCancelled(true);
        }
    }

    @EventHandler
    public void onBlockPlace(BlockPlaceEvent e) {
        ItemStack item;
        if (e.getHand() == EquipmentSlot.OFF_HAND) {
            item = e.getPlayer().getInventory().getItemInOffHand();
        } else {
            item = e.getItemInHand();
        }
        if (item.getType() == Material.AIR)
            return;

        RPGItem rItem = ItemManager.toRPGItem(item).orElse(null);
        if (rItem == null)
            return;
        e.setCancelled(true);
    }

    @EventHandler
    public void onPlayerJoin(PlayerJoinEvent e) {
        Player player = e.getPlayer();
        PlayerInventory in = player.getInventory();
        for (int i = 0; i < in.getSize(); i++) {
            ItemStack item = in.getItem(i);
            ItemManager.toRPGItemByMeta(item).ifPresent(rpgItem -> rpgItem.updateItem(item,player));
        }
        for (ItemStack item : player.getInventory().getArmorContents()) {
            ItemManager.toRPGItemByMeta(item).ifPresent(rpgItem -> rpgItem.updateItem(item,player));
        }
        if (WGSupport.hasSupport() && WGSupport.useWorldGuard) {
            WGHandler.onPlayerJoin(e);
        }
    }

    @SuppressWarnings("deprecation")
    @EventHandler(priority = EventPriority.HIGHEST, ignoreCancelled = true)
    public void onPlayerPickupTrident(PlayerPickupArrowEvent e) {
        if (e.getItem().getItemStack().getType() != Material.TRIDENT || !e.getItem().getItemStack().hasItemMeta()) {
            return;
        }
        ItemStack tridentItem = e.getItem().getItemStack();
        ItemMeta itemMeta = tridentItem.getItemMeta();
        if (!rpgProjectiles.containsKey(e.getArrow().getEntityId()) || !itemMeta.hasLore() || itemMeta.getLore().isEmpty()) {
            return;
        }
        try {
            UUID uuid = UUID.fromString(itemMeta.getLore().getFirst());
            ItemStack realItem = removeLocalItemStack(uuid);
            if (realItem != null) {
                if (realItem.getType() == Material.AIR) {
                    e.getArrow().setPickupStatus(Arrow.PickupStatus.DISALLOWED);
                    e.getArrow().setPersistent(false);
                    e.setCancelled(true);
                    Bukkit.getScheduler().runTaskLater(plugin, () -> e.getArrow().remove(), 100L);
                } else {
                    RPGItem.updateItemStack(realItem,e.getPlayer());
                    e.getItem().setItemStack(realItem);
                }
            }
        } catch (IllegalArgumentException ex) {
            logger.log(Level.WARNING, "Exception when PlayerPickupArrowEvent. May be harmless.", ex);
        }
    }

    @EventHandler(priority = EventPriority.MONITOR, ignoreCancelled = true)
    public void onPlayerPickup(EntityPickupItemEvent e) {
        if (!(e.getEntity() instanceof Player p)) {
            return;
        }
        new BukkitRunnable() {
            @Override
            public void run() {
                if (!p.isOnline()) return;
                PlayerInventory in = p.getInventory();
                for (int i = 0; i < in.getSize(); i++) {
                    ItemStack item = in.getItem(i);
                    ItemManager.toRPGItem(item).ifPresent(rpgItem -> rpgItem.updateItem(item,p));
                }
                for (ItemStack item : in.getArmorContents()) {
                    ItemManager.toRPGItem(item).ifPresent(rpgItem -> rpgItem.updateItem(item,p));
                }
            }
        }.runTaskLater(plugin, 1L);
    }

    @EventHandler
    public void onInventoryClose(InventoryCloseEvent e) {
        updatePlayerInventory(e.getInventory(), (Player) e.getPlayer(), e);
    }

    @EventHandler
    public void onPlayerChangeItem(PlayerItemHeldEvent ev) {
        Player player = ev.getPlayer();
        ItemStack item = player.getInventory().getItem(ev.getNewSlot());
        ItemManager.toRPGItem(item).ifPresent(rpgItem -> rpgItem.updateItem(item,player));
        if (switchCooldown.contains(player.getUniqueId())) return;
        ItemStack[] armorContents = player.getInventory().getArmorContents();
        for (ItemStack stack : armorContents) {
            ItemManager.toRPGItem(stack).ifPresent(rpgItem -> rpgItem.updateItem(stack, player));
        }
        ItemStack offhandItem = player.getInventory().getItemInOffHand();
        ItemManager.toRPGItem(offhandItem).ifPresent(rpgItem -> rpgItem.updateItem(offhandItem,player));
        switchCooldown.add(player.getUniqueId());
        new BukkitRunnable() {
            @Override
            public void run() {
                switchCooldown.remove(player.getUniqueId());
            }
        }.runTaskLater(plugin, 20);
    }


    private void updatePlayerInventory(Inventory inventory, Player p, InventoryEvent e) {
        Iterator<ItemStack> it = inventory.iterator();
        try {
            while (it.hasNext()) {
                ItemStack item = it.next();
                ItemManager.toRPGItemByMeta(item).ifPresent(rpgItem -> rpgItem.updateItem(item,p));
            }
            PlayerInventory inventory1 = p.getInventory();
            it = inventory1.iterator();
            while (it.hasNext()) {
                ItemStack item = it.next();
                ItemManager.toRPGItemByMeta(item).ifPresent(rpgItem -> rpgItem.updateItem(item,p));
            }
        } catch (ArrayIndexOutOfBoundsException ex) {
            logger.log(Level.WARNING, "Exception when InventoryOpenEvent. May be harmless.", ex);
            // Fix for the bug with anvils in craftbukkit
        }
    }

    @EventHandler(ignoreCancelled = true, priority = EventPriority.HIGHEST)
    public void onInventoryClick(InventoryClickEvent e) {
        if (e.getClickedInventory() instanceof AnvilInventory) {
            if (e.getRawSlot() == 2) {
                HumanEntity p = e.getWhoClicked();
                ItemStack ind1 = e.getView().getItem(0);
                ItemStack ind2 = e.getView().getItem(1);
                Optional<RPGItem> opt1 = ItemManager.toRPGItemByMeta(ind1);
                Optional<RPGItem> opt2 = ItemManager.toRPGItemByMeta(ind2);
                AtomicBoolean hasRGI = new AtomicBoolean(false);
                opt1.ifPresent(item -> {
                    checkEnchantPerm(e, p, item);
                    hasRGI.set(true);
                });
                opt2.ifPresent(item -> {
                    checkEnchantPerm(e, p, item);
                    hasRGI.set(true);
                });

                if (hasRGI.get() && !plugin.cfg.allowAnvilEnchant) {
                    e.setCancelled(true);
                }
            }
        }
    }

    public void checkEnchantPerm(Cancellable e, HumanEntity p, RPGItem itemStack) {
        RPGItem.EnchantMode enchantMode = itemStack.getEnchantMode();
        if (enchantMode == RPGItem.EnchantMode.DISALLOW) {
            e.setCancelled(true);
        } else if (enchantMode == RPGItem.EnchantMode.PERMISSION && !(p.hasPermission("rpgitem.enchant." + itemStack.getName()) || p.hasPermission("rpgitem.enchant." + itemStack.getUid()))) {
            e.setCancelled(true);
        }
    }

    //this function can't catch event when player open their backpack.
    //fuck there's no event when player open their backpack.
    @EventHandler(ignoreCancelled = true, priority = EventPriority.HIGHEST)
    public void onInventoryOpen(final InventoryOpenEvent e) {
        if (e.getInventory().getHolder() == null || e.getInventory().getLocation() == null)
            return;
        if (e.getInventory().getType() != InventoryType.CHEST) {
            updatePlayerInventory(e.getInventory(), (Player) e.getPlayer(), e);
        }
    }

    @EventHandler(priority = EventPriority.HIGH, ignoreCancelled = true)
    public void onDamage(EntityDamageByEntityEvent ev) {
        if (ev.getDamager() instanceof Player) {
            playerDamager(ev);
        } else if (ev.getDamager() instanceof Projectile) {
            projectileDamager(ev);
        }
    }

    private void playerDamager(EntityDamageByEntityEvent e) {
        Player player = (Player) e.getDamager();
        Entity entity = e.getEntity();
        ItemStack item = player.getInventory().getItemInMainHand();

        if (e.getCause() == EntityDamageEvent.DamageCause.THORNS)
            return;

        Optional<Boolean> suppressMelee = LightContext.getTemp(player.getUniqueId(), SUPPRESS_MELEE);
        Optional<Double> overridingDamage = LightContext.getTemp(player.getUniqueId(), OVERRIDING_DAMAGE);
        Optional<ItemStack> sourceItem = LightContext.getTemp(player.getUniqueId(), DAMAGE_SOURCE_ITEM);

        if (overridingDamage.isEmpty()) {
            overridingDamage = Optional.ofNullable(Context.instance().getDouble(player.getUniqueId(), OVERRIDING_DAMAGE));
        }

        if (sourceItem.isPresent()) {
            item = sourceItem.get();
        } else {
            sourceItem = Optional.ofNullable((ItemStack) Context.instance().get(player.getUniqueId(), DAMAGE_SOURCE_ITEM));
            if (sourceItem.isPresent()) {
                item = sourceItem.get();
            }
        }

        if (suppressMelee.isPresent() && suppressMelee.get()) {
            overridingDamage.ifPresent(e::setDamage);
            return;
        } else {
            suppressMelee = Optional.ofNullable(Context.instance().getBoolean(player.getUniqueId(), SUPPRESS_MELEE));
            if (suppressMelee.isPresent() && suppressMelee.get()) {
                overridingDamage.ifPresent(e::setDamage);
                return;
            }
        }

        RPGItem rItem = ItemManager.toRPGItem(item).orElse(null);

        if (rItem != null && e.getCause().equals(EntityDamageEvent.DamageCause.BLOCK_EXPLOSION) || e.getCause().equals(EntityDamageEvent.DamageCause.ENTITY_EXPLOSION)) {
            int damageMin = rItem.getDamageMin();
            int damageMax = rItem.getDamageMax();
            double dmg = damageMin < damageMax ? ThreadLocalRandom.current().nextDouble(damageMin, damageMax) : damageMax;
            overridingDamage = Optional.of(dmg);
        }

        double originDamage = e.getDamage();
        double damage = originDamage;
        if (rItem != null && overridingDamage.isEmpty()) {
            damage = rItem.meleeDamage(player, originDamage, item, entity);
        } else if (overridingDamage.isPresent()) {
            damage = overridingDamage.get();
        }
        if (damage == -1) {
            e.setCancelled(true);
            return;
        }
        e.setDamage(damage);
        if (!(entity instanceof LivingEntity)) return;
        if (rItem != null) {
            String damageType = rItem.getDamageType();
            Context.instance().putTemp(player.getUniqueId(), DAMAGE_TYPE, damageType);
            damage = rItem.power(player, item, e, BaseTriggers.HIT).orElse(damage);
        }
        ItemStack[] inventory = player.getInventory().getContents();
        runGlobalHitTrigger(e, player, damage, rItem == null ? "" : rItem.getDamageType(), inventory);
    }

    private void projectileDamager(EntityDamageByEntityEvent e) {
        Projectile projectile = (Projectile) e.getDamager();
        Integer projectileID = rpgProjectiles.get(projectile.getEntityId());
        if (projectileID == null) {
            if (projectile.hasMetadata("RPGItems.OriginalForce")) {
                double damage = e.getDamage() * projectile.getMetadata("RPGItems.Force").getFirst().asFloat() / projectile.getMetadata("RPGItems.OriginalForce").getFirst().asFloat();
                e.setDamage(damage);
            }
            return;
        }
        RPGItem rItem = ItemManager.getItem(projectileID).orElse(null);
        if (rItem == null || !(projectile.getShooter() instanceof Player player))
            return;
        if (!((Player) projectile.getShooter()).isOnline()) {
            return;
        }
        ItemStack item = player.getInventory().getItemInMainHand();
        RPGItem hItem = ItemManager.toRPGItem(item).orElse(null);

        if (hasLocalItemStack(projectile.getUniqueId())) {
            item = getLocalItemStack(projectile.getUniqueId());
            rItem = ItemManager.toRPGItem(item).orElse(null);
            if (rItem == null) throw new IllegalStateException();
        } else {
            if (rItem != hItem) {
//                item = player.getInventory().getItemInOffHand();
                hItem = ItemManager.toRPGItem(item).orElse(null);
                if (rItem != hItem) {
//                    return;
                }
            }
        }

        Boolean suppressProjectile = Context.instance().getBoolean(player.getUniqueId(), SUPPRESS_PROJECTILE);
        Double overridingDamage = Context.instance().getDouble(player.getUniqueId(), OVERRIDING_DAMAGE);

        if (suppressProjectile != null && suppressProjectile) {
            if (overridingDamage != null) {
                e.setDamage(overridingDamage);
            }
            return;
        }

        double originDamage = e.getDamage();
        double damage;
        if (overridingDamage == null) {
            damage = rItem.projectileDamage(player, originDamage, item, projectile, e.getEntity());
        } else {
            damage = overridingDamage;
        }
        if (damage == -1) {
            e.setCancelled(true);
            return;
        }
        e.setDamage(damage);
        if (!(e.getEntity() instanceof LivingEntity)) return;
        ItemStack[] armorContents = player.getInventory().getContents();
        String damageType = rItem.getDamageType();
        Context.instance().putTemp(player.getUniqueId(), DAMAGE_TYPE, damageType);
        damage = rItem.power(player, item, e, BaseTriggers.HIT).orElse(damage);
        runGlobalHitTrigger(e, player, damage, damageType, armorContents);
    }

    private void runGlobalHitTrigger(EntityDamageByEntityEvent e, Player player, double damage, String damageType, ItemStack[] itemStacks) {
        ItemStack itemInMainHand = player.getInventory().getItemInMainHand();
        for (ItemStack itemStack : itemStacks) {
            if (itemStack == null) continue;
            if (itemStack.equals(itemInMainHand)) continue;
            RPGItem rpgItem = ItemManager.toRPGItem(itemStack).orElse(null);
            if (rpgItem == null) continue;
            Context.instance().putTemp(player.getUniqueId(), DAMAGE_TYPE, damageType);
            damage = rpgItem.power(player, itemStack, e, BaseTriggers.HIT_GLOBAL).orElse(damage);
        }
        if (damage <= 0) {
            e.setCancelled(true);
            e.setDamage(0);
        }
        e.setDamage(Math.max(damage, 0));
    }

    @EventHandler(priority = EventPriority.HIGH, ignoreCancelled = true)
    public void onPlayerHit(EntityDamageEvent e) {
        if (e.getEntity() instanceof Player player) {
            ItemStack[] armour = player.getInventory().getArmorContents();
            boolean hasRPGItem = false;
            double damage = e.getDamage();
            Entity damager = null;
            if (e instanceof EntityDamageByEntityEvent) {
                damager = ((EntityDamageByEntityEvent) e).getDamager();
            }
            for (ItemStack pArmour : armour) {
                RPGItem pRItem = ItemManager.toRPGItem(pArmour).orElse(null);
                if (pRItem == null) {
                    continue;
                }
                hasRPGItem = true;
                damage = pRItem.takeDamage(player, damage, pArmour, damager);
            }
            for (ItemStack pArmour : armour) {
                try {
                    RPGItem pRItem = ItemManager.toRPGItem(pArmour).orElse(null);
                    if (pRItem == null) {
                        continue;
                    }
                    damage = Utils.eval(player, damage, e, damager, pRItem);
                } catch (Exception ignored) {
                }
            }
            if (hasRPGItem) {
                player.getInventory().setArmorContents(armour);
            }
            e.setDamage(damage);
        }
    }

    @EventHandler(priority = EventPriority.HIGHEST, ignoreCancelled = true)
    public void onPlayerHitTaken(EntityDamageEvent ev) {
        if (ev.getEntity() instanceof Player entity) {
            ev.setDamage(playerHitTaken(entity, ev));
            double finalDamage = ev.getFinalDamage();
            if (finalDamage >= entity.getHealth()) {
                triggerRescue(entity, ev);
            }
        }
    }

    private void triggerRescue(Player entity, EntityDamageEvent ev) {
        double ret = ev.getDamage();
        for (ItemStack item : entity.getInventory().getContents()) {
            RPGItem ri = ItemManager.toRPGItem(item).orElse(null);
            if (ri == null) continue;
            ri.power(entity, item, ev, BaseTriggers.DYING);
        }
    }

    private double playerHitTaken(Player e, EntityDamageEvent ev) {
        double ret = ev.getDamage();
        for (ItemStack item : e.getInventory().getContents()) {
            RPGItem ri = ItemManager.toRPGItem(item).orElse(null);
            if (ri == null) continue;
            ret = ri.power(e, item, ev, BaseTriggers.HIT_TAKEN).orElse(ret);
        }
        return ret;
    }

    @EventHandler(priority = EventPriority.MONITOR, ignoreCancelled = true)
    private void onPlayerHurt(EntityDamageByEntityEvent ev) {
        if (ev.getEntity() instanceof Player e) {
            for (ItemStack item : e.getInventory().getContents()) {
                RPGItem ri = ItemManager.toRPGItem(item).orElse(null);
                if (ri == null) continue;
                ri.power(e, item, ev, BaseTriggers.HURT);
            }
        }
    }

    @EventHandler(ignoreCancelled = true, priority = EventPriority.NORMAL)
    public void onItemDamage(PlayerItemDamageEvent e) {
        if (ItemManager.toRPGItem(e.getItem()).isPresent()) {
            e.setCancelled(true);
        }
    }

    @EventHandler(ignoreCancelled = true, priority = EventPriority.HIGH)
    public void onBeamHitBlock(BeamHitBlockEvent event) {
        Player player = event.getPlayer();
        if (player == null) return;

        RPGItem rItem = ItemManager.toRPGItem(event.getItemStack()).orElse(null);
        if (rItem == null) return;

        rItem.power(player, event.getItemStack(), event, BaseTriggers.BEAM_HIT_BLOCK, null);
//        ItemStack itemInMainHand = player.getInventory().getItemInMainHand();
//        ItemStack[] contents = player.getInventory().getContents();
//        for (ItemStack content : contents) {
//            if (content == null) continue;
//            if (content.equals(itemInMainHand))continue;
//            RPGItem rpgItem = ItemManager.toRPGItem(content).orElse(null);
//            if (rpgItem == null) continue;
//
//            rpgItem.power(player, content, event, BaseTriggers.BEAM_HIT_BLOCK, null);
//        }
    }

    @EventHandler(ignoreCancelled = true, priority = EventPriority.HIGH)
    public void onBeamHitEntity(BeamHitEntityEvent event) {
        Player player = event.getPlayer();
        if (player == null) return;

        RPGItem rItem = ItemManager.toRPGItem(event.getItemStack()).orElse(null);
        if (rItem == null) return;

        rItem.power(player, event.getItemStack(), event, BaseTriggers.BEAM_HIT_ENTITY, null);
//        ItemStack itemInMainHand = player.getInventory().getItemInMainHand();
//        ItemStack[] contents = player.getInventory().getContents();
//        for (ItemStack content : contents) {
//            if (content == null) continue;
//            RPGItem rpgItem = ItemManager.toRPGItem(content).orElse(null);
//            if (rpgItem == null) continue;
//
//            rpgItem.power(player, content, event, BaseTriggers.BEAM_HIT_ENTITY, null);
//        }
    }
    @EventHandler(ignoreCancelled = true, priority = EventPriority.HIGH)
    public void onBeamEnd(BeamEndEvent event) {
        Player player = event.getPlayer();
        if (player == null) return;

        RPGItem rItem = ItemManager.toRPGItem(event.getItemStack()).orElse(null);
        if (rItem == null) return;

        rItem.power(player, event.getItemStack(), event, BaseTriggers.BEAM_END, null);
    }
    @EventHandler(ignoreCancelled = true, priority = EventPriority.HIGHEST)
    public void onEggHatch(ThrownEggHatchEvent event) {
        Egg egg = event.getEgg();
        NamespacedKey hatchOrNot = new NamespacedKey(RPGItems.plugin,"RPGItemHatchOrNot");
        NamespacedKey hacthNumber = new NamespacedKey(RPGItems.plugin,"RPGItemHatchNumber");
        NamespacedKey hatchEntity = new NamespacedKey(RPGItems.plugin,"RPGItemHatchEntity");
        if(egg.getPersistentDataContainer().has(hatchOrNot)){
            Boolean shouldHatch = egg.getPersistentDataContainer().get(hatchOrNot, PersistentDataType.BOOLEAN);
            if(Boolean.FALSE.equals(shouldHatch)){
                event.setHatching(false);
                return;
            }
        }
        if(egg.getPersistentDataContainer().has(hacthNumber)){
            Integer hatchNumber = egg.getPersistentDataContainer().get(hacthNumber, PersistentDataType.INTEGER);
            if(hatchNumber != null){
                if(hatchNumber != -1){
                    event.setNumHatches(hatchNumber.byteValue());
                }
            }
        }
        if(egg.getPersistentDataContainer().has(hatchEntity)){
            String entityType = egg.getPersistentDataContainer().get(hatchEntity, PersistentDataType.STRING);
            if(entityType != null){
                event.setHatchingType(EntityType.valueOf(entityType));
            }
        }
    }
    //For power Undead only
    @EventHandler(ignoreCancelled = true,priority = EventPriority.HIGH)
    public void onWitherAndUndeadTarget(EntityTargetEvent event){
        if((event.getEntity() instanceof Wither || EntityTags.UNDEADS.isTagged(event.getEntityType()))&& event.getTarget() instanceof Player p){
            PlayerInventory inventory = p.getInventory();
            ItemManager.toRPGItemByMeta(inventory.getItemInOffHand()).ifPresent(rpgItem -> {
                for (Power power : rpgItem.getPowers()){
                    if(power.getName().equals("undead")){
                        Undead undead = (Undead) power;
                        if(undead.getAllowOffHand()){
                            event.setCancelled(true);
                        }
                    }
                }
            });
            ItemManager.toRPGItemByMeta(inventory.getItemInMainHand()).ifPresent(rpgItem -> {
                for (Power power : rpgItem.getPowers()){
                    if(power.getName().equals("undead")) event.setCancelled(true);
                }
            });
            for(ItemStack i : inventory.getArmorContents()){
                ItemManager.toRPGItemByMeta(i).ifPresent(rpgItem -> {
                    for (Power power : rpgItem.getPowers()){
                        if(power.getName().equals("undead")) event.setCancelled(true);
                    }
                });
            }
        }
    }

    //For power Undead only
    @EventHandler(ignoreCancelled = true,priority = EventPriority.HIGHEST)
    public void onInstantPotion(EntityPotionEffectEvent event){
        if(event.getEntity() instanceof Player p && event.getCause() != EntityPotionEffectEvent.Cause.PLUGIN && event.getAction()== EntityPotionEffectEvent.Action.ADDED && (event.getModifiedType()==PotionEffectType.INSTANT_HEALTH||event.getModifiedType()==PotionEffectType.INSTANT_DAMAGE)){
            PlayerInventory inventory = p.getInventory();
            int amplifier = event.getNewEffect().getAmplifier();
            int duration = event.getNewEffect().getDuration();
            boolean icon = event.getNewEffect().hasIcon();
            boolean particle = event.getNewEffect().hasParticles();
            boolean ambient = event.getNewEffect().isAmbient();
            ItemManager.toRPGItemByMeta(inventory.getItemInOffHand()).ifPresent(rpgItem -> {
                for (Power power : rpgItem.getPowers()){
                    if(power.getName().equals("undead")){
                        Undead undead = (Undead) power;
                        if(undead.getAllowOffHand()){
                            event.setCancelled(true);
                            if(event.getNewEffect().getType()== PotionEffectType.INSTANT_HEALTH){
                                PotionEffect potion = new PotionEffect(PotionEffectType.INSTANT_DAMAGE,duration,amplifier,ambient,particle,icon);
                                p.addPotionEffect(potion);
                            }
                            else{
                                PotionEffect potion = new PotionEffect(PotionEffectType.INSTANT_HEALTH,duration,amplifier,ambient,particle,icon);
                                p.addPotionEffect(potion);
                            }
                        }
                    }
                }
            });
            ItemManager.toRPGItemByMeta(inventory.getItemInMainHand()).ifPresent(rpgItem -> {
                for (Power power : rpgItem.getPowers()){
                    if(power.getName().equals("undead")){
                        event.setCancelled(true);
                        if(event.getNewEffect().getType()== PotionEffectType.INSTANT_HEALTH){
                            PotionEffect potion = new PotionEffect(PotionEffectType.INSTANT_DAMAGE,duration,amplifier,ambient,particle,icon);
                            p.addPotionEffect(potion);
                        }
                        else{
                            PotionEffect potion = new PotionEffect(PotionEffectType.INSTANT_HEALTH,duration,amplifier,ambient,particle,icon);
                            p.addPotionEffect(potion);
                        }
                    }
                }
            });
            for(ItemStack i : inventory.getArmorContents()){
                ItemManager.toRPGItemByMeta(i).ifPresent(rpgItem -> {
                    for (Power power : rpgItem.getPowers()){
                        if(power.getName().equals("undead")){
                            event.setCancelled(true);
                            if(event.getNewEffect().getType()== PotionEffectType.INSTANT_HEALTH){
                                PotionEffect potion = new PotionEffect(PotionEffectType.INSTANT_DAMAGE,duration,amplifier,ambient,particle,icon);
                                p.addPotionEffect(potion);
                            }
                            else{
                                PotionEffect potion = new PotionEffect(PotionEffectType.INSTANT_HEALTH,duration,amplifier,ambient,particle,icon);
                                p.addPotionEffect(potion);
                            }
                        }
                    }
                });
            }
        }
    }

    //For power Undead only
    @EventHandler(ignoreCancelled = true, priority = EventPriority.HIGHEST)
    public void onInstantSplashPotion(PotionSplashEvent event) {
        List<LivingEntity> affectedEntities = new ArrayList<>(event.getAffectedEntities());

        for (int j = 0;j < affectedEntities.size();j++) {
            Entity entity = affectedEntities.get(j);
            if (entity instanceof Player player) {
                boolean instantDetected = false;
                Collection<PotionEffect> potionEffects = event.getPotion().getEffects();
                int instantAmplifier = 0;
                int instantDuration = 1;
                for (PotionEffect potionEffect : potionEffects) {
                    if (potionEffect.getType() == PotionEffectType.INSTANT_HEALTH || potionEffect.getType() == PotionEffectType.INSTANT_DAMAGE) {
                        instantDetected = true;
                        instantAmplifier = potionEffect.getAmplifier();
                        break;
                    }
                }
                if (instantDetected) {
                    PlayerInventory inventory = player.getInventory();
                    int finalInstantAmplifier = instantAmplifier;
                    ItemManager.toRPGItemByMeta(inventory.getItemInOffHand()).ifPresent(rpgItem -> {
                        for (Power power : rpgItem.getPowers()){
                            if(power.getName().equals("undead")&&((Undead)power).getAllowOffHand()){
                                event.setIntensity(player,0);
                                for (PotionEffect potionEffect : potionEffects) {
                                    if (potionEffect.getType() == PotionEffectType.INSTANT_HEALTH) {
                                        player.addPotionEffect(new PotionEffect(PotionEffectType.INSTANT_DAMAGE, instantDuration, finalInstantAmplifier, false, false, true));
                                    } else if (potionEffect.getType() == PotionEffectType.INSTANT_DAMAGE) {
                                        player.addPotionEffect(new PotionEffect(PotionEffectType.INSTANT_HEALTH, instantDuration, finalInstantAmplifier, false, false, true));
                                    } else {
                                        player.addPotionEffect(potionEffect);
                                    }
                                }
                                return;
                            }
                        }
                    });
                    ItemManager.toRPGItemByMeta(inventory.getItemInMainHand()).ifPresent(rpgItem -> {
                        for (Power power : rpgItem.getPowers()){
                            if(power.getName().equals("undead")){
                                event.setIntensity(player,0);
                                for (PotionEffect potionEffect : potionEffects) {
                                    if (potionEffect.getType() == PotionEffectType.INSTANT_HEALTH) {
                                        player.addPotionEffect(new PotionEffect(PotionEffectType.INSTANT_DAMAGE, instantDuration, finalInstantAmplifier, false, false, true));
                                    } else if (potionEffect.getType() == PotionEffectType.INSTANT_DAMAGE) {
                                        player.addPotionEffect(new PotionEffect(PotionEffectType.INSTANT_HEALTH, instantDuration, finalInstantAmplifier, false, false, true));
                                    } else {
                                        player.addPotionEffect(potionEffect);
                                    }
                                }
                                return;
                            }
                        }
                    });
                    for(ItemStack i : inventory.getArmorContents()){
                        int finalInstantAmplifier1 = instantAmplifier;
                        ItemManager.toRPGItemByMeta(i).ifPresent(rpgItem -> {
                            for (Power power : rpgItem.getPowers()){
                                if(power.getName().equals("undead")){
                                    event.setIntensity(player,0);
                                    for (PotionEffect potionEffect : potionEffects) {
                                        if (potionEffect.getType() == PotionEffectType.INSTANT_HEALTH) {
                                            player.addPotionEffect(new PotionEffect(PotionEffectType.INSTANT_DAMAGE, instantDuration, finalInstantAmplifier1, false, false, true));
                                        } else if (potionEffect.getType() == PotionEffectType.INSTANT_DAMAGE) {
                                            player.addPotionEffect(new PotionEffect(PotionEffectType.INSTANT_HEALTH, instantDuration, finalInstantAmplifier1, false, false, true));
                                        } else {
                                            player.addPotionEffect(potionEffect);
                                        }
                                    }
                                    return;
                                }
                            }
                        });
                    }
                }
            }
        }
    }

    //For power Undead only
    @EventHandler(ignoreCancelled = true, priority = EventPriority.HIGHEST)
    public void onAreaEffectCloud(AreaEffectCloudApplyEvent event) {
        List<LivingEntity> affectedEntities = new ArrayList<>(event.getAffectedEntities());

        for (int j = 0;j < affectedEntities.size();j++) {
            LivingEntity entity = affectedEntities.get(j);
            if (entity instanceof Player player) {
                boolean instantDetected = false;
                PotionType basePotionType = event.getEntity().getBasePotionType();
                Collection<PotionEffect> potionEffects = new ArrayList<>(List.of());
                if(basePotionType!=null){
                    potionEffects.addAll(basePotionType.getPotionEffects());
                }
                if(event.getEntity().hasCustomEffects()){
                    potionEffects.addAll(event.getEntity().getCustomEffects());
                }
                if(potionEffects.isEmpty()) return;
                int instantAmplifier = 0;
                int instantDuration = 1;

                for (PotionEffect potionEffect : potionEffects) {
                    if (potionEffect.getType() == PotionEffectType.INSTANT_HEALTH || potionEffect.getType() == PotionEffectType.INSTANT_DAMAGE) {
                        instantDetected = true;
                        instantAmplifier = potionEffect.getAmplifier();
                        break;
                    }
                }

                if (instantDetected) {
                    PlayerInventory inventory = player.getInventory();
                    int finalInstantAmplifier = instantAmplifier;
                    ItemManager.toRPGItemByMeta(inventory.getItemInOffHand()).ifPresent(rpgItem -> {
                        for (Power power : rpgItem.getPowers()) {
                            if (power.getName().equals("undead")&&((Undead)power).getAllowOffHand()) {
                                affectedEntities.remove(player);
                                event.getEntity().setDuration(event.getEntity().getDuration()-event.getEntity().getDurationOnUse());
                                event.getEntity().setReapplicationDelay(event.getEntity().getReapplicationDelay());
                                event.getEntity().setRadius(event.getEntity().getRadius()-event.getEntity().getRadiusOnUse());
                                event.getEntity().setDuration(event.getEntity().getDuration()-event.getEntity().getDurationOnUse());
                                for (PotionEffect potionEffect : potionEffects) {
                                    if (potionEffect.getType() == PotionEffectType.INSTANT_HEALTH) {
                                        player.addPotionEffect(new PotionEffect(PotionEffectType.INSTANT_DAMAGE, instantDuration, finalInstantAmplifier));
                                    } else if (potionEffect.getType() == PotionEffectType.INSTANT_DAMAGE) {
                                        player.addPotionEffect(new PotionEffect(PotionEffectType.INSTANT_HEALTH, instantDuration, finalInstantAmplifier));
                                    } else {
                                        player.addPotionEffect(potionEffect);
                                    }
                                }
                            }
                        }
                    });
                    ItemManager.toRPGItemByMeta(inventory.getItemInMainHand()).ifPresent(rpgItem -> {
                        for (Power power : rpgItem.getPowers()) {
                            if (power.getName().equals("undead")) {
                                affectedEntities.remove(player);
                                event.getEntity().setDuration(event.getEntity().getDuration()-event.getEntity().getDurationOnUse());
                                event.getEntity().setReapplicationDelay(event.getEntity().getReapplicationDelay());
                                event.getEntity().setRadius(event.getEntity().getRadius()-event.getEntity().getRadiusOnUse());
                                event.getEntity().setDuration(event.getEntity().getDuration()-event.getEntity().getDurationOnUse());
                                for (PotionEffect potionEffect : potionEffects) {
                                    if (potionEffect.getType() == PotionEffectType.INSTANT_HEALTH) {
                                        player.addPotionEffect(new PotionEffect(PotionEffectType.INSTANT_DAMAGE, instantDuration, finalInstantAmplifier));
                                    } else if (potionEffect.getType() == PotionEffectType.INSTANT_DAMAGE) {
                                        player.addPotionEffect(new PotionEffect(PotionEffectType.INSTANT_HEALTH, instantDuration, finalInstantAmplifier));
                                    } else {
                                        player.addPotionEffect(potionEffect); // 保留其他效果
                                    }
                                }
                            }
                        }
                    });

                    for (ItemStack i : inventory.getArmorContents()) {
                        ItemManager.toRPGItemByMeta(i).ifPresent(rpgItem -> {
                            for (Power power : rpgItem.getPowers()) {
                                if (power.getName().equals("undead")) {
                                    affectedEntities.remove(player);
                                    event.getEntity().setDuration(event.getEntity().getDuration()-event.getEntity().getDurationOnUse());
                                    event.getEntity().setReapplicationDelay(event.getEntity().getReapplicationDelay());
                                    event.getEntity().setRadius(event.getEntity().getRadius()-event.getEntity().getRadiusOnUse());
                                    event.getEntity().setDuration(event.getEntity().getDuration()-event.getEntity().getDurationOnUse());
                                    for (PotionEffect potionEffect : potionEffects) {
                                        if (potionEffect.getType() == PotionEffectType.INSTANT_HEALTH) {
                                            player.addPotionEffect(new PotionEffect(PotionEffectType.INSTANT_DAMAGE, instantDuration, finalInstantAmplifier));
                                        } else if (potionEffect.getType() == PotionEffectType.INSTANT_DAMAGE) {
                                            player.addPotionEffect(new PotionEffect(PotionEffectType.INSTANT_HEALTH, instantDuration, finalInstantAmplifier));
                                        } else {
                                            player.addPotionEffect(potionEffect); // 保留其他效果
                                        }
                                    }
                                }
                            }
                        });
                    }
                }
            }
        }

        event.getAffectedEntities().clear();
        event.getAffectedEntities().addAll(affectedEntities);
    }

    //For power Undead only
    @EventHandler(ignoreCancelled = true, priority = EventPriority.HIGHEST)
    public void onInstantConsume(PlayerItemConsumeEvent event) {
        ItemStack consumedItem = event.getItem();
        Player player = event.getPlayer();
        PlayerInventory inventory = player.getInventory();
        final boolean[] undead = {false};
        for (ItemStack i : inventory.getArmorContents()) {
            ItemManager.toRPGItemByMeta(i).ifPresent(rpgItem -> {
                for (Power power : rpgItem.getPowers()) {
                    if (power.getName().equals("undead")) {
                        undead[0] = true;
                    }
                }
            });
        }
        ItemManager.toRPGItemByMeta(inventory.getItemInOffHand()).ifPresent(rpgItem -> {
            for (Power power : rpgItem.getPowers()) {
                if (power.getName().equals("undead")&&((Undead)power).getAllowOffHand()) {
                    undead[0] = true;
                }
            }
        });
        if(!undead[0]) return;
        if (consumedItem.getType() == Material.POTION) {
            PotionMeta potionMeta = (PotionMeta) consumedItem.getItemMeta();
            if (potionMeta != null) {
                Collection<PotionEffect> potionEffects = new ArrayList<>(potionMeta.getCustomEffects());
                boolean instantDetected = false;
                int instantAmplifier = 0;
                int instantDuration = 1;
                if(potionMeta.hasBasePotionType()){
                    if(potionMeta.getBasePotionType()==PotionType.HARMING||potionMeta.getBasePotionType()==PotionType.STRONG_HARMING||potionMeta.getBasePotionType()==PotionType.HEALING||potionMeta.getBasePotionType()==PotionType.STRONG_HEALING){
                        instantDetected = true;
                    }
                }
                for (PotionEffect potionEffect : potionEffects) {
                    if (potionEffect.getType() == PotionEffectType.INSTANT_HEALTH || potionEffect.getType() == PotionEffectType.INSTANT_DAMAGE) {
                        instantDetected = true;
                        instantAmplifier = potionEffect.getAmplifier();
                        break;
                    }
                }

                if (instantDetected) {
                    if(potionMeta.hasBasePotionType()){
                        if(potionMeta.getBasePotionType()==PotionType.HARMING){
                            potionMeta.setBasePotionType(PotionType.HEALING);
                        }
                        else if(potionMeta.getBasePotionType()==PotionType.STRONG_HARMING){
                            potionMeta.setBasePotionType(PotionType.STRONG_HEALING);
                        }
                        else if(potionMeta.getBasePotionType()==PotionType.HEALING){
                            potionMeta.setBasePotionType(PotionType.HARMING);
                        }
                        else if(potionMeta.getBasePotionType()==PotionType.STRONG_HEALING){
                            potionMeta.setBasePotionType(PotionType.STRONG_HARMING);
                        }
                    }
                    potionMeta.clearCustomEffects();
                    for (PotionEffect potionEffect : potionEffects) {
                        if (potionEffect.getType() == PotionEffectType.INSTANT_HEALTH) {
                            potionMeta.addCustomEffect(new PotionEffect(PotionEffectType.INSTANT_DAMAGE, instantDuration, instantAmplifier), true);
                        } else if (potionEffect.getType() == PotionEffectType.INSTANT_DAMAGE) {
                            potionMeta.addCustomEffect(new PotionEffect(PotionEffectType.INSTANT_HEALTH, instantDuration, instantAmplifier), true);
                        } else {
                            potionMeta.addCustomEffect(potionEffect, true);
                        }
                    }
                    consumedItem.setItemMeta(potionMeta);
                    event.setItem(consumedItem);
                }
            }
        }
    }


    //For power Undead only
    @EventHandler(ignoreCancelled = true,priority = EventPriority.HIGHEST)
    public void onRegenAndPoison(EntityPotionEffectEvent event){
        if(event.getEntity() instanceof Player p && (event.getAction() == EntityPotionEffectEvent.Action.ADDED || event.getAction() == EntityPotionEffectEvent.Action.CHANGED) && (event.getModifiedType()==PotionEffectType.POISON || event.getModifiedType()==PotionEffectType.REGENERATION)){
            PlayerInventory inventory = p.getInventory();
            ItemManager.toRPGItemByMeta(inventory.getItemInOffHand()).ifPresent(rpgItem -> {
                for (Power power : rpgItem.getPowers()){
                    if(power.getName().equals("undead")){
                        Undead undead = (Undead) power;
                        if(undead.getAllowOffHand()){
                            event.setCancelled(true);
                        }
                    }
                }
            });
            ItemManager.toRPGItemByMeta(inventory.getItemInMainHand()).ifPresent(rpgItem -> {
                for (Power power : rpgItem.getPowers()){
                    if(power.getName().equals("undead")){
                        event.setCancelled(true);
                    }
                }
            });
            for(ItemStack i : inventory.getArmorContents()){
                ItemManager.toRPGItemByMeta(i).ifPresent(rpgItem -> {
                    for (Power power : rpgItem.getPowers()){
                        if(power.getName().equals("undead")){
                            event.setCancelled(true);
                        }
                    }
                });
            }
        }
    }
    //For power Undead only now
    @EventHandler(ignoreCancelled = true,priority = EventPriority.LOWEST)
    public void onUndeadSmite(EntityDamageByEntityEvent event){
        if(event.getEntity() instanceof Player p && event.getDamager() instanceof LivingEntity e){
            PlayerInventory inventory = p.getInventory();
            ItemStack item = e.getEquipment()!=null ? e.getEquipment().getItemInMainHand() : null ;
            if(item==null) return;
            int level = item.getEnchantmentLevel(Enchantment.SMITE);
            if(level==0) return;
            ItemManager.toRPGItemByMeta(inventory.getItemInOffHand()).ifPresent(rpgItem -> {
                for (Power power : rpgItem.getPowers()){
                    if(power.getName().equals("undead")){
                        Undead undead = (Undead) power;
                        if(undead.getAllowOffHand()){
                            event.setDamage(event.getFinalDamage()+2.5*level);
                        }
                    }
                }
            });
            ItemManager.toRPGItemByMeta(inventory.getItemInMainHand()).ifPresent(rpgItem -> {
                for (Power power : rpgItem.getPowers()){
                    if(power.getName().equals("undead")){
                        event.setDamage(event.getFinalDamage()+2.5*level);
                    }
                }
            });
            for(ItemStack i : inventory.getArmorContents()){
                ItemManager.toRPGItemByMeta(i).ifPresent(rpgItem -> {
                    for (Power power : rpgItem.getPowers()){
                        if(power.getName().equals("undead")) {
                            event.setDamage(event.getFinalDamage()+2.5*level);
                        }
                    }
                });
            }
        }
    }
}