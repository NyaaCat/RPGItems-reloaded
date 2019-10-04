package think.rpgitems.power.trigger;

import org.bukkit.entity.Player;
import org.bukkit.event.inventory.InventoryClickEvent;
import org.bukkit.inventory.ItemStack;
import think.rpgitems.power.PowerOffhandItem;
import think.rpgitems.power.PowerResult;

class PickupOffhand extends Trigger<InventoryClickEvent, PowerOffhandItem, Boolean, Boolean> {
    PickupOffhand() {
        super(InventoryClickEvent.class, PowerOffhandItem.class, Boolean.class, Boolean.class, "PICKUP_OFF_HAND");
    }
    public PickupOffhand(String name) {
        super(name, "PICKUP_OFF_HAND", InventoryClickEvent.class, PowerOffhandItem.class, Boolean.class, Boolean.class);
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
    public PowerResult<Boolean> warpResult(PowerResult<Void> overrideResult, PowerOffhandItem power, Player player, ItemStack i, InventoryClickEvent event) {
        return overrideResult.with(true);
    }

    @Override
    public PowerResult<Boolean> run(PowerOffhandItem power, Player player, ItemStack i, InventoryClickEvent event) {
        return power.pickupOffhand(player, i, event);
    }
}
