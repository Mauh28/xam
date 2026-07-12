package org.xam.command;

import com.mojang.brigadier.CommandDispatcher;
import com.mojang.brigadier.arguments.BoolArgumentType;
import com.mojang.brigadier.arguments.StringArgumentType;
import com.mojang.brigadier.builder.LiteralArgumentBuilder;
import net.minecraft.commands.CommandSourceStack;
import net.minecraft.commands.Commands;
import net.minecraft.commands.arguments.EntityArgument;
import net.minecraft.commands.arguments.ResourceLocationArgument;
import net.minecraft.network.chat.Component;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.server.level.ServerPlayer;
import net.minecraftforge.event.RegisterCommandsEvent;
import net.minecraftforge.eventbus.api.SubscribeEvent;
import net.minecraftforge.fml.common.Mod;
import net.minecraftforge.network.PacketDistributor;
import org.xam.XamConstants;
import org.xam.config.ConfigManager;
import org.xam.config.PathInfo;
import org.xam.config.Requirement;
import org.xam.data.PlayerDataProvider;
import org.xam.network.SyncConfigPacket;
import org.xam.network.XamNetwork;
import org.xam.progression.MasteryService;
import org.xam.progression.RequirementFormatter;

@Mod.EventBusSubscriber(modid = XamConstants.MODID, bus = Mod.EventBusSubscriber.Bus.FORGE)
public class XamCommand {

    @SubscribeEvent
    public static void onRegisterCommands(RegisterCommandsEvent event) {
        CommandDispatcher<CommandSourceStack> dispatcher = event.getDispatcher();
        LiteralArgumentBuilder<CommandSourceStack> xamCommand = Commands.literal("xam");

        // dev subcommand
        xamCommand.then(Commands.literal("dev")
            .requires(source -> source.hasPermission(2))
            .executes(context -> {
                ServerPlayer player = context.getSource().getPlayerOrException();
                player.getCapability(PlayerDataProvider.PLAYER_DATA).ifPresent(data -> {
                    boolean newDev = !data.isDevMode();
                    data.setDevMode(newDev);
                    MasteryService.sync(player);
                    MasteryService.updateArmorModifiers(player);
                    context.getSource().sendSuccess(() -> Component.translatable("xam.msg.dev_mode_format",
                            Component.translatable(newDev ? "xam.msg.dev_mode_on" : "xam.msg.dev_mode_off"),
                            player.getGameProfile().getName()), true);
                });
                return 1;
            })
        );

        // info subcommand
        xamCommand.then(Commands.literal("info")
            .executes(context -> {
                ServerPlayer player = context.getSource().getPlayerOrException();
                printPlayerInfo(context.getSource(), player);
                return 1;
            })
            .then(Commands.argument("player", EntityArgument.player())
                .requires(source -> source.hasPermission(2))
                .executes(context -> {
                    ServerPlayer player = EntityArgument.getPlayer(context, "player");
                    printPlayerInfo(context.getSource(), player);
                    return 1;
                })
            )
        );

        // reset subcommand
        xamCommand.then(Commands.literal("reset")
            .requires(source -> source.hasPermission(2))
            .then(Commands.argument("player", EntityArgument.player())
                .executes(context -> {
                    ServerPlayer player = EntityArgument.getPlayer(context, "player");
                    player.getCapability(PlayerDataProvider.PLAYER_DATA).ifPresent(data -> {
                        data.clearAll();
                        MasteryService.sync(player);
                        MasteryService.updateArmorModifiers(player);
                        context.getSource().sendSuccess(() -> Component.translatable("xam.msg.reset_announcement", player.getGameProfile().getName()), true);
                    });
                    return 1;
                })
            )
        );

        // master subcommand
        xamCommand.then(Commands.literal("master")
            .requires(source -> source.hasPermission(2))
            .then(Commands.argument("path_id", ResourceLocationArgument.id())
                .suggests((context, builder) -> {
                    for (PathInfo path : ConfigManager.PATHS) {
                        builder.suggest(path.getId());
                    }
                    return builder.buildFuture();
                })
                .executes(context -> {
                    ServerPlayer player = context.getSource().getPlayerOrException();
                    ResourceLocation rl = ResourceLocationArgument.getId(context, "path_id");
                    String pathId = rl.getPath();
                    masterPath(context.getSource(), player, pathId, true);
                    return 1;
                })
                .then(Commands.argument("player", EntityArgument.player())
                    .executes(context -> {
                        ServerPlayer player = EntityArgument.getPlayer(context, "player");
                        ResourceLocation rl = ResourceLocationArgument.getId(context, "path_id");
                        String pathId = rl.getPath();
                        masterPath(context.getSource(), player, pathId, true);
                        return 1;
                    })
                )
                .then(Commands.argument("mastered", BoolArgumentType.bool())
                    .executes(context -> {
                        ServerPlayer player = context.getSource().getPlayerOrException();
                        ResourceLocation rl = ResourceLocationArgument.getId(context, "path_id");
                        String pathId = rl.getPath();
                        boolean mastered = BoolArgumentType.getBool(context, "mastered");
                        masterPath(context.getSource(), player, pathId, mastered);
                        return 1;
                    })
                    .then(Commands.argument("player", EntityArgument.player())
                        .executes(context -> {
                            ServerPlayer player = EntityArgument.getPlayer(context, "player");
                            ResourceLocation rl = ResourceLocationArgument.getId(context, "path_id");
                            String pathId = rl.getPath();
                            boolean mastered = BoolArgumentType.getBool(context, "mastered");
                            masterPath(context.getSource(), player, pathId, mastered);
                            return 1;
                        })
                    )
                )
            )
        );

        // select subcommand
        xamCommand.then(Commands.literal("select")
            .requires(source -> source.hasPermission(2))
            .then(Commands.argument("path_id", ResourceLocationArgument.id())
                .suggests((context, builder) -> {
                    for (PathInfo path : ConfigManager.PATHS) {
                        builder.suggest(path.getId());
                    }
                    return builder.buildFuture();
                })
                .executes(context -> {
                    ServerPlayer player = context.getSource().getPlayerOrException();
                    ResourceLocation rl = ResourceLocationArgument.getId(context, "path_id");
                    String pathId = rl.getPath();
                    selectPath(context.getSource(), player, pathId);
                    return 1;
                })
                .then(Commands.argument("player", EntityArgument.player())
                    .executes(context -> {
                        ServerPlayer player = EntityArgument.getPlayer(context, "player");
                        ResourceLocation rl = ResourceLocationArgument.getId(context, "path_id");
                        String pathId = rl.getPath();
                        selectPath(context.getSource(), player, pathId);
                        return 1;
                    })
                )
            )
        );

        // reload subcommand
        xamCommand.then(Commands.literal("reload")
            .requires(source -> source.hasPermission(2))
            .executes(context -> {
                ConfigManager.loadConfig();
                String pathsJson = ConfigManager.getPathsJson();
                XamNetwork.CHANNEL.send(PacketDistributor.ALL.noArg(), new SyncConfigPacket(pathsJson));
                for (ServerPlayer player : context.getSource().getServer().getPlayerList().getPlayers()) {
                    player.getCapability(PlayerDataProvider.PLAYER_DATA).ifPresent(data -> {
                        MasteryService.checkAndRefreshPlayerData(player, data);
                        MasteryService.updateArmorModifiers(player);
                    });
                }
                context.getSource().sendSuccess(() -> Component.translatable("xam.msg.reload_success"), true);
                return 1;
            })
        );

        // progress subcommand
        xamCommand.then(Commands.literal("progress")
            .executes(context -> {
                ServerPlayer player = context.getSource().getPlayerOrException();
                showPlayerProgress(context.getSource(), player);
                return 1;
            })
            .then(Commands.argument("player", EntityArgument.player())
                .requires(source -> source.hasPermission(2))
                .executes(context -> {
                    ServerPlayer player = EntityArgument.getPlayer(context, "player");
                    showPlayerProgress(context.getSource(), player);
                    return 1;
                })
            )
        );

        // started subcommand
        xamCommand.then(Commands.literal("started")
            .executes(context -> {
                ServerPlayer player = context.getSource().getPlayerOrException();
                showStartedPaths(context.getSource(), player);
                return 1;
            })
            .then(Commands.argument("player", EntityArgument.player())
                .requires(source -> source.hasPermission(2))
                .executes(context -> {
                    ServerPlayer player = EntityArgument.getPlayer(context, "player");
                    showStartedPaths(context.getSource(), player);
                    return 1;
                })
            )
        );

        // complete_req subcommand
        xamCommand.then(Commands.literal("complete_req")
            .requires(source -> source.hasPermission(2))
            .then(Commands.argument("requirement", StringArgumentType.string())
                .suggests((context, builder) -> {
                    try {
                        ServerPlayer player = context.getSource().getPlayerOrException();
                        player.getCapability(PlayerDataProvider.PLAYER_DATA).ifPresent(data -> {
                            String currentPath = data.getCurrentPath();
                            if (currentPath != null) {
                                PathInfo path = ConfigManager.PATHS_MAP.get(currentPath);
                                if (path != null) {
                                    for (Requirement req : path.getRequirements()) {
                                        builder.suggest(MasteryService.getRequirementShortKey(req));
                                    }
                                }
                            }
                        });
                    } catch (Exception ignored) {}
                    return builder.buildFuture();
                })
                .executes(context -> {
                    ServerPlayer player = context.getSource().getPlayerOrException();
                    String reqKey = StringArgumentType.getString(context, "requirement");
                    completeRequirement(context.getSource(), player, reqKey);
                    return 1;
                })
                .then(Commands.argument("player", EntityArgument.player())
                    .executes(context -> {
                        ServerPlayer player = EntityArgument.getPlayer(context, "player");
                        String reqKey = StringArgumentType.getString(context, "requirement");
                        completeRequirement(context.getSource(), player, reqKey);
                        return 1;
                    })
                )
            )
        );

        // deletepath subcommand
        xamCommand.then(Commands.literal("deletepath")
            .requires(source -> source.hasPermission(2))
            .then(Commands.argument("path_id", ResourceLocationArgument.id())
                .suggests((context, builder) -> {
                    for (PathInfo path : ConfigManager.PATHS) {
                        builder.suggest(path.getId());
                    }
                    return builder.buildFuture();
                })
                .executes(context -> {
                    ResourceLocation rl = ResourceLocationArgument.getId(context, "path_id");
                    String pathId = rl.getPath();
                    deletePath(context.getSource(), pathId);
                    return 1;
                })
            )
        );

        // help subcommand
        xamCommand.then(Commands.literal("help")
            .executes(context -> {
                CommandSourceStack source = context.getSource();
                boolean isOp = source.hasPermission(2);
                source.sendSuccess(() -> Component.translatable("xam.cmd.help_header"), false);
                source.sendSuccess(() -> Component.translatable("xam.cmd.help_info"), false);
                source.sendSuccess(() -> Component.translatable("xam.cmd.help_progress"), false);
                source.sendSuccess(() -> Component.translatable("xam.cmd.help_started"), false);
                source.sendSuccess(() -> Component.translatable("xam.cmd.help_help"), false);
                if (isOp) {
                    source.sendSuccess(() -> Component.translatable("xam.cmd.help_dev"), false);
                    source.sendSuccess(() -> Component.translatable("xam.cmd.help_select"), false);
                    source.sendSuccess(() -> Component.translatable("xam.cmd.help_master"), false);
                    source.sendSuccess(() -> Component.translatable("xam.cmd.help_deletepath"), false);
                    source.sendSuccess(() -> Component.translatable("xam.cmd.help_complete_req"), false);
                    source.sendSuccess(() -> Component.translatable("xam.cmd.help_reset"), false);
                    source.sendSuccess(() -> Component.translatable("xam.cmd.help_reload"), false);
                }
                return 1;
            })
        );

        dispatcher.register(xamCommand);
    }

    private static void printPlayerInfo(CommandSourceStack source, ServerPlayer player) {
        player.getCapability(PlayerDataProvider.PLAYER_DATA).ifPresent(data -> {
            final String active = data.getCurrentPath() != null ? data.getCurrentPath() : Component.translatable("xam.screen.mastery_hub.none").getString();
            final String mastered = data.getMasteredPaths().isEmpty() ? Component.translatable("xam.screen.mastery_hub.none").getString() : String.join(", ", data.getMasteredPaths());
            final boolean devMode = data.isDevMode();
            source.sendSuccess(() -> Component.translatable("xam.msg.info_header", player.getGameProfile().getName()), false);
            source.sendSuccess(() -> Component.translatable("xam.msg.info_active", active), false);
            source.sendSuccess(() -> Component.translatable("xam.msg.info_mastered", mastered), false);
            source.sendSuccess(() -> Component.translatable("xam.msg.info_dev_mode", (devMode ? Component.translatable("xam.msg.dev_mode_on") : Component.translatable("xam.msg.dev_mode_off"))), false);
        });
    }

    private static void masterPath(CommandSourceStack source, ServerPlayer player, String pathId, boolean mastered) {
        player.getCapability(PlayerDataProvider.PLAYER_DATA).ifPresent(data -> {
            boolean exists = ConfigManager.PATHS_MAP.containsKey(pathId);
            if (!exists) {
                source.sendFailure(Component.translatable("xam.msg.path_not_exists", pathId));
                return;
            }
            if (mastered) {
                data.addMasteredPath(pathId);
                PathInfo path = ConfigManager.PATHS_MAP.get(pathId);
                if (path != null) {
                    for (Requirement req : path.getRequirements()) {
                        data.addCompletedRequirement(MasteryService.getRequirementKey(pathId, req));
                    }
                }
                if (pathId.equals(data.getCurrentPath())) {
                    data.setCurrentPath(null);
                }
                MasteryService.sync(player);
                MasteryService.updateArmorModifiers(player);
                source.sendSuccess(() -> Component.translatable("xam.msg.path_mastered_success", pathId, player.getGameProfile().getName()), true);
            } else {
                data.removeMasteredPath(pathId);
                data.removeCompletedRequirementsIf(k -> k.startsWith(pathId + ":"));
                MasteryService.sync(player);
                MasteryService.updateArmorModifiers(player);
                source.sendSuccess(() -> Component.translatable("xam.msg.path_unmastered_success", pathId, player.getGameProfile().getName()), true);
            }
        });
    }

    private static void selectPath(CommandSourceStack source, ServerPlayer player, String pathId) {
        player.getCapability(PlayerDataProvider.PLAYER_DATA).ifPresent(data -> {
            PathInfo targetPath = ConfigManager.PATHS_MAP.get(pathId);
            if (targetPath == null) {
                source.sendFailure(Component.translatable("xam.msg.path_not_exists", pathId));
                return;
            }
            String oldPath = data.getCurrentPath();
            if (oldPath != null && !data.getMasteredPaths().contains(oldPath)) {
                data.removeCompletedRequirementsIf(k -> k.startsWith(oldPath + ":"));
            }
            data.setCurrentPath(pathId);
            MasteryService.sync(player);
            MasteryService.updateArmorModifiers(player);
            source.sendSuccess(() -> Component.translatable("xam.msg.path_selected_success", pathId, player.getGameProfile().getName()), true);
        });
    }

    private static void showPlayerProgress(CommandSourceStack source, ServerPlayer player) {
        player.getCapability(PlayerDataProvider.PLAYER_DATA).ifPresent(data -> {
            String currentPath = data.getCurrentPath();
            if (currentPath == null) {
                source.sendSuccess(() -> Component.translatable("xam.msg.no_active_mastery", player.getGameProfile().getName()), false);
                return;
            }
            PathInfo pathInfo = ConfigManager.PATHS_MAP.get(currentPath);
            if (pathInfo == null) {
                source.sendSuccess(() -> Component.translatable("xam.msg.invalid_active_mastery", player.getGameProfile().getName()), false);
                return;
            }
            source.sendSuccess(() -> Component.translatable("xam.msg.progress_header", player.getGameProfile().getName(), pathInfo.getName()), false);
            for (Requirement req : pathInfo.getRequirements()) {
                boolean done = MasteryService.isRequirementCompleted(player, data, pathInfo.getId(), req);
                String symbol = done ? "§a[✔]§r" : "§c[✘]§r";
                String reqDesc = RequirementFormatter.formatRequirementDescription(req);
                source.sendSuccess(() -> Component.literal(symbol + " " + reqDesc + " (" + MasteryService.getRequirementShortKey(req) + ")"), false);
            }
        });
    }

    private static void showStartedPaths(CommandSourceStack source, ServerPlayer player) {
        player.getCapability(PlayerDataProvider.PLAYER_DATA).ifPresent(data -> {
            java.util.List<String> started = new java.util.ArrayList<>(data.getStartedPaths());
            started.removeAll(data.getMasteredPaths());
            if (started.isEmpty()) {
                source.sendSuccess(() -> Component.translatable("xam.msg.no_started_paths", player.getGameProfile().getName()), false);
                return;
            }
            source.sendSuccess(() -> Component.translatable("xam.msg.started_paths_header", player.getGameProfile().getName()), false);
            for (String pathId : started) {
                PathInfo pathInfo = ConfigManager.PATHS_MAP.get(pathId);
                String displayName = pathInfo != null ? pathInfo.getName() : pathId;
                source.sendSuccess(() -> Component.literal("- " + displayName + " (" + pathId + ")"), false);
            }
        });
    }

    private static void deletePath(CommandSourceStack source, String pathId) {
        boolean inConfig = ConfigManager.PATHS_MAP.containsKey(pathId);
        if (inConfig) {
            java.util.List<PathInfo> updatedPaths = new java.util.ArrayList<>(ConfigManager.PATHS);
            updatedPaths.removeIf(p -> p.getId().equals(pathId));

            String json = ConfigManager.serializePaths(updatedPaths);
            ConfigManager.saveConfigFromServer(source.getServer(), json);
        }

        for (ServerPlayer player : source.getServer().getPlayerList().getPlayers()) {
            player.getCapability(PlayerDataProvider.PLAYER_DATA).ifPresent(data -> {
                boolean modified = false;
                if (pathId.equals(data.getCurrentPath())) {
                    data.setCurrentPath(null);
                    data.removeCompletedRequirementsIf(k -> k.startsWith(pathId + ":"));
                    modified = true;
                }
                if (data.getMasteredPaths().contains(pathId)) {
                    data.removeMasteredPath(pathId);
                    modified = true;
                }
                if (data.getStartedPaths().contains(pathId)) {
                    data.removeStartedPath(pathId);
                    modified = true;
                }
                if (modified) {
                    MasteryService.sync(player);
                    MasteryService.updateArmorModifiers(player);
                }
            });
        }

        if (inConfig) {
            source.sendSuccess(() -> Component.translatable("xam.msg.path_deleted_success", pathId), true);
        } else {
            source.sendSuccess(() -> Component.literal("The path '" + pathId + "' was not in the config file, but has been cleaned up from all online players."), true);
        }
    }

    private static void completeRequirement(CommandSourceStack source, ServerPlayer player, String reqKey) {
        player.getCapability(PlayerDataProvider.PLAYER_DATA).ifPresent(data -> {
            String currentPath = data.getCurrentPath();
            if (currentPath == null) {
                source.sendFailure(Component.translatable("xam.msg.no_active_mastery", player.getGameProfile().getName()));
                return;
            }
            PathInfo pathInfo = ConfigManager.PATHS_MAP.get(currentPath);
            if (pathInfo == null) {
                source.sendFailure(Component.translatable("xam.msg.invalid_mastery"));
                return;
            }
            Requirement foundReq = null;
            for (Requirement req : pathInfo.getRequirements()) {
                if (MasteryService.getRequirementShortKey(req).equals(reqKey)) {
                    foundReq = req;
                    break;
                }
            }
            if (foundReq == null) {
                source.sendFailure(Component.translatable("xam.msg.req_not_in_path", reqKey, currentPath));
                return;
            }
            if (foundReq.getType().equals("advancement")) {
                ResourceLocation resLoc = ResourceLocation.tryParse(foundReq.getId());
                if (resLoc != null) {
                    net.minecraft.advancements.Advancement adv = player.server.getAdvancements().getAdvancement(resLoc);
                    if (adv != null) {
                        net.minecraft.advancements.AdvancementProgress progress = player.getAdvancements().getOrStartProgress(adv);
                        if (!progress.isDone()) {
                            for (String criteria : progress.getRemainingCriteria()) {
                                player.getAdvancements().award(adv, criteria);
                            }
                        }
                    }
                }
            } else {
                String fullKey = MasteryService.getRequirementKey(pathInfo.getId(), foundReq);
                if (!data.getCompletedRequirements().contains(fullKey)) {
                    data.addCompletedRequirement(fullKey);
                    data.addStartedPath(pathInfo.getId());
                    MasteryService.sync(player);
                    MasteryService.checkPathCompletion(player, data, pathInfo);
                }
            }
            source.sendSuccess(() -> Component.translatable("xam.msg.req_completed_success", reqKey, player.getGameProfile().getName()), true);
        });
    }
}
