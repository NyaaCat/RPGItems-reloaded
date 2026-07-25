package think.rpgitems.utils.component.handler;

import io.papermc.paper.datacomponent.DataComponentType;
import io.papermc.paper.datacomponent.DataComponentTypes;
import io.papermc.paper.datacomponent.item.AttackRange;
import org.bukkit.configuration.ConfigurationSection;
import org.jetbrains.annotations.Nullable;
import think.rpgitems.item.RPGItem;
import think.rpgitems.utils.component.ComponentHandler;

public class AttackRangeHandler implements ComponentHandler<AttackRange> {

    @Override
    public DataComponentType type() {
        return DataComponentTypes.ATTACK_RANGE;
    }

    @Override
    public String yamlKey() {
        return "attack_range";
    }

    @Override
    public @Nullable Object decode(ConfigurationSection parent, @Nullable RPGItem rpgItem) {
        ConfigurationSection section = parent.getConfigurationSection(yamlKey());
        if (section == null) {
            return null;
        }
        return AttackRange.attackRange()
                .maxReach((float) section.getDouble("max_reach", 3))
                .minReach((float) section.getDouble("min_reach"))
                .hitboxMargin((float) section.getDouble("hitbox_margin", 0.3))
                .mobFactor((float) section.getDouble("mob_factor", 1.0));
    }

    @Override
    public void encode(AttackRange value, ConfigurationSection config) {
        ConfigurationSection section = config.createSection(yamlKey());
        section.set("max_reach", value.maxReach());
        section.set("min_reach", value.minReach());
        section.set("hitbox_margin", value.hitboxMargin());
        section.set("mob_factor", value.mobFactor());
    }
}
