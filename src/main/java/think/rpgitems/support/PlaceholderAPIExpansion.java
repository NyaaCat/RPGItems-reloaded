package think.rpgitems.support;

import think.rpgitems.RPGItems;
import me.clip.placeholderapi.expansion.PlaceholderExpansion;
import org.bukkit.OfflinePlayer;
import org.jetbrains.annotations.NotNull;
import think.rpgitems.item.ItemManager;

import java.io.File;


public class PlaceholderAPIExpansion extends PlaceholderExpansion {

    public static RPGItems plugin; //

    public PlaceholderAPIExpansion(RPGItems plugin) {
        this.plugin = plugin;
    }

    @Override
    @NotNull
    public String getAuthor() {
        return String.join(", ", plugin.getDescription().getAuthors()); //
    }

    @Override
    @NotNull
    public String getIdentifier() {
        return "rpgitems";
    }

    @Override
    @NotNull
    public String getVersion() {
        return plugin.getDescription().getVersion(); //
    }

    @Override
    public boolean persist() {
        return true; //
    }

    @Override
    public String onRequest(OfflinePlayer player, @NotNull String params) {
        String[] param = params.split("_");
        String error = "%rpgitems_items%\n%rpgitems_item_display_<id>%\n%rpgitems_item_lores_<id>%";
        if (params.isEmpty()||params.length()==3||params.length()>4||(params.length()==1&&!param[0].equals("version"))) {
            return error;
        }
        if(params.length()==1){
            File folder = RPGItems.plugin.getDataFolder();
            File targetFolder = new File(folder,"items");
            if (targetFolder.exists() && targetFolder.isDirectory()) {
                int fileCount = targetFolder.listFiles().length;
                return String.valueOf(fileCount);
            } else {
                return String.valueOf(0);
            }
        }
        if(params.length()==4){
            if(param[2].equals("display")){
                try {
                    return ItemManager.getItemByName(param[3]).getDisplayName();
                } catch (Exception e) {
                    return "ID "+param[3]+" does not exist";
                }
            } else if (param[2].equals("lores")) {
                try {
                    String lores="";
                    for (String line:ItemManager.getItemByName(param[3]).getDescription()){
                        lores += line+"\n";
                    }
                    return lores;
                } catch (Exception e) {
                    return "ID "+param[3]+" does not exist";
                }
            }else{
                return error;
            }
        }
        return null;
    }
}