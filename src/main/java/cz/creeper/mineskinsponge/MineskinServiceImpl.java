package cz.creeper.mineskinsponge;

import com.google.common.collect.BiMap;
import com.google.common.collect.HashBiMap;
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

import java.io.IOException;
import java.io.InputStream;
import java.nio.file.Files;
import java.nio.file.Path;
import java.security.DigestInputStream;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;
import java.util.*;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.ExecutionException;
import java.util.function.Function;
import java.util.stream.Collectors;

public class MineskinServiceImpl implements MineskinService {
    public static final String CONFIG_FILE_NAME = "permanent_cache.conf";
    public static final String CONFIG_NODE_MD5TOSKIN = "md5_to_skin";
    private final MineskinClient client = new MineskinClient();
    private final Map<Path, CompletableFuture<String>> pathToMd5 = Maps.newHashMap();
    /** Do not access directly, use the corresponding methods instead. */
    @Deprecated
    private final BiMap<String, SkinRecord> md5ToSkin = HashBiMap.create();

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

        //noinspection deprecation
        synchronized (md5ToSkin) {
            md5ToSkinNode.getChildrenMap().entrySet().forEach(entry -> {
                Object key = entry.getKey();
                CommentedConfigurationNode value = entry.getValue();

                try {
                    //noinspection deprecation
                    md5ToSkin.put((String) key, value.getValue(TypeToken.of(SkinRecord.class)));
                } catch (ObjectMappingException | ClassCastException e) {
                    plugin.getLogger().warn("Could not parse a SkinRecord: " + Objects.toString(key) + " -> " +
                                            Objects.toString(value) + "; skipping.");
                }
            });
        }

        return this;
    }

    public void save() {
        MineskinSponge plugin = MineskinSponge.getInstance();
        Path configPath = getConfigPath();
        ConfigurationLoader<CommentedConfigurationNode> loader = getConfigurationLoader();
        CommentedConfigurationNode rootNode = loader.createEmptyNode();
        CommentedConfigurationNode md5ToSkinNode = rootNode.getNode(CONFIG_NODE_MD5TOSKIN);

        //noinspection deprecation
        synchronized (md5ToSkin) {
            //noinspection deprecation
            for(Map.Entry<String, SkinRecord> entry : md5ToSkin.entrySet()) {
                String key = entry.getKey();
                SkinRecord value = entry.getValue();

                try {
                    md5ToSkinNode.getNode(key).setValue(TypeToken.of(SkinRecord.class), value);
                } catch (ObjectMappingException e) {
                    plugin.getLogger().warn("Could not serialize a SkinRecord: " + Objects.toString(key) + " -> " +
                            Objects.toString(value) + "; skipping.");
                }
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

    @Deprecated
    @Override
    public MineskinClient getDirectAPI() {
        return client;
    }

    @Override
    public CompletableFuture<SkinRecord> getSkin(Path texturePath) {
        MineskinSponge plugin = MineskinSponge.getInstance();

        return getMd5(texturePath).thenCompose(md5 -> {
            Optional<SkinRecord> skinRecord = getSkinRecord(md5);

            if (skinRecord.isPresent()) {
                return CompletableFuture.completedFuture(skinRecord.get());
            } else {
                SimpleSkinCallback callback = new SimpleSkinCallback(plugin.getAsyncExecutor());

                client.generateUpload(texturePath.toFile(), callback);

                return callback.getFuture()
                        .thenApply(SkinRecord::new)
                        .thenApply(newRecord -> {
                            registerSkinRecord(md5, newRecord);
                            return newRecord;
                        });
            }
        })
        .thenApplyAsync(Function.identity(), plugin.getSyncExecutor());
    }

    @Override
    public Collection<SkinRecord> getCachedSkins() {
        //noinspection deprecation
        synchronized (md5ToSkin) {
            //noinspection deprecation
            return Sets.newHashSet(md5ToSkin.values());
        }
    }

    @Override
    public Map<Path, SkinRecord> getCachedSkinMap() {
        //noinspection deprecation
        synchronized (md5ToSkin) {
            return pathToMd5.entrySet().stream()
                    .filter(entry -> {
                        CompletableFuture<String> md5 = entry.getValue();

                        return md5.isDone() && !md5.isCancelled() && !md5.isCompletedExceptionally();
                    })
                    .map(entry -> {
                        try {
                            //noinspection deprecation
                            return Pair.of(entry.getKey(), md5ToSkin.get(entry.getValue().get()));
                        } catch (InterruptedException | ExecutionException e) {
                            throw new RuntimeException(e);
                        }
                    })
                    .collect(Collectors.toMap(Pair::getLeft, Pair::getRight));
        }
    }

    @Override
    public void clearCache(boolean clearPermanentCache) {
        pathToMd5.entrySet().stream()
                .filter(entry -> !entry.getValue().isDone())
                .forEach(entry -> entry.getValue().cancel(true));
        pathToMd5.clear();

        //noinspection deprecation
        synchronized (md5ToSkin) {
            //noinspection deprecation
            md5ToSkin.clear();
        }

        if(clearPermanentCache) {
            try {
                Files.deleteIfExists(getConfigPath());
            } catch (IOException e) {
                throw new RuntimeException(e);
            }
        }
    }

    public void registerSkinRecord(String md5, SkinRecord skinRecord) {
        //noinspection deprecation
        synchronized (md5ToSkin) {
            //noinspection deprecation
            md5ToSkin.put(md5, skinRecord);
        }
    }

    public Optional<SkinRecord> getSkinRecord(String md5) {
        //noinspection deprecation
        synchronized (md5ToSkin) {
            //noinspection deprecation
            return Optional.ofNullable(md5ToSkin.get(md5));
        }
    }

    public Optional<String> getMd5(SkinRecord skinRecord) {
        //noinspection deprecation
        synchronized (md5ToSkin) {
            //noinspection deprecation
            return Optional.ofNullable(md5ToSkin.inverse().get(skinRecord));
        }
    }

    private CompletableFuture<String> getMd5(Path path) {
        return pathToMd5.computeIfAbsent(path, k -> generateMd5(path));
    }

    private CompletableFuture<String> generateMd5(Path path) {
        return CompletableFuture.supplyAsync(() -> {
            try {
                MessageDigest md = MessageDigest.getInstance("MD5");

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
}
