package org.hotamachisubaru.miniutility.forge.chat;

import net.minecraft.ChatFormatting;
import net.minecraft.network.chat.Component;
import net.minecraft.server.level.ServerPlayer;
import net.minecraftforge.event.ServerChatEvent;
import net.minecraftforge.event.entity.player.PlayerEvent;
import net.minecraftforge.eventbus.api.EventPriority;
import net.minecraftforge.eventbus.api.SubscribeEvent;
import org.hotamachisubaru.miniutility.forge.GUI.ForgeGuiActionService;
import org.hotamachisubaru.miniutility.forge.nickname.ForgeNicknameManager;
import org.hotamachisubaru.miniutility.forge.nickname.ForgeNicknameValidator;
import org.hotamachisubaru.miniutility.forge.util.ForgeComponentUtil;

import java.util.Map;
import java.util.UUID;
import java.util.concurrent.ConcurrentHashMap;

public final class ForgeChatListener {
    private final ForgeNicknameManager nicknameManager;
    private final ForgeGuiActionService guiActionService;
    private final Map<UUID, Boolean> waitingForNickname = new ConcurrentHashMap<>();
    private final Map<UUID, Boolean> waitingForColorInput = new ConcurrentHashMap<>();
    private final Map<UUID, Boolean> waitingForExpInput = new ConcurrentHashMap<>();

    public ForgeChatListener(ForgeNicknameManager nicknameManager, ForgeGuiActionService guiActionService) {
        this.nicknameManager = nicknameManager;
        this.guiActionService = guiActionService;
    }

    public void awaitNicknameInput(ServerPlayer player) {
        setWaitingState(this.waitingForNickname, player, true);
    }

    public void awaitColorNicknameInput(ServerPlayer player) {
        setWaitingState(this.waitingForColorInput, player, true);
    }

    public void awaitExpInput(ServerPlayer player) {
        setWaitingState(this.waitingForExpInput, player, true);
    }

    @SubscribeEvent(priority = EventPriority.HIGHEST)
    public void onServerChat(ServerChatEvent event) {
        ServerPlayer player = event.getPlayer();
        String plainMessage = event.getRawText();
        if (tryHandleWaitingInput(player, plainMessage)) {
            event.setCanceled(true);
            return;
        }

        event.setMessage(Component.literal("")
                .append(this.nicknameManager.getDisplayNameComponent(player))
                .append(Component.literal(": ").withStyle(ChatFormatting.GRAY))
                .append(Component.literal(plainMessage)));
    }

    @SubscribeEvent
    public void onPlayerLoggedIn(PlayerEvent.PlayerLoggedInEvent event) {
        if (event.getEntity() instanceof ServerPlayer player) {
            this.nicknameManager.updateDisplayName(player);
        }
    }

    @SubscribeEvent
    public void onPlayerLoggedOut(PlayerEvent.PlayerLoggedOutEvent event) {
        if (event.getEntity() instanceof ServerPlayer player) {
            clearWaitingState(player.getUUID());
        }
    }

    @SubscribeEvent
    public void onNameFormat(PlayerEvent.NameFormat event) {
        if (event.getEntity() instanceof ServerPlayer player) {
            event.setDisplayname(this.nicknameManager.getDisplayNameComponent(player));
        }
    }

    @SubscribeEvent
    public void onTabListNameFormat(PlayerEvent.TabListNameFormat event) {
        if (event.getEntity() instanceof ServerPlayer player) {
            event.setDisplayName(this.nicknameManager.getDisplayNameComponent(player));
        }
    }

    private boolean tryHandleWaitingInput(ServerPlayer player, String plainMessage) {
        if (player == null) {
            return false;
        }

        if (isWaiting(this.waitingForExpInput, player)) {
            String input = plainMessage == null ? "" : plainMessage.trim();
            try {
                int change = Integer.parseInt(input);
                setWaitingState(this.waitingForExpInput, player, false);
                this.guiActionService.changeExperienceLevels(player, change);
            } catch (NumberFormatException exception) {
                player.sendSystemMessage(Component.literal("数値を入力してください。例: 99 または -5"));
            }
            return true;
        }

        if (isWaiting(this.waitingForNickname, player)) {
            String input = plainMessage == null ? "" : plainMessage.trim();
            String validated = ForgeNicknameValidator.validatePlain(input);
            if (validated != null) {
                setWaitingState(this.waitingForNickname, player, false);
                this.nicknameManager.setNickname(player, validated);
                player.sendSystemMessage(Component.literal("ニックネームを「" + validated + "」に設定しました。"));
            } else {
                player.sendSystemMessage(Component.literal("無効なニックネームです。1〜16文字、記号は _- のみ使用可。空白不可。"));
            }
            return true;
        }

        if (isWaiting(this.waitingForColorInput, player)) {
            String raw = plainMessage == null ? "" : plainMessage.trim();
            if (raw.isEmpty()) {
                player.sendSystemMessage(Component.literal("例: &6a, &bほたまち"));
                return true;
            }

            String visible = ForgeNicknameValidator.visibleWithoutLegacyCodes(raw);
            if (ForgeNicknameValidator.validatePlain(visible) == null) {
                player.sendSystemMessage(Component.literal("無効なニックネームです。1〜16文字、記号は _- のみ、空白不可。"));
                return true;
            }

            setWaitingState(this.waitingForColorInput, player, false);
            String coloredNickname = ForgeComponentUtil.ampersandToSection(raw);
            this.nicknameManager.setNickname(player, coloredNickname);
            player.sendSystemMessage(Component.literal("ニックネームを設定しました: ")
                    .append(ForgeComponentUtil.fromLegacy(coloredNickname)));
            return true;
        }

        return false;
    }

    private void clearWaitingState(UUID uniqueId) {
        this.waitingForNickname.remove(uniqueId);
        this.waitingForColorInput.remove(uniqueId);
        this.waitingForExpInput.remove(uniqueId);
    }

    private static boolean isWaiting(Map<UUID, Boolean> state, ServerPlayer player) {
        return state.getOrDefault(player.getUUID(), false);
    }

    private static void setWaitingState(Map<UUID, Boolean> state, ServerPlayer player, boolean waiting) {
        if (player == null) {
            return;
        }

        if (waiting) {
            state.put(player.getUUID(), true);
        } else {
            state.remove(player.getUUID());
        }
    }
}
