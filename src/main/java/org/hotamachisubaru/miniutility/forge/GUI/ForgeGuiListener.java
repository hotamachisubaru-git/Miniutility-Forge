package org.hotamachisubaru.miniutility.forge.GUI;

import net.minecraft.server.level.ServerPlayer;
import net.minecraftforge.event.entity.player.PlayerEvent;
import net.minecraftforge.eventbus.api.SubscribeEvent;

public final class ForgeGuiListener {
    private final ForgeTrashBoxSessionStore trashBoxSessionStore;

    public ForgeGuiListener(ForgeTrashBoxSessionStore trashBoxSessionStore) {
        this.trashBoxSessionStore = trashBoxSessionStore;
    }

    @SubscribeEvent
    public void onPlayerLoggedOut(PlayerEvent.PlayerLoggedOutEvent event) {
        if (event.getEntity() instanceof ServerPlayer player) {
            this.trashBoxSessionStore.endSession(player.getUUID());
        }
    }
}
