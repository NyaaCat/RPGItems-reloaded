package think.rpgitems.utils;

import org.bukkit.potion.PotionEffectType;
import think.rpgitems.power.Getter;
import think.rpgitems.power.Setter;

public class PotionEffectUtils implements Setter, Getter {

    @Override
    public Object set(String effect) {
        return PotionEffectType.getByName(effect);
    }

    @Override
    public String get(Object object) {
        return ((PotionEffectType) object).getName();
    }
}
