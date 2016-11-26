/*
 *  This file is part of RPG Items.
 *
 *  RPG Items is free software: you can redistribute it and/or modify
 *  it under the terms of the GNU General Public License as published by
 *  the Free Software Foundation, either version 3 of the License, or
 *  (at your option) any later version.
 *
 *  RPG Items is distributed in the hope that it will be useful,
 *  but WITHOUT ANY WARRANTY; without even the implied warranty of
 *  MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
 *  GNU General Public License for more details.
 *
 *  You should have received a copy of the GNU General Public License
 *  along with RPG Items.  If not, see <http://www.gnu.org/licenses/>.
 */
package think.rpgitems;

import gnu.trove.map.hash.TIntByteHashMap;
import gnu.trove.map.hash.TIntIntHashMap;
import gnu.trove.map.hash.TObjectIntHashMap;
import org.bukkit.Bukkit;
import org.bukkit.ChatColor;
import org.bukkit.Material;
import org.bukkit.entity.*;
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
import org.bukkit.event.player.PlayerInteractEvent;
import org.bukkit.event.player.PlayerJoinEvent;
import org.bukkit.event.player.PlayerPickupItemEvent;
import org.bukkit.inventory.*;
import org.bukkit.metadata.FixedMetadataValue;
import org.bukkit.potion.PotionEffect;
import org.bukkit.potion.PotionEffectType;
import org.bukkit.projectiles.ProjectileSource;
import org.bukkit.scheduler.BukkitRunnable;
import think.rpgitems.commands.RPGItemUpdateCommandHandler;
import think.rpgitems.data.Locale;
import think.rpgitems.data.RPGMetadata;
import think.rpgitems.item.ItemManager;
import think.rpgitems.item.LocaleInventory;
import think.rpgitems.item.RPGItem;
import think.rpgitems.support.WorldGuard;

import java.util.*;

public class Events implements Listener {

    public static TIntByteHashMap removeArrows = new TIntByteHashMap();
    public static TIntIntHashMap rpgProjectiles = new TIntIntHashMap();
    public static TObjectIntHashMap<String> recipeWindows = new TObjectIntHashMap<String>();
    public static HashMap<String, Set<Integer>> drops = new HashMap<String, Set<Integer>>();
    public static boolean useLocaleInv = false;

    @EventHandler
    public void onItemEnchant(EnchantItemEvent e) {
        if (ItemManager.toRPGItem(e.getItem()) != null) {
            if (!e.getEnchanter().hasPermission("rpgitem.allowenchant.new"))
                e.setCancelled(true);
        } else if (RPGItemUpdateCommandHandler.isOldRPGItem(e.getItem())) {
            if (!e.getEnchanter().hasPermission("rpgitem.allowenchant.old"))
                e.setCancelled(true);
        }
    }

    @EventHandler
    public void onHangingBreak(HangingBreakEvent e) {
        if (e.getCause().equals(RemoveCause.EXPLOSION))
            if (e.getEntity().hasMetadata("RPGItems.Rumble")) {
                e.getEntity().removeMetadata("RPGItems.Rumble", Plugin.plugin); // Allow the entity to be broken again
                e.setCancelled(true);
            }
    }

    @EventHandler
    public void onBreak(BlockPhysicsEvent e) { // Is not triggered when the block a torch is attached to is removed
        if (e.getChangedType().equals(Material.TORCH))
            if (e.getBlock().hasMetadata("RPGItems.Torch")) {
                e.setCancelled(true); // Cancelling this does not work
                e.getBlock().removeMetadata("RPGItems.Torch", Plugin.plugin);
                e.getBlock().setType(Material.AIR);
            }
    }

    @EventHandler(ignoreCancelled = true)
    public void onBlockBreak(BlockBreakEvent e) {
        if (e.getBlock().getType().equals(Material.TORCH))
            if (e.getBlock().hasMetadata("RPGItems.Torch"))
                e.setCancelled(true);

        Player player = e.getPlayer();
        ItemStack item = player.getInventory().getItemInMainHand();
        RPGItem rItem;
        if ((rItem = ItemManager.toRPGItem(item)) != null) {
            RPGMetadata meta = RPGItem.getMetadata(item);
            int durability = 1;
            if (rItem.getMaxDurability() != -1) {
                durability = meta.containsKey(RPGMetadata.DURABILITY) ? ((Number) meta.get(RPGMetadata.DURABILITY)).intValue() : rItem.getMaxDurability();
                durability--;
                meta.put(RPGMetadata.DURABILITY, Integer.valueOf(durability));
            }
            RPGItem.updateItem(item, meta);
            if (durability <= 0) {
                player.getInventory().setItemInMainHand(null);
            } else {
                player.getInventory().setItemInMainHand(item);
            }
        }

    }

    @EventHandler
    public void onEntityDeath(EntityDeathEvent e) {
        String type = e.getEntity().getType().toString();
        Random random = new Random();
        if (drops.containsKey(type)) {
            Set<Integer> items = drops.get(type);
            Iterator<Integer> it = items.iterator();
            while (it.hasNext()) {
                int id = it.next();
                RPGItem item = ItemManager.getItemById(id);
                if (item == null) {
                    it.remove();
                    continue;
                }
                double chance = item.dropChances.get(type);
                if (random.nextDouble() < chance / 100d) {
                    e.getDrops().add(item.toItemStack());
                }
            }
        }
    }

    @EventHandler
    public void onProjectileHit(ProjectileHitEvent e) {
        final Entity entity = e.getEntity();
        if (removeArrows.contains(entity.getEntityId())) {
            entity.remove();
            removeArrows.remove(entity.getEntityId());
        } else if (rpgProjectiles.contains(entity.getEntityId())) {
            RPGItem item = ItemManager.getItemById(rpgProjectiles.get(entity.getEntityId()));
            new BukkitRunnable() {
                @Override
                public void run() {
                    rpgProjectiles.remove(entity.getEntityId());
                }
            }.runTask(Plugin.plugin);
            if (item == null)
                return;
            item.projectileHit((Player) ((Projectile) entity).getShooter(), (Projectile) entity);
        }
    }
    
    @EventHandler
    public void onBowShoot(EntityShootBowEvent e) {
        e.getProjectile().setMetadata("rpgitems.force", new FixedMetadataValue(Plugin.plugin, e.getForce()));
    }

    @EventHandler
    public void onProjectileFire(ProjectileLaunchEvent e) {
        ProjectileSource shooter = e.getEntity().getShooter();
        if (shooter instanceof Player) {
            Player player = (Player) shooter;
            ItemStack item = player.getInventory().getItemInMainHand();
            RPGItem rItem = ItemManager.toRPGItem(item);
            if (rItem == null)
                return;
            if (!WorldGuard.canPvP(player) && !rItem.ignoreWorldGuard)
                return;
            if (rItem.getHasPermission() == true && player.hasPermission(rItem.getPermission()) == false) {
                e.setCancelled(true);
                player.sendMessage(ChatColor.RED + String.format(Locale.get("message.error.permission")));
            }
            RPGMetadata meta = RPGItem.getMetadata(item);
            int durability = 1;
            if (rItem.getMaxDurability() != -1) {
                durability = meta.containsKey(RPGMetadata.DURABILITY) ? ((Number) meta.get(RPGMetadata.DURABILITY)).intValue() : rItem.getMaxDurability();
                durability--;
                meta.put(RPGMetadata.DURABILITY, Integer.valueOf(durability));
            }
            RPGItem.updateItem(item, meta);
            if (durability <= 0) {
                player.getInventory().setItemInMainHand(null);
            } else {
                player.getInventory().setItemInMainHand(item);
            }
            rpgProjectiles.put(e.getEntity().getEntityId(), rItem.getID());
        }
    }

    private Set<Material> BYPASS_BLOCK = new HashSet<Material>(){{
        add(Material.ACACIA_DOOR);
        add(Material.BIRCH_DOOR);
        add(Material.DARK_OAK_DOOR);
        add(Material.IRON_DOOR);
        add(Material.JUNGLE_DOOR);
        add(Material.SPRUCE_DOOR);
        add(Material.TRAP_DOOR);
        add(Material.WOOD_DOOR);
        add(Material.WOODEN_DOOR);
        add(Material.CHEST);
        add(Material.TRAPPED_CHEST);
        add(Material.CHEST);
        add(Material.CHEST);
        add(Material.CHEST);
    }};
    @EventHandler
    public void onPlayerAction(PlayerInteractEvent e) {
        Player p = e.getPlayer();
        if (e.getAction() == Action.PHYSICAL) return;
        if (e.getHand() == EquipmentSlot.OFF_HAND) return;
        RPGItem rItem = ItemManager.toRPGItem(e.getItem());
        if (rItem == null) return;
        Material im = e.getMaterial();
        if (im == Material.BOW || im == Material.SNOW_BALL || im == Material.EGG || im == Material.POTION || im == Material.AIR)
            return;
        if (!WorldGuard.canPvP(p) && !rItem.ignoreWorldGuard)
            return;
        if (rItem.getHasPermission() && !p.hasPermission(rItem.getPermission())) {
            p.sendMessage(ChatColor.RED + Locale.get("message.error.permission"));
            return;
        }

        Action action = e.getAction();
        if (action == Action.RIGHT_CLICK_AIR) {
            rItem.rightClick(p, e.getClickedBlock());
        } else if (action == Action.RIGHT_CLICK_BLOCK && !BYPASS_BLOCK.contains(e.getMaterial())) {
            rItem.rightClick(p, e.getClickedBlock());
        } else if (action == Action.LEFT_CLICK_AIR || action == Action.LEFT_CLICK_BLOCK) {
            rItem.leftClick(p, e.getClickedBlock());
        }
        RPGItem.updateItem(e.getItem());
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

        RPGItem rItem = ItemManager.toRPGItem(item);
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
            if (ItemManager.toRPGItem(item) != null)
                RPGItem.updateItem(item);
        }
        for (ItemStack item : player.getInventory().getArmorContents()) {
            if (ItemManager.toRPGItem(item) != null)
                RPGItem.updateItem(item);
        }
    }

    @EventHandler(priority = EventPriority.MONITOR, ignoreCancelled = true)
    public void onPlayerPickup(PlayerPickupItemEvent e) {
        final Player p = e.getPlayer();
        new BukkitRunnable(){
            @Override
            public void run() {
                if (!p.isOnline()) return;
                PlayerInventory in = p.getInventory();
                for (int i = 0; i < in.getSize(); i++) {
                    ItemStack item = in.getItem(i);
                    if (ItemManager.toRPGItem(item) != null)
                        RPGItem.updateItem(item);
                }
                for (ItemStack item : p.getInventory().getArmorContents()) {
                    if (ItemManager.toRPGItem(item) != null)
                        RPGItem.updateItem(item);
                }
            }
        }.runTaskLater(Plugin.plugin, 1L);
    }

    private HashSet<LocaleInventory> localeInventories = new HashSet<LocaleInventory>();

    @EventHandler
    public void onInventoryClose(InventoryCloseEvent e) {
        if (recipeWindows.containsKey(e.getPlayer().getName())) {
            int id = recipeWindows.remove(e.getPlayer().getName());
            RPGItem item = ItemManager.getItemById(id);
            if (item.recipe == null) {
                item.recipe = new ArrayList<ItemStack>();
            }
            item.recipe.clear();
            for (int y = 0; y < 3; y++) {
                for (int x = 0; x < 3; x++) {
                    int i = x + y * 9;
                    ItemStack it = e.getInventory().getItem(i);
                    item.recipe.add(it);
                }
            }
            item.hasRecipe = true;
            item.resetRecipe(true);
            ItemManager.save(Plugin.plugin);
            ((Player) e.getPlayer()).sendMessage(ChatColor.AQUA + "Recipe set for " + item.getName());
        } else if (useLocaleInv && e.getView() instanceof LocaleInventory) {
            localeInventories.remove(e.getView());
            ((LocaleInventory) e.getView()).getView().close();
        }
    }

    @EventHandler(ignoreCancelled = true, priority = EventPriority.HIGHEST)
    public void onInventoryClick(InventoryClickEvent e) {
        if (useLocaleInv && e.getView() instanceof LocaleInventory) {
            LocaleInventory inv = (LocaleInventory) e.getView();
            InventoryClickEvent clickEvent = new InventoryClickEvent(inv.getView(), e.getSlotType(), e.getSlot(), e.getClick(), e.getAction());
            Bukkit.getServer().getPluginManager().callEvent(clickEvent);
            if (clickEvent.isCancelled()) {
                e.setCancelled(true);
            } else {
                switch (clickEvent.getResult()) {
                case DEFAULT: // Can't really do this with current events
                case ALLOW:
                    System.out.println("ok...");
                    System.out.println(inv.getView().getItem(e.getRawSlot()));
                    inv.getView().setItem(e.getRawSlot(), clickEvent.getCursor());
                    System.out.println(inv.getView().getItem(e.getRawSlot()));
                    break;
                case DENY:
                    break;
                }
            }
            for (LocaleInventory localeInv : localeInventories) {
                if (localeInv != inv)
                    localeInv.reload();
            }
        }
        if (!e.isCancelled() && e.getClickedInventory() instanceof AnvilInventory) {
            if (e.getRawSlot() == 2) {
                HumanEntity p = e.getWhoClicked();
                ItemStack ind1 = e.getView().getItem(0);
                ItemStack ind2 = e.getView().getItem(1);
                if (ItemManager.toRPGItem(ind1) != null || ItemManager.toRPGItem(ind2) != null) {
                    if (!p.hasPermission("rpgitem.allowenchant.new"))
                        e.setCancelled(true);
                } else if (RPGItemUpdateCommandHandler.isOldRPGItem(ind1) || RPGItemUpdateCommandHandler.isOldRPGItem(ind2)) {
                    if (!p.hasPermission("rpgitem.allowenchant.old"))
                        e.setCancelled(true);
                }
            }
        }
    }

    @EventHandler(ignoreCancelled = true, priority = EventPriority.HIGHEST)
    public void onInventoryOpen(final InventoryOpenEvent e) {
        if (e.getView() instanceof LocaleInventory)
            return;
        if (e.getInventory().getType() != InventoryType.CHEST || !useLocaleInv) {
            Inventory in = e.getInventory();
            Iterator<ItemStack> it = in.iterator();
            try {
                while (it.hasNext()) {
                    ItemStack item = it.next();
                    if (ItemManager.toRPGItem(item) != null)
                        RPGItem.updateItem(item);
                }
            } catch (ArrayIndexOutOfBoundsException ex) {
                // Fix for the bug with anvils in craftbukkit
            }
        } else if (useLocaleInv) {
            LocaleInventory localeInv = new LocaleInventory((Player) e.getPlayer(), e.getView());
            e.setCancelled(true);
            e.getPlayer().openInventory(localeInv);
            localeInventories.add(localeInv);
        }
    }

    private Random random = new Random();

    private double playerDamager(EntityDamageByEntityEvent e, double damage) {
        Player player = (Player) e.getDamager();
        ItemStack item = player.getInventory().getItemInMainHand();
        if (item.getType() == Material.BOW || item.getType() == Material.SNOW_BALL || item.getType() == Material.EGG || item.getType() == Material.POTION)
            return damage;

        RPGItem rItem = ItemManager.toRPGItem(item);
        if (rItem == null)
            return damage;
        if (!WorldGuard.canPvP(player) && !rItem.ignoreWorldGuard)
            return damage;
        if (rItem.getHasPermission() == true && player.hasPermission(rItem.getPermission()) == false) {
            damage = 0;
            e.setCancelled(true);
            player.sendMessage(ChatColor.RED + String.format(Locale.get("message.error.permission")));
        }
        damage = rItem.getDamageMin() != rItem.getDamageMax() ? (rItem.getDamageMin() + random.nextInt(rItem.getDamageMax() - rItem.getDamageMin())) : rItem.getDamageMin();
        Collection<PotionEffect> potionEffects = player.getActivePotionEffects();
        double strength = 0, weak = 0;
        for (PotionEffect pe : potionEffects) {
            if (pe.getType().equals(PotionEffectType.INCREASE_DAMAGE)) {
                strength = 3 * (pe.getAmplifier() + 1);//MC 1.9+
            }
            if (pe.getType().equals(PotionEffectType.WEAKNESS)) {
                weak = 4 * (pe.getAmplifier() + 1);//MC 1.9+
            }
        }
        damage = damage + strength - weak;
        if (e.getEntity() instanceof LivingEntity) {
            rItem.hit(player, (LivingEntity) e.getEntity(), e.getDamage());
        }
        RPGMetadata meta = RPGItem.getMetadata(item);
        int durability = 1;
        if (rItem.getMaxDurability() != -1) {
            durability = meta.containsKey(RPGMetadata.DURABILITY) ? ((Number) meta.get(RPGMetadata.DURABILITY)).intValue() : rItem.getMaxDurability();
            durability--;
            meta.put(RPGMetadata.DURABILITY, Integer.valueOf(durability));
        }
        RPGItem.updateItem(item, meta);
        if (durability <= 0) {
            player.getInventory().setItemInMainHand(null);
        } else {
            player.getInventory().setItemInMainHand(item);
        }
        return damage;
    }

    private double projectileDamager(EntityDamageByEntityEvent e, double damage) {
        Projectile entity = (Projectile) e.getDamager();
        if (rpgProjectiles.contains(entity.getEntityId())) {
            RPGItem rItem = ItemManager.getItemById(rpgProjectiles.get(entity.getEntityId()));
            if (rItem == null)
                return damage;
            damage = rItem.getDamageMin() != rItem.getDamageMax() ? (rItem.getDamageMin() + random.nextInt(rItem.getDamageMax() - rItem.getDamageMin())) : rItem.getDamageMin();
            
            //Apply force adjustments
            if(e.getDamager().hasMetadata("rpgitems.force")) {
                damage *= e.getDamager().getMetadata("rpgitems.force").get(0).asFloat();
            }
            
            if (e.getEntity() instanceof LivingEntity) {
                LivingEntity le = (LivingEntity) e.getEntity();
                rItem.hit((Player) entity.getShooter(), le, e.getDamage());
            }
        }
        return damage;
    }

    private double playerHit(EntityDamageByEntityEvent e, double damage) {
        Player p = (Player) e.getEntity();
        if (e.isCancelled() || !WorldGuard.canPvP(p))
            return damage;
        ItemStack[] armour = p.getInventory().getArmorContents();
        boolean hasRPGItem = false;
        for (int i = 0; i < armour.length; i++) {
            ItemStack pArmour = armour[i];
            RPGItem pRItem = ItemManager.toRPGItem(pArmour);
            if (pRItem == null) {
                continue;
            } else {
                hasRPGItem = true;
            }
            if (!WorldGuard.canPvP(p) && !pRItem.ignoreWorldGuard)
                return damage;
            if (pRItem.getHasPermission() == true && p.hasPermission(pRItem.getPermission()) == false) {
                damage = 0;
                e.setCancelled(true);
                p.sendMessage(ChatColor.RED + String.format(Locale.get("message.error.permission")));
            }
            if (pRItem.getArmour() > 0) {
                damage -= Math.round(((double) damage) * (((double) pRItem.getArmour()) / 100d));
            }
            RPGMetadata meta = RPGItem.getMetadata(pArmour);
            if (pRItem.getMaxDurability() != -1) {
                int durability = meta.containsKey(RPGMetadata.DURABILITY) ? ((Number) meta.get(RPGMetadata.DURABILITY)).intValue() : pRItem.getMaxDurability();
                durability--;
                if (durability <= 0) {
                    armour[i] = null;
                }
                meta.put(RPGMetadata.DURABILITY, Integer.valueOf(durability));
            }
            RPGItem.updateItem(pArmour, meta);
        }
        if(hasRPGItem) {
            p.getInventory().setArmorContents(armour);
        }
        return damage;
    }

    private double playerHurt(Player e, double damage) {
        double ret = Double.MAX_VALUE;
        for (ItemStack item : e.getInventory().getArmorContents()) {
            RPGItem ri = ItemManager.toRPGItem(item);
            if (ri == null) continue;
            double d = ri.takeHit(e, null, damage);
            if (d < 0) continue;
            if (d < ret) ret = d;
        }
        for (ItemStack item : e.getInventory().getContents()) {
            RPGItem ri = ItemManager.toRPGItem(item);
            if (ri == null) continue;
            double d = ri.takeHit(e, null, damage);
            if (d < 0) continue;
            if (d < ret) ret = d;
        }
        return ret == Double.MAX_VALUE? damage : ret;
    }

    @EventHandler(priority = EventPriority.LOWEST, ignoreCancelled = true)
    public void onDamage(EntityDamageEvent ev) {
        if (ev instanceof EntityDamageByEntityEvent) {
            EntityDamageByEntityEvent e = (EntityDamageByEntityEvent) ev;
            double damage = e.getDamage();
            if (e.getDamager() instanceof Player) {
                damage = playerDamager(e, damage);
            } else if (e.getDamager() instanceof Projectile) {
                damage = projectileDamager(e, damage);
            }
            if (e.getEntity() instanceof Player) {
                damage = playerHit(e, damage);
            }
            e.setDamage(damage);
        }
        if (ev.getEntity() instanceof Player) {
            ev.setDamage(playerHurt((Player)ev.getEntity(), ev.getDamage()));
        }
    }

    @EventHandler(ignoreCancelled = true, priority = EventPriority.HIGHEST)
    public void onItemCraft(PrepareItemCraftEvent e) {
        RPGItem rpg = ItemManager.toRPGItem(e.getInventory().getResult());
        if (rpg != null) {
            List<ItemStack> temp = rpg.recipe;
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
                    if (random.nextInt(ItemManager.toRPGItem(e.getInventory().getResult()).recipechance) != 0) {
                        ItemStack baseitem = new ItemStack(e.getInventory().getResult().getType());
                        e.getInventory().setResult(baseitem);
                    }
                }
            } else {
                e.getInventory().setResult(new ItemStack(Material.AIR));
            }
        }
    }

    static private boolean canStack(ItemStack a, ItemStack b) {
        if (a!=null && a.getType() == Material.AIR) a = null;
        if (b!=null && b.getType() == Material.AIR) b = null;
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
}