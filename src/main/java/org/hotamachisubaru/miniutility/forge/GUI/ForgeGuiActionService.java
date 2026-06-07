package org.hotamachisubaru.miniutility.forge.GUI;

import net.minecraft.network.chat.Component;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.SimpleContainer;
import net.minecraft.world.SimpleMenuProvider;
import net.minecraft.world.inventory.ChestMenu;
import net.minecraft.world.inventory.ContainerLevelAccess;
import net.minecraft.world.inventory.CraftingMenu;
import net.minecraft.world.level.GameType;
import org.hotamachisubaru.miniutility.forge.creeper.ForgeCreeperProtectionService;
import org.hotamachisubaru.miniutility.forge.death.DeathLocationStore;
import org.hotamachisubaru.miniutility.forge.nickname.ForgeNicknameManager;

public final class ForgeGuiActionService {
    private final ForgeNicknameManager nicknameManager;
    private final ForgeCreeperProtectionService creeperProtectionService;
    private final DeathLocationStore deathLocationStore;
    private final ForgeTrashBoxSessionStore trashBoxSessionStore;

    public ForgeGuiActionService(
            ForgeNicknameManager nicknameManager,
            ForgeCreeperProtectionService creeperProtectionService,
            DeathLocationStore deathLocationStore,
            ForgeTrashBoxSessionStore trashBoxSessionStore
    ) {
        this.nicknameManager = nicknameManager;
        this.creeperProtectionService = creeperProtectionService;
        this.deathLocationStore = deathLocationStore;
        this.trashBoxSessionStore = trashBoxSessionStore;
    }

    public void sendMenu(ServerPlayer player) {
        openMainMenu(player);
    }

    public void openMainMenu(ServerPlayer player) {
        if (player == null || player.isDeadOrDying()) {
            return;
        }

        player.openMenu(new SimpleMenuProvider(
                (containerId, inventory, owner) -> new ForgeMainMenu(containerId, inventory, this),
                Component.literal("Miniutility")
        ));
    }

    public void openDeathLocationMenu(ServerPlayer player) {
        if (player == null || player.isDeadOrDying()) {
            return;
        }

        player.openMenu(new SimpleMenuProvider(
                (containerId, inventory, owner) -> new ForgeDeathLocationMenu(containerId, inventory, this),
                Component.literal("死亡地点")
        ));
    }

    public void openTrashBox(ServerPlayer player) {
        if (player == null || player.isDeadOrDying()) {
            return;
        }

        this.trashBoxSessionStore.startSession(player.getUUID());
        SimpleContainer trash = new SimpleContainer(27);
        player.openMenu(new SimpleMenuProvider(
                (containerId, inventory, owner) -> new ForgeTrashBoxMenu(containerId, inventory, trash),
                Component.literal("ゴミ箱")
        ));
        player.sendSystemMessage(Component.literal("このゴミ箱に入れて閉じたアイテムは削除されます。"));
    }

    public void teleportToDeathLocation(ServerPlayer player) {
        if (player == null || player.isDeadOrDying()) {
            return;
        }

        DeathLocationStore.DeathLocation deathLocation = this.deathLocationStore.getDeathLocation(player.getUUID());
        if (deathLocation == null) {
            player.sendSystemMessage(Component.literal("死亡地点が見つかりません。"));
            return;
        }

        ServerLevel targetLevel = player.server.getLevel(deathLocation.dim());
        if (targetLevel == null) {
            player.sendSystemMessage(Component.literal("死亡地点のディメンションが見つかりません。"));
            return;
        }

        player.closeContainer();
        player.teleportTo(
                targetLevel,
                deathLocation.pos().x,
                deathLocation.pos().y,
                deathLocation.pos().z,
                deathLocation.yaw(),
                player.getXRot()
        );
        player.sendSystemMessage(Component.literal("死亡地点にワープしました。"));
    }

    public void openEnderChest(ServerPlayer player) {
        if (player == null || player.isDeadOrDying()) {
            return;
        }

        player.openMenu(new SimpleMenuProvider(
                (containerId, inventory, owner) -> ChestMenu.threeRows(containerId, inventory, player.getEnderChestInventory()),
                Component.translatable("container.enderchest")
        ));
    }

    public void openCraftingTable(ServerPlayer player) {
        if (player == null || player.isDeadOrDying()) {
            return;
        }

        player.openMenu(new SimpleMenuProvider(
                (containerId, inventory, owner) -> new CraftingMenu(
                        containerId,
                        inventory,
                        ContainerLevelAccess.create(player.level(), player.blockPosition())
                ),
                Component.literal("作業台")
        ));
    }

    public void toggleCreeperProtection(ServerPlayer player) {
        boolean enabled = this.creeperProtectionService.toggleProtection();
        player.sendSystemMessage(Component.literal(
                "クリーパーの爆破によるブロック破壊防止が " + (enabled ? "有効" : "無効") + " になりました。"
        ));
    }

    public void changeExperienceLevels(ServerPlayer player, int amount) {
        if (player == null || player.isDeadOrDying()) {
            return;
        }

        int beforeLevel = player.experienceLevel;
        int appliedChange = Math.max(-beforeLevel, amount);
        player.giveExperienceLevels(appliedChange);
        int afterLevel = player.experienceLevel;
        String deltaText = (appliedChange > 0 ? "+" : "") + appliedChange;
        player.sendSystemMessage(Component.literal("経験値レベルを " + deltaText + " しました。現在レベル: " + afterLevel));
    }

    public void cycleGameMode(ServerPlayer player) {
        if (player == null || player.isDeadOrDying()) {
            return;
        }

        GameType current = player.gameMode.getGameModeForPlayer();
        GameType next = current == GameType.SURVIVAL ? GameType.CREATIVE : GameType.SURVIVAL;
        if (player.setGameMode(next)) {
            player.sendSystemMessage(Component.literal(
                    "ゲームモードを " + (next == GameType.CREATIVE ? "クリエイティブ" : "サバイバル") + " に変更しました。"
            ));
        }
    }
}
