package think.rpgitems.trigger;

import cat.nyaa.nyaacore.Pair;
import org.bukkit.entity.Player;
import org.bukkit.event.Event;
import org.bukkit.inventory.ItemStack;
import think.rpgitems.item.ItemManager;
import think.rpgitems.power.PowerAttachment;
import think.rpgitems.power.PowerResult;
import think.rpgitems.power.Trigger;

public class Attachment extends Trigger<Event, PowerAttachment, Void, Void> {
    public static final Trigger<Event, PowerAttachment, Void, Void> ATTACHMENT = new Attachment();

    public Attachment() {
        super(Event.class, PowerAttachment.class, Void.class, Void.class, "ATTACHMENT");
    }

    @Override
    public PowerResult<Void> run(PowerAttachment power, Player player, ItemStack i, Event event) {
        throw new IllegalStateException();
    }

    @Override
    public PowerResult<Void> run(PowerAttachment power, Player player, ItemStack i, Event event, Object data) {
        ItemStack originalItemstack = (ItemStack) ((Pair) data).getKey();
        Event originalEvent = (Event) ((Pair) data).getValue();
        return power.attachment(player, i, ItemManager.toRPGItem(originalItemstack).orElseThrow(IllegalArgumentException::new), originalEvent, originalItemstack);
    }
}
