package cz.creeper.mineskinsponge;

import org.spongepowered.api.Sponge;
import org.spongepowered.api.command.CommandManager;
import org.spongepowered.api.command.CommandResult;
import org.spongepowered.api.command.args.GenericArguments;
import org.spongepowered.api.command.spec.CommandSpec;
import org.spongepowered.api.plugin.PluginContainer;
import org.spongepowered.api.text.Text;
import org.spongepowered.api.text.format.TextColors;

import java.nio.file.Path;
import java.util.Collection;
import java.util.Map;
import java.util.stream.Collectors;

public class MineskinCommands {
    public static final String FLAG_CACHE_CLEAR_PERMANENT = "permanent";
    public static final String FLAG_CACHE_CLEAR_INTERRUPT = "interrupt";

    public static void register() {
        MineskinSponge plugin = MineskinSponge.getInstance();
        PluginContainer pluginContainer = plugin.getPluginContainer();
        String pluginId = pluginContainer.getId();
        CommandManager commandManager = Sponge.getCommandManager();

        CommandSpec cacheInfo = CommandSpec.builder()
                .permission(pluginId + ".commands.cache.info")
                .description(Text.of("Displays the cache contents."))
                .executor((src, args) -> {
                    MineskinService service = plugin.getService();
                    Map<Path, SkinRecord> skinMap = service.getCachedSkinMap();
                    Collection<SkinRecord> skins = service.getCachedSkins();

                    src.sendMessage(Text.of(TextColors.GRAY, "Total number of cached skins: ", TextColors.YELLOW, skins.size()));
                    src.sendMessage(Text.of(TextColors.GRAY, "Skins accessed this session: ", TextColors.WHITE,  skinMap.keySet().stream()
                            .map(Object::toString)
                            .collect(Collectors.joining(", "))
                    ));

                    return CommandResult.success();
                })
                .build();

        CommandSpec cacheClear = CommandSpec.builder()
                .permission(pluginId + ".commands.cache.clear")
                .description(Text.of("Clears the skin cache."))
                .arguments(
                        GenericArguments.flags()
                                .permissionFlag(pluginId + ".commands.cache.clear.permanent", makeFlags(FLAG_CACHE_CLEAR_PERMANENT))
                                .permissionFlag(pluginId + ".commands.cache.clear.interrupt", makeFlags(FLAG_CACHE_CLEAR_INTERRUPT))
                                .buildWith(GenericArguments.none())
                )
                .executor((src, args) -> {
                    boolean clearPermanentCache = args.hasAny(FLAG_CACHE_CLEAR_PERMANENT);
                    boolean interrupt = args.hasAny(FLAG_CACHE_CLEAR_INTERRUPT);

                    plugin.getService().clearCache(clearPermanentCache, interrupt);
                    src.sendMessage(Text.of(TextColors.GRAY, "The skin cache has been cleared."));

                    return CommandResult.success();
                })
                .build();

        CommandSpec cache = CommandSpec.builder()
                .permission(pluginId + ".commands.cache")
                .description(Text.of("Skin cache commands."))
                .child(cacheClear, "clear", "c")
                .child(cacheInfo, "info", "i")
                .build();

        CommandSpec mineskin = CommandSpec.builder()
                .permission(pluginId + ".commands")
                .description(Text.of("All the various MineskinSponge commands."))
                .child(cache, "cache", "c")
                .build();

        commandManager.register(plugin, mineskin, "mineskinsponge", "mineskin", "mssponge", "mss", "ms");
    }

    private static String[] makeFlags(String longFlag) {
        return new String[] { '-' + longFlag, Character.toString(longFlag.charAt(0)) };
    }
}
