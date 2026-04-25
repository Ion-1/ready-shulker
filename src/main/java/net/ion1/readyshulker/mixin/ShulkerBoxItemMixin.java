package net.ion1.readyshulker.mixin;

import net.ion1.readyshulker.BackedShulkerBoxMenu;
import net.ion1.readyshulker.QueuedMenuProvider;
import net.minecraft.core.NonNullList;
import net.minecraft.core.component.DataComponents;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.sounds.SoundEvents;
import net.minecraft.world.SimpleContainer;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.SlotAccess;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.inventory.ClickAction;
import net.minecraft.world.inventory.Slot;
import net.minecraft.world.item.BlockItem;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.component.ItemContainerContents;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.ShulkerBoxBlock;
import java.util.function.BiConsumer;
import java.util.function.IntFunction;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Unique;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;

@Mixin(Item.class)
public abstract class ShulkerBoxItemMixin {
    @Unique
    private static final int SHULKER_SLOT_COUNT = 27;

    @Inject(method = "overrideOtherStackedOnMe", at = @At("HEAD"), cancellable = true)
    private void readyShulker$overrideOtherStackedOnMe(ItemStack self, ItemStack other, Slot slot, ClickAction clickAction, Player player, SlotAccess carriedItem, CallbackInfoReturnable<Boolean> cir) {
        if (!(clickAction == ClickAction.SECONDARY)) {
            return;
        }

        if (!(slot.allowModification(player))) {
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

        if (other.isEmpty() && (!(serverPlayer.containerMenu instanceof BackedShulkerBoxMenu shulkerMenu) || !shulkerMenu.isBackingStack(self))) {
            player.containerMenu.setCarried(ItemStack.EMPTY);
            cir.setReturnValue(true);
            cir.cancel();
            readyShulker$openShulkerMenu(self, shulkerBlock, player);
            playOpenSound(player);
            return;
        }

        boolean success = readyShulker$insertIntoBox(self, other, carriedItem, serverPlayer);
        if (success) {
            playInsertSound(player);
        } else {
            playInsertFailSound(player);
        }
        cir.setReturnValue(true);
        cir.cancel();
    }

    @Unique
    private static void readyShulker$openShulkerMenu(ItemStack shulkerStack, ShulkerBoxBlock block, Player player) {
        if (!(player instanceof ServerPlayer serverPlayer)) {
            return;
        }

        NonNullList<ItemStack> items = NonNullList.withSize(SHULKER_SLOT_COUNT, ItemStack.EMPTY);
        ItemContainerContents contents = shulkerStack.get(DataComponents.CONTAINER);
        if (contents != null) {
            contents.copyInto(items);
        }

        SimpleContainer container = new SimpleContainer(SHULKER_SLOT_COUNT);
        for (int i = 0; i < items.size(); i++) {
            container.setItem(i, items.get(i).copy());
        }

        // We enqueue the menu opening so the clicked method can wrap up
        // A SimpleMenuProvider would have the InventoryMenu.carried to be set to the shulker stack
        QueuedMenuProvider.enqueue(
                serverPlayer,
                (containerId, inventory, _)
                        -> new BackedShulkerBoxMenu(containerId, inventory, container, shulkerStack),
                block.getName());
    }

    @Unique
    private static boolean readyShulker$insertIntoBox(ItemStack shulkerStack, ItemStack carriedStack, SlotAccess carriedSlot, ServerPlayer serverPlayer) {
        if (!carriedStack.getItem().canFitInsideContainerItems()) {
            return false;
        }

        if (serverPlayer.containerMenu instanceof BackedShulkerBoxMenu shulkerMenu && shulkerMenu.isBackingStack(shulkerStack)) {
            SimpleContainer container = shulkerMenu.getBackingContainer();
            ItemStack remainder = readyShulker$insertIntoContainer(container, carriedStack);
            int inserted = carriedStack.getCount() - remainder.getCount();
            if (inserted <= 0) {
                return false;
            }

            carriedSlot.set(remainder.isEmpty() ? ItemStack.EMPTY : remainder);
            shulkerMenu.syncBackingFromContainer();
            return true;
        }

        ItemContainerContents contents = shulkerStack.get(DataComponents.CONTAINER);
        NonNullList<ItemStack> items = NonNullList.withSize(SHULKER_SLOT_COUNT, ItemStack.EMPTY);
        if (contents != null) {
            contents.copyInto(items);
        }

        ItemStack remainder = readyShulker$insertIntoItems(items, carriedStack);

        int inserted = carriedStack.getCount() - remainder.getCount();
        if (inserted <= 0) {
            return false;
        }

        shulkerStack.set(DataComponents.CONTAINER, ItemContainerContents.fromItems(items));
        carriedSlot.set(remainder.isEmpty() ? ItemStack.EMPTY : remainder);
        return true;
    }

    @Unique
    private static ItemStack readyShulker$insertIntoContainer(SimpleContainer container, ItemStack carriedStack) {
        return readyShulker$insertIntoSlots(container.getContainerSize(), container::getItem, container::setItem, carriedStack);
    }

    @Unique
    private static ItemStack readyShulker$insertIntoItems(NonNullList<ItemStack> items, ItemStack carriedStack) {
        return readyShulker$insertIntoSlots(items.size(), items::get, items::set, carriedStack);
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
    private static void playInsertSound(final Entity entity) {
        entity.playSound(SoundEvents.BUNDLE_INSERT, 0.8F, 0.8F + entity.level().getRandom().nextFloat() * 0.4F);
    }

    @Unique
    private static void playInsertFailSound(final Entity entity) {
        entity.playSound(SoundEvents.BUNDLE_INSERT_FAIL, 1.0F, 1.0F);
    }

    @Unique
    private static void playOpenSound(final Entity entity) {
        entity.playSound(SoundEvents.SHULKER_BOX_OPEN, 1.0F, 1.0F);
    }
}

