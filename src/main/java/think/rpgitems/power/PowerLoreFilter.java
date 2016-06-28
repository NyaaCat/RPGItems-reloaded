package think.rpgitems.power;

import org.bukkit.configuration.ConfigurationSection;

public class PowerLoreFilter extends Power{
    public String regex = null;
    public String desc = "";
    @Override
    public void init(ConfigurationSection s) {
        regex = s.getString("regex", null);
        desc = s.getString("desc", "");
    }

    @Override
    public void save(ConfigurationSection s) {
        s.set("regex", regex);
        s.set("desc", desc);
    }

    @Override
    public String getName() {
        return "lorefilter";
    }

    @Override
    public String displayText() {
        return desc;
    }
}
