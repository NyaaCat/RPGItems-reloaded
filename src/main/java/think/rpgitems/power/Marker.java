package think.rpgitems.power;

import com.google.common.base.Strings;
import com.google.common.reflect.TypeToken;
import think.rpgitems.RPGItems;

import javax.annotation.Nullable;
import java.util.Collections;
import java.util.Locale;
import java.util.Objects;
import java.util.Set;
import java.util.stream.Collectors;
import java.util.stream.Stream;

/**
 * Base interface for all powers
 */
public interface Marker extends PropertyHolder {

    /**
     * Display name of this power
     *
     * @return Display name
     */
    @Nullable
    String displayName();

    /**
     * Display name or default name of this power
     *
     * @return Display name or default name
     */
    default String getLocalizedDisplayName() {
        return Strings.isNullOrEmpty(displayName()) ? getLocalizedName(RPGItems.plugin.cfg.language) : displayName();
    }

    /**
     * Display text of this power
     *
     * @return Display text
     */
    String displayText();

    /**
     * Localized name of this power
     *
     * @param locale Locale tag
     * @return Localized name
     */
    default String localizedDisplayText(String locale) {
        return displayText();
    }
}
