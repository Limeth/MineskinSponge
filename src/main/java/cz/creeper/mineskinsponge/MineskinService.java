package cz.creeper.mineskinsponge;

import org.mineskin.MineskinClient;

import java.nio.file.Path;
import java.util.Collection;
import java.util.Map;
import java.util.concurrent.CompletableFuture;

public interface MineskinService {
    /**
     * The direct usage of the Mineskin Client is discouraged,
     * but do as you please.
     */
    @Deprecated
    MineskinClient getDirectAPI();

    /**
     * Sends the specified texture to the Mineskin service,
     * the received data is then registered and cached to be re-used the next
     * time this method is called.
     *
     * @param texturePath The path to the texture
     * @return A {@link CompletableFuture} containing the resulting {@link SkinRecord}
     */
    CompletableFuture<SkinRecord> getSkin(Path texturePath);

    /**
     * @return All skins that have been cached
     */
    Collection<SkinRecord> getCachedSkins();

    /**
     * This is typically a more expensive method than {@link #getCachedSkins()}.
     *
     * @return A map of skins that have been cached
     */
    Map<Path, SkinRecord> getCachedSkinMap();

    /**
     * Clears the data cache. This forces all skin data to be re-downloaded
     * from the Mineskin service.
     *
     * @param clearPermanentCache Delete the cache files too.
     */
    void clearCache(boolean clearPermanentCache);
}
