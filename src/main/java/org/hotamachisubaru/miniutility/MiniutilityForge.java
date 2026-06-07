package org.hotamachisubaru.miniutility;

import net.minecraft.core.BlockPos;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.phys.Vec3;
import net.minecraftforge.common.MinecraftForge;
import net.minecraftforge.event.entity.living.LivingDeathEvent;
import net.minecraftforge.event.entity.player.PlayerEvent;
import net.minecraftforge.event.server.ServerStartingEvent;
import net.minecraftforge.eventbus.api.EventPriority;
import net.minecraftforge.eventbus.api.IEventBus;
import net.minecraftforge.eventbus.api.SubscribeEvent;
import net.minecraftforge.fml.common.Mod;
import net.minecraftforge.fml.event.lifecycle.FMLCommonSetupEvent;
import net.minecraftforge.fml.javafmlmod.FMLJavaModLoadingContext;
import org.hotamachisubaru.miniutility.forge.GUI.ForgeGuiActionService;
import org.hotamachisubaru.miniutility.forge.GUI.ForgeGuiListener;
import org.hotamachisubaru.miniutility.forge.GUI.ForgeTrashBoxSessionStore;
import org.hotamachisubaru.miniutility.forge.chat.ForgeChatListener;
import org.hotamachisubaru.miniutility.forge.command.ForgeCommandManager;
import org.hotamachisubaru.miniutility.forge.creeper.ForgeCreeperProtectionService;
import org.hotamachisubaru.miniutility.forge.death.DeathLocationStore;
import org.hotamachisubaru.miniutility.forge.nickname.ForgeNicknameManager;
import org.hotamachisubaru.miniutility.forge.util.ForgeLuckPermsUtil;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

@Mod(MiniutilityForge.MOD_ID)
public final class MiniutilityForge {
    public static final String MOD_ID = "miniutility";
    public static final Logger LOGGER = LoggerFactory.getLogger(MOD_ID);

    private static MiniutilityForge instance;

    private final DeathLocationStore deathLocationStore;
    private final ForgeLuckPermsUtil luckPermsUtil;
    private final ForgeNicknameManager nicknameManager;
    private final ForgeCreeperProtectionService creeperProtectionService;
    private final ForgeTrashBoxSessionStore trashBoxSessionStore;
    private final ForgeGuiActionService guiActionService;
    private final ForgeCommandManager commandManager;
    private final ForgeChatListener chatListener;
    private final ForgeGuiListener guiListener;

    public MiniutilityForge() {
        instance = this;

        this.deathLocationStore = new DeathLocationStore();
        this.luckPermsUtil = new ForgeLuckPermsUtil();
        this.nicknameManager = new ForgeNicknameManager(this.luckPermsUtil);
        this.creeperProtectionService = new ForgeCreeperProtectionService(false);
        this.trashBoxSessionStore = new ForgeTrashBoxSessionStore();
        this.guiActionService = new ForgeGuiActionService(
                this.nicknameManager,
                this.creeperProtectionService,
                this.deathLocationStore,
                this.trashBoxSessionStore
        );
        this.chatListener = new ForgeChatListener(this.nicknameManager, this.guiActionService);
        this.commandManager = new ForgeCommandManager(
                this.nicknameManager,
                this.guiActionService,
                this.chatListener
        );
        this.guiListener = new ForgeGuiListener(this.trashBoxSessionStore);

        IEventBus modBus = FMLJavaModLoadingContext.get().getModEventBus();
        modBus.addListener(this::onCommonSetup);

        MinecraftForge.EVENT_BUS.register(this);
        MinecraftForge.EVENT_BUS.register(this.commandManager);
        MinecraftForge.EVENT_BUS.register(this.creeperProtectionService);
        MinecraftForge.EVENT_BUS.register(this.chatListener);
        MinecraftForge.EVENT_BUS.register(this.guiListener);
    }

    public static MiniutilityForge getInstance() {
        return instance;
    }

    public DeathLocationStore getDeathLocationStore() {
        return deathLocationStore;
    }

    public ForgeNicknameManager getNicknameManager() {
        return nicknameManager;
    }

    public ForgeCreeperProtectionService getCreeperProtectionService() {
        return creeperProtectionService;
    }

    public ForgeCommandManager getCommandManager() {
        return commandManager;
    }

    public ForgeGuiListener getGuiListener() {
        return guiListener;
    }

    public ForgeTrashBoxSessionStore getTrashBoxSessionStore() {
        return trashBoxSessionStore;
    }

    public ForgeGuiActionService getGuiActionService() {
        return guiActionService;
    }

    public ForgeLuckPermsUtil getLuckPermsUtil() {
        return luckPermsUtil;
    }

    private void onCommonSetup(FMLCommonSetupEvent event) {
        LOGGER.info("Miniutility Forge mod is setting up.");
    }

    @SubscribeEvent
    public void onServerStarting(ServerStartingEvent event) {
        this.nicknameManager.reload(event.getServer());
        LOGGER.info("Miniutility Forge mod is starting on server.");
    }

    @SubscribeEvent(priority = EventPriority.HIGHEST)
    public void onPlayerDeath(LivingDeathEvent event) {
        if (!(event.getEntity() instanceof ServerPlayer player)) {
            return;
        }

        BlockPos deathBlock = player.blockPosition().above();
        Vec3 deathPosition = Vec3.atCenterOf(deathBlock);
        this.deathLocationStore.record(player.getUUID(), deathPosition, player.level().dimension(), player.getYRot());
    }

    @SubscribeEvent
    public void onPlayerClone(PlayerEvent.Clone event) {
        if (!event.isWasDeath() || !(event.getEntity() instanceof ServerPlayer cloned)) {
            return;
        }

        DeathLocationStore.DeathLocation location = this.deathLocationStore.getDeathLocation(event.getOriginal().getUUID());
        if (location != null) {
            this.deathLocationStore.record(cloned.getUUID(), location.pos(), location.dim(), location.yaw());
        }
    }
}
