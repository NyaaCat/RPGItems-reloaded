package think.rpgitems.utils.component.handler;

import io.papermc.paper.datacomponent.DataComponentType;
import io.papermc.paper.datacomponent.DataComponentTypes;
import io.papermc.paper.datacomponent.item.Weapon;
import org.bukkit.configuration.ConfigurationSection;
import org.jetbrains.annotations.Nullable;
import think.rpgitems.item.RPGItem;
import think.rpgitems.utils.component.ComponentHandler;

public class WeaponHandler implements ComponentHandler<Weapon> {

    @Override
    public DataComponentType type() {
        return DataComponentTypes.WEAPON;
    }

    @Override
    public String yamlKey() {
        return "weapon";
    }

    @Override
    public @Nullable Object decode(ConfigurationSection parent, @Nullable RPGItem rpgItem) {
        ConfigurationSection section = parent.getConfigurationSection(yamlKey());
        if (section == null) {
            return null;
        }
        Weapon.Builder builder = Weapon.weapon();
        String itemName = rpgItem == null ? "" : ":" + rpgItem.getName();

        if (section.contains("item_damage_per_attack")) {
            int damage = section.getInt("item_damage_per_attack", 1);
            if (damage < 0) {
                throw new IllegalArgumentException("Weapon damage cannot be negative" + itemName);
            }
            builder.itemDamagePerAttack(damage);
        }
        if (section.contains("disable_blocking_for_seconds")) {
            int disable = section.getInt("disable_blocking_for_seconds", 0);
            if (disable <= 0) {
                throw new IllegalArgumentException("Weapon attack speed must be greater than 0" + itemName);
            }
            builder.disableBlockingForSeconds(disable);
        }
        return builder;
    }

    @Override
    public void encode(Weapon value, ConfigurationSection config) {
        ConfigurationSection section = config.createSection(yamlKey());
        section.set("item_damage_per_attack", value.itemDamagePerAttack());
        section.set("disable_blocking_for_seconds", value.disableBlockingForSeconds());
    }
}
