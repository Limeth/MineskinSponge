package cz.creeper.mineskinsponge;

import com.google.inject.Inject;
import org.slf4j.Logger;
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
    @Inject
    private Logger logger;
}
