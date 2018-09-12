package think.rpgitems.utils;

import org.bukkit.potion.PotionEffectType;
import think.rpgitems.power.Getter;
import think.rpgitems.power.Setter;

public class PotionEffectUtils implements Setter<PotionEffectType>, Getter<PotionEffectType> {

    @Override
    public PotionEffectType set(String effect) {
        return PotionEffectType.getByName(effect);
    }

    @Override
    public String get(PotionEffectType object) {
        return object.getName();
    }
}
