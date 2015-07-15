package think.rpgitems.item;

import java.util.ArrayList;
import java.util.List;

import org.bukkit.configuration.ConfigurationSection;

public class ItemGroup {
    private String name;
    private List<Integer> items = new ArrayList<Integer>();
    
    public ItemGroup(String name) {
        this.name = name;
    }
    
    public ItemGroup(ConfigurationSection s) {
        name = s.getString("name");
        items = s.getIntegerList("items");
    }
    
    public void save(ConfigurationSection s) {
        s.set("name", name);
        s.set("items", items);
    }
    
    public String getName() {
        return name;
    }
}
