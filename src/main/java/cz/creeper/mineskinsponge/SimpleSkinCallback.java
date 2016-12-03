package cz.creeper.mineskinsponge;

import lombok.Getter;
import org.mineskin.data.Skin;
import org.mineskin.data.SkinCallback;

import java.util.concurrent.CompletableFuture;
import java.util.concurrent.Executor;

public class SimpleSkinCallback implements SkinCallback {
    private final Futurify<Skin> futurify;

    public SimpleSkinCallback(Executor executor) {
        futurify = new Futurify<>(executor);
    }

    public CompletableFuture<Skin> getFuture() {
        return futurify.getFuture();
    }

    @Override
    public void done(Skin skin) {
        futurify.getCallback().accept(skin);
    }

    @Override
    public void error(String errorMessage) {
        futurify.getErrorCallback().accept(
                new SkinCallbackResponseErrorException(errorMessage)
        );
    }

    @Override
    public void exception(Exception exception) {
        futurify.getErrorCallback().accept(
                new SkinCallbackRequestException(exception)
        );
    }

    @Override
    public void parseException(Exception exception, String body) {
        futurify.getErrorCallback().accept(
                new SkinCallbackResponseParseException(exception, body)
        );
    }

    public static class SkinCallbackRequestException extends RuntimeException {
        private SkinCallbackRequestException(Throwable t) {
            super("An error occured while requesting a skin.", t);
        }
    }

    public static class SkinCallbackResponseErrorException extends RuntimeException {
        private SkinCallbackResponseErrorException(String message) {
            super(message);
        }
    }

    public static class SkinCallbackResponseParseException extends RuntimeException {
        @Getter
        private final String body;

        private SkinCallbackResponseParseException(Throwable t, String body) {
            super("Could not parse the JSON result.", t);

            this.body = body;
        }
    }
}
