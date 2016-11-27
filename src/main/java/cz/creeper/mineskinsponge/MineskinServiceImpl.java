package cz.creeper.mineskinsponge;

import com.sun.org.apache.xerces.internal.impl.dv.util.HexBin;
import org.mineskin.MineskinClient;

import java.io.File;
import java.io.IOException;
import java.io.InputStream;
import java.nio.file.Files;
import java.nio.file.Paths;
import java.security.DigestInputStream;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;
import java.util.concurrent.CompletableFuture;

public class MineskinServiceImpl implements MineskinService {
    private final MineskinClient client = new MineskinClient();

    @Deprecated
    @Override
    public MineskinClient getDirectAPI() {
        return client;
    }

    @Override
    public CompletableFuture<SkinResult> getSkin(File texture) {
        MineskinSponge plugin = MineskinSponge.getInstance();
        SimpleSkinCallback callback = new SimpleSkinCallback(plugin);

        client.generateUpload(texture, callback);


        return callback.getFuture().thenApply(SkinResult::new);
    }

    private String md5(File file) {
        try {
            MessageDigest md = MessageDigest.getInstance("MD5");

            try (InputStream is = Files.newInputStream(Paths.get("file.txt"));
                 DigestInputStream dis = new DigestInputStream(is, md)) {}

            byte[] bytes = md.digest();

            return HexBin.encode(bytes);
        } catch (IOException | NoSuchAlgorithmException e) {
            throw new RuntimeException(e);
        }
    }
}
