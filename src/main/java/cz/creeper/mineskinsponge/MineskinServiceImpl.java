package cz.creeper.mineskinsponge;

import com.google.common.collect.Maps;
import com.google.common.collect.Sets;
import com.google.common.reflect.TypeToken;
import com.sun.org.apache.xerces.internal.impl.dv.util.HexBin;
import ninja.leaping.configurate.commented.CommentedConfigurationNode;
import ninja.leaping.configurate.hocon.HoconConfigurationLoader;
import ninja.leaping.configurate.loader.ConfigurationLoader;
import ninja.leaping.configurate.objectmapping.ObjectMappingException;
import org.apache.commons.lang3.tuple.Pair;
import org.mineskin.MineskinClient;

import javax.imageio.ImageIO;
import java.awt.*;
import java.awt.image.BufferedImage;
import java.io.IOException;
import java.io.InputStream;
import java.net.URI;
import java.net.URL;
import java.nio.file.Files;
import java.nio.file.Path;
import java.security.DigestInputStream;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;
import java.util.Collection;
import java.util.Map;
import java.util.Objects;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.ConcurrentHashMap;
import java.util.stream.Collectors;

public class MineskinServiceImpl implements MineskinService {
    public static final String CACHE_DIRECTORY_NAME = "cache";
    public static final String CONFIG_FILE_NAME = "permanent_cache.conf";
    public static final String CONFIG_NODE_MD5TOSKIN = "md5_to_skin";
    private final MineskinClient client = new MineskinClient();
    private final Map<ResourceIdentifier, CompletableFuture<Path>> resourceIdToPath = new ConcurrentHashMap<>();
    private final Map<Path, CompletableFuture<String>> pathToMd5 = new ConcurrentHashMap<>();
    private final Map<String, CompletableFuture<SkinRecord>> md5ToSkin = new ConcurrentHashMap<>();

    public MineskinServiceImpl load() {
        MineskinSponge plugin = MineskinSponge.getInstance();
        ConfigurationLoader<CommentedConfigurationNode> loader = getConfigurationLoader();
        CommentedConfigurationNode rootNode;

        try {
            rootNode = loader.load();
        } catch (IOException e) {
            plugin.getLogger().error("An error occurred while loading the cache.");
            throw new RuntimeException(e);
        }

        CommentedConfigurationNode md5ToSkinNode = rootNode.getNode(CONFIG_NODE_MD5TOSKIN);

        clearCache(false);

        md5ToSkinNode.getChildrenMap().entrySet().forEach(entry -> {
            Object key = entry.getKey();
            CommentedConfigurationNode value = entry.getValue();

            try {
                md5ToSkin.put((String) key, CompletableFuture.completedFuture(value.getValue(TypeToken.of(SkinRecord.class))));
            } catch (ObjectMappingException | ClassCastException e) {
                plugin.getLogger().warn("Could not parse a SkinRecord: " + Objects.toString(key) + " -> " +
                                        Objects.toString(value) + "; skipping.");
            }
        });

        return this;
    }

    public void save() {
        MineskinSponge plugin = MineskinSponge.getInstance();
        Path configPath = getConfigPath();
        ConfigurationLoader<CommentedConfigurationNode> loader = getConfigurationLoader();
        CommentedConfigurationNode rootNode = loader.createEmptyNode();
        CommentedConfigurationNode md5ToSkinNode = rootNode.getNode(CONFIG_NODE_MD5TOSKIN);

        for(Map.Entry<String, CompletableFuture<SkinRecord>> entry : md5ToSkin.entrySet()) {
            if(!MineskinService.isSuccessfulEntry(entry))
                continue;

            String key = entry.getKey();
            SkinRecord value = MineskinService.unwrap(entry.getValue());

            try {
                md5ToSkinNode.getNode(key).setValue(TypeToken.of(SkinRecord.class), value);
            } catch (ObjectMappingException e) {
                plugin.getLogger().warn("Could not serialize a SkinRecord: " + Objects.toString(key) + " -> " +
                        Objects.toString(value) + "; skipping.");
            }
        }

        try {
            Files.createDirectories(configPath.getParent());

            if(!Files.isRegularFile(configPath))
                Files.createFile(configPath);

            loader.save(rootNode);
        } catch (IOException e) {
            plugin.getLogger().error("An error occurred while saving the cache.");
            throw new RuntimeException(e);
        }
    }

    public ConfigurationLoader<CommentedConfigurationNode> getConfigurationLoader() {
        MineskinSponge plugin = MineskinSponge.getInstance();
        Path configPath = getConfigPath();

        return HoconConfigurationLoader.builder()
                .setDefaultOptions(plugin.getConfigurationOptions())
                .setPath(configPath)
                .build();
    }

    public Path getConfigPath() {
        return MineskinSponge.getInstance().getConfigDir().resolve(CONFIG_FILE_NAME);
    }

    public Path getCachePath() {
        return MineskinSponge.getInstance().getConfigDir().resolve(CACHE_DIRECTORY_NAME);
    }

    @Deprecated
    @Override
    public MineskinClient getDirectAPI() {
        return client;
    }

    @Override
    public CompletableFuture<SkinRecord> getSkinAsync(Path texturePath) {
        return getMd5(texturePath).thenCompose(md5 -> md5ToSkin.computeIfAbsent(md5, k -> computeSkin(texturePath)));
    }

    @Override
    public CompletableFuture<SkinRecord> getSkinAsync(Resource resource) {
        return resourceIdToPath.computeIfAbsent(resource.getIdentifier(), k -> copyTexture(resource)).thenCompose(this::getSkinAsync);
    }

    private CompletableFuture<Path> copyTexture(Resource resource) {
        return getPath(resource.getIdentifier()).thenApply(texturePath -> {
            try {
                Files.createDirectories(texturePath.getParent());

                if(Files.isRegularFile(texturePath))
                    return texturePath;
                else
                    MineskinSponge.getInstance().getLogger()
                            .info("Caching texture: " + resource.getIdentifier());

                BufferedImage inputImage = ImageIO.read(resource.openStream());
                int width = inputImage.getWidth();
                int height = inputImage.getHeight();
                BufferedImage outputImage;

                if(width == 64 && height == 16) {
                    outputImage = new BufferedImage(64, 64, BufferedImage.TYPE_INT_ARGB);
                    Graphics2D outputGraphics = outputImage.createGraphics();
                    Color transparent = new Color(0, 0, 0, 0);

                    outputGraphics.setComposite(AlphaComposite.SrcOver);
                    outputGraphics.setRenderingHint(RenderingHints.KEY_INTERPOLATION, RenderingHints.VALUE_INTERPOLATION_NEAREST_NEIGHBOR);
                    outputGraphics.setRenderingHint(RenderingHints.KEY_RENDERING, RenderingHints.VALUE_RENDER_QUALITY);
                    outputGraphics.setRenderingHint(RenderingHints.KEY_ANTIALIASING, RenderingHints.VALUE_ANTIALIAS_OFF);
                    outputGraphics.drawImage(inputImage, 0, 0, 64, 16, transparent, null);
                    outputGraphics.dispose();
                } else if(width == 64 && height == 64) {
                    outputImage = inputImage;
                } else {
                    throw new IllegalArgumentException("The texture must either be a skin (64x64,"
                            + " png file) or just the upper part of the skin, head only (64x16, png file).");
                }

                ImageIO.write(outputImage, "png", texturePath.toFile());
            } catch (IOException e) {
                throw new RuntimeException(e);
            }

            return texturePath;
        });
    }

    private CompletableFuture<Path> getPath(ResourceIdentifier identifier) {
        return computeMd5(identifier.getUri().toString(), identifier.getVersion())
                .thenApply(md5 -> getCachePath().resolve(md5 + ".png"));
    }

    private CompletableFuture<SkinRecord> computeSkin(Path texturePath) {
        MineskinSponge plugin = MineskinSponge.getInstance();
        SimpleSkinCallback callback = new SimpleSkinCallback(plugin.getAsyncExecutor());

        client.generateUpload(texturePath.toFile(), callback);

        return callback.getFuture().thenApply(SkinRecord::new);
    }

    @Override
    public Collection<CompletableFuture<SkinRecord>> getSkinsAsync() {
        return Sets.newHashSet(md5ToSkin.values());
    }

    @Override
    public Map<Path, CompletableFuture<SkinRecord>> getSkinMapAsync() {
        return pathToMd5.keySet().stream()
                .map(path -> Pair.of(path, getSkinAsync(path)))
                .collect(Collectors.toMap(Pair::getLeft, Pair::getRight));
    }

    @Override
    public void clearCache(boolean clearPermanentCache, boolean interrupt) {
        if (interrupt) {
            pathToMd5.entrySet().stream()
                    .filter(entry -> !entry.getValue().isDone())
                    .forEach(entry -> entry.getValue().cancel(true));
        }

        pathToMd5.clear();

        if(interrupt) {
            md5ToSkin.entrySet().stream()
                    .filter(entry -> !entry.getValue().isDone())
                    .forEach(entry -> entry.getValue().cancel(true));
        }

        md5ToSkin.clear();

        if(clearPermanentCache) {
            try {
                Files.deleteIfExists(getConfigPath());
            } catch (IOException e) {
                throw new RuntimeException(e);
            }
        }
    }

    private CompletableFuture<String> getMd5(Path path) {
        return pathToMd5.computeIfAbsent(path, k -> computeMd5(path));
    }

    private static CompletableFuture<String> computeMd5(Path path) {
        return CompletableFuture.supplyAsync(() -> {
            try {
                MessageDigest md = MessageDigest.getInstance("MD5");  // Possibly use a different hashing algorithm

                try (InputStream is = Files.newInputStream(path);
                     DigestInputStream dis = new DigestInputStream(is, md)) {
                    int readByte;

                    do {
                        readByte = dis.read();
                    } while(readByte != -1);
                }

                byte[] bytes = md.digest();

                return HexBin.encode(bytes);
            } catch (IOException | NoSuchAlgorithmException e) {
                throw new RuntimeException(e);
            }
        }, MineskinSponge.getInstance().getAsyncExecutor());
    }

    private static CompletableFuture<String> computeMd5(String... strings) {
        return CompletableFuture.supplyAsync(() -> {
            try {
                MessageDigest md = MessageDigest.getInstance("MD5");  // Possibly use a different hashing algorithm

                for(String string : strings) {
                    byte[] bytesOfMessage = string.getBytes("UTF-8");
                    md.update(bytesOfMessage);
                }

                return HexBin.encode(md.digest());
            } catch (IOException | NoSuchAlgorithmException e) {
                throw new RuntimeException(e);
            }
        }, MineskinSponge.getInstance().getAsyncExecutor());
    }
}
