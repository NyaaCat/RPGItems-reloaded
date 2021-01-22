package think.rpgitems;

import cat.nyaa.nyaacore.LanguageRepository;
import cat.nyaa.nyaacore.Message;
import cat.nyaa.nyaacore.cmdreceiver.Arguments;
import cat.nyaa.nyaacore.cmdreceiver.BadCommandException;
import cat.nyaa.nyaacore.cmdreceiver.SubCommand;
import org.bukkit.command.CommandSender;
import think.rpgitems.item.ItemManager;
import think.rpgitems.item.RPGItem;
import think.rpgitems.power.PlaceholderHolder;
import think.rpgitems.power.Property;
import think.rpgitems.power.RPGCommandReceiver;

import java.lang.reflect.Field;
import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;
import java.util.Set;

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
            new Message("").append(i18n.format("command.template.not_template")).send(sender);
            return;
        }
        Set<RPGItem> toUpdate = new HashSet<>();
        ItemManager.items().stream().filter(rpgItem -> rpgItem.isTemplateOf(itemName))
                .forEach(rpgItem -> {
                    rpgItem.updateFromTemplate(target);
                    toUpdate.add(rpgItem);
                });
        toUpdate.forEach(ItemManager::save);
    }


    private void sendBadMsg(CommandSender sender, String s) {
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
    private List<String> checkPlaceHolder(RPGItem rpgItem, List<String> placeHolder) {
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
