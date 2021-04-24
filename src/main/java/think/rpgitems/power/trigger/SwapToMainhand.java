package think.rpgitems.power.trigger;

import org.bukkit.entity.Player;
import org.bukkit.event.player.PlayerSwapHandItemsEvent;
import org.bukkit.inventory.ItemStack;
import think.rpgitems.power.Power;
import think.rpgitems.power.PowerOffhandItem;
import think.rpgitems.power.PowerResult;

class SwapToMainhand<TPower extends Power>
        extends Trigger<
                PlayerSwapHandItemsEvent, TPower, PowerOffhandItem<TPower>, Boolean, Boolean> {
    SwapToMainhand() {
        super(
                PlayerSwapHandItemsEvent.class,
                PowerOffhandItem.class,
                Boolean.class,
                Boolean.class,
                "SWAP_TO_MAINHAND");
    }

    public SwapToMainhand(String name) {
        super(
                name,
                "SWAP_TO_MAINHAND",
                PlayerSwapHandItemsEvent.class,
                PowerOffhandItem.class,
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
            PowerOffhandItem<TPower> pimpl,
            Player player,
            ItemStack i,
            PlayerSwapHandItemsEvent event) {
        return overrideResult.with(true);
    }

    @Override
    public PowerResult<Boolean> run(
            TPower power,
            PowerOffhandItem<TPower> pimpl,
            Player player,
            ItemStack i,
            PlayerSwapHandItemsEvent event) {
        return pimpl.swapToMainhand(power, player, i, event);
    }
}
