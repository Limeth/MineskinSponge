package cz.creeper.mineskinsponge;

import com.google.inject.Inject;
import lombok.AccessLevel;
import lombok.Getter;
import org.slf4j.Logger;
import org.spongepowered.api.Sponge;
import org.spongepowered.api.event.Listener;
import org.spongepowered.api.event.Order;
import org.spongepowered.api.event.game.state.GameConstructionEvent;
import org.spongepowered.api.event.game.state.GamePreInitializationEvent;
import org.spongepowered.api.plugin.Plugin;

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
public class MineskinSponge {
    @Getter(AccessLevel.PACKAGE)
    private static MineskinSponge instance;
    @Inject
    private Logger logger;
    private MineskinServiceImpl service;

    @Listener(order = Order.EARLY)
    public void onGameConstruction(GameConstructionEvent event) {
        instance = this;
    }

    @Listener(order = Order.EARLY)
    public void onGamePreInitialization(GamePreInitializationEvent event) {
        service = new MineskinServiceImpl();
        Sponge.getServiceManager().setProvider(this, MineskinService.class, service);
    }
}
