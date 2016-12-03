package cz.creeper.mineskinsponge;

import com.google.common.reflect.TypeToken;
import ninja.leaping.configurate.ConfigurationNode;
import ninja.leaping.configurate.objectmapping.ObjectMappingException;
import ninja.leaping.configurate.objectmapping.serialize.TypeSerializer;

import java.util.UUID;

public class SkinRecordSerializer implements TypeSerializer<SkinRecord> {
    public static String KEY_PLAYER_ID = "player_id";
    public static String KEY_TEXTURE = "texture";
    public static String KEY_SIGNATURE = "signature";

    @Override
    public SkinRecord deserialize(TypeToken<?> type, ConfigurationNode value) throws ObjectMappingException {
        try {
            return new SkinRecord(
                    value.getNode(KEY_PLAYER_ID).getValue(TypeToken.of(UUID.class)),
                    value.getNode(KEY_TEXTURE).getString(),
                    value.getNode(KEY_SIGNATURE).getString()
            );
        } catch(NullPointerException e) {
            throw new ObjectMappingException(e);
        }
    }

    @Override
    public void serialize(TypeToken<?> type, SkinRecord obj, ConfigurationNode value) throws ObjectMappingException {
        value.getNode(KEY_PLAYER_ID).setValue(TypeToken.of(UUID.class), obj.getPlayerId());
        value.getNode(KEY_TEXTURE).setValue(obj.getTexture());
        value.getNode(KEY_SIGNATURE).setValue(obj.getSignature());
    }
}
