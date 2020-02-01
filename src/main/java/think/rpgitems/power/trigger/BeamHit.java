package think.rpgitems.power.trigger;

import org.bukkit.entity.Player;
import org.bukkit.event.Event;
import org.bukkit.inventory.ItemStack;
import think.rpgitems.event.BeamHitBlockEvent;
import think.rpgitems.event.BeamHitEntityEvent;
import think.rpgitems.power.Pimpl;
import think.rpgitems.power.PowerBeamHit;
import think.rpgitems.power.PowerResult;

public class BeamHit<TEvent extends Event, TResult, TReturn> extends Trigger<TEvent, PowerBeamHit, TResult, TReturn> {

    BeamHit(Class<TEvent> tEventClass, Class<TResult> tResultClass, Class returnClass, String name) {
        super(tEventClass, PowerBeamHit.class, tResultClass, returnClass, name);
    }

    @Override
    public PowerResult<TResult> run(PowerBeamHit powerBeamHit, Player player, ItemStack i, TEvent event) {
        if (event instanceof BeamHitBlockEvent){
            BeamHitBlockEvent event1 = ((BeamHitBlockEvent) event);
            return (PowerResult<TResult>) powerBeamHit.hitBlock(player, i, event1.getLocation(), event1);
        }else if (event instanceof BeamHitEntityEvent){
            BeamHitEntityEvent event1 = (BeamHitEntityEvent) event;
            return (PowerResult<TResult>) powerBeamHit.hitEntity(player, i, event1.getEntity(), event1.getDamage(), event1);
        }else return PowerResult.fail();
    }
}
