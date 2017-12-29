package think.rpgitems.power.types;

import org.bukkit.entity.Player;
import org.bukkit.scheduler.BukkitRunnable;
import think.rpgitems.RPGItems;

public interface PowerDelayable extends IPower {

    /**
     * make a power support delay.
     * usage: put what you want to do later and call triggerLater();
     * remember to add a property "delay".
     *
     * @param task a Runnable that will be called later.
     * @param delay ticks before activate, if it is 0, it will be triggered immediately
     */
    default void triggerLater(Runnable task, int delay){
        if( delay != 0 ){
            new BukkitRunnable() {
                @Override
                public void run() {
                    task.run();
                }
            }.runTaskLater(RPGItems.plugin,delay);
        }else task.run();
    }

}
