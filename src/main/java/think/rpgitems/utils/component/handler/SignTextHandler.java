package think.rpgitems.utils.component.handler;

import io.papermc.paper.datacomponent.DataComponentType;
import io.papermc.paper.datacomponent.DataComponentTypes;
import io.papermc.paper.datacomponent.item.SignText;
import net.kyori.adventure.text.Component;
import net.kyori.adventure.text.ComponentLike;
import net.kyori.adventure.text.minimessage.MiniMessage;
import org.bukkit.DyeColor;
import org.bukkit.block.Sign;
import org.bukkit.configuration.ConfigurationSection;
import org.jspecify.annotations.Nullable;
import think.rpgitems.item.RPGItem;
import think.rpgitems.utils.component.ComponentHandler;

import java.util.ArrayList;
import java.util.List;
import java.util.Locale;

public class SignTextHandler implements ComponentHandler<SignText> {
    public enum Direction {
        FRONT, BACK
    }

    private final Direction direction;

    public SignTextHandler(Direction direction) {
        this.direction = direction;
    }

    @Override
    public DataComponentType type() {
        return direction == Direction.FRONT ? DataComponentTypes.SIGN_TEXT_FRONT : DataComponentTypes.SIGN_TEXT_BACK;
    }

    @Override
    public String yamlKey() {
        return direction == Direction.FRONT ? "sign_text_front" : "sign_text_back";
    }

    @Override
    public @Nullable Object decode(ConfigurationSection parent, @Nullable RPGItem rpgItem) {
        ConfigurationSection section = parent.getConfigurationSection(yamlKey());
        if (section == null) {
            return null;
        }
        DyeColor color = DyeColor.valueOf(section.getString("color", "BLACK").toUpperCase(Locale.ROOT));
        boolean glow = section.getBoolean("has_glowing_text", false);
        List<ComponentLike> text = new ArrayList<>();
        for(String s : section.getStringList("messages")) {
            text.add(MiniMessage.miniMessage().deserialize(s));
        }
        return SignText.signText().color(color).hasGlowingText(glow).lines(text);
    }

    @Override
    public void encode(SignText value, ConfigurationSection config) {
        ConfigurationSection section = config.createSection(yamlKey());
        section.set("color", value.color().toString().toUpperCase(Locale.ROOT));
        section.set("has_glowing_text", value.hasGlowingText());
        List<String> messages = new ArrayList<>();
        for(Component line : value.lines()) {
            messages.add(MiniMessage.miniMessage().serialize(line));
        }
        section.set("messages", messages);
    }
}
