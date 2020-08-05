package think.rpgitems.power.cond;

import org.bukkit.Material;
import org.bukkit.entity.Player;
import org.bukkit.inventory.EquipmentSlot;
import org.bukkit.inventory.ItemStack;
import think.rpgitems.item.ItemManager;
import think.rpgitems.item.RPGItem;
import think.rpgitems.power.Meta;
import think.rpgitems.power.PowerResult;
import think.rpgitems.power.Property;
import think.rpgitems.power.PropertyHolder;

import java.util.*;
import java.util.stream.Collectors;
import java.util.stream.Stream;

@Meta(marker = true)
public class EquipmentCondition extends BaseCondition<Void> {

    @Property(order = 0, required = true)
    public String id;

    @Property
    public boolean isCritical = false;

    @Property(alias = "slot")
    public Set<EquipmentSlot> slots;

    @Property
    public Material material;

    @Property
    public ItemStack itemStack;

    @Property
    public String rpgitem;

    @Property
    public boolean matchAllSlot = false;

    @Property
    public boolean requireEmpty = false;

    @Override
    public String id() {
        return id;
    }

    @Override
    public boolean isStatic() {
        return true;
    }

    @Override
    public boolean isCritical() {
        return isCritical;
    }

    @Override
    public PowerResult<Void> check(Player player, ItemStack stack, Map<PropertyHolder, PowerResult<?>> context) {
        if (slots.isEmpty()) {
            List<ItemStack> itemStacks = Stream.concat(
                    Arrays.stream(player.getInventory().getArmorContents()),
                    Stream.of(player.getInventory().getItemInMainHand(), player.getInventory().getItemInOffHand())
            ).collect(Collectors.toList());
            if (matchAllSlot) {
                return itemStacks.stream().allMatch(this::match) ? PowerResult.ok() : PowerResult.fail();
            } else {
                return itemStacks.stream().anyMatch(this::match) ? PowerResult.ok() : PowerResult.fail();
            }
        } else {
            int matches = 0;
            for (EquipmentSlot sl : slots) {
                ItemStack s;
                switch (sl) {
                    case HAND:
                        s = player.getInventory().getItemInMainHand();
                        break;
                    case OFF_HAND:
                        s = player.getInventory().getItemInOffHand();
                        break;
                    case FEET:
                        s = player.getInventory().getBoots();
                        break;
                    case LEGS:
                        s = player.getInventory().getLeggings();
                        break;
                    case CHEST:
                        s = player.getInventory().getChestplate();
                        break;
                    case HEAD:
                        s = player.getInventory().getHelmet();
                        break;
                    default:
                        throw new IllegalStateException();
                }
                if (match(s)) matches += 1;
            }
            if (matchAllSlot && matches == slots.size()) {
                return PowerResult.ok();
            } else if (!matchAllSlot && matches > 0) {
                return PowerResult.ok();
            }
            return PowerResult.fail();
        }
    }

    public boolean match(ItemStack stack) {
        if (requireEmpty){
            return stack == null || stack.getType().isAir();
        }
        if (stack == null) return material == null && itemStack == null && rpgitem == null;
        if (material != null && !stack.getType().equals(material)) {
            return false;
        }
        if (itemStack != null && !stack.isSimilar(itemStack)) {
            return false;
        }
        Optional<RPGItem> stackItem = ItemManager.toRPGItem(stack);
        if (rpgitem != null) {
            if (!stackItem.isPresent()) {
                return false;
            }
            try {
                int uid = Integer.parseInt(rpgitem);
                Set<RPGItem> items = ItemManager.getItems(uid);
                if (!items.contains(stackItem.get())) return false;
            } catch (NumberFormatException e) {
                Set<RPGItem> items = ItemManager.getItems(rpgitem);
                if (!items.contains(stackItem.get())) return false;
            }
        }
        return true;
    }

    @Override
    public Set<String> getConditions() {
        return Collections.emptySet();
    }

    @Override
    public String getName() {
        return "equipmentcondition";
    }

    @Override
    public String displayText() {
        return null;
    }
}
