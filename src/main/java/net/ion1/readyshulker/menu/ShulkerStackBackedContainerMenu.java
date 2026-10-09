package net.ion1.readyshulker.menu;

import net.ion1.readyshulker.container.ItemStackBackedContainer;
import net.ion1.readyshulker.container.SharedShulkerContainers;
import net.minecraft.world.Container;
import net.minecraft.world.entity.player.Inventory;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.inventory.ShulkerBoxMenu;
import net.minecraft.world.inventory.ShulkerBoxSlot;
import net.minecraft.world.inventory.Slot;
import net.minecraft.world.item.ItemStack;
import org.jspecify.annotations.NonNull;

public class ShulkerStackBackedContainerMenu extends ShulkerBoxMenu {
    private final Container parent;
    private final int parentSlot;
    private final ItemStack expectedStack;
    private final ItemStackBackedContainer backingContainer;

    public ShulkerStackBackedContainerMenu(int containerId, Inventory inventory, Container parent, int parentSlot, ItemStack expectedStack, ItemStackBackedContainer backingContainer) {
        super(containerId, inventory, backingContainer);
        this.parent = parent;
        this.parentSlot = parentSlot;
        this.expectedStack = expectedStack;
        this.backingContainer = backingContainer;
        this.installBackingSlots();
    }

    public Container parent() {
        return this.parent;
    }

    public ItemStackBackedContainer backingContainer() {
        return this.backingContainer;
    }

    public boolean matchesBackingLocation(Container parent, int parentSlot, ItemStack expectedStack) {
        return this.parent == parent && this.parentSlot == parentSlot && this.expectedStack == expectedStack;
    }

    @Override
    public boolean stillValid(@NonNull Player player) {
        ItemStack current = this.parent.getItem(this.parentSlot);
        return current == this.expectedStack && !current.isEmpty() && this.parent.stillValid(player);
    }

    @Override
    public void removed(@NonNull Player player) {
        try {
            super.removed(player);
        } finally {
            SharedShulkerContainers.release(this.expectedStack, this.backingContainer);
        }
    }

    private void installBackingSlots() {
        for (int i = 0; i < this.backingContainer.getContainerSize(); i++) {
            Slot slot = new ShulkerBoxSlot(this.backingContainer, i, 8 + (i % 9) * 18, 18 + (i / 9) * 18);
            slot.index = i;
            this.slots.set(i, slot);
        }
    }
}

