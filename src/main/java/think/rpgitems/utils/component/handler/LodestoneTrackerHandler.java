package think.rpgitems.utils.component.handler;

import io.papermc.paper.datacomponent.DataComponentType;
import io.papermc.paper.datacomponent.DataComponentTypes;
import io.papermc.paper.datacomponent.item.LodestoneTracker;
import org.bukkit.Bukkit;
import org.bukkit.Location;
import org.bukkit.configuration.ConfigurationSection;
import org.jetbrains.annotations.Nullable;
import think.rpgitems.item.RPGItem;
import think.rpgitems.utils.component.ComponentHandler;

import java.util.List;

public class LodestoneTrackerHandler implements ComponentHandler<LodestoneTracker> {

    @Override
    public DataComponentType type() {
        return DataComponentTypes.LODESTONE_TRACKER;
    }

    @Override
    public String yamlKey() {
        return "lodestone_tracker";
    }

    @Override
    public @Nullable Object decode(ConfigurationSection parent, @Nullable RPGItem rpgItem) {
        ConfigurationSection section = parent.getConfigurationSection(yamlKey());
        if (section == null) {
            return null;
        }
        LodestoneTracker.Builder builder = LodestoneTracker.lodestoneTracker();
        ConfigurationSection target = section.getConfigurationSection("target");
        if (target != null) {
            String dimensionStr = target.getString("dimension", Bukkit.getWorlds().getFirst().getName());
            List<Integer> xyz = target.getIntegerList("pos");
            if (xyz.isEmpty()) {
                xyz = List.of(0, 0, 0);
            }
            builder.location(new Location(Bukkit.getWorld(dimensionStr), xyz.get(0), xyz.get(1), xyz.get(2)));
        }
        builder.tracked(section.getBoolean("tracked", true));
        return builder;
    }

    @Override
    public void encode(LodestoneTracker value, ConfigurationSection config) {
        ConfigurationSection section = config.createSection(yamlKey());
        section.set("tracked", value.tracked());
        Location location = value.location();
        if (location.getWorld() != null) {
            section.set("target.dimension", location.getWorld().getName());
            section.set("target.pos", List.of(location.x(), location.y(), location.z()));
        }
    }
}
