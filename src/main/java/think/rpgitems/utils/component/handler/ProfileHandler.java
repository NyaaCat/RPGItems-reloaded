package think.rpgitems.utils.component.handler;

import com.destroystokyo.paper.profile.ProfileProperty;
import io.papermc.paper.datacomponent.DataComponentType;
import io.papermc.paper.datacomponent.DataComponentTypes;
import io.papermc.paper.datacomponent.item.ResolvableProfile;
import org.bukkit.configuration.ConfigurationSection;
import org.jetbrains.annotations.Nullable;
import think.rpgitems.RPGItems;
import think.rpgitems.item.RPGItem;
import think.rpgitems.utils.component.ComponentHandler;

import java.util.Base64;
import java.util.Optional;

/**
 * profile: "<玩家名 或 base64 皮肤纹理>"
 */
public class ProfileHandler implements ComponentHandler<ResolvableProfile> {

    @Override
    public DataComponentType type() {
        return DataComponentTypes.PROFILE;
    }

    @Override
    public String yamlKey() {
        return "profile";
    }

    @Override
    public @Nullable Object decode(ConfigurationSection parent, @Nullable RPGItem rpgItem) {
        String value = parent.getString(yamlKey());
        if (value == null) {
            return null;
        }
        if (isBase64(value)) {
            return ResolvableProfile.resolvableProfile()
                    .addProperty(new ProfileProperty("textures", value))
                    .uuid(RPGItems.getUUID());
        }
        return ResolvableProfile.resolvableProfile().name(value).uuid(RPGItems.getUUID());
    }

    @Override
    public void encode(ResolvableProfile value, ConfigurationSection config) {
        Optional<ProfileProperty> textures = value.properties().stream().findFirst();
        if (textures.isPresent()) {
            config.set(yamlKey(), textures.get().getValue());
        } else if (value.name() != null) {
            config.set(yamlKey(), value.name());
        }
    }

    private static boolean isBase64(String input) {
        if (input.length() % 4 != 0) {
            return false;
        }
        try {
            Base64.getDecoder().decode(input);
            return true;
        } catch (IllegalArgumentException e) {
            return false;
        }
    }
}
