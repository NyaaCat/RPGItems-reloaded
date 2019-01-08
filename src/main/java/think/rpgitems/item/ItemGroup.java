package think.rpgitems.item;

import com.google.common.base.Strings;
import org.bukkit.command.CommandSender;
import org.bukkit.configuration.ConfigurationSection;
import org.bukkit.entity.Player;
import think.rpgitems.RPGItems;

import java.io.File;
import java.util.HashSet;
import java.util.Objects;
import java.util.Set;
import java.util.regex.Pattern;
import java.util.stream.Collectors;

public class ItemGroup {
    private static RPGItems plugin;

    private String name;
    private int uid;
    private String regex;
    private Set<Integer> itemUids;
    private Set<RPGItem> items;
    private File file;

    private String author = plugin.cfg.defaultAuthor;
    private String note = plugin.cfg.defaultNote;
    private String license = plugin.cfg.defaultLicense;

    public ItemGroup(String name, int uid, String regex, CommandSender author) {
        this.name = name;
        this.uid = uid;
        this.regex = regex;
        this.setAuthor(author instanceof Player ? ((Player) author).getUniqueId().toString() : plugin.cfg.defaultAuthor);
        if (!Strings.isNullOrEmpty(regex)) {
            Pattern p = Pattern.compile(regex);
            setItems(ItemManager.itemNames().stream().filter(p.asPredicate()).map(ItemManager::getItemByName).collect(Collectors.toSet()));
            setItemUids(items.stream().map(RPGItem::getUid).collect(Collectors.toSet()));
        }
    }

    public ItemGroup(ConfigurationSection s, File f) {
        setFile(f);
        name = s.getString("name");
        uid = s.getInt("uid");
        regex = s.getString("regex");

        restore(s);
    }

    public ItemGroup(ConfigurationSection s, String name, int uid, String regex) {
        if (uid >= 0) throw new IllegalArgumentException();
        this.name = name;
        this.uid = uid;
        this.regex = regex;
        restore(s);
    }

    private void restore(ConfigurationSection s) {
        setAuthor(s.getString("author", ""));
        setNote(s.getString("note", ""));
        setLicense(s.getString("license", ""));

        setItemUids(new HashSet<>(s.getIntegerList("item_uids")));
        setItems(getItemUids().stream().map(ItemManager::getItemById).filter(Objects::nonNull).collect(Collectors.toSet()));
    }

    public String getAuthor() {
        return author;
    }

    public File getFile() {
        return file;
    }

    public Set<Integer> getItemUids() {
        return itemUids;
    }

    public Set<RPGItem> getItems() {
        return items;
    }

    public String getLicense() {
        return license;
    }

    public String getName() {
        return name;
    }

    public String getNote() {
        return note;
    }

    public String getRegex() {
        return regex;
    }

    public int getUid() {
        return uid;
    }

    public void setAuthor(String author) {
        this.author = author;
    }

    public void setFile(File file) {
        this.file = file;
    }

    public void setItemUids(Set<Integer> itemUids) {
        this.itemUids = itemUids;
    }

    public void setItems(Set<RPGItem> items) {
        this.items = items;
    }

    public void setLicense(String license) {
        this.license = license;
    }

    public void setNote(String note) {
        this.note = note;
    }
}
