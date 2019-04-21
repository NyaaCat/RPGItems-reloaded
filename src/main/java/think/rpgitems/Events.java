package think.rpgitems;

import cat.nyaa.nyaacore.utils.TridentUtils;
import org.bukkit.Bukkit;
import org.bukkit.ChatColor;
import org.bukkit.Material;
import org.bukkit.block.Block;
import org.bukkit.entity.*;
import org.bukkit.event.Event;
import org.bukkit.event.EventHandler;
import org.bukkit.event.EventPriority;
import org.bukkit.event.Listener;
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
import org.bukkit.inventory.meta.Damageable;
import org.bukkit.inventory.meta.ItemMeta;
import org.bukkit.metadata.FixedMetadataValue;
import org.bukkit.projectiles.ProjectileSource;
import org.bukkit.scheduler.BukkitRunnable;
import org.bukkit.util.Vector;
import think.rpgitems.item.ItemManager;
import think.rpgitems.item.RPGItem;
import think.rpgitems.power.Power;
import think.rpgitems.power.PowerSneak;
import think.rpgitems.power.PowerSprint;
import think.rpgitems.power.Trigger;
import think.rpgitems.power.impl.PowerRanged;
import think.rpgitems.power.impl.PowerRangedOnly;
import think.rpgitems.power.impl.PowerTranslocator;
import think.rpgitems.support.WGHandler;
import think.rpgitems.support.WGSupport;

import java.util.*;
import java.util.concurrent.atomic.AtomicReference;
import java.util.logging.Level;
import java.util.stream.Collectors;
import java.util.stream.Stream;

import static think.rpgitems.RPGItems.logger;
import static think.rpgitems.RPGItems.plugin;

public class Events implements Listener {

    public static HashMap<String, Set<Integer>> drops = new HashMap<>();

    static HashMap<String, Integer> recipeWindows = new HashMap<>();

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

    public static ItemStack getLocalItemStack(UUID entityId) {
        return localItemStacks.get(entityId);
    }

    public static ItemStack removeLocalItemStack(UUID entityId) {
        return localItemStacks.remove(entityId);
    }

    public static void registerRPGProjectile(RPGItem rpgItem, ItemStack itemStack, Player player) {
        if (projectilePlayer != null) {
            throw new IllegalStateException();
        }
        Events.projectileRpgItem = rpgItem;
        Events.projectileItemStack = itemStack;
        Events.projectilePlayer = player;
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
            RPGItem.EnchantMode enchantMode = item.getEnchantMode();
            if (enchantMode == RPGItem.EnchantMode.DISALLOW) {
                e.setCancelled(true);
            } else if (enchantMode == RPGItem.EnchantMode.PERMISSION && !(p.hasPermission("rpgitem.enchant." + item.getName()) || p.hasPermission("rpgitem.enchant." + item.getUid()))) {
                e.setCancelled(true);
            }
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
    public void onEntityDeath(EntityDeathEvent e) {
        String type = e.getEntity().getType().toString();
        if (PowerTranslocator.translocatorPlayerMap.getIfPresent(e.getEntity().getUniqueId()) != null) {
            e.getDrops().clear();
        }
        Random random = new Random();
        if (drops.containsKey(type)) {
            Set<Integer> items = drops.get(type);
            Iterator<Integer> it = items.iterator();
            while (it.hasNext()) {
                int id = it.next();
                RPGItem item = ItemManager.getItem(id).orElse(null);
                if (item == null) {
                    it.remove();
                    continue;
                }
                double chance = item.getDropChances().get(type);
                if (random.nextDouble() < chance / 100d) {
                    e.getDrops().add(item.toItemStack());
                }
            }
        }
    }

    @EventHandler
    public void onProjectileHit(ProjectileHitEvent e) {
        final Projectile entity = e.getEntity();
        if (removeProjectiles.contains(entity.getEntityId())) {
            Bukkit.getScheduler().runTask(plugin, () -> {
                removeProjectiles.remove(entity.getEntityId());
                entity.remove();
            });
        }
        if (rpgProjectiles.containsKey(entity.getEntityId())) {
            try {
                RPGItem rItem = ItemManager.getItem(rpgProjectiles.get(entity.getEntityId())).orElse(null);

                if (rItem == null || !(entity.getShooter() instanceof Player))
                    return;
                Player player = (Player) entity.getShooter();
                if (player.isOnline() && !player.isDead()) {
                    ItemStack item = player.getInventory().getItemInMainHand();
                    RPGItem hItem = ItemManager.toRPGItem(item).orElse(null);

                    if (hasLocalItemStack(entity.getUniqueId())) {
                        item = getLocalItemStack(entity.getUniqueId());
                        rItem = ItemManager.toRPGItem(item).orElse(null);
                        if (rItem == null) throw new IllegalStateException();
                    } else {
                        if (rItem != hItem) {
                            item = player.getInventory().getItemInOffHand();
                            hItem = ItemManager.toRPGItem(item).orElse(null);
                            if (rItem != hItem) {
                                return;
                            }
                        }
                    }

                    List<PowerRanged> ranged = rItem.getPower(PowerRanged.class, true);
                    if (!ranged.isEmpty()) {
                        double distance = player.getLocation().distance(e.getEntity().getLocation());
                        if (ranged.get(0).rm > distance || distance > ranged.get(0).r) {
                            return;
                        }
                    }
                    rItem.power(player, item, e, Trigger.PROJECTILE_HIT);
                }
            } finally {
                Bukkit.getScheduler().runTask(plugin, () -> rpgProjectiles.remove(entity.getEntityId()));
            }
        }
    }

    @EventHandler
    public void onBowShoot(EntityShootBowEvent e) {
        e.getProjectile().setMetadata("rpgitems.force", new FixedMetadataValue(plugin, e.getForce()));
    }

    @EventHandler(priority = EventPriority.HIGH)
    public void onPlayerShootBow(EntityShootBowEvent e){
        LivingEntity entity = e.getEntity();
        Vector velocity = e.getProjectile().getVelocity();
        if (entity instanceof Player){
            ItemStack bow = e.getBow();
            ItemManager.toRPGItem(bow).ifPresent(rpgItem -> rpgItem.power(((Player) entity), bow, e, Trigger.BOW_SHOOT));
        }
    }

    @EventHandler
    public void onProjectileFire(ProjectileLaunchEvent e) {
        Projectile entity = e.getEntity();
        ProjectileSource shooter = entity.getShooter();
        if (!(shooter instanceof Player)) return;
        Player player = (Player) shooter;
        if (projectilePlayer != null) {
            if (projectilePlayer != player) {
                throw new IllegalStateException();
            }
            registerRPGProjectile(e.getEntity().getEntityId(), projectileRpgItem.getUid());
            projectileRpgItem.power(player, projectileItemStack, e, Trigger.LAUNCH_PROJECTILE);
            projectileRpgItem = null;
            projectilePlayer = null;
            projectileItemStack = null;
            return;
        }

        ItemStack item = player.getInventory().getItemInMainHand();
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
                item = player.getInventory().getItemInOffHand();
                rItem = ItemManager.toRPGItem(item).orElse(null);
                if (rItem == null) {
                    return;
                }
            }
        }
        if (!rItem.hasPower(PowerRanged.class) && !rItem.hasPower(PowerRangedOnly.class) && item.getType() != Material.BOW && item.getType() != Material.SNOWBALL && item.getType() != Material.EGG && item.getType() != Material.POTION && item.getType() != Material.TRIDENT) {
            return;
        }
        if (ItemManager.canUse(player, rItem) == Event.Result.DENY) {
            return;
        }
        registerRPGProjectile(e.getEntity().getEntityId(), rItem.getUid());
        rItem.power(player, item, e, Trigger.LAUNCH_PROJECTILE);
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
        if (e.getHand() == EquipmentSlot.OFF_HAND) {
            rItem.power(player, e.getItem(), e, Trigger.OFFHAND_CLICK);
        } else if (action == Action.RIGHT_CLICK_AIR) {
            rItem.power(player, e.getItem(), e, Trigger.RIGHT_CLICK);
        } else if (action == Action.RIGHT_CLICK_BLOCK &&
                !(e.getClickedBlock().getType().isInteractable() && !player.isSneaking())) {
            rItem.power(player, e.getItem(), e, Trigger.RIGHT_CLICK);
        } else if (action == Action.LEFT_CLICK_AIR || action == Action.LEFT_CLICK_BLOCK) {
            rItem.power(player, e.getItem(), e, Trigger.LEFT_CLICK);
        }
    }

    @EventHandler
    public void onPlayerSneak(PlayerToggleSneakEvent e) {
        if (!e.isSneaking()) {
            return;
        }
        Player p = e.getPlayer();
        Trigger<PlayerToggleSneakEvent, PowerSneak, Void, Void> trigger = Trigger.SNEAK;

        trigger(p, e, p.getInventory().getItemInMainHand(), trigger);
        //todo
//        trigger(p, e, p.getInventory().getItemInOffHand(), trigger);

        ItemStack[] armorContents = p.getInventory().getArmorContents();
        Stream.of(armorContents)
                .map(ItemManager::toRPGItem)
                .filter(Optional::isPresent)
                .forEach(rpgItem -> trigger(p, e, rpgItem.get().toItemStack(), trigger));
    }

    <TEvent extends Event, TPower extends Power, TResult, TReturn> TReturn trigger(Player player, TEvent event, ItemStack itemStack, Trigger<TEvent, TPower, TResult, TReturn> trigger) {
        Optional<RPGItem> rpgItem = ItemManager.toRPGItem(itemStack);
        return rpgItem.map(rpgItem1 -> rpgItem1.power(player, itemStack, event, trigger)).orElse(null);
    }

    @EventHandler
    public void onPlayerSprint(PlayerToggleSprintEvent e) {
        if (!e.isSprinting()) {
            return;
        }
        Player p = e.getPlayer();
        Trigger<PlayerToggleSprintEvent, PowerSprint, Void, Void> sprint = Trigger.SPRINT;

        trigger(p, e, p.getInventory().getItemInMainHand(), sprint);
        //todo
//        trigger(p, e, p.getInventory().getItemInOffHand(), sprint);
        ItemStack[] armorContents = p.getInventory().getArmorContents();
        Stream.of(armorContents)
                .map(ItemManager::toRPGItem)
                .filter(Optional::isPresent)
                .forEach(rpgItem -> trigger(p, e, rpgItem.get().toItemStack(), sprint));

    }

    @EventHandler
    public void onSwapHand(PlayerSwapHandItemsEvent e) {
        Player player = e.getPlayer();
        ItemStack ois = e.getOffHandItem();
        ItemStack mis = e.getMainHandItem();
        RPGItem mitem = ItemManager.toRPGItem(mis).orElse(null);
        RPGItem oitem = ItemManager.toRPGItem(ois).orElse(null);

        if (mitem != null) {
            Boolean cont = mitem.power(player, mis, e, Trigger.SWAP_TO_MAINHAND);
            if (!cont) e.setCancelled(true);
        }

        if (oitem != null) {
            Boolean cont = oitem.power(player, ois, e, Trigger.SWAP_TO_OFFHAND);
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
            Boolean cont = currentItem.power(player, currentIs, e, Trigger.PICKUP_OFF_HAND);
            if (!cont) e.setCancelled(true);
        }

        if (cursorItem != null && (e.getAction() == InventoryAction.PLACE_SOME || e.getAction() == InventoryAction.PLACE_ONE || e.getAction() == InventoryAction.PLACE_ALL)) {
            Boolean cont = cursorItem.power(player, cursorIs, e, Trigger.PLACE_OFF_HAND);
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
        if (item == null)
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
            ItemManager.toRPGItem(item).ifPresent(rpgItem -> rpgItem.updateItem(item));
        }
        for (ItemStack item : player.getInventory().getArmorContents()) {
            ItemManager.toRPGItem(item).ifPresent(rpgItem -> rpgItem.updateItem(item));
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
        if (recipeWindows.containsKey(e.getPlayer().getName())) {
            int id = recipeWindows.remove(e.getPlayer().getName());
            RPGItem item = ItemManager.getItem(id).orElse(null);
            if (item == null) return;
            if (item.getRecipe() == null) {
                item.setRecipe(new ArrayList<>());
            }
            item.getRecipe().clear();
            for (int y = 0; y < 3; y++) {
                for (int x = 0; x < 3; x++) {
                    int i = x + y * 9;
                    ItemStack it = e.getInventory().getItem(i);
                    item.getRecipe().add(it);
                }
            }
            item.setHasRecipe(true);
            item.resetRecipe(true);
            ItemManager.save();
            e.getPlayer().sendMessage(ChatColor.AQUA + "Recipe set for " + item.getName());
        }
    }

    @EventHandler(ignoreCancelled = true, priority = EventPriority.HIGHEST)
    public void onInventoryClick(InventoryClickEvent e) {
        if (e.getClickedInventory() instanceof AnvilInventory) {
            if (e.getRawSlot() == 2) {
                HumanEntity p = e.getWhoClicked();
                ItemStack ind1 = e.getView().getItem(0);
                ItemStack ind2 = e.getView().getItem(1);
                Optional<RPGItem> opt1 = ItemManager.toRPGItem(ind1);
                Optional<RPGItem> opt2 = ItemManager.toRPGItem(ind2);
                opt1.ifPresent(item -> checkEnchantPerm(e, p, item));
                opt2.ifPresent(item -> checkEnchantPerm(e, p, item));
            }
        }
    }

    public void checkEnchantPerm(InventoryClickEvent e, HumanEntity p, RPGItem item) {
        RPGItem.EnchantMode enchantMode = item.getEnchantMode();
        if (enchantMode == RPGItem.EnchantMode.DISALLOW) {
            e.setCancelled(true);
        } else if (enchantMode == RPGItem.EnchantMode.PERMISSION && !(p.hasPermission("rpgitem.enchant." + item.getName()) || p.hasPermission("rpgitem.enchant." + item.getUid()))) {
            e.setCancelled(true);
        }
    }

    @EventHandler(ignoreCancelled = true, priority = EventPriority.HIGHEST)
    public void onInventoryOpen(final InventoryOpenEvent e) {
        if (e.getInventory().getHolder() == null || e.getInventory().getLocation() == null)
            return;
        if (e.getInventory().getType() != InventoryType.CHEST) {
            Inventory in = e.getInventory();
            Iterator<ItemStack> it = in.iterator();
            try {
                while (it.hasNext()) {
                    ItemStack item = it.next();
                    ItemManager.toRPGItem(item).ifPresent(rpgItem -> rpgItem.updateItem(item));
                }
            } catch (ArrayIndexOutOfBoundsException ex) {
                logger.log(Level.WARNING, "Exception when InventoryOpenEvent. May be harmless.", ex);
                // Fix for the bug with anvils in craftbukkit
            }
        }
    }

    @EventHandler(priority = EventPriority.HIGH, ignoreCancelled = true)
    public void onDamage(EntityDamageByEntityEvent ev) {
        if (ev.getDamager() instanceof Player) {
            playerDamager(ev);
        } else if (ev.getDamager() instanceof Projectile) {
            projectileDamager(ev);
        }
        if (ev.getEntity() instanceof Player) {
            playerHit(ev);
        }
    }

    private void playerDamager(EntityDamageByEntityEvent e) {
        Player player = (Player) e.getDamager();
        Entity entity = e.getEntity();
        ItemStack item = player.getInventory().getItemInMainHand();

        if (item.getType() == Material.BOW || item.getType() == Material.SNOWBALL || item.getType() == Material.EGG || item.getType() == Material.POTION)
            return;

        if (e.getCause() == EntityDamageEvent.DamageCause.THORNS)
            return;

        RPGItem rItem = ItemManager.toRPGItem(item).orElse(null);
        ItemStack[] armorContents = player.getInventory().getArmorContents();
        double originDamage = e.getDamage();
        AtomicReference<Double> damage = new AtomicReference<>(originDamage);
        if (rItem != null) {
            damage.set(rItem.meleeDamage(player, originDamage, item, entity));
        }
        if (damage.get() == -1) {
            e.setCancelled(true);
            return;
        }
//        List<RPGItem> collect = Stream.concat(
//                Stream.of(rItem),
//                Stream.of(player.getInventory().getArmorContents())
//                        .map(ItemManager::toRPGItem)
//                        .filter(Optional::isPresent)
//                        .map(Optional::get)
//        ).filter(Objects::nonNull).collect(Collectors.toList());

        e.setDamage(damage.get());
//        if (!collect.isEmpty()) {
//            collect.forEach(rpgItem -> {
//                if (entity instanceof LivingEntity) {
//                    damage.set(rpgItem.power(player, rpgItem.toItemStack(), e, Trigger.HIT));
//                }
//                e.setDamage(damage.get());
//            });
//        }
        if (!(entity instanceof LivingEntity))return;
        if (rItem!=null){
            damage.set(rItem.power(player, item, e, Trigger.HIT));
        }
        for (ItemStack armorContent : armorContents) {
            if (armorContent == null) continue;
            RPGItem rpgItem = ItemManager.toRPGItem(armorContent).orElse(null);
            if (rpgItem == null)continue;
            damage.set(rpgItem.power(player, armorContent, e, Trigger.HIT));
        }
        e.setDamage(damage.get());
        ItemStack itemInOffHand = player.getInventory().getItemInOffHand();
        Optional<RPGItem> offhand = ItemManager.toRPGItem(itemInOffHand);
        //todo
//        offhand.ifPresent(rpgItem -> damage.set(rpgItem.power(player, itemInOffHand, e, Trigger.OFFHAND_HIT)));
        e.setDamage(damage.get());
    }

    private void projectileDamager(EntityDamageByEntityEvent e) {
        Projectile projectile = (Projectile) e.getDamager();
        if (PowerTranslocator.translocatorPlayerMap.getIfPresent(projectile.getUniqueId()) != null) {
            e.setCancelled(true);
            return;
        }
        Integer projectileID = rpgProjectiles.get(projectile.getEntityId());
        if (projectileID == null) return;
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
                item = player.getInventory().getItemInOffHand();
                hItem = ItemManager.toRPGItem(item).orElse(null);
                if (rItem != hItem) {
                    return;
                }
            }
        }

        double originDamage = e.getDamage();
        AtomicReference<Double> damage = new AtomicReference<>(rItem.projectileDamage(player, originDamage, item, projectile, e.getEntity()));
        ItemStack[] armorContents = player.getInventory().getArmorContents();
        e.setDamage(damage.get());
//        List<RPGItem> collect = Stream.concat(
//                Stream.of(rItem),
//                Stream.of(player.getInventory().getArmorContents())
//                        .map(ItemManager::toRPGItem)
//                        .filter(Optional::isPresent)
//                        .map(Optional::get)
//        ).filter(Objects::nonNull).collect(Collectors.toList());
//        if (!collect.isEmpty()) {
//            collect.forEach(rpgItem -> {
//                if (e.getEntity() instanceof LivingEntity) {
//                    damage.set(rpgItem.power((Player) projectile.getShooter(), rpgItem.toItemStack(), e, Trigger.HIT));
//                }
//                e.setDamage(damage.get());
//            });
//        }
        if (!(e.getEntity() instanceof LivingEntity))return;
        if (rItem!=null){
            damage.set(rItem.power(player, item, e, Trigger.HIT));
        }
        for (ItemStack armorContent : armorContents) {
            if (armorContent == null) continue;
            RPGItem rpgItem = ItemManager.toRPGItem(armorContent).orElse(null);
            if (rpgItem == null)continue;
            damage.set(rpgItem.power(player, armorContent, e, Trigger.HIT));
        }
        e.setDamage(damage.get());
        ItemStack itemInOffHand = player.getInventory().getItemInOffHand();
        Optional<RPGItem> offhand = ItemManager.toRPGItem(itemInOffHand);
        //todo
//        offhand.ifPresent(rpgItem -> rpgItem.power(player, itemInOffHand, e, Trigger.OFFHAND_HIT));
    }

    private void playerHit(EntityDamageByEntityEvent e) {
        Player player = (Player) e.getEntity();
        ItemStack[] armour = player.getInventory().getArmorContents();
        boolean hasRPGItem = false;
        double damage = e.getDamage();
        for (ItemStack pArmour : armour) {
            RPGItem pRItem = ItemManager.toRPGItem(pArmour).orElse(null);
            if (pRItem == null) {
                continue;
            }
            hasRPGItem = true;
            damage = pRItem.takeDamage(player, damage, pArmour, e.getDamager());
        }
        if (hasRPGItem) {
            player.getInventory().setArmorContents(armour);
        }
        e.setDamage(damage);
    }

    @EventHandler(priority = EventPriority.HIGHEST, ignoreCancelled = true)
    public void onPlayerHitTaken(EntityDamageEvent ev) {
        if (ev.getEntity() instanceof Player) {
            ev.setDamage(playerHitTaken((Player) ev.getEntity(), ev));
        }
    }

    private double playerHitTaken(Player e, EntityDamageEvent ev) {
        double ret = ev.getDamage();
        for (ItemStack item : e.getInventory().getContents()) {
            RPGItem ri = ItemManager.toRPGItem(item).orElse(null);
            if (ri == null) continue;
            double d = ri.power(e, item, ev, Trigger.HIT_TAKEN);
            ret = d < ret ? d : ret;
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
                ri.power(e, item, ev, Trigger.HURT);
            }
        }
    }

    @Deprecated
    @EventHandler(ignoreCancelled = true, priority = EventPriority.HIGHEST)
    public void onItemCraft(PrepareItemCraftEvent e) {
        RPGItem rpg = ItemManager.toRPGItem(e.getInventory().getResult()).orElse(null);
        if (rpg != null) {
            if (!rpg.isHasRecipe()) {
                e.getInventory().setResult(new ItemStack(Material.AIR));
                return;
            }
            List<ItemStack> temp = rpg.getRecipe();
            if (temp != null && temp.size() == 9) {
                int idx = 0;
                for (ItemStack s : temp) {
                    if (!canStack(s, e.getInventory().getMatrix()[idx])) {
                        idx = -1;
                        break;
                    }
                    idx++;
                }
                if (idx < 0) {
                    e.getInventory().setResult(new ItemStack(Material.AIR));
                } else {
                    Random random = new Random();
                    if (random.nextInt(rpg.getRecipeChance()) != 0) {
                        ItemStack baseitem = new ItemStack(e.getInventory().getResult().getType());
                        e.getInventory().setResult(baseitem);
                    }
                }
            } else {
                e.getInventory().setResult(new ItemStack(Material.AIR));
            }
        }
    }

    @EventHandler(ignoreCancelled = true, priority = EventPriority.NORMAL)
    public void onItemDamage(PlayerItemDamageEvent e) {
        ItemMeta itemMeta = e.getItem().getItemMeta();
        if (e.getItem().getType().getMaxDurability() - (((Damageable) itemMeta).getDamage() + e.getDamage()) <= 0) {
            if (ItemManager.toRPGItem(e.getItem()).isPresent()) {
                e.setCancelled(true);
            }
        }
    }
}