package cz.creeper.mineskinsponge;

import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.NonNull;
import ninja.leaping.configurate.objectmapping.Setting;
import ninja.leaping.configurate.objectmapping.serialize.ConfigSerializable;
import org.mineskin.data.Skin;
import org.spongepowered.api.data.key.Keys;
import org.spongepowered.api.data.type.SkullTypes;
import org.spongepowered.api.item.ItemTypes;
import org.spongepowered.api.item.inventory.ItemStack;
import org.spongepowered.api.profile.GameProfile;

import java.util.UUID;

@AllArgsConstructor
@Getter
@ConfigSerializable
public class SkinResult {
    @NonNull @Setting(comment = "The MD5 of the texture")
    private final String md5;
    @NonNull @Setting(comment = "The UUID of the player that provided the skin")
    private final UUID playerId;
    @NonNull @Setting(comment = "The name of the player that provided the skin")
    private final String playerName;
    @NonNull @Setting(comment = "The texture data")
    private final String texture;
    @NonNull @Setting(comment = "The signature from Mojang")
    private final String signature;
    @NonNull @Setting  // TODO add a comment
    private final String url;

    public SkinResult(String md5, Skin skin) {
        this(md5, skin.data.uuid, skin.name, skin.data.texture.value, skin.data.texture.signature, skin.data.texture.url);
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
        itemStack.offer(Keys.REPRESENTED_PLAYER, getProvider());
        // TODO
    }

    public GameProfile getProvider() {
        return GameProfile.of(playerId, playerName);
    }
}
