package org.hotamachisubaru.miniutility.forge.command;

import com.mojang.brigadier.Command;
import com.mojang.brigadier.CommandDispatcher;
import com.mojang.brigadier.arguments.IntegerArgumentType;
import com.mojang.brigadier.arguments.StringArgumentType;
import net.minecraft.commands.CommandSourceStack;
import net.minecraft.commands.Commands;
import net.minecraft.network.chat.Component;
import net.minecraft.server.level.ServerPlayer;
import net.minecraftforge.event.RegisterCommandsEvent;
import net.minecraftforge.eventbus.api.SubscribeEvent;
import org.hotamachisubaru.miniutility.forge.GUI.ForgeGuiActionService;
import org.hotamachisubaru.miniutility.forge.chat.ForgeChatListener;
import org.hotamachisubaru.miniutility.forge.nickname.ForgeNicknameManager;
import org.hotamachisubaru.miniutility.forge.nickname.ForgeNicknameValidator;

import java.util.Locale;

public final class ForgeCommandManager {
    private final ForgeNicknameManager nicknameManager;
    private final ForgeGuiActionService guiActionService;
    private final ForgeChatListener chatListener;

    public ForgeCommandManager(
            ForgeNicknameManager nicknameManager,
            ForgeGuiActionService guiActionService,
            ForgeChatListener chatListener
    ) {
        this.nicknameManager = nicknameManager;
        this.guiActionService = guiActionService;
        this.chatListener = chatListener;
    }

    @SubscribeEvent
    public void onRegisterCommands(RegisterCommandsEvent event) {
        registerCommands(event.getDispatcher());
    }

    public void registerCommands(CommandDispatcher<CommandSourceStack> dispatcher) {
        dispatcher.register(Commands.literal("menu")
                .executes(context -> runForPlayer(context.getSource(), this.guiActionService::sendMenu)));

        dispatcher.register(Commands.literal("load")
                .executes(context -> reloadNicknames(context.getSource())));

        dispatcher.register(Commands.literal("prefixtoggle")
                .executes(context -> togglePrefix(context.getSource(), null))
                .then(Commands.literal("on")
                        .executes(context -> togglePrefix(context.getSource(), Boolean.TRUE)))
                .then(Commands.literal("off")
                        .executes(context -> togglePrefix(context.getSource(), Boolean.FALSE))));

        dispatcher.register(Commands.literal("miniutility")
                .then(Commands.literal("menu")
                        .executes(context -> runForPlayer(context.getSource(), this.guiActionService::sendMenu)))
                .then(Commands.literal("load")
                        .executes(context -> reloadNicknames(context.getSource())))
                .then(Commands.literal("prefixtoggle")
                        .executes(context -> togglePrefix(context.getSource(), null))
                        .then(Commands.literal("on")
                                .executes(context -> togglePrefix(context.getSource(), Boolean.TRUE)))
                        .then(Commands.literal("off")
                                .executes(context -> togglePrefix(context.getSource(), Boolean.FALSE))))
                .then(Commands.literal("nickname")
                        .then(Commands.literal("set")
                                .then(Commands.argument("nickname", StringArgumentType.greedyString())
                                        .executes(context -> setNickname(
                                                context.getSource(),
                                                StringArgumentType.getString(context, "nickname")
                                        ))))
                        .then(Commands.literal("colored")
                                .then(Commands.argument("nickname", StringArgumentType.greedyString())
                                        .executes(context -> setColoredNickname(
                                                context.getSource(),
                                                StringArgumentType.getString(context, "nickname")
                                        ))))
                        .then(Commands.literal("color")
                                .then(Commands.argument("color", StringArgumentType.word())
                                        .executes(context -> setNicknameColor(
                                                context.getSource(),
                                                StringArgumentType.getString(context, "color")
                                        ))))
                        .then(Commands.literal("input")
                                .executes(context -> runForPlayer(context.getSource(), player -> {
                                    this.chatListener.awaitNicknameInput(player);
                                    player.sendSystemMessage(Component.literal("新しいニックネームをチャットに入力してください。"));
                                })))
                        .then(Commands.literal("colorinput")
                                .executes(context -> runForPlayer(context.getSource(), player -> {
                                    this.chatListener.awaitColorNicknameInput(player);
                                    player.sendSystemMessage(Component.literal("色付きのニックネームをチャットに入力してください。例: &6ほたまち"));
                                })))
                        .then(Commands.literal("remove")
                                .executes(context -> runForPlayer(context.getSource(), player -> {
                                    this.nicknameManager.removeNickname(player);
                                    player.sendSystemMessage(Component.literal("ニックネームをリセットしました。"));
                                }))))
                .then(Commands.literal("death")
                        .executes(context -> runForPlayer(context.getSource(), this.guiActionService::openDeathLocationMenu)))
                .then(Commands.literal("enderchest")
                        .executes(context -> runForPlayer(context.getSource(), this.guiActionService::openEnderChest)))
                .then(Commands.literal("crafting")
                        .executes(context -> runForPlayer(context.getSource(), this.guiActionService::openCraftingTable)))
                .then(Commands.literal("trash")
                        .executes(context -> runForPlayer(context.getSource(), this.guiActionService::openTrashBox)))
                .then(Commands.literal("creeper")
                        .executes(context -> runForPlayer(context.getSource(), this.guiActionService::toggleCreeperProtection)))
                .then(Commands.literal("exp")
                        .then(Commands.argument("amount", IntegerArgumentType.integer())
                                .executes(context -> runForPlayer(context.getSource(), player ->
                                        this.guiActionService.changeExperienceLevels(
                                                player,
                                                IntegerArgumentType.getInteger(context, "amount")
                                        )))))
                .then(Commands.literal("gamemode")
                        .executes(context -> runForPlayer(context.getSource(), this.guiActionService::cycleGameMode))));
    }

    private int reloadNicknames(CommandSourceStack source) {
        try {
            this.nicknameManager.reload(source.getServer());
            source.sendSystemMessage(Component.literal("ニックネームデータを再読み込みしました。"));
            return Command.SINGLE_SUCCESS;
        } catch (RuntimeException exception) {
            source.sendSystemMessage(Component.literal("データベース再読み込みに失敗しました: " + exception.getMessage()));
            return 0;
        }
    }

    private int togglePrefix(CommandSourceStack source, Boolean forcedState) {
        ServerPlayer player = getPlayer(source);
        if (player == null) {
            return 0;
        }

        boolean enabled;
        if (forcedState == null) {
            enabled = this.nicknameManager.togglePrefix(player.getUUID(), player);
        } else {
            enabled = forcedState;
            this.nicknameManager.setPrefixEnabled(player.getUUID(), enabled, player);
        }

        player.sendSystemMessage(Component.literal("プレフィックスの表示が " + (enabled ? "有効" : "無効") + " になりました。"));
        return Command.SINGLE_SUCCESS;
    }

    private int setNickname(CommandSourceStack source, String rawNickname) {
        ServerPlayer player = getPlayer(source);
        if (player == null) {
            return 0;
        }

        String nickname = ForgeNicknameValidator.validatePlain(rawNickname == null ? "" : rawNickname.trim());
        if (nickname == null) {
            player.sendSystemMessage(Component.literal("無効なニックネームです。1〜16文字、記号は _- のみ使用可。空白不可。"));
            return 0;
        }

        this.nicknameManager.setNickname(player, nickname);
        player.sendSystemMessage(Component.literal("ニックネームを「" + nickname + "」に設定しました。"));
        return Command.SINGLE_SUCCESS;
    }

    private int setColoredNickname(CommandSourceStack source, String rawNickname) {
        ServerPlayer player = getPlayer(source);
        if (player == null) {
            return 0;
        }

        String input = rawNickname == null ? "" : rawNickname.trim();
        String visible = ForgeNicknameValidator.visibleWithoutLegacyCodes(input);
        if (ForgeNicknameValidator.validatePlain(visible) == null) {
            player.sendSystemMessage(Component.literal("無効なニックネームです。1〜16文字、記号は _- のみ、空白不可。"));
            return 0;
        }

        this.nicknameManager.setNickname(player, input.replace('&', '§'));
        player.sendSystemMessage(Component.literal("色付きニックネームを設定しました。"));
        return Command.SINGLE_SUCCESS;
    }

    private int setNicknameColor(CommandSourceStack source, String colorName) {
        ServerPlayer player = getPlayer(source);
        if (player == null) {
            return 0;
        }

        if (this.nicknameManager.setColor(player, colorName.toLowerCase(Locale.ROOT))) {
            player.sendSystemMessage(Component.literal("ニックネーム色を " + colorName + " に変更しました。"));
            return Command.SINGLE_SUCCESS;
        }

        player.sendSystemMessage(Component.literal("無効な色です。red, green, blue, yellow, purple, white, gray, gold, aqua などを指定してください。"));
        return 0;
    }

    private int runForPlayer(CommandSourceStack source, PlayerAction action) {
        ServerPlayer player = getPlayer(source);
        if (player == null) {
            return 0;
        }

        action.run(player);
        return Command.SINGLE_SUCCESS;
    }

    private static ServerPlayer getPlayer(CommandSourceStack source) {
        ServerPlayer player = source.getPlayer();
        if (player == null) {
            source.sendSystemMessage(Component.literal("プレイヤーのみ使用できます。"));
        }
        return player;
    }

    @FunctionalInterface
    private interface PlayerAction {
        void run(ServerPlayer player);
    }
}
