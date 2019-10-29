package think.rpgitems;

import cat.nyaa.nyaacore.LanguageRepository;
import cat.nyaa.nyaacore.Pair;
import cat.nyaa.nyaacore.cmdreceiver.Arguments;
import cat.nyaa.nyaacore.cmdreceiver.SubCommand;
import org.bukkit.NamespacedKey;
import org.bukkit.command.CommandSender;
import think.rpgitems.power.*;
import think.rpgitems.power.trigger.Trigger;

import java.io.*;
import java.lang.reflect.Method;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.*;
import java.util.logging.Level;
import java.util.stream.Collectors;
import java.util.stream.Stream;

public class WikiCommand extends RPGCommandReceiver {

    public WikiCommand(RPGItems plugin, LanguageRepository i18n) {
        super(plugin, i18n);
    }

    @SuppressWarnings("ConstantConditions")
    @SubCommand(isDefaultCommand = true)
    public void genWiki(CommandSender sender, Arguments args) throws IOException {
        String lc = args.next();
        Locale locale = Locale.forLanguageTag((lc == null ? RPGItems.plugin.cfg.language : lc).replace('_', '-'));
        File wikiDir = new File(RPGItems.plugin.getDataFolder(), "wiki/");
        if (!wikiDir.mkdirs()) {
            if (!wikiDir.exists() || !wikiDir.isDirectory()) {
                throw new IllegalStateException();
            }
        }

        int customHeaderLine = -1;
        int customDescriptionLine = -1;
        int customPropertiesLine = -1;
        int customExampleLine = -1;
        int customNoteLine = -1;

        String propertyName = null;
        String propertyType = null;
        String propertyDefaultValue = null;
        String propertyRequired = null;
        String propertyDescription = null;

        String propertyDefaultTrigger = null;
        String propertyAvailableTrigger = null;
        String propertyImmutableTrigger = null;
        String propertyMarker = null;

        InputStream inputStream = RPGItems.plugin.getResource("Template_" + locale.toString() + ".md");
        String newLine = System.getProperty("line.separator");
        BufferedReader reader = new BufferedReader(new InputStreamReader(inputStream, StandardCharsets.UTF_8));
        List<String> template = new ArrayList<>(100);
        for (String line; (line = reader.readLine()) != null; ) {
            if (line.contains("<!-- begin")) {
                int current = template.size();
                if (line.contains("beginCustomHeader")) {
                    customHeaderLine = current;
                } else if (line.contains("beginCustomDescription")) {
                    customDescriptionLine = current;
                } else if (line.contains("beginCustomProperties")) {
                    customPropertiesLine = current;
                } else if (line.contains("beginCustomExample")) {
                    customExampleLine = current;
                } else if (line.contains("beginCustomNote")) {
                    customNoteLine = current;
                }
            }
            if (line.contains("<!-- property")) {
                String pattern = line.substring(line.indexOf("[") + 1, line.lastIndexOf("]")).replace("\\n", newLine);
                if (line.contains("propertyName")) {
                    propertyName = pattern;
                } else if (line.contains("propertyType")) {
                    propertyType = pattern;
                } else if (line.contains("propertyDefaultValue")) {
                    propertyDefaultValue = pattern;
                } else if (line.contains("propertyRequired")) {
                    propertyRequired = pattern;
                } else if (line.contains("propertyDescription")) {
                    propertyDescription = pattern;
                } else if (line.contains("propertyDefaultTrigger")) {
                    propertyDefaultTrigger = pattern;
                } else if (line.contains("propertyAvailableTrigger")) {
                    propertyAvailableTrigger = pattern;
                } else if (line.contains("propertyImmutableTrigger")) {
                    propertyImmutableTrigger = pattern;
                } else if (line.contains("propertyMarker")) {
                    propertyMarker = pattern;
                }
                continue;
            }
            template.add(line);
        }

        if (Stream.of(customHeaderLine, customDescriptionLine, customPropertiesLine, customExampleLine, customNoteLine)
                  .anyMatch(i -> i == -1)) {
            throw new IllegalArgumentException();
        }

        if (Stream.of(propertyName, propertyType, propertyDefaultValue, propertyRequired, propertyDescription, propertyDefaultTrigger, propertyAvailableTrigger, propertyImmutableTrigger, propertyMarker)
                  .anyMatch(Objects::isNull)) {
            throw new IllegalArgumentException();
        }

        Map<Class<? extends PropertyHolder>, Map<String, Pair<Method, PropertyInstance>>> allProperties = PowerManager.getProperties();

        Map<String, StringBuilder> catalogs = new HashMap<>(5);
        catalogs.put("condition", new StringBuilder("# Conditions\n\n"));
        catalogs.put("power", new StringBuilder("# Powers\n\n"));
        catalogs.put("marker", new StringBuilder("# Markers\n\n"));
        catalogs.put("trigger", new StringBuilder("# Triggers\n\n"));
        catalogs.put("modifier", new StringBuilder("# Modifiers\n\n"));

        for (Map.Entry<Class<? extends PropertyHolder>, Map<String, Pair<Method, PropertyInstance>>> entry : allProperties.entrySet().stream().sorted(Comparator.comparing(e -> e.getKey().getCanonicalName(), String::compareToIgnoreCase)).collect(Collectors.toList())) {
            Class<? extends PropertyHolder> clazz = entry.getKey();
            Map<String, Pair<Method, PropertyInstance>> properties = entry.getValue();
            PropertyHolder instance = Trigger.class.isAssignableFrom(clazz) ? Trigger.values().stream().filter(p -> p.getClass().equals(clazz)).findAny().get() : PowerManager.instantiate(clazz);
            String type = instance.getPropertyHolderType();
            String localizedName = instance.getLocalizedName(locale);
            NamespacedKey namespacedKey = instance.getNamespacedKey();
            StringBuilder propertiesDesc = new StringBuilder();
            Meta meta = PowerManager.getMeta(clazz);
            String powerDesc = PowerManager.getDescription(locale.toString(), namespacedKey, null);
            Path file = wikiDir.toPath().resolve(instance.getName() + "-" + locale.toString() + ".md");

            String catalogEntry = "* [" + localizedName + " " + "(" + namespacedKey.toString() + ")](./" + file.getFileName().toString().replace(".md", "") + ")\n";
            catalogEntry += "  " + powerDesc + "\n";
            RPGItems.logger.log(Level.INFO, "Generating wiki for " + instance.getPropertyHolderType() + " " + instance.getLocalizedName(locale));

            catalogs.get(type).append(catalogEntry);
            StringBuilder customHeader = new StringBuilder();
            StringBuilder customDescription = new StringBuilder();
            StringBuilder customProperties = new StringBuilder();
            StringBuilder customExample = new StringBuilder();
            StringBuilder customNote = new StringBuilder();

            if (file.toFile().exists()) {
                List<String> old = Files.readAllLines(file, StandardCharsets.UTF_8);
                Iterator<String> oldIterator = old.iterator();

                while (oldIterator.hasNext()) {
                    String current = oldIterator.next();
                    if (current.contains("<!-- beginCustomHeader -->")) {
                        while (oldIterator.hasNext()) {
                            String chLine = oldIterator.next();
                            if (chLine.contains("<!-- endCustomHeader -->")) {
                                break;
                            }
                            customHeader.append(chLine).append("\n");
                        }
                        customHeader.delete(Math.max(customHeader.length() - 1, 0), customHeader.length());
                    }
                    if (current.contains("<!-- beginCustomDescription -->")) {
                        while (oldIterator.hasNext()) {
                            String chLine = oldIterator.next();
                            if (chLine.contains("<!-- endCustomDescription -->")) {
                                break;
                            }
                            customDescription.append(chLine).append("\n");
                        }
                        customDescription.delete(Math.max(customDescription.length() - 1, 0), customDescription.length());
                    }
                    if (current.contains("<!-- beginCustomProperties -->")) {
                        while (oldIterator.hasNext()) {
                            String chLine = oldIterator.next();
                            if (chLine.contains("<!-- endCustomProperties -->")) {
                                break;
                            }
                            customProperties.append(chLine).append("\n");
                        }
                        customProperties.delete(Math.max(customProperties.length() - 1, 0), customProperties.length());
                    }
                    if (current.contains("<!-- beginCustomExample -->")) {
                        while (oldIterator.hasNext()) {
                            String chLine = oldIterator.next();
                            if (chLine.contains("<!-- endCustomExample -->")) {
                                break;
                            }
                            customExample.append(chLine).append("\n");
                        }
                        customExample.delete(Math.max(customExample.length() - 1, 0), customExample.length());
                    }
                    if (current.contains("<!-- beginCustomNote -->")) {
                        while (oldIterator.hasNext()) {
                            String chLine = oldIterator.next();
                            if (chLine.contains("<!-- endCustomNote -->")) {
                                break;
                            }
                            customNote.append(chLine).append("\n");
                        }
                        customNote.delete(Math.max(customNote.length() - 1, 0), customNote.length());
                    }
                }
            }
            for (Map.Entry<String, Pair<Method, PropertyInstance>> propertyEntry : properties.entrySet().stream().sorted(Map.Entry.comparingByKey()).collect(Collectors.toList())) {
                String name = propertyEntry.getKey();
                PropertyInstance property = propertyEntry.getValue().getValue();

                if (!type.equals("trigger") && isTrivialProperty(meta, name)) {
                    continue;
                }

                propertiesDesc.append(propertyName.replace("${}", name));
                propertiesDesc.append(propertyType.replace("${}", property.field().getGenericType().getTypeName().replaceAll("(java|think|org\\.bukkit)\\.([a-zA-Z0-9_$]+\\.)*", "").replace("<", "&lt;").replace(">", "&gt;")));
                if (property.required()) {
                    propertiesDesc.append(propertyRequired);
                } else {
                    String value = Utils.getProperty(instance, name, property.field());
                    if (value != null && !value.trim().isEmpty()) {
                        propertiesDesc.append(propertyDefaultValue.replace("${}", value));
                    }
                }
                String description = PowerManager.getDescription(locale.toString(), namespacedKey, name);
                propertiesDesc.append(propertyDescription.replace("${}", description == null ? I18n.getInstance(sender).format("message.power.no_description") : description));
            }
            List<String> powerTemplate = new ArrayList<>(template);
            if (customNote.length() > 0) {
                powerTemplate.add(customNoteLine + 1, customNote.toString());
            }
            if (customExample.length() > 0) {
                powerTemplate.add(customExampleLine + 1, customExample.toString());
            }
            if (customProperties.length() > 0) {
                powerTemplate.add(customPropertiesLine + 1, customProperties.toString());
            }
            if (customDescription.length() > 0) {
                powerTemplate.add(customDescriptionLine + 1, customDescription.toString());
            }
            if (customHeader.length() > 0) {
                powerTemplate.add(customHeaderLine + 1, customHeader.toString());
            }
            String fullTemplate = String.join(newLine, powerTemplate);
            fullTemplate = fullTemplate.replace("${powerName}", localizedName);
            fullTemplate = fullTemplate.replace("${namespacedKey}", namespacedKey.toString());
            fullTemplate = fullTemplate.replace("${plugin}", PowerManager.getExtensions().get(namespacedKey.getNamespace()).getName());

            if (meta == null) {
                fullTemplate = fullTemplate.replace("${trigger}", "");
            } else if (meta.marker()) {
                fullTemplate = fullTemplate.replace("${trigger}", propertyMarker);
            } else if (instance instanceof Power) {
                String defTriggers = ((Power) instance).getTriggers().stream().map(Trigger::name).map(s -> "`" + s + "`").sorted().collect(Collectors.joining(", "));
                if (meta.immutableTrigger()) {
                    String trigger = propertyImmutableTrigger.replace("${}", defTriggers);
                    fullTemplate = fullTemplate.replace("${trigger}", trigger);
                } else {
                    String trigger = propertyDefaultTrigger.replace("${}", defTriggers);
                    String available = PowerManager.getAcceptedValue(clazz, properties.get("triggers").getValue().field().getAnnotation(AcceptedValue.class)).stream().map(s -> "`" + s + "`").collect(Collectors.joining(", "));
                    trigger += propertyAvailableTrigger.replace("${}", available);
                    fullTemplate = fullTemplate.replace("${trigger}", trigger);
                }
            }

            fullTemplate = fullTemplate.replace("${description}", powerDesc == null ? I18n.getInstance(sender).format("message.power.no_description") : powerDesc);
            fullTemplate = fullTemplate.replace("${properties}", propertiesDesc.toString());
            java.nio.file.Files.write(file, fullTemplate.getBytes(StandardCharsets.UTF_8));
        }
        catalogs.entrySet().forEach(e -> {
            try {
                Path catalogFile = wikiDir.toPath().resolve(e.getKey() + "-" + locale.toString() + ".md");
                Files.write(catalogFile, e.getValue().toString().getBytes(StandardCharsets.UTF_8));
            } catch (IOException ex) {
                RPGItems.logger.log(Level.WARNING, "Error saving wiki catalog" + e.getKey(), ex);
            }
        });

    }

    @Override
    public String getHelpPrefix() {
        return null;
    }
}
