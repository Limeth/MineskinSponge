package cz.creeper.mineskinsponge;

import org.mineskin.MineskinClient;

import java.io.File;
import java.util.concurrent.CompletableFuture;

public interface MineskinService {
    @Deprecated
    MineskinClient getDirectAPI();
    CompletableFuture<SkinResult> getSkin(File texture);
}
