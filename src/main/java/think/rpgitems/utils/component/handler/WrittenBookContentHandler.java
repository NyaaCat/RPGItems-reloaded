package think.rpgitems.utils.component.handler;

import io.papermc.paper.datacomponent.DataComponentType;
import io.papermc.paper.datacomponent.DataComponentTypes;
import io.papermc.paper.datacomponent.item.WrittenBookContent;
import net.kyori.adventure.text.minimessage.MiniMessage;
import org.bukkit.configuration.ConfigurationSection;
import org.jetbrains.annotations.Nullable;
import think.rpgitems.item.RPGItem;
import think.rpgitems.utils.component.ComponentHandler;

import java.util.List;

/**
 * written_book_content:
 *   title: "<gold>My Book"
 *   author: "Steve"
 *   generation: 0
 *   pages:
 *   - "<red>Page one"
 * <p>
 * 页面内容用 MiniMessage 解析成富文本；title/author 是纯字符串。
 */
public class WrittenBookContentHandler implements ComponentHandler<WrittenBookContent> {

    @Override
    public DataComponentType type() {
        return DataComponentTypes.WRITTEN_BOOK_CONTENT;
    }

    @Override
    public String yamlKey() {
        return "written_book_content";
    }

    @Override
    public @Nullable Object decode(ConfigurationSection parent, @Nullable RPGItem rpgItem) {
        ConfigurationSection section = parent.getConfigurationSection(yamlKey());
        if (section == null) {
            return null;
        }
        String title = section.getString("title");
        String author = section.getString("author");
        if (title == null || author == null) {
            return null;
        }
        WrittenBookContent.Builder builder = WrittenBookContent.writtenBookContent(title, author)
                .generation(section.getInt("generation", 0));
        for (String page : section.getStringList("pages")) {
            builder.addPage(MiniMessage.miniMessage().deserialize(page));
        }
        return builder;
    }

    @Override
    public void encode(WrittenBookContent value, ConfigurationSection config) {
        ConfigurationSection section = config.createSection(yamlKey());
        section.set("title", value.title().raw());
        section.set("author", value.author());
        section.set("generation", value.generation());
        List<String> pages = value.pages().stream()
                .map(page -> MiniMessage.miniMessage().serialize(page.raw()))
                .toList();
        section.set("pages", pages);
    }
}
