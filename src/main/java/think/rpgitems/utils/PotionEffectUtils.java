package think.rpgitems.utils;

import java.util.Optional;
import org.bukkit.potion.PotionEffectType;
import think.rpgitems.power.Getter;
import think.rpgitems.power.Setter;

public class PotionEffectUtils implements Setter<PotionEffectType>, Getter<PotionEffectType> {

    @Override
    public Optional<PotionEffectType> set(String effect) {
        PotionEffectType potionEffect = PotionEffectType.getByName(effect);
        if (potionEffect == null) throw new IllegalArgumentException();
        return Optional.of(potionEffect);
    }

    @Override
    public String get(PotionEffectType object) {
        return object.getName();
    }
}
