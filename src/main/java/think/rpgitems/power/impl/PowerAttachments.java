package think.rpgitems.power.impl;

import cat.nyaa.nyaacore.Pair;
import org.bukkit.entity.LivingEntity;
import org.bukkit.entity.Player;
import org.bukkit.event.Event;
import org.bukkit.event.entity.EntityDamageByEntityEvent;
import org.bukkit.event.player.PlayerInteractEvent;
import org.bukkit.inventory.EquipmentSlot;
import org.bukkit.inventory.ItemStack;
import org.bukkit.inventory.PlayerInventory;
import org.librazy.nclangchecker.LangKey;
import think.rpgitems.item.ItemManager;
import think.rpgitems.item.RPGItem;
import think.rpgitems.power.*;

import java.util.*;
import java.util.stream.Collectors;

@PowerMeta(defaultTrigger = "RIGHT_CLICK", withSelectors = true, generalInterface = PowerPlain.class)
public class PowerAttachments extends BasePower implements PowerTick, PowerRightClick, PowerLeftClick, PowerOffhandClick, PowerPlain, PowerHit {

    @Property
    public List<EquipmentSlot> allowedSlots;

    @Property
    public List<Integer> allowedInvSlots;

    @Property
    public int limit;

    @Property
    public Set<String> allowedItems;

    @Override
    public PowerResult<Void> tick(Player player, ItemStack stack) {
        return fire(player, stack);
    }

    @Override
    public PowerResult<Double> hit(Player player, ItemStack stack, LivingEntity entity, double damage, EntityDamageByEntityEvent event) {
        return fire(player, stack, event).with(damage);
    }

    @Override
    public PowerResult<Void> leftClick(Player player, ItemStack stack, PlayerInteractEvent event) {
        return fire(player, stack, event);
    }

    @Override
    public PowerResult<Void> offhandClick(Player player, ItemStack stack, PlayerInteractEvent event) {
        return fire(player, stack, event);
    }

    @Override
    public PowerResult<Void> rightClick(Player player, ItemStack stack, PlayerInteractEvent event) {
        return fire(player, stack, event);
    }

    @Override
    public PowerResult<Void> fire(Player player, ItemStack stack) {
        return fire(player, stack, null);
    }

    public PowerResult<Void> fire(Player player, ItemStack stack, Event event) {
        Set<RPGItem> allow = (allowedItems == null || allowedItems.isEmpty()) ? null : allowedItems.stream().flatMap(s -> {
            try {
                int uid = Integer.parseInt(s);
                Set<RPGItem> items = ItemManager.getItems(uid);
                return items.stream();
            } catch (NumberFormatException e) {
                Set<RPGItem> items = ItemManager.getItems(s);
                return items.stream();
            }
        }).collect(Collectors.toSet());
        int num = 0;
        ItemStack itemStack = null;
        PlayerInventory inventory = player.getInventory();
        for (EquipmentSlot allowedSlot : allowedSlots) {
            switch (allowedSlot) {
                case HAND:
                    itemStack = inventory.getItemInMainHand();
                    break;
                case OFF_HAND:
                    itemStack = inventory.getItemInOffHand();
                    break;
                case FEET:
                    itemStack = inventory.getBoots();
                    break;
                case LEGS:
                    itemStack = inventory.getLeggings();
                    break;
                case CHEST:
                    itemStack = inventory.getChestplate();
                    break;
                case HEAD:
                    itemStack = inventory.getHelmet();
                    break;
            }
            if (attach(player, stack, event, itemStack, allow)) {
                num += 1;
            }
            if (num >= limit) return PowerResult.ok();
        }
        if (allowedInvSlots == null || allowedInvSlots.isEmpty()) {
            for (ItemStack envSlot: inventory.getContents()) {
                if (attach(player, stack, event, envSlot, allow)) {
                    num += 1;
                }
                if (num >= limit) return PowerResult.ok();
            }
        } else {
            for (int envSlot: allowedInvSlots) {
                if (envSlot < 0) break;
                itemStack = inventory.getItem(envSlot);
                if (attach(player, stack, event, itemStack, allow)) {
                    num += 1;
                }
                if (num >= limit) return PowerResult.ok();
            }
        }

        if (num == 0) {
            return PowerResult.fail();
        } else {
            return PowerResult.ok();
        }
    }

    public boolean attach(Player player, ItemStack stack, Event event, ItemStack itemStack, Set<RPGItem> allow) {
        if (itemStack == null) return false;
        if (itemStack.equals(stack)) return false;
        Optional<RPGItem> optItem = ItemManager.toRPGItem(itemStack);
        if (!optItem.isPresent()) return false;
        RPGItem item = optItem.get();
        if (allow != null && !allow.contains(item)) return false;
        item.power(player, itemStack, event, Trigger.ATTACHMENT, Pair.of(stack, event));
        return true;
    }

    @Override
    public @LangKey(skipCheck = true) String getName() {
        return "attachments";
    }

    @Override
    public String displayText() {
        return null;
    }
}
