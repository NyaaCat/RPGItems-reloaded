package think.rpgitems.power.cond;

import org.bukkit.entity.Player;
import org.bukkit.inventory.ItemStack;
import org.bukkit.inventory.PlayerInventory;
import org.bukkit.scheduler.BukkitRunnable;
import think.rpgitems.RPGItems;
import think.rpgitems.data.LightContext;
import think.rpgitems.power.*;

import java.util.*;
import java.util.concurrent.atomic.AtomicBoolean;
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
                            checkLeggings(inventory, stack) || checkLeggings(inventory, stack);
                case HAND:
                    return checkMainHand(inventory, stack) && checkOffHand(inventory, stack);
                case BACKPACK:
                    return checkBackpack(inventory, stack);
                case BELT:
                    break;
                case INVENTORY:
                    break;
                case MAIN_HAND:
                    break;
                case OFF_HAND:
                    break;
            }
            return false;
        }

        private boolean checkBackpack(PlayerInventory inventory, ItemStack stack) {
            ItemStack[] contents = inventory.getContents();
            ItemStack[] itemStacks = Arrays.copyOfRange(contents, 0, 27);
            return false;
        }

        private boolean checkMainHand(PlayerInventory inventory, ItemStack stack) {
            ItemStack itemInMainHand = inventory.getItemInMainHand();
            return stack.equals(itemInMainHand);
        }

        private boolean checkOffHand(PlayerInventory inventory, ItemStack stack) {
            ItemStack itemInOffHand = inventory.getItemInOffHand();
            return stack.equals(itemInOffHand);
        }

        private boolean checkBoots(PlayerInventory inventory, ItemStack stack) {
            ItemStack boots = inventory.getBoots();
            return stack.equals(boots);
        }

        private boolean checkLeggings(PlayerInventory inventory, ItemStack stack) {
            ItemStack leggings = inventory.getLeggings();
            return stack.equals(leggings);
        }

        private boolean checkChestPlate(PlayerInventory inventory, ItemStack stack) {
            ItemStack chestPlate = inventory.getChestplate();
            return stack.equals(chestPlate);
        }

        private boolean checkHelmet(PlayerInventory inventory, ItemStack stack) {
            ItemStack helmet = inventory.getHelmet();
            return stack.equals(helmet);
        }
    }
}
