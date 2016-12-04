package cz.creeper.mineskinsponge;

import com.google.common.reflect.TypeToken;
import com.google.inject.Inject;
import lombok.AccessLevel;
import lombok.Getter;
import ninja.leaping.configurate.ConfigurationOptions;
import ninja.leaping.configurate.objectmapping.serialize.TypeSerializerCollection;
import org.slf4j.Logger;
import org.spongepowered.api.Sponge;
import org.spongepowered.api.config.ConfigDir;
import org.spongepowered.api.event.Listener;
import org.spongepowered.api.event.Order;
import org.spongepowered.api.event.game.state.GameConstructionEvent;
import org.spongepowered.api.event.game.state.GamePreInitializationEvent;
import org.spongepowered.api.event.game.state.GameStoppedEvent;
import org.spongepowered.api.plugin.Plugin;
import org.spongepowered.api.scheduler.Scheduler;

import java.nio.file.Path;
import java.util.concurrent.Executor;

@Plugin(
        id = "mineskinsponge",
        name = "MineskinSponge",
        description = "An easy to use API for the Mineskin service",
        url = "https://github.com/Limeth/MineskinSponge",
        authors = {
                "Limeth",
                "Inventivetalent"
        }
)
@Getter
public class MineskinSponge {
    @Getter(AccessLevel.PACKAGE)
    private static MineskinSponge instance;
    @Inject
    private Logger logger;
    @Inject
    @ConfigDir(sharedRoot = false)
    private Path configDir;
    @Getter(lazy = true)
    private final ConfigurationOptions configurationOptions = initConfigurationOptions();
    private Executor asyncExecutor;
    private Executor syncExecutor;
    private MineskinServiceImpl service;

    @Listener(order = Order.EARLY)
    public void onGameConstruction(GameConstructionEvent event) {
        instance = this;
    }

    @Listener(order = Order.EARLY)
    public void onGamePreInitialization(GamePreInitializationEvent event) {
        Scheduler scheduler = Sponge.getScheduler();
        asyncExecutor = scheduler.createAsyncExecutor(this);
        syncExecutor = scheduler.createSyncExecutor(this);
        service = new MineskinServiceImpl().load();
        Sponge.getServiceManager().setProvider(this, MineskinService.class, service);
    }

    @Listener(order = Order.LATE)
    public void onGameStopped(GameStoppedEvent event) {
        service.save();
    }

    private ConfigurationOptions initConfigurationOptions() {
        ConfigurationOptions defaults = ConfigurationOptions.defaults();
        TypeSerializerCollection serializers = defaults.getSerializers().newChild();

        serializers.registerType(TypeToken.of(SkinRecord.class), new SkinRecordSerializer());

        return defaults.setShouldCopyDefaults(true)
                .setSerializers(serializers);
    }
}
