package think.rpgitems.power.trigger;

import org.bukkit.entity.Player;
import org.bukkit.event.player.PlayerSwapHandItemsEvent;
import org.bukkit.inventory.ItemStack;
import think.rpgitems.power.Power;
import think.rpgitems.power.PowerMainhandItem;
import think.rpgitems.power.PowerResult;

class SwapToOffhand<TPower extends Power>
        extends Trigger<
                PlayerSwapHandItemsEvent, TPower, PowerMainhandItem<TPower>, Boolean, Boolean> {
    SwapToOffhand() {
        super(
                PlayerSwapHandItemsEvent.class,
                PowerMainhandItem.class,
                Boolean.class,
                Boolean.class,
                "SWAP_TO_OFFHAND");
    }

    public SwapToOffhand(String name) {
        super(
                name,
                "SWAP_TO_OFFHAND",
                PlayerSwapHandItemsEvent.class,
                PowerMainhandItem.class,
                Boolean.class,
                Boolean.class);
    }

    @Override
    public Boolean def(Player player, ItemStack i, PlayerSwapHandItemsEvent event) {
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
            PlayerSwapHandItemsEvent event) {
        return overrideResult.with(true);
    }

    @Override
    public PowerResult<Boolean> run(
            TPower power,
            PowerMainhandItem<TPower> pimpl,
            Player player,
            ItemStack i,
            PlayerSwapHandItemsEvent event) {
        return pimpl.swapToOffhand(power, player, i, event);
    }
}
