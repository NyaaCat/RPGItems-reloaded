package think.rpgitems;

import cat.nyaa.nyaacore.utils.RayTraceUtils;
import cat.nyaa.nyaacore.utils.TridentUtils;
import org.bukkit.Bukkit;
import org.bukkit.Material;
import org.bukkit.block.Block;
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
import org.bukkit.metadata.FixedMetadataValue;
import org.bukkit.projectiles.ProjectileSource;
import org.bukkit.scheduler.BukkitRunnable;
import think.rpgitems.data.Context;
import think.rpgitems.event.BeamEndEvent;
import think.rpgitems.event.BeamHitBlockEvent;
import think.rpgitems.event.BeamHitEntityEvent;
import think.rpgitems.item.ItemManager;
import think.rpgitems.item.RPGItem;
import think.rpgitems.power.Pimpl;
import think.rpgitems.power.PowerSneak;
import think.rpgitems.power.PowerSprint;
import think.rpgitems.power.Utils;
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

    private static HashSet<Integer> removeProjectiles = new HashSet<>();
    private static HashMap<Integer, Integer> rpgProjectiles = new HashMap<>();
    private static Map<UUID, ItemStack> localItemStacks = new HashMap<>();

    private static RPGItem projectileRpgItem;
    private static ItemStack projectileItemStack;
    private static Player projectilePlayer;

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

    private static Map<UUID, UUID> projectileRegisterMap = new WeakHashMap<>();

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
                if (e.getHitEntity() != null && e.getEntity() instanceof AbstractArrow && ((AbstractArrow) e.getEntity()).getPierceLevel() > 0 ) {
                    return;
                }
                removeProjectiles.remove(entity.getEntityId());
                entity.remove();
            });
        }
        if (rpgProjectiles.containsKey(entity.getEntityId())) {
            try {
                if (entity instanceof Trident && entity.getScoreboardTags().contains("rgi_projectile")){
                    ((Trident) entity).setPickupStatus(AbstractArrow.PickupStatus.DISALLOWED);
                }
                RPGItem rItem = ItemManager.getItem(rpgProjectiles.get(entity.getEntityId())).orElse(null);

                if (rItem == null || !(entity.getShooter() instanceof Player))
                    return;
                Player player = (Player) entity.getShooter();
                if (player.isOnline() && !player.isDead()) {
                    ItemStack item = player.getInventory().getItemInMainHand();
                    RPGItem hItem = ItemManager.toRPGItem(item).orElse(null);

                    final UUID projectileUuid = entity.getUniqueId();
                    if (hasLocalItemStack(projectileUuid)) {
                        item = getLocalItemStack(projectileUuid);
                        new BukkitRunnable(){
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
                        double distance = player.getLocation().distance(e.getEntity().getLocation());
                        if (ranged.get(0).rm > distance || distance > ranged.get(0).r) {
                            return;
                        }
                    }
                    rItem.power(player, item, e, BaseTriggers.PROJECTILE_HIT);
                }
            } finally {
                Bukkit.getScheduler().runTask(plugin, () -> {
                    if (e.getHitEntity() != null && e.getEntity() instanceof AbstractArrow && ((AbstractArrow) e.getEntity()).getPierceLevel() > 0 ) {
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
        Player player = (shooter instanceof Player) ? (Player) shooter : Bukkit.getPlayer(projectileRegisterMap.get(((LivingEntity) shooter).getUniqueId()));
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
            item = TridentUtils.getTridentItemStack((Trident) entity);
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
            TridentUtils.setTridentItemStack((Trident) entity, fakeItem);
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
        if (isChargeable(item) || (isOffhand && isThrowable(itemInMainHand))){
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
        if (!(playerTarget instanceof ItemFrame) && (im.isEdible() || im.isRecord() || isPlacable(im) || isItemConsumer(e.getClickedBlock()))){
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
        switch(im){
            case ARMOR_STAND:
            case MINECART:
            case CHEST_MINECART:
            case COMMAND_BLOCK_MINECART:
            case FURNACE_MINECART:
            case TNT_MINECART:
            case HOPPER_MINECART:
            case END_CRYSTAL:
            case PRISMARINE_CRYSTALS:
            case BUCKET:
            case LAVA_BUCKET:
            case WATER_BUCKET:
            case COD_BUCKET:
            case PUFFERFISH_BUCKET:
            case SALMON_BUCKET:
            case TROPICAL_FISH_BUCKET:
            case SADDLE:
            case LEAD:
            case BOWL:

            return true;
        }
        return false;
    }

    private boolean isTools(Material im) {
        switch(im){
            //<editor-fold>
            case BOW:
            case CROSSBOW:
            case DIAMOND_AXE:
            case GOLDEN_AXE:
            case IRON_AXE:
            case STONE_AXE:
            case WOODEN_AXE:
            case DIAMOND_PICKAXE:
            case GOLDEN_PICKAXE:
            case IRON_PICKAXE:
            case STONE_PICKAXE:
            case WOODEN_PICKAXE:
            case DIAMOND_HOE:
            case GOLDEN_HOE:
            case IRON_HOE:
            case STONE_HOE:
            case WOODEN_HOE:
            case DIAMOND_SHOVEL:
            case GOLDEN_SHOVEL:
            case IRON_SHOVEL:
            case STONE_SHOVEL:
            case WOODEN_SHOVEL:
            case FISHING_ROD:
            case SHEARS:
            case LEAD:
            case FLINT_AND_STEEL:
                //</editor-fold>
                return true;
        }
        return false;
    }

    public boolean isItemConsumer(Block im) {
        if (im == null)return false;
        switch (im.getType()) {
            // <editor-fold defaultstate="collapsed" desc="isInteractable">
            case COMPOSTER:
            case JUKEBOX:
            case FLOWER_POT:

                // </editor-fold>
                return true;
            default:
                return false;
        }
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
        if (!(e.getWhoClicked() instanceof Player)) return;
        Player player = (Player) e.getWhoClicked();
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
            ItemManager.toRPGItemByMeta(item).ifPresent(rpgItem -> rpgItem.updateItem(item));
        }
        for (ItemStack item : player.getInventory().getArmorContents()) {
            ItemManager.toRPGItemByMeta(item).ifPresent(rpgItem -> rpgItem.updateItem(item));
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
            UUID uuid = UUID.fromString(itemMeta.getLore().get(0));
            ItemStack realItem = removeLocalItemStack(uuid);
            if (realItem != null) {
                if (realItem.getType() == Material.AIR) {
                    e.getArrow().setPickupStatus(Arrow.PickupStatus.DISALLOWED);
                    e.getArrow().setPersistent(false);
                    e.setCancelled(true);
                    Bukkit.getScheduler().runTaskLater(RPGItems.plugin, () -> e.getArrow().remove(), 100L);
                } else {
                    RPGItem.updateItemStack(realItem);
                    e.getItem().setItemStack(realItem);
                }
            }
        } catch (IllegalArgumentException ex) {
            logger.log(Level.WARNING, "Exception when PlayerPickupArrowEvent. May be harmless.", ex);
        }
    }

    @EventHandler(priority = EventPriority.MONITOR, ignoreCancelled = true)
    public void onPlayerPickup(EntityPickupItemEvent e) {
        if (!(e.getEntity() instanceof Player)) {
            return;
        }
        final Player p = (Player) e.getEntity();
        new BukkitRunnable() {
            @Override
            public void run() {
                if (!p.isOnline()) return;
                PlayerInventory in = p.getInventory();
                for (int i = 0; i < in.getSize(); i++) {
                    ItemStack item = in.getItem(i);
                    ItemManager.toRPGItem(item).ifPresent(rpgItem -> rpgItem.updateItem(item));
                }
                for (ItemStack item : in.getArmorContents()) {
                    ItemManager.toRPGItem(item).ifPresent(rpgItem -> rpgItem.updateItem(item));
                }
            }
        }.runTaskLater(plugin, 1L);
    }

    @EventHandler
    public void onInventoryClose(InventoryCloseEvent e) {
        updatePlayerInventory(e.getInventory(), (Player) e.getPlayer(), e);
    }

    List<UUID> switchCooldown = new ArrayList<>();

    @EventHandler
    public void onPlayerChangeItem(PlayerItemHeldEvent ev) {
        Player player = ev.getPlayer();
        ItemStack item = player.getInventory().getItem(ev.getNewSlot());
        ItemManager.toRPGItem(item).ifPresent(rpgItem -> rpgItem.updateItem(item));
        if (switchCooldown.contains(player.getUniqueId())) return;
        ItemStack[] armorContents = player.getInventory().getArmorContents();
        for (int i = 0; i < armorContents.length; i++) {
            ItemStack stack = armorContents[i];
            ItemManager.toRPGItem(stack).ifPresent(rpgItem -> rpgItem.updateItem(stack));
        }
        ItemStack offhandItem = player.getInventory().getItemInOffHand();
        ItemManager.toRPGItem(offhandItem).ifPresent(rpgItem -> rpgItem.updateItem(offhandItem));
        switchCooldown.add(player.getUniqueId());
        new BukkitRunnable() {
            @Override
            public void run() {
                switchCooldown.remove(player.getUniqueId());
            }
        }.runTaskLater(plugin, 20);
    }


    private void updatePlayerInventory(Inventory inventory, Player p, InventoryEvent e) {
        Inventory in = inventory;
        Iterator<ItemStack> it = in.iterator();
        try {
            while (it.hasNext()) {
                ItemStack item = it.next();
                ItemManager.toRPGItemByMeta(item).ifPresent(rpgItem -> rpgItem.updateItem(item));
            }
            PlayerInventory inventory1 = p.getInventory();
            it = inventory1.iterator();
            while (it.hasNext()) {
                ItemStack item = it.next();
                ItemManager.toRPGItemByMeta(item).ifPresent(rpgItem -> rpgItem.updateItem(item));
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

                if (hasRGI.get() && !plugin.cfg.allowAnvilEnchant){
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

    //this function cann't catch event when player open their backpack.
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

        if (!overridingDamage.isPresent()) {
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

        if (rItem != null && e.getCause().equals(EntityDamageEvent.DamageCause.BLOCK_EXPLOSION) || e.getCause().equals(EntityDamageEvent.DamageCause.ENTITY_EXPLOSION)){
            int damageMin = rItem.getDamageMin();
            int damageMax = rItem.getDamageMax();
            double dmg = damageMin < damageMax ? ThreadLocalRandom.current().nextDouble(damageMin, damageMax) : damageMax;
            overridingDamage = Optional.of(dmg);
        }

        double originDamage = e.getDamage();
        double damage = originDamage;
        if (rItem != null && !overridingDamage.isPresent()) {
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
                double damage = e.getDamage() * projectile.getMetadata("RPGItems.Force").get(0).asFloat() / projectile.getMetadata("RPGItems.OriginalForce").get(0).asFloat();
                e.setDamage(damage);
            }
            return;
        }
        RPGItem rItem = ItemManager.getItem(projectileID).orElse(null);
        if (rItem == null || !(projectile.getShooter() instanceof Player))
            return;
        if (!((Player) projectile.getShooter()).isOnline()) {
            return;
        }
        Player player = (Player) projectile.getShooter();
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
            if (itemStack.equals(itemInMainHand))continue;
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
        if (e.getEntity() instanceof Player) {
            Player player = (Player) e.getEntity();
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
                }catch (Exception ignored){
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
        if (ev.getEntity() instanceof Player) {
            ev.setDamage(playerHitTaken((Player) ev.getEntity(), ev));
            double finalDamage = ev.getFinalDamage();
            Player entity = (Player)ev.getEntity();
            if (finalDamage >= entity.getHealth()){
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
        if (ev.getEntity() instanceof Player) {
            Player e = (Player) ev.getEntity();
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
    public void onBeamHitBlock(BeamHitBlockEvent event){
        Player player = event.getPlayer();
        if (player == null)return;

        RPGItem rItem = ItemManager.toRPGItem(event.getItemStack()).orElse(null);
        if (rItem == null)return;

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
    public void onBeamHitEntity(BeamHitEntityEvent event){
        Player player = event.getPlayer();
        if (player == null)return;

        RPGItem rItem = ItemManager.toRPGItem(event.getItemStack()).orElse(null);
        if (rItem == null)return;

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
        if (player == null)return;

        RPGItem rItem = ItemManager.toRPGItem(event.getItemStack()).orElse(null);
        if (rItem == null) return;

        rItem.power(player, event.getItemStack(), event, BaseTriggers.BEAM_END, null);
    }

}