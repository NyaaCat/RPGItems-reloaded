package think.rpgitems.power.trigger;

import org.bukkit.entity.Player;
import org.bukkit.event.inventory.InventoryClickEvent;
import org.bukkit.inventory.ItemStack;
import think.rpgitems.power.Power;
import think.rpgitems.power.PowerOffhandItem;
import think.rpgitems.power.PowerResult;

class PickupOffhand<TPower extends Power>
    extends Trigger<InventoryClickEvent, TPower, PowerOffhandItem<TPower>, Boolean, Boolean> {
  PickupOffhand() {
    super(
        InventoryClickEvent.class,
        PowerOffhandItem.class,
        Boolean.class,
        Boolean.class,
        "PICKUP_OFF_HAND");
  }

  public PickupOffhand(String name) {
    super(
        name,
        "PICKUP_OFF_HAND",
        InventoryClickEvent.class,
        PowerOffhandItem.class,
        Boolean.class,
        Boolean.class);
  }

  @Override
  public Boolean def(Player player, ItemStack i, InventoryClickEvent event) {
    return true;
  }

  @Override
  public Boolean next(Boolean a, PowerResult<Boolean> b) {
    return b.isOK() ? b.data() && a : a;
  }

  @Override
  public PowerResult<Boolean> warpResult(
      PowerResult<Void> overrideResult,
      TPower power,
      PowerOffhandItem<TPower> pimpl,
      Player player,
      ItemStack i,
      InventoryClickEvent event) {
    return overrideResult.with(true);
  }

  @Override
  public PowerResult<Boolean> run(
      TPower power,
      PowerOffhandItem<TPower> pimpl,
      Player player,
      ItemStack i,
      InventoryClickEvent event) {
    return pimpl.pickupOffhand(power, player, i, event);
  }
}
