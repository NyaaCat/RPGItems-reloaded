package think.rpgitems.power.trigger;

import org.bukkit.event.entity.EntityDamageByEntityEvent;
import think.rpgitems.power.PowerHit;

import java.util.Optional;

public class HitGlobal extends Hit {
    HitGlobal() {
        super("HIT_GLOBAL", 1);
    }
}
