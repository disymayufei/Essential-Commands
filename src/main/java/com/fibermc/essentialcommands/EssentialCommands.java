package com.fibermc.essentialcommands;

import java.io.IOException;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.List;

import com.fibermc.essentialcommands.commands.RulesCommand;
import com.fibermc.essentialcommands.config.EssentialCommandsConfig;
import com.fibermc.essentialcommands.config.EssentialCommandsConfigSnapshot;
import com.fibermc.essentialcommands.text.ECText;
import com.fibermc.essentialcommands.util.EssentialsConvertor;
import org.apache.logging.log4j.Level;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;

import net.minecraft.server.MinecraftServer;
import net.minecraft.server.world.ServerWorld;

import net.fabricmc.api.ModInitializer;
import net.fabricmc.fabric.api.command.v2.CommandRegistrationCallback;
import net.fabricmc.fabric.api.event.lifecycle.v1.ServerLifecycleEvents;
import net.fabricmc.fabric.api.event.lifecycle.v1.ServerWorldEvents;
import net.fabricmc.loader.api.FabricLoader;
import net.fabricmc.loader.api.ModContainer;
import net.fabricmc.loader.api.metadata.ModMetadata;

import dev.jpcode.eccore.util.TimeUtil;

public final class EssentialCommands implements ModInitializer {
    private static final String MOD_CONTAINER_ID = "essential_commands";
    public static final ModMetadata MOD_METADATA = FabricLoader.getInstance().getModContainer(MOD_CONTAINER_ID).map(ModContainer::getMetadata).orElse(null);
    public static final String MOD_ID = MOD_METADATA == null ? "essentialcommands | ERR - NO MOD DATA" : MOD_METADATA.getId();
    public static final Logger LOGGER = LogManager.getLogger("EssentialCommands");
    public static final EssentialCommandsConfig BACKING_CONFIG = new EssentialCommandsConfig(
        Path.of("./config/EssentialCommands.properties"),
        "Essential Commands Config",
        "https://github.com/John-Paul-R/Essential-Commands/wiki/Config-Documentation"
    );
    @SuppressWarnings("checkstyle:StaticVariableName")
    public static EssentialCommandsConfigSnapshot CONFIG = EssentialCommandsConfigSnapshot.create(BACKING_CONFIG);

    public static final List<ServerWorld> WORLD_LIST = new ArrayList<>();

    public static MinecraftServer SERVER_INSTANCE = null;

    public static void log(Level level, String message, Object... args) {
        final String logPrefix = "[EssentialCommands]: ";
        LOGGER.log(level, logPrefix.concat(message), args);
    }

    @Override
    public void onInitialize() {
        if (MOD_METADATA == null) {
            log(Level.WARN, "failed to load mod metadata using mod container id '{}' ", MOD_CONTAINER_ID);
        }

        log(Level.INFO, "Mod Load Initiated.");

        BACKING_CONFIG.registerLoadHandler((backingConfig) -> CONFIG = EssentialCommandsConfigSnapshot.create(backingConfig));
        BACKING_CONFIG.loadOrCreateProperties();

        ECPlaceholderRegistry.register();
        ECAbilitySources.init();

        ManagerLocator managers = ManagerLocator.getInstance();
        managers.init();
        ServerLifecycleEvents.SERVER_STARTING.register((server) -> {
            ECText.init(server);
            TimeUtil.init(server);
            managers.onServerStart(server);
            ECPerms.init(); // ECPerms must start after WorldDataManager at present (for warps).

            if (CONFIG.ENABLE_RULES) {
                try {
                    RulesCommand.reload(server);
                } catch (IOException e) {
                    LOGGER.error("An error occurred while loading EssentialCommands rules file.");
                    e.printStackTrace();
                }
            }
        });

        CommandRegistrationCallback.EVENT.register(EssentialCommandRegistry::register);

        ServerWorldEvents.LOAD.register((server, world) ->{
            SERVER_INSTANCE = server;

            if(!WORLD_LIST.contains(world)){
                WORLD_LIST.add(world);
            }
        });

        ServerLifecycleEvents.SERVER_STARTED.register(server -> {
            // load converter
            EssentialsConvertor.homeConvert();
            EssentialsConvertor.warpConvert();
        });

        if (CONFIG.CHECK_FOR_UPDATES) {
            Updater.checkForUpdates();
        }

        log(Level.INFO, "Mod Load Complete.");
    }
}
