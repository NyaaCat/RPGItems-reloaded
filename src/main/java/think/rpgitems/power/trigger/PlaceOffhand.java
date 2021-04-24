package think.rpgitems.power.trigger;

import org.bukkit.entity.Player;
import org.bukkit.event.inventory.InventoryClickEvent;
import org.bukkit.inventory.ItemStack;
import think.rpgitems.power.Power;
import think.rpgitems.power.PowerMainhandItem;
import think.rpgitems.power.PowerResult;

class PlaceOffhand<TPower extends Power>
    extends Trigger<InventoryClickEvent, TPower, PowerMainhandItem<TPower>, Boolean, Boolean> {
  PlaceOffhand() {
    super(
        InventoryClickEvent.class,
        PowerMainhandItem.class,
        Boolean.class,
        Boolean.class,
        "PLACE_OFF_HAND");
  }

  public PlaceOffhand(String name) {
    super(
        name,
        "PLACE_OFF_HAND",
        InventoryClickEvent.class,
        PowerMainhandItem.class,
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
      PowerMainhandItem<TPower> pimpl,
      Player player,
      ItemStack i,
      InventoryClickEvent event) {
    return overrideResult.with(true);
  }

  @Override
  public PowerResult<Boolean> run(
      TPower power,
      PowerMainhandItem<TPower> pimpl,
      Player player,
      ItemStack i,
      InventoryClickEvent event) {
    return pimpl.placeOffhand(power, player, i, event);
  }
}
