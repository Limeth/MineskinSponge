package cz.creeper.mineskinsponge;

import lombok.RequiredArgsConstructor;

import java.util.concurrent.CompletableFuture;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.Executor;
import java.util.function.Consumer;

/**
 * Converts a callback into a future
 */
@RequiredArgsConstructor
public class Futurify<T> {
    private final Executor executor;
    private CountDownLatch latch = new CountDownLatch(1);
    private T result;
    private RuntimeException error;

    public CompletableFuture<T> getFuture() {
        if(executor != null)
            return CompletableFuture.supplyAsync(this::supply, executor);
        else
            return CompletableFuture.supplyAsync(this::supply);
    }

    private T supply() {
        try {
            latch.await();
        } catch (InterruptedException e) {
            throw new RuntimeException(e);
        }

        if(error != null)
            throw error;

        return result;
    }

    public Consumer<T> getCallback() {
        return this::consume;
    }

    private void consume(T result) {
        this.result = result;

        latch.countDown();
    }

    public Consumer<RuntimeException> getErrorCallback() {
        return this::consumeError;
    }

    private void consumeError(RuntimeException error) {
        this.error = error;

        latch.countDown();
    }
}
