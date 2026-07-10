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
                        data.setCurrentPath(null);
                        data.getMasteredPaths().clear();
                        data.getStartedPaths().clear();
                        data.clearCompletedRequirements();
                        data.setDevMode(false);
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
                        builder.suggest(path.id);
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
                        builder.suggest(path.id);
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
                                    for (Requirement req : path.requirements) {
                                        builder.suggest(req.type + ":" + req.id);
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

        // help subcommand
        xamCommand.then(Commands.literal("help")
            .executes(context -> {
                CommandSourceStack source = context.getSource();
                boolean isOp = source.hasPermission(2);
                source.sendSuccess(() -> Component.translatable("xam.cmd.help_header"), false);
                source.sendSuccess(() -> Component.translatable("xam.cmd.help_info"), false);
                source.sendSuccess(() -> Component.translatable("xam.cmd.help_progress"), false);
                source.sendSuccess(() -> Component.translatable("xam.cmd.help_help"), false);
                if (isOp) {
                    source.sendSuccess(() -> Component.translatable("xam.cmd.help_dev"), false);
                    source.sendSuccess(() -> Component.translatable("xam.cmd.help_select"), false);
                    source.sendSuccess(() -> Component.translatable("xam.cmd.help_master"), false);
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
                if (pathId.equals(data.getCurrentPath())) {
                    data.setCurrentPath(null);
                    data.clearCompletedRequirements();
                }
                MasteryService.sync(player);
                MasteryService.updateArmorModifiers(player);
                source.sendSuccess(() -> Component.translatable("xam.msg.path_mastered_success", pathId, player.getGameProfile().getName()), true);
            } else {
                data.getMasteredPaths().remove(pathId);
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
            if (oldPath != null) {
                data.getCompletedRequirements().removeIf(k -> k.startsWith(oldPath + ":"));
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
            source.sendSuccess(() -> Component.translatable("xam.msg.progress_header", player.getGameProfile().getName(), pathInfo.name), false);
            for (Requirement req : pathInfo.requirements) {
                boolean done = MasteryService.isRequirementCompleted(player, data, pathInfo.id, req);
                String symbol = done ? "§a[✔]§r" : "§c[✘]§r";
                String reqDesc = RequirementFormatter.formatRequirementDescription(req);
                source.sendSuccess(() -> Component.literal(symbol + " " + reqDesc + " (" + req.type + ":" + req.id + ")"), false);
            }
        });
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
            boolean exists = false;
            String type = "";
            String targetId = "";
            if (reqKey.contains(":")) {
                int firstColon = reqKey.indexOf(":");
                type = reqKey.substring(0, firstColon);
                targetId = reqKey.substring(firstColon + 1);
            }
            for (Requirement req : pathInfo.requirements) {
                if (req.type.equals(type) && req.id.equals(targetId)) {
                    exists = true;
                    break;
                }
            }
            if (!exists) {
                source.sendFailure(Component.translatable("xam.msg.req_not_in_path", reqKey, currentPath));
                return;
            }
            if (type.equals("advancement")) {
                ResourceLocation resLoc = ResourceLocation.tryParse(targetId);
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
                String fullKey = pathInfo.id + ":" + reqKey;
                if (!data.getCompletedRequirements().contains(fullKey)) {
                    data.addCompletedRequirement(fullKey);
                    MasteryService.sync(player);
                    MasteryService.checkPathCompletion(player, data, pathInfo);
                }
            }
            source.sendSuccess(() -> Component.translatable("xam.msg.req_completed_success", reqKey, player.getGameProfile().getName()), true);
        });
    }
}
