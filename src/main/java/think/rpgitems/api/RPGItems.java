package think.rpgitems.api;

import static think.rpgitems.item.ItemManager.parseItemInfo;

import java.util.Objects;
import org.bukkit.inventory.ItemStack;
import think.rpgitems.item.ItemManager;
import think.rpgitems.item.RPGItem;

public class RPGItems {

  /**
   * If the itemstack is a RPGItem this will return the RPGItem version of the item. If the
   * itemstack isn't a RPGItem this will return null.
   *
   * @param itemstack The item to converted
   * @return The RPGItem or null
   */
  public RPGItem toRPGItem(ItemStack itemstack) {
    return ItemManager.toRPGItemByMeta(itemstack).orElse(null);
  }

  public boolean isEqual(ItemStack a, ItemStack b) {
    return Objects.equals(parseItemInfo(a), parseItemInfo(b));
  }
}
