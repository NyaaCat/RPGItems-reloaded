package think.rpgitems.utils.component.handler;

import io.papermc.paper.datacomponent.DataComponentType;
import io.papermc.paper.datacomponent.DataComponentTypes;
import io.papermc.paper.datacomponent.item.PiercingWeapon;
import net.kyori.adventure.key.Key;
import org.bukkit.configuration.ConfigurationSection;
import org.intellij.lang.annotations.Subst;
import org.jetbrains.annotations.Nullable;
import think.rpgitems.item.RPGItem;
import think.rpgitems.utils.component.ComponentHandler;

public class PiercingWeaponHandler implements ComponentHandler<PiercingWeapon> {

    @Override
    public DataComponentType type() {
        return DataComponentTypes.PIERCING_WEAPON;
    }

    @Override
    public String yamlKey() {
        return "piercing_weapon";
    }

    @Override
    public @Nullable Object decode(ConfigurationSection parent, @Nullable RPGItem rpgItem) {
        ConfigurationSection section = parent.getConfigurationSection(yamlKey());
        if (section == null) {
            return null;
        }
        PiercingWeapon.Builder builder = PiercingWeapon.piercingWeapon();
        @Subst("item.spear.use") String sound = section.getString("sound", "item.spear.use");
        builder.sound(Key.key(sound));
        @Subst("item.spear.hit") String hitSound = section.getString("hit_sound", "item.spear.hit");
        builder.hitSound(Key.key(hitSound));
        builder.dealsKnockback(section.getBoolean("deals_knockback", true));
        builder.dismounts(section.getBoolean("dismounts", false));
        return builder;
    }

    @Override
    public void encode(PiercingWeapon value, ConfigurationSection config) {
        ConfigurationSection section = config.createSection(yamlKey());
        section.set("sound", value.sound().asString());
        section.set("hit_sound", value.hitSound().asString());
        section.set("deals_knockback", value.dealsKnockback());
        section.set("dismounts", value.dismounts());
    }
}
