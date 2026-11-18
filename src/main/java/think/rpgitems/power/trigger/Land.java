package think.rpgitems.power.trigger;

import com.destroystokyo.paper.event.player.PlayerJumpEvent;
import io.papermc.paper.event.entity.EntityLandEvent;
import org.bukkit.entity.Player;
import org.bukkit.inventory.ItemStack;
import think.rpgitems.power.PowerJump;
import think.rpgitems.power.PowerLand;
import think.rpgitems.power.PowerResult;

class Land extends Trigger<EntityLandEvent, PowerLand, Void, Void> {
    Land() {
        super(EntityLandEvent.class, PowerLand.class, Void.class, Void.class, "LAND");
    }

    public Land(String name) {
        super(name, "LAND", EntityLandEvent.class, PowerLand.class, Void.class, Void.class);
    }

    @Override
    public PowerResult<Void> run(PowerLand power, Player player, ItemStack i, EntityLandEvent event) {
        return power.land(player, i, event);
    }
}
