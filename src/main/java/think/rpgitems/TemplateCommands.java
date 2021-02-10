package think.rpgitems;

import cat.nyaa.nyaacore.ILocalizer;
import cat.nyaa.nyaacore.LanguageRepository;
import cat.nyaa.nyaacore.Message;
import cat.nyaa.nyaacore.cmdreceiver.Arguments;
import cat.nyaa.nyaacore.cmdreceiver.BadCommandException;
import cat.nyaa.nyaacore.cmdreceiver.CommandReceiver;
import cat.nyaa.nyaacore.cmdreceiver.SubCommand;
import org.bukkit.command.CommandSender;
import org.bukkit.plugin.Plugin;
import think.rpgitems.item.ItemManager;
import think.rpgitems.item.RPGItem;
import think.rpgitems.power.*;

import java.lang.reflect.Field;
import java.util.*;

public class TemplateCommands extends RPGCommandReceiver {
    public TemplateCommands(RPGItems plugin, LanguageRepository i18n) {
        super(plugin, i18n);
    }

    @SubCommand("create")
    public void onCreate(CommandSender sender, Arguments arguments){
        String itemName = arguments.nextString();
        List<String> placeHolder = new ArrayList<>();
        while (arguments.remains() > 0){
            String s = arguments.nextString();
            placeHolder.add(s);
        }

        RPGItem rpgItem = ItemManager.getItem(itemName).orElseThrow(BadCommandException::new);
        List<String> badPlaceHolders = checkPlaceHolder(rpgItem, placeHolder);
        if (!badPlaceHolders.isEmpty()){
            badPlaceHolders.forEach(s ->{
                sendBadMsg(sender, s);
            });
            return;
        }
        rpgItem.setIsTemplate(true);
        rpgItem.setTemplatePlaceHolders(placeHolder);
        ItemManager.save(rpgItem);
        new Message("").append(I18n.getInstance(sender).format("command.template.create.success", itemName)).send(sender);
    }

    @SubCommand("delete")
    public void onDelete(CommandSender sender, Arguments arguments){
        String itemName = arguments.nextString();
        RPGItem rpgItem = ItemManager.getItem(itemName).orElseThrow(BadCommandException::new);
        rpgItem.setIsTemplate(false);
        new Message("").append(I18n.getInstance(sender).format("command.template.delete.success", itemName)).send(sender);
    }

    @SubCommand("apply")
    public void onApply(CommandSender sender, Arguments arguments){
        String itemName = arguments.nextString();
        RPGItem target = ItemManager.getItem(itemName).orElseThrow(BadCommandException::new);
        boolean isTemplate = target.isTemplate();
        I18n i18n = I18n.getInstance(sender);
        if (!isTemplate){
            new Message("").append(i18n.format("command.template.not_template", itemName)).send(sender);
            return;
        }
        Set<RPGItem> toUpdate = new HashSet<>();
        ItemManager.items().stream().filter(rpgItem -> rpgItem.isTemplateOf(itemName))
                .forEach(rpgItem -> {
                    try {
                        rpgItem.updateFromTemplate(target);
                    } catch (UnknownPowerException e) {
                        e.printStackTrace();
                        sender.sendMessage("error applying template: " + e.toString());
                    }
                    toUpdate.add(rpgItem);
                });
        toUpdate.forEach(ItemManager::save);
        new Message("").append(i18n.format("command.template.apply.success", itemName)).send(sender);
    }

    @SubCommand("placeholder")
    PlaceholderCommands placeholderCommands = new PlaceholderCommands(RPGItems.plugin, I18n.getInstance(RPGItems.plugin.cfg.language));

    public static class PlaceholderCommands extends CommandReceiver{

        /**
         * @param plugin for logging purpose only
         * @param _i18n
         */
        public PlaceholderCommands(Plugin plugin, ILocalizer _i18n) {
            super(plugin, _i18n);
        }

        @SubCommand("add")
        public void onAdd(CommandSender sender, Arguments arguments){
            String itemName = arguments.nextString();

            RPGItem rpgItem = ItemManager.getItem(itemName).orElse(null);
            if (rpgItem == null) {
                new Message("").append(I18n.getInstance(sender).format("message.item.cant.find")).send(sender);
                return;
            }

            List<String> toAdd = new ArrayList<>();
            while (arguments.remains() > 0){
                toAdd.add(arguments.nextString());
            }

            String name = rpgItem.getName();
            if (!rpgItem.isTemplate()){
                new Message("").append(I18n.getInstance(sender).format("command.template.placeholder.not_template", name)).send(sender);
                return;
            }
            List<String> badPlaceHolders = checkPlaceHolder(rpgItem, toAdd);
            if (!badPlaceHolders.isEmpty()){
                badPlaceHolders.forEach(s ->{
                    sendBadMsg(sender, s);
                });
                return;
            }
            toAdd.forEach(s -> {
                rpgItem.addTemplatePlaceHolder(s);
                new Message("").append(I18n.getInstance(sender).format("command.template.placeholder.add.success", name, s)).send(sender);
            });

        }

        @SubCommand("remove")
        public void onRemove(CommandSender sender, Arguments arguments){
            String itemName = arguments.nextString();
            String toRemove = arguments.nextString();
            RPGItem rpgItem = ItemManager.getItem(itemName).orElse(null);
            if (rpgItem == null) {
                new Message("").append(I18n.getInstance(sender).format("message.item.cant.find")).send(sender);
                return;
            }
            String name = rpgItem.getName();
            if (!rpgItem.isTemplate()){
                new Message("").append(I18n.getInstance(sender).format("command.template.placeholder.not_template", name)).send(sender);
                return;
            }
            rpgItem.removeTemplatePlaceHolder(toRemove);
            new Message("").append(I18n.getInstance(sender).format("command.template.placeholder.remove.success", name)).send(sender);

        }

        @SubCommand("list")
        public void onList(CommandSender sender, Arguments arguments){
            String itemName = arguments.nextString();
            RPGItem rpgItem = ItemManager.getItem(itemName).orElse(null);
            if (rpgItem == null) {
                new Message("").append(I18n.getInstance(sender).format("message.item.cant.find")).send(sender);
                return;
            }
            String name = rpgItem.getName();
            if (!rpgItem.isTemplate()){
                new Message("").append(I18n.getInstance(sender).format("command.template.placeholder.not_template", name)).send(sender);
                return;
            }
            new Message("").append(I18n.getInstance(sender).format("command.template.placeholder.itemName", name)).send(sender);
            rpgItem.getTemplatePlaceholders().forEach(placeholderHolder -> {
                new Message("").append(I18n.getInstance(sender).format("command.template.placeholder.info", placeholderHolder)).send(sender);
            });
        }

        @Override
        public String getHelpPrefix() {
            return null;
        }
    }


    private static void sendBadMsg(CommandSender sender, String s) {
        I18n i18n = I18n.getInstance(sender);
        new Message("").append(i18n.format("command.template.bad_placeholder", s));
    }

    /**
     * check syntax of placeholders
     * &lt;placeholderId:propName&gt;
     * @param rpgItem
     * @param placeHolder
     * @return bad place holders
     */
    private static List<String> checkPlaceHolder(RPGItem rpgItem, List<String> placeHolder) {
        List<String> ret = new ArrayList<>();
        placeHolder.forEach(s ->{
            try{
                String[] split = s.split(":", 3);
                String powerid;
                String propName;
                if (split.length != 2){
                    ret.add(s);
                    return;
                }
                powerid = split[0];
                propName = split[1];
                PlaceholderHolder power = rpgItem.getPlaceholderHolder(powerid);
                Class<?> aClass = power.getClass();
                while (aClass != null){
                    Field declaredField;
                    try{
                        declaredField = aClass.getDeclaredField(propName);
                    }catch (Exception e){
                        declaredField = null;
                    }
                    //if found field with name [propName] and this field has @Property
                    //it's a valid placeholder
                    if (declaredField != null && declaredField.getAnnotation(Property.class) != null){
                        return;
                    }
                    aClass = aClass.getSuperclass();
                }
                ret.add(s);
            } catch (Exception e){
                ret.add(s);
            }
        });
        return ret;
    }

    public static void msgs(CommandSender target, String template, Object... args) {
        I18n i18n = I18n.getInstance(target);
        target.sendMessage(i18n.getFormatted(template, args));
    }

    @Override
    public String getHelpPrefix() {
        return null;
    }
}
