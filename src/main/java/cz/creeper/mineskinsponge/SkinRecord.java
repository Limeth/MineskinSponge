package cz.creeper.mineskinsponge;

import lombok.*;
import org.mineskin.data.Skin;
import org.spongepowered.api.Sponge;
import org.spongepowered.api.data.key.Keys;
import org.spongepowered.api.data.type.SkullTypes;
import org.spongepowered.api.item.ItemTypes;
import org.spongepowered.api.item.inventory.ItemStack;
import org.spongepowered.api.plugin.PluginContainer;
import org.spongepowered.api.profile.GameProfile;
import org.spongepowered.api.profile.property.ProfileProperty;

import java.util.UUID;

@AllArgsConstructor(access = AccessLevel.PACKAGE)
@Getter
@EqualsAndHashCode(exclude = "profile")
public final class SkinRecord {
    public static final String PROPERTY_TEXTURES = "textures";
    @NonNull
    private final UUID playerId;
    @NonNull
    private final String texture;
    @NonNull
    private final String signature;
    @NonNull @Getter(lazy = true)
    private final GameProfile profile = initProfile();

    SkinRecord(Skin skin) {
        this(skin.data.uuid, skin.data.texture.value, skin.data.texture.signature);
    }

    public ItemStack create() {
        return create(1);
    }

    public ItemStack create(int quantity) {
        ItemStack result = ItemStack.of(ItemTypes.SKULL, quantity);

        apply(result);

        return result;
    }

    public void apply(ItemStack itemStack) {
        itemStack.offer(Keys.SKULL_TYPE, SkullTypes.PLAYER);
        itemStack.offer(Keys.REPRESENTED_PLAYER, getProfile());
    }

    private GameProfile initProfile() {
        MineskinSponge plugin = MineskinSponge.getInstance();
        PluginContainer pluginContainer = Sponge.getPluginManager().fromInstance(plugin)
                .orElseThrow(() -> new IllegalStateException("Could not access the plugin container."));
        GameProfile profile = GameProfile.of(playerId, pluginContainer.getId());

        profile.getPropertyMap().removeAll(PROPERTY_TEXTURES);
        profile.getPropertyMap().put(PROPERTY_TEXTURES, ProfileProperty.of(PROPERTY_TEXTURES, texture, signature));

        return profile;
    }
}
