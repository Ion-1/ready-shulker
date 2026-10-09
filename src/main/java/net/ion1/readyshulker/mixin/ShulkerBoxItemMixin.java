package net.ion1.readyshulker.mixin;

import net.ion1.readyshulker.QueuedMenuProvider;
import net.ion1.readyshulker.container.ItemStackBackedContainer;
import net.ion1.readyshulker.container.SharedShulkerContainers;
import net.ion1.readyshulker.menu.ShulkerStackBackedContainerMenu;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.sounds.SoundEvents;
import net.minecraft.world.Container;
import net.minecraft.world.entity.SlotAccess;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.inventory.AbstractContainerMenu;
import net.minecraft.world.inventory.ClickAction;
import net.minecraft.world.inventory.Slot;
import net.minecraft.world.item.BlockItem;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.ShulkerBoxBlock;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Unique;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;

import java.util.function.BiConsumer;
import java.util.function.IntFunction;

@Mixin(Item.class)
public abstract class ShulkerBoxItemMixin {
    @Unique
    private static boolean readyShulker$isOpenBackingMenu(AbstractContainerMenu menu, Container parent, int parentSlot, ItemStack expectedStack) {
        return menu instanceof ShulkerStackBackedContainerMenu shulkerMenu && shulkerMenu.matchesBackingLocation(parent, parentSlot, expectedStack);
    }

    @Unique
    private static void readyShulker$openShulkerMenu(Container parent, int parentSlot, ItemStack expectedStack, ShulkerBoxBlock block, ServerPlayer serverPlayer) {
        QueuedMenuProvider.enqueue(serverPlayer, (containerId, inventory, player) -> {
            if (!parent.stillValid(player)) {
                return null;
            }

            if (parentSlot < 0 || parentSlot >= parent.getContainerSize()) {
                return null;
            }

            ItemStack current = parent.getItem(parentSlot);
            if (current.isEmpty()) {
                return null;
            }

            if (current != expectedStack) {
                return null;
            }

            ItemStackBackedContainer backingContainer = SharedShulkerContainers.acquire(expectedStack, parent, parentSlot);
            try {
                return new ShulkerStackBackedContainerMenu(containerId, inventory, parent, parentSlot, expectedStack, backingContainer);
            } catch (RuntimeException | Error e) {
                SharedShulkerContainers.release(expectedStack, backingContainer);
                throw e;
            }
        }, block.getName());
    }

    @Unique
    private static boolean readyShulker$insertIntoBox(Container parent, int parentSlot, ItemStack expectedStack, ItemStack carriedStack, SlotAccess carriedSlot, ServerPlayer serverPlayer) {
        if (!carriedStack.getItem().canFitInsideContainerItems()) {
            return false;
        }

        // Reuse the same live inventory as any open shulker menu, even when it
        // originated from a different player's interaction
        ItemStackBackedContainer backingContainer = SharedShulkerContainers.acquire(expectedStack, parent, parentSlot);
        try {
            ItemStack remainder = readyShulker$insertIntoContainer(backingContainer, carriedStack);
            int inserted = carriedStack.getCount() - remainder.getCount();
            if (inserted <= 0) {
                return false;
            }
            carriedSlot.set(remainder.isEmpty() ? ItemStack.EMPTY : remainder);
            return true;
        } finally {
            SharedShulkerContainers.release(expectedStack, backingContainer);
        }
    }

    @Unique
    private static ItemStack readyShulker$insertIntoContainer(Container container, ItemStack carriedStack) {
        return readyShulker$insertIntoSlots(container.getContainerSize(), container::getItem, container::setItem, carriedStack);
    }

    @Unique
    private static ItemStack readyShulker$insertIntoSlots(int slotCount, IntFunction<ItemStack> getItem, BiConsumer<Integer, ItemStack> setItem, ItemStack carriedStack) {
        ItemStack remainder = carriedStack.copy();
        int maxStackSize = remainder.getItem().getDefaultMaxStackSize();

        for (int i = 0; i < slotCount && !remainder.isEmpty(); i++) {
            ItemStack existing = getItem.apply(i);
            if (!existing.isEmpty() && ItemStack.isSameItemSameComponents(existing, remainder)) {
                int space = maxStackSize - existing.getCount();
                if (space > 0) {
                    int moved = Math.min(space, remainder.getCount());
                    setItem.accept(i, existing.copyWithCount(existing.getCount() + moved));
                    remainder.shrink(moved);
                }
            }
        }

        for (int i = 0; i < slotCount && !remainder.isEmpty(); i++) {
            if (getItem.apply(i).isEmpty()) {
                int moved = Math.min(maxStackSize, remainder.getCount());
                setItem.accept(i, remainder.copyWithCount(moved));
                remainder.shrink(moved);
            }
        }

        return remainder;
    }

    @Unique
    private static void playInsertSound(Player player) {
        player.playSound(SoundEvents.BUNDLE_INSERT, 0.8F, 1.0F);
    }

    @Unique
    private static void playInsertFailSound(Player player) {
        player.playSound(SoundEvents.BUNDLE_INSERT_FAIL, 1.0F, 1.0F);
    }

    @Unique
    private static void playOpenSound(Player player) {
        player.playSound(SoundEvents.SHULKER_BOX_OPEN, 1.0F, 1.0F);
    }

    @Inject(method = "overrideOtherStackedOnMe", at = @At("HEAD"), cancellable = true)
    private void readyShulker$overrideOtherStackedOnMe(ItemStack self, ItemStack other, Slot slot, ClickAction clickAction, Player player, SlotAccess carriedItem, CallbackInfoReturnable<Boolean> cir) {
        if (clickAction != ClickAction.SECONDARY || !slot.allowModification(player)) {
            return;
        }

        if (!(self.getItem() instanceof BlockItem blockItem)) {
            return;
        }

        Block block = blockItem.getBlock();
        if (!(block instanceof ShulkerBoxBlock shulkerBlock)) {
            return;
        }

        if (!(player instanceof ServerPlayer serverPlayer)) {
            return;
        }

        Container parent = slot.container;
        int parentSlot = slot.getContainerSlot();
        ItemStack expectedStack = parent.getItem(parentSlot);

        if (other.isEmpty()) {
            if (!readyShulker$isOpenBackingMenu(serverPlayer.containerMenu, parent, parentSlot, expectedStack)) {
                readyShulker$openShulkerMenu(parent, parentSlot, expectedStack, shulkerBlock, serverPlayer);
                playOpenSound(player);
            }

            cir.setReturnValue(true);
            cir.cancel();
            return;
        }

        boolean success = readyShulker$insertIntoBox(parent, parentSlot, expectedStack, other, carriedItem, serverPlayer);
        if (success) {
            playInsertSound(player);
        } else {
            playInsertFailSound(player);
        }

        cir.setReturnValue(true);
        cir.cancel();
    }
}

