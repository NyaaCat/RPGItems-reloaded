package think.rpgitems.utils.component.handler;

import io.papermc.paper.datacomponent.DataComponentType;
import io.papermc.paper.datacomponent.DataComponentTypes;
import io.papermc.paper.datacomponent.item.WritableBookContent;
import io.papermc.paper.text.Filtered;
import org.bukkit.configuration.ConfigurationSection;
import org.jetbrains.annotations.Nullable;
import think.rpgitems.item.RPGItem;
import think.rpgitems.utils.component.ComponentHandler;

import java.util.List;

/**
 * writable_book_content:
 * - "page 1"
 * - "page 2"
 * <p>
 * 书与笔的页面本身就是纯文本（客户端未定稿前不支持富文本），不走 MiniMessage。
 */
public class WritableBookContentHandler implements ComponentHandler<WritableBookContent> {

    @Override
    public DataComponentType type() {
        return DataComponentTypes.WRITABLE_BOOK_CONTENT;
    }

    @Override
    public String yamlKey() {
        return "writable_book_content";
    }

    @Override
    public @Nullable Object decode(ConfigurationSection parent, @Nullable RPGItem rpgItem) {
        List<String> pages = parent.getStringList(yamlKey());
        if (pages.isEmpty()) {
            return null;
        }
        return WritableBookContent.writeableBookContent().addPages(pages);
    }

    @Override
    public void encode(WritableBookContent value, ConfigurationSection config) {
        config.set(yamlKey(), value.pages().stream().map(Filtered::raw).toList());
    }
}
