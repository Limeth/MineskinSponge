package cz.creeper.mineskinsponge;

import org.apache.commons.lang3.tuple.Pair;
import org.mineskin.MineskinClient;
import org.spongepowered.api.scheduler.Scheduler;

import java.nio.file.Path;
import java.util.Collection;
import java.util.Map;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.ExecutionException;
import java.util.concurrent.Executor;
import java.util.function.Function;
import java.util.stream.Collectors;

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
     * Any {@link CompletableFuture#thenApply(Function)} method calls on these futures
     * will be executed asynchronously in order to access the Sponge instance,
     * make sure to use the {@link CompletableFuture#thenApplyAsync(Function, Executor)} methods
     * where the {@link Executor} is the result of {@link Scheduler#createSyncExecutor(Object)}.
     *
     * @param texturePath The path to the texture
     * @return A {@link CompletableFuture} containing the resulting {@link SkinRecord}
     *
     * @see #getSkin(Path)
     */
    CompletableFuture<SkinRecord> getSkinAsync(Path texturePath);

    /**
     * Sends the specified texture to the Mineskin service,
     * the received data is then registered and cached to be re-used the next
     * time this method is called.
     *
     * The skin retrieval is done asynchronously, but any {@link CompletableFuture#thenApply(Function)}
     * method call on the returned {@link CompletableFuture} is run synchronously on the main thread.
     *
     * @param texturePath The path to the texture
     * @return A {@link CompletableFuture} containing the resulting {@link SkinRecord}
     *
     * @see #getSkinAsync(Path)
     */
    default CompletableFuture<SkinRecord> getSkin(Path texturePath) {
        return synchronize(getSkinAsync(texturePath));
    }

    /**
     * Any {@link CompletableFuture#thenApply(Function)} method calls on these futures
     * will be executed asynchronously in order to access the Sponge instance,
     * make sure to use the {@link CompletableFuture#thenApplyAsync(Function, Executor)} methods
     * where the {@link Executor} is the result of {@link Scheduler#createSyncExecutor(Object)}.
     *
     * @return All skins that are being fetched or have been cached
     */
    Collection<CompletableFuture<SkinRecord>> getSkinsAsync();

    /**
     * The skin retrieval is done asynchronously, but any {@link CompletableFuture#thenApply(Function)}
     * method call on the returned {@link CompletableFuture} is run synchronously on the main thread.
     *
     * @return All skins that are being fetched or have been cached
     */
    default Collection<CompletableFuture<SkinRecord>> getSkins() {
        return getSkinsAsync().stream().map(MineskinService::synchronize).collect(Collectors.toSet());
    }

    /**
     * @return All cached skins
     */
    default Collection<SkinRecord> getCachedSkins() {
        return getSkinsAsync().stream()
                .filter(MineskinService::isSuccessful)
                .map(MineskinService::unwrap)
                .collect(Collectors.toSet());
    }

    /**
     * Any {@link CompletableFuture#thenApply(Function)} method calls on these futures
     * will be executed asynchronously in order to access the Sponge instance,
     * make sure to use the {@link CompletableFuture#thenApplyAsync(Function, Executor)} methods
     * where the {@link Executor} is the result of {@link Scheduler#createSyncExecutor(Object)}.
     *
     * @return A map of all skins that are being fetched or have been cached
     */
    Map<Path, CompletableFuture<SkinRecord>> getSkinMapAsync();


    /**
     * The skin retrieval is done asynchronously, but any {@link CompletableFuture#thenApply(Function)}
     * method call on the returned {@link CompletableFuture} is run synchronously on the main thread.
     *
     * @return A map of all skins that are being fetched or have been cached
     */
    default Map<Path, CompletableFuture<SkinRecord>> getSkinMap() {
        return getSkinMapAsync().entrySet().stream()
                .map(entry -> Pair.of(entry.getKey(), synchronize(entry.getValue())))
                .collect(Collectors.toMap(Pair::getLeft, Pair::getRight));
    }

    /**
     * This is typically a more expensive method than {@link #getCachedSkins()}.
     *
     * @return A map of skins that have been cached
     */
    default Map<Path, SkinRecord> getCachedSkinMap() {
        return getSkinMapAsync().entrySet().stream()
                .filter(MineskinService::isSuccessfulEntry)
                .map(entry -> Pair.of(entry.getKey(), unwrap(entry.getValue())))
                .collect(Collectors.toMap(Pair::getLeft, Pair::getRight));
    }

    /**
     * Clears the data cache. This forces all skin data to be re-downloaded
     * from the Mineskin service.
     *
     * @param clearPermanentCache Delete the cache files too
     * @param interrupt Interrupt futures that are currently being completed.
     *                  May have cause side effects in plugins that do not expect futures not to finish successfully.
     */
    @SuppressWarnings("SameParameterValue")
    void clearCache(boolean clearPermanentCache, boolean interrupt);

    default void clearCache(boolean clearPermanentCache) {
        clearCache(clearPermanentCache, false);
    }

    default void clearCache() {
        clearCache(true);
    }

    /**
     * Run the following {@link CompletableFuture#thenApply(Function)} method calls on the main thread.
     */
    static <T> CompletableFuture<T> synchronize(CompletableFuture<T> future) {
        MineskinSponge plugin = MineskinSponge.getInstance();

        return future.thenApplyAsync(Function.identity(), plugin.getSyncExecutor());
    }

    static <T> boolean isSuccessfulEntry(Map.Entry<?, CompletableFuture<T>> entry) {
        return isSuccessful(entry.getValue());
    }

    static boolean isSuccessful(CompletableFuture<?> future) {
        return future.isDone() && !future.isCompletedExceptionally() && !future.isCancelled();
    }

    static <T> T unwrap(CompletableFuture<T> future) {
        try {
            return future.get();
        } catch (InterruptedException | ExecutionException e) {
            throw new IllegalStateException("Invalid futures should have been filtered out at this point.", e);
        }
    }
}
