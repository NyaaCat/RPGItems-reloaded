package think.rpgitems.trigger;

import org.bukkit.entity.Player;
import org.bukkit.event.player.PlayerSwapHandItemsEvent;
import org.bukkit.inventory.ItemStack;
import think.rpgitems.power.PowerMainhandItem;
import think.rpgitems.power.PowerResult;
import think.rpgitems.power.Trigger;

public class SwapToOffhand extends Trigger<PlayerSwapHandItemsEvent, PowerMainhandItem, Boolean, Boolean> {
    public static final Trigger<PlayerSwapHandItemsEvent, PowerMainhandItem, Boolean, Boolean> SWAP_TO_OFFHAND = new SwapToOffhand();

    public SwapToOffhand() {
        super(PlayerSwapHandItemsEvent.class, PowerMainhandItem.class, Boolean.class, Boolean.class, "SWAP_TO_OFFHAND");
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
    public PowerResult<Boolean> warpResult(PowerResult<Void> overrideResult, PowerMainhandItem power, Player player, ItemStack i, PlayerSwapHandItemsEvent event) {
        return overrideResult.with(true);
    }

    @Override
    public PowerResult<Boolean> run(PowerMainhandItem power, Player player, ItemStack i, PlayerSwapHandItemsEvent event) {
        return power.swapToOffhand(player, i, event);
    }
}
