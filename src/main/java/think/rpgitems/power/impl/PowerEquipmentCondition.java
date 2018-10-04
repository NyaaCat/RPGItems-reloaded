package think.rpgitems.power.impl;

import org.bukkit.Material;
import org.bukkit.entity.Player;
import org.bukkit.inventory.EquipmentSlot;
import org.bukkit.inventory.ItemStack;
import think.rpgitems.item.ItemManager;
import think.rpgitems.item.RPGItem;
import think.rpgitems.power.*;

import java.util.*;

@PowerMeta(marker = true)
public class PowerEquipmentCondition extends BasePower implements PowerCondition<Void> {

    @Property(order = 0, required = true)
    public String id;

    @Property
    public boolean isCritical = false;

    @Property
    public EquipmentSlot slot;

    @Property
    public Material material;

    @Property
    public ItemStack itemStack;

    @Property
    public String rpgitem;

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
    public PowerResult<Void> check(Player player, ItemStack stack, Map<Power, PowerResult> context) {
        if (slot == null) {
            List<ItemStack> itemStacks = new ArrayList<>(Arrays.asList(player.getInventory().getArmorContents()));
            itemStacks.add(player.getInventory().getItemInMainHand());
            itemStacks.add(player.getInventory().getItemInOffHand());
            return itemStacks.stream().anyMatch(this::match) ? PowerResult.ok() : PowerResult.fail();
        } else {
            ItemStack s;
            switch (slot) {
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
            return match(s) ? PowerResult.ok() : PowerResult.fail();
        }
    }

    public boolean match(ItemStack stack) {
        if (material != null && !stack.getType().equals(material)) {
            return false;
        }
        if (itemStack != null && !stack.isSimilar(itemStack)) {
            return false;
        }
        RPGItem rpgItem = ItemManager.toRPGItem(stack);
        if (rpgitem != null && (rpgItem == null || !(rpgItem.getName().equals(rpgitem) || String.valueOf(rpgItem.getUID()).equals(rpgitem)))) {
            return false;
        }
        return true;
    }

    @Override
    public String getName() {
        return "equipmentcondition";
    }

    @Override
    public String displayText() {
        return null;
    }

    @Override
    public Set<String> getConditions() {
        return Collections.emptySet();
    }
}
