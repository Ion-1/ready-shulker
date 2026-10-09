package net.ion1.readyshulker.container;

import net.minecraft.core.NonNullList;
import net.minecraft.core.component.DataComponents;
import net.minecraft.world.Container;
import net.minecraft.world.ContainerHelper;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.component.ItemContainerContents;
import org.jspecify.annotations.NullMarked;

/**
 * Live mutable inventory while menus are open; persisted into the backing stack's data component on change.
 */
@NullMarked
public class ItemStackBackedContainer implements Container {
    public static final int SIZE = 27;

    private final ItemStack backingStack;
    private final Container parent;
    private final int parentSlot;
    private final NonNullList<ItemStack> items = NonNullList.withSize(SIZE, ItemStack.EMPTY);
    private ItemContainerContents lastContents;

    ItemStackBackedContainer(ItemStack backingStack, Container parent, int parentSlot) {
        this.backingStack = backingStack;
        this.parent = parent;
        this.parentSlot = parentSlot;
        this.refreshFromComponent();
    }

    private void refreshFromComponent() {
        this.lastContents = this.backingStack.getOrDefault(DataComponents.CONTAINER, ItemContainerContents.EMPTY);
        this.lastContents.copyInto(this.items);
    }

    private void refreshIfChanged() {
        ItemContainerContents current = this.backingStack.getOrDefault(DataComponents.CONTAINER, ItemContainerContents.EMPTY);
        if (current != this.lastContents) {
            this.lastContents = current;
            this.lastContents.copyInto(this.items);
        }
    }

    @Override
    public int getContainerSize() {
        return SIZE;
    }

    @Override
    public boolean isEmpty() {
        this.refreshIfChanged();
        for (ItemStack stack : this.items) {
            if (!stack.isEmpty()) return false;
        }
        return true;
    }

    @Override
    public ItemStack getItem(int slot) {
        if (slot < 0 || slot >= SIZE) return ItemStack.EMPTY;
        this.refreshIfChanged();
        return this.items.get(slot);
    }

    @Override
    public ItemStack removeItem(int slot, int amount) {
        if (slot < 0 || slot >= SIZE || amount <= 0) return ItemStack.EMPTY;
        this.refreshIfChanged();
        ItemStack removed = ContainerHelper.removeItem(this.items, slot, amount);
        if (!removed.isEmpty()) this.setChanged();
        return removed;
    }

    @Override
    public ItemStack removeItemNoUpdate(int slot) {
        if (slot < 0 || slot >= SIZE) return ItemStack.EMPTY;
        this.refreshIfChanged();
        ItemStack removed = ContainerHelper.takeItem(this.items, slot);
        if (!removed.isEmpty()) this.setChanged();
        return removed;
    }

    @Override
    public void setItem(int slot, ItemStack stack) {
        if (slot < 0 || slot >= SIZE) return;
        this.refreshIfChanged();
        this.items.set(slot, stack);
        this.setChanged();
    }

    @Override
    public void setChanged() {
        this.lastContents = ItemContainerContents.fromItems(this.items);
        this.backingStack.set(DataComponents.CONTAINER, this.lastContents);
        if (this.parentSlot >= 0 && this.parentSlot < this.parent.getContainerSize() && this.parent.getItem(this.parentSlot) == this.backingStack) {
            this.parent.setChanged();
        }
    }

    @Override
    public boolean stillValid(Player player) {
        return this.parentSlot >= 0 && this.parentSlot < this.parent.getContainerSize() && this.parent.getItem(this.parentSlot) == this.backingStack && this.parent.stillValid(player);
    }

    @Override
    public void clearContent() {
        this.refreshIfChanged();
        for (int i = 0; i < SIZE; i++) this.items.set(i, ItemStack.EMPTY);
        this.setChanged();
    }
}
