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

import java.util.Collection;
import java.util.Optional;
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

    /**
     * Gets a SkinRecord that has been assigned to this {@link ItemStack}.
     *
     * @param itemStack The {@link ItemStack} to get the {@link SkinRecord} of
     * @return The {@link SkinRecord}, if found, or {@link Optional#empty()} instead.
     */
    public static Optional<SkinRecord> of(ItemStack itemStack) {
        if(itemStack.getItem() != ItemTypes.SKULL
                || itemStack.get(Keys.SKULL_TYPE).orElse(SkullTypes.SKELETON) != SkullTypes.PLAYER)
            return Optional.empty();

        return itemStack.get(Keys.REPRESENTED_PLAYER).flatMap(profile -> {
            Collection<ProfileProperty> properties = profile.getPropertyMap().get(PROPERTY_TEXTURES);

            if(properties.size() != 1)
                return Optional.empty();

            ProfileProperty property = properties.iterator().next();
            Optional<String> signature = property.getSignature();

            if(!signature.isPresent())
                return Optional.empty();

            return Optional.of(new SkinRecord(profile.getUniqueId(), property.getValue(), signature.get()));
        });
    }

    /**
     * @return A player head with this skin
     */
    public ItemStack create() {
        return create(1);
    }

    /**
     * @return A player head with this skin
     */
    public ItemStack create(int quantity) {
        ItemStack result = ItemStack.of(ItemTypes.SKULL, quantity);

        apply(result);

        return result;
    }

    /**
     * Applies this skin to the specified {@link ItemStack}.
     *
     * @param itemStack The {@link ItemStack} to apply the skin to
     */
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
