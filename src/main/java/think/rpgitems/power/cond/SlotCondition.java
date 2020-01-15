package think.rpgitems.power.cond;

import com.google.common.cache.Cache;
import com.google.common.cache.CacheBuilder;
import org.bukkit.entity.Player;
import org.bukkit.inventory.ItemStack;
import org.bukkit.inventory.PlayerInventory;
import org.bukkit.scheduler.BukkitRunnable;
import think.rpgitems.RPGItems;
import think.rpgitems.data.LightContext;
import think.rpgitems.item.ItemManager;
import think.rpgitems.item.RPGItem;
import think.rpgitems.power.*;

import java.util.*;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicBoolean;
import java.util.function.Supplier;
import java.util.stream.Stream;

@Meta(marker = true)
public class SlotCondition extends BaseCondition<Void> {
    @Property(required = true, order = 0)
    public String id = "defaultSlot";
    @Property
    public boolean critical = false;
    @Property
    @AcceptedValue({"ARMOR",
            //both hands
            "HAND",
            //Inventory is the combination of backpack and belt (main/off hand is not included)
            "BACKPACK", "BELT", "INVENTORY",
            "HELMET", "CHESTPLATE", "LEGGINGS", "BOOTS",
            "MAIN_HAND", "OFF_HAND"})
    Set<Slots> slots = new HashSet<>();

    @Override
    public String id() {
        return id;
    }

    @Override
    public boolean isStatic() {
        return false;
    }

    @Override
    public boolean isCritical() {
        return critical;
    }

    final static String slotConditionKey = "rpgitem:slotCondition.counter";
    final static UUID slotConditionExistence = UUID.randomUUID();

    @Override
    public PowerResult<Void> check(Player player, ItemStack stack, Map<PropertyHolder, PowerResult<?>> context) {
        boolean res = false;
        //make sure context is cleared in next tick.
        Optional<Object> existence = LightContext.getTemp(slotConditionExistence, slotConditionKey);
        if (!existence.isPresent()) {
            new BukkitRunnable() {
                @Override
                public void run() {
                    LightContext.clear();
                }
            }.runTaskLater(RPGItems.plugin, 1);
        }
        Optional<Object> temp = LightContext.getTemp(player.getUniqueId(), slotConditionKey);
        int triggered = 0;
        if (!temp.isPresent()) {
            LightContext.putTemp(player.getUniqueId(), slotConditionKey, triggered);
        } else {
            triggered = ((int) temp.get());
        }
        int count = triggered;
        for (Slots slots1 : slots) {
            res = (slots1.eval(player.getInventory(), stack));
            if (res && count-- <= 0) {
                LightContext.putTemp(player.getUniqueId(), slotConditionKey, triggered + 1);
                return PowerResult.ok();
            }
        }
        return PowerResult.fail();
    }

    @Override
    public String getName() {
        return "slotcondition";
    }

    enum Slots {
        //ARMOR slot
        ARMOR,
        //both hands
        HAND,
        //Inventory is the combination of backpack and belt (main/off hand is not included)
        BACKPACK, BELT, INVENTORY,
        HELMET, CHESTPLATE, LEGGINGS, BOOTS,
        MAIN_HAND, OFF_HAND;

        public boolean eval(PlayerInventory inventory, ItemStack stack) {
            switch (this) {
                case HELMET:
                    return checkHelmet(inventory, stack);
                case CHESTPLATE:
                    return checkChestPlate(inventory, stack);
                case LEGGINGS:
                    return checkLeggings(inventory, stack);
                case BOOTS:
                    return checkBoots(inventory, stack);
                case ARMOR:
                    return checkHelmet(inventory, stack) || checkChestPlate(inventory, stack) ||
                            checkLeggings(inventory, stack) || checkBoots(inventory, stack);
                case HAND:
                    return checkMainHand(inventory, stack) && checkOffHand(inventory, stack);
                case BACKPACK:
                    return checkBackpack(inventory, stack);
                case BELT:
                    return checkBelts(inventory, stack);
                case INVENTORY:
                    return checkBelts(inventory, stack) || checkBackpack(inventory, stack);
                case MAIN_HAND:
                    return checkMainHand(inventory, stack);
                case OFF_HAND:
                    return checkOffHand(inventory, stack);
            }
            return false;
        }

        private boolean checkBelts(PlayerInventory inventory, ItemStack stack) {
            UUID uniqueId = inventory.getHolder().getUniqueId();
            ItemStack[] contents = inventory.getContents();
            return cachedContainsOr(backpackCache, uniqueId, stack, () -> Arrays.copyOfRange(contents, 0, 9));
        }

        Cache<UUID, ItemStack[]> backpackCache = CacheBuilder.newBuilder()
                .expireAfterWrite(50, TimeUnit.MICROSECONDS)
                .build();
        Cache<UUID, ItemStack[]> beltCache = CacheBuilder.newBuilder()
                .expireAfterWrite(50, TimeUnit.MICROSECONDS)
                .build();

        private boolean checkBackpack(PlayerInventory inventory, ItemStack stack) {
            UUID uniqueId = inventory.getHolder().getUniqueId();
            ItemStack[] contents = inventory.getContents();
            return cachedContainsOr(backpackCache, uniqueId, stack, () -> Arrays.copyOfRange(contents, 10, 36));
         }

        private boolean cachedContainsOr(Cache<UUID, ItemStack[]> backpackCache, UUID uuid, ItemStack stack, Supplier<ItemStack[]> supplier) {
            ItemStack[] ifPresent = backpackCache.getIfPresent(uuid);
            if (ifPresent == null){
                ifPresent = supplier.get();
                backpackCache.put(uuid, ifPresent);
            }
            return Stream.of(ifPresent).anyMatch(itemStack -> match(itemStack, stack));
        }

        private boolean checkMainHand(PlayerInventory inventory, ItemStack stack) {
            ItemStack itemInMainHand = inventory.getItemInMainHand();
            return match(itemInMainHand, stack);
        }

        private boolean checkOffHand(PlayerInventory inventory, ItemStack stack) {
            ItemStack itemInOffHand = inventory.getItemInOffHand();
            return match(itemInOffHand, stack);
        }

        private boolean checkBoots(PlayerInventory inventory, ItemStack stack) {
            ItemStack boots = inventory.getBoots();
            return match(boots, stack);
        }

        private boolean checkLeggings(PlayerInventory inventory, ItemStack stack) {
            ItemStack leggings = inventory.getLeggings();
            return match(leggings, stack);
        }

        private boolean checkChestPlate(PlayerInventory inventory, ItemStack stack) {
            ItemStack chestPlate = inventory.getChestplate();
            return match(chestPlate, stack);
        }

        private boolean checkHelmet(PlayerInventory inventory, ItemStack stack) {
            ItemStack helmet = inventory.getHelmet();
            return match(helmet, stack);
        }

        public boolean match(ItemStack itemStack, ItemStack stack) {
            if (stack == null) return itemStack == null;
            if (itemStack != null && !stack.isSimilar(itemStack)) {
                return false;
            }
            Optional<RPGItem> itemUsed = ItemManager.toRPGItem(stack);
            Optional<RPGItem> stackItem = ItemManager.toRPGItem(itemStack);
            if (itemUsed.isPresent()) {
                if (!stackItem.isPresent()) {
                    return false;
                }
                if (!itemUsed.get().equals(stackItem.get()))
                    return false;
            }
            return true;
        }
    }
}
