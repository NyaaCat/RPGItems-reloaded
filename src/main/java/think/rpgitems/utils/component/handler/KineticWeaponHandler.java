package think.rpgitems.utils.component.handler;

import io.papermc.paper.datacomponent.DataComponentType;
import io.papermc.paper.datacomponent.DataComponentTypes;
import io.papermc.paper.datacomponent.item.KineticWeapon;
import net.kyori.adventure.key.Key;
import org.bukkit.configuration.ConfigurationSection;
import org.intellij.lang.annotations.Subst;
import org.jetbrains.annotations.Nullable;
import think.rpgitems.item.RPGItem;
import think.rpgitems.utils.component.ComponentHandler;

public class KineticWeaponHandler implements ComponentHandler<KineticWeapon> {

    @Override
    public DataComponentType type() {
        return DataComponentTypes.KINETIC_WEAPON;
    }

    @Override
    public String yamlKey() {
        return "kinetic_weapon";
    }

    @Override
    public @Nullable Object decode(ConfigurationSection parent, @Nullable RPGItem rpgItem) {
        ConfigurationSection section = parent.getConfigurationSection(yamlKey());
        if (section == null) {
            return null;
        }
        KineticWeapon.Builder builder = KineticWeapon.kineticWeapon();
        builder.delayTicks(section.getInt("delay_ticks", 0));
        builder.forwardMovement((float) section.getDouble("forward_movement", 0.0));
        builder.damageMultiplier((float) section.getDouble("damage_multiplier", 1.0));
        @Subst("item.spear.use") String sound = section.getString("sound", "item.spear.use");
        builder.sound(Key.key(sound));
        @Subst("item.spear.hit") String hitSound = section.getString("hit_sound", "item.spear.hit");
        builder.hitSound(Key.key(hitSound));

        applyCondition(section, "dismount_conditions", builder::dismountConditions);
        applyCondition(section, "knockback_conditions", builder::knockbackConditions);
        applyCondition(section, "damage_conditions", builder::damageConditions);

        builder.contactCooldownTicks(section.getInt("contact_cooldown_ticks", 10));
        return builder;
    }

    private void applyCondition(ConfigurationSection section, String path,
                                 java.util.function.Consumer<KineticWeapon.Condition> setter) {
        ConfigurationSection sec = section.getConfigurationSection(path);
        if (sec == null) {
            return;
        }
        setter.accept(KineticWeapon.condition(
                sec.getInt("max_duration_ticks", 80),
                (float) sec.getDouble("min_speed", 0),
                (float) sec.getDouble("min_relative_speed", 0)));
    }

    @Override
    public void encode(KineticWeapon value, ConfigurationSection config) {
        ConfigurationSection section = config.createSection(yamlKey());
        section.set("delay_ticks", value.delayTicks());
        section.set("forward_movement", value.forwardMovement());
        section.set("damage_multiplier", value.damageMultiplier());
        if (value.sound() != null) {
            section.set("sound", value.sound().asString());
        }
        if (value.hitSound() != null) {
            section.set("hit_sound", value.hitSound().asString());
        }
        serializeCondition(section, "dismount_conditions", value.dismountConditions());
        serializeCondition(section, "knockback_conditions", value.knockbackConditions());
        serializeCondition(section, "damage_conditions", value.damageConditions());
        section.set("contact_cooldown_ticks", value.contactCooldownTicks());
    }

    private void serializeCondition(ConfigurationSection parent, String path, KineticWeapon.Condition cond) {
        if (cond == null) {
            return;
        }
        ConfigurationSection sec = parent.createSection(path);
        sec.set("max_duration_ticks", cond.maxDurationTicks());
        sec.set("min_speed", cond.minSpeed());
        sec.set("min_relative_speed", cond.minRelativeSpeed());
    }
}
