package org.hotamachisubaru.miniutility.forge.nickname;

import net.minecraft.network.chat.Component;
import net.minecraft.server.MinecraftServer;
import net.minecraft.server.level.ServerPlayer;
import net.minecraftforge.fml.loading.FMLPaths;
import org.hotamachisubaru.miniutility.forge.util.ForgeComponentUtil;
import org.hotamachisubaru.miniutility.forge.util.ForgeLuckPermsUtil;

import java.nio.file.Path;
import java.util.Map;
import java.util.UUID;
import java.util.concurrent.ConcurrentHashMap;

public final class ForgeNicknameManager {
    private final Map<UUID, String> nicknameMap = new ConcurrentHashMap<>();
    private final Map<UUID, Boolean> prefixEnabled = new ConcurrentHashMap<>();
    private final ForgeNicknameDatabase nicknameDatabase;
    private final ForgeLuckPermsUtil luckPermsUtil;

    public ForgeNicknameManager(ForgeLuckPermsUtil luckPermsUtil) {
        this.luckPermsUtil = luckPermsUtil;
        Path dbPath = FMLPaths.GAMEDIR.get().resolve("config").resolve("miniutility").resolve("nickname.db");
        this.nicknameDatabase = new ForgeNicknameDatabase(dbPath);
        reload(null);
    }

    public void reload(MinecraftServer server) {
        this.nicknameMap.clear();
        this.nicknameMap.putAll(this.nicknameDatabase.loadAll());

        if (server != null) {
            refreshOnlinePlayers(server);
        }
    }

    public void persistAll() {
        this.nicknameDatabase.saveAll(this.nicknameMap);
    }

    public void refreshOnlinePlayers(MinecraftServer server) {
        server.getPlayerList().getPlayers().forEach(this::updateDisplayName);
    }

    public void setNickname(ServerPlayer player, String nickname) {
        if (player == null || nickname == null) {
            return;
        }

        this.nicknameMap.put(player.getUUID(), nickname);
        this.nicknameDatabase.saveNickname(player.getUUID(), nickname);
        updateDisplayName(player);
    }

    public void removeNickname(ServerPlayer player) {
        if (player == null) {
            return;
        }

        this.nicknameMap.remove(player.getUUID());
        this.nicknameDatabase.deleteNickname(player.getUUID());
        updateDisplayName(player);
    }

    public String getDisplayName(ServerPlayer player) {
        if (player == null) {
            return "";
        }
        return this.nicknameMap.getOrDefault(player.getUUID(), player.getGameProfile().getName());
    }

    public Component getDisplayNameComponent(ServerPlayer player) {
        if (player == null) {
            return Component.literal("");
        }

        String nickname = getDisplayName(player);
        String prefix = isPrefixEnabled(player.getUUID()) ? this.luckPermsUtil.safePrefix(player) : "";
        if (!prefix.isEmpty() && nickname.startsWith(prefix)) {
            prefix = "";
        }

        String displayText = prefix.isEmpty() ? nickname : prefix + "&r " + nickname;
        return ForgeComponentUtil.fromLegacy(ForgeComponentUtil.ampersandToSection(displayText));
    }

    public void updateDisplayName(ServerPlayer player) {
        if (player == null) {
            return;
        }

        Component displayName = getDisplayNameComponent(player);
        player.setCustomName(displayName);
        player.setCustomNameVisible(true);
        player.refreshDisplayName();
        player.refreshTabListName();
    }

    public boolean togglePrefix(UUID uniqueId, ServerPlayer player) {
        boolean nextState = !isPrefixEnabled(uniqueId);
        setPrefixEnabled(uniqueId, nextState, player);
        return nextState;
    }

    public void setPrefixEnabled(UUID uniqueId, boolean enabled, ServerPlayer player) {
        if (uniqueId == null) {
            return;
        }

        this.prefixEnabled.put(uniqueId, enabled);
        if (player != null) {
            updateDisplayName(player);
        }
    }

    public boolean setColor(ServerPlayer player, String colorName) {
        if (player == null || colorName == null) {
            return false;
        }

        String colorCode = mapColorName(colorName);
        if (colorCode == null) {
            return false;
        }

        UUID uniqueId = player.getUUID();
        String nickname = this.nicknameMap.get(uniqueId);
        if (nickname == null || nickname.isEmpty()) {
            return false;
        }

        String base = ForgeNicknameValidator.stripLeadingLegacyCodes(nickname);
        String recolored = "§" + colorCode + base;
        this.nicknameMap.put(uniqueId, recolored);
        this.nicknameDatabase.saveNickname(uniqueId, recolored);
        updateDisplayName(player);
        return true;
    }

    private boolean isPrefixEnabled(UUID uniqueId) {
        return this.prefixEnabled.getOrDefault(uniqueId, true);
    }

    private static String mapColorName(String colorName) {
        return switch (colorName.toLowerCase()) {
            case "black" -> "0";
            case "dark_blue" -> "1";
            case "dark_green" -> "2";
            case "dark_aqua" -> "3";
            case "dark_red" -> "4";
            case "dark_purple" -> "5";
            case "gold" -> "6";
            case "gray" -> "7";
            case "dark_gray" -> "8";
            case "blue" -> "9";
            case "green" -> "a";
            case "aqua" -> "b";
            case "red" -> "c";
            case "purple", "light_purple" -> "d";
            case "yellow" -> "e";
            case "white" -> "f";
            default -> null;
        };
    }
}
