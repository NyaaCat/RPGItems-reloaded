/*
 *  This file is part of RPG Items.
 *
 *  RPG Items is free software: you can redistribute it and/or modify
 *  it under the terms of the GNU General Public License as published by
 *  the Free Software Foundation, either version 3 of the License, or
 *  (at your option) any later version.
 *
 *  RPG Items is distributed in the hope that it will be useful,
 *  but WITHOUT ANY WARRANTY; without even the implied warranty of
 *  MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
 *  GNU General Public License for more details.
 *
 *  You should have received a copy of the GNU General Public License
 *  along with RPG Items.  If not, see <http://www.gnu.org/licenses/>.
 */
package think.rpgitems.commands;

import gnu.trove.map.hash.TCharObjectHashMap;

import java.io.BufferedReader;
import java.io.BufferedWriter;
import java.io.File;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.InputStreamReader;
import java.io.OutputStreamWriter;
import java.io.UnsupportedEncodingException;
import java.lang.reflect.InvocationTargetException;
import java.lang.reflect.Method;
import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Calendar;
import java.util.Collection;
import java.util.Collections;
import java.util.Date;
import java.util.HashMap;
import java.util.List;
import java.util.Map.Entry;
import java.util.SimpleTimeZone;

import org.bukkit.Bukkit;
import org.bukkit.ChatColor;
import org.bukkit.Material;
import org.bukkit.command.CommandSender;
import org.bukkit.entity.Player;
import org.bukkit.potion.PotionEffectType;

import think.rpgitems.Plugin;
import think.rpgitems.data.Locale;
import think.rpgitems.item.ItemManager;
import think.rpgitems.item.RPGItem;

abstract public class Commands {
    private static HashMap<String, ArrayList<CommandDef>> commands = new HashMap<String, ArrayList<CommandDef>>();
    private static TCharObjectHashMap<Class<? extends CommandArgument>> argTypes = new TCharObjectHashMap<Class<? extends CommandArgument>>();

    static {
        argTypes.put('s', ArgumentString.class);
        argTypes.put('i', ArgumentInteger.class);
        argTypes.put('f', ArgumentDouble.class);
        argTypes.put('p', ArgumentPlayer.class);
        argTypes.put('o', ArgumentOption.class);
        argTypes.put('n', ArgumentItem.class);
        argTypes.put('m', ArgumentMaterial.class);
        argTypes.put('e', ArgumentEnum.class);
    }

    public static void exec(CommandSender sender, String com) {
        com = com.trim();
        if (com.length() == 0)
            return;
        String comName;
        int pos = com.indexOf(' ');
        if (pos == -1) {
            comName = com;
        } else {
            comName = com.substring(0, pos);
        }
        com = com.substring(pos + 1);

        String locale = sender instanceof Player ? Locale.getPlayerLocale((Player)sender) : "en_GB";
        
        ArrayList<CommandDef> command = commands.get(comName);
        if (command == null) {
            sender.sendMessage(ChatColor.RED + String.format(Locale.get("message.error.unknown.command", locale), comName));
            return;
        }

        if (pos == -1) {
            for (CommandDef c : command) {
                if (c.arguments.length == 0) {
                    try {
                        if (c.handlePermissions || sender.hasPermission("rpgitem"))
                            c.method.invoke(c.handler, sender);
                        else
                            sender.sendMessage(ChatColor.RED + Locale.get("message.error.permission", locale));
                    } catch (IllegalAccessException e) {
                        // TODO Auto-generated catch block
                        e.printStackTrace();
                    } catch (IllegalArgumentException e) {
                        // TODO Auto-generated catch block
                        e.printStackTrace();
                    } catch (InvocationTargetException e) {
                        // TODO Auto-generated catch block
                        e.printStackTrace();
                    }
                    return;
                }
            }
            // Print usage
            if (sender.hasPermission("rpgitem")) {
                sender.sendMessage(String.format(ChatColor.GREEN + Locale.get("message.command.usage", locale), comName, Plugin.plugin.getDescription().getVersion()));
                for (CommandDef c : command) {
                    StringBuilder buf = new StringBuilder();
                    buf.append(ChatColor.GREEN).append('/').append(comName);
                    for (CommandArgument a : c.arguments) {
                        buf.append(' ');
                        if (a.name.length() != 0) {
                            buf.append(ChatColor.RED);
                            buf.append(Locale.get("command.info." + a.name, locale));
                        }
                        buf.append(a.isConst() ? ChatColor.GREEN : ChatColor.GOLD);
                        buf.append(a.printable(locale));
                    }
                    sender.sendMessage(buf.toString());
                }
                sender.sendMessage(ChatColor.GREEN + Locale.get("message.command.info", locale));
            } else
                sender.sendMessage(ChatColor.RED + Locale.get("message.error.permission", locale));
            return;
        }
        ArrayList<String> args = new ArrayList<String>();
        while (true) {
            int end;
            if (com.length() == 0) {
                break;
            }
            boolean quote = false;
            if (com.charAt(0) == '`') {
                com = com.substring(1);
                end = com.indexOf('`');
                quote = true;
            } else {
                end = com.indexOf(' ');
            }
            if (end == -1) {
                args.add(com);
            } else {
                args.add(com.substring(0, end));
            }
            if (quote) {
                com = com.substring(end + 1);
                end = com.indexOf(' ');
            }
            if (end != -1) {
                com = com.substring(end + 1);
            } else {
                break;
            }
        }
        CommandError lastError = null;
        comLoop: for (CommandDef c : command) {
            if (c.arguments.length != args.size()) {
                if (c.arguments.length != 0 && c.arguments[c.arguments.length - 1] instanceof ArgumentString) {
                    if (args.size() < c.arguments.length)
                        continue;
                } else {
                    continue;
                }
            }
            ArrayList<Object> outArgs = new ArrayList<Object>();
            outArgs.add(sender);
            for (int i = 0; i < c.arguments.length; i++) {
                CommandArgument a = c.arguments[i];
                if (!a.isConst()) {
                    if (i == c.arguments.length - 1) {
                        // Special case for strings so they do not need to be quoted
                        if (a instanceof ArgumentString) {
                            StringBuilder joined = new StringBuilder();
                            for (int j = i; j < args.size(); j++) {
                                joined.append(args.get(j)).append(' ');
                            }
                            args.set(i, joined.toString().trim());
                        }
                    }
                    Object res = a.parse(args.get(i), locale);
                    if (res instanceof CommandError) {
                        lastError = (CommandError) res;
                        continue comLoop;
                    }
                    outArgs.add(res);
                } else {
                    ArgumentConst cst = (ArgumentConst) a;
                    if (!cst.value.equals(args.get(i))) {
                        continue comLoop;
                    }
                }
            }
            try {
                if (c.handlePermissions || sender.hasPermission("rpgitem"))
                    c.method.invoke(c.handler, outArgs.toArray());
                else
                    sender.sendMessage(ChatColor.RED + Locale.get("message.error.permission", locale));
            } catch (IllegalAccessException e) {
                // TODO Auto-generated catch block
                e.printStackTrace();
            } catch (IllegalArgumentException e) {
                // TODO Auto-generated catch block
                e.printStackTrace();
            } catch (InvocationTargetException e) {
                // TODO Auto-generated catch block
                e.printStackTrace();
            }
            return;
        }
        if (sender.hasPermission("rpgitem")) {
            if (lastError != null) {
                sender.sendMessage(ChatColor.RED + String.format(Locale.get("message.error.command", locale), lastError.error));
            } else {
                ArrayList<String> consts = new ArrayList<String>();
                comLoop: for (CommandDef c : command) {
                    for (int i = 0; i < c.arguments.length; i++) {
                        if (i >= args.size())
                            break;
                        CommandArgument a = c.arguments[i];
                        if (!a.isConst()) {
                            if (i == c.arguments.length - 1) {
                                // Special case for strings so they do not need to be quoted
                                if (a instanceof ArgumentString) {
                                    StringBuilder joined = new StringBuilder();
                                    for (int j = i; j < args.size(); j++) {
                                        joined.append(args.get(j)).append(' ');
                                    }
                                    args.set(i, joined.toString().trim());
                                }
                            }
                            Object res = a.parse(args.get(i), locale);
                            if (res instanceof CommandError) {
                                lastError = (CommandError) res;
                                continue comLoop;
                            }
                        } else {
                            ArgumentConst cst = (ArgumentConst) a;
                            if (!cst.value.equals(args.get(i))) {
                                continue comLoop;
                            } else {
                                consts.add(cst.value);
                            }
                        }
                    }
                }
                StringBuilder search = new StringBuilder();
                for (String term : consts) {
                    search.append(term).append(' ');
                }
                searchHelp(sender, search.toString());
            }
        } else
            sender.sendMessage(ChatColor.RED + Locale.get("message.error.permission", locale));
    }

    public static List<String> complete(CommandSender sender, String com) {
        com = com.trim();
        if (com.length() == 0) {
            return new ArrayList<String>();
        }
        String comName;
        int pos = com.indexOf(' ');
        if (pos == -1) {
            comName = com;
        } else {
            comName = com.substring(0, pos);
        }
        com = com.substring(pos + 1);

        String locale = sender instanceof Player ? Locale.getPlayerLocale((Player)sender) : "en_GB";
        
        ArrayList<CommandDef> command = commands.get(comName);

        if (command == null) {
            if (pos == -1) {
                ArrayList<String> out = new ArrayList<String>();
                for (String n : commands.keySet()) {
                    if (n.startsWith(comName)) {
                        out.add("/" + n);
                    }
                }
                return out;
            }
            return new ArrayList<String>();
        }
        ArrayList<String> args = new ArrayList<String>();
        while (true) {
            int end;
            if (com.length() == 0) {
                break;
            }
            boolean quote = false;
            if (com.charAt(0) == '`') {
                com = com.substring(1);
                end = com.indexOf('`');
                quote = true;
            } else {
                end = com.indexOf(' ');
            }
            if (end == -1) {
                args.add(com);
            } else {
                args.add(com.substring(0, end));
            }
            if (quote) {
                com = com.substring(end + 1);
                end = com.indexOf(' ');
            }
            if (end != -1) {
                com = com.substring(end + 1);
            } else {
                break;
            }
        }
        HashMap<String, Boolean> out = new HashMap<String, Boolean>();

        comLoop: for (CommandDef c : command) {
            for (int i = 0; i < c.arguments.length; i++) {
                CommandArgument a = c.arguments[i];
                if (i == args.size() - 1) {
                    List<String> res = a.tabComplete(args.get(i));
                    if (res != null) {
                        for (String s : res) {
                            out.put(s, true);
                        }
                        continue comLoop;
                    }
                } else {
                    if (!a.isConst()) {
                        Object res = a.parse(args.get(i), locale);
                        if (res instanceof CommandError) {
                            continue comLoop;
                        }
                    } else {
                        ArgumentConst cst = (ArgumentConst) a;
                        if (!cst.value.equals(args.get(i))) {
                            continue comLoop;
                        }
                    }
                }
            }
        }
        ArrayList<String> outList = new ArrayList<String>();
        for (String s : out.keySet()) {
            outList.add(s);
        }
        return outList;
    }

    public static void register(CommandHandler handler) {
        Method[] methods = handler.getClass().getMethods();
        for (Method method : methods) {
            Class<?>[] params = method.getParameterTypes();
            CommandString comString = method.getAnnotation(CommandString.class);
            if (comString == null) {
                continue;
            }
            if (params.length == 0 || !params[0].isAssignableFrom(CommandSender.class)) {
                throw new RuntimeException("First argument must be CommandSender @ " + method.getName());
            }
            add(comString.value(), method, handler);
        }
        Collection<ArrayList<CommandDef>> coms = commands.values();
        for (ArrayList<CommandDef> c : coms) {
            Collections.sort(c);
        }
    }

    private static void add(String com, Method method, CommandHandler handler) {
        com = com.trim();
        int pos = com.indexOf(' ');
        String comName;
        if (pos == -1) {
            comName = com;
        } else {
            comName = com.substring(0, pos);
        }

        CommandDef def = new CommandDef();
        def.commandString = com;
        def.method = method;
        def.handler = handler;
        Class<?>[] params = method.getParameterTypes();
        if (method.isAnnotationPresent(CommandDocumentation.class)) {
            def.documentation = method.getAnnotation(CommandDocumentation.class).value();
        } else {
            def.documentation = "";
        }
        if (method.isAnnotationPresent(CommandGroup.class)) {
            def.sortKey = method.getAnnotation(CommandGroup.class).value();
        } else {
            def.sortKey = "";
        }
        CommandString comString = method.getAnnotation(CommandString.class);
        def.handlePermissions = comString.handlePermissions();

        if (!commands.containsKey(comName)) {
            commands.put(comName, new ArrayList<CommandDef>());
        }
        commands.get(comName).add(def);
        if (pos == -1) {
            def.arguments = new CommandArgument[0];
            return;
        }
        com = com.substring(pos + 1);
        ArrayList<CommandArgument> arguments = new ArrayList<CommandArgument>();
        int realArgumentsCount = 0;
        while (true) {
            pos = com.indexOf(' ');
            String a;
            if (pos == -1) {
                a = com;
            } else {
                a = com.substring(0, pos);
                com = com.substring(pos + 1);
            }
            if (a.charAt(0) == '$') { // Variable
                String name = "";
                if (a.contains(":")) {
                    String[] as = a.split(":");
                    name = as[0].substring(1);
                    a = "$" + as[1];
                }
                char t = a.charAt(1);
                Class<? extends CommandArgument> cAT = argTypes.get(t);
                if (cAT == null) {
                    throw new RuntimeException("Invalid command argument type " + t);
                }
                CommandArgument arg;
                try {
                    arg = cAT.newInstance();
                    arg.init(a.substring(3, a.length() - 1));
                    if (!params[realArgumentsCount + 1].isAssignableFrom(arg.getType())) {
                        throw new RuntimeException("Type mismatch for " + method.getName());
                    }
                    arg.name = name;
                    arguments.add(arg);
                    realArgumentsCount++;
                } catch (Exception e) {
                    e.printStackTrace();
                }
            } else { // Const
                arguments.add(new ArgumentConst(a));
            }
            if (pos == -1) {
                break;
            }
        }
        if (params.length != realArgumentsCount + 1) {
            throw new RuntimeException("Argument count mis-match for " + method.getName());
        }
        def.arguments = new CommandArgument[arguments.size()];
        arguments.toArray(def.arguments);
    }

    static {
        register(new CommandHandler() {

            @CommandString("rpgitem help $terms:s[]")
            @CommandDocumentation("$command.rpgitem.help")
            @CommandGroup("help")
            public void help(CommandSender sender, String query) {
                searchHelp(sender, query);
            }
        });
    }

    public static void searchHelp(CommandSender sender, String terms) {
        if (terms.equalsIgnoreCase("_genhelp")) {
            for (String locale : Locale.getLocales()) {
                generateHelp(locale);
            }
            return;
        }
        String locale = sender instanceof Player ? Locale.getPlayerLocale((Player) sender) : "en_GB";
        sender.sendMessage(ChatColor.GREEN + String.format(Locale.get("message.help.for", locale), terms));
        String[] term = terms.toLowerCase().split(" ");
        for (Entry<String, ArrayList<CommandDef>> command : commands.entrySet()) {
            for (CommandDef c : command.getValue()) {
                int count = 0;
                for (String t : term) {
                    if (c.commandString.toLowerCase().contains(t)) {
                        count++;
                    }
                }
                if (count == term.length) {
                    StringBuilder buf = new StringBuilder();
                    buf.append(ChatColor.GREEN).append(ChatColor.BOLD).append('/').append(command.getKey());
                    for (CommandArgument a : c.arguments) {
                        buf.append(' ');
                        if (a.name.length() != 0) {
                            buf.append(ChatColor.RED).append(ChatColor.BOLD);
                            buf.append(Locale.get("command.info." + a.name, locale));
                        }
                        buf.append(a.isConst() ? ChatColor.GREEN : ChatColor.GOLD).append(ChatColor.BOLD);
                        buf.append(a.printable(locale));
                    }
                    sender.sendMessage(buf.toString());
                    String docStr = c.documentation;
                    if (docStr.charAt(0) == '$') {
                        if (docStr.contains("+")) {
                            String[] dArgs = docStr.split("\\+");
                            docStr = Locale.get(dArgs[0].substring(1), locale);
                            if (dArgs[1].equalsIgnoreCase("PotionEffectType")) {
                                StringBuilder out = new StringBuilder();
                                for (PotionEffectType type : PotionEffectType.values()) {
                                    if (type != null)
                                        out.append(type.getName().toLowerCase()).append(", ");
                                }
                                docStr += out.toString();
                            }
                        } else {
                            docStr = Locale.get(docStr.substring(1), locale);
                        }
                    }
                    docStr = docStr.replaceAll("@", "" + ChatColor.BLUE).replaceAll("#", "" + ChatColor.WHITE);
                    
                    StringBuilder docBuf = new StringBuilder();
                    char[] chars = docStr.toCharArray();
                    docBuf.append(ChatColor.WHITE);
                    for (int i = 0; i < chars.length; i++) {
                        char l = chars[i];
                        if (l == '&') {
                            i++;
                            l = chars[i];
                            switch (l) {
                            case '0':
                                docBuf.append(ChatColor.BLACK);
                                break;
                            case '1':
                                docBuf.append(ChatColor.DARK_BLUE);
                                break;
                            case '2':
                                docBuf.append(ChatColor.DARK_GREEN);
                                break;
                            case '3':
                                docBuf.append(ChatColor.DARK_AQUA);
                                break;
                            case '4':
                                docBuf.append(ChatColor.DARK_RED);
                                break;
                            case '5':
                                docBuf.append(ChatColor.DARK_PURPLE);
                                break;
                            case '6':
                                docBuf.append(ChatColor.GOLD);
                                break;
                            case '7':
                                docBuf.append(ChatColor.GRAY);
                                break;
                            case '8':
                                docBuf.append(ChatColor.DARK_GRAY);
                                break;
                            case '9':
                                docBuf.append(ChatColor.BLUE);
                                break;
                            case 'a':
                                docBuf.append(ChatColor.GREEN);
                                break;
                            case 'b':
                                docBuf.append(ChatColor.AQUA);
                                break;
                            case 'c':
                                docBuf.append(ChatColor.RED);
                                break;
                            case 'd':
                                docBuf.append(ChatColor.LIGHT_PURPLE);
                                break;
                            case 'e':
                                docBuf.append(ChatColor.YELLOW);
                                break;
                            case 'f':
                                docBuf.append(ChatColor.WHITE);
                                break;
                            case 'r':
                                docBuf.append(ChatColor.WHITE);
                                break;
                            }
                        } else {
                            docBuf.append(l);
                        }
                    }
                    sender.sendMessage(docBuf.toString());
                }
            }
        }
    }

    private static HashMap<String, String> getMap() {
        HashMap<String, String> langMap = new HashMap<String, String>();
        langMap.put("en_US", "English (US)");
        BufferedReader r = null;
        try {
            r = new BufferedReader(new InputStreamReader(Plugin.plugin.getResource("languages.txt"), "UTF-8"));
            String line = null;
            while ((line = r.readLine()) != null) {
                String []args = line.split("=");
                langMap.put(args[0], args[1]);
            }
        } catch (UnsupportedEncodingException e) {
            e.printStackTrace();
        } catch (IOException e) {
            // TODO Auto-generated catch block
            e.printStackTrace();
        } finally {
            try {
                r.close();
            } catch (IOException e) {
                // TODO Auto-generated catch block
                e.printStackTrace();
            }
        }
        return langMap;        
    }
    
    public static void generateHelp(String locale) {
        BufferedWriter w = null;
        
        HashMap<String, String> langMap = getMap();
        
        try {
            File out = new File(Plugin.plugin.getDataFolder(), Calendar.getInstance().get(Calendar.YEAR) + "-" + Calendar.getInstance().get(Calendar.MONTH) + "-" + Calendar.getInstance().get(Calendar.DAY_OF_MONTH) + "-" + locale + ".md");
            if (out.exists()) {
                out.delete();
            }
            w = new BufferedWriter(new OutputStreamWriter(new FileOutputStream(out), "UTF-8"));
            w.write("---\n");
            w.write("layout: locale\n");
            w.write("title: " + langMap.get(locale) + "\n");
            w.write("permalink: " + locale + ".html\n");
            w.write("---\n");
            for (Entry<String, ArrayList<CommandDef>> command : commands.entrySet()) {
                w.write(String.format("## Commands /%s ", command.getKey()));
                w.write("\n\n");
                for (CommandDef c : command.getValue()) {
                    StringBuilder buf = new StringBuilder();
                    buf.append("### /");
                    buf.append(command.getKey()).append(" ");
                    for (CommandArgument a : c.arguments) {
                        if (a.name.length() != 0) {
                            buf.append("<span style='color:#006EFF'>");
                            buf.append(Locale.get("command.info." + a.name, locale));
                            buf.append("</span>");
                        }
                        if (a.isConst())
                            buf.append("<span style='color:#b5e853'>");
                        else
                            buf.append("<span style='color:#1BE0BF'>");
                        buf.append(a.printable(locale));
                        buf.append("</span> ");
                    }
                    buf.append("\n");
                    String docStr = c.documentation;
                    if (docStr.charAt(0) == '$') {
                        if (docStr.contains("+")) {
                            String[] dArgs = docStr.split("\\+");
                            docStr = Locale.get(dArgs[0].substring(1), locale);
                            if (dArgs[1].equalsIgnoreCase("PotionEffectType")) {
                                StringBuilder out2 = new StringBuilder();
                                for (PotionEffectType type : PotionEffectType.values()) {
                                    if (type != null)
                                        out2.append(type.getName().toLowerCase()).append(", ");
                                }
                                docStr += out2.toString();
                            }
                        } else {
                            docStr = Locale.get(docStr.substring(1), locale);
                        }
                    }
                    docStr = docStr.replaceAll("#", "</span>").replaceAll("@", "<span style='color:#0000ff'>").replaceAll("`", "`` ` ``");
                    StringBuilder docBuf = new StringBuilder();
                    char[] chars = docStr.toCharArray();
                    for (int i = 0; i < chars.length; i++) {
                        char l = chars[i];
                        if (l == '&') {
                            i++;
                            l = chars[i];
                            if (l != 'r') {
                                docBuf.append("<span style='color:#");
                            }
                            switch (l) {
                            case '0':
                                docBuf.append("000000");
                                break;
                            case '1':
                                docBuf.append("0000aa");
                                break;
                            case '2':
                                docBuf.append("00aa00");
                                break;
                            case '3':
                                docBuf.append("00aaaa");
                                break;
                            case '4':
                                docBuf.append("aa0000");
                                break;
                            case '5':
                                docBuf.append("aa00aa");
                                break;
                            case '6':
                                docBuf.append("ffaa00");
                                break;
                            case '7':
                                docBuf.append("aaaaaa");
                                break;
                            case '8':
                                docBuf.append("555555");
                                break;
                            case '9':
                                docBuf.append("5555ff");
                                break;
                            case 'a':
                                docBuf.append("55ff55");
                                break;
                            case 'b':
                                docBuf.append("55ffff");
                                break;
                            case 'c':
                                docBuf.append("ff5555");
                                break;
                            case 'd':
                                docBuf.append("ff55ff");
                                break;
                            case 'e':
                                docBuf.append("ffff55");
                                break;
                            case 'f':
                                docBuf.append("ffffff");
                                break;
                            case 'r':
                                docBuf.append("'></span'");
                                break;
                            }
                            docBuf.append("'>");
                        } else {
                            docBuf.append(l);
                        }
                    }
                    buf.append(docBuf.toString());
                    buf.append("\n\n");
                    w.write(buf.toString());
                }
            }
            w.write("\n\n");
            w.write("Generated at: ");
            SimpleDateFormat sdf = new SimpleDateFormat();
            sdf.setTimeZone(new SimpleTimeZone(0, "GMT"));
            sdf.applyPattern("dd MMM yyyy HH:mm:ss z");
            w.write(sdf.format(new Date()));

        } catch (IOException e) {
            e.printStackTrace();
        } finally {
            try {
                w.close();
            } catch (IOException e) {
                e.printStackTrace();
            }
        }
    }
}

class CommandDef implements Comparable<CommandDef> {
    public boolean handlePermissions;
    public String commandString;
    public CommandHandler handler;
    public Method method;
    public CommandArgument[] arguments;
    public String documentation;
    public String sortKey;

    @Override
    public int compareTo(CommandDef o) {
        return sortKey.compareToIgnoreCase(o.sortKey);
    }
}

abstract class CommandArgument {
    public abstract void init(String a);

    public abstract Object parse(String in, String locale);

    public abstract List<String> tabComplete(String in);

    public abstract String printable(String locale);

    public abstract Class<?> getType();

    public String name = "";

    public boolean isConst() {
        return false;
    }
}

class CommandError {

    public String error;

    public CommandError(String error) {
        this.error = error;
    }
}

class ArgumentInteger extends CommandArgument {

    private boolean hasLimits;
    private int min;
    private int max;

    @Override
    public void init(String a) {
        if (a.length() == 0) {
            hasLimits = false;
        } else {
            hasLimits = true;
            String[] args = a.split(",");
            if (args.length != 2) {
                throw new RuntimeException("ArgumentInteger limits errror");
            }
            min = Integer.parseInt(args[0]);
            max = Integer.parseInt(args[1]);
        }
    }

    @Override
    public Object parse(String in, String locale) {
        if (hasLimits) {
            try {
                int i = Integer.parseInt(in);
                if (i < min || i > max) {
                    return new CommandError(String.format(Locale.get("message.error.integer.limit", locale), min, max));
                }
                return i;
            } catch (NumberFormatException e) {
                return new CommandError(String.format(Locale.get("message.error.integer.format", locale), in));
            }
        } else {
            try {
                int i = Integer.parseInt(in);
                return i;
            } catch (NumberFormatException e) {
                return new CommandError(String.format(Locale.get("message.error.integer.format", locale), in));
            }
        }
    }

    @Override
    public List<String> tabComplete(String in) {
        return new ArrayList<String>();
    }

    @Override
    public String printable(String locale) {
        if (hasLimits) {
            return String.format(Locale.get("command.info.integer.limit", locale), min, max);
        }
        return Locale.get("command.info.integer", locale);
    }

    @Override
    public Class<?> getType() {
        return int.class;
    }

}

class ArgumentDouble extends CommandArgument {

    private boolean hasLimits;
    private double min;
    private double max;

    @Override
    public void init(String a) {
        if (a.length() == 0) {
            hasLimits = false;
        } else {
            hasLimits = true;
            String[] args = a.split(",");
            if (args.length != 2) {
                throw new RuntimeException("ArgumentDouble limits errror");
            }
            min = Double.parseDouble(args[0]);
            max = Double.parseDouble(args[1]);
        }
    }

    @Override
    public Object parse(String in, String locale) {
        if (hasLimits) {
            try {
                double i = Double.parseDouble(in);
                if (i < min || i > max) {
                    return new CommandError(String.format(Locale.get("message.error.double.limit", locale), min, max));
                }
                return i;
            } catch (NumberFormatException e) {
                return new CommandError(String.format(Locale.get("message.error.double.format", locale), in));
            }
        } else {
            try {
                double i = Double.parseDouble(in);
                return i;
            } catch (NumberFormatException e) {
                return new CommandError(String.format(Locale.get("message.error.double.format", locale), in));
            }
        }
    }

    @Override
    public List<String> tabComplete(String in) {
        return new ArrayList<String>();
    }

    @Override
    public String printable(String locale) {
        if (hasLimits) {
            return String.format(Locale.get("command.info.double.limit", locale), min, max);
        }
        return Locale.get("command.info.double", locale);
    }

    @Override
    public Class<?> getType() {
        return double.class;
    }

}

class ArgumentString extends CommandArgument {

    private int maxLength;

    @Override
    public void init(String a) {
        if (a.length() == 0) {
            maxLength = 0;
        } else {
            maxLength = Integer.parseInt(a);
        }
    }

    @Override
    public Object parse(String in, String locale) {
        if (maxLength != 0 && in.length() > maxLength)
            return new CommandError(String.format(Locale.get("message.error.string.length", locale), in, maxLength));
        return in;
    }

    @Override
    public List<String> tabComplete(String in) {
        return new ArrayList<String>();
    }

    @Override
    public String printable(String locale) {
        if (maxLength != 0)
            return String.format(Locale.get("command.info.string.limit", locale), maxLength);
        return Locale.get("command.info.string", locale);
    }

    @Override
    public Class<?> getType() {
        return String.class;
    }

}

class ArgumentConst extends CommandArgument {

    public String value;

    public ArgumentConst(String v) {
        value = v;
    }

    @Override
    public void init(String a) {
        throw new RuntimeException("Const cannot be init'ed");
    }

    @Override
    public Object parse(String in, String locale) {
        return null;
    }

    @Override
    public List<String> tabComplete(String in) {
        ArrayList<String> a = new ArrayList<String>();
        String lValue = value;
        if (lValue.startsWith(in))
            a.add(lValue);
        return a;
    }

    @Override
    public String printable(String locale) {
        return value;
    }

    @Override
    public boolean isConst() {
        return true;
    }

    @Override
    public Class<?> getType() {
        return null;
    }
}

class ArgumentPlayer extends CommandArgument {

    @Override
    public void init(String a) {
    }

    @Override
    public Object parse(String in, String locale) {
        Player player = Bukkit.getPlayer(in);
        if (player == null)
            return new CommandError(String.format(Locale.get("message.error.player", locale), in));
        return player;
    }

    @Override
    public List<String> tabComplete(String in) {
        List<Player> players = Bukkit.matchPlayer(in);
        ArrayList<String> out = new ArrayList<String>();
        for (Player player : players) {
            out.add(player.getName());
        }
        return out;
    }

    @Override
    public String printable(String locale) {
        return Locale.get("command.info.player", locale);
    }

    @Override
    public Class<?> getType() {
        return Player.class;
    }

}

class ArgumentOption extends CommandArgument {

    private String[] options;
    private String shortVersion = "";

    @Override
    public void init(String a) {
        if (a.contains("@")) {
            String[] args = a.split("@");
            shortVersion = args[0];
            a = args[1];

        }
        options = a.split(",");
        for (int i = 0; i < options.length; i++) {
            options[i] = options[i].trim();
        }
    }

    @Override
    public Object parse(String in, String locale) {
        for (String o : options) {
            if (o.equalsIgnoreCase(in)) {
                return o;
            }
        }
        return new CommandError(String.format(Locale.get("message.error.option", locale), in));
    }

    @Override
    public List<String> tabComplete(String in) {
        ArrayList<String> out = new ArrayList<String>();
        in = in.toLowerCase();
        for (String o : options) {
            if (o.startsWith(in)) {
                out.add(o);
            }
        }
        return out;
    }

    @Override
    public String printable(String locale) {
        if (shortVersion.length() == 0) {
            StringBuilder out = new StringBuilder();
            out.append('[');
            for (int i = 0; i < options.length; i++) {
                out.append(options[i]).append(i == options.length - 1 ? ']' : ',');
            }
            return out.toString();
        } else {
            return "[" + shortVersion + "]";
        }
    }

    @Override
    public Class<?> getType() {
        return String.class;
    }

}

class ArgumentItem extends CommandArgument {

    @Override
    public void init(String a) {

    }

    @Override
    public Object parse(String in, String locale) {
        in = in.toLowerCase();
        RPGItem item = ItemManager.getItemByName(in);
        if (item == null) {
            return new CommandError(String.format(Locale.get("message.error.item", locale), in));
        }
        return item;
    }

    @Override
    public List<String> tabComplete(String in) {
        in = in.toLowerCase();
        ArrayList<String> out = new ArrayList<String>();
        for (String i : ItemManager.itemByName.keySet()) {
            if (i.startsWith(in)) {
                out.add(i);
            }
        }
        return out;
    }

    @Override
    public String printable(String locale) {
        return Locale.get("command.info.item", locale);
    }

    @Override
    public Class<?> getType() {
        return RPGItem.class;
    }

}

class ArgumentMaterial extends CommandArgument {

    @Override
    public void init(String a) {

    }

    @Override
    public Object parse(String in, String locale) {
        Material mat = Material.matchMaterial(in);
        if (mat == null) {
            return new CommandError(String.format(Locale.get("message.error.material", locale), in));
        }
        return mat;
    }

    @Override
    public List<String> tabComplete(String in) {
        ArrayList<String> out = new ArrayList<String>();
        String it = in.toUpperCase();
        for (Material m : Material.values()) {
            if (m.toString().startsWith(it)) {
                out.add(m.toString());
            }
        }
        return out;
    }

    @Override
    public String printable(String locale) {
        return Locale.get("command.info.material", locale);
    }

    @Override
    public Class<?> getType() {
        return Material.class;
    }

}

class ArgumentEnum extends CommandArgument {

    private Class<?> e;
    private List<?> enumConsts;

    @Override
    public void init(String a) {
        try {
           e = Class.forName(a);
           if (!e.isEnum()) {
               throw new RuntimeException(a + " is not an enum");
           }
           enumConsts = Arrays.asList(e.getEnumConstants());
        } catch (ClassNotFoundException e) {
            throw new RuntimeException(e);
        }
    }

    @SuppressWarnings({ "unchecked", "rawtypes" })
    @Override
    public Object parse(String in, String locale) {
        Enum<?> en = null;
        try {
            en = Enum.valueOf((Class<Enum>)e, in.toUpperCase());
        } catch (IllegalArgumentException ex) {
            return new CommandError(String.format("%s is not a %s", in, e.getSimpleName()));
        }
        return en;
    }

    @Override
    public List<String> tabComplete(String in) {
        ArrayList<String> out = new ArrayList<String>();
        String it = in.toUpperCase();
        for (Object en : enumConsts) {
            if (en.toString().startsWith(it)) {
                out.add(en.toString());
            }
        }
        return out;
    }

    @Override
    public String printable(String locale) {
        return "[" + e.getSimpleName() + "]";
    }

    @Override
    public Class<?> getType() {
        return e;
    }

}