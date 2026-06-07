package org.hotamachisubaru.miniutility.forge.GUI;

import net.minecraft.network.chat.Component;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.Container;
import net.minecraft.world.SimpleContainer;
import net.minecraft.world.entity.player.Inventory;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.inventory.AbstractContainerMenu;
import net.minecraft.world.inventory.ClickType;
import net.minecraft.world.inventory.MenuType;
import net.minecraft.world.inventory.Slot;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Items;

public final class ForgeDeathLocationMenu extends AbstractContainerMenu {
    private static final int TELEPORT_SLOT = 13;
    private static final int BACK_SLOT = 26;

    private final Container container;
    private final ForgeGuiActionService actionService;

    public ForgeDeathLocationMenu(int windowId, Inventory playerInventory, ForgeGuiActionService actionService) {
        super(MenuType.GENERIC_9x3, windowId);
        this.container = new SimpleContainer(27);
        this.actionService = actionService;

        this.container.setItem(TELEPORT_SLOT, namedItem(Items.COMPASS.getDefaultInstance(), "死亡地点へワープ"));
        this.container.setItem(BACK_SLOT, namedItem(Items.ARROW.getDefaultInstance(), "戻る"));

        addMenuSlots(playerInventory);
    }

    @Override
    public void clicked(int slotId, int button, ClickType clickType, Player player) {
        if (slotId >= 0 && slotId < this.container.getContainerSize()) {
            if (player instanceof ServerPlayer serverPlayer) {
                handleMenuClick(slotId, serverPlayer);
            }
            return;
        }

        super.clicked(slotId, button, clickType, player);
    }

    @Override
    public ItemStack quickMoveStack(Player player, int index) {
        return ItemStack.EMPTY;
    }

    @Override
    public boolean stillValid(Player player) {
        return true;
    }

    @Override
    public void removed(Player player) {
        super.removed(player);
        this.container.stopOpen(player);
    }

    private void handleMenuClick(int slotId, ServerPlayer player) {
        switch (slotId) {
            case TELEPORT_SLOT -> this.actionService.teleportToDeathLocation(player);
            case BACK_SLOT -> this.actionService.openMainMenu(player);
            default -> {
            }
        }
    }

    private void addMenuSlots(Inventory playerInventory) {
        this.container.startOpen(playerInventory.player);

        for (int row = 0; row < 3; row++) {
            for (int col = 0; col < 9; col++) {
                this.addSlot(new LockedSlot(this.container, col + row * 9, 8 + col * 18, 18 + row * 18));
            }
        }

        for (int row = 0; row < 3; row++) {
            for (int col = 0; col < 9; col++) {
                this.addSlot(new Slot(playerInventory, col + row * 9 + 9, 8 + col * 18, 84 + row * 18));
            }
        }

        for (int col = 0; col < 9; col++) {
            this.addSlot(new Slot(playerInventory, col, 8 + col * 18, 142));
        }
    }

    private static ItemStack namedItem(ItemStack stack, String name) {
        stack.setHoverName(Component.literal(name));
        return stack;
    }

    private static final class LockedSlot extends Slot {
        private LockedSlot(Container container, int slot, int x, int y) {
            super(container, slot, x, y);
        }

        @Override
        public boolean mayPlace(ItemStack stack) {
            return false;
        }

        @Override
        public boolean mayPickup(Player player) {
            return false;
        }
    }
}
