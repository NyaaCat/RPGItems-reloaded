package think.rpgitems.trigger;

import org.bukkit.entity.Player;
import org.bukkit.event.player.PlayerSwapHandItemsEvent;
import org.bukkit.inventory.ItemStack;
import think.rpgitems.power.PowerOffhandItem;
import think.rpgitems.power.PowerResult;
import think.rpgitems.power.Trigger;

public class SwapToMainhand extends Trigger<PlayerSwapHandItemsEvent, PowerOffhandItem, Boolean, Boolean> {
    public static final Trigger<PlayerSwapHandItemsEvent, PowerOffhandItem, Boolean, Boolean> SWAP_TO_MAINHAND = new SwapToMainhand();

    public SwapToMainhand() {
        super(PlayerSwapHandItemsEvent.class, PowerOffhandItem.class, Boolean.class, Boolean.class, "SWAP_TO_MAINHAND");
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
    public PowerResult<Boolean> warpResult(PowerResult<Void> overrideResult, PowerOffhandItem power, Player player, ItemStack i, PlayerSwapHandItemsEvent event) {
        return overrideResult.with(true);
    }

    @Override
    public PowerResult<Boolean> run(PowerOffhandItem power, Player player, ItemStack i, PlayerSwapHandItemsEvent event) {
        return power.swapToMainhand(player, i, event);
    }
}
