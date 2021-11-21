package think.rpgitems;

import cat.nyaa.nyaacore.LanguageRepository;
import cat.nyaa.nyaacore.cmdreceiver.Arguments;
import cat.nyaa.nyaacore.cmdreceiver.SubCommand;
import org.bukkit.command.CommandSender;
import think.rpgitems.power.PowerManager;
import think.rpgitems.power.PropertyHolder;
import think.rpgitems.power.PropertyInstance;
import think.rpgitems.power.RPGCommandReceiver;
import think.rpgitems.power.trigger.Trigger;

import java.lang.reflect.Method;
import java.util.HashSet;

public class I18nCommands extends RPGCommandReceiver {
    private final RPGItems plugin;

    public I18nCommands(RPGItems plugin, LanguageRepository i18n) {
        super(plugin, i18n);
        this.plugin = plugin;
    }

    @Override
    public String getHelpPrefix() {
        return "i18n";
    }

    @SubCommand(value = "properties")
    public void onProperties(CommandSender sender, Arguments arguments) {
        HashSet<String> keySet = new HashSet<>();
        PowerManager.getProperties().forEach((clazz, v) -> v.forEach((name, pair) -> {
            Method getter = pair.getKey();
            PropertyInstance propertyInstance = pair.getValue();
            PropertyHolder instance = Trigger.class.isAssignableFrom(clazz) ? Trigger.values().stream().filter(p -> p.getClass().equals(clazz)).findAny().get() : PowerManager.instantiate(clazz);

            String powerKey = "properties." + instance.getNamespacedKey().getKey() + ".main_description";
            String mainNameKay = "properties." + instance.getNamespacedKey().getKey() + ".main_name";
            String baseKey = "properties.base." + name;
            String key = "properties." + instance.getNamespacedKey().getKey() + "." + name;
            //("properties." + getName() + ".main_name");
            if (!i18n.hasKey(baseKey)) {
                if (!i18n.hasKey(key)) {
                    keySet.add(key);
                }
            }
            if (!i18n.hasKey(powerKey)) keySet.add(powerKey);
            if (!i18n.hasKey(mainNameKay)) keySet.add(mainNameKay);
        }));
//        Gson gson = new GsonBuilder().disableHtmlEscaping().create();
//        Map<String, String> keyMap = new HashMap<>();
//        keySet.forEach((k) -> keyMap.put(k, "<TO DO>"));
//        sender.sendMessage(gson.toJson(keyMap));
        keySet.forEach(sender::sendMessage);
    }
}
